package uz.alex2276564.mmospawnpoint.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class SafeLocationFinder {

    // Cache via Caffeine
    private static Cache<@NotNull CacheKey, Location> CACHE;

    // Global config-driven sets (fast membership)
    private static Set<Material> globalGroundBlacklist = EnumSet.noneOf(Material.class);
    private static Set<Material> globalPassableBlacklist = EnumSet.noneOf(Material.class);

    // Per-call override (ground whitelist). If set and non-empty, globalGroundBlacklist is ignored.
    private static final ThreadLocal<Set<Material>> GROUND_WHITELIST_TL = new ThreadLocal<>();

    // Cache settings (keep for snapshot)
    private static boolean cacheEnabled = true;
    private static long cacheExpiry = 300000; // ms
    private static int maxCacheSize = 1000;
    private static boolean debugCache = false;

    // Overworld Y selection
    private enum OverworldYMode {MIXED, HIGHEST_ONLY, RANDOM_ONLY}

    private enum MixedFirstGroup {HIGHEST, RANDOM}

    private static OverworldYMode yMode = OverworldYMode.MIXED;
    private static MixedFirstGroup mixedFirstGroup = MixedFirstGroup.HIGHEST;
    private static double mixedFirstShare = 0.6; // fraction [0..1] of attempts for FIRST group in MIXED

    // Stats (for snapshot)
    private static final AtomicLong totalSearches = new AtomicLong(0);

    // Nether fallback Y
    private static final int DEFAULT_NETHER_Y = 64;

    // Type-safe cache key
    private record CacheKey(
            String type, String worldName,
            int x, int y, int z,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
            UUID playerId, boolean playerSpecific
    ) {
    }

    // Lightweight fail tags for debug diagnostics
    public enum FailTag {
        FEET_NOT_PASSABLE,
        HEAD_NOT_PASSABLE,
        GROUND_NOT_SOLID,
        GROUND_BLACKLISTED,
        GROUND_NOT_WHITELISTED
    }

    private static final ThreadLocal<FailTag> TL_LAST_FAIL = new ThreadLocal<>();

    // --------------- Configuration ----------------

    public static void configureCaching(boolean enabled, long expiryMs, int maxSize, boolean debug) {
        cacheEnabled = enabled;
        cacheExpiry = expiryMs;
        maxCacheSize = maxSize;
        debugCache = debug;

        if (!enabled) {
            if (CACHE != null) CACHE.invalidateAll();
            return;
        }

        CACHE = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expiryMs, TimeUnit.MILLISECONDS)
                .recordStats()
                .build();

        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info(
                    "[SafeLocationFinder] Cache configured - enabled: " + cacheEnabled +
                            ", expiry: " + (expiryMs / 1000) + "s, maxSize: " + maxSize
            );
        }
    }

    public static void configureGlobalGroundBlacklist(List<String> materialNames) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
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
        Set<Material> materials = EnumSet.noneOf(Material.class);
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
        // Search settings
        int baseSearchRadius = Math.max(1, radius);
        int maxSearchRadius = Math.max(baseSearchRadius, baseSearchRadius * 4);
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

    // --------------- Cache public helpers ----------------

    public static void clearPlayerCache(UUID playerId) {
        if (CACHE == null || playerId == null) return;
        CACHE.asMap().keySet().removeIf(k -> k.playerSpecific && playerId.equals(k.playerId));
    }

    public static void clearCache() {
        if (CACHE != null) CACHE.invalidateAll();
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Cache cleared");
        }
    }

    // --------------- Attempt (single-step) API ----------------

    public static Location attemptFindSafeInRegionSingle(World world,
                                                         double minX, double maxX,
                                                         double minY, double maxY,
                                                         double minZ, double maxZ,
                                                         java.util.Set<org.bukkit.Material> groundWhitelist) {
        return withWhitelist(groundWhitelist, () -> {
            double x = minX + java.util.concurrent.ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxX - minX)));
            double z = minZ + java.util.concurrent.ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxZ - minZ)));

            boolean isNether = world.getEnvironment() == World.Environment.NETHER;
            double y;
            if (isNether) {
                int hy = findSafeYInNether(world, (int) Math.floor(x), (int) Math.floor(z), world.getMaxHeight(), resolveMinY(world));
                y = hy + 1.0;
            } else {
                int modePick = (yMode == OverworldYMode.HIGHEST_ONLY) ? 0 :
                        (yMode == OverworldYMode.RANDOM_ONLY) ? 1 :
                                (java.util.concurrent.ThreadLocalRandom.current().nextDouble() < mixedFirstShare
                                        ? (mixedFirstGroup == MixedFirstGroup.HIGHEST ? 0 : 1)
                                        : (mixedFirstGroup == MixedFirstGroup.HIGHEST ? 1 : 0));
                if (modePick == 0) {
                    int hy = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
                    y = clamp(hy + 1.0, minY, maxY);
                } else {
                    y = minY + java.util.concurrent.ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxY - minY)));
                }
            }
            Location loc = new Location(world, x, y, z);
            return isSafeLocation(loc) ? loc.clone() : null;
        });
    }

    public static Location attemptFindSafeNearSingle(Location baseLocation,
                                                     int radius,
                                                     java.util.Set<org.bukkit.Material> groundWhitelist) {
        return withWhitelist(groundWhitelist, () -> {
            if (baseLocation == null || baseLocation.getWorld() == null) return null;
            World world = baseLocation.getWorld();
            boolean isNether = world.getEnvironment() == World.Environment.NETHER;

            double offsetX = java.util.concurrent.ThreadLocalRandom.current().nextDouble(-radius, radius);
            double offsetZ = java.util.concurrent.ThreadLocalRandom.current().nextDouble(-radius, radius);
            Location test = baseLocation.clone().add(offsetX, 0.0, offsetZ);

            double y;
            if (isNether) {
                int hy = findSafeYInNether(world, test.getBlockX(), test.getBlockZ(), world.getMaxHeight(), resolveMinY(world));
                y = hy + 1.0;
            } else {
                int hy = world.getHighestBlockYAt(test.getBlockX(), test.getBlockZ());
                y = hy + 1.0;
            }
            test.setY(y);
            return isSafeLocation(test) ? test.clone() : null;
        });
    }

    // --------------- Core helpers ----------------

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

    private static int resolveMinY(World world) {
        try {
            return (int) World.class.getMethod("getMinHeight").invoke(world);
        } catch (Throwable ignored) {
            return 0; // 1.16.5 and older
        }
    }

    private static int findSafeYInNether(World world, int x, int z, int maxY, int minY) {
        // Clamp provided bounds to world limits
        int worldMinY = resolveMinY(world);
        int worldMaxY = Math.max(worldMinY, world.getMaxHeight() - 1);

        int min = Math.max(worldMinY, Math.min(minY, worldMaxY));
        int max = Math.max(worldMinY, Math.min(maxY, worldMaxY));
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
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

    private static boolean isSolidWithTwoPassableAbove(World world, int x, int y, int z) {
        Block ground = world.getBlockAt(x, y, z);
        Block feet = world.getBlockAt(x, y + 1, z);
        Block head = world.getBlockAt(x, y + 2, z);

        return ground.getType().isSolid()
                && groundAllowed(ground.getType())
                && isPassableSafe(feet)
                && isPassableSafe(head);
    }

    public static boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().subtract(0, 1, 0).getBlock();

        if (!isPassableSafe(feet)) {
            TL_LAST_FAIL.set(FailTag.FEET_NOT_PASSABLE);
            return false;
        }
        if (!isPassableSafe(head)) {
            TL_LAST_FAIL.set(FailTag.HEAD_NOT_PASSABLE);
            return false;
        }
        if (!ground.getType().isSolid()) {
            TL_LAST_FAIL.set(FailTag.GROUND_NOT_SOLID);
            return false;
        }
        if (!groundAllowed(ground.getType())) {
            return false;
        }
        return true;
    }

    private static boolean isPassableSafe(Block block) {
        Material type = block.getType();
        return block.isPassable() && !globalPassableBlacklist.contains(type);
    }

    private static boolean groundAllowed(Material groundType) {
        Set<Material> wl = GROUND_WHITELIST_TL.get();
        if (wl != null && !wl.isEmpty()) {
            // When whitelist is set, it overrides the global blacklist
            if (!wl.contains(groundType)) {
                TL_LAST_FAIL.set(FailTag.GROUND_NOT_WHITELISTED);
                return false;
            }
            return true;
        }
        // Otherwise, use global blacklist
        if (globalGroundBlacklist.contains(groundType)) {
            TL_LAST_FAIL.set(FailTag.GROUND_BLACKLISTED);
            return false;
        }
        return true;
    }

    public static FailTag pollLastFailTag() {
        FailTag t = TL_LAST_FAIL.get();
        TL_LAST_FAIL.remove();
        return t;
    }

    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max) ? max : v;
    }

    // --------------- Snapshot ----------------

    public static final class SafeLocationFinderExports {
        public record Snapshot(long searches, long hits, long misses, int size, boolean enabled, long expirySeconds,
                               int maxSize) {
        }

        public static Snapshot snapshot() {
            CacheStats s = (CACHE != null) ? CACHE.stats() : CacheStats.empty();
            int size = (CACHE != null) ? CACHE.asMap().size() : 0;
            return new Snapshot(
                    totalSearches.get(),
                    s.hitCount(),
                    s.missCount(),
                    size,
                    cacheEnabled,
                    cacheExpiry / 1000L,
                    maxCacheSize
            );
        }
    }
}