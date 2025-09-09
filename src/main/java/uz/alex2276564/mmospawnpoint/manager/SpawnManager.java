package uz.alex2276564.mmospawnpoint.manager;

import io.papermc.lib.PaperLib;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfig;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;
import uz.alex2276564.mmospawnpoint.utils.SecurityUtils;
import uz.alex2276564.mmospawnpoint.utils.runner.TaskHandle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * SpawnManager (Runner + Folia-safe)
 *
 * - Uses your Runner API for all scheduling/teleports
 * - Paper path: multiple attempts per tick within a time budget (main thread)
 * - Folia path: exactly one attempt per tick scheduled on the correct region thread via runAtLocation
 * - Waiting room when requireSafe=true
 * - Chunk-aware area sampling (each attempt stays within a single chunk)
 * - Clean cancellation and player cleanup
 */
public class SpawnManager {

    private final MMOSpawnPoint plugin;

    // Death locations are accessed from event threads; keep it concurrent
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    // AFTER-phase execution state for non-waiting-room flows
    private record PendingAfter(SpawnPointsConfig.LocationOption loc, SpawnPointsConfig.ActionsConfig global) {}
    private final Map<UUID, PendingAfter> pendingAfterActions = new ConcurrentHashMap<>();

    // WAITING_ROOM-phase pending state
    private record PendingWR(SpawnPointsConfig.LocationOption loc, SpawnPointsConfig.ActionsConfig global) {}
    private final Map<UUID, PendingWR> pendingWaitingRoomActions = new ConcurrentHashMap<>();

    // Track active batch jobs per player (waiting-room async search)
    private final Map<UUID, BatchJob> activeBatchJobs = new ConcurrentHashMap<>();

    // Batch safe-search parameters (from config)
    private final int attemptsPerTick;
    private final long timeBudgetNs;

    @Setter
    private PartyManager partyManager;

    public SpawnManager(MMOSpawnPoint plugin) {
        this.plugin = plugin;
        var batch = plugin.getConfigManager().getMainConfig().settings.safeSearchBatch;
        this.attemptsPerTick = Math.max(10, batch.attemptsPerTick);
        this.timeBudgetNs = Math.max(1, batch.timeBudgetMillis) * 1_000_000L;
    }

    // ========== Lifecycle / housekeeping ==========

    public void cleanup() {
        for (BatchJob job : activeBatchJobs.values()) {
            try {
                job.cancel();
            } catch (Exception ignored) {}
        }
        activeBatchJobs.clear();
        pendingAfterActions.clear();
        deathLocations.clear();
    }

    public void cleanupPlayerData(UUID playerId) {
        try {
            deathLocations.remove(playerId);
            cancelBatchJob(playerId);
            pendingAfterActions.remove(playerId);
            if (isDebug()) {
                plugin.getLogger().info("Cleaned up spawn manager data for player: " + playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up spawn manager data for " + playerId + ": " + e.getMessage());
        }
    }

    public void recordDeathLocation(Player player, Location location) {
        // Keep the first death location only until it is consumed at respawn
        deathLocations.putIfAbsent(player.getUniqueId(), location.clone());
        if (isDebug()) {
            plugin.getLogger().info("Recorded death location for " + player.getName() + ": " + locationToString(location));
        }
    }

    // ========== Entry points (join/death flows) ==========

    public boolean processJoinSpawn(Player player) {
        try {
            if (isDebug()) {
                plugin.getLogger().info("Processing join spawn for " + player.getName());
            }

            // Party join scoped
            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled
                    && partyManager != null
                    && ("join".equalsIgnoreCase(partyScope) || "both".equalsIgnoreCase(partyScope))) {

                Location partyLocation = partyManager.findPartyJoinLocation(player);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    teleportPlayerWithDelay(player, partyLocation, "join");
                    return true;
                }
            }

            // Normal rules
            Location joinLocation = findSpawnLocationByPriority("join", player.getLocation(), player);
            if (joinLocation != null) {
                teleportPlayerWithDelay(player, joinLocation, "join");
                return true;
            }

            if (isDebug()) {
                plugin.getLogger().info("No join spawn location found for " + player.getName());
            }
            plugin.getMessageManager().sendMessage(player, plugin.getConfigManager().getMessagesConfig().general.noSpawnFound);
            return false;

        } catch (Exception e) {
            plugin.getLogger().severe("Error processing join spawn for " + player.getName() + ": " + e.getMessage());
            if (isDebug()) e.printStackTrace();
            return false;
        }
    }

    public boolean processDeathSpawn(Player player) {
        try {
            Location deathLocation = deathLocations.remove(player.getUniqueId());
            if (deathLocation == null) {
                if (isDebug()) {
                    plugin.getLogger().info("No death location found for " + player.getName() + ", using server default");
                }
                return false;
            }

            if (isDebug()) {
                plugin.getLogger().info("Processing death spawn for " + player.getName()
                        + " who died at " + locationToString(deathLocation));
            }

            // Party death scoped
            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled
                    && partyManager != null
                    && ("death".equalsIgnoreCase(partyScope) || "both".equalsIgnoreCase(partyScope))) {

                Location partyLocation = partyManager.findPartyRespawnLocation(player, deathLocation);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    if (isDebug()) {
                        plugin.getLogger().info("Using party respawn location for "
                                + player.getName() + ": " + locationToString(partyLocation));
                    }
                    teleportPlayerWithDelay(player, partyLocation, "death");
                    return true;
                }
            }

            // Normal rules
            Location spawnLocation = findSpawnLocationByPriority("death", deathLocation, player);
            if (spawnLocation != null) {
                teleportPlayerWithDelay(player, spawnLocation, "death");
                return true;
            }

            if (isDebug()) {
                plugin.getLogger().warning("No death spawn location found for " + player.getName());
            }
            plugin.getMessageManager().sendMessage(player, plugin.getConfigManager().getMessagesConfig().general.noSpawnFound);
            return false;

        } catch (Exception e) {
            plugin.getLogger().severe("Error processing death spawn for "
                    + player.getName() + ": " + e.getMessage());
            if (isDebug()) e.printStackTrace();
            return false;
        }
    }

    // ========== Core resolution flow ==========

    /**
     * Returns final location or waiting-room location if requireSafe=true (async search will continue).
     */
    public Location findSpawnLocationByPriority(String eventType, Location referenceLocation, Player player) {
        List<SpawnEntry> matchingEntries = plugin.getConfigManager().getMatchingSpawnEntries(eventType, referenceLocation);

        if (isDebug()) {
            plugin.getLogger().info("Found " + matchingEntries.size()
                    + " matching spawn entries for " + eventType);
        }

        for (SpawnEntry entry : matchingEntries) {
            if (isDebug()) {
                plugin.getLogger().info("Checking spawn entry with priority "
                        + entry.calculatedPriority() + " from " + entry.fileName());
            }

            Location spawnLocation = processSpawnEntry(entry, player, eventType);
            if (spawnLocation != null) {
                if (isDebug()) {
                    plugin.getLogger().info("Selected spawn entry with priority "
                            + entry.calculatedPriority() + " from " + entry.fileName());
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

    /**
     * Processes one spawn entry:
     * - run BEFORE/WAITING_ROOM actions as needed
     * - if requireSafe=true => start async search job and return waiting-room loc
     * - else => return final (possibly unsafe) location, and remember AFTER phase
     */
    private Location processEntry(
            Player player,
            SpawnPointsConfig.ConditionsConfig conditions,
            List<SpawnPointsConfig.LocationOption> destinations,
            SpawnPointsConfig.ActionsConfig globalActions,
            SpawnPointsConfig.WaitingRoomConfig entryWaitingRoom,
            String eventType
    ) {
        if (isDebug()) {
            plugin.getLogger().info("processEntry eventType=" + eventType);
        }

        if (conditionsNotMet(player, conditions)) {
            return null;
        }

        // Actions-only entry (no teleport)
        if (destinations == null || destinations.isEmpty()) {
            runPhaseForActions(player, globalActions, SpawnPointsConfig.Phase.AFTER);
            return null;
        }

        SpawnPointsConfig.LocationOption selected = selectDestination(player, destinations);
        if (selected == null) return null;

        boolean requireSafe = selected.requireSafe;
        boolean useWaitingRoom = plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled && requireSafe;

        // NEW: is this a weighted region scenario? (more than one destination option)
        boolean regionWeighted = destinations.size() > 1;

        if (useWaitingRoom) {
            // BEFORE strictly before any teleport
            runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);

            // Defer WAITING_ROOM phase until the player is actually in the waiting room
            pendingWaitingRoomActions.put(player.getUniqueId(), new PendingWR(selected, globalActions));

            // Start async safe search (now with regionWeighted flag)
            long enteredMs = System.currentTimeMillis();
            startBatchedLocationSearchForSelected(player, selected, globalActions, enteredMs, regionWeighted);

            // If death + useSetRespawn, schedule WAITING_ROOM one tick later (server will teleport after event)
            boolean useSetRespawn = plugin.getConfigManager().getMainConfig().settings.teleport.useSetRespawnLocationForDeath;
            if ("death".equalsIgnoreCase(eventType) && useSetRespawn) {
                plugin.getRunner().runGlobalLater(() -> runWaitingRoomPhaseIfPending(player), 1L);
            }

            // Waiting room location
            return getBestWaitingRoom(selected.waitingRoom, entryWaitingRoom);
        }

        // Non-safe destination → immediate final
        runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);

        Location finalLoc = resolveNonSafeLocation(selected);
        if (finalLoc == null) return null;

        // AFTER runs after final teleport
        pendingAfterActions.put(player.getUniqueId(), new PendingAfter(selected, globalActions));
        return finalLoc;
    }

    /**
     * Run WAITING_ROOM phase once (if pending). Returns true if executed.
     */
    private boolean runWaitingRoomPhaseIfPending(Player player) {
        PendingWR wr = pendingWaitingRoomActions.remove(player.getUniqueId());
        if (wr == null) return false;
        // Safety: ensure player is still online
        if (player.isOnline()) {
            runPhaseForEntry(player, wr.loc, wr.global, SpawnPointsConfig.Phase.WAITING_ROOM);
        }
        return true;
    }

    // ========== Destination selection / conditions ==========

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
                    case "placeholder" ->
                            plugin.isPlaceholderAPIEnabled() && PlaceholderUtils.checkPlaceholderCondition(player, cond.value);
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

    // ========== Waiting-room: async safe-location search job ==========

    private void startBatchedLocationSearchForSelected(Player player,
                                                       SpawnPointsConfig.LocationOption selected,
                                                       SpawnPointsConfig.ActionsConfig globalActions,
                                                       long enteredWaitingMs,
                                                       boolean regionWeighted) {
        UUID playerId = player.getUniqueId();
        cancelBatchJob(playerId);

        BatchJob job = new BatchJob(player, selected, globalActions, enteredWaitingMs, regionWeighted);
        activeBatchJobs.put(playerId, job);
        job.start();
    }

    private void cancelBatchJob(UUID playerId) {
        BatchJob prev = activeBatchJobs.remove(playerId);
        if (prev != null) {
            prev.cancel();
        }
    }

    /**
     * One async search job per player while they are in the waiting room.
     *
     * Paper:
     *  - Multiple attempts per tick on the main thread within timeBudgetNs
     * Folia:
     *  - Exactly one attempt per tick; we schedule that attempt on the region owning the candidate location (runAtLocation)
     */
    private final class BatchJob {

        final Player player;
        final UUID playerId;
        final SpawnPointsConfig.LocationOption option;
        final SpawnPointsConfig.ActionsConfig globalActions;
        final long waitingEnteredAtMs;

        // Chunk-loading state (used by attemptInRect)
        int pendingChunkX = Integer.MIN_VALUE;
        int pendingChunkZ = Integer.MIN_VALUE;
        boolean waitingChunkLoad = false;

        final World world;
        final boolean isFixedPoint;
        int currentRadius;
        int attemptsDone;

        // cache profile
        final boolean cacheEnabled;
        final boolean cachePlayerSpecific;
        final String cacheTypeTag;

        // Debug counters (optional)
        int failFeet = 0;
        int failHead = 0;
        int failGroundNotSolid = 0;
        int failGroundBlacklisted = 0;
        int failGroundNotWhitelisted = 0;

        // Runner task handle
        private TaskHandle handle;
        private volatile boolean attemptInProgress = false;

        // Precomputed include/exclude rects for area search
        final List<Rect> includeRects;
        final List<Rect> excludeRects;

        BatchJob(Player p,
                 SpawnPointsConfig.LocationOption option,
                 SpawnPointsConfig.ActionsConfig globalActions,
                 long waitingEnteredAtMs,
                 boolean regionWeighted) {
            this.player = p;
            this.playerId = p.getUniqueId();
            this.option = option;
            this.globalActions = globalActions;
            this.waitingEnteredAtMs = waitingEnteredAtMs;

            this.world = Bukkit.getWorld(option.world);
            this.isFixedPoint = isFixedPointOption(option);
            this.currentRadius = plugin.getConfigManager().getMainConfig().settings.safeLocationRadius;
            this.attemptsDone = 0;

            this.includeRects = buildRectsForOption(option, world);
            this.excludeRects = toRects(option.excludeRects, world);

            // Determine cache profile from main config
            var cacheCfg = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching;

            boolean cEnabled;
            boolean cPlayerSpecific;
            String cTag;

            if (isFixedPoint) {
                cEnabled = cacheCfg.fixedSafe.enabled;
                cPlayerSpecific = cacheCfg.fixedSafe.playerSpecific;
                cTag = "FIXED_SAFE";
            } else {
                if (regionWeighted) {
                    cEnabled = cacheCfg.regionSafeWeighted.enabled;
                    cPlayerSpecific = cacheCfg.regionSafeWeighted.playerSpecific;
                    cTag = "REGION_SAFE_WEIGHTED";
                } else {
                    cEnabled = cacheCfg.regionSafeSingle.enabled;
                    cPlayerSpecific = cacheCfg.regionSafeSingle.playerSpecific;
                    cTag = "REGION_SAFE_SINGLE";
                }
            }

            this.cacheEnabled = cEnabled;
            this.cachePlayerSpecific = cPlayerSpecific;
            this.cacheTypeTag = cTag;
        }

        void start() {
            handle = plugin.getRunner().runGlobalTimer(this::tick, 1L, 1L);
        }

        void cancel() {
            if (handle != null) handle.cancel();
        }

        private void tick() {
            if (!player.isOnline()) { finish(null, false); return; }
            if (world == null)     { finish(null, false); return; }

            // Timeout check
            long timeoutMs = plugin.getConfigManager().getMainConfig().settings.waitingRoom.asyncSearchTimeout * 1000L;
            if (timeoutMs > 0 && System.currentTimeMillis() - waitingEnteredAtMs > timeoutMs) {
                if (isDebug()) {
                    plugin.getLogger().warning("[MMOSpawnPoint] Safe search TIMEOUT for "
                            + player.getName() + " in world=" + world.getName()
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

            if (plugin.getRunner().isFolia()) {
                // Folia: do one attempt per tick on the proper region thread
                if (attemptInProgress) return;
                attemptInProgress = true;

                // Choose candidate location (no world access on global thread)
                final Location candidateRegionLoc = chooseCandidateRegionLocation();

                plugin.getRunner().runAtLocation(candidateRegionLoc, () -> {
                    try {
                        Location found = singleAttemptInRegion(candidateRegionLoc);
                        if (found != null) {
                            plugin.getRunner().runGlobal(() -> finish(found, true));
                        }
                    } finally {
                        attemptInProgress = false;
                    }
                });

            } else {
                // Paper: burst multiple attempts within the time budget on main thread
                long endBy = System.nanoTime() + timeBudgetNs;
                int attemptsThisTick = 0;

                while (attemptsThisTick < attemptsPerTick && System.nanoTime() < endBy) {
                    attemptsThisTick++;
                    attemptsDone++;

                    Location found = singleAttemptLocal();
                    if (found != null) {
                        finish(found, true);
                        return;
                    }
                }
            }
        }

        /**
         * Choose a region location where the attempt will be executed on Folia.
         * - For fixed point: use that point (approx y=100 if not provided)
         * - For area: choose a random chunk center inside one of include rects (or axes-derived rect)
         */
        private Location chooseCandidateRegionLocation() {
            if (isFixedPoint) {
                double x = option.x.value;
                double z = option.z.value;
                double y = (option.y != null && option.y.isValue()) ? option.y.value : 100.0;
                return new Location(world, x, y, z);
            }

            Rect rect = includeRects.isEmpty() ? rectFromAxesOption(option, world) : pickRect(includeRects);

            double rxMinX = Math.min(rect.minX(), rect.maxX());
            double rxMaxX = Math.max(rect.minX(), rect.maxX());
            double rzMinZ = Math.min(rect.minZ(), rect.maxZ());
            double rzMaxZ = Math.max(rect.minZ(), rect.maxZ());

            double x = ThreadLocalRandom.current().nextDouble(rxMinX, rxMaxX);
            double z = ThreadLocalRandom.current().nextDouble(rzMinZ, rzMaxZ);

            int cx = ((int) Math.floor(x)) >> 4;
            int cz = ((int) Math.floor(z)) >> 4;

            double centerX = (cx << 4) + 8.0;
            double centerZ = (cz << 4) + 8.0;
            double centerY = (rect.minY() + rect.maxY()) / 2.0;

            return new Location(world, centerX, centerY, centerZ);
        }

        /**
         * Paper attempt: safe to read world on main thread.
         */
        private Location singleAttemptLocal() {
            if (isFixedPoint) {
                double x = option.x.value;
                double z = option.z.value;
                double y = (option.y != null && option.y.isValue())
                        ? option.y.value
                        : world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;
                Location base = new Location(world, x, y, z);

                // Ensure chunk is loaded
                if (!world.isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
                    PaperLib.getChunkAtAsync(base, true).thenRun(() -> {});
                    return null;
                }

                Set<Material> wl = toMaterialSet(option.groundWhitelist);
                String tag = buildCacheTag(wl);
                Location found = cacheEnabled
                        ? SafeLocationFinder.cachedFindSafeNear(base, currentRadius, wl, playerId, cachePlayerSpecific, true, tag)
                        : SafeLocationFinder.attemptFindSafeNearSingle(base, currentRadius, wl);

                if (found == null) {
                    bumpFailCounters();
                    adaptRadiusIfNeeded();
                    return null;
                }
                applyYawPitch(option, found);
                return found;

            } else {
                // Area path
                Rect rect = includeRects.isEmpty() ? rectFromAxesOption(option, world) : pickRect(includeRects);
                Location found = attemptInRectLocal(rect, excludeRects, toMaterialSet(option.groundWhitelist));
                if (found == null) {
                    bumpFailCounters();
                    return null;
                }
                applyYawPitch(option, found);
                return found;
            }
        }

        /**
         * Folia attempt on the correct region thread.
         * The parameter regionLoc is where this task is scheduled (must be in the same chunk as sampling).
         */
        // BatchJob method
        private Location singleAttemptInRegion(Location regionLoc) {
            if (isFixedPoint) {
                double x = option.x.value;
                double z = option.z.value;

                int cx = ((int) Math.floor(x)) >> 4;
                int cz = ((int) Math.floor(z)) >> 4;

                if (!world.isChunkLoaded(cx, cz)) {
                    double px = (cx << 4) + 8.0;
                    double pz = (cz << 4) + 8.0;
                    Location probe = new Location(world, px, regionLoc.getY(), pz);
                    PaperLib.getChunkAtAsync(probe, true).thenRun(() -> {});
                    return null;
                }

                double y = (option.y != null && option.y.isValue())
                        ? option.y.value
                        : world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;

                Location base = new Location(world, x, y, z);

                Set<Material> wl = toMaterialSet(option.groundWhitelist);
                String tag = buildCacheTag(wl);
                Location found = cacheEnabled
                        ? SafeLocationFinder.cachedFindSafeNear(base, currentRadius, wl, playerId, cachePlayerSpecific, true, tag)
                        : SafeLocationFinder.attemptFindSafeNearSingle(base, currentRadius, wl);
                if (found == null) {
                    bumpFailCounters();
                    adaptRadiusIfNeeded();
                    return null;
                }

                applyYawPitch(option, found);
                return found;
            } else {
                int cx = regionLoc.getBlockX() >> 4;
                int cz = regionLoc.getBlockZ() >> 4;

                if (!world.isChunkLoaded(cx, cz)) {
                    double px = (cx << 4) + 8.0;
                    double pz = (cz << 4) + 8.0;
                    Location probe = new Location(world, px, regionLoc.getY(), pz);
                    PaperLib.getChunkAtAsync(probe, true).thenRun(() -> {});
                    return null;
                }

                Rect rect = includeRects.isEmpty()
                        ? rectFromAxesOption(option, world)
                        : closestRectToChunkCenter(regionLoc, includeRects);

                double chunkMinX = (cx << 4);
                double chunkMaxX = chunkMinX + 15.0;
                double chunkMinZ = (cz << 4);
                double chunkMaxZ = chunkMinZ + 15.0;

                double rxMinX = Math.min(rect.minX(), rect.maxX());
                double rxMaxX = Math.max(rect.minX(), rect.maxX());
                double rzMinZ = Math.min(rect.minZ(), rect.maxZ());
                double rzMaxZ = Math.max(rect.minZ(), rect.maxZ());

                double minX = Math.max(rxMinX, chunkMinX);
                double maxX = Math.min(rxMaxX, chunkMaxX);
                double minZ = Math.max(rzMinZ, chunkMinZ);
                double maxZ = Math.min(rzMaxZ, chunkMaxZ);

                if (minX > maxX || minZ > maxZ) {
                    return null;
                }

                Set<Material> wl = toMaterialSet(option.groundWhitelist);
                String tag = buildCacheTag(wl);
                Predicate<Location> notExcluded = l -> !isInsideAny(l, excludeRects);
                Location found = cacheEnabled
                        ? SafeLocationFinder.cachedFindSafeInRegionValidated(
                        world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ,
                        wl, playerId, cachePlayerSpecific, true, tag, notExcluded)
                        : SafeLocationFinder.attemptFindSafeInRegionSingle(world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl);

                if (found == null) return null;

                if (!excludeRects.isEmpty()) {
                    for (Rect ex : excludeRects) {
                        if (isInsideRect(found, ex)) return null;
                    }
                }

                applyYawPitch(option, found);
                return found;
            }
        }

        /**
         * Paper-only area attempt with chunk awareness.
         */
        private Location attemptInRectLocal(Rect rect, List<Rect> exclude, Set<Material> wl) {
            double rxMinX = Math.min(rect.minX(), rect.maxX());
            double rxMaxX = Math.max(rect.minX(), rect.maxX());
            double rzMinZ = Math.min(rect.minZ(), rect.maxZ());
            double rzMaxZ = Math.max(rect.minZ(), rect.maxZ());

            // 1) Pending chunk already requested
            if (waitingChunkLoad) {
                if (!world.isChunkLoaded(pendingChunkX, pendingChunkZ)) {
                    return null; // still loading
                }
                waitingChunkLoad = false;

                double chunkMinX = (pendingChunkX << 4);
                double chunkMaxX = chunkMinX + 15.0;
                double chunkMinZ = (pendingChunkZ << 4);
                double chunkMaxZ = chunkMinZ + 15.0;

                double minX = Math.max(rxMinX, chunkMinX);
                double maxX = Math.min(rxMaxX, chunkMaxX);
                double minZ = Math.max(rzMinZ, chunkMinZ);
                double maxZ = Math.min(rzMaxZ, chunkMaxZ);

                pendingChunkX = Integer.MIN_VALUE;
                pendingChunkZ = Integer.MIN_VALUE;

                if (minX > maxX || minZ > maxZ) {
                    return null;
                }

                String tag = buildCacheTag(wl);
                Predicate<Location> notExcluded = l -> !isInsideAny(l, exclude);
                Location loc = cacheEnabled
                        ? SafeLocationFinder.cachedFindSafeInRegionValidated(
                        world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ,
                        wl, playerId, cachePlayerSpecific, true, tag, notExcluded)
                        : SafeLocationFinder.attemptFindSafeInRegionSingle(world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl);
                if (loc == null) return null;

                return loc;
            }

            // 2) Pick random chunk inside rect
            double x = ThreadLocalRandom.current().nextDouble(rxMinX, rxMaxX);
            double z = ThreadLocalRandom.current().nextDouble(rzMinZ, rzMaxZ);
            int cx = ((int) Math.floor(x)) >> 4;
            int cz = ((int) Math.floor(z)) >> 4;

            if (!world.isChunkLoaded(cx, cz)) {
                pendingChunkX = cx;
                pendingChunkZ = cz;
                waitingChunkLoad = true;

                double px = (cx << 4) + 8.0;
                double pz = (cz << 4) + 8.0;
                Location loadProbe = new Location(world, px, rect.minY(), pz);
                PaperLib.getChunkAtAsync(loadProbe, true).thenRun(() -> {});
                return null;
            }

            double chunkMinX = (cx << 4);
            double chunkMaxX = chunkMinX + 15.0;
            double chunkMinZ = (cz << 4);
            double chunkMaxZ = chunkMinZ + 15.0;

            double minX = Math.max(rxMinX, chunkMinX);
            double maxX = Math.min(rxMaxX, chunkMaxX);
            double minZ = Math.max(rzMinZ, chunkMinZ);
            double maxZ = Math.min(rzMaxZ, chunkMaxZ);

            if (minX > maxX || minZ > maxZ) {
                return null;
            }

            String tag = buildCacheTag(wl);
            Location loc = cacheEnabled
                    ? SafeLocationFinder.cachedFindSafeInRegion(world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ,
                    wl, playerId, cachePlayerSpecific, true, tag)
                    : SafeLocationFinder.attemptFindSafeInRegionSingle(world, minX, maxX, rect.minY(), rect.maxY(), minZ, maxZ, wl);
            if (loc == null) return null;

            if (!exclude.isEmpty()) {
                for (Rect ex : exclude) {
                    if (isInsideRect(loc, ex)) return null;
                }
            }
            return loc;
        }

        private void bumpFailCounters() {
            if (!isDebug()) return;
            var tag = SafeLocationFinder.pollLastFailTag();
            if (tag == null) return;
            switch (tag) {
                case FEET_NOT_PASSABLE -> failFeet++;
                case HEAD_NOT_PASSABLE -> failHead++;
                case GROUND_NOT_SOLID -> failGroundNotSolid++;
                case GROUND_BLACKLISTED -> failGroundBlacklisted++;
                case GROUND_NOT_WHITELISTED -> failGroundNotWhitelisted++;
            }
        }

        private void adaptRadiusIfNeeded() {
            // Expand radius occasionally if we keep failing at fixed-point safe search
            int step = Math.max(2, plugin.getConfigManager().getMainConfig().settings.maxSafeLocationAttempts / 3);
            if ((attemptsDone % step) == 0) {
                currentRadius = Math.min(
                        currentRadius * 2,
                        Math.max(currentRadius, plugin.getConfigManager().getMainConfig().settings.safeLocationRadius * 4)
                );
            }
        }

        private Rect closestRectToChunkCenter(Location chunkCenter, List<Rect> rects) {
            if (rects.isEmpty()) return rectFromAxesOption(option, world);
            Rect best = rects.get(0);
            double bx = chunkCenter.getX();
            double bz = chunkCenter.getZ();
            double bestDist = Double.MAX_VALUE;

            for (Rect r : rects) {
                double cx = (Math.min(r.minX(), r.maxX()) + Math.max(r.minX(), r.maxX())) / 2.0;
                double cz = (Math.min(r.minZ(), r.maxZ()) + Math.max(r.minZ(), r.maxZ())) / 2.0;
                double d2 = (cx - bx) * (cx - bx) + (cz - bz) * (cz - bz);
                if (d2 < bestDist) {
                    bestDist = d2;
                    best = r;
                }
            }
            return best;
        }

        /**
         * Finalize job: cancel, remove from registry, teleport with min-stay logic, run AFTER actions.
         */
        private void finish(Location found, boolean success) {
            try { cancel(); } catch (Exception ignored) {}
            activeBatchJobs.remove(playerId);

            if (!success || found == null) return;

            int delayConfig = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;
            int minStayTicks = plugin.getConfigManager().getMainConfig().settings.waitingRoom.minStayTicks;
            long elapsedMs = System.currentTimeMillis() - waitingEnteredAtMs;
            long requiredMs = Math.max(0L, minStayTicks * 50L - elapsedMs);
            int requiredTicks = (int) Math.ceil(requiredMs / 50.0);
            int finalDelay = Math.max(1, Math.max(delayConfig, requiredTicks));

            plugin.getRunner().runGlobalLater(() -> {
                if (!player.isOnline()) return;
                plugin.getRunner().teleportAsync(player, found).thenAccept(successTp -> {
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

            // If it's a fixed point — no rects (use fixed-point behavior)
            if (isFixedPointOption(option)) {
                return Collections.emptyList();
            }

            // Otherwise build a single rect from axis (xyz)
            RegionSpec area = resolveAreaFromOption(option, world);
            return List.of(new Rect(area.minX(), area.maxX(), area.minY(), area.maxY(), area.minZ(), area.maxZ()));
        }

        private String buildCacheTag(Set<Material> wl) {
            var ysel = plugin.getConfigManager().getMainConfig().settings.teleport.ySelection;
            String yMode = (ysel.mode == null) ? "mixed" : ysel.mode.toLowerCase(Locale.ROOT);
            String yFirst = (ysel.first == null) ? "highest" : ysel.first.toLowerCase(Locale.ROOT);
            double yShare = ysel.firstShare;

            // Stable hash of whitelist names (sorted)
            String wlHash;
            if (wl == null || wl.isEmpty()) {
                wlHash = "0";
            } else {
                String[] arr = wl.stream().map(Enum::name).sorted().toArray(String[]::new);
                wlHash = Integer.toHexString(Arrays.hashCode(arr));
            }
            return cacheTypeTag + "|ys=" + yMode + ":" + yFirst + ":" + yShare + "|wl=" + wlHash;
        }

        private boolean isInsideAny(Location loc, List<Rect> rects) {
            if (rects == null || rects.isEmpty() || loc == null) return false;
            for (Rect r : rects) {
                if (isInsideRect(loc, r)) return true;
            }
            return false;
        }
    }

    // ========== Helpers (outer scope) ==========

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
            return 0; // 1.16.5 and older
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

        double minX = Math.min(r.minX(), r.maxX());
        double maxX = Math.max(r.minX(), r.maxX());
        double minZ = Math.min(r.minZ(), r.maxZ());
        double maxZ = Math.max(r.minZ(), r.maxZ());

        return x >= minX && x <= maxX
                && y >= Math.min(r.minY(), r.maxY()) && y <= Math.max(r.minY(), r.maxY())
                && z >= minZ && z <= maxZ;
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

        // Always map legacy xyz axis to a single rect if rects are not provided
        List<Rect> rects = toRects(option.rects, world);
        if (rects.isEmpty()) {
            rects = List.of(rectFromAxesOption(option, world));
        }
        List<Rect> exclude = toRects(option.excludeRects, world);

        Rect r = pickRect(rects);
        if (r == null) return null;

        // Try up to 16 random samples
        double rxMinX = Math.min(r.minX(), r.maxX());
        double rxMaxX = Math.max(r.minX(), r.maxX());
        double rzMinZ = Math.min(r.minZ(), r.maxZ());
        double rzMaxZ = Math.max(r.minZ(), r.maxZ());

        for (int i = 0; i < 16; i++) {
            double x = (rxMinX == rxMaxX) ? rxMinX : ThreadLocalRandom.current().nextDouble(rxMinX, Math.nextUp(rxMaxX));
            double z = (rzMinZ == rzMaxZ) ? rzMinZ : ThreadLocalRandom.current().nextDouble(rzMinZ, Math.nextUp(rzMaxZ));

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

    private double clampPitch(double pitch) {
        if (pitch < -90.0) return -90.0;
        if (pitch > 90.0) return 90.0;
        return pitch;
    }

    // ========== Actions / commands / messaging ==========

    /**
     * Run actions with phase ordering relative to globalActions as per actionExecutionMode.
     */
    private void runPhaseForEntry(Player player,
                                  SpawnPointsConfig.LocationOption selected,
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

    private void runPhaseForActions(Player player, SpawnPointsConfig.ActionsConfig actions, SpawnPointsConfig.Phase phase) {
        if (actions == null) return;

        if (actions.messages != null) {
            for (SpawnPointsConfig.MessageEntry msg : actions.messages) {
                List<SpawnPointsConfig.Phase> phases = (msg.phases == null || msg.phases.isEmpty())
                        ? List.of(SpawnPointsConfig.Phase.AFTER)
                        : msg.phases;
                if (!phases.contains(phase)) continue;

                int chance = getEffectiveMessageChance(player, msg);
                if ((chance >= 100 || ThreadLocalRandom.current().nextInt(100) < chance)
                        && msg.text != null && !msg.text.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, processPlaceholders(player, msg.text));
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
                if (isDebug()) {
                    plugin.getLogger().info("Checking command for " + player.getName()
                            + " with chance: " + chance + ", phase: " + phase);
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
                    case "permission" ->
                            PlaceholderUtils.evaluatePermissionExpression(player, condition.value, player.isOp() || player.hasPermission("*"));
                    case "placeholder" ->
                            plugin.isPlaceholderAPIEnabled() && PlaceholderUtils.checkPlaceholderCondition(player, condition.value);
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
                    case "permission" ->
                            PlaceholderUtils.evaluatePermissionExpression(player, condition.value, player.isOp() || player.hasPermission("*"));
                    case "placeholder" ->
                            plugin.isPlaceholderAPIEnabled() && PlaceholderUtils.checkPlaceholderCondition(player, condition.value);
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

    private void executeCommand(Player player, String command) {
        if (command == null || command.isEmpty()) return;

        String safeName = SecurityUtils.sanitize(player.getName(), SecurityUtils.SanitizeType.PLAYER_NAME);
        String processedCommand = processPlaceholders(player, command.replace("%player%", safeName));

        plugin.getRunner().runGlobal(() -> {
            if (isDebug()) {
                plugin.getLogger().info("Executing command: " + processedCommand);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
        });
    }

    private void teleportPlayerWithDelay(Player player, Location location, String eventType) {
        int delayTicks = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;

        Runnable afterTeleport = () -> {
            // If this teleport was to WAITING ROOM (i.e., we have pending WR actions) — run them first
            if (runWaitingRoomPhaseIfPending(player)) {
                // WAITING_ROOM executed — do not run AFTER here; AFTER will be run later on the final destination teleport
            } else {
                // Otherwise, we might be on final destination (non-waiting flow) — run AFTER if pending
                PendingAfter pending = pendingAfterActions.remove(player.getUniqueId());
                if (pending != null) {
                    runPhaseForEntry(player, pending.loc, pending.global, SpawnPointsConfig.Phase.AFTER);
                }
            }

            // Message per event
            sendTeleportMessage(player, eventType);
        };

        if (delayTicks <= 1) {
            plugin.getRunner().teleportAsync(player, location).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) afterTeleport.run();
            });
        } else {
            plugin.getRunner().runGlobalLater(() -> {
                if (player.isOnline()) {
                    plugin.getRunner().teleportAsync(player, location).thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) afterTeleport.run();
                    });
                }
            }, delayTicks);
        }
    }

    private void sendTeleportMessage(Player player, String eventType) {
        String message = "";
        if ("join".equalsIgnoreCase(eventType)) {
            message = plugin.getConfigManager().getMessagesConfig().join.teleportedOnJoin;
        }
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessageKeyed(player, "join.teleportedOnJoin", message);
        }
    }

    private String processPlaceholders(Player player, String text) {
        return plugin.isPlaceholderAPIEnabled()
                ? PlaceholderUtils.setPlaceholders(player, text)
                : text;
    }

    // ========== Misc utils ==========

    private boolean isDebug() {
        return plugin.getConfigManager().getMainConfig().settings.debugMode;
    }

    private String locationToString(Location loc) {
        if (loc == null) return "null";
        return "World: " + loc.getWorld().getName()
                + ", X: " + String.format("%.2f", loc.getX())
                + ", Y: " + String.format("%.2f", loc.getY())
                + ", Z: " + String.format("%.2f", loc.getZ());
    }

    /**
     * Waiting room resolution priority:
     * - local (destination.waitingRoom) > entry.waitingRoom > global settings.waitingRoom
     */
    private Location getBestWaitingRoom(SpawnPointsConfig.WaitingRoomConfig local, SpawnPointsConfig.WaitingRoomConfig entry) {
        SpawnPointsConfig.WaitingRoomConfig target = (local != null) ? local : entry;
        if (target == null) target = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;

        World world = Bukkit.getWorld(target.world);
        if (world == null) {
            plugin.getLogger().warning("Waiting room world not found: " + target.world + " — falling back to default world spawn.");
            return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return new Location(world, target.x, target.y, target.z, target.yaw, target.pitch);
    }
}