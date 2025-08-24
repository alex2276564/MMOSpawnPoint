package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.mmospawnpoint.config.utils.validation.Validators;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@UtilityClass
public class RegionSpawnsConfigValidator {

    public static void validate(RegionSpawnsConfig config, String fileName) {
        ValidationResult result = new ValidationResult();
        validateRegionSpawns(result, config.regionSpawns);
        result.throwIfInvalid("Region spawns configuration (" + fileName + ")");
    }

    private static void validateRegionSpawns(ValidationResult result, List<RegionSpawnsConfig.RegionSpawnEntry> spawns) {
        if (spawns == null || spawns.isEmpty()) {
            result.addError("regionSpawns", "Region spawns list cannot be empty");
            return;
        }

        for (int i = 0; i < spawns.size(); i++) {
            RegionSpawnsConfig.RegionSpawnEntry entry = spawns.get(i);
            String prefix = "regionSpawns[" + i + "]";
            validateRegionSpawnEntry(result, entry, prefix);
        }
    }

    private static void validateRegionSpawnEntry(ValidationResult result, RegionSpawnsConfig.RegionSpawnEntry entry, String prefix) {
        Validators.notBlank(result, prefix + ".region", entry.region, "Region cannot be empty");

        if (entry.destinations != null) {
            for (int j = 0; j < entry.destinations.size(); j++) {
                RegionSpawnsConfig.LocationOption loc = entry.destinations.get(j);
                validateDestination(result, loc, prefix + ".destinations[" + j + "]");
            }
        }

        if (entry.conditions != null) {
            validateConditions(result, entry.conditions, prefix + ".conditions");
        }
        if (entry.actions != null) {
            validateActions(result, entry.actions, prefix + ".actions");
        }
        if (entry.waitingRoom != null) {
            Validators.notBlank(result, prefix + ".waitingRoom.world", entry.waitingRoom.world, "Waiting room world cannot be empty");
        }
    }

    public static void validateDestination(ValidationResult result, RegionSpawnsConfig.LocationOption loc, String prefix) {
        Validators.notBlank(result, prefix + ".world", loc.world, "Destination world cannot be empty");

        // X/Z required
        if (loc.x == null) {
            result.addError(prefix + ".x", "X axis is required (value or range)");
        } else {
            AxisSpecValidator.validateAxisSpec(result, loc.x, prefix + ".x");
        }
        if (loc.z == null) {
            result.addError(prefix + ".z", "Z axis is required (value or range)");
        } else {
            AxisSpecValidator.validateAxisSpec(result, loc.z, prefix + ".z");
        }

        // Y required if requireSafe=false; optional otherwise
        if (!loc.requireSafe) {
            if (loc.y == null) {
                result.addError(prefix + ".y", "Y axis is required when requireSafe=false (use value or range)");
            } else {
                AxisSpecValidator.validateAxisSpec(result, loc.y, prefix + ".y");
            }
        } else if (loc.y != null) {
            AxisSpecValidator.validateAxisSpec(result, loc.y, prefix + ".y");
        }

        // Yaw/pitch (optional)
        if (loc.yaw != null) AxisSpecValidator.validateAxisSpec(result, loc.yaw, prefix + ".yaw");
        if (loc.pitch != null) AxisSpecValidator.validateAxisSpec(result, loc.pitch, prefix + ".pitch");

        Validators.min(result, prefix + ".weight", loc.weight, 1, "Weight must be at least 1");

        if (loc.weightConditions != null) {
            for (int k = 0; k < loc.weightConditions.size(); k++) {
                RegionSpawnsConfig.WeightConditionEntry wc = loc.weightConditions.get(k);
                String wcp = prefix + ".weightConditions[" + k + "]";
                Set<String> validTypes = Set.of("permission", "placeholder");
                if (!validTypes.contains(wc.type)) {
                    result.addError(wcp + ".type", "Invalid condition type. Valid: permission, placeholder");
                }
                Validators.notBlank(result, wcp + ".value", wc.value, "Condition value cannot be empty");
                if ("placeholder".equals(wc.type) && PlaceholderUtils.isInvalidLogicalExpression(wc.value)) {
                    result.addError(wcp + ".value", "Invalid placeholder expression: " + wc.value);
                }
                Validators.min(result, wcp + ".weight", wc.weight, 1, "Weight must be at least 1");
            }
        }

        if (loc.actions != null) {
            validateActions(result, loc.actions, prefix + ".actions");
        }

        if (loc.actionExecutionMode != null) {
            String mode = loc.actionExecutionMode.toLowerCase(Locale.ROOT);
            Set<String> modes = Set.of("before", "after", "instead");
            if (!modes.contains(mode)) {
                result.addError(prefix + ".actionExecutionMode", "Invalid actionExecutionMode. Valid: before, after, instead");
            }
        }

        if (loc.waitingRoom != null) {
            Validators.notBlank(result, prefix + ".waitingRoom.world", loc.waitingRoom.world, "Waiting room world cannot be empty");
        }
    }

    private static void validateConditions(ValidationResult result, RegionSpawnsConfig.ConditionsConfig cond, String prefix) {
        if (cond.permissions != null) {
            for (int i = 0; i < cond.permissions.size(); i++) {
                String expr = cond.permissions.get(i);
                if (PlaceholderUtils.isInvalidLogicalExpression(expr)) {
                    result.addError(prefix + ".permissions[" + i + "]", "Invalid logical expression: " + expr);
                }
            }
        }
        if (cond.placeholders != null) {
            for (int i = 0; i < cond.placeholders.size(); i++) {
                String expr = cond.placeholders.get(i);
                if (PlaceholderUtils.isInvalidLogicalExpression(expr)) {
                    result.addError(prefix + ".placeholders[" + i + "]", "Invalid placeholder expression: " + expr);
                }
            }
        }
    }

    public static void validateActions(ValidationResult result, RegionSpawnsConfig.ActionsConfig actions, String prefix) {
        if (actions.messages != null) {
            for (int i = 0; i < actions.messages.size(); i++) {
                RegionSpawnsConfig.MessageEntry me = actions.messages.get(i);
                String mp = prefix + ".messages[" + i + "]";
                Validators.notBlank(result, mp + ".text", me.text, "Message text cannot be empty");

                if (me.phases != null) {
                    for (int j = 0; j < me.phases.size(); j++) {
                        if (me.phases.get(j) == null) {
                            result.addError(mp + ".phases[" + j + "]", "Phase cannot be null (use BEFORE, WAITING_ROOM, AFTER)");
                        }
                    }
                }
            }
        }

        if (actions.commands != null) {
            for (int i = 0; i < actions.commands.size(); i++) {
                RegionSpawnsConfig.CommandActionEntry cmd = actions.commands.get(i);
                String cp = prefix + ".commands[" + i + "]";
                Validators.notBlank(result, cp + ".command", cmd.command, "Command cannot be empty");
                Validators.min(result, cp + ".chance", cmd.chance, 0, "Command chance cannot be negative");
                Validators.max(result, cp + ".chance", cmd.chance, 100, "Command chance cannot exceed 100");

                if (cmd.phases != null) {
                    for (int j = 0; j < cmd.phases.size(); j++) {
                        if (cmd.phases.get(j) == null) {
                            result.addError(cp + ".phases[" + j + "]", "Phase cannot be null (use BEFORE, WAITING_ROOM, AFTER)");
                        }
                    }
                }

                if (cmd.chanceConditions != null) {
                    for (int j = 0; j < cmd.chanceConditions.size(); j++) {
                        RegionSpawnsConfig.ChanceConditionEntry cc = cmd.chanceConditions.get(j);
                        String ccp = cp + ".chanceConditions[" + j + "]";
                        Set<String> validTypes = Set.of("permission", "placeholder");
                        if (!validTypes.contains(cc.type)) {
                            result.addError(ccp + ".type", "Invalid condition type. Valid: permission, placeholder");
                        }
                        Validators.notBlank(result, ccp + ".value", cc.value, "Condition value cannot be empty");
                        if ("placeholder".equals(cc.type) && PlaceholderUtils.isInvalidLogicalExpression(cc.value)) {
                            result.addError(ccp + ".value", "Invalid placeholder expression: " + cc.value);
                        }
                    }
                }
            }
        }
    }
}