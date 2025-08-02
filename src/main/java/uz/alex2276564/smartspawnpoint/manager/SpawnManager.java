package uz.alex2276564.smartspawnpoint.manager;

import lombok.Setter;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.model.SpawnAction;
import uz.alex2276564.smartspawnpoint.model.SpawnCondition;
import uz.alex2276564.smartspawnpoint.model.SpawnLocation;
import uz.alex2276564.smartspawnpoint.model.SpawnPoint;
import uz.alex2276564.smartspawnpoint.party.PartyManager;
import uz.alex2276564.smartspawnpoint.utils.PlaceholderUtils;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;
import uz.alex2276564.smartspawnpoint.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.*;

public class SpawnManager {
    private final SmartSpawnPoint plugin;
    private final Random random = new Random();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    // For weighted random location selection
    private final Map<UUID, SpawnLocation> selectedWeightedLocations = new HashMap<>();

    // For async safe location search
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Location>> pendingLocations = new ConcurrentHashMap<>();

    // Party manager reference
    @Setter
    private PartyManager partyManager;

    public SpawnManager(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    public void recordDeathLocation(Player player, Location location) {
        UUID playerId = player.getUniqueId();

        // Only record if there's no existing death location
        if (!deathLocations.containsKey(playerId)) {
            deathLocations.put(playerId, location.clone());
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Recorded death location for " + player.getName() + ": " + locationToString(location));
            }
        }
    }

    public void cleanupPlayerData(UUID playerId) {
        try {
            // Remove death location
            deathLocations.remove(playerId);

            // Remove selected weighted location
            selectedWeightedLocations.remove(playerId);

            // Cancel and remove pending teleports
            BukkitTask task = pendingTeleports.remove(playerId);
            if (task != null) {
                task.cancel();
            }

            // Cancel and remove pending location searches
            CompletableFuture<Location> future = pendingLocations.remove(playerId);
            if (future != null) {
                future.cancel(true);
            }

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Cleaned up spawn manager data for player: " + playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up player data for " + playerId + ": " + e.getMessage());
        }
    }


    public Location getSpawnLocation(Player player) {
        try {
            Location deathLocation = deathLocations.remove(player.getUniqueId());
            if (deathLocation == null) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("No death location found for " + player.getName() + ", using default spawn");
                }
                return null;
            }

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Getting spawn location for " + player.getName() + " who died at " + locationToString(deathLocation));
            }

            // Check for pending async teleports
            if (pendingTeleports.containsKey(player.getUniqueId())) {
                pendingTeleports.get(player.getUniqueId()).cancel();
                pendingTeleports.remove(player.getUniqueId());

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Cancelled pending teleport for " + player.getName());
                }
            }

            // Check for party respawn if party system is enabled
            if (plugin.getConfigManager().isPartyEnabled() && partyManager != null) {
                Location partyLocation = partyManager.findPartyRespawnLocation(player, deathLocation);
                if (partyLocation != null) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Using party respawn location for " + player.getName() + ": " + locationToString(partyLocation));
                    }
                    return partyLocation;
                }
            }

            // Find appropriate spawn point and location
            SpawnPoint spawnPoint = null;
            Location finalLocation = null;

            // Try to find a region-based spawn first (higher priority)
            if (plugin.isWorldGuardEnabled()) {
                spawnPoint = findRegionBasedSpawnPoint(player, deathLocation);
                if (spawnPoint != null) {
                    if (spawnPoint.hasLocation()) {
                        if (plugin.getConfigManager().isUseWaitingRoom()) {
                            Location waitingRoom = getWaitingRoomLocation(player, spawnPoint);
                            if (waitingRoom != null) {
                                startAsyncLocationSearch(player, spawnPoint);
                                return waitingRoom;
                            }
                        }
                        finalLocation = getLocationFromSpawnPoint(player, spawnPoint);
                    }
                    executeActions(player, spawnPoint.getActions());
                }
            }

            // Fall back to world-based spawn if no region spawn or no location
            if (finalLocation == null) {
                spawnPoint = findWorldBasedSpawnPoint(player, deathLocation.getWorld().getName());

                if (spawnPoint != null) {
                    if (spawnPoint.hasLocation()) {
                        if (plugin.getConfigManager().isUseWaitingRoom()) {
                            Location waitingRoom = getWaitingRoomLocation(player, spawnPoint);
                            if (waitingRoom != null) {
                                startAsyncLocationSearch(player, spawnPoint);
                                return waitingRoom;
                            }
                        }
                        finalLocation = getLocationFromSpawnPoint(player, spawnPoint);
                    }
                    executeActions(player, spawnPoint.getActions());
                }
            }

            if (plugin.getConfigManager().isDebugMode()) {
                if (finalLocation != null) {
                    plugin.getLogger().info("Final respawn location for " + player.getName() + ": " + locationToString(finalLocation));
                } else {
                    plugin.getLogger().warning("No respawn location found for " + player.getName() + ", using default");
                }
            }

            return finalLocation;
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting spawn location for " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfigManager().isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private Location getWaitingRoomLocation(Player player, SpawnPoint spawnPoint) {
        // First check if there's a selected weighted location with its own waiting room
        UUID playerId = player.getUniqueId();
        SpawnLocation selectedLocation = selectedWeightedLocations.get(playerId);

        if (selectedLocation != null && selectedLocation.getWaitingRoom() != null) {
            Location customWaitingRoom = selectedLocation.getWaitingRoom().toBukkitLocation();
            if (customWaitingRoom != null) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Using weighted location custom waiting room: " + locationToString(customWaitingRoom));
                }
                return customWaitingRoom;
            }
        }

        // Check if spawn point has a custom waiting room
        if (spawnPoint.getWaitingRoom() != null) {
            Location customWaitingRoom = spawnPoint.getWaitingRoom().toBukkitLocation();
            if (customWaitingRoom != null) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Using spawn point custom waiting room: " + locationToString(customWaitingRoom));
                }
                return customWaitingRoom;
            }
        }

        // Fall back to global waiting room
        if (plugin.getConfigManager().getWaitingRoomLocation() != null) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Using global waiting room: " +
                        locationToString(plugin.getConfigManager().getWaitingRoomLocation()));
            }
            return plugin.getConfigManager().getWaitingRoomLocation();
        }

        return null;
    }

    private void startAsyncLocationSearch(Player player, SpawnPoint spawnPoint) {
        UUID playerId = player.getUniqueId();

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Starting async location search for " + player.getName());
        }

        // Cancel any existing search for this player
        CompletableFuture<Location> existingFuture = pendingLocations.remove(playerId);
        if (existingFuture != null) {
            existingFuture.cancel(true);
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Cancelled existing async search for " + player.getName());
            }
        }

        // Create future for location search
        CompletableFuture<Location> locationFuture = CompletableFuture.supplyAsync(() -> {
            try {
                // Check if task was canceled before starting
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }

                return getLocationFromSpawnPoint(player, spawnPoint);
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Error in async location search for " + player.getName() + ": " + e.getMessage());
                }
                return null;
            }
        });

        // Store the future
        pendingLocations.put(playerId, locationFuture);

        // Set timeout and handle completion
        locationFuture.orTimeout(plugin.getConfigManager().getAsyncSearchTimeout(), TimeUnit.SECONDS)
                .whenCompleteAsync((location, ex) -> {
                    // Clean up immediately
                    pendingLocations.remove(playerId);
                    selectedWeightedLocations.remove(playerId);

                    if (ex != null) {
                        if (ex instanceof TimeoutException) {
                            if (plugin.getConfigManager().isDebugMode()) {
                                plugin.getLogger().warning("Async location search timed out for " + player.getName());
                            }
                        } else if (!(ex instanceof CancellationException)) {
                            if (plugin.getConfigManager().isDebugMode()) {
                                plugin.getLogger().warning("Async location search failed for " + player.getName() + ": " + ex.getMessage());
                            }
                        }
                        return;
                    }

                    if (location != null && player.isOnline()) {
                        scheduleTeleport(player, location, spawnPoint.getActions());
                    } else if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().warning("Async location search returned null for " + player.getName());
                    }
                });
    }

    private void scheduleTeleport(Player player, Location location, List<SpawnAction> actions) {
        if (!player.isOnline()) return;

        // Schedule teleport on main thread after a short delay (1 second)
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Teleporting " + player.getName() + " to: " + locationToString(location));
                }

                player.teleport(location);

                // Execute actions
                executeActions(player, actions);

                // Clean up
                pendingTeleports.remove(player.getUniqueId());
            }
        }, 20L); // 20 ticks = 1 second

        pendingTeleports.put(player.getUniqueId(), task);
    }

    private SpawnPoint findRegionBasedSpawnPoint(Player player, Location deathLocation) {
        // Get regions at death location
        Set<String> regions = WorldGuardUtils.getRegionsAt(deathLocation);

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Found regions at death location: " + String.join(", ", regions));
        }

        if (regions.isEmpty()) {
            return null;
        }

        // Find spawn points for these regions
        List<SpawnPoint> regionSpawns = plugin.getConfigManager().getRegionSpawns();
        for (SpawnPoint spawnPoint : regionSpawns) {
            if (regions.contains(spawnPoint.getRegion()) &&
                    (spawnPoint.getRegionWorld().equals("*") || deathLocation.getWorld().getName().equals(spawnPoint.getRegionWorld())) &&
                    checkConditions(player, spawnPoint)) {

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Found matching region spawn point: " + spawnPoint.getRegion() + " in world " + spawnPoint.getRegionWorld());
                }

                return spawnPoint;
            }
        }

        return null;
    }

    private SpawnPoint findWorldBasedSpawnPoint(Player player, String worldName) {
        Map<String, List<SpawnPoint>> worldSpawns = plugin.getConfigManager().getWorldSpawns();

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Looking for world-based spawn in world: " + worldName);
        }

        if (worldSpawns.containsKey(worldName)) {
            for (SpawnPoint spawnPoint : worldSpawns.get(worldName)) {
                if (checkConditions(player, spawnPoint)) {
                    return spawnPoint;
                }
            }
        }

        return null;
    }

    private Location getLocationFromSpawnPoint(Player player, SpawnPoint spawnPoint) {
        String type = spawnPoint.getType();
        int maxAttempts = plugin.getConfigManager().getMaxSafeLocationAttempts();
        UUID playerId = player.getUniqueId();

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Getting location from spawn point type: " + type);
        }

        if ("none".equals(type)) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Spawn point type is 'none', no teleport will be performed");
            }
            return null;
        } else if ("fixed".equals(type)) {
            SpawnLocation spawnLocation = spawnPoint.getLocation();
            Location location = spawnLocation.toBukkitLocation();

            if (location == null) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Fixed location conversion returned null");
                }
                return null;
            }

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Original fixed location: " + locationToString(location));
            }

            if (spawnLocation.isRequireSafe()) {
                // Get cache settings for fixed spawns
                boolean useCache = plugin.getConfigManager().isSafeCacheFixedEnabled();
                boolean playerSpecific = plugin.getConfigManager().isSafeCacheFixedPlayerSpecific();

                Location safeLocation = SafeLocationFinder.findSafeLocation(location, maxAttempts, playerId, useCache, playerSpecific);

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Safe fixed location: " + (safeLocation != null ? locationToString(safeLocation) : "null"));
                }

                return safeLocation;
            }

            return location;
        } else if ("random".equals(type)) {
            SpawnLocation spawnLocation = spawnPoint.getLocation();
            World world = Bukkit.getWorld(spawnLocation.getWorld());

            if (world == null) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().warning("Random location world not found: " + spawnLocation.getWorld());
                }
                return null;
            }

            if (spawnLocation.isRequireSafe()) {
                // Get cache settings for random spawns
                boolean useCache = plugin.getConfigManager().isSafeCacheRandomEnabled();
                boolean playerSpecific = plugin.getConfigManager().isSafeCacheRandomPlayerSpecific();

                Location safeLocation = SafeLocationFinder.findSafeLocationInRegion(
                        spawnLocation.getMinX(), spawnLocation.getMaxX(),
                        spawnLocation.getMinY(), spawnLocation.getMaxY(),
                        spawnLocation.getMinZ(), spawnLocation.getMaxZ(),
                        world, maxAttempts, playerId, useCache, playerSpecific
                );

                if (plugin.getConfigManager().isDebugMode() && safeLocation != null) {
                    plugin.getLogger().info("Safe random location: " + locationToString(safeLocation));
                }

                return safeLocation;
            } else {
                // Generate random coordinates
                double x = spawnLocation.getMinX() + random.nextDouble() * (spawnLocation.getMaxX() - spawnLocation.getMinX());
                double y = spawnLocation.getMinY() + random.nextDouble() * (spawnLocation.getMaxY() - spawnLocation.getMinY());
                double z = spawnLocation.getMinZ() + random.nextDouble() * (spawnLocation.getMaxZ() - spawnLocation.getMinZ());

                Location randomLocation = new Location(world, x, y, z);

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Random location (not safe): " + locationToString(randomLocation));
                }

                return randomLocation;
            }
        } else if ("weighted_random".equals(type)) {
            return getWeightedRandomLocation(player, spawnPoint.getWeightedLocations(), maxAttempts);
        }

        return null;
    }

    private Location getWeightedRandomLocation(Player player, List<SpawnLocation> weightedLocations, int maxAttempts) {
        if (weightedLocations.isEmpty()) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("No weighted locations found");
            }
            return null;
        }

        UUID playerId = player.getUniqueId();

        // Calculate total effective weight
        int totalWeight = 0;
        for (SpawnLocation location : weightedLocations) {
            totalWeight += location.getEffectiveWeight(player);
        }

        if (totalWeight <= 0) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().warning("Total weight for weighted random locations is 0 or negative");
            }
            return null;
        }

        int randomValue = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (SpawnLocation location : weightedLocations) {
            int effectiveWeight = location.getEffectiveWeight(player);
            cumulativeWeight += effectiveWeight;

            if (randomValue < cumulativeWeight) {
                // Store the selected location for potential waiting room lookup
                selectedWeightedLocations.put(player.getUniqueId(), location);

                Location bukkitLoc = location.toBukkitLocation();

                if (bukkitLoc == null) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().warning("Weighted random location conversion returned null");
                    }
                    continue;
                }

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Selected weighted random location: " + locationToString(bukkitLoc) +
                            " with weight: " + effectiveWeight);
                }

                if (location.isRequireSafe()) {
                    // Get cache settings for weighted spawns
                    boolean useCache = plugin.getConfigManager().isSafeCacheWeightedEnabled();
                    boolean playerSpecific = plugin.getConfigManager().isSafeCacheWeightedPlayerSpecific();

                    Location safeLocation = SafeLocationFinder.findSafeLocation(bukkitLoc, maxAttempts, playerId, useCache, playerSpecific);

                    if (plugin.getConfigManager().isDebugMode() && safeLocation != null) {
                        plugin.getLogger().info("Safe weighted random location: " + locationToString(safeLocation));
                    }

                    return safeLocation;
                }

                return bukkitLoc;
            }
        }

        // If we somehow got here, return the first location
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().warning("No weighted location selected, using first location");
        }

        SpawnLocation firstLocation = weightedLocations.get(0);
        selectedWeightedLocations.put(player.getUniqueId(), firstLocation);

        Location bukkitLoc = firstLocation.toBukkitLocation();

        if (firstLocation.isRequireSafe() && bukkitLoc != null) {
            // Get cache settings for weighted spawns
            boolean useCache = plugin.getConfigManager().isSafeCacheWeightedEnabled();
            boolean playerSpecific = plugin.getConfigManager().isSafeCacheWeightedPlayerSpecific();

            return SafeLocationFinder.findSafeLocation(bukkitLoc, maxAttempts, playerId, useCache, playerSpecific);
        }

        return bukkitLoc;
    }


    private boolean checkConditions(Player player, SpawnPoint spawnPoint) {
        List<SpawnCondition> conditions = spawnPoint.getConditions();

        if (conditions.isEmpty()) {
            return true; // No conditions means always valid
        }

        // Players with op or * permission bypass permission checks
        boolean bypass = player.isOp() || player.hasPermission("*");

        if (bypass && plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Player " + player.getName() + " bypassing permission checks (OP or * permission)");
        }

        for (SpawnCondition condition : conditions) {
            if (!checkCondition(player, condition, bypass)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkCondition(Player player, SpawnCondition condition, boolean bypass) {
        String type = condition.getType();
        String value = condition.getValue();

        if ("permission".equals(type)) {
            // Validate permission expression
            if (value.contains("&&") || value.contains("||")) {
                if (!PlaceholderUtils.isValidLogicalExpression(value)) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().warning("Invalid permission expression: " + value);
                    }
                    return false;
                }
                return evaluateComplexPermissionCondition(player, value, bypass);
            }

            // Simple permission check
            boolean hasPerm = bypass || player.hasPermission(value);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Permission check for " + player.getName() + ": " + value + " = " + hasPerm);
            }

            return hasPerm;
        } else if ("placeholder".equals(type) && plugin.isPlaceholderAPIEnabled()) {
            // Validation is handled inside PlaceholderUtils.checkPlaceholderCondition
            boolean result = PlaceholderUtils.checkPlaceholderCondition(player, value);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Placeholder check for " + player.getName() + ": " + value + " = " + result);
            }

            return result;
        }

        return true;
    }

    private boolean evaluateComplexPermissionCondition(Player player, String condition, boolean bypass) {
        // Split by OR first (lowest precedence)
        String[] orParts = condition.split("\\|\\|");

        for (String orPart : orParts) {
            // Split by AND (higher precedence)
            String[] andParts = orPart.trim().split("&&");
            boolean andResult = true;

            for (String andPart : andParts) {
                String permission = andPart.trim();
                boolean hasPerm = bypass || player.hasPermission(permission);

                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Complex permission check for " + player.getName() + ": " + permission + " = " + hasPerm);
                }

                if (!hasPerm) {
                    andResult = false;
                    break;
                }
            }

            // If any OR part is true, the whole condition is true
            if (andResult) {
                return true;
            }
        }

        // If no OR part was true, the whole condition is false
        return false;
    }



    private void executeActions(Player player, List<SpawnAction> actions) {
        for (SpawnAction action : actions) {
            int effectiveChance = action.getEffectiveChance(player);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Checking action for " + player.getName() +
                        " with type: " + action.getType() +
                        ", value: " + action.getValue() +
                        ", chance: " + effectiveChance);

                // Debug conditions
                if (!action.getChanceConditions().isEmpty()) {
                    for (SpawnCondition condition : action.getChanceConditions()) {
                        boolean conditionMet = false;
                        if (condition.getType().equals("permission")) {
                            conditionMet = player.hasPermission(condition.getValue());
                        } else if (condition.getType().equals("placeholder")) {
                            conditionMet = PlaceholderUtils.checkPlaceholderCondition(player, condition.getValue());
                        }

                        plugin.getLogger().info("  Condition: " + condition.getType() +
                                ", value: " + condition.getValue() +
                                ", met: " + conditionMet);
                    }
                }
            }

            // Check if action should execute based on chance
            if (effectiveChance >= 100 || random.nextInt(100) < effectiveChance) {
                executeAction(player, action);
            }
        }
    }


    private void executeAction(Player player, SpawnAction action) {
        String type = action.getType();
        final String value = action.getValue().replace("%player%", player.getName());

        final String processedValue;
        if (plugin.isPlaceholderAPIEnabled()) {
            processedValue = PlaceholderUtils.setPlaceholders(player, value);
        } else {
            processedValue = value;
        }

        if ("message".equals(type)) {
            player.sendMessage(processedValue);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Sent message to " + player.getName() + ": " + processedValue);
            }
        } else if ("command".equals(type)) {
            plugin.getRunner().runDelayed(() -> {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Executing command for " + player.getName() + ": " + processedValue);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedValue);
            }, 1L);
        }
    }

    private String locationToString(Location loc) {
        if (loc == null) return "null";
        return "World: " + loc.getWorld().getName() +
                ", X: " + String.format("%.2f", loc.getX()) +
                ", Y: " + String.format("%.2f", loc.getY()) +
                ", Z: " + String.format("%.2f", loc.getZ());
    }

    // Method to clean up pending tasks when plugin disables
    public void cleanup() {
        // Cancel all pending teleports
        for (BukkitTask task : pendingTeleports.values()) {
            task.cancel();
        }
        pendingTeleports.clear();
        pendingLocations.clear();
        selectedWeightedLocations.clear();
    }
}