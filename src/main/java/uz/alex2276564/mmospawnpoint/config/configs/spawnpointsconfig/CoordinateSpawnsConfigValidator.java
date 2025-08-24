package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.mmospawnpoint.config.utils.validation.Validators;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;

import java.util.List;

@UtilityClass
public class CoordinateSpawnsConfigValidator {

    public static void validate(CoordinateSpawnsConfig config, String fileName) {
        ValidationResult result = new ValidationResult();
        validateCoordinateSpawns(result, config.coordinateSpawns);
        result.throwIfInvalid("Coordinate spawns configuration (" + fileName + ")");
    }

    private static void validateCoordinateSpawns(ValidationResult result, List<CoordinateSpawnsConfig.CoordinateSpawnEntry> spawns) {
        if (spawns == null || spawns.isEmpty()) {
            result.addError("coordinateSpawns", "Coordinate spawns list cannot be empty");
            return;
        }

        for (int i = 0; i < spawns.size(); i++) {
            CoordinateSpawnsConfig.CoordinateSpawnEntry entry = spawns.get(i);
            String prefix = "coordinateSpawns[" + i + "]";
            validateCoordinateSpawnEntry(result, entry, prefix);
        }
    }

    private static void validateCoordinateSpawnEntry(ValidationResult result, CoordinateSpawnsConfig.CoordinateSpawnEntry entry, String prefix) {
        if (entry.triggerArea == null) {
            result.addError(prefix + ".triggerArea", "Trigger area cannot be null");
        } else {
            Validators.notBlank(result, prefix + ".triggerArea.world", entry.triggerArea.world, "Trigger area world cannot be empty");

            boolean hasAnyAxis = entry.triggerArea.x != null || entry.triggerArea.y != null || entry.triggerArea.z != null;
            if (!hasAnyAxis) {
                result.addError(prefix + ".triggerArea", "Trigger area must define at least one axis (x, y, or z)");
            }
            if (entry.triggerArea.x != null)
                AxisSpecValidator.validateAxisSpec(result, entry.triggerArea.x, prefix + ".triggerArea.x");
            if (entry.triggerArea.y != null)
                AxisSpecValidator.validateAxisSpec(result, entry.triggerArea.y, prefix + ".triggerArea.y");
            if (entry.triggerArea.z != null)
                AxisSpecValidator.validateAxisSpec(result, entry.triggerArea.z, prefix + ".triggerArea.z");
        }

        if (entry.destinations != null) {
            for (int j = 0; j < entry.destinations.size(); j++) {
                RegionSpawnsConfig.LocationOption loc = entry.destinations.get(j);
                RegionSpawnsConfigValidator.validateDestination(result, loc, prefix + ".destinations[" + j + "]");
            }
        }

        if (entry.conditions != null) {
            validateConditions(result, entry.conditions, prefix + ".conditions");
        }
        if (entry.actions != null) {
            RegionSpawnsConfigValidator.validateActions(result, entry.actions, prefix + ".actions");
        }
        if (entry.waitingRoom != null) {
            Validators.notBlank(result, prefix + ".waitingRoom.world", entry.waitingRoom.world, "Waiting room world cannot be empty");
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
}