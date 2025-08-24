package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

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

    public static class TriggerArea extends OkaeriConfig {
        @Comment("World name")
        public String world = "world";

        @Comment("X axis trigger (value or range). If null -> no constraint on X")
        public RegionSpawnsConfig.AxisSpec x;

        @Comment("Y axis trigger (value or range). If null -> no constraint on Y")
        public RegionSpawnsConfig.AxisSpec y;

        @Comment("Z axis trigger (value or range). If null -> no constraint on Z")
        public RegionSpawnsConfig.AxisSpec z;
    }

    public static class CoordinateSpawnEntry extends OkaeriConfig {
        @Comment("Trigger area where this spawn point is active")
        public TriggerArea triggerArea;

        @Comment("Priority for this specific spawn point (0-9999)")
        public Integer priority;

        @Comment("Destinations list. Empty = actions only (no teleport).")
        public List<RegionSpawnsConfig.LocationOption> destinations = new ArrayList<>();

        @Comment("Conditions that must be met for this spawn to be used")
        public RegionSpawnsConfig.ConditionsConfig conditions = new RegionSpawnsConfig.ConditionsConfig();

        @Comment("Actions to execute when this spawn is used")
        public RegionSpawnsConfig.ActionsConfig actions = new RegionSpawnsConfig.ActionsConfig();

        @Comment("Custom waiting room for this spawn point")
        public RegionSpawnsConfig.WaitingRoomConfig waitingRoom;

        @Comment("Whether party respawn is disabled for this spawn point")
        public boolean partyRespawnDisabled = false;
    }
}