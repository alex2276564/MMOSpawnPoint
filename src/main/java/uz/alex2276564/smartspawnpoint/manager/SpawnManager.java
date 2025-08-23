package uz.alex2276564.smartspawnpoint.manager;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.CoordinateSpawnsConfig;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.RegionSpawnsConfig;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.WorldSpawnsConfig;
import uz.alex2276564.smartspawnpoint.party.PartyManager;
import uz.alex2276564.smartspawnpoint.utils.PlaceholderUtils;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SpawnManager {
    private final SmartSpawnPoint plugin;
    private final Random random = new Random();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    // Selected location option for current player during weighted selection / waiting room
    private final Map<UUID, RegionSpawnsConfig.LocationOption> selectedLocationOptions = new ConcurrentHashMap<>();

    // Async handling
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Location>> pendingLocations = new ConcurrentHashMap<>();

    @Setter
    private PartyManager partyManager;

    public SpawnManager(SmartSpawnPoint plugin) {
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

            // Party join spawns
            String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled &&
                    partyManager != null && ("joins".equals(partyScope) || "both".equals(partyScope))) {

                Location partyLocation = partyManager.findPartyJoinLocation(player);
                if (partyLocation != null && partyLocation != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    teleportPlayerWithDelay(player, partyLocation, "join");
                    return true;
                }
            }

            // Priority system
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

            // Cancel any pending teleport
            BukkitTask pending = pendingTeleports.remove(player.getUniqueId());
            if (pending != null) pending.cancel();

            // Party respawn
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

            // Priority system
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

    // ============================= REGION/WORLD/COORDINATE PROCESSORS =============================

    private Location processRegionSpawnEntry(RegionSpawnsConfig.RegionSpawnEntry entry, Player player) {
        return processGenericEntry(
                player,
                entry.conditions,
                entry.locations,
                entry.actions,
                entry.waitingRoom
        );
    }

    private Location processWorldSpawnEntry(WorldSpawnsConfig.WorldSpawnEntry entry, Player player) {
        return processGenericEntry(
                player,
                entry.conditions,
                entry.locations,
                entry.actions,
                entry.waitingRoom
        );
    }

    private Location processCoordinateSpawnEntry(CoordinateSpawnsConfig.CoordinateSpawnEntry entry, Player player) {
        return processGenericEntry(
                player,
                entry.conditions,
                entry.locations,
                entry.actions,
                entry.waitingRoom
        );
    }

    private Location processGenericEntry(
            Player player,
            RegionSpawnsConfig.ConditionsConfig conditions,
            List<RegionSpawnsConfig.LocationOption> locations,
            RegionSpawnsConfig.ActionsConfig actions,
            RegionSpawnsConfig.WaitingRoomConfig entryWaitingRoom
    ) {
        // Conditions
        if (conditionsNotMet(player, conditions)) {
            return null;
        }

        // None (actions only)
        if (locations == null || locations.isEmpty()) {
            executeActions(player, actions, false);
            return null;
        }

        // Select location option (weighted if multiple)
        RegionSpawnsConfig.LocationOption selected = selectLocationOption(player, locations);
        if (selected == null) return null;

        boolean isWeightedSelection = locations.size() > 1;
        boolean useWaitingRoom = plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled && selected.requireSafe;

        if (useWaitingRoom) {
            selectedLocationOptions.put(player.getUniqueId(), selected);
            Location waiting = getBestWaitingRoom(selected.waitingRoom, entryWaitingRoom);
            if (waiting != null) {
                executeWeightedLocationActions(player, selected, actions, true);
                startAsyncLocationSearchForSelected(player, selected, actions);
                return waiting;
            }
        }

        Location finalLoc = resolveLocationFromOption(player, selected, isWeightedSelection);
        if (finalLoc != null) {
            executeWeightedLocationActions(player, selected, actions, false);
        }
        return finalLoc;
    }

    // ============================= LOCATION SELECTION/RESOLUTION =============================

    private RegionSpawnsConfig.LocationOption selectLocationOption(Player player, List<RegionSpawnsConfig.LocationOption> options) {
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

    private Location resolveLocationFromOption(Player player, RegionSpawnsConfig.LocationOption option, boolean isWeighted) {
        int maxAttempts = plugin.getConfigManager().getMainConfig().settings.maxSafeLocationAttempts;

        boolean cacheEnabled;
        boolean cachePlayerSpecific;
        if (isWeighted) {
            cacheEnabled = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching.weightedRandomSpawns.enabled;
            cachePlayerSpecific = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching.weightedRandomSpawns.playerSpecific;
        } else {
            if (option.isFixed()) {
                cacheEnabled = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching.fixedSpawns.enabled;
                cachePlayerSpecific = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching.fixedSpawns.playerSpecific;
            } else {
                cacheEnabled = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching.randomSpawns.enabled;
                cachePlayerSpecific = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.spawnTypeCaching.randomSpawns.playerSpecific;
            }
        }

        World world = Bukkit.getWorld(option.world);
        if (world == null) return null;

        if (option.isFixed()) {
            Location base = new Location(world, option.x, option.y, option.z, option.yaw, option.pitch);
            if (option.requireSafe) {
                return SafeLocationFinder.findSafeLocation(base, maxAttempts, player.getUniqueId(), cacheEnabled, cachePlayerSpecific);
            }
            return base;
        } else {
            if (option.requireSafe) {
                return SafeLocationFinder.findSafeLocationInRegion(
                        option.minX, option.maxX, option.minY, option.maxY, option.minZ, option.maxZ,
                        world, maxAttempts, player.getUniqueId(), cacheEnabled, cachePlayerSpecific
                );
            } else {
                double x = option.minX + random.nextDouble() * (option.maxX - option.minX);
                double y = option.minY + random.nextDouble() * (option.maxY - option.minY);
                double z = option.minZ + random.nextDouble() * (option.maxZ - option.minZ);
                return new Location(world, x, y, z);
            }
        }
    }

    // ============================= WAITING ROOM + ASYNC =============================

    private Location getBestWaitingRoom(RegionSpawnsConfig.WaitingRoomConfig local, RegionSpawnsConfig.WaitingRoomConfig entry) {
        RegionSpawnsConfig.WaitingRoomConfig target = (local != null) ? local : entry;
        if (target == null) target = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;

        World world = Bukkit.getWorld(target.world);
        if (world == null) return null;

        return new Location(world, target.x, target.y, target.z, target.yaw, target.pitch);
    }

    private void startAsyncLocationSearchForSelected(Player player, RegionSpawnsConfig.LocationOption selected,
                                                     RegionSpawnsConfig.ActionsConfig globalActions) {
        UUID playerId = player.getUniqueId();

        CompletableFuture<Location> locationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                if (Thread.currentThread().isInterrupted()) return null;
                return resolveLocationFromOption(player, selected, true);
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
                executeWeightedLocationActions(player, selected, globalActions, false);
            } else {
                executeActions(player, globalActions, false);
            }

            selectedLocationOptions.remove(playerId);
            pendingTeleports.remove(playerId);
        }, Math.max(delayTicks, 20L)); // at least 1 second

        pendingTeleports.put(playerId, task);
    }

    // ============================= ACTIONS =============================

    private void executeWeightedLocationActions(Player player, RegionSpawnsConfig.LocationOption selected,
                                                RegionSpawnsConfig.ActionsConfig globalActions, boolean inWaitingRoom) {
        String mode = selected.actionExecutionMode == null ? "before" : selected.actionExecutionMode.toLowerCase(Locale.ROOT);

        switch (mode) {
            case "before":
                executeActions(player, selected.actions, inWaitingRoom);
                executeActions(player, globalActions, inWaitingRoom);
                break;
            case "after":
                executeActions(player, globalActions, inWaitingRoom);
                executeActions(player, selected.actions, inWaitingRoom);
                break;
            case "instead":
                executeActions(player, selected.actions, inWaitingRoom);
                break;
            default:
                executeActions(player, selected.actions, inWaitingRoom);
                executeActions(player, globalActions, inWaitingRoom);
                break;
        }
    }

    private void executeActions(Player player, RegionSpawnsConfig.ActionsConfig actions, boolean inWaitingRoom) {
        if (actions == null) return;

        if (actions.messages != null) {
            for (String message : actions.messages) {
                plugin.getMessageManager().sendMessage(player, processPlaceholders(player, message));
            }
        }

        if (actions.commands != null) {
            for (RegionSpawnsConfig.CommandActionEntry command : actions.commands) {
                if (command.executeInWaitingRoom != inWaitingRoom) continue;

                int effectiveChance = getEffectiveChance(player, command);

                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Checking command for " + player.getName() +
                            " with chance: " + effectiveChance + ", inWaitingRoom: " + inWaitingRoom);
                }

                if (effectiveChance >= 100 || random.nextInt(100) < effectiveChance) { // use field 'random'
                    executeCommand(player, command.command);
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
                    checkPermissionCondition(player, condition.value, player.isOp() || player.hasPermission("*"));
            case "placeholder" -> plugin.isPlaceholderAPIEnabled() &&
                    PlaceholderUtils.checkPlaceholderCondition(player, condition.value);
            default -> false;
        };
    }

    private void executeCommand(Player player, String command) {
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

    // ============================= CONDITIONS =============================

    private boolean conditionsNotMet(Player player, RegionSpawnsConfig.ConditionsConfig conditions) {
        if (conditions == null) return false;

        boolean bypass = player.isOp() || player.hasPermission("*");

        if (bypass && plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Player " + player.getName() + " bypassing permission checks (OP or * permission)");
        }

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

    /**
     * Helper to evaluate permission conditions (simple or complex) using the expression engine.
     */
    private boolean checkPermissionCondition(Player player, String condition, boolean bypass) {
        return PlaceholderUtils.evaluatePermissionExpression(player, condition, bypass);
    }

    // ============================= UTIL =============================

    private void teleportPlayerWithDelay(Player player, Location location, String eventType) {
        int delayTicks = plugin.getConfigManager().getMainConfig().settings.teleport.delayTicks;

        if (delayTicks <= 0) {
            player.teleport(location);
            sendTeleportMessage(player, eventType);
        } else {
            plugin.getRunner().runDelayed(() -> {
                if (player.isOnline()) {
                    player.teleport(location);
                    sendTeleportMessage(player, eventType);
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
        deathLocations.clear();
    }
}