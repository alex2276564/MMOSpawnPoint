package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

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

    // ---- Enums ----

    public enum Phase {
        BEFORE,
        WAITING_ROOM,
        AFTER
    }

    // ---- Data classes ----

    //    Axis specification: exactly one of 'value' or 'min'+'max'"
    public static class AxisSpec extends OkaeriConfig {
        @Comment("Fixed value for this axis")
        public Double value;

        @Comment("Minimum bound (must be strictly less than 'max')")
        public Double min;

        @Comment("Maximum bound (must be strictly greater than 'min')")
        public Double max;

        public boolean isValue() {
            return value != null && min == null && max == null;
        }

        public boolean isRange() {
            return value == null && min != null && max != null;
        }
    }

    public static class MessageEntry extends OkaeriConfig {
        @Comment("Message text (MiniMessage supported)")
        public String text;

        @Comment("Phases when to execute this message: BEFORE, WAITING_ROOM, AFTER")
        public List<Phase> phases = new ArrayList<>(List.of(Phase.AFTER));
    }

    public static class ActionsConfig extends OkaeriConfig {
        @Comment("Messages to send")
        public List<MessageEntry> messages = new ArrayList<>();

        @Comment("Commands to execute")
        public List<CommandActionEntry> commands = new ArrayList<>();
    }

    public static class CommandActionEntry extends OkaeriConfig {
        @Comment("Command to execute (without leading slash)")
        public String command;

        @Comment("Chance to execute this command (0-100)")
        public int chance = 100;

        @Comment("Phases when to execute this command: BEFORE, WAITING_ROOM, AFTER")
        public List<Phase> phases = new ArrayList<>(List.of(Phase.AFTER));

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

    public static class WeightConditionEntry extends OkaeriConfig {
        @Comment("Condition type: permission or placeholder")
        public String type;

        @Comment("Condition value (permission node or placeholder expression)")
        public String value;

        @Comment("Weight to use if this condition is met")
        public int weight = 100;
    }

    public static class ConditionsConfig extends OkaeriConfig {
        @Comment("Permission conditions (supports full expressions with parentheses)")
        public List<String> permissions = new ArrayList<>();

        @Comment("PlaceholderAPI conditions (supports full expressions with parentheses)")
        public List<String> placeholders = new ArrayList<>();
    }

    public static class WaitingRoomConfig extends OkaeriConfig {
        public String world = "world";
        public double x = 0;
        public double y = 100;
        public double z = 0;
        public float yaw = 0;
        public float pitch = 0;
    }

    public static class RegionSpawnEntry extends OkaeriConfig {
        @Comment("WorldGuard region name")
        public String region;

        @Comment("Priority for this specific spawn point (0-9999)")
        @Comment("Overrides file priority if specified")
        public Integer priority;

        @Comment("World where this region is located. Use '*' for all worlds.")
        public String regionWorld = "*";

        @Comment("Destinations list. Empty = actions only (no teleport).")
        public List<LocationOption> destinations = new ArrayList<>();

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

        @Comment("Whether safe block checking is required for this destination")
        public boolean requireSafe = false;

        @Comment("X axis spec (value or range)")
        public AxisSpec x;

        @Comment("Y axis spec (value or range)")
        public AxisSpec y;

        @Comment("Z axis spec (value or range)")
        public AxisSpec z;

        @Comment("Yaw spec (value or range)")
        public AxisSpec yaw;

        @Comment("Pitch spec (value or range)")
        public AxisSpec pitch;

        @Comment("Weight for weighted selection (higher = more likely)")
        public int weight = 100;

        @Comment("Conditional weight adjustments based on player conditions")
        public List<WeightConditionEntry> weightConditions = new ArrayList<>();

        @Comment("Custom waiting room for this destination (overrides entry/global)")
        public WaitingRoomConfig waitingRoom;

        @Comment("Actions specific to this destination")
        public ActionsConfig actions = new ActionsConfig();

        @Comment("Order of local vs global actions inside a phase: before, after, instead")
        public String actionExecutionMode = "before";
    }
}