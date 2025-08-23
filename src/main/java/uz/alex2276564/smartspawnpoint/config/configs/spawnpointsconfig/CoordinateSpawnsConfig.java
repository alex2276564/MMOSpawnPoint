package uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

public class CoordinateSpawnsConfig extends OkaeriConfig {

    @Comment("Configuration type: deaths, joins, both")
    public String configType = "deaths";

    @Comment("Priority for this configuration file (0-9999, higher = more priority)")
    public Integer priority;

    @Comment("Coordinate-based spawn points")
    public List<CoordinateSpawnEntry> coordinateSpawns = new ArrayList<>();

    public static class CoordinateSpawnEntry extends OkaeriConfig {
        @Comment("Coordinate area where this spawn point is active")
        public CoordinateAreaConfig coordinates;

        @Comment("Priority for this specific spawn point (0-9999)")
        public Integer priority;

        @Comment("List of destination locations (fixed or random). Empty list = actions only (no teleport).")
        public List<RegionSpawnsConfig.LocationOption> locations = new ArrayList<>();

        @Comment("Conditions that must be met for this spawn to be used")
        public RegionSpawnsConfig.ConditionsConfig conditions = new RegionSpawnsConfig.ConditionsConfig();

        @Comment("Actions to execute when this spawn is used")
        public RegionSpawnsConfig.ActionsConfig actions = new RegionSpawnsConfig.ActionsConfig();

        @Comment("Custom waiting room for this spawn point")
        public RegionSpawnsConfig.WaitingRoomConfig waitingRoom;

        @Comment("Whether party respawn is disabled for this spawn point")
        public boolean partyRespawnDisabled = false;
    }

    public static class CoordinateAreaConfig extends OkaeriConfig {
        @Comment("World name")
        public String world = "world";

        @Comment("Coordinate boundaries")
        public double minX = 0;
        public double maxX = 100;
        public double minY = 0;
        public double maxY = 256;
        public double minZ = 0;
        public double maxZ = 100;
    }
}