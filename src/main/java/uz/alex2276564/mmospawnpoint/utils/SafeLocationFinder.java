package uz.alex2276564.mmospawnpoint.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SafeLocationFinder {
    private static final Random RANDOM = new Random();

    private static final int DEFAULT_NETHER_Y = 64; // Fallback Y for Nether

    // Global config-driven sets
    private static Set<Material> globalGroundBlacklist = new HashSet<>();
    private static Set<Material> globalPassableBlacklist = new HashSet<>();

    // Per-call override (ground whitelist). If set and non-empty, globalGroundBlacklist is ignored.
    private static final ThreadLocal<Set<Material>> GROUND_WHITELIST_TL = new ThreadLocal<>();

    // Search settings
    private static int baseSearchRadius = 5;
    private static int maxSearchRadius = 20;

    // Cache settings
    private static boolean cacheEnabled = true;
    private static long cacheExpiry = 300000; // 5 min
    private static int maxCacheSize = 1000;
    private static boolean debugCache = false;

    // Overworld Y selection
    private enum OverworldYMode {MIXED, HIGHEST_ONLY, RANDOM_ONLY}

    private enum MixedFirstGroup {HIGHEST, RANDOM}

    private static OverworldYMode yMode = OverworldYMode.MIXED;
    private static MixedFirstGroup mixedFirstGroup = MixedFirstGroup.HIGHEST;
    private static double mixedFirstShare = 0.6; // fraction [0..1] of attempts for FIRST group in MIXED

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
        if (!enabled) clearCache();

        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache configured - enabled: " + enabled +
                    ", expiry: " + (expiryMs / 1000) + "s, maxSize: " + maxSize);
        }
    }

    public static void configureGlobalGroundBlacklist(List<String> materialNames) {
        Set<Material> materials = new HashSet<>();
        for (String name : materialNames) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                materials.add(m);
            } else {
                MMOSpawnPoint.getInstance().getLogger().warning("Unknown material in globalGroundBlacklist: " + name);
            }
        }
        globalGroundBlacklist = materials;
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Configured " + materials.size() + " ground blacklist materials");
        }
    }

    public static void configureGlobalPassableBlacklist(List<String> materialNames) {
        Set<Material> materials = new HashSet<>();
        for (String name : materialNames) {
            Material m = Material.matchMaterial(name);
            if (m != null) materials.add(m);
            else
                MMOSpawnPoint.getInstance().getLogger().warning("Unknown material in globalPassableBlacklist: " + name);
        }
        globalPassableBlacklist = materials;
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Configured " + materials.size() + " passable blacklist materials");
        }
    }

    public static void configureSearchRadius(int radius) {
        baseSearchRadius = Math.max(1, radius);
        maxSearchRadius = Math.max(baseSearchRadius, baseSearchRadius * 4);
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Search radius configured - base: " + baseSearchRadius + ", cap: " + maxSearchRadius);
        }
    }

    /**
     * Configure overworld Y selection strategy for region safe search.
     *
     * @param mode       "mixed" | "highest_only" | "random_only"
     * @param first      (for MIXED) "highest" | "random" — which group of attempts is executed first
     * @param firstShare (for MIXED) fraction [0..1] of total attempts assigned to the FIRST group
     */
    public static void configureOverworldYSelection(String mode, String first, double firstShare) {
        // Mode
        if (mode == null) mode = "mixed";
        switch (mode.toLowerCase(Locale.ROOT)) {
            case "highest_only" -> yMode = OverworldYMode.HIGHEST_ONLY;
            case "random_only" -> yMode = OverworldYMode.RANDOM_ONLY;
            default -> yMode = OverworldYMode.MIXED;
        }

        // First group (for MIXED)
        if (first == null) first = "highest";
        mixedFirstGroup = "random".equalsIgnoreCase(first)
                ? MixedFirstGroup.RANDOM
                : MixedFirstGroup.HIGHEST;

        // First group share (for MIXED)
        mixedFirstShare = (Double.isNaN(firstShare) || Double.isInfinite(firstShare)) ? 0.6 : firstShare;
        mixedFirstShare = Math.max(0.0, Math.min(1.0, mixedFirstShare));

        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Y-selection: mode=" + yMode
                    + ", first=" + mixedFirstGroup + ", firstShare=" + mixedFirstShare);
        }
    }

// --------------- Public API (with caching) ----------------

    public static Location findSafeLocation(Location baseLocation, int maxAttempts, UUID playerId,
                                            boolean useCache, boolean playerSpecific) {
        return findSafeLocationWithWhitelist(baseLocation, maxAttempts, playerId, useCache, playerSpecific, null);
    }

    public static Location findSafeLocationWithWhitelist(Location baseLocation, int maxAttempts, UUID playerId,
                                                         boolean useCache, boolean playerSpecific,
                                                         Set<Material> groundWhitelist) {
        totalSearches++;

        if (baseLocation == null || !cacheEnabled || !useCache) {
            return withWhitelist(groundWhitelist, () -> findSafeLocationNoCache(baseLocation, maxAttempts));
        }

        World world = baseLocation.getWorld();
        if (world == null) return null;

        CacheKey key = new CacheKey(
                "fixed", world.getName(),
                (int) baseLocation.getX(), (int) baseLocation.getY(), (int) baseLocation.getZ(),
                0, 0, 0, 0, 0, 0,
                playerId, playerSpecific
        );

        return getCachedOrCompute(key, () -> withWhitelist(groundWhitelist, () -> findSafeLocationNoCache(baseLocation, maxAttempts)));
    }

    public static Location findSafeLocationInRegion(double minX, double maxX, double minY, double maxY,
                                                    double minZ, double maxZ, World world, int maxAttempts,
                                                    UUID playerId, boolean useCache, boolean playerSpecific) {
        return findSafeLocationInRegionWithWhitelist(minX, maxX, minY, maxY, minZ, maxZ, world,
                maxAttempts, playerId, useCache, playerSpecific, null);
    }

    public static Location findSafeLocationInRegionWithWhitelist(double minX, double maxX, double minY, double maxY,
                                                                 double minZ, double maxZ, World world, int maxAttempts,
                                                                 UUID playerId, boolean useCache, boolean playerSpecific,
                                                                 Set<Material> groundWhitelist) {
        totalSearches++;

        if (world == null || !cacheEnabled || !useCache) {
            return withWhitelist(groundWhitelist, () -> findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts));
        }

        CacheKey key = new CacheKey(
                "region", world.getName(),
                0, 0, 0,
                (int) minX, (int) maxX, (int) minY, (int) maxY, (int) minZ, (int) maxZ,
                playerId, playerSpecific
        );

        return getCachedOrCompute(key, () -> withWhitelist(groundWhitelist, () -> findSafeLocationInRegionNoCache(minX, maxX, minY, maxY, minZ, maxZ, world, maxAttempts)));
    }

    private static <T> T withWhitelist(Set<Material> wl, Supplier<T> supplier) {
        if (wl == null || wl.isEmpty()) {
            return supplier.get();
        }
        GROUND_WHITELIST_TL.set(wl);
        try {
            return supplier.get();
        } finally {
            GROUND_WHITELIST_TL.remove();
        }
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
                highestY = findSafeYInNether(
                        world,
                        testLocation.getBlockX(),
                        testLocation.getBlockZ(),
                        world.getMaxHeight(),
                        resolveMinY(world)
                );
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

        // Try center using highest Y first
        int centerHy = isNether
                ? findSafeYInNether(world, (int) centerX, (int) centerZ, (int) maxY, (int) minY)
                : world.getHighestBlockYAt((int) centerX, (int) centerZ);
        Location center = new Location(world, centerX, centerHy + 1.0, centerZ);
        if (!isNether) {
            center.setY(clamp(center.getY(), minY, maxY));
        }
        if (isSafeLocation(center)) return center.clone();

        if (isNether) {
            int attempts = 0;
            while (attempts < maxAttempts) {
                attempts++;
                double x = minX + RANDOM.nextDouble() * (maxX - minX);
                double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                double safeY = findSafeYInNether(world, (int) x, (int) z, (int) maxY, (int) minY) + 1.0;
                Location loc = new Location(world, x, safeY, z);
                if (isSafeLocation(loc)) return loc.clone();
            }
            return fallback(world, minX, maxX, minY, maxY, minZ, maxZ, centerX, centerZ, true);
        }

        // Overworld: Y-selection logic
        int total = Math.max(1, maxAttempts);

        switch (yMode) {
            case HIGHEST_ONLY -> {
                for (int i = 0; i < total; i++) {
                    double x = minX + RANDOM.nextDouble() * (maxX - minX);
                    double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                    int hy = world.getHighestBlockYAt((int) x, (int) z);
                    double y = clamp(hy + 1.0, minY, maxY);
                    Location loc = new Location(world, x, y, z);
                    if (isSafeLocation(loc)) return loc.clone();
                }
                return fallback(world, minX, maxX, minY, maxY, minZ, maxZ, centerX, centerZ, false);
            }
            case RANDOM_ONLY -> {
                for (int i = 0; i < total; i++) {
                    double x = minX + RANDOM.nextDouble() * (maxX - minX);
                    double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                    double y = minY + RANDOM.nextDouble() * Math.max(1.0, (maxY - minY));
                    Location loc = new Location(world, x, y, z);
                    if (isSafeLocation(loc)) return loc.clone();
                }
                return fallback(world, minX, maxX, minY, maxY, minZ, maxZ, centerX, centerZ, false);
            }
            case MIXED -> {
                int firstCount = (int) Math.round(total * mixedFirstShare);
                firstCount = Math.max(0, Math.min(total, firstCount));
                int secondCount = total - firstCount;

                if (mixedFirstGroup == MixedFirstGroup.HIGHEST) {
                    // Highest first
                    for (int i = 0; i < firstCount; i++) {
                        double x = minX + RANDOM.nextDouble() * (maxX - minX);
                        double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                        int hy = world.getHighestBlockYAt((int) x, (int) z);
                        double y = clamp(hy + 1.0, minY, maxY);
                        Location loc = new Location(world, x, y, z);
                        if (isSafeLocation(loc)) return loc.clone();
                    }
                    for (int i = 0; i < secondCount; i++) {
                        double x = minX + RANDOM.nextDouble() * (maxX - minX);
                        double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                        double y = minY + RANDOM.nextDouble() * Math.max(1.0, (maxY - minY));
                        Location loc = new Location(world, x, y, z);
                        if (isSafeLocation(loc)) return loc.clone();
                    }
                } else {
                    // Random first
                    for (int i = 0; i < firstCount; i++) {
                        double x = minX + RANDOM.nextDouble() * (maxX - minX);
                        double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                        double y = minY + RANDOM.nextDouble() * Math.max(1.0, (maxY - minY));
                        Location loc = new Location(world, x, y, z);
                        if (isSafeLocation(loc)) return loc.clone();
                    }
                    for (int i = 0; i < secondCount; i++) {
                        double x = minX + RANDOM.nextDouble() * (maxX - minX);
                        double z = minZ + RANDOM.nextDouble() * (maxZ - minZ);
                        int hy = world.getHighestBlockYAt((int) x, (int) z);
                        double y = clamp(hy + 1.0, minY, maxY);
                        Location loc = new Location(world, x, y, z);
                        if (isSafeLocation(loc)) return loc.clone();
                    }
                }
                return fallback(world, minX, maxX, minY, maxY, minZ, maxZ, centerX, centerZ, false);
            }
        }

        // Should never reach here
        return fallback(world, minX, maxX, minY, maxY, minZ, maxZ, centerX, centerZ, false);
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
                ? findSafeYInNether(world, (int) centerX, (int) centerZ, world.getMaxHeight(), resolveMinY(world))
                : world.getHighestBlockYAt((int) centerX, (int) centerZ);
        return new Location(world, centerX, hyCenter + 1.0, centerZ);
    }

    private static int findSafeYInNether(World world, int x, int z, int maxY, int minY) {
        // Clamp provided bounds to world limits
        int worldMinY = resolveMinY(world);
        int worldMaxY = Math.max(worldMinY, world.getMaxHeight() - 1);

        int min = Math.max(worldMinY, Math.min(minY, worldMaxY));
        int max = Math.max(worldMinY, Math.min(maxY, worldMaxY));
        if (min > max) {
            int tmp = min; min = max; max = tmp;
        }

        int startY = (max + min) / 2;

        // Downward scan
        for (int y = startY; y >= min; y--) {
            if (isSolidWithTwoPassableAbove(world, x, y, z)) return y;
        }

        // Upward scan with headroom
        int upper = Math.max(min + 2, max - 2);
        if (startY + 1 <= upper) {
            for (int y = startY + 1; y <= upper; y++) {
                if (isSolidWithTwoPassableAbove(world, x, y, z)) return y;
            }
        }

        // Fallback
        return DEFAULT_NETHER_Y;
    }

    private static int resolveMinY(World world) {
        try {
            // Paper 1.18+ has World#getMinHeight()
            return (int) World.class.getMethod("getMinHeight").invoke(world);
        } catch (NoSuchMethodException ignored) {
            // 1.16.5 and older: no negative Y
            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static boolean isSolidWithTwoPassableAbove(World world, int x, int y, int z) {
        Block ground = world.getBlockAt(x, y, z);
        Block feet = world.getBlockAt(x, y + 1, z);
        Block head = world.getBlockAt(x, y + 2, z);

        return ground.getType().isSolid()
                && groundAllowed(ground.getType())
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

        if (!isPassableSafe(feet) || !isPassableSafe(head)) return false;
        return ground.getType().isSolid() && groundAllowed(ground.getType());
    }

    private static boolean isPassableSafe(Block block) {
        Material type = block.getType();
        return block.isPassable() && !globalPassableBlacklist.contains(type);
    }

    private static boolean groundAllowed(Material groundType) {
        Set<Material> wl = GROUND_WHITELIST_TL.get();
        if (wl != null && !wl.isEmpty()) {
            // When whitelist is set, it overrides the global blacklist
            return wl.contains(groundType);
        }
        // Otherwise, use global blacklist
        return !globalGroundBlacklist.contains(groundType);
    }

    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max) ? max : v;
    }

    public static final class SafeLocationFinderExports {
        public record Snapshot(long searches, long hits, long misses, int size, boolean enabled, long expirySeconds,
                               int maxSize) {
        }

        public static Snapshot snapshot() {
            return new Snapshot(
                    totalSearches, cacheHits, cacheMisses,
                    SAFE_LOCATION_CACHE.size(), cacheEnabled, cacheExpiry / 1000L, maxCacheSize
            );
        }
    }
}