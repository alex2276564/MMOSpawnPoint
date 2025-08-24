package uz.alex2276564.mmospawnpoint.manager;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.CoordinateSpawnsConfig;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.RegionSpawnsConfig;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.WorldSpawnsConfig;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SpawnManager {
    private final MMOSpawnPoint plugin;
    private final Random random = new Random();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    // Selected destination (for waiting room async)
    private final Map<UUID, RegionSpawnsConfig.LocationOption> selectedLocationOptions = new ConcurrentHashMap<>();

    // Pending AFTER-phase for direct path teleport
    private record PendingAfter(RegionSpawnsConfig.LocationOption loc, RegionSpawnsConfig.ActionsConfig global) {
    }

    private final Map<UUID, PendingAfter> pendingAfterActions = new ConcurrentHashMap<>();

    // Async handling
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Location>> pendingLocations = new ConcurrentHashMap<>();

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
            selectedLocationOptions.remove(playerId);
            pendingAfterActions.remove(playerId);

            BukkitTask task = pendingTeleports.remove(playerId);
            if (task != null) task.cancel();

            CompletableFuture<Location> future = pendingLocations.remove(playerId);
            if (future != null) future.cancel(true);

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Cleaned up spawn manager data for player: " + playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up player data for " + playerId + ": " + e.getMessage());
        }
    }

    // ============================= JOIN/DEATH ENTRY POINTS =============================

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
                    plugin.getLogger().info("No death location found for " + player.getName() + ", using default spawn");
                }
                return false;
            }

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Processing death spawn for " + player.getName() + " who died at " + locationToString(deathLocation));
            }

            BukkitTask pending = pendingTeleports.remove(player.getUniqueId());
            if (pending != null) pending.cancel();

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

    // ============================= PRIORITY PROCESSING =============================

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
        Object data = entry.spawnData();
        return switch (entry.type()) {
            case REGION -> processRegionSpawnEntry((RegionSpawnsConfig.RegionSpawnEntry) data, player);
            case WORLD -> processWorldSpawnEntry((WorldSpawnsConfig.WorldSpawnEntry) data, player);
            case COORDINATE -> processCoordinateSpawnEntry((CoordinateSpawnsConfig.CoordinateSpawnEntry) data, player);
        };
    }

    private Location processRegionSpawnEntry(RegionSpawnsConfig.RegionSpawnEntry entry, Player player) {
        return processGenericEntry(player, entry.conditions, entry.destinations, entry.actions, entry.waitingRoom);
    }

    private Location processWorldSpawnEntry(WorldSpawnsConfig.WorldSpawnEntry entry, Player player) {
        return processGenericEntry(player, entry.conditions, entry.destinations, entry.actions, entry.waitingRoom);
    }

    private Location processCoordinateSpawnEntry(CoordinateSpawnsConfig.CoordinateSpawnEntry entry, Player player) {
        return processGenericEntry(player, entry.conditions, entry.destinations, entry.actions, entry.waitingRoom);
    }

    private Location processGenericEntry(
            Player player,
            RegionSpawnsConfig.ConditionsConfig conditions,
            List<RegionSpawnsConfig.LocationOption> destinations,
            RegionSpawnsConfig.ActionsConfig globalActions,
            RegionSpawnsConfig.WaitingRoomConfig entryWaitingRoom
    ) {
        if (conditionsNotMet(player, conditions)) {
            return null;
        }

        if (destinations == null || destinations.isEmpty()) {
            // Actions only: AFTER-phase
            runPhaseForActions(player, globalActions, RegionSpawnsConfig.Phase.AFTER);
            return null;
        }

        boolean isWeightedSelection = destinations.size() > 1;

        RegionSpawnsConfig.LocationOption selected = selectDestination(player, destinations);
        if (selected == null) return null;

        boolean useWaitingRoom = plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled && selected.requireSafe;

        if (useWaitingRoom) {
            runPhaseForEntry(player, selected, globalActions, RegionSpawnsConfig.Phase.BEFORE);
            runPhaseForEntry(player, selected, globalActions, RegionSpawnsConfig.Phase.WAITING_ROOM);

            selectedLocationOptions.put(player.getUniqueId(), selected);
            Location waiting = getBestWaitingRoom(selected.waitingRoom, entryWaitingRoom);
            if (waiting != null) {
                startAsyncLocationSearchForSelected(player, selected, globalActions, isWeightedSelection);
                return waiting;
            }
            // Fallback: direct path
        }

        runPhaseForEntry(player, selected, globalActions, RegionSpawnsConfig.Phase.BEFORE);

        Location finalLoc = resolveLocationFromOption(player, selected, isWeightedSelection);
        if (finalLoc == null) return null;

        pendingAfterActions.put(player.getUniqueId(), new PendingAfter(selected, globalActions));
        return finalLoc;
    }

    private RegionSpawnsConfig.LocationOption selectDestination(Player player, List<RegionSpawnsConfig.LocationOption> options) {
        if (options.size() == 1) return options.get(0);

        int totalWeight = 0;
        for (RegionSpawnsConfig.LocationOption opt : options) {
            totalWeight += getEffectiveWeight(player, opt);
        }
        if (totalWeight <= 0) return null;

        int rnd = random.nextInt(totalWeight);
        int acc = 0;
        for (RegionSpawnsConfig.LocationOption opt : options) {
            int w = getEffectiveWeight(player, opt);
            acc += w;
            if (rnd < acc) return opt;
        }
        return options.get(0);
    }

    private int getEffectiveWeight(Player player, RegionSpawnsConfig.LocationOption option) {
        if (option.weightConditions == null || option.weightConditions.isEmpty()) {
            return option.weight;
        }
        for (RegionSpawnsConfig.WeightConditionEntry cond : option.weightConditions) {
            if (checkWeightCondition(player, cond)) {
                return cond.weight;
            }
        }
        return option.weight;
    }

    private boolean checkWeightCondition(Player player, RegionSpawnsConfig.WeightConditionEntry cond) {
        return switch (cond.type) {
            case "permission" -> player.hasPermission(cond.value);
            case "placeholder" -> plugin.isPlaceholderAPIEnabled() &&
                    PlaceholderUtils.checkPlaceholderCondition(player, cond.value);
            default -> false;
        };
    }

    private boolean isFixedPointOption(RegionSpawnsConfig.LocationOption option) {
        return option.x != null && option.z != null
                && option.x.isValue() && option.z.isValue()
                && (option.y == null || option.y.isValue());
    }

    private Location resolveLocationFromOption(Player player, RegionSpawnsConfig.LocationOption option, boolean isWeightedSelection) {
        World world = Bukkit.getWorld(option.world);
        if (world == null) return null;

        // choose caching profiles from main config
        var cacheConfig = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching;

        if (option.requireSafe) {
            // FIXED-SAFE: find around a single point
            if (isFixedPointOption(option)) {
                double x = option.x.value;
                double z = option.z.value;
                double y;
                if (option.y != null && option.y.isValue()) {
                    y = option.y.value;
                } else {
                    // If Y not provided — start near ground
                    y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0;
                }

                Location base = new Location(world, x, y, z);
                int maxAttempts = plugin.getConfigManager().getMainConfig().settings.maxSafeLocationAttempts;

                boolean useCache = cacheConfig.fixedSpawns.enabled;
                boolean playerSpecific = cacheConfig.fixedSpawns.playerSpecific;

                Location safe = SafeLocationFinder.findSafeLocation(base, maxAttempts, player.getUniqueId(), useCache, playerSpecific);
                if (safe == null) return null;

                // Apply yaw/pitch
                applyYawPitch(option, safe);
                return safe;
            }

            // REGION-SAFE: search inside region
            double minX = option.x != null ? (option.x.isValue() ? option.x.value : option.x.min) : world.getWorldBorder().getCenter().getX() - 32;
            double maxX = option.x != null ? (option.x.isValue() ? option.x.value : option.x.max) : world.getWorldBorder().getCenter().getX() + 32;
            double minZ = option.z != null ? (option.z.isValue() ? option.z.value : option.z.min) : world.getWorldBorder().getCenter().getZ() - 32;
            double maxZ = option.z != null ? (option.z.isValue() ? option.z.value : option.z.max) : world.getWorldBorder().getCenter().getZ() + 32;

            double minY = 0;
            double maxY = world.getMaxHeight();
            if (option.y != null) {
                minY = option.y.isValue() ? option.y.value : option.y.min;
                maxY = option.y.isValue() ? option.y.value : option.y.max;
            }

            int maxAttempts = plugin.getConfigManager().getMainConfig().settings.maxSafeLocationAttempts;

            boolean useCache;
            boolean playerSpecific;
            if (isWeightedSelection) {
                useCache = cacheConfig.weightedRandomSpawns.enabled;
                playerSpecific = cacheConfig.weightedRandomSpawns.playerSpecific;
            } else {
                useCache = cacheConfig.randomSpawns.enabled;
                playerSpecific = cacheConfig.randomSpawns.playerSpecific;
            }

            Location safe = SafeLocationFinder.findSafeLocationInRegion(
                    minX, maxX, minY, maxY, minZ, maxZ,
                    world, maxAttempts, player.getUniqueId(), useCache, playerSpecific
            );
            if (safe == null) return null;

            applyYawPitch(option, safe);
            return safe;

        } else {
            // Direct (no safe search)
            if (option.y == null) return null; // validator catches this earlier

            double x = option.x.isValue() ? option.x.value : option.x.min + random.nextDouble() * (option.x.max - option.x.min);
            double y = option.y.isValue() ? option.y.value : option.y.min + random.nextDouble() * (option.y.max - option.y.min);
            double z = option.z.isValue() ? option.z.value : option.z.min + random.nextDouble() * (option.z.max - option.z.min);

            float yaw = (float) (option.yaw == null ? 0.0 : (option.yaw.isValue() ? option.yaw.value : option.yaw.min + random.nextDouble() * (option.yaw.max - option.yaw.min)));
            float pitch = (float) clampPitch(option.pitch == null ? 0.0 : (option.pitch.isValue() ? option.pitch.value : option.pitch.min + random.nextDouble() * (option.pitch.max - option.pitch.min)));

            return new Location(world, x, y, z, yaw, pitch);
        }
    }

    private void applyYawPitch(RegionSpawnsConfig.LocationOption option, Location loc) {
        float yaw = (float) (option.yaw == null ? loc.getYaw() : (option.yaw.isValue() ? option.yaw.value : option.yaw.min + random.nextDouble() * (option.yaw.max - option.yaw.min)));
        float pitch = (float) clampPitch(option.pitch == null ? loc.getPitch() : (option.pitch.isValue() ? option.pitch.value : option.pitch.min + random.nextDouble() * (option.pitch.max - option.pitch.min)));
        loc.setYaw(yaw);
        loc.setPitch(pitch);
    }

    private double clampPitch(double pitch) {
        if (pitch < -90.0) return -90.0;
        if (pitch > 90.0) return 90.0;
        return pitch;
    }

    private Location getBestWaitingRoom(RegionSpawnsConfig.WaitingRoomConfig local, RegionSpawnsConfig.WaitingRoomConfig entry) {
        RegionSpawnsConfig.WaitingRoomConfig target = (local != null) ? local : entry;
        if (target == null) target = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;

        World world = Bukkit.getWorld(target.world);
        if (world == null) return null;

        return new Location(world, target.x, target.y, target.z, target.yaw, target.pitch);
    }

    private void startAsyncLocationSearchForSelected(Player player, RegionSpawnsConfig.LocationOption selected,
                                                     RegionSpawnsConfig.ActionsConfig globalActions, boolean isWeightedSelection) {
        UUID playerId = player.getUniqueId();

        CompletableFuture<Location> locationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) return null;
                return resolveLocationFromOption(player, selected, isWeightedSelection);
            } catch (Exception e) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().warning("Error in async location search: " + e.getMessage());
                }
                return null;
            }
        });

        pendingLocations.put(playerId, locationFuture);

        locationFuture.orTimeout(plugin.getConfigManager().getMainConfig().settings.waitingRoom.asyncSearchTimeout, TimeUnit.SECONDS)
                .whenCompleteAsync((location, ex) -> {
                    pendingLocations.remove(playerId);

                    if (ex != null) {
                        if (!(ex instanceof CancellationException) && plugin.getConfigManager().getMainConfig().settings.debugMode) {
                            plugin.getLogger().warning("Async location search failed: " + ex.getMessage());
                        }
                        return;
                    }

                    if (location != null && player.isOnline()) {
                        scheduleTeleportWithActions(player, location, globalActions);
                    }
                });
    }

    private void scheduleTeleportWithActions(Player player, Location location, RegionSpawnsConfig.ActionsConfig globalActions) {
        if (!player.isOnline()) return;

        int delayTicks = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;
        UUID playerId = player.getUniqueId();

        BukkitTask task = plugin.getRunner().runDelayed(() -> {
            if (!player.isOnline()) return;

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Async teleporting " + player.getName() + " to: " + locationToString(location));
            }

            player.teleport(location);

            RegionSpawnsConfig.LocationOption selected = selectedLocationOptions.get(playerId);
            if (selected != null) {
                runPhaseForEntry(player, selected, globalActions, RegionSpawnsConfig.Phase.AFTER);
            } else {
                runPhaseForActions(player, globalActions, RegionSpawnsConfig.Phase.AFTER);
            }

            selectedLocationOptions.remove(playerId);
            pendingTeleports.remove(playerId);
        }, Math.max(delayTicks, 20L)); // минимум 1 сек (для стабильности)
        pendingTeleports.put(playerId, task);
    }

    private void runPhaseForEntry(Player player, RegionSpawnsConfig.LocationOption selected,
                                  RegionSpawnsConfig.ActionsConfig globalActions,
                                  RegionSpawnsConfig.Phase phase) {
        switch (selected.actionExecutionMode == null ? "before" : selected.actionExecutionMode.toLowerCase(Locale.ROOT)) {
            case "before":
                runPhaseForActions(player, selected.actions, phase);
                runPhaseForActions(player, globalActions, phase);
                break;
            case "after":
                runPhaseForActions(player, globalActions, phase);
                runPhaseForActions(player, selected.actions, phase);
                break;
            case "instead":
                runPhaseForActions(player, selected.actions, phase);
                break;
            default:
                runPhaseForActions(player, selected.actions, phase);
                runPhaseForActions(player, globalActions, phase);
                break;
        }
    }

    private void runPhaseForActions(Player player, RegionSpawnsConfig.ActionsConfig actions,
                                    RegionSpawnsConfig.Phase phase) {
        if (actions == null) return;

        if (actions.messages != null) {
            for (RegionSpawnsConfig.MessageEntry msg : actions.messages) {
                List<RegionSpawnsConfig.Phase> phases = (msg.phases == null || msg.phases.isEmpty())
                        ? List.of(RegionSpawnsConfig.Phase.AFTER)
                        : msg.phases;
                if (phases.contains(phase) && msg.text != null && !msg.text.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, processPlaceholders(player, msg.text));
                }
            }
        }

        if (actions.commands != null) {
            for (RegionSpawnsConfig.CommandActionEntry cmd : actions.commands) {
                List<RegionSpawnsConfig.Phase> phases = (cmd.phases == null || cmd.phases.isEmpty())
                        ? List.of(RegionSpawnsConfig.Phase.AFTER)
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

    private int getEffectiveChance(Player player, RegionSpawnsConfig.CommandActionEntry command) {
        if (command.chanceConditions == null || command.chanceConditions.isEmpty()) {
            return command.chance;
        }
        for (RegionSpawnsConfig.ChanceConditionEntry condition : command.chanceConditions) {
            if (checkChanceCondition(player, condition)) {
                return condition.weight;
            }
        }
        return command.chance;
    }

    private boolean checkChanceCondition(Player player, RegionSpawnsConfig.ChanceConditionEntry condition) {
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

        String processedCommand = processPlaceholders(player, command.replace("%player%", player.getName()));
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

    private boolean conditionsNotMet(Player player, RegionSpawnsConfig.ConditionsConfig conditions) {
        if (conditions == null) return false;

        boolean bypass = player.isOp() || player.hasPermission("*");
        if (bypass && plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Player " + player.getName() + " bypassing permission checks (OP or * permission)");
        }

        if (conditions.permissions != null && !conditions.permissions.isEmpty()) {
            for (String permissionExpr : conditions.permissions) {
                if (!PlaceholderUtils.evaluatePermissionExpression(player, permissionExpr, bypass)) {
                    return true; // not met
                }
            }
        }

        if (conditions.placeholders != null && !conditions.placeholders.isEmpty() && plugin.isPlaceholderAPIEnabled()) {
            for (String placeholderExpr : conditions.placeholders) {
                if (!PlaceholderUtils.checkPlaceholderCondition(player, placeholderExpr)) {
                    return true; // not met
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
                runPhaseForEntry(player, pending.loc, pending.global, RegionSpawnsConfig.Phase.AFTER);
            }
            sendTeleportMessage(player, eventType);
        };

        if (delayTicks <= 0) {
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
        for (BukkitTask task : pendingTeleports.values()) {
            task.cancel();
        }
        pendingTeleports.clear();
        pendingLocations.clear();
        selectedLocationOptions.clear();
        pendingAfterActions.clear();
        deathLocations.clear();
    }
}