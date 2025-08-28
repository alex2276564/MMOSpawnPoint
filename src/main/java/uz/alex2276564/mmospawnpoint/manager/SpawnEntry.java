package uz.alex2276564.mmospawnpoint.manager;

import org.bukkit.Location;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfig;
import uz.alex2276564.mmospawnpoint.utils.WorldGuardUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public record SpawnEntry(
        Type type,
        int calculatedPriority,
        String event, // "deaths" | "joins" | "both"
        SpawnPointsConfig.SpawnPointEntry spawnData,
        String fileName
) {
    public enum Type {REGION, WORLD, COORDINATE}

    private static final ConcurrentHashMap<String, Pattern> REGEX_CACHE = new ConcurrentHashMap<>();

    public boolean isForEventType(String eventType) {
        if (event == null || eventType == null) return false;
        String e = event.toLowerCase();
        String et = eventType.toLowerCase();
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

        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        if (area.x != null && !matchesAxis(area.x, location.getX(), bx)) return false;
        if (area.y != null && !matchesAxis(area.y, location.getY(), by)) return false;
        return area.z == null || matchesAxis(area.z, location.getZ(), bz);
    }

    private boolean matchesAxis(SpawnPointsConfig.AxisSpec axis, double coord, int blockCoord) {
        if (axis.isValue()) {
            return blockCoord == (int) Math.floor(axis.value);
        } else if (axis.isRange()) {
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