package uz.alex2276564.smartspawnpoint.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SafeLocationFinder {
    private static final Random RANDOM = new Random();
    private static Set<Material> unsafeMaterials = new HashSet<>();

    // Search settings (configurable)
    private static int baseSearchRadius = 5; // will be overridden by config
    private static int maxSearchRadius = 20; // will be derived from base radius

    // Configurable cache settings
    private static boolean cacheEnabled = true;
    private static long cacheExpiry = 300000; // 5 minutes
    private static int maxCacheSize = 1000;
    private static boolean debugCache = false;

    // Cache storage
    private static final Map<String, Location> SAFE_LOCATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Long> CACHE_TIMESTAMPS = new ConcurrentHashMap<>();

    // Cache statistics
    private static long cacheHits = 0;
    private static long cacheMisses = 0;
    private static long totalSearches = 0;

    public static void configureCaching(boolean enabled, long expiryMs, int maxSize, boolean debug) {
        cacheEnabled = enabled;
        cacheExpiry = expiryMs;
        maxCacheSize = maxSize;
        debugCache = debug;

        if (!enabled) {
            clearCache();
        }

        if (debugCache) {
            SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache configured - enabled: " + enabled +
                    ", expiry: " + (expiryMs / 1000) + "s, maxSize: " + maxSize);
        }
    }

    public static void configureUnsafeMaterials(List<String> materialNames) {
        Set<Material> materials = new HashSet<>();

        for (String materialName : materialNames) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                materials.add(material);
            } catch (IllegalArgumentException e) {
                SmartSpawnPoint.getInstance().getLogger().warning("Unknown material in unsafe-materials list: " + materialName);
            }
        }

        unsafeMaterials = materials;

        if (debugCache) {
            SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Configured " + materials.size() + " unsafe materials");
        }
    }

    public static void configureSearchRadius(int radius) {
        // Configure base search radius and derived cap
        baseSearchRadius = Math.max(1, radius);
        maxSearchRadius = Math.max(baseSearchRadius, baseSearchRadius * 4);
        if (debugCache) {
            SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Search radius configured - base: " + baseSearchRadius + ", cap: " + maxSearchRadius);
        }
    }

    public static Location findSafeLocation(Location baseLocation, int maxAttempts, UUID playerId,
                                            boolean useCache, boolean playerSpecificCache) {
        totalSearches++;

        if (baseLocation == null || !cacheEnabled || !useCache) {
            return findSafeLocationNoCache(baseLocation, maxAttempts);
        }

        World world = baseLocation.getWorld();
        if (world == null) {
            return null;
        }

        String cacheKey = generateCacheKey("fixed", world.getName(),
                (int) baseLocation.getX(), (int) baseLocation.getY(), (int) baseLocation.getZ(),
                0, 0, 0, 0, 0, 0,
                playerId, playerSpecificCache);

        return getCachedOrCompute(cacheKey, () -> findSafeLocationNoCache(baseLocation, maxAttempts));
    }

    public static Location findSafeLocationInRegion(double minX, double maxX, double minY, double maxY,
                                                    double minZ, double maxZ, World world, int maxAttempts,
                                                    UUID playerId, boolean useCache, boolean playerSpecificCache) {
        totalSearches++;

        if (world == null || !cacheEnabled || !useCache) {
            return findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts);
        }

        String cacheKey = generateCacheKey("region", world.getName(), 0, 0, 0,
                (int) minX, (int) maxX, (int) minY, (int) maxY, (int) minZ, (int) maxZ,
                playerId, playerSpecificCache);

        return getCachedOrCompute(cacheKey, () -> findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts));
    }

    private static Location getCachedOrCompute(String cacheKey, Supplier<Location> supplier) {
        // Cache hit
        Location cachedLocation = getCachedLocation(cacheKey);
        if (cachedLocation != null) {
            cacheHits++;
            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache HIT for key: " + cacheKey);
                logCacheStatistics();
            }
            return cachedLocation.clone();
        }

        // Cache miss
        cacheMisses++;
        Location safeLocation = supplier.get();

        // Store in cache
        if (safeLocation != null) {
            cacheLocation(cacheKey, safeLocation);
            if (debugCache) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache STORE for key: " + cacheKey);
                logCacheStatistics();
            }
        }

        return safeLocation;
    }

    private static void logCacheStatistics() {
        double hitRate = totalSearches > 0 ? (cacheHits * 100.0) / totalSearches : 0;
        SmartSpawnPoint.getInstance().getLogger().info(String.format(
                "[SafeLocationFinder] Stats: %d searches, %d hits, %d misses, %.1f%% hit rate, %d cached",
                totalSearches, cacheHits, cacheMisses, hitRate, SAFE_LOCATION_CACHE.size()
        ));
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

        if (playerSpecific && playerId != null) {
            key.append(":player:").append(playerId);
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
            SAFE_LOCATION_CACHE.remove(cacheKey);
            CACHE_TIMESTAMPS.remove(cacheKey);
            return null;
        }
    }

    private static void cacheLocation(String cacheKey, Location location) {
        if (SAFE_LOCATION_CACHE.size() >= maxCacheSize) {
            cleanOldestCacheEntries();
        }

        SAFE_LOCATION_CACHE.put(cacheKey, location.clone());
        CACHE_TIMESTAMPS.put(cacheKey, System.currentTimeMillis());
    }

    private static void cleanOldestCacheEntries() {
        try {
            int entriesToRemove = Math.max(1, maxCacheSize / 5);

            List<String> keysToRemove = CACHE_TIMESTAMPS.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(entriesToRemove)
                    .map(Map.Entry::getKey)
                    .toList();

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
            String playerPrefix = ":player:" + playerId;
            int removedCount = 0;

            Iterator<Map.Entry<String, Location>> it = SAFE_LOCATION_CACHE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Location> entry = it.next();
                if (entry.getKey().contains(playerPrefix)) {
                    it.remove();
                    removedCount++;
                }
            }

            CACHE_TIMESTAMPS.entrySet().removeIf(entry -> entry.getKey().contains(playerPrefix));

            if (debugCache && removedCount > 0) {
                SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cleared " + removedCount + " cache entries for player: " + playerId);
            }
        } catch (Exception e) {
            SmartSpawnPoint.getInstance().getLogger().warning("[SafeLocationFinder] Error clearing player cache: " + e.getMessage());
        }
    }

    private static Location findSafeLocationNoCache(Location baseLocation, int maxAttempts) {
        if (baseLocation == null) return null;

        World world = baseLocation.getWorld();
        if (world == null) return null;

        // If original location is already safe
        if (isSafeLocation(baseLocation)) {
            return baseLocation.clone();
        }

        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        int attempts = 0;
        int radius = baseSearchRadius;

        while (attempts < maxAttempts) {
            attempts++;

            double offsetX = RANDOM.nextDouble() * (radius * 2) - radius;
            double offsetZ = RANDOM.nextDouble() * (radius * 2) - radius;

            Location testLocation = baseLocation.clone().add(offsetX, 0.0, offsetZ);

            int highestY;
            if (isNether) {
                highestY = findSafeYInNether(world, testLocation.getBlockX(), testLocation.getBlockZ(), 120, 30);
            } else {
                highestY = world.getHighestBlockYAt(testLocation.getBlockX(), testLocation.getBlockZ());
            }

            testLocation.setY(highestY + 1.0);

            if (isSafeLocation(testLocation)) {
                return testLocation.clone();
            }

            if (attempts > maxAttempts / 2 && radius < maxSearchRadius) {
                radius = Math.min(maxSearchRadius, radius * 2);
            }
        }

        return world.getSpawnLocation();
    }

    private static Location findSafeLocationInRegionNoCache(double minX, double maxX, double minY, double maxY,
                                                            double minZ, double maxZ, World world, int maxAttempts) {
        if (world == null) return null;

        double centerX = (minX + maxX) / 2;
        double centerY = (minY + maxY) / 2;
        double centerZ = (minZ + maxZ) / 2;

        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        int centerHighestY;
        if (isNether) {
            centerHighestY = findSafeYInNether(world, (int) centerX, (int) centerZ, (int) maxY, (int) minY);
        } else {
            centerHighestY = world.getHighestBlockYAt((int) centerX, (int) centerZ);
        }

        Location centerLoc = new Location(world, centerX, centerHighestY + 1.0, centerZ);

        if (isSafeLocation(centerLoc)) {
            return centerLoc.clone();
        }

        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;

            double x = minX + RANDOM.nextDouble() * (maxX - minX);
            double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);

            double y;
            if (isNether) {
                y = findSafeYInNether(world, (int) x, (int) z, (int) maxY, (int) minY);
            } else {
                y = minY + RANDOM.nextDouble() * (maxY - minY);
            }

            Location location = new Location(world, x, y, z);

            if (isSafeLocation(location)) {
                return location.clone();
            }

            if (attempts == maxAttempts / 2) {
                x = minX + RANDOM.nextDouble() * (maxX - minX);
                z = minZ + RANDOM.nextDouble() * (maxZ - minZ);

                int y2;
                if (isNether) {
                    y2 = findSafeYInNether(world, (int) x, (int) z, (int) maxY, (int) minY);
                } else {
                    y2 = world.getHighestBlockYAt((int) x, (int) z);
                }

                Location highestLocation = new Location(world, x, y2 + 1.0, z);

                if (isSafeLocation(highestLocation)) {
                    return highestLocation.clone();
                }
            }
        }

        Location worldSpawn = world.getSpawnLocation();
        if (isWithinBounds(worldSpawn, minX, maxX, minY, maxY, minZ, maxZ)) {
            return worldSpawn.clone();
        }

        return new Location(world, centerX, centerY, centerZ);
    }

    private static int findSafeYInNether(World world, int x, int z, int maxY, int minY) {
        int startY = (maxY + minY) / 2;

        // Scan down
        for (int y = startY; y >= minY; y--) {
            if (isSolidWithTwoAirAbove(world, x, y, z)) return y;
        }
        // Scan up
        for (int y = startY + 1; y <= maxY - 2; y++) {
            if (isSolidWithTwoAirAbove(world, x, y, z)) return y;
        }

        return 64;
    }

    private static boolean isSolidWithTwoAirAbove(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        Block blockAbove = world.getBlockAt(x, y + 1, z);
        Block blockAbove2 = world.getBlockAt(x, y + 2, z);

        return block.getType().isSolid() &&
                !unsafeMaterials.contains(block.getType()) &&
                blockAbove.getType().isAir() &&
                blockAbove2.getType().isAir();
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

        if (!feet.getType().isAir() || !head.getType().isAir()) {
            return false;
        }

        return ground.getType().isSolid() && !unsafeMaterials.contains(ground.getType());
    }

    public static void clearCache() {
        SAFE_LOCATION_CACHE.clear();
        CACHE_TIMESTAMPS.clear();

        if (debugCache) {
            SmartSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache cleared");
        }
    }
}