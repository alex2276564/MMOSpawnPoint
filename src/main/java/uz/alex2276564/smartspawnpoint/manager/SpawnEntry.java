package uz.alex2276564.smartspawnpoint.manager;

import org.bukkit.Location;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.CoordinateSpawnsConfig;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.RegionSpawnsConfig;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.WorldSpawnsConfig;
import uz.alex2276564.smartspawnpoint.utils.WorldGuardUtils;

import java.util.Set;

/**
 * Single, flattened spawn entry with pre-calculated priority.
 *
 * @param spawnData RegionSpawnEntry, WorldSpawnEntry, etc.
 */
public record SpawnEntry(Type type, int calculatedPriority, String configType, Object spawnData, String fileName) {
    public enum Type {REGION, WORLD, COORDINATE}

    public boolean isForEventType(String eventType) {
        return "both".equals(configType) || eventType.equals(configType);
    }

    public boolean matchesLocation(Location location) {
        return switch (type) {
            case REGION -> matchesRegion(location);
            case WORLD -> matchesWorld(location);
            case COORDINATE -> matchesCoordinates(location);
        };
    }

    private boolean matchesRegion(Location location) {
        if (!(spawnData instanceof RegionSpawnsConfig.RegionSpawnEntry entry)) {
            return false;
        }

        // Skip region matches if WorldGuard is not available
        if (!SmartSpawnPoint.getInstance().isWorldGuardEnabled()) {
            return false;
        }

        try {
            Set<String> regions = WorldGuardUtils.getRegionsAt(location);
            if (regions.isEmpty()) {
                return false;
            }

            boolean regionMatch = regions.contains(entry.region);
            boolean worldMatch = entry.regionWorld.equals("*")
                    || location.getWorld().getName().equals(entry.regionWorld);

            return regionMatch && worldMatch;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean matchesWorld(Location location) {
        if (!(spawnData instanceof WorldSpawnsConfig.WorldSpawnEntry entry)) {
            return false;
        }
        return location.getWorld().getName().equals(entry.world);
    }

    private boolean matchesCoordinates(Location location) {
        if (!(spawnData instanceof CoordinateSpawnsConfig.CoordinateSpawnEntry entry)) {
            return false;
        }

        var coords = entry.coordinates;
        if (!location.getWorld().getName().equals(coords.world)) {
            return false;
        }

        return location.getX() >= coords.minX && location.getX() <= coords.maxX &&
                location.getY() >= coords.minY && location.getY() <= coords.maxY &&
                location.getZ() >= coords.minZ && location.getZ() <= coords.maxZ;
    }
}