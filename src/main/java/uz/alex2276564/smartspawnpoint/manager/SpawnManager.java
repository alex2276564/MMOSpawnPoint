package uz.alex2276564.smartspawnpoint.manager;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.model.SpawnAction;
import uz.alex2276564.smartspawnpoint.model.SpawnCondition;
import uz.alex2276564.smartspawnpoint.model.SpawnLocation;
import uz.alex2276564.smartspawnpoint.model.SpawnPoint;
import uz.alex2276564.smartspawnpoint.util.PlaceholderUtils;
import uz.alex2276564.smartspawnpoint.util.SafeLocationFinder;
import uz.alex2276564.smartspawnpoint.util.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SpawnManager {
    private final SmartSpawnPoint plugin;
    private final Random random = new Random();
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    // For weighted random location selection
    private final Map<UUID, SpawnLocation> selectedWeightedLocations = new HashMap<>();

    // For async safe location search
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<Location>> pendingLocations = new ConcurrentHashMap<>();

    public SpawnManager(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    public void recordDeathLocation(Player player, Location location) {
        deathLocations.put(player.getUniqueId(), location.clone());
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Recorded death location for " + player.getName() + ": " + locationToString(location));
        }
    }

    public Location getSpawnLocation(Player player) {
        Location deathLocation = deathLocations.remove(player.getUniqueId());
        if (deathLocation == null) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("No death location found for " + player.getName() + ", using default spawn");
            }
            return null; // Use default spawn if no death location found
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

        // Find appropriate spawn point and location
        SpawnPoint spawnPoint = null;
        Location finalLocation = null;

        // Try to find a region-based spawn first (higher priority)
        if (plugin.isWorldGuardEnabled()) {
            spawnPoint = findRegionBasedSpawnPoint(player, deathLocation);
            if (spawnPoint != null) {
                // Check if this spawn point has a location (might be "none" type)
                if (spawnPoint.hasLocation()) {
                    // Check if we should use async search
                    if (plugin.getConfigManager().isUseWaitingRoom()) {
                        // Get appropriate waiting room location
                        Location waitingRoom = getWaitingRoomLocation(player, spawnPoint);
                        if (waitingRoom != null) {
                            // Start async location search
                            startAsyncLocationSearch(player, spawnPoint);
                            return waitingRoom;
                        }
                    }

                    // Use synchronous search
                    finalLocation = getLocationFromSpawnPoint(player, spawnPoint);
                }

                // Execute actions regardless of location
                executeActions(player, spawnPoint.getActions());
            }
        }

        // Fall back to world-based spawn if no region spawn or no location
        if (finalLocation == null) {
            spawnPoint = findWorldBasedSpawnPoint(player, deathLocation.getWorld().getName());

            if (spawnPoint != null) {
                // Check if this spawn point has a location (might be "none" type)
                if (spawnPoint.hasLocation()) {
                    // Check if we should use async search
                    if (plugin.getConfigManager().isUseWaitingRoom()) {
                        // Get appropriate waiting room location
                        Location waitingRoom = getWaitingRoomLocation(player, spawnPoint);
                        if (waitingRoom != null) {
                            // Start async location search
                            startAsyncLocationSearch(player, spawnPoint);
                            return waitingRoom;
                        }
                    }

                    // Use synchronous search
                    finalLocation = getLocationFromSpawnPoint(player, spawnPoint);
                }

                // Execute actions regardless of location
                executeActions(player, spawnPoint.getActions());
            }
        }

        // Log the final location
        if (plugin.getConfigManager().isDebugMode()) {
            if (finalLocation != null) {
                plugin.getLogger().info("Final respawn location for " + player.getName() + ": " + locationToString(finalLocation));
            } else {
                plugin.getLogger().warning("No respawn location found for " + player.getName() + ", using default");
            }
        }

        return finalLocation;
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

        // Create future for location search
        CompletableFuture<Location> locationFuture = CompletableFuture.supplyAsync(() -> {
            // Calculate the actual spawn location on a different thread
            return getLocationFromSpawnPoint(player, spawnPoint);
        });

        // Store the future
        pendingLocations.put(playerId, locationFuture);

        // Set timeout
        locationFuture.orTimeout(plugin.getConfigManager().getAsyncSearchTimeout(), TimeUnit.SECONDS)
                .thenAcceptAsync(location -> {
                    // Once we have the location, schedule teleport on main thread
                    if (location != null) {
                        scheduleTeleport(player, location, spawnPoint.getActions());
                    } else if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().warning("Async location search returned null for " + player.getName());
                    }

                    // Clean up
                    pendingLocations.remove(playerId);
                    selectedWeightedLocations.remove(playerId);
                })
                .exceptionally(ex -> {
                    // Handle exceptions/timeout
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().warning("Async location search failed for " + player.getName() + ": " + ex.getMessage());
                    }
                    pendingLocations.remove(playerId);
                    selectedWeightedLocations.remove(playerId);
                    return null;
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
                Location safeLocation = SafeLocationFinder.findSafeLocation(location, maxAttempts);

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
                Location safeLocation = SafeLocationFinder.findSafeLocationInRegion(
                        spawnLocation.getMinX(), spawnLocation.getMaxX(),
                        spawnLocation.getMinY(), spawnLocation.getMaxY(),
                        spawnLocation.getMinZ(), spawnLocation.getMaxZ(),
                        world, maxAttempts
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
                    Location safeLocation = SafeLocationFinder.findSafeLocation(bukkitLoc, maxAttempts);

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
            return SafeLocationFinder.findSafeLocation(bukkitLoc, maxAttempts);
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
            boolean hasPerm = bypass || player.hasPermission(value);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Permission check for " + player.getName() + ": " + value + " = " + hasPerm);
            }

            return hasPerm;
        } else if ("placeholder".equals(type) && plugin.isPlaceholderAPIEnabled()) {
            boolean result = PlaceholderUtils.checkPlaceholderCondition(player, value);

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Placeholder check for " + player.getName() + ": " + value + " = " + result);
            }

            return result;
        }

        return true;
    }

    private void executeActions(Player player, List<SpawnAction> actions) {
        for (SpawnAction action : actions) {
            int effectiveChance = action.getEffectiveChance(player);

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