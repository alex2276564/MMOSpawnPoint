package uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

public class RegionSpawnsConfig extends OkaeriConfig {

    @Comment("Configuration type: deaths, joins, both")
    @Comment("deaths - only for player death respawns")
    @Comment("joins - only for player join teleports")
    @Comment("both - for both death and join events")
    public String configType = "deaths";

    @Comment("Priority for this configuration file (0-9999, higher = more priority)")
    @Comment("If not specified, uses default priority based on spawn type")
    public Integer priority;

    @Comment("Region-based spawn points (WorldGuard)")
    public List<RegionSpawnEntry> regionSpawns = new ArrayList<>();

    public static class RegionSpawnEntry extends OkaeriConfig {
        @Comment("WorldGuard region name")
        public String region;

        @Comment("Priority for this specific spawn point (0-9999)")
        @Comment("Overrides file priority if specified")
        public Integer priority;

        @Comment("World where this region is located. Use '*' for all worlds.")
        public String regionWorld = "*";

        @Comment("List of location options (fixed or random). Empty list = actions only (no teleport).")
        public List<LocationOption> locations = new ArrayList<>();

        @Comment("Conditions that must be met for this spawn to be used")
        public ConditionsConfig conditions = new ConditionsConfig();

        @Comment("Actions to execute when this spawn is used")
        public ActionsConfig actions = new ActionsConfig();

        @Comment("Custom waiting room for this spawn point (overrides global)")
        public WaitingRoomConfig waitingRoom;

        @Comment("Whether party respawn is disabled for this spawn point")
        public boolean partyRespawnDisabled = false;
    }

    public static class LocationOption extends OkaeriConfig {
        @Comment("Destination world")
        public String world = "world";

        @Comment("Whether safe block checking is required for this location")
        public boolean requireSafe = false;

        // Fixed location (use x/y/z, optional yaw/pitch)
        @Comment("Exact coordinates (fixed)")
        public Double x;
        public Double y;
        public Double z;
        public Float yaw = 0f;
        public Float pitch = 0f;

        // Random region (use min/max ranges)
        @Comment("Random coordinate ranges (if present -> random)")
        public Double minX;
        public Double maxX;
        public Double minY;
        public Double maxY;
        public Double minZ;
        public Double maxZ;

        @Comment("Weight for weighted selection (higher = more likely)")
        public int weight = 100;

        @Comment("Conditional weight adjustments based on player conditions")
        public List<WeightConditionEntry> weightConditions = new ArrayList<>();

        @Comment("Custom waiting room for this specific location (overrides entry/global)")
        public WaitingRoomConfig waitingRoom;

        @Comment("Actions specific to this location")
        public ActionsConfig actions = new ActionsConfig();

        @Comment("When to execute location-specific actions relative to global actions")
        @Comment("Options: before, after, instead")
        public String actionExecutionMode = "before";

        // Helpers
        public boolean isFixed() {
            return x != null && y != null && z != null;
        }

        public boolean isRandom() {
            return minX != null && maxX != null &&
                    minY != null && maxY != null &&
                    minZ != null && maxZ != null;
        }
    }

    public static class WeightConditionEntry extends OkaeriConfig {
        @Comment("Condition type: permission or placeholder")
        public String type;

        @Comment("Condition value (permission node or placeholder expression)")
        public String value;

        @Comment("Weight to use if this condition is met")
        public int weight = 100;
    }

    public static class ConditionsConfig extends OkaeriConfig {
        @Comment("Permission conditions (supports simple logical expressions: && and ||)")
        public List<String> permissions = new ArrayList<>();

        @Comment("PlaceholderAPI conditions (supports simple logical expressions: && and ||)")
        public List<String> placeholders = new ArrayList<>();
    }

    public static class ActionsConfig extends OkaeriConfig {
        @Comment("Messages to send to the player")
        public List<String> messages = new ArrayList<>();

        @Comment("Commands to execute")
        public List<CommandActionEntry> commands = new ArrayList<>();
    }

    public static class CommandActionEntry extends OkaeriConfig {
        @Comment("Command to execute (without leading slash)")
        public String command;

        @Comment("Chance to execute this command (0-100)")
        public int chance = 100;

        @Comment("Whether to execute this command in waiting room")
        public boolean executeInWaitingRoom = false;

        @Comment("Conditional chances based on player conditions")
        public List<ChanceConditionEntry> chanceConditions = new ArrayList<>();
    }

    public static class ChanceConditionEntry extends OkaeriConfig {
        @Comment("Condition type: permission or placeholder")
        public String type;

        @Comment("Condition value")
        public String value;

        @Comment("Chance to use if this condition is met")
        public int weight = 100;
    }

    public static class WaitingRoomConfig extends OkaeriConfig {
        public String world = "world";
        public double x = 0;
        public double y = 100;
        public double z = 0;
        public float yaw = 0;
        public float pitch = 0;
    }
}