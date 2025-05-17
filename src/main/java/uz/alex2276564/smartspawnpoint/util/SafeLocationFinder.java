package uz.alex2276564.smartspawnpoint.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SafeLocationFinder {
    private static final Random RANDOM = new Random();
    private static Set<Material> unsafeMaterials = new HashSet<>();

    // Cache for performance optimization
    private static final Map<String, Location> SAFE_LOCATION_CACHE = new HashMap<>();
    private static final long CACHE_EXPIRY = 60000; // 1 minute
    private static final Map<String, Long> CACHE_TIMESTAMPS = new HashMap<>();

    static {
        // Default unsafe materials
        unsafeMaterials.add(Material.LAVA);
        unsafeMaterials.add(Material.FIRE);
        unsafeMaterials.add(Material.CACTUS);
        unsafeMaterials.add(Material.WATER);
        unsafeMaterials.add(Material.AIR);
        unsafeMaterials.add(Material.MAGMA_BLOCK);
        unsafeMaterials.add(Material.CAMPFIRE);
        unsafeMaterials.add(Material.SOUL_CAMPFIRE);
        unsafeMaterials.add(Material.WITHER_ROSE);
        unsafeMaterials.add(Material.SWEET_BERRY_BUSH);
    }

    public static void setUnsafeMaterials(Set<Material> materials) {
        if (materials != null && !materials.isEmpty()) {
            unsafeMaterials = materials;
        }
    }

    public static Location findSafeLocation(Location baseLocation, int maxAttempts) {
        if (baseLocation == null) {
            return null;
        }

        World world = baseLocation.getWorld();
        if (world == null) {
            return null;
        }

        // Generate cache key
        String cacheKey = world.getName() + ":" +
                (int)baseLocation.getX() + ":" +
                (int)baseLocation.getY() + ":" +
                (int)baseLocation.getZ();

        // Check cache
        if (SAFE_LOCATION_CACHE.containsKey(cacheKey)) {
            long timestamp = CACHE_TIMESTAMPS.getOrDefault(cacheKey, 0L);
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRY) {
                return SAFE_LOCATION_CACHE.get(cacheKey).clone();
            } else {
                // Cache expired
                SAFE_LOCATION_CACHE.remove(cacheKey);
                CACHE_TIMESTAMPS.remove(cacheKey);
            }
        }

        // Check if the original location is safe
        if (isSafeLocation(baseLocation)) {
            cacheLocation(cacheKey, baseLocation);
            return baseLocation.clone();
        }

        // Try to find a safe location nearby
        int attempts = 0;
        int radius = 5; // Smaller radius for better performance

        while (attempts < maxAttempts) {
            attempts++;

            // Get random location within radius
            double offsetX = RANDOM.nextDouble() * (radius * 2) - radius;
            double offsetZ = RANDOM.nextDouble() * (radius * 2) - radius;

            Location testLocation = baseLocation.clone().add(offsetX, 0.0, offsetZ);

            // Find the highest block at this X,Z coordinate
            int highestY = world.getHighestBlockYAt(testLocation.getBlockX(), testLocation.getBlockZ());
            testLocation.setY(highestY + 1.0); // Add 1.0 to ensure we're above the block

            if (isSafeLocation(testLocation)) {
                cacheLocation(cacheKey, testLocation);
                return testLocation.clone();
            }

            // Increase search radius after several attempts
            if (attempts > maxAttempts / 2 && radius < 20) {
                radius *= 2;
            }
        }

        // If no safe location found, return the world spawn
        Location worldSpawn = world.getSpawnLocation();
        cacheLocation(cacheKey, worldSpawn);
        return worldSpawn;
    }

    public static Location findSafeLocationInRegion(double minX, double maxX, double minY, double maxY, double minZ, double maxZ,
                                                    World world, int maxAttempts) {
        if (world == null) {
            return null;
        }

        // Cache key for this region
        String cacheKey = world.getName() + ":region:" +
                (int)minX + ":" + (int)maxX + ":" +
                (int)minY + ":" + (int)maxY + ":" +
                (int)minZ + ":" + (int)maxZ;

        // Check cache
        if (SAFE_LOCATION_CACHE.containsKey(cacheKey)) {
            long timestamp = CACHE_TIMESTAMPS.getOrDefault(cacheKey, 0L);
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRY) {
                return SAFE_LOCATION_CACHE.get(cacheKey).clone();
            } else {
                // Cache expired
                SAFE_LOCATION_CACHE.remove(cacheKey);
                CACHE_TIMESTAMPS.remove(cacheKey);
            }
        }

        // Calculate center of region for default
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        double centerZ = (minZ + maxZ) / 2;

        // Try to find a good default by using highest block at center
        int centerHighestY = world.getHighestBlockYAt((int)centerX, (int)centerZ);
        Location centerLoc = new Location(world, centerX, centerHighestY + 1.0, centerZ);

        if (isSafeLocation(centerLoc)) {
            cacheLocation(cacheKey, centerLoc);
            return centerLoc.clone();
        }

        // Try random locations
        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;

            // Generate random coordinates within the specified bounds
            double x = minX + RANDOM.nextDouble() * (maxX - minX);
            double y = minY + RANDOM.nextDouble() * (maxY - minY);
            double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);

            Location location = new Location(world, x, y, z);

            if (isSafeLocation(location)) {
                cacheLocation(cacheKey, location);
                return location.clone();
            }

            // After half attempts, try using highest block Y
            if (attempts == maxAttempts / 2) {
                x = minX + RANDOM.nextDouble() * (maxX - minX);
                z = minZ + RANDOM.nextDouble() * (maxZ - minZ);

                int y2 = world.getHighestBlockYAt((int)x, (int)z);
                Location highestLocation = new Location(world, x, y2 + 1.0, z);

                if (isSafeLocation(highestLocation)) {
                    cacheLocation(cacheKey, highestLocation);
                    return highestLocation.clone();
                }
            }
        }

        // If still no safe location found, use world spawn if within bounds
        Location worldSpawn = world.getSpawnLocation();
        if (isWithinBounds(worldSpawn, minX, maxX, minY, maxY, minZ, maxZ)) {
            cacheLocation(cacheKey, worldSpawn);
            return worldSpawn.clone();
        }

        // Last resort - use center of region (even if not safe)
        Location centerLocation = new Location(world, centerX, centerY, centerZ);
        cacheLocation(cacheKey, centerLocation);
        return centerLocation.clone();
    }

    private static boolean isWithinBounds(Location loc, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    private static void cacheLocation(String key, Location location) {
        SAFE_LOCATION_CACHE.put(key, location.clone());
        CACHE_TIMESTAMPS.put(key, System.currentTimeMillis());
    }

    private static boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        Block feet = location.getBlock();
        Block ground = location.clone().subtract(0, 1, 0).getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();

        // Check if feet and head positions are safe (not solid blocks)
        if (!feet.getType().isAir() || !head.getType().isAir()) {
            return false;
        }

        // Check if the ground is solid and safe to stand on
        return ground.getType().isSolid() && !unsafeMaterials.contains(ground.getType());
    }

    // Clear cache method - can be called periodically or on reload
    public static void clearCache() {
        SAFE_LOCATION_CACHE.clear();
        CACHE_TIMESTAMPS.clear();
    }
}