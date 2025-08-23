package uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.smartspawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.smartspawnpoint.config.utils.validation.Validators;
import uz.alex2276564.smartspawnpoint.utils.PlaceholderUtils;

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
        // Region
        Validators.notBlank(result, prefix + ".region", entry.region, "Region cannot be empty");

        // Locations: allowed empty (actions only)
        if (entry.locations != null) {
            for (int j = 0; j < entry.locations.size(); j++) {
                RegionSpawnsConfig.LocationOption loc = entry.locations.get(j);
                validateLocationOptionStrict(result, loc, prefix + ".locations[" + j + "]");
            }
        }

        // Conditions
        if (entry.conditions != null) {
            validateConditions(result, entry.conditions, prefix + ".conditions");
        }

        // Actions (global)
        if (entry.actions != null) {
            validateActions(result, entry.actions, prefix + ".actions");
        }

        // Waiting room
        if (entry.waitingRoom != null) {
            Validators.notBlank(result, prefix + ".waitingRoom.world", entry.waitingRoom.world, "Waiting room world cannot be empty");
        }
    }

    // Strict validation: no mixing fixed/random fields; completeness checks
    private static void validateLocationOptionStrict(ValidationResult result, RegionSpawnsConfig.LocationOption loc, String prefix) {
        Validators.notBlank(result, prefix + ".world", loc.world, "Location world cannot be empty");

        boolean hasAnyFixedField = (loc.x != null) || (loc.y != null) || (loc.z != null);
        boolean fixed = loc.isFixed();
        boolean hasAnyRandomField = (loc.minX != null) || (loc.maxX != null) ||
                (loc.minY != null) || (loc.maxY != null) ||
                (loc.minZ != null) || (loc.maxZ != null);
        boolean random = loc.isRandom();

        // Partial fixed fields detection
        if (hasAnyFixedField && !fixed) {
            result.addError(prefix, "Fixed location must define ALL of x, y, z or none of them");
        }

        // Partial random fields detection
        if (hasAnyRandomField && !random) {
            result.addError(prefix, "Random location must define ALL of minX/maxX/minY/maxY/minZ/maxZ or none of them");
        }

        if (fixed && random) {
            result.addError(prefix, "Location option cannot be both fixed (x/y/z) and random (min/max)");
        }
        if (!fixed && !random) {
            result.addError(prefix, "Location option must be either fixed (x/y/z) or random (min/max)");
        }

        // Disallow stray fields from the other form
        if (random && hasAnyFixedField) {
            result.addError(prefix, "Random location cannot contain x/y/z (remove x/y/z for random form)");
        }
        if (fixed && hasAnyRandomField) {
            result.addError(prefix, "Fixed location cannot contain min/max fields (remove min*/max* for fixed form)");
        }

        if (random) {
            if (loc.minX >= loc.maxX) result.addError(prefix + ".minX/maxX", "minX must be less than maxX");
            if (loc.minY >= loc.maxY) result.addError(prefix + ".minY/maxY", "minY must be less than maxY");
            if (loc.minZ >= loc.maxZ) result.addError(prefix + ".minZ/maxZ", "minZ must be less than maxZ");
        }

        // Weight
        Validators.min(result, prefix + ".weight", loc.weight, 1, "Weight must be at least 1");

        // Weight conditions
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

        // actionExecutionMode
        if (loc.actionExecutionMode != null) {
            String mode = loc.actionExecutionMode.toLowerCase(Locale.ROOT);
            Set<String> modes = Set.of("before", "after", "instead");
            if (!modes.contains(mode)) {
                result.addError(prefix + ".actionExecutionMode", "Invalid actionExecutionMode. Valid: before, after, instead");
            }
        }

        if (loc.actions != null) {
            validateActions(result, loc.actions, prefix + ".actions");
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

    private static void validateActions(ValidationResult result, RegionSpawnsConfig.ActionsConfig actions, String prefix) {
        if (actions.commands != null) {
            for (int i = 0; i < actions.commands.size(); i++) {
                RegionSpawnsConfig.CommandActionEntry cmd = actions.commands.get(i);
                String cp = prefix + ".commands[" + i + "]";

                Validators.notBlank(result, cp + ".command", cmd.command, "Command cannot be empty");
                Validators.min(result, cp + ".chance", cmd.chance, 0, "Command chance cannot be negative");
                Validators.max(result, cp + ".chance", cmd.chance, 100, "Command chance cannot exceed 100");

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