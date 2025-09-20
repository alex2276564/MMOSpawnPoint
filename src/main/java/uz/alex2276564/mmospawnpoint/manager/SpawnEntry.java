package uz.alex2276564.mmospawnpoint.manager;

import org.bukkit.Location;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfig;
import uz.alex2276564.mmospawnpoint.utils.WorldGuardUtils;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public record SpawnEntry(
        Type type,
        int calculatedPriority,
        String event, // "death" | "join" | "both"
        SpawnPointsConfig.SpawnPointEntry spawnData,
        String fileName
) {
    public enum Type {REGION, WORLD, COORDINATE}

    private static final ConcurrentHashMap<String, Pattern> REGEX_CACHE = new ConcurrentHashMap<>();

    public static void clearRegexCache() {
        REGEX_CACHE.clear();
    }

    public boolean isForEventType(String eventType) {
        if (event == null || eventType == null) return false;
        String e = event.toLowerCase(Locale.ROOT);
        String et = eventType.toLowerCase(Locale.ROOT);
        return "both".equals(e) || e.equals(et);
    }

    public boolean matchesLocation(Location location) {
        return switch (type) {
            case REGION -> matchesRegion(location);
            case WORLD -> matchesWorld(location);
            case COORDINATE -> matchesCoordinates(location);
        };
    }

    private boolean matchesRegion(Location location) {
        if (spawnData == null) return false;
        if (!MMOSpawnPoint.getInstance().isWorldGuardEnabled()) {
            return false;
        }

        try {
            // World check: null or "*" => any world
            boolean worldOk = (spawnData.regionWorld == null)
                    || "*".equals(spawnData.regionWorld)
                    || matchByMode(spawnData.regionWorld, spawnData.regionWorldMatchMode, location.getWorld().getName());
            if (!worldOk) return false;

            // Region check
            Set<String> regions = WorldGuardUtils.getRegionsAt(location);
            if (regions.isEmpty()) return false;

            for (String id : regions) {
                if (matchByMode(spawnData.region, spawnData.regionMatchMode, id)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean matchesWorld(Location location) {
        if (spawnData == null) return false;
        return matchByMode(spawnData.world, spawnData.worldMatchMode, location.getWorld().getName());
    }

    private boolean matchesCoordinates(Location location) {
        if (spawnData == null || spawnData.triggerArea == null) return false;

        SpawnPointsConfig.TriggerArea area = spawnData.triggerArea;
        if (!matchByMode(area.world, area.worldMatchMode, location.getWorld().getName())) return false;

        if (area.rects != null && !area.rects.isEmpty()) {
            boolean insideInclude = false;
            for (SpawnPointsConfig.RectSpec r : area.rects) {
                if (r == null || r.x == null || r.z == null) continue;
                double minX = r.x.Value() ? r.x.value : r.x.min;
                double maxX = r.x.Value() ? r.x.value : r.x.max;
                double minZ = r.z.Value() ? r.z.value : r.z.min;
                double maxZ = r.z.Value() ? r.z.value : r.z.max;
                double minY;
                double maxY;
                if (r.y == null) {
                    minY = Double.NEGATIVE_INFINITY;
                    maxY = Double.POSITIVE_INFINITY;
                } else if (r.y.Value()) {
                    minY = r.y.value;
                    maxY = r.y.value;
                } else {
                    minY = r.y.min;
                    maxY = r.y.max;
                }
                if (insideRect(location, minX, maxX, minY, maxY, minZ, maxZ)) {
                    insideInclude = true;
                    break;
                }
            }
            if (!insideInclude) return false;

            if (area.excludeRects != null && !area.excludeRects.isEmpty()) {
                for (SpawnPointsConfig.RectSpec ex : area.excludeRects) {
                    if (ex == null || ex.x == null || ex.z == null) continue;
                    double exMinX = ex.x.Value() ? ex.x.value : ex.x.min;
                    double exMaxX = ex.x.Value() ? ex.x.value : ex.x.max;
                    double exMinZ = ex.z.Value() ? ex.z.value : ex.z.min;
                    double exMaxZ = ex.z.Value() ? ex.z.value : ex.z.max;
                    double exMinY;
                    double exMaxY;
                    if (ex.y == null) {
                        exMinY = Double.NEGATIVE_INFINITY;
                        exMaxY = Double.POSITIVE_INFINITY;
                    } else if (ex.y.Value()) {
                        exMinY = ex.y.value;
                        exMaxY = ex.y.value;
                    } else {
                        exMinY = ex.y.min;
                        exMaxY = ex.y.max;
                    }
                    if (insideRect(location, exMinX, exMaxX, exMinY, exMaxY, exMinZ, exMaxZ)) {
                        return false;
                    }
                }
            }
            return true;
        }

        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        if (area.x != null && !matchesAxis(area.x, location.getX(), bx)) return false;
        if (area.y != null && !matchesAxis(area.y, location.getY(), by)) return false;
        return area.z == null || matchesAxis(area.z, location.getZ(), bz);
    }

    private boolean insideRect(Location loc, double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= Math.min(minX, maxX) && x <= Math.max(minX, maxX)
                && y >= Math.min(minY, maxY) && y <= Math.max(minY, maxY)
                && z >= Math.min(minZ, maxZ) && z <= Math.max(minZ, maxZ);
    }

    private boolean matchesAxis(SpawnPointsConfig.AxisSpec axis, double coord, int blockCoord) {
        if (axis.Value()) {
            return blockCoord == (int) Math.floor(axis.value);
        } else if (axis.Range()) {
            return coord >= axis.min && coord <= axis.max;
        }
        return false;
    }

    private boolean matchByMode(String patternOrText, String mode, String candidate) {
        if (patternOrText == null || candidate == null) return false;
        if (mode == null || mode.equalsIgnoreCase("exact")) {
            return candidate.equals(patternOrText);
        }
        if (mode.equalsIgnoreCase("regex")) {
            Pattern p = REGEX_CACHE.computeIfAbsent(patternOrText, Pattern::compile);
            return p.matcher(candidate).matches();
        }
        // Fallback: exact
        return candidate.equals(patternOrText);
    }
}