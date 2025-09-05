package uz.alex2276564.mmospawnpoint.manager;

import io.papermc.lib.PaperLib;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfig;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;
import uz.alex2276564.mmospawnpoint.utils.SecurityUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnManager {
    private final MMOSpawnPoint plugin;
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    private record PendingAfter(SpawnPointsConfig.LocationOption loc, SpawnPointsConfig.ActionsConfig global) {}
    private final Map<UUID, PendingAfter> pendingAfterActions = new ConcurrentHashMap<>();

    private final Map<UUID, BatchJob> activeBatchJobs = new ConcurrentHashMap<>();

    private int attemptsPerTick;
    private long timeBudgetNs;

    @Setter
    private PartyManager partyManager;

    public SpawnManager(MMOSpawnPoint plugin) {
        this.plugin = plugin;
        var batch = plugin.getConfigManager().getMainConfig().settings.safeSearchBatch;
        this.attemptsPerTick = Math.max(10, batch.attemptsPerTick);
        this.timeBudgetNs = Math.max(1, batch.timeBudgetMillis) * 1_000_000L;
    }

    public void recordDeathLocation(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        if (!deathLocations.containsKey(playerId)) {
            deathLocations.put(playerId, location.clone());
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Recorded death location for " + player.getName() + ": " + locationToString(location));
            }
        }
    }

    public void cleanupPlayerData(UUID playerId) {
        try {
            deathLocations.remove(playerId);
            cancelBatchJob(playerId);
            pendingAfterActions.remove(playerId);
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Cleaned up spawn manager data for player: " + playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up spawn manager data for " + playerId + ": " + e.getMessage());
        }
    }

    public boolean processJoinSpawn(Player player) {
        try {
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Processing join spawn for " + player.getName());
            }

            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled &&
                    partyManager != null && ("join".equals(partyScope) || "both".equals(partyScope))) {

                Location partyLocation = partyManager.findPartyJoinLocation(player);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    teleportPlayerWithDelay(player, partyLocation, "join");
                    return true;
                }
            }

            Location joinLocation = findSpawnLocationByPriority("join", player.getLocation(), player);
            if (joinLocation != null) {
                teleportPlayerWithDelay(player, joinLocation, "join");
                return true;
            } else {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("No join spawn location found for " + player.getName());
                }
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing join spawn for " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) e.printStackTrace();
            return false;
        }
    }

    public boolean processDeathSpawn(Player player) {
        try {
            Location deathLocation = deathLocations.remove(player.getUniqueId());
            if (deathLocation == null) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("No death location found for " + player.getName() + ", using server default");
                }
                return false;
            }

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Processing death spawn for " + player.getName() + " who died at " + locationToString(deathLocation));
            }

            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled &&
                    partyManager != null && ("death".equals(partyScope) || "both".equals(partyScope))) {

                Location partyLocation = partyManager.findPartyRespawnLocation(player, deathLocation);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                        plugin.getLogger().info("Using party respawn location for " + player.getName() + ": " + locationToString(partyLocation));
                    }
                    teleportPlayerWithDelay(player, partyLocation, "death");
                    return true;
                }
            }

            Location spawnLocation = findSpawnLocationByPriority("death", deathLocation, player);
            if (spawnLocation != null) {
                teleportPlayerWithDelay(player, spawnLocation, "death");
                return true;
            }

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().warning("No death spawn location found for " + player.getName());
            }
            return false;

        } catch (Exception e) {
            plugin.getLogger().severe("Error processing death spawn for " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) e.printStackTrace();
            return false;
        }
    }

    public void cleanup() {
        for (BatchJob job : activeBatchJobs.values()) {
            try { job.cancel(); } catch (Exception ignored) {}
        }
        activeBatchJobs.clear();
        pendingAfterActions.clear();
        deathLocations.clear();
    }

    public Location findSpawnLocationByPriority(String eventType, Location referenceLocation, Player player) {
        List<SpawnEntry> matchingEntries = plugin.getConfigManager().getMatchingSpawnEntries(eventType, referenceLocation);

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Found " + matchingEntries.size() + " matching spawn entries for " + eventType);
        }

        for (SpawnEntry entry : matchingEntries) {
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Checking spawn entry with priority " + entry.calculatedPriority() +
                        " from " + entry.fileName());
            }

            Location spawnLocation = processSpawnEntry(entry, player, eventType);
            if (spawnLocation != null) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Selected spawn entry with priority " + entry.calculatedPriority() +
                            " from " + entry.fileName());
                }
                return spawnLocation;
            }
        }

        return null;
    }

    private Location processSpawnEntry(SpawnEntry entry, Player player, String eventType) {
        SpawnPointsConfig.SpawnPointEntry data = entry.spawnData();
        return processEntry(player, data.conditions, data.destinations, data.actions, data.waitingRoom, eventType);
    }

    private Location processEntry(
            Player player,
            SpawnPointsConfig.ConditionsConfig conditions,
            List<SpawnPointsConfig.LocationOption> destinations,
            SpawnPointsConfig.ActionsConfig globalActions,
            SpawnPointsConfig.WaitingRoomConfig entryWaitingRoom,
            String eventType
    ) {
        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("processEntry eventType=" + eventType);
        }

        if (conditionsNotMet(player, conditions)) {
            return null;
        }

        if (destinations == null || destinations.isEmpty()) {
            runPhaseForActions(player, globalActions, SpawnPointsConfig.Phase.AFTER);
            return null;
        }

        SpawnPointsConfig.LocationOption selected = selectDestination(player, destinations);
        if (selected == null) return null;

        boolean requireSafe = selected.requireSafe;
        boolean useWaitingRoom = plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled && requireSafe;

        if (useWaitingRoom) {
            runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);
            runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.WAITING_ROOM);

            long enteredMs = System.currentTimeMillis();
            startBatchedLocationSearchForSelected(player, selected, globalActions, enteredMs);

            return getBestWaitingRoom(selected.waitingRoom, entryWaitingRoom);
        }

        runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);

        Location finalLoc = resolveNonSafeLocation(selected);
        if (finalLoc == null) return null;

        pendingAfterActions.put(player.getUniqueId(), new PendingAfter(selected, globalActions));
        return finalLoc;
    }

    private SpawnPointsConfig.LocationOption selectDestination(Player player, List<SpawnPointsConfig.LocationOption> options) {
        if (options.size() == 1) return options.get(0);

        int total = 0;
        for (SpawnPointsConfig.LocationOption opt : options) {
            total += getEffectiveWeight(player, opt);
        }
        if (total <= 0) return null;

        int rnd = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (SpawnPointsConfig.LocationOption opt : options) {
            acc += getEffectiveWeight(player, opt);
            if (rnd < acc) return opt;
        }
        return options.get(0);
    }

    private int getEffectiveWeight(Player player, SpawnPointsConfig.LocationOption option) {
        int weight = option.weight;
        if (option.weightConditions != null) {
            for (SpawnPointsConfig.WeightConditionEntry cond : option.weightConditions) {
                boolean match = switch (cond.type) {
                    case "permission" ->
                            PlaceholderUtils.evaluatePermissionExpression(player, cond.value, player.isOp() || player.hasPermission("*"));
                    case "placeholder" -> plugin.isPlaceholderAPIEnabled() && PlaceholderUtils.checkPlaceholderCondition(player, cond.value);
                    default -> false;
                };
                if (!match) continue;

                String mode = normMode(cond.mode);
                switch (mode) {
                    case "set" -> weight = cond.weight;
                    case "add" -> weight = weight + cond.weight;
                    case "mul" -> weight = (int) Math.round(weight * (double) cond.weight);
                }
                weight = clampWeight(weight);
            }
        }
        return weight;
    }

    private boolean conditionsNotMet(Player player, SpawnPointsConfig.ConditionsConfig conditions) {
        if (conditions == null) return false;

        boolean bypass = player.isOp() || player.hasPermission("*");

        if (conditions.permissions != null && !conditions.permissions.isEmpty()) {
            for (String permissionExpr : conditions.permissions) {
                if (!PlaceholderUtils.evaluatePermissionExpression(player, permissionExpr, bypass)) {
                    return true;
                }
            }
        }

        if (conditions.placeholders != null && !conditions.placeholders.isEmpty() && plugin.isPlaceholderAPIEnabled()) {
            for (String placeholderExpr : conditions.placeholders) {
                if (!PlaceholderUtils.checkPlaceholderCondition(player, placeholderExpr)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void startBatchedLocationSearchForSelected(Player player,
                                                       SpawnPointsConfig.LocationOption selected,
                                                       SpawnPointsConfig.ActionsConfig globalActions,
                                                       long enteredWaitingMs) {
        UUID playerId = player.getUniqueId();
        cancelBatchJob(playerId);

        BatchJob job = new BatchJob(player, selected, globalActions, enteredWaitingMs);
        activeBatchJobs.put(playerId, job);
        job.start();
    }

    private void cancelBatchJob(UUID playerId) {
        BatchJob prev = activeBatchJobs.remove(playerId);
        if (prev != null) {
            prev.cancel();
        }
    }

    private class BatchJob extends BukkitRunnable {
        final Player player;
        final UUID playerId;
        final SpawnPointsConfig.LocationOption option;
        final SpawnPointsConfig.ActionsConfig globalActions;
        final long waitingEnteredAtMs;

        int pendingChunkX = Integer.MIN_VALUE;
        int pendingChunkZ = Integer.MIN_VALUE;
        boolean waitingChunkLoad = false;

        final World world;
        final boolean requireSafe;

        final List<Rect> includeRects;
        final List<Rect> excludeRects;

        final boolean isFixedPoint;
        int currentRadius;
        int attemptsDone;

        int failFeet = 0;
        int failHead = 0;
        int failGroundNotSolid = 0;
        int failGroundBlacklisted = 0;
        int failGroundNotWhitelisted = 0;

        BatchJob(Player p,
                 SpawnPointsConfig.LocationOption option,
                 SpawnPointsConfig.ActionsConfig globalActions,
                 long waitingEnteredAtMs) {
            this.player = p;
            this.playerId = p.getUniqueId();
            this.option = option;
            this.globalActions = globalActions;
            this.waitingEnteredAtMs = waitingEnteredAtMs;

            this.world = Bukkit.getWorld(option.world);
            this.requireSafe = option.requireSafe;

            this.includeRects = buildRectsForOption(option, world);
            this.excludeRects = toRects(option.excludeRects, world);

            this.isFixedPoint = isFixedPointOption(option);
            this.currentRadius = plugin.getConfigManager().getMainConfig().settings.safeLocationRadius;
            this.attemptsDone = 0;
        }

        void start() { this.runTaskTimer(plugin, 1L, 1L); }

        @Override
        public void run() {
            if (!player.isOnline()) { finish(null, false); return; }
            if (world == null) { finish(null, false); return; }

            // Timeout on waiting room search (re-use config)
            long timeoutMs = plugin.getConfigManager().getMainConfig().settings.waitingRoom.asyncSearchTimeout * 1000L;
            if (timeoutMs > 0 && System.currentTimeMillis() - waitingEnteredAtMs > timeoutMs) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().warning("[MMOSpawnPoint] Safe search TIMEOUT for " + player.getName()
                            + " in world=" + world.getName()
                            + " attempts=" + attemptsDone
                            + " fail{feet=" + failFeet
                            + ", head=" + failHead
                            + ", groundNotSolid=" + failGroundNotSolid
                            + ", blacklisted=" + failGroundBlacklisted
                            + ", notWhitelisted=" + failGroundNotWhitelisted + "}"
                    );
                }
                finish(null, false);
                return;
            }

            long endBy = System.nanoTime() + timeBudgetNs;
            int attemptsThisTick = 0;

            while (attemptsThisTick < attemptsPerTick && System.nanoTime() < endBy) {
                attemptsThisTick++;
                attemptsDone++;

                Location found;

                if (isFixedPoint) {
                    double x = option.x.value;
                    double z = option.z.value;
                    double y = (option.y != null && option.y.isValue())
                            ? option.y.value
                            : world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;
                    Location base = new Location(world, x, y, z);

                    if (!world.isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
                        PaperLib.getChunkAtAsync(base, true).thenRun(() -> {});
                        break;
                    }

                    found = SafeLocationFinder.attemptFindSafeNearSingle(base, currentRadius, toMaterialSet(option.groundWhitelist));
                    if (found == null) {
                        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                            var tag = SafeLocationFinder.pollLastFailTag();
                            if (tag != null) switch (tag) {
                                case FEET_NOT_PASSABLE -> failFeet++;
                                case HEAD_NOT_PASSABLE -> failHead++;
                                case GROUND_NOT_SOLID -> failGroundNotSolid++;
                                case GROUND_BLACKLISTED -> failGroundBlacklisted++;
                                case GROUND_NOT_WHITELISTED -> failGroundNotWhitelisted++;
                            }
                        }
                        if (attemptsDone % Math.max(2, plugin.getConfigManager().getMainConfig().settings.maxSafeLocationAttempts / 3) == 0) {
                            currentRadius = Math.min(currentRadius * 2,
                                    Math.max(currentRadius, plugin.getConfigManager().getMainConfig().settings.safeLocationRadius * 4));
                        }
                    }

                } else {
                    Rect rect = pickRect(includeRects);
                    if (rect == null) {
                        RegionSpec area = resolveAreaFromOption(option, world);
                        found = SafeLocationFinder.attemptFindSafeInRegionSingle(
                                world, area.minX(), area.maxX(), area.minY(), area.maxY(), area.minZ(), area.maxZ(), toMaterialSet(option.groundWhitelist));
                    } else {
                        found = attemptInRect(rect, excludeRects, toMaterialSet(option.groundWhitelist));
                    }
                }

                if (found != null) {
                    applyYawPitch(option, found);
                    finish(found, true);
                    return;
                }
            }
        }

        private Location attemptInRect(Rect rect, List<Rect> exclude, Set<Material> wl) {
            // 1) If we are already waiting for a specific chunk to load, we simply check its readiness.
            if (waitingChunkLoad) {
                if (!world.isChunkLoaded(pendingChunkX, pendingChunkZ)) {
                    // still loading, yield this tick
                    return null;
                }
                // loaded, you can search inside this chunk
                waitingChunkLoad = false;
                // We do not select any new candidates — we use the already selected chunk
                // Intersection of a rectangle with the chunk boundaries
                double chunkMinX = (pendingChunkX << 4);
                double chunkMaxX = chunkMinX + 15;
                double chunkMinZ = (pendingChunkZ << 4);
                double chunkMaxZ = chunkMinZ + 15;

                double minX = Math.max(Math.min(rect.minX(), rect.maxX()), chunkMinX);
                double maxX = Math.min(Math.max(rect.minX(), rect.maxX()), chunkMaxX);
                double minZ = Math.max(Math.min(rect.minZ(), rect.maxZ()), chunkMinZ);
                double maxZ = Math.min(Math.max(rect.minZ(), rect.maxZ()), chunkMaxZ);

                if (minX > maxX || minZ > maxZ) {
                    // just in case — if there is no intersection (should not happen)
                    pendingChunkX = Integer.MIN_VALUE;
                    pendingChunkZ = Integer.MIN_VALUE;
                    return null;
                }

                // One attempt inside THIS chunk
                Location loc = SafeLocationFinder.attemptFindSafeInRegionSingle(
                        world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl);
                if (loc == null) {
                    // не нашли здесь — сбрасываем pending и дадим выбрать следующий chunk в следующий цикл
                    pendingChunkX = Integer.MIN_VALUE;
                    pendingChunkZ = Integer.MIN_VALUE;
                    return null;
                }

                // excludeRects
                if (!exclude.isEmpty()) {
                    for (Rect ex : exclude) {
                        if (isInsideRect(loc, ex)) {
                            // эта точка запрещена — ищем дальше (в этом же чанке повторять не будем, просто сбросим и на следующем тике выберем другой чанк)
                            pendingChunkX = Integer.MIN_VALUE;
                            pendingChunkZ = Integer.MIN_VALUE;
                            return null;
                        }
                    }
                }
                // found a valid point
                pendingChunkX = Integer.MIN_VALUE;
                pendingChunkZ = Integer.MIN_VALUE;
                return loc;
            }

            // 2) No active loading — select a random chunk within rect
            double x = ThreadLocalRandom.current().nextDouble(Math.min(rect.minX(), rect.maxX()), Math.max(rect.minX(), rect.maxX()));
            double z = ThreadLocalRandom.current().nextDouble(Math.min(rect.minZ(), rect.maxZ()), Math.max(rect.minZ(), rect.maxZ()));
            int cx = ((int) Math.floor(x)) >> 4;
            int cz = ((int) Math.floor(z)) >> 4;

            // If the chunk is not loaded, we request it and wait.
            if (!world.isChunkLoaded(cx, cz)) {
                pendingChunkX = cx;
                pendingChunkZ = cz;
                waitingChunkLoad = true;
                Location loadProbe = new Location(world, (cx << 4) + 8, rect.minY(), (cz << 4) + 8);
                PaperLib.getChunkAtAsync(loadProbe, true).thenRun(() -> {});
                return null;
            }

            // The chunk is already loaded — we immediately try within its boundaries (intersection)
            double chunkMinX = (cx << 4);
            double chunkMaxX = chunkMinX + 15;
            double chunkMinZ = (cz << 4);
            double chunkMaxZ = chunkMinZ + 15;

            double minX = Math.max(Math.min(rect.minX(), rect.maxX()), chunkMinX);
            double maxX = Math.min(Math.max(rect.minX(), rect.maxX()), chunkMaxX);
            double minZ = Math.max(Math.min(rect.minZ(), rect.maxZ()), chunkMinZ);
            double maxZ = Math.min(Math.max(rect.minZ(), rect.maxZ()), chunkMaxZ);

            if (minX > maxX || minZ > maxZ) {
                return null;
            }

            Location loc = SafeLocationFinder.attemptFindSafeInRegionSingle(
                    world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl);
            if (loc == null) return null;

            if (!exclude.isEmpty()) {
                for (Rect ex : exclude) {
                    if (isInsideRect(loc, ex)) return null;
                }
            }
            return loc;
        }

        private void finish(Location found, boolean success) {
            try { this.cancel(); } catch (Exception ignored) {}
            activeBatchJobs.remove(playerId);

            if (!success || found == null) return;

            int delayConfig = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;
            int minStayTicks = plugin.getConfigManager().getMainConfig().settings.waitingRoom.minStayTicks;
            long elapsedMs = System.currentTimeMillis() - waitingEnteredAtMs;
            long requiredMs = Math.max(0L, minStayTicks * 50L - elapsedMs);
            int requiredTicks = (int) Math.ceil(requiredMs / 50.0);
            int finalDelay = Math.max(1, Math.max(delayConfig, requiredTicks));

            plugin.getRunner().runDelayed(() -> {
                if (!player.isOnline()) return;
                PaperLib.teleportAsync(player, found).thenAccept(successTp -> {
                    if (Boolean.TRUE.equals(successTp)) {
                        runPhaseForEntry(player, option, globalActions, SpawnPointsConfig.Phase.AFTER);
                    }
                });
            }, finalDelay);
        }

        private List<Rect> buildRectsForOption(SpawnPointsConfig.LocationOption option, World world) {
            // If rects are explicitly provided — use them
            List<Rect> rects = toRects(option.rects, world);
            if (!rects.isEmpty()) return rects;

            // If it's a fixed point (x/z have value; y null|value) — keep near-fixed behavior (no rects)
            if (isFixedPointOption(option)) {
                return Collections.emptyList();
            }

            // Otherwise build a single rect from axis (xyz)
            RegionSpec area = resolveAreaFromOption(option, world);
            return List.of(new Rect(area.minX(), area.maxX(), area.minY(), area.maxY(), area.minZ(), area.maxZ()));

        }
    }

    private Rect rectFromAxesOption(SpawnPointsConfig.LocationOption option, World world) {
        RegionSpec area = resolveAreaFromOption(option, world);
        return new Rect(area.minX(), area.maxX(), area.minY(), area.maxY(), area.minZ(), area.maxZ());
    }

    private void applyYawPitch(SpawnPointsConfig.LocationOption option, Location loc) {
        float yaw = (option.yaw == null) ? loc.getYaw()
                : option.yaw.isValue() ? option.yaw.value.floatValue()
                : (float) (option.yaw.min + ThreadLocalRandom.current().nextDouble() * (option.yaw.max - option.yaw.min));
        float pitch = (option.pitch == null) ? loc.getPitch()
                : option.pitch.isValue() ? option.pitch.value.floatValue()
                : (float) (option.pitch.min + ThreadLocalRandom.current().nextDouble() * (option.pitch.max - option.pitch.min));
        pitch = (float) clampPitch(pitch);

        loc.setYaw(yaw);
        loc.setPitch(pitch);
    }

    private record Rect(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {}
    private record RegionSpec(double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {}

    private List<Rect> toRects(List<SpawnPointsConfig.RectSpec> list, World world) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        List<Rect> out = new ArrayList<>();
        for (SpawnPointsConfig.RectSpec r : list) {
            if (r == null || r.x == null || r.z == null) continue;
            double minX = r.x.isValue() ? r.x.value : r.x.min;
            double maxX = r.x.isValue() ? r.x.value : r.x.max;
            double minZ = r.z.isValue() ? r.z.value : r.z.min;
            double maxZ = r.z.isValue() ? r.z.value : r.z.max;

            double minY;
            double maxY;
            if (r.y == null) {
                minY = resolveWorldMinY(world);
                maxY = world.getMaxHeight();
            } else if (r.y.isValue()) {
                minY = r.y.value;
                maxY = r.y.value;
            } else {
                minY = r.y.min;
                maxY = r.y.max;
            }

            out.add(new Rect(minX, maxX, minY, maxY, minZ, maxZ));
        }
        return out;
    }

    private int resolveWorldMinY(World world) {
        try {
            return (int) World.class.getMethod("getMinHeight").invoke(world);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private Rect pickRect(List<Rect> rects) {
        if (rects == null || rects.isEmpty()) return null;
        return rects.get(ThreadLocalRandom.current().nextInt(rects.size()));
    }

    private boolean isInsideRect(Location loc, Rect r) {
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= Math.min(r.minX(), r.maxX()) && x <= Math.max(r.minX(), r.maxX()) &&
                y >= Math.min(r.minY(), r.maxY()) && y <= Math.max(r.minY(), r.maxY()) &&
                z >= Math.min(r.minZ(), r.maxZ()) && z <= Math.max(r.minZ(), r.maxZ());
    }

    private RegionSpec resolveAreaFromOption(SpawnPointsConfig.LocationOption option, World world) {
        double centerX = world.getWorldBorder().getCenter().getX();
        double centerZ = world.getWorldBorder().getCenter().getZ();

        double minX;
        double maxX;
        double minZ;
        double maxZ;

        if (option.x == null) {
            minX = centerX - 32.0; maxX = centerX + 32.0;
        } else if (option.x.isValue()) {
            minX = option.x.value; maxX = option.x.value;
        } else {
            minX = option.x.min; maxX = option.x.max;
        }

        if (option.z == null) {
            minZ = centerZ - 32.0; maxZ = centerZ + 32.0;
        } else if (option.z.isValue()) {
            minZ = option.z.value; maxZ = option.z.value;
        } else {
            minZ = option.z.min; maxZ = option.z.max;
        }

        double minY = resolveWorldMinY(world);
        double maxY = world.getMaxHeight();
        if (option.y != null) {
            if (option.y.isValue()) {
                minY = option.y.value; maxY = option.y.value;
            } else {
                minY = option.y.min; maxY = option.y.max;
            }
        }

        return new RegionSpec(minX, maxX, minY, maxY, minZ, maxZ);
    }

    private Location resolveNonSafeLocation(SpawnPointsConfig.LocationOption option) {
        World world = Bukkit.getWorld(option.world);
        if (world == null) return null;

       // Always use rects under the hood: if no rects provided explicitly, map xyz -> single rect
        List<Rect> rects = toRects(option.rects, world);
        if (rects.isEmpty()) {
            rects = List.of(rectFromAxesOption(option, world));
        }
        List<Rect> exclude = toRects(option.excludeRects, world);

        Rect r = pickRect(rects);
        if (r == null) return null;

        // Try up to N samples inside rect
        for (int i = 0; i < 16; i++) {
            double minX = Math.min(r.minX(), r.maxX());
            double maxX = Math.max(r.minX(), r.maxX());
            double minZ = Math.min(r.minZ(), r.maxZ());
            double maxZ = Math.max(r.minZ(), r.maxZ());

            // If the bound equals origin, pick a fixed coordinate to avoid IllegalArgumentException
            double x = (minX == maxX)
                    ? minX
                    : ThreadLocalRandom.current().nextDouble(minX, Math.nextUp(maxX));
            double z = (minZ == maxZ)
                    ? minZ
                    : ThreadLocalRandom.current().nextDouble(minZ, Math.nextUp(maxZ));

            double y;
            if (r.minY() == r.maxY()) {
                y = r.minY();
            } else {
                double dy = Math.max(1.0, (r.maxY() - r.minY()));
                y = r.minY() + ThreadLocalRandom.current().nextDouble(dy);
            }

            float yaw = computeYaw(option);
            float pitch = (float) clampPitch(computePitch(option));
            Location loc = new Location(world, x, y, z, yaw, pitch);

            if (!exclude.isEmpty()) {
                boolean insideEx = false;
                for (Rect ex : exclude) {
                    if (isInsideRect(loc, ex)) { insideEx = true; break; }
                }
                if (insideEx) continue;
            }
            return loc;
        }
        return null;
    }

    private Set<Material> toMaterialSet(List<String> names) {
        if (names == null || names.isEmpty()) return Collections.emptySet();
        Set<Material> set = new HashSet<>();
        for (String n : names) {
            Material m = Material.matchMaterial(n);
            if (m != null) set.add(m);
            else plugin.getLogger().warning("Unknown material in groundWhitelist: " + n);
        }
        return set;
    }

    private float computeYaw(SpawnPointsConfig.LocationOption option) {
        if (option.yaw == null) return 0.0f;
        if (option.yaw.isValue()) return option.yaw.value.floatValue();
        double d = option.yaw.min + ThreadLocalRandom.current().nextDouble() * (option.yaw.max - option.yaw.min);
        return (float) d;
    }

    private float computePitch(SpawnPointsConfig.LocationOption option) {
        if (option.pitch == null) return 0.0f;
        if (option.pitch.isValue()) return option.pitch.value.floatValue();
        double d = option.pitch.min + ThreadLocalRandom.current().nextDouble() * (option.pitch.max - option.pitch.min);
        return (float) d;
    }

    private boolean isFixedPointOption(SpawnPointsConfig.LocationOption option) {
        return option.x != null && option.z != null
                && option.x.isValue() && option.z.isValue()
                && (option.y == null || option.y.isValue());
    }

    private void runPhaseForEntry(Player player, SpawnPointsConfig.LocationOption selected,
                                  SpawnPointsConfig.ActionsConfig globalActions,
                                  SpawnPointsConfig.Phase phase) {
        String mode = selected.actionExecutionMode == null ? "before" : selected.actionExecutionMode.toLowerCase(Locale.ROOT);
        switch (mode) {
            case "before" -> {
                runPhaseForActions(player, selected.actions, phase);
                runPhaseForActions(player, globalActions, phase);
            }
            case "after" -> {
                runPhaseForActions(player, globalActions, phase);
                runPhaseForActions(player, selected.actions, phase);
            }
            case "instead" -> runPhaseForActions(player, selected.actions, phase);
            default -> {
                runPhaseForActions(player, selected.actions, phase);
                runPhaseForActions(player, globalActions, phase);
            }
        }
    }

    private void runPhaseForActions(Player player, SpawnPointsConfig.ActionsConfig actions,
                                    SpawnPointsConfig.Phase phase) {
        if (actions == null) return;

        if (actions.messages != null) {
            for (SpawnPointsConfig.MessageEntry msg : actions.messages) {
                List<SpawnPointsConfig.Phase> phases = (msg.phases == null || msg.phases.isEmpty())
                        ? List.of(SpawnPointsConfig.Phase.AFTER)
                        : msg.phases;
                if (!phases.contains(phase)) continue;

                int chance = getEffectiveMessageChance(player, msg);
                if (chance >= 100 || ThreadLocalRandom.current().nextInt(100) < chance) {
                    if (msg.text != null && !msg.text.isEmpty()) {
                        plugin.getMessageManager().sendMessage(player, processPlaceholders(player, msg.text));
                    }
                }
            }
        }

        if (actions.commands != null) {
            for (SpawnPointsConfig.CommandActionEntry cmd : actions.commands) {
                List<SpawnPointsConfig.Phase> phases = (cmd.phases == null || cmd.phases.isEmpty())
                        ? List.of(SpawnPointsConfig.Phase.AFTER)
                        : cmd.phases;

                if (!phases.contains(phase)) continue;

                int chance = getEffectiveCommandChance(player, cmd);
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Checking command for " + player.getName() +
                            " with chance: " + chance + ", phase: " + phase);
                }

                if (chance >= 100 || ThreadLocalRandom.current().nextInt(100) < chance) {
                    executeCommand(player, cmd.command);
                }
            }
        }
    }

    private int getEffectiveMessageChance(Player player, SpawnPointsConfig.MessageEntry message) {
        int chance = message.chance;
        if (message.chanceConditions != null) {
            for (SpawnPointsConfig.ChanceConditionEntry condition : message.chanceConditions) {
                boolean match = switch (condition.type) {
                    case "permission" -> PlaceholderUtils.evaluatePermissionExpression(player, condition.value, player.isOp() || player.hasPermission("*"));
                    case "placeholder" -> plugin.isPlaceholderAPIEnabled() && PlaceholderUtils.checkPlaceholderCondition(player, condition.value);
                    default -> false;
                };
                if (!match) continue;

                String mode = normMode(condition.mode);
                switch (mode) {
                    case "set" -> chance = clampChance(condition.weight);
                    case "add" -> chance = clampChance(chance + condition.weight);
                    case "mul" -> chance = clampChance((int) Math.round(chance * (double) condition.weight));
                }
            }
        }
        return chance;
    }

    private int getEffectiveCommandChance(Player player, SpawnPointsConfig.CommandActionEntry command) {
        int chance = command.chance;
        if (command.chanceConditions != null) {
            for (SpawnPointsConfig.ChanceConditionEntry condition : command.chanceConditions) {
                boolean match = switch (condition.type) {
                    case "permission" -> PlaceholderUtils.evaluatePermissionExpression(player, condition.value, player.isOp() || player.hasPermission("*"));
                    case "placeholder" -> plugin.isPlaceholderAPIEnabled() && PlaceholderUtils.checkPlaceholderCondition(player, condition.value);
                    default -> false;
                };
                if (!match) continue;

                String mode = normMode(condition.mode);
                switch (mode) {
                    case "set" -> chance = clampChance(condition.weight);
                    case "add" -> chance = clampChance(chance + condition.weight);
                    case "mul" -> chance = clampChance((int) Math.round(chance * (double) condition.weight));
                }
            }
        }
        return chance;
    }

    private static String normMode(String mode) {
        if (mode == null) return "set";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "set", "add", "mul" -> mode.toLowerCase(Locale.ROOT);
            default -> "set";
        };
    }

    private static int clampChance(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static int clampWeight(int v) {
        return Math.max(1, v);
    }

    private void executeCommand(Player player, String command) {
        if (command == null || command.isEmpty()) return;

        String safeName = SecurityUtils.sanitize(player.getName(), SecurityUtils.SanitizeType.PLAYER_NAME);

        String processedCommand = processPlaceholders(player, command.replace("%player%", safeName));
        plugin.getRunner().runDelayed(() -> {
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Executing command: " + processedCommand);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        }, 1L);
    }

    private String processPlaceholders(Player player, String text) {
        if (plugin.isPlaceholderAPIEnabled()) {
            return PlaceholderUtils.setPlaceholders(player, text);
        }
        return text;
    }

    private void teleportPlayerWithDelay(Player player, Location location, String eventType) {
        int delayTicks = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;

        Runnable afterTeleport = () -> {
            PendingAfter pending = pendingAfterActions.remove(player.getUniqueId());
            if (pending != null) {
                runPhaseForEntry(player, pending.loc, pending.global, SpawnPointsConfig.Phase.AFTER);
            }
            sendTeleportMessage(player, eventType);
        };

        if (delayTicks <= 1) {
            PaperLib.teleportAsync(player, location).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) afterTeleport.run();
            });
        } else {
            plugin.getRunner().runDelayed(() -> {
                if (player.isOnline()) {
                    PaperLib.teleportAsync(player, location).thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) afterTeleport.run();
                    });
                }
            }, delayTicks);
        }
    }

    private void sendTeleportMessage(Player player, String eventType) {
        String message = "";
        if ("join".equals(eventType)) {
            message = plugin.getConfigManager().getMessagesConfig().joins.teleportedOnJoin;
        }
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, message);
        }
    }

    private Location getBestWaitingRoom(SpawnPointsConfig.WaitingRoomConfig local, SpawnPointsConfig.WaitingRoomConfig entry) {
        SpawnPointsConfig.WaitingRoomConfig target = (local != null) ? local : entry;
        if (target == null) target = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;

        World world = Bukkit.getWorld(target.world);
        if (world == null) {
            plugin.getLogger().warning("Waiting room world not found: " + target.world + " — falling back to default world spawn.");
            if (Bukkit.getWorlds().isEmpty()) return null;
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        return new Location(world, target.x, target.y, target.z, target.yaw, target.pitch);
    }

    private String locationToString(Location loc) {
        if (loc == null) return "null";
        return "World: " + loc.getWorld().getName() +
                ", X: " + String.format("%.2f", loc.getX()) +
                ", Y: " + String.format("%.2f", loc.getY()) +
                ", Z: " + String.format("%.2f", loc.getZ());
    }

    private double clampPitch(double pitch) {
        if (pitch < -90.0) return -90.0;
        if (pitch > 90.0) return 90.0;
        return pitch;
    }
}