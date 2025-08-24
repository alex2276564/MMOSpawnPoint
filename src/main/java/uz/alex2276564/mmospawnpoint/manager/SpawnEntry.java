package uz.alex2276564.mmospawnpoint.manager;

import org.bukkit.Location;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.CoordinateSpawnsConfig;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.RegionSpawnsConfig;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.WorldSpawnsConfig;
import uz.alex2276564.mmospawnpoint.utils.WorldGuardUtils;

import java.util.Set;

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
        if (!MMOSpawnPoint.getInstance().isWorldGuardEnabled()) {
            return false;
        }
        try {
            Set<String> regions = WorldGuardUtils.getRegionsAt(location);
            if (regions.isEmpty()) return false;

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

        CoordinateSpawnsConfig.TriggerArea area = entry.triggerArea;
        if (area == null) return false;
        if (!location.getWorld().getName().equals(area.world)) return false;

        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        if (area.x != null && !matchesAxis(area.x, location.getX(), bx)) return false;
        if (area.y != null && !matchesAxis(area.y, location.getY(), by)) return false;
        return area.z == null || matchesAxis(area.z, location.getZ(), bz);
    }

    private boolean matchesAxis(RegionSpawnsConfig.AxisSpec axis, double coord, int blockCoord) {
        if (axis.isValue()) {
            return blockCoord == (int) Math.floor(axis.value);
        } else if (axis.isRange()) {
            return coord >= axis.min && coord <= axis.max;
        }
        return false;
    }
}