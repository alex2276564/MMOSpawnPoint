package uz.alex2276564.mmospawnpoint.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Threading note:
 * - This class is called from async tasks. Bukkit API is not guaranteed to be
 * thread-safe. On Paper 1.16.5+ many getters are practically safe in read-only
 * scenarios, but you use it at your own risk.
 * - You explicitly requested to keep the async approach as before.
 */
public class SafeLocationFinder {
    private static final Random RANDOM = new Random();

    // Configurable sets
    private static Set<Material> unsafeMaterials = new HashSet<>();
    private static Set<Material> bannedPassable = new HashSet<>();

    // Search settings (configurable)
    private static int baseSearchRadius = 5;
    private static int maxSearchRadius = 20;

    // Configurable cache settings
    private static boolean cacheEnabled = true;
    private static long cacheExpiry = 300000; // 5 min
    private static int maxCacheSize = 1000;
    private static boolean debugCache = false;

    // Overworld Y selection strategy and ratio (for MIXED)
    private enum OverworldYStrategy {MIXED, HIGHEST_FIRST, HIGHEST_ONLY}

    private static OverworldYStrategy overworldYStrategy = OverworldYStrategy.MIXED;
    private static double highestBlockYAttemptRatio = 0.6; // 60% highest-block attempts for MIXED

    // Cache storage
    private static final Map<CacheKey, Location> SAFE_LOCATION_CACHE = new ConcurrentHashMap<>();
    private static final Map<CacheKey, Long> CACHE_TIMESTAMPS = new ConcurrentHashMap<>();

    // Cache statistics
    private static long cacheHits = 0;
    private static long cacheMisses = 0;
    private static long totalSearches = 0;

    // Type-safe cache key
    private record CacheKey(
            String type, String worldName,
            int x, int y, int z,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
            UUID playerId, boolean playerSpecific
    ) {
    }

    // --------------- Configuration ----------------

    public static void configureCaching(boolean enabled, long expiryMs, int maxSize, boolean debug) {
        cacheEnabled = enabled;
        cacheExpiry = expiryMs;
        maxCacheSize = maxSize;
        debugCache = debug;

        if (!enabled) {
            clearCache();
        }

        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache configured - enabled: " + enabled +
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
                MMOSpawnPoint.getInstance().getLogger().warning("Unknown material in unsafe-materials list: " + materialName);
            }
        }
        unsafeMaterials = materials;

        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Configured " + materials.size() + " unsafe materials");
        }
    }

    public static void configureBannedPassable(List<String> materialNames) {
        Set<Material> materials = new HashSet<>();
        for (String materialName : materialNames) {
            Material m = Material.matchMaterial(materialName);
            if (m != null) {
                materials.add(m);
            } else {
                MMOSpawnPoint.getInstance().getLogger().warning("Unknown material in banned-passable list: " + materialName);
            }
        }
        bannedPassable = materials;
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Configured " + materials.size() + " banned passable materials");
        }
    }

    public static void configureOverworldYStrategy(String strategy) {
        try {
            overworldYStrategy = OverworldYStrategy.valueOf(strategy.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            overworldYStrategy = OverworldYStrategy.MIXED;
        }
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Overworld Y strategy: " + overworldYStrategy);
        }
    }

    public static void configureOverworldYRatio(double ratio) {
        highestBlockYAttemptRatio = Math.max(0.0, Math.min(1.0, ratio));
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Overworld Y ratio (MIXED): " + highestBlockYAttemptRatio);
        }
    }

    public static void configureSearchRadius(int radius) {
        baseSearchRadius = Math.max(1, radius);
        maxSearchRadius = Math.max(baseSearchRadius, baseSearchRadius * 4);
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Search radius configured - base: " + baseSearchRadius + ", cap: " + maxSearchRadius);
        }
    }

    // --------------- Public API (with caching) ----------------

    public static Location findSafeLocation(Location baseLocation, int maxAttempts, UUID playerId,
                                            boolean useCache, boolean playerSpecific) {
        totalSearches++;

        if (baseLocation == null || !cacheEnabled || !useCache) {
            return findSafeLocationNoCache(baseLocation, maxAttempts);
        }

        World world = baseLocation.getWorld();
        if (world == null) return null;

        CacheKey key = new CacheKey(
                "fixed", world.getName(),
                (int) baseLocation.getX(), (int) baseLocation.getY(), (int) baseLocation.getZ(),
                0, 0, 0, 0, 0, 0,
                playerId, playerSpecific
        );

        return getCachedOrCompute(key, () -> findSafeLocationNoCache(baseLocation, maxAttempts));
    }

    public static Location findSafeLocationInRegion(double minX, double maxX, double minY, double maxY,
                                                    double minZ, double maxZ, World world, int maxAttempts,
                                                    UUID playerId, boolean useCache, boolean playerSpecific) {
        totalSearches++;

        if (world == null || !cacheEnabled || !useCache) {
            return findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts);
        }

        CacheKey key = new CacheKey(
                "region", world.getName(),
                0, 0, 0,
                (int) minX, (int) maxX, (int) minY, (int) maxY, (int) minZ, (int) maxZ,
                playerId, playerSpecific
        );

        return getCachedOrCompute(key, () -> findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts));
    }

    // --------------- Cache internals ----------------

    private static Location getCachedOrCompute(CacheKey key, Supplier<Location> supplier) {
        Location cached = getCachedLocation(key);
        if (cached != null) {
            cacheHits++;
            if (debugCache) {
                MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache HIT for key: " + key);
                logCacheStatistics();
            }
            return cached.clone();
        }

        cacheMisses++;
        Location safe = supplier.get();
        if (safe != null) {
            cacheLocation(key, safe);
            if (debugCache) {
                MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache STORE for key: " + key);
                logCacheStatistics();
            }
        }
        return safe;
    }

    private static Location getCachedLocation(CacheKey key) {
        Long ts = CACHE_TIMESTAMPS.get(key);
        if (ts == null) return null;

        if (System.currentTimeMillis() - ts < cacheExpiry) {
            return SAFE_LOCATION_CACHE.get(key);
        } else {
            SAFE_LOCATION_CACHE.remove(key);
            CACHE_TIMESTAMPS.remove(key);
            return null;
        }
    }

    private static void cacheLocation(CacheKey key, Location location) {
        if (SAFE_LOCATION_CACHE.size() >= maxCacheSize) {
            cleanOldestCacheEntries();
        }
        SAFE_LOCATION_CACHE.put(key, location.clone());
        CACHE_TIMESTAMPS.put(key, System.currentTimeMillis());
    }

    private static void cleanOldestCacheEntries() {
        try {
            int entriesToRemove = Math.max(1, maxCacheSize / 5);
            List<CacheKey> keysToRemove = CACHE_TIMESTAMPS.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .limit(entriesToRemove)
                    .map(Map.Entry::getKey)
                    .toList();

            for (CacheKey key : keysToRemove) {
                SAFE_LOCATION_CACHE.remove(key);
                CACHE_TIMESTAMPS.remove(key);
            }

            if (debugCache) {
                MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cleaned " + keysToRemove.size() + " oldest cache entries");
            }
        } catch (Exception e) {
            MMOSpawnPoint.getInstance().getLogger().warning("[SafeLocationFinder] Error cleaning cache entries: " + e.getMessage());
        }
    }

    private static void logCacheStatistics() {
        double hitRate = totalSearches > 0 ? (cacheHits * 100.0) / totalSearches : 0;
        MMOSpawnPoint.getInstance().getLogger().info(String.format(
                "[SafeLocationFinder] Stats: %d searches, %d hits, %d misses, %.1f%% hit rate, %d cached",
                totalSearches, cacheHits, cacheMisses, hitRate, SAFE_LOCATION_CACHE.size()
        ));
    }

    public static void clearPlayerCache(UUID playerId) {
        if (playerId == null) return;
        int removed = 0;

        for (CacheKey key : new ArrayList<>(SAFE_LOCATION_CACHE.keySet())) {
            if (key.playerSpecific && playerId.equals(key.playerId)) {
                SAFE_LOCATION_CACHE.remove(key);
                CACHE_TIMESTAMPS.remove(key);
                removed++;
            }
        }
        if (debugCache && removed > 0) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cleared " + removed + " cache entries for player: " + playerId);
        }
    }

    public static void clearCache() {
        SAFE_LOCATION_CACHE.clear();
        CACHE_TIMESTAMPS.clear();
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache cleared");
        }
    }

    // --------------- Core search (no cache) ----------------

    private static Location findSafeLocationNoCache(Location baseLocation, int maxAttempts) {
        if (baseLocation == null) return null;

        World world = baseLocation.getWorld();
        if (world == null) return null;

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

        double centerX = (minX + maxX) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        // Overworld branch with strategy
        if (!isNether) {
            int attemptsHighest = 0;
            int attemptsRandom = 0;

            switch (overworldYStrategy) {
                case HIGHEST_ONLY -> attemptsHighest = maxAttempts;
                case HIGHEST_FIRST -> attemptsHighest = maxAttempts;
                case MIXED -> {
                    attemptsHighest = (int) Math.round(maxAttempts * highestBlockYAttemptRatio);
                    attemptsHighest = Math.max(0, Math.min(maxAttempts, attemptsHighest));
                    attemptsRandom = Math.max(0, maxAttempts - attemptsHighest);
                }
            }

            // First quick check at region center (highest)
            int centerHy = world.getHighestBlockYAt((int) centerX, (int) centerZ);
            Location center = new Location(world, centerX, centerHy + 1.0, centerZ);
            if (isSafeLocation(center)) return center.clone();

            // Highest-block attempts
            for (int i = 0; i < attemptsHighest; i++) {
                double x = minX + RANDOM.nextDouble() * (maxX - minX);
                double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                int hy = world.getHighestBlockYAt((int) x, (int) z);
                Location loc = new Location(world, x, hy + 1.0, z);
                if (isSafeLocation(loc)) return loc.clone();
            }

            // Random-Y attempts
            for (int i = 0; i < attemptsRandom; i++) {
                double x = minX + RANDOM.nextDouble() * (maxX - minX);
                double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                double y = minY + RANDOM.nextDouble() * Math.max(1.0, (maxY - minY));
                Location loc = new Location(world, x, y, z);
                if (isSafeLocation(loc)) return loc.clone();
            }

            // Fallback
            return fallback(world, minX, maxX, minY, maxY, minZ, maxZ, centerX, centerZ, false);
        }

        // Nether (unchanged logic with vertical scan)
        int centerHighestY = findSafeYInNether(world, (int) centerX, (int) centerZ, (int) maxY, (int) minY);
        Location centerLoc = new Location(world, centerX, centerHighestY + 1.0, centerZ);
        if (isSafeLocation(centerLoc)) {
            return centerLoc.clone();
        }

        int attempts = 0;
        while (attempts < maxAttempts) {
            attempts++;

            double x = minX + RANDOM.nextDouble() * (maxX - minX);
            double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);

            double y = findSafeYInNether(world, (int) x, (int) z, (int) maxY, (int) minY) + 1.0;

            Location location = new Location(world, x, y, z);
            if (isSafeLocation(location)) {
                return location.clone();
            }
        }
        return fallback(world, minX, maxX, minY, maxY, minZ, maxZ, centerX, centerZ, true);
    }

    private static Location fallback(World world, double minX, double maxX, double minY, double maxY,
                                     double minZ, double maxZ, double centerX, double centerZ, boolean isNether) {
        Location worldSpawn = world.getSpawnLocation();
        if (isWithinBounds(worldSpawn, minX, maxX, minY, maxY, minZ, maxZ)) {
            Location spawnCandidate = worldSpawn.clone();
            int hy = world.getHighestBlockYAt(spawnCandidate.getBlockX(), spawnCandidate.getBlockZ());
            spawnCandidate.setY(hy + 1.0);
            return spawnCandidate;
        }

        int hyCenter = isNether
                ? findSafeYInNether(world, (int) centerX, (int) centerZ, world.getMaxHeight(), 0)
                : world.getHighestBlockYAt((int) centerX, (int) centerZ);
        return new Location(world, centerX, hyCenter + 1.0, centerZ);
    }

    private static int findSafeYInNether(World world, int x, int z, int maxY, int minY) {
        int startY = (maxY + minY) / 2;

        for (int y = startY; y >= minY; y--) {
            if (isSolidWithTwoPassableAbove(world, x, y, z)) return y;
        }
        for (int y = startY + 1; y <= Math.max(minY + 2, maxY - 2); y++) {
            if (isSolidWithTwoPassableAbove(world, x, y, z)) return y;
        }
        return 64;
    }

    private static boolean isSolidWithTwoPassableAbove(World world, int x, int y, int z) {
        Block ground = world.getBlockAt(x, y, z);
        Block feet = world.getBlockAt(x, y + 1, z);
        Block head = world.getBlockAt(x, y + 2, z);

        return ground.getType().isSolid()
                && !unsafeMaterials.contains(ground.getType())
                && isPassableSafe(feet)
                && isPassableSafe(head);
    }

    private static boolean isWithinBounds(Location loc, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    private static boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().subtract(0, 1, 0).getBlock();

        if (!isPassableSafe(feet) || !isPassableSafe(head)) {
            return false;
        }
        return ground.getType().isSolid() && !unsafeMaterials.contains(ground.getType());
    }

    private static boolean isPassableSafe(Block block) {
        Material type = block.getType();
        return block.isPassable() && !bannedPassable.contains(type);
    }
}