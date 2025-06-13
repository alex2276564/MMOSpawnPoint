package uz.alex2276564.smartspawnpoint.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SafeLocationFinder {
    private static final Random RANDOM = new Random();
    private static Set<Material> unsafeMaterials = new HashSet<>();

    // Configurable cache settings
    private static boolean cacheEnabled = true;
    private static long cacheExpiry = 300000; // 5 minutes
    private static int maxCacheSize = 1000;
    private static boolean debugCache = false;

    // Cache storage
    private static final Map<String, Location> SAFE_LOCATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHE_TIMESTAMPS = new ConcurrentHashMap<>();

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

    public static void configureCaching(boolean enabled, long expiryMs, int maxSize, boolean debug) {
        cacheEnabled = enabled;
        cacheExpiry = expiryMs;
        maxCacheSize = maxSize;
        debugCache = debug;

        if (!enabled) {
            clearCache();
        }
    }

    public static void setUnsafeMaterials(Set<Material> materials) {
        if (materials != null && !materials.isEmpty()) {
            unsafeMaterials = materials;
        }
    }

    public static Location findSafeLocation(Location baseLocation, int maxAttempts, UUID playerId,
                                            boolean useCache, boolean playerSpecificCache) {
        if (baseLocation == null || !cacheEnabled || !useCache) {
            return findSafeLocationNoCache(baseLocation, maxAttempts);
        }

        World world = baseLocation.getWorld();
        if (world == null) {
            return null;
        }

        // Generate cache key
        String cacheKey = generateCacheKey("fixed", world.getName(),
                (int)baseLocation.getX(), (int)baseLocation.getY(), (int)baseLocation.getZ(),
                0, 0, 0, 0, 0, 0,
                playerId, playerSpecificCache);

        // Check cache
        Location cachedLocation = getCachedLocation(cacheKey);
        if (cachedLocation != null) {
            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache HIT for key: " + cacheKey);
            }
            return cachedLocation.clone();
        }

        // Find new safe location
        Location safeLocation = findSafeLocationNoCache(baseLocation, maxAttempts);

        if (safeLocation != null) {
            cacheLocation(cacheKey, safeLocation);
            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache STORE for key: " + cacheKey);
            }
        }

        return safeLocation;
    }

    public static Location findSafeLocationInRegion(double minX, double maxX, double minY, double maxY,
                                                    double minZ, double maxZ, World world, int maxAttempts,
                                                    UUID playerId, boolean useCache, boolean playerSpecificCache) {
        if (world == null || !cacheEnabled || !useCache) {
            return findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts);
        }

        // Generate cache key for region
        String cacheKey = generateCacheKey("region", world.getName(), 0, 0, 0,
                (int)minX, (int)maxX, (int)minY, (int)maxY, (int)minZ, (int)maxZ,
                playerId, playerSpecificCache);

        // Check cache
        Location cachedLocation = getCachedLocation(cacheKey);
        if (cachedLocation != null) {
            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache HIT for key: " + cacheKey);
            }
            return cachedLocation.clone();
        }

        // Find new safe location
        Location safeLocation = findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts);

        if (safeLocation != null) {
            cacheLocation(cacheKey, safeLocation);
            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache STORE for key: " + cacheKey);
            }
        }

        return safeLocation;
    }

    // Overloaded methods for backward compatibility (when cache options aren't specified)
    public static Location findSafeLocation(Location baseLocation, int maxAttempts) {
        return findSafeLocationNoCache(baseLocation, maxAttempts);
    }

    public static Location findSafeLocationInRegion(double minX, double maxX, double minY, double maxY,
                                                    double minZ, double maxZ, World world, int maxAttempts) {
        return findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts);
    }

    private static String generateCacheKey(String type, String worldName, int x, int y, int z,
                                           int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                                           UUID playerId, boolean playerSpecific) {
        StringBuilder key = new StringBuilder();
        key.append(worldName).append(":").append(type);

        if ("fixed".equals(type)) {
            key.append(":").append(x).append(":").append(y).append(":").append(z);
        } else if ("region".equals(type)) {
            key.append(":").append(minX).append(":").append(maxX)
                    .append(":").append(minY).append(":").append(maxY)
                    .append(":").append(minZ).append(":").append(maxZ);
        }

        // Add player ID to cache key if player-specific caching is enabled
        if (playerSpecific && playerId != null) {
            key.append(":player:").append(playerId.toString());
        }

        return key.toString();
    }

    private static Location getCachedLocation(String cacheKey) {
        if (!SAFE_LOCATION_CACHE.containsKey(cacheKey)) {
            return null;
        }

        long timestamp = CACHE_TIMESTAMPS.getOrDefault(cacheKey, 0L);
        if (System.currentTimeMillis() - timestamp < cacheExpiry) {
            return SAFE_LOCATION_CACHE.get(cacheKey);
        } else {
            // Cache expired
            SAFE_LOCATION_CACHE.remove(cacheKey);
            CACHE_TIMESTAMPS.remove(cacheKey);
            return null;
        }
    }

    private static void cacheLocation(String cacheKey, Location location) {
        // Check cache size limit
        if (SAFE_LOCATION_CACHE.size() >= maxCacheSize) {
            cleanOldestCacheEntries();
        }

        SAFE_LOCATION_CACHE.put(cacheKey, location.clone());
        CACHE_TIMESTAMPS.put(cacheKey, System.currentTimeMillis());
    }

    private static void cleanOldestCacheEntries() {
        try {
            // Remove 20% of oldest entries when cache is full
            int entriesToRemove = Math.max(1, maxCacheSize / 5);

            List<String> keysToRemove = CACHE_TIMESTAMPS.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(entriesToRemove)
                    .map(Map.Entry::getKey)
                    .collect(java.util.stream.Collectors.toList());

            for (String key : keysToRemove) {
                SAFE_LOCATION_CACHE.remove(key);
                CACHE_TIMESTAMPS.remove(key);
            }

            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cleaned " + keysToRemove.size() + " oldest cache entries");
            }
        } catch (Exception e) {
            SmartSpawnPoint.getInstance().getLogger().warning("[SafeLocationFinder] Error cleaning cache entries: " + e.getMessage());
        }
    }

    public static void clearPlayerCache(UUID playerId) {
        if (playerId == null) return;

        try {
            String playerPrefix = ":player:" + playerId.toString();
            SAFE_LOCATION_CACHE.entrySet().removeIf(entry -> entry.getKey().contains(playerPrefix));
            CACHE_TIMESTAMPS.entrySet().removeIf(entry -> entry.getKey().contains(playerPrefix));

            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cleared cache for player: " + playerId);
            }
        } catch (Exception e) {
            SmartSpawnPoint.getInstance().getLogger().warning("[SafeLocationFinder] Error clearing player cache: " + e.getMessage());
        }
    }

    private static Location findSafeLocationNoCache(Location baseLocation, int maxAttempts) {
        if (baseLocation == null) {
            return null;
        }

        World world = baseLocation.getWorld();
        if (world == null) {
            return null;
        }

        // Check if the original location is safe
        if (isSafeLocation(baseLocation)) {
            return baseLocation.clone();
        }

        // Special handling for Nether
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

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
            int highestY;
            if (isNether) {
                // For Nether, find a safe Y coordinate
                highestY = findSafeYInNether(world, testLocation.getBlockX(), testLocation.getBlockZ(),
                        120, 30); // Common Nether height range
            } else {
                // For other worlds, use highest block
                highestY = world.getHighestBlockYAt(testLocation.getBlockX(), testLocation.getBlockZ());
            }

            testLocation.setY(highestY + 1.0); // Add 1.0 to ensure we're above the block

            if (isSafeLocation(testLocation)) {
                return testLocation.clone();
            }

            // Increase search radius after several attempts
            if (attempts > maxAttempts / 2 && radius < 20) {
                radius *= 2;
            }
        }

        // If no safe location found, return the world spawn
        Location worldSpawn = world.getSpawnLocation();
        return worldSpawn;
    }

    private static Location findSafeLocationInRegionNoCache(double minX, double maxX, double minY, double maxY,
                                                            double minZ, double maxZ, World world, int maxAttempts) {
        if (world == null) {
            return null;
        }

        // Calculate center of region for default
        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        double centerZ = (minZ + maxZ) / 2;

        // Special handling for Nether
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        // Try to find a good default by using highest block at center
        int centerHighestY;
        if (isNether) {
            // For Nether, start from the middle and search down
            centerHighestY = findSafeYInNether(world, (int)centerX, (int)centerZ, (int)maxY, (int)minY);
        } else {
            // For other worlds, use highest block
            centerHighestY = world.getHighestBlockYAt((int)centerX, (int)centerZ);
        }

        Location centerLoc = new Location(world, centerX, centerHighestY + 1.0, centerZ);

        if (isSafeLocation(centerLoc)) {
            return centerLoc.clone();
        }

        // Try random locations
        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;

            // Generate random coordinates within the specified bounds
            double x = minX + RANDOM.nextDouble() * (maxX - minX);
            double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);

            double y;
            if (isNether) {
                // For Nether, find a safe Y coordinate
                y = findSafeYInNether(world, (int)x, (int)z, (int)maxY, (int)minY);
            } else {
                // For other worlds, use random Y within bounds
                y = minY + RANDOM.nextDouble() * (maxY - minY);
            }

            Location location = new Location(world, x, y, z);

            if (isSafeLocation(location)) {
                return location.clone();
            }

            // After half attempts, try using highest block Y
            if (attempts == maxAttempts / 2) {
                x = minX + RANDOM.nextDouble() * (maxX - minX);
                z = minZ + RANDOM.nextDouble() * (maxZ - minZ);

                int y2;
                if (isNether) {
                    y2 = findSafeYInNether(world, (int)x, (int)z, (int)maxY, (int)minY);
                } else {
                    y2 = world.getHighestBlockYAt((int)x, (int)z);
                }

                Location highestLocation = new Location(world, x, y2 + 1.0, z);

                if (isSafeLocation(highestLocation)) {
                    return highestLocation.clone();
                }
            }
        }

        // If still no safe location found, use world spawn if within bounds
        Location worldSpawn = world.getSpawnLocation();
        if (isWithinBounds(worldSpawn, minX, maxX, minY, maxY, minZ, maxZ)) {
            return worldSpawn.clone();
        }

        // Last resort - use center of region (even if not safe)
        Location centerLocation = new Location(world, centerX, centerY, centerZ);
        return centerLocation.clone();
    }

    private static int findSafeYInNether(World world, int x, int z, int maxY, int minY) {
        // Start from the middle of the range
        int startY = (maxY + minY) / 2;

        // First try going down from the middle
        for (int y = startY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            Block blockAbove2 = world.getBlockAt(x, y + 2, z);

            // Check if we have 2 air blocks above a solid block
            if (block.getType().isSolid() &&
                    !unsafeMaterials.contains(block.getType()) &&
                    blockAbove.getType().isAir() &&
                    blockAbove2.getType().isAir()) {
                return y;
            }
        }

        // If not found, try going up
        for (int y = startY + 1; y <= maxY - 2; y++) {
            Block block = world.getBlockAt(x, y, z);
            Block blockAbove = world.getBlockAt(x, y + 1, z);
            Block blockAbove2 = world.getBlockAt(x, y + 2, z);

            // Check if we have 2 air blocks above a solid block
            if (block.getType().isSolid() &&
                    !unsafeMaterials.contains(block.getType()) &&
                    blockAbove.getType().isAir() &&
                    blockAbove2.getType().isAir()) {
                return y;
            }
        }

        // If nothing found, return a reasonable default
        return 64; // Common safe height in Nether
    }

    private static boolean isWithinBounds(Location loc, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
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

    public static void clearCache() {
        SAFE_LOCATION_CACHE.clear();
        CACHE_TIMESTAMPS.clear();
    }
}
