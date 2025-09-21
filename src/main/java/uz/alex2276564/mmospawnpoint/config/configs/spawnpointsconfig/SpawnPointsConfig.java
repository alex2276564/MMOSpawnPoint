package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import eu.okaeri.configs.OkaeriConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified spawn-points configuration.
 * One or many files in spawnpoints/ can define spawns list.
 */
public class SpawnPointsConfig extends OkaeriConfig {

    // Unified spawn rules list
    public List<SpawnPointEntry> spawns = new ArrayList<>();

    // ========== DSL ENUMS ==========

    public enum Phase {
        BEFORE,
        WAITING_ROOM,
        AFTER
    }

    // ========== DSL SHARED TYPES ==========

    public static class AxisSpec extends OkaeriConfig {
        // Fixed value for this axis
        public Double value;

        // Minimum bound (strictly less than max)
        public Double min;

        // Maximum bound (strictly greater than min)
        public Double max;

        public boolean isValue() {
            return value != null && min == null && max == null;
        }

        public boolean isRange() {
            return value == null && min != null && max != null;
        }
    }

    public static class MessageEntry extends OkaeriConfig {
        // Message text (MiniMessage supported)
        public String text;

        // Chance to send this message (0-100)
        public int chance = 100;

        // Phases when to execute this message: BEFORE, WAITING_ROOM, AFTER
        public List<Phase> phases = new ArrayList<>(List.of(Phase.AFTER));

        // Conditional chances (permission/placeholder)
        public List<ChanceConditionEntry> chanceConditions = new ArrayList<>();
    }

    public static class ActionsConfig extends OkaeriConfig {
        // Messages to send
        public List<MessageEntry> messages = new ArrayList<>();

        // Commands to execute
        public List<CommandActionEntry> commands = new ArrayList<>();
    }

    public static class CommandActionEntry extends OkaeriConfig {
        // Command to execute (without leading slash)
        public String command;

        // Chance to execute this command (0-100)
        public int chance = 100;

        // Phases when to execute this command: BEFORE, WAITING_ROOM, AFTER
        public List<Phase> phases = new ArrayList<>(List.of(Phase.AFTER));

        // Conditional chances based on player conditions
        public List<ChanceConditionEntry> chanceConditions = new ArrayList<>();
    }

    public static class ChanceConditionEntry extends OkaeriConfig {
        // Condition type: permission or placeholder
        public String type;

        // Condition value (permission node or placeholder expression)
        public String value;

        // Mode: set | add | mul (default: set)
        public String mode = "set";

        // Value to apply based on mode. For set: 0..100; add: +/-delta; mul: multiplier (e.g., 2)
        public int weight = 100;
    }

    public static class WeightConditionEntry extends OkaeriConfig {
        // Condition type: permission or placeholder
        public String type;

        // Condition value (permission node or placeholder expression)
        public String value;

        // Mode: set | add | mul (default: set)
        public String mode = "set";

        // Value to apply based on mode. For set: final weight; add: +/-delta; mul: multiplier
        public int weight = 100;
    }

    public static class ConditionsConfig extends OkaeriConfig {
        // Permission conditions (supports full expressions with parentheses)
        public List<String> permissions = new ArrayList<>();

        // PlaceholderAPI conditions (supports full expressions with parentheses)
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

    public static class PartyRule extends OkaeriConfig {
        public boolean respawnDisabled = false;
        // future: Integer respawnCooldownOverride, String modeOverride, etc.
    }

    // Per-destination Y-selection override
    public static class YSelectionOverride extends OkaeriConfig {
        // Mode: 'mixed'|'highest_only'|'random_only' or 'scan' (Nether only)
        public String mode;

        // For 'mixed' only: first group and share
        public String first;      // 'highest' | 'random'
        public double firstShare = 0.6; // [0..1]

        // Nether-specific: whether scan should respect destination Y-range (rects)
        public Boolean respectRange; // optional; only meaningful for nether+scan
    }

    // Per-destination cache override
    public static class CacheOverride extends OkaeriConfig {
        // override global enabled
        public Boolean enabled;

        // override global playerSpecific
        public Boolean playerSpecific;
    }

    public static class Destination extends OkaeriConfig {
        // Destination world
        public String world = "world";

        // Whether safe block checking is required for this destination
        public boolean requireSafe = false;

        // X axis spec (value or range)
        public AxisSpec x;

        // Y axis spec (value or range)
        public AxisSpec y;

        // Z axis spec (value or range)
        public AxisSpec z;

        // Yaw spec (value or range)
        public AxisSpec yaw;

        // Pitch spec (value or range)
        public AxisSpec pitch;

        // Optional list of rectangles to pick from (overrides x/y/z axis specs)
        public List<RectSpec> rects = new ArrayList<>();

        // Optional exclusion rectangles (applied for safe search)
        public List<RectSpec> excludeRects = new ArrayList<>();

        // Weight for weighted selection (higher = more likely)
        public int weight = 100;

        // Conditional weight adjustments based on player conditions
        public List<WeightConditionEntry> weightConditions = new ArrayList<>();

        // Custom waiting room for this destination (overrides entry/global)
        public WaitingRoomConfig waitingRoom;

        // Actions specific to this destination
        public ActionsConfig actions = new ActionsConfig();

        // Order of local vs global actions inside a phase: before, after, instead
        public String actionExecutionMode = "before";

        // Optional per-destination override
        public YSelectionOverride ySelection;

        // Per-entry ground whitelist (allowed blocks under feet)
        public List<String> groundWhitelist = new ArrayList<>();

        // Optional per-destination cache override
        public CacheOverride cache;
    }

    // ========== MATCHING TYPES ==========

    public static class TriggerArea extends OkaeriConfig {
        // World name (or pattern controlled by worldMatchMode)
        public String world = "world";

        // Match mode for 'world': exact or regex
        public String worldMatchMode = "exact";

        // X axis trigger (value or range). If null -> no constraint on X
        public AxisSpec x;

        // Y axis trigger (value or range). If null -> no constraint on Y
        public AxisSpec y;

        // Z axis trigger (value or range). If null -> no constraint on Z
        public AxisSpec z;

        // Optional list of include rectangles
        public List<RectSpec> rects = new ArrayList<>();

        // Optional list of exclude rectangles
        public List<RectSpec> excludeRects = new ArrayList<>();
    }

    public static class SpawnPointEntry extends OkaeriConfig {
        // Match type: region | world | coordinate
        public String kind = "region";

        // Event type: death | join | both
        public String event = "death";

        // Priority for this specific spawn point (0-9999). If null -> uses defaults by kind from main config
        public Integer priority;

        // ========== REGION MATCHING ==========
        // WorldGuard region id or pattern (used if kind=region)
        public String region;

        // Match mode for 'region': exact | regex
        public String regionMatchMode = "exact";

        // World name or pattern for the region (used if kind=region). If null -> any world
        public String regionWorld;

        // Match mode for 'regionWorld': exact | regex
        public String regionWorldMatchMode = "exact";

        // ========== WORLD MATCHING ==========
        // World name or pattern (used if kind=world)
        public String world;

        // Match mode for 'world': exact | regex
        public String worldMatchMode = "exact";

        // ========== COORDINATE MATCHING ==========
        // Trigger area (used if kind=coordinate)
        public TriggerArea triggerArea;

        // ========== COMMON DATA ==========
        // Destinations list. Empty = actions only (no teleport)
        public List<Destination> destinations = new ArrayList<>();

        // Conditions that must be met for this spawn to be used
        public ConditionsConfig conditions = new ConditionsConfig();

        // Actions to execute when this spawn is used
        public ActionsConfig actions = new ActionsConfig();

        // Custom waiting room for this spawn point (overrides global)
        public WaitingRoomConfig waitingRoom;

        // nullable; if null – no special party rules
        public PartyRule party;
    }

    public static class RectSpec extends OkaeriConfig {
        public AxisSpec x;
        public AxisSpec y; // optional
        public AxisSpec z;
    }
}