package uz.alex2276564.mmospawnpoint.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
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

    // Dimension-aware Y selection
    private enum DimYMode { MIXED, HIGHEST_ONLY, RANDOM_ONLY }
    private enum NetherMode { SCAN, HIGHEST_ONLY, RANDOM_ONLY }

    // Overworld
    private static DimYMode owMode = DimYMode.MIXED;
    private static MixedFirstGroup owFirst = MixedFirstGroup.HIGHEST;
    private static double owShare = 0.6;

    // End
    private static DimYMode endMode = DimYMode.HIGHEST_ONLY;
    private static MixedFirstGroup endFirst = MixedFirstGroup.HIGHEST;
    private static double endShare = 0.6;

    // Nether
    private static NetherMode netherMode = NetherMode.SCAN;
    private static boolean netherRespectRange = false;

    // Custom (Environment.CUSTOM)
    private static DimYMode customMode = DimYMode.MIXED;
    private static MixedFirstGroup customFirst = MixedFirstGroup.HIGHEST;
    private static double customShare = 0.6;

    private enum MixedFirstGroup {HIGHEST, RANDOM}

    // Stats (for snapshot)
    private static final AtomicLong totalSearches = new AtomicLong(0);

    // Nether fallback Y
    private static final int DEFAULT_NETHER_Y = 64;

    private static final ConcurrentHashMap<String, Integer> MIN_Y_CACHE = new ConcurrentHashMap<>();

    // Type-safe cache key
    private record CacheKey(
            String type, String worldName,
            int x, int y, int z,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
            UUID playerId, boolean playerSpecific,
            String ySignature, int wlHash
    ) {}

    // Lightweight fail tags for debug diagnostics
    public enum FailReason {
        FEET_NOT_PASSABLE,
        HEAD_NOT_PASSABLE,
        GROUND_NOT_SOLID,
        GROUND_BLACKLISTED,
        GROUND_NOT_WHITELISTED
    }

    private static final ThreadLocal<FailReason> TL_LAST_FAIL = new ThreadLocal<>();

    /**
     * @param mode         mode: "mixed"|"highest_only"|"random_only" or "scan"
     * @param first        for mixed only
     * @param respectRange nether-only
     */ // Per-call Y override (thread-local), similar to whitelist TL
        public record YSelectionOverride(String mode, String first, Double firstShare, Boolean respectRange) {
            public YSelectionOverride(String mode, String first, Double firstShare, Boolean respectRange) {
                // mode: "mixed"|"highest_only"|"random_only" or "scan"
                this.mode = (mode == null) ? null : mode.toLowerCase(Locale.ROOT);
                // for mixed only
                this.first = (first == null) ? null : first.toLowerCase(Locale.ROOT);
                this.firstShare = firstShare;
                // nether-only
                this.respectRange = respectRange;
            }
        }

    private static final ThreadLocal<YSelectionOverride> Y_OVERRIDE_TL = new ThreadLocal<>();

    public static <T> T withYSelectionOverride(@Nullable SafeLocationFinder.YSelectionOverride override, Supplier<T> supplier) {
        if (override == null) return supplier.get();
        Y_OVERRIDE_TL.set(override);
        try {
            return supplier.get();
        } finally {
            Y_OVERRIDE_TL.remove();
        }
    }

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

    /**
     * Applies pre-validated ground blacklist. Unknown/legacy materials must be filtered by validators.
     */
    public static void configureGlobalGroundBlacklist(List<String> materialNames) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : materialNames) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                materials.add(m);
            }
            // no logging here; validators handle diagnostics
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
            if (m != null) {
                materials.add(m);
            }
            // no logging here; validators handle diagnostics
        }
        globalPassableBlacklist = materials;
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Configured " + materials.size() + " passable blacklist materials");
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
        String m = mode == null ? "mixed" : mode.toLowerCase(Locale.ROOT);
        owMode = switch (m) {
            case "highest_only" -> DimYMode.HIGHEST_ONLY;
            case "random_only" -> DimYMode.RANDOM_ONLY;
            default -> DimYMode.MIXED;
        };
        String f = first == null ? "highest" : first.toLowerCase(Locale.ROOT);
        owFirst = "random".equals(f) ? MixedFirstGroup.RANDOM : MixedFirstGroup.HIGHEST;
        owShare = Double.isNaN(firstShare) || Double.isInfinite(firstShare) ? 0.6 : Math.max(0.0, Math.min(1.0, firstShare));
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Overworld Y-selection: " + owMode + " first=" + owFirst + " share=" + owShare);
        }
    }

    public static void configureEndYSelection(String mode, String first, double firstShare) {
        String m = mode == null ? "highest_only" : mode.toLowerCase(Locale.ROOT);
        endMode = switch (m) {
            case "mixed" -> DimYMode.MIXED;
            case "random_only" -> DimYMode.RANDOM_ONLY;
            default -> DimYMode.HIGHEST_ONLY;
        };
        String f = first == null ? "highest" : first.toLowerCase(Locale.ROOT);
        endFirst = "random".equals(f) ? MixedFirstGroup.RANDOM : MixedFirstGroup.HIGHEST;
        endShare = Double.isNaN(firstShare) || Double.isInfinite(firstShare) ? 0.6 : Math.max(0.0, Math.min(1.0, firstShare));
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] End Y-selection: " + endMode + " first=" + endFirst + " share=" + endShare);
        }
    }

    public static void configureNetherYSelection(String mode, boolean respectRange) {
        String m = mode == null ? "scan" : mode.toLowerCase(Locale.ROOT);
        netherMode = switch (m) {
            case "highest_only" -> NetherMode.HIGHEST_ONLY;
            case "random_only" -> NetherMode.RANDOM_ONLY;
            default -> NetherMode.SCAN;
        };
        netherRespectRange = respectRange;
        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] Nether Y-selection: " + netherMode + ", respectRange=" + netherRespectRange);
        }
    }

    /**
     * Configure CUSTOM-dimension Y selection strategy.
     *
     * @param mode       "mixed" | "highest_only" | "random_only"
     * @param first      (for MIXED) "highest" | "random"
     * @param firstShare (for MIXED) [0..1] share for the FIRST strategy
     */
    public static void configureCustomYSelection(String mode, String first, double firstShare) {
        String m = mode == null ? "mixed" : mode.toLowerCase(Locale.ROOT);
        customMode = switch (m) {
            case "highest_only" -> DimYMode.HIGHEST_ONLY;
            case "random_only" -> DimYMode.RANDOM_ONLY;
            default -> DimYMode.MIXED;
        };
        String f = first == null ? "highest" : first.toLowerCase(Locale.ROOT);
        customFirst = "random".equals(f) ? MixedFirstGroup.RANDOM : MixedFirstGroup.HIGHEST;
        customShare = Double.isNaN(firstShare) || Double.isInfinite(firstShare)
                ? 0.6
                : Math.max(0.0, Math.min(1.0, firstShare));

        if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info(
                    "[SafeLocationFinder] Custom Y-selection: " + customMode
                            + " first=" + customFirst + " share=" + customShare
            );
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

    public static void cleanup() {
        GROUND_WHITELIST_TL.remove();
        Y_OVERRIDE_TL.remove();
        TL_LAST_FAIL.remove();
    }

    // --------------- Attempt (single-step) API ----------------

    public static @Nullable Location attemptSafeInAreaOnce(World world,
                                                           double minX, double maxX,
                                                           double minY, double maxY,
                                                           double minZ, double maxZ,
                                                           Set<Material> groundWhitelist) {
        totalSearches.incrementAndGet();
        return withWhitelist(groundWhitelist, () -> {
            double x = minX + ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxX - minX)));
            double z = minZ + ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxZ - minZ)));

            World.Environment env = world.getEnvironment();
            double y;

            if (env == World.Environment.NETHER) {
                // Nether: choose policy (override → global)
                YSelectionOverride o = Y_OVERRIDE_TL.get();
                NetherMode modeUse = (o != null && o.mode != null)
                        ? switch (o.mode) {
                    case "highest_only" -> NetherMode.HIGHEST_ONLY;
                    case "random_only" -> NetherMode.RANDOM_ONLY;
                    default -> NetherMode.SCAN;
                }
                        : netherMode;

                boolean respect = (o != null && o.respectRange != null) ? o.respectRange : netherRespectRange;

                if (modeUse == NetherMode.SCAN) {
                    int hy = findSafeYInNether(world,
                            (int) Math.floor(x), (int) Math.floor(z),
                            respect ? (int) Math.floor(maxY) : world.getMaxHeight(),
                            respect ? (int) Math.floor(minY) : resolveMinY(world));
                    y = hy + 1.0;
                } else if (modeUse == NetherMode.HIGHEST_ONLY) {
                    int hy = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
                    y = clamp(hy + 1.0, minY, maxY);
                } else { // RANDOM_ONLY
                    y = minY + ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxY - minY)));
                }
            } else {
                // Overworld / End / Custom: choose dimension policy
                DimYMode dimMode;
                MixedFirstGroup dimFirst;
                double dimShare;

                if (env == World.Environment.THE_END) {
                    dimMode = endMode; dimFirst = endFirst; dimShare = endShare;
                    YSelectionOverride o = Y_OVERRIDE_TL.get();
                    if (o != null && o.mode != null) {
                        dimMode = switch (o.mode) {
                            case "random_only" -> DimYMode.RANDOM_ONLY;
                            case "mixed" -> DimYMode.MIXED;
                            default -> DimYMode.HIGHEST_ONLY;
                        };
                        if (dimMode == DimYMode.MIXED) {
                            if (o.first != null) {
                                dimFirst = "random".equals(o.first) ? MixedFirstGroup.RANDOM : MixedFirstGroup.HIGHEST;
                            }
                            if (o.firstShare != null) {
                                dimShare = Math.max(0.0, Math.min(1.0, o.firstShare));
                            }
                        }
                    }
                } else if (env == World.Environment.CUSTOM) {
                    dimMode = customMode; dimFirst = customFirst; dimShare = customShare;
                    YSelectionOverride o = Y_OVERRIDE_TL.get();
                    if (o != null && o.mode != null) {
                        dimMode = switch (o.mode) {
                            case "random_only" -> DimYMode.RANDOM_ONLY;
                            case "mixed" -> DimYMode.MIXED;
                            default -> DimYMode.HIGHEST_ONLY;
                        };
                        if (dimMode == DimYMode.MIXED) {
                            if (o.first != null) {
                                dimFirst = "random".equals(o.first) ? MixedFirstGroup.RANDOM : MixedFirstGroup.HIGHEST;
                            }
                            if (o.firstShare != null) {
                                dimShare = Math.max(0.0, Math.min(1.0, o.firstShare));
                            }
                        }
                    }
                } else {
                    // Overworld (NORMAL) and any other fallbacks
                    dimMode = owMode; dimFirst = owFirst; dimShare = owShare;
                    YSelectionOverride o = Y_OVERRIDE_TL.get();
                    if (o != null && o.mode != null) {
                        dimMode = switch (o.mode) {
                            case "random_only" -> DimYMode.RANDOM_ONLY;
                            case "mixed" -> DimYMode.MIXED;
                            default -> DimYMode.HIGHEST_ONLY;
                        };
                        if (dimMode == DimYMode.MIXED) {
                            if (o.first != null) {
                                dimFirst = "random".equals(o.first) ? MixedFirstGroup.RANDOM : MixedFirstGroup.HIGHEST;
                            }
                            if (o.firstShare != null) {
                                dimShare = Math.max(0.0, Math.min(1.0, o.firstShare));
                            }
                        }
                    }
                }

                if (dimMode == DimYMode.HIGHEST_ONLY) {
                    int hy = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
                    y = clamp(hy + 1.0, minY, maxY);
                } else if (dimMode == DimYMode.RANDOM_ONLY) {
                    y = minY + ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxY - minY)));
                } else { // MIXED
                    int firstPick = ThreadLocalRandom.current().nextDouble() < dimShare
                            ? (dimFirst == MixedFirstGroup.HIGHEST ? 0 : 1)
                            : (dimFirst == MixedFirstGroup.HIGHEST ? 1 : 0);
                    if (firstPick == 0) { // highest first
                        int hy = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));
                        y = clamp(hy + 1.0, minY, maxY);
                    } else { // random first
                        y = minY + ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxY - minY)));
                    }
                }
            }

            Location loc = new Location(world, x, y, z);
            return isSafeLocation(loc) ? loc.clone() : null;
        });
    }

    public static Location attemptSafeNearOnce(Location baseLocation,
                                               int radius,
                                               Set<Material> groundWhitelist) {
        totalSearches.incrementAndGet();
        return withWhitelist(groundWhitelist, () -> {
            if (baseLocation == null || baseLocation.getWorld() == null) return null;
            World world = baseLocation.getWorld();
            World.Environment env = world.getEnvironment();

            double offsetX = ThreadLocalRandom.current().nextDouble(-radius, radius);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-radius, radius);
            Location test = baseLocation.clone().add(offsetX, 0.0, offsetZ);

            double y;
            if (env == World.Environment.NETHER) {
                YSelectionOverride o = Y_OVERRIDE_TL.get();
                NetherMode modeUse = (o != null && o.mode != null)
                        ? switch (o.mode) {
                    case "highest_only" -> NetherMode.HIGHEST_ONLY;
                    case "random_only" -> NetherMode.RANDOM_ONLY;
                    default -> NetherMode.SCAN;
                }
                        : netherMode;

                if (modeUse == NetherMode.SCAN) {
                    int hy = findSafeYInNether(world, test.getBlockX(), test.getBlockZ(), world.getMaxHeight(), resolveMinY(world));
                    y = hy + 1.0;
                } else if (modeUse == NetherMode.HIGHEST_ONLY) {
                    int hy = world.getHighestBlockYAt(test.getBlockX(), test.getBlockZ());
                    y = hy + 1.0;
                } else { // RANDOM_ONLY (no explicit range for near)
                    int minY = resolveMinY(world);
                    int maxY = world.getMaxHeight();
                    y = minY + ThreadLocalRandom.current().nextDouble(Math.max(1.0, (maxY - minY)));
                }
            } else {
                // Keep near-search simple for OW/END (highest), to avoid surprises
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

    public static int resolveMinY(World world) {
        if (world == null) return 0;
        String key = world.getName(); // world identity key; name is stable
        Integer cached = MIN_Y_CACHE.get(key);
        if (cached != null) return cached;

        int minY;
        try {
            // 1.17+ API
            minY = (int) World.class.getMethod("getMinHeight").invoke(world);
        } catch (Throwable ignored) {
            // 1.16.5 and below
            minY = 0;
        }
        MIN_Y_CACHE.put(key, minY);
        return minY;
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
            TL_LAST_FAIL.set(FailReason.FEET_NOT_PASSABLE);
            return false;
        }
        if (!isPassableSafe(head)) {
            TL_LAST_FAIL.set(FailReason.HEAD_NOT_PASSABLE);
            return false;
        }
        if (!ground.getType().isSolid()) {
            TL_LAST_FAIL.set(FailReason.GROUND_NOT_SOLID);
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
                TL_LAST_FAIL.set(FailReason.GROUND_NOT_WHITELISTED);
                return false;
            }
            return true;
        }
        // Otherwise, use global blacklist
        if (globalGroundBlacklist.contains(groundType)) {
            TL_LAST_FAIL.set(FailReason.GROUND_BLACKLISTED);
            return false;
        }
        return true;
    }

    public static FailReason getAndClearLastFailReason() {
        FailReason t = TL_LAST_FAIL.get();
        TL_LAST_FAIL.remove();
        return t;
    }

    private static int hashGroundWhitelist(@Nullable Set<Material> wl) {
        if (wl == null || wl.isEmpty()) return 0;
        // Stable hash: sort by name to avoid iteration order issues
        List<String> names = new ArrayList<>(wl.size());
        for (Material m : wl) names.add(m.name());
        Collections.sort(names);
        return names.hashCode();
    }

    private static String currentYSignature(@NotNull World world) {
        // Build signature from override (if present) or global settings
        YSelectionOverride o = Y_OVERRIDE_TL.get();
        World.Environment env = world.getEnvironment();

        if (env == World.Environment.NETHER) {
            String mode = (o != null && o.mode != null) ? o.mode : switch (netherMode) {
                case SCAN -> "scan";
                case HIGHEST_ONLY -> "highest_only";
                case RANDOM_ONLY -> "random_only";
            };
            boolean respect = (o != null && o.respectRange != null) ? o.respectRange : netherRespectRange;
            return "n:" + mode + ";rr=" + respect;
        } else if (env == World.Environment.THE_END) {
            String mode = (o != null && o.mode != null) ? o.mode : switch (endMode) {
                case MIXED -> "mixed";
                case HIGHEST_ONLY -> "highest_only";
                case RANDOM_ONLY -> "random_only";
            };
            String first = (o != null && o.first != null) ? o.first : (endFirst == MixedFirstGroup.RANDOM ? "random" : "highest");
            double share = (o != null && o.firstShare != null) ? o.firstShare : endShare;
            return "e:" + mode + ";f=" + first + ";s=" + String.format(java.util.Locale.ROOT, "%.3f", share);
        } else if (env == World.Environment.CUSTOM) {
            String mode = (o != null && o.mode != null) ? o.mode : switch (customMode) {
                case MIXED -> "mixed";
                case HIGHEST_ONLY -> "highest_only";
                case RANDOM_ONLY -> "random_only";
            };
            String first = (o != null && o.first != null) ? o.first
                    : (customFirst == MixedFirstGroup.RANDOM ? "random" : "highest");
            double share = (o != null && o.firstShare != null) ? o.firstShare : customShare;
            return "cu:" + mode + ";f=" + first + ";s=" + String.format(Locale.ROOT, "%.3f", share);
        } else {
            // Overworld / fallback
            String mode = (o != null && o.mode != null) ? o.mode : switch (owMode) {
                case MIXED -> "mixed";
                case HIGHEST_ONLY -> "highest_only";
                case RANDOM_ONLY -> "random_only";
            };
            String first = (o != null && o.first != null) ? o.first
                    : (owFirst == MixedFirstGroup.RANDOM ? "random" : "highest");
            double share = (o != null && o.firstShare != null) ? o.firstShare : owShare;
            return "ow:" + mode + ";f=" + first + ";s=" + String.format(Locale.ROOT, "%.3f", share);
        }
    }

    private static double clamp(double v, double min, double max) {
        return (v < min) ? min : (v > max) ? max : v;
    }

    /**
     * Cached "near" safe search around a fixed base X/Z (Y is ignored in the cache key).
     * - Key does NOT include radius (by design) and Y (to increase hit-rate).
     * - Player-specific toggle supported via playerSpecific flag.
     */
    public static @Nullable Location cachedFindSafeNear(Location base,
                                                        int radius,
                                                        Set<Material> groundWhitelist,
                                                        UUID playerId,
                                                        boolean playerSpecific,
                                                        boolean enabled,
                                                        String typeTag,
                                                        @Nullable Predicate<Location> accept) {
        if (!enabled || !cacheEnabled || CACHE == null || base == null || base.getWorld() == null) {
            return attemptSafeNearOnce(base, radius, groundWhitelist);
        }

        String world = base.getWorld().getName();
        int bx = base.getBlockX();
        int bz = base.getBlockZ();
        int wlHash = hashGroundWhitelist(groundWhitelist);
        String ySig = currentYSignature(base.getWorld());

        CacheKey key = new CacheKey(
                typeTag, world,
                bx, 0, bz, // Y intentionally ignored for near
                0, 0, 0, 0, 0, 0,
                playerSpecific ? playerId : null,
                playerSpecific,
                ySig, wlHash
        );

        Location cached = CACHE.getIfPresent(key);
        if (cached != null) {
            boolean ok = withWhitelist(groundWhitelist, () -> isSafeLocation(cached));
            if (ok && (accept == null || accept.test(cached))) {
                if (debugCache) {
                    MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] NEAR HIT " + typeTag + " @" + world + " (" + bx + "," + bz + ") ySig=" + ySig + " wl=" + wlHash);
                }
                return cached.clone();
            }
            // invalidate stale/unsafe cached
            CACHE.invalidate(key);
            if (debugCache) {
                MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] NEAR INVALIDATED cached entry @" + world + " (" + bx + "," + bz + ")");
            }
        } else if (debugCache) {
            MMOSpawnPoint.getInstance().getLogger().info("[SafeLocationFinder] NEAR MISS " + typeTag + " @" + world + " (" + bx + "," + bz + ")");
        }

        @SuppressWarnings("squid:S2583") // SonarLint false positive
        Location found = attemptSafeNearOnce(base, radius, groundWhitelist);
        if (found != null && (accept == null || accept.test(found))) {
            CACHE.put(key, found.clone());
        }
        return found;
    }

    // Cached area lookup with validation predicate:
    // - on hit: if !accept.test(cached) -> invalidate + recompute
    // - on recompute: if result accepted -> store, else don't store
    public static Location cachedFindSafeInAreaValidated(World world,
                                                         double minX, double maxX,
                                                         double minY, double maxY,
                                                         double minZ, double maxZ,
                                                         Set<Material> groundWhitelist,
                                                         UUID playerId,
                                                         boolean playerSpecific,
                                                         boolean enabled,
                                                         String typeTag,
                                                         java.util.function.Predicate<Location> accept) {
        if (!enabled || !cacheEnabled || CACHE == null || world == null) {
            Location fresh = attemptSafeInAreaOnce(world, minX, maxX, minY, maxY, minZ, maxZ, groundWhitelist);
            return (fresh != null && (accept == null || accept.test(fresh))) ? fresh : null;
        }

        String wname = world.getName();
        int kMinX = (int) Math.floor(Math.min(minX, maxX));
        int kMaxX = (int) Math.floor(Math.max(minX, maxX));
        int kMinY = (int) Math.floor(Math.min(minY, maxY));
        int kMaxY = (int) Math.floor(Math.max(minY, maxY));
        int kMinZ = (int) Math.floor(Math.min(minZ, maxZ));
        int kMaxZ = (int) Math.floor(Math.max(minZ, maxZ));
        int wlHash = hashGroundWhitelist(groundWhitelist);
        String ySig = currentYSignature(world);

        CacheKey key = new CacheKey(
                typeTag, wname,
                0, 0, 0,
                kMinX, kMaxX, kMinY, kMaxY, kMinZ, kMaxZ,
                playerSpecific ? playerId : null,
                playerSpecific,
                ySig, wlHash
        );

        Location cached = CACHE.getIfPresent(key);
        if (cached != null) {
            if (accept == null || accept.test(cached)) {
                return cached.clone();
            }
            CACHE.invalidate(key);
        }

        Location fresh = attemptSafeInAreaOnce(world, minX, maxX, minY, maxY, minZ, maxZ, groundWhitelist);
        if (fresh != null && (accept == null || accept.test(fresh))) {
            CACHE.put(key, fresh.clone());
            return fresh;
        }
        return null;
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