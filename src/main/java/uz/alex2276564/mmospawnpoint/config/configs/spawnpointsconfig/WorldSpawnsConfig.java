package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

public class WorldSpawnsConfig extends OkaeriConfig {

    @Comment("Configuration type: deaths, joins, both")
    public String configType = "deaths";

    @Comment("Priority for this configuration file (0-9999, higher = more priority)")
    public Integer priority;

    @Comment("World-based spawn points")
    public List<WorldSpawnEntry> worldSpawns = new ArrayList<>();

    public static class WorldSpawnEntry extends OkaeriConfig {
        @Comment("Trigger world name (or pattern controlled by worldMatchMode)")
        public String world;

        @Comment("Match mode for 'world': exact or regex")
        public String worldMatchMode = "exact";

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