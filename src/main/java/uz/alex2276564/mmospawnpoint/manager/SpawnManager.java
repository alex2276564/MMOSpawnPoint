package uz.alex2276564.mmospawnpoint.manager;

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

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SpawnManager {
    private final MMOSpawnPoint plugin;
    private final Random random = new Random();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    private record PendingAfter(SpawnPointsConfig.LocationOption loc, SpawnPointsConfig.ActionsConfig global) {
    }

    private final Map<UUID, PendingAfter> pendingAfterActions = new ConcurrentHashMap<>();

    private record SearchProcess(SpawnPointsConfig.LocationOption selected,
                                 SpawnPointsConfig.ActionsConfig globalActions,
                                 CompletableFuture<Location> future,
                                 long waitingEnteredAtMs) {
    }

    private final Map<UUID, SearchProcess> searchProcesses = new ConcurrentHashMap<>();

    @Setter
    private PartyManager partyManager;

    public SpawnManager(MMOSpawnPoint plugin) {
        this.plugin = plugin;
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
            SearchProcess sp = searchProcesses.remove(playerId);
            if (sp != null) {
                try {
                    sp.future.cancel(true);
                } catch (Exception ignored) {
                }
            }
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
                    partyManager != null && ("joins".equals(partyScope) || "both".equals(partyScope))) {

                Location partyLocation = partyManager.findPartyJoinLocation(player);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    teleportPlayerWithDelay(player, partyLocation, "join");
                    return true;
                }
            }

            Location joinLocation = findSpawnLocationByPriority("joins", player.getLocation(), player);
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
                    partyManager != null && ("deaths".equals(partyScope) || "both".equals(partyScope))) {

                Location partyLocation = partyManager.findPartyRespawnLocation(player, deathLocation);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                        plugin.getLogger().info("Using party respawn location for " + player.getName() + ": " + locationToString(partyLocation));
                    }
                    teleportPlayerWithDelay(player, partyLocation, "death");
                    return true;
                }
            }

            Location spawnLocation = findSpawnLocationByPriority("deaths", deathLocation, player);
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

    private Location findSpawnLocationByPriority(String eventType, Location referenceLocation, Player player) {
        List<SpawnEntry> matchingEntries = plugin.getConfigManager().getMatchingSpawnEntries(eventType, referenceLocation);

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Found " + matchingEntries.size() + " matching spawn entries for " + eventType);
        }

        for (SpawnEntry entry : matchingEntries) {
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Checking spawn entry with priority " + entry.calculatedPriority() +
                        " from " + entry.fileName());
            }

            Location spawnLocation = processSpawnEntry(entry, player);
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

    private Location processSpawnEntry(SpawnEntry entry, Player player) {
        SpawnPointsConfig.SpawnPointEntry data = entry.spawnData();
        return processEntry(player, data.conditions, data.destinations, data.actions, data.waitingRoom);
    }

    private Location processEntry(
            Player player,
            SpawnPointsConfig.ConditionsConfig conditions,
            List<SpawnPointsConfig.LocationOption> destinations,
            SpawnPointsConfig.ActionsConfig globalActions,
            SpawnPointsConfig.WaitingRoomConfig entryWaitingRoom
    ) {
        if (conditionsNotMet(player, conditions)) {
            return null;
        }

        if (destinations == null || destinations.isEmpty()) {
            runPhaseForActions(player, globalActions, SpawnPointsConfig.Phase.AFTER);
            return null;
        }

        boolean isWeightedSelection = destinations.size() > 1;
        SpawnPointsConfig.LocationOption selected = selectDestination(player, destinations);
        if (selected == null) return null;

        boolean useWaitingRoom = plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled && selected.requireSafe;

        if (useWaitingRoom) {
            runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);
            runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.WAITING_ROOM);

            long enteredMs = System.currentTimeMillis();
            startAsyncLocationSearchForSelected(player, selected, globalActions, isWeightedSelection, enteredMs);

            return getBestWaitingRoom(selected.waitingRoom, entryWaitingRoom);
        }

        runPhaseForEntry(player, selected, globalActions, SpawnPointsConfig.Phase.BEFORE);

        Location finalLoc = resolveLocationFromOption(player, selected, isWeightedSelection);
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

        int rnd = random.nextInt(total);
        int acc = 0;
        for (SpawnPointsConfig.LocationOption opt : options) {
            acc += getEffectiveWeight(player, opt);
            if (rnd < acc) return opt;
        }
        return options.get(0);
    }

    private int getEffectiveWeight(Player player, SpawnPointsConfig.LocationOption option) {
        if (option.weightConditions == null || option.weightConditions.isEmpty()) {
            return option.weight;
        }
        for (SpawnPointsConfig.WeightConditionEntry cond : option.weightConditions) {
            if (checkWeightCondition(player, cond)) {
                return cond.weight;
            }
        }
        return option.weight;
    }

    private boolean checkWeightCondition(Player player, SpawnPointsConfig.WeightConditionEntry cond) {
        return switch (cond.type) {
            case "permission" -> player.hasPermission(cond.value);
            case "placeholder" -> plugin.isPlaceholderAPIEnabled() &&
                    PlaceholderUtils.checkPlaceholderCondition(player, cond.value);
            default -> false;
        };
    }

    private void startAsyncLocationSearchForSelected(Player player,
                                                     SpawnPointsConfig.LocationOption selected,
                                                     SpawnPointsConfig.ActionsConfig globalActions,
                                                     boolean isWeightedSelection,
                                                     long enteredWaitingMs) {
        UUID playerId = player.getUniqueId();

        SearchProcess prev = searchProcesses.remove(playerId);
        if (prev != null) {
            try {
                prev.future.cancel(true);
            } catch (Exception ignored) {
            }
        }

        // Run the heavy/async work on Bukkit async scheduler instead of commonPool
        CompletableFuture<Location> future = new CompletableFuture<>();
        plugin.getRunner().runAsync(() -> {
            try {
                Location loc = resolveLocationFromOption(player, selected, isWeightedSelection);
                future.complete(loc);
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            }
        });

        SearchProcess sp = new SearchProcess(selected, globalActions, future, enteredWaitingMs);
        searchProcesses.put(playerId, sp);

        future.orTimeout(plugin.getConfigManager().getMainConfig().settings.waitingRoom.asyncSearchTimeout, TimeUnit.SECONDS)
                .whenComplete((location, ex) -> {
                    SearchProcess current = searchProcesses.remove(playerId);
                    if (current == null) return;

                    if (ex != null) {
                        if (!(ex instanceof CancellationException) && plugin.getConfigManager().getMainConfig().settings.debugMode) {
                            plugin.getLogger().warning("Async location search failed: " + ex.getMessage());
                        }
                        return;
                    }

                    if (location != null && player.isOnline() && !player.isDead()) {
                        int delayConfig = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;
                        int minStayTicks = plugin.getConfigManager().getMainConfig().settings.waitingRoom.minStayTicks;

                        long elapsedMs = System.currentTimeMillis() - current.waitingEnteredAtMs;
                        long requiredMs = Math.max(0L, minStayTicks * 50L - elapsedMs);
                        int requiredTicks = (int) Math.ceil(requiredMs / 50.0);

                        int finalDelay = Math.max(1, Math.max(delayConfig, requiredTicks));

                        plugin.getRunner().runDelayed(() -> {
                            if (!player.isOnline()) return;
                            player.teleport(location);
                            runPhaseForEntry(player, current.selected, current.globalActions, SpawnPointsConfig.Phase.AFTER);
                        }, finalDelay);
                    }
                });
    }

    private Location resolveLocationFromOption(Player player, SpawnPointsConfig.LocationOption option, boolean isWeightedSelection) {
        World world = Bukkit.getWorld(option.world);
        if (world == null) return null;

        var cacheConfig = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching;
        int maxAttempts = plugin.getConfigManager().getMainConfig().settings.maxSafeLocationAttempts;

        Set<Material> groundWhitelist = toMaterialSet(option.groundWhitelist);

        if (option.requireSafe) {
            if (isFixedPointOption(option)) {
                double x = option.x.value;
                double z = option.z.value;
                double y;
                if (option.y != null && option.y.isValue()) {
                    y = option.y.value;
                } else {
                    int hx = (int) Math.floor(x);
                    int hz = (int) Math.floor(z);
                    y = world.getHighestBlockYAt(hx, hz) + 1.0;
                }

                Location base = new Location(world, x, y, z);

                boolean useCache = cacheConfig.fixedSafe.enabled;
                boolean playerSpecific = cacheConfig.fixedSafe.playerSpecific;

                boolean useWhitelist = !groundWhitelist.isEmpty();
                Location safe = useWhitelist
                        ? SafeLocationFinder.findSafeLocationWithWhitelist(base, maxAttempts, player.getUniqueId(), useCache, playerSpecific, groundWhitelist)
                        : SafeLocationFinder.findSafeLocation(base, maxAttempts, player.getUniqueId(), useCache, playerSpecific);

                if (safe == null) return null;

                applyYawPitch(option, safe);
                return safe;
            }

            double centerX = world.getWorldBorder().getCenter().getX();
            double centerZ = world.getWorldBorder().getCenter().getZ();

            double minX;
            double maxX;
            double minZ;
            double maxZ;
            if (option.x == null) {
                minX = centerX - 32.0;
                maxX = centerX + 32.0;
            } else if (option.x.isValue()) {
                minX = option.x.value;
                maxX = option.x.value;
            } else {
                minX = option.x.min;
                maxX = option.x.max;
            }

            if (option.z == null) {
                minZ = centerZ - 32.0;
                maxZ = centerZ + 32.0;
            } else if (option.z.isValue()) {
                minZ = option.z.value;
                maxZ = option.z.value;
            } else {
                minZ = option.z.min;
                maxZ = option.z.max;
            }

            double minY = 0.0;
            double maxY = world.getMaxHeight();
            if (option.y != null) {
                if (option.y.isValue()) {
                    minY = option.y.value;
                    maxY = option.y.value;
                } else {
                    minY = option.y.min;
                    maxY = option.y.max;
                }
            }

            boolean useCache;
            boolean playerSpecific;
            if (isWeightedSelection) {
                useCache = cacheConfig.regionSafeWeighted.enabled;
                playerSpecific = cacheConfig.regionSafeWeighted.playerSpecific;
            } else {
                useCache = cacheConfig.regionSafeSingle.enabled;
                playerSpecific = cacheConfig.regionSafeSingle.playerSpecific;
            }

            boolean useWhitelist = !groundWhitelist.isEmpty();
            Location safe = useWhitelist
                    ? SafeLocationFinder.findSafeLocationInRegionWithWhitelist(minX, maxX, minY, maxY, minZ, maxZ,
                    world, maxAttempts, player.getUniqueId(), useCache, playerSpecific, groundWhitelist)
                    : SafeLocationFinder.findSafeLocationInRegion(minX, maxX, minY, maxY, minZ, maxZ,
                    world, maxAttempts, player.getUniqueId(), useCache, playerSpecific);

            if (safe == null) return null;

            applyYawPitch(option, safe);
            return safe;

        } else {
            if (option.y == null) return null;

            double x = option.x.isValue() ? option.x.value : option.x.min + random.nextDouble() * (option.x.max - option.x.min);
            double y = option.y.isValue() ? option.y.value : option.y.min + random.nextDouble() * (option.y.max - option.y.min);
            double z = option.z.isValue() ? option.z.value : option.z.min + random.nextDouble() * (option.z.max - option.z.min);

            float yaw = computeYaw(option);
            float pitch = computePitch(option);
            pitch = (float) clampPitch(pitch);

            return new Location(world, x, y, z, yaw, pitch);
        }
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
        double d = option.yaw.min + random.nextDouble() * (option.yaw.max - option.yaw.min);
        return (float) d;
    }

    private float computePitch(SpawnPointsConfig.LocationOption option) {
        if (option.pitch == null) return 0.0f;
        if (option.pitch.isValue()) return option.pitch.value.floatValue();
        double d = option.pitch.min + random.nextDouble() * (option.pitch.max - option.pitch.min);
        return (float) d;
    }

    private boolean isFixedPointOption(SpawnPointsConfig.LocationOption option) {
        return option.x != null && option.z != null
                && option.x.isValue() && option.z.isValue()
                && (option.y == null || option.y.isValue());
    }

    private void applyYawPitch(SpawnPointsConfig.LocationOption option, Location loc) {
        float yaw = (option.yaw == null) ? loc.getYaw()
                : option.yaw.isValue() ? option.yaw.value.floatValue()
                : (float) (option.yaw.min + random.nextDouble() * (option.yaw.max - option.yaw.min));
        float pitch = (option.pitch == null) ? loc.getPitch()
                : option.pitch.isValue() ? option.pitch.value.floatValue()
                : (float) (option.pitch.min + random.nextDouble() * (option.pitch.max - option.pitch.min));
        pitch = (float) clampPitch(pitch);

        loc.setYaw(yaw);
        loc.setPitch(pitch);
    }

    private double clampPitch(double pitch) {
        if (pitch < -90.0) return -90.0;
        if (pitch > 90.0) return 90.0;
        return pitch;
    }

    private Location getBestWaitingRoom(SpawnPointsConfig.WaitingRoomConfig local, SpawnPointsConfig.WaitingRoomConfig entry) {
        SpawnPointsConfig.WaitingRoomConfig target = (local != null) ? local : entry;
        if (target == null) target = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;

        World world = Bukkit.getWorld(target.world);
        if (world == null) return null;

        return new Location(world, target.x, target.y, target.z, target.yaw, target.pitch);
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
                if (phases.contains(phase) && msg.text != null && !msg.text.isEmpty()) {
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

                int chance = getEffectiveChance(player, cmd);
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Checking command for " + player.getName() +
                            " with chance: " + chance + ", phase: " + phase);
                }

                if (chance >= 100 || random.nextInt(100) < chance) {
                    executeCommand(player, cmd.command);
                }
            }
        }
    }

    private int getEffectiveChance(Player player, SpawnPointsConfig.CommandActionEntry command) {
        if (command.chanceConditions == null || command.chanceConditions.isEmpty()) {
            return command.chance;
        }
        for (SpawnPointsConfig.ChanceConditionEntry condition : command.chanceConditions) {
            if (checkChanceCondition(player, condition)) {
                return condition.weight;
            }
        }
        return command.chance;
    }

    private boolean checkChanceCondition(Player player, SpawnPointsConfig.ChanceConditionEntry condition) {
        return switch (condition.type) {
            case "permission" ->
                    PlaceholderUtils.evaluatePermissionExpression(player, condition.value, player.isOp() || player.hasPermission("*"));
            case "placeholder" -> plugin.isPlaceholderAPIEnabled() &&
                    PlaceholderUtils.checkPlaceholderCondition(player, condition.value);
            default -> false;
        };
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
            player.teleport(location);
            afterTeleport.run();
        } else {
            plugin.getRunner().runDelayed(() -> {
                if (player.isOnline()) {
                    player.teleport(location);
                    afterTeleport.run();
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

    private String locationToString(Location loc) {
        if (loc == null) return "null";
        return "World: " + loc.getWorld().getName() +
                ", X: " + String.format("%.2f", loc.getX()) +
                ", Y: " + String.format("%.2f", loc.getY()) +
                ", Z: " + String.format("%.2f", loc.getZ());
    }

    public void cleanup() {
        for (SearchProcess sp : searchProcesses.values()) {
            try {
                sp.future.cancel(true);
            } catch (Exception ignored) {
            }
        }
        searchProcesses.clear();

        pendingAfterActions.clear();
        deathLocations.clear();
    }
}