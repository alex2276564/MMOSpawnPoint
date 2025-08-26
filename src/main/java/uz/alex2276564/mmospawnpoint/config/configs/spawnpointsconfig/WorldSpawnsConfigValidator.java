package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.mmospawnpoint.config.utils.validation.Validators;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
public class WorldSpawnsConfigValidator {

    public static void validate(WorldSpawnsConfig config, String fileName) {
        ValidationResult result = new ValidationResult();
        validateWorldSpawns(result, config.worldSpawns);
        result.throwIfInvalid("World spawns configuration (" + fileName + ")");
    }

    private static void validateWorldSpawns(ValidationResult result, List<WorldSpawnsConfig.WorldSpawnEntry> spawns) {
        if (spawns == null || spawns.isEmpty()) {
            result.addError("worldSpawns", "World spawns list cannot be empty");
            return;
        }

        for (int i = 0; i < spawns.size(); i++) {
            WorldSpawnsConfig.WorldSpawnEntry entry = spawns.get(i);
            String prefix = "worldSpawns[" + i + "]";
            validateWorldSpawnEntry(result, entry, prefix);
        }
    }

    private static void validateWorldSpawnEntry(ValidationResult result, WorldSpawnsConfig.WorldSpawnEntry entry, String prefix) {
        Validators.notBlank(result, prefix + ".world", entry.world, "World cannot be empty");

        // worldMatchMode
        if (entry.worldMatchMode != null) {
            Set<String> modes = Set.of("exact", "regex");
            if (!modes.contains(entry.worldMatchMode.toLowerCase(Locale.ROOT))) {
                result.addError(prefix + ".worldMatchMode", "Invalid match mode. Valid: exact, regex");
            }
        }
        if ("regex".equalsIgnoreCase(entry.worldMatchMode)) {
            try {
                Pattern.compile(entry.world);
            } catch (Exception e) {
                result.addError(prefix + ".world", "Invalid regex: " + e.getMessage());
            }
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