package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.mmospawnpoint.config.utils.validation.Validators;
import uz.alex2276564.mmospawnpoint.utils.PlaceholderUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@UtilityClass
public class SpawnPointsConfigValidator {

    public static void validate(SpawnPointsConfig config, String fileName) {
        ValidationResult result = new ValidationResult();

        if (config.spawns == null || config.spawns.isEmpty()) {
            result.addError("spawns", "Spawn list cannot be empty");
        } else {
            // Runtime hook availability
            MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();
            boolean wgConfigured = plugin.getConfigManager().getMainConfig().hooks.useWorldGuard;
            boolean wgPresent = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
            boolean papiConfigured = plugin.getConfigManager().getMainConfig().hooks.usePlaceholderAPI;
            boolean papiPresent = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;

            for (int i = 0; i < config.spawns.size(); i++) {
                SpawnPointsConfig.SpawnPointEntry e = config.spawns.get(i);
                String p = "spawns[" + i + "]";

                // event
                String event = str(e.event);
                if (!Set.of("death", "join", "both").contains(event)) {
                    result.addError(p + ".event", "Invalid event. Valid: death, join, both");
                }

                // kind
                String kind = str(e.kind);
                if (!Set.of("region", "world", "coordinate").contains(kind)) {
                    result.addError(p + ".kind", "Invalid kind. Valid: region, world, coordinate");
                }

                // priority (optional)
                if (e.priority != null) {
                    Validators.min(result, p + ".priority", e.priority, 0, "Priority must be >= 0");
                    Validators.max(result, p + ".priority", e.priority, 9999, "Priority must be <= 9999");
                }

                // kind-specific required/forbidden
                switch (kind) {
                    case "region" -> {
                        // required
                        Validators.notBlank(result, p + ".region", e.region, "Region cannot be empty");
                        // forbidden fields from other kinds
                        if (e.world != null) result.addError(p + ".world", "Not allowed when kind=region");
                        if (e.triggerArea != null) result.addError(p + ".triggerArea", "Not allowed when kind=region");

                        // WorldGuard hook required
                        if (!wgConfigured) {
                            result.addError(p + ".kind", "WorldGuard usage is disabled by main-config.hooks.useWorldGuard=false");
                        } else if (!wgPresent) {
                            result.addError(p + ".kind", "WorldGuard plugin is not installed but kind=region requires it");
                        }

                        // match modes
                        validateMatchMode(result, e.regionMatchMode, p + ".regionMatchMode");
                        validateMatchMode(result, e.regionWorldMatchMode, p + ".regionWorldMatchMode");

                        if ("regex".equalsIgnoreCase(e.regionMatchMode)) {
                            try {
                                Pattern.compile(e.region);
                            } catch (Exception ex) {
                                result.addError(p + ".region", "Invalid regex: " + ex.getMessage());
                            }
                        }
                        // regionWorld is optional (null -> any world)
                        if (e.regionWorld != null && "regex".equalsIgnoreCase(e.regionWorldMatchMode)) {
                            try {
                                Pattern.compile(e.regionWorld);
                            } catch (Exception ex) {
                                result.addError(p + ".regionWorld", "Invalid regex: " + ex.getMessage());
                            }
                        }
                    }
                    case "world" -> {
                        // required
                        Validators.notBlank(result, p + ".world", e.world, "World cannot be empty");
                        // forbidden
                        if (e.region != null) result.addError(p + ".region", "Not allowed when kind=world");
                        if (e.regionWorld != null) result.addError(p + ".regionWorld", "Not allowed when kind=world");
                        if (e.triggerArea != null) result.addError(p + ".triggerArea", "Not allowed when kind=world");
                        // match modes
                        validateMatchMode(result, e.worldMatchMode, p + ".worldMatchMode");
                        if ("regex".equalsIgnoreCase(e.worldMatchMode)) {
                            try {
                                Pattern.compile(e.world);
                            } catch (Exception ex) {
                                result.addError(p + ".world", "Invalid regex: " + ex.getMessage());
                            }
                        }
                    }
                    case "coordinate" -> {
                        // required
                        if (e.triggerArea == null) {
                            result.addError(p + ".triggerArea", "Trigger area cannot be null (kind=coordinate)");
                        } else {
                            // forbidden
                            if (e.region != null) result.addError(p + ".region", "Not allowed when kind=coordinate");
                            if (e.regionWorld != null)
                                result.addError(p + ".regionWorld", "Not allowed when kind=coordinate");
                            if (e.world != null) result.addError(p + ".world", "Not allowed when kind=coordinate");

                            // validate trigger area
                            Validators.notBlank(result, p + ".triggerArea.world", e.triggerArea.world, "Trigger area world cannot be empty");
                            validateMatchMode(result, e.triggerArea.worldMatchMode, p + ".triggerArea.worldMatchMode");
                            if ("regex".equalsIgnoreCase(e.triggerArea.worldMatchMode)) {
                                try {
                                    Pattern.compile(e.triggerArea.world);
                                } catch (Exception ex) {
                                    result.addError(p + ".triggerArea.world", "Invalid regex: " + ex.getMessage());
                                }
                            }
                            if (e.triggerArea != null) {
                                boolean hasRects = e.triggerArea.rects != null && !e.triggerArea.rects.isEmpty();
                                boolean hasAnyAxis = e.triggerArea.x != null || e.triggerArea.y != null || e.triggerArea.z != null;

                                if (hasRects && hasAnyAxis) {
                                    result.addError(p + ".triggerArea", "Use either rects or axis specs (x/y/z), not both");
                                }
                                if (!hasRects && !hasAnyAxis) {
                                    result.addError(p + ".triggerArea", "Define at least one axis or rects");
                                }
                                // validate rects if present (x/z required, y optional)
                                if (hasRects) {
                                    while (i < e.triggerArea.rects.size()) {
                                        var r = e.triggerArea.rects.get(i);
                                        String rp = p + ".triggerArea.rects[" + i + "]";
                                        if (r == null) {
                                            result.addError(rp, "Rect cannot be null");
                                            continue;
                                        }
                                        if (r.x == null) result.addError(rp + ".x", "Rect.x is required");
                                        else validateAxisSpec(result, r.x, rp + ".x");
                                        if (r.z == null) result.addError(rp + ".z", "Rect.z is required");
                                        else validateAxisSpec(result, r.z, rp + ".z");
                                        if (r.y != null) validateAxisSpec(result, r.y, rp + ".y");
                                    }
                                    // excludeRects
                                    if (e.triggerArea.excludeRects != null) {
                                        while (i < e.triggerArea.excludeRects.size()) {
                                            var r = e.triggerArea.excludeRects.get(i);
                                            String rp = p + ".triggerArea.excludeRects[" + i + "]";
                                            if (r == null) {
                                                result.addError(rp, "Exclude rect cannot be null");
                                                continue;
                                            }
                                            if (r.x == null) result.addError(rp + ".x", "Rect.x is required");
                                            else validateAxisSpec(result, r.x, rp + ".x");
                                            if (r.z == null) result.addError(rp + ".z", "Rect.z is required");
                                            else validateAxisSpec(result, r.z, rp + ".z");
                                            if (r.y != null) validateAxisSpec(result, r.y, rp + ".y");
                                        }
                                    }
                                } else {
                                    // legacy axis checks
                                    if (e.triggerArea.x != null)
                                        validateAxisSpec(result, e.triggerArea.x, p + ".triggerArea.x");
                                    if (e.triggerArea.y != null)
                                        validateAxisSpec(result, e.triggerArea.y, p + ".triggerArea.y");
                                    if (e.triggerArea.z != null)
                                        validateAxisSpec(result, e.triggerArea.z, p + ".triggerArea.z");
                                }

                            }
                        }
                    }
                }

                // common: conditions / actions
                validateConditions(result, e.conditions, p + ".conditions");
                validateActions(result, e.actions, p + ".actions");

                // PlaceholderAPI usage detection across entry
                boolean usesPlaceholder =
                        (e.conditions != null && e.conditions.placeholders != null && !e.conditions.placeholders.isEmpty())
                                || entryUsesPlaceholderInActions(e.actions)
                                || entryUsesPlaceholderInDestinations(e.destinations);

                if (usesPlaceholder) {
                    if (!papiConfigured) {
                        result.addError(p, "Placeholder usage detected but main-config.hooks.usePlaceholderAPI=false");
                    } else if (!papiPresent) {
                        result.addError(p, "Placeholder usage detected but PlaceholderAPI is not installed");
                    }
                }

                // common: destinations
                if (e.destinations != null) {
                    for (int j = 0; j < e.destinations.size(); j++) {
                        validateLocationOption(result, e.destinations.get(j), p + ".destinations[" + j + "]");
                    }
                }

                if (e.waitingRoom != null) {
                    Validators.notBlank(result, p + ".waitingRoom.world", e.waitingRoom.world, "Waiting room world cannot be empty");
                }
            }
        }

        result.throwIfInvalid("SpawnPoints configuration (" + fileName + ")");
    }

    private static boolean entryUsesPlaceholderInActions(SpawnPointsConfig.ActionsConfig actions) {
        if (actions == null || actions.commands == null) return false;
        for (SpawnPointsConfig.CommandActionEntry cmd : actions.commands) {
            if (cmd.chanceConditions == null) continue;
            for (SpawnPointsConfig.ChanceConditionEntry cc : cmd.chanceConditions) {
                if ("placeholder".equalsIgnoreCase(cc.type)) return true;
            }
        }
        return false;
    }

    private static boolean entryUsesPlaceholderInDestinations(List<SpawnPointsConfig.LocationOption> list) {
        if (list == null) return false;
        for (SpawnPointsConfig.LocationOption loc : list) {
            if (loc.weightConditions != null) {
                for (SpawnPointsConfig.WeightConditionEntry wc : loc.weightConditions) {
                    if ("placeholder".equalsIgnoreCase(wc.type)) return true;
                }
            }
            if (loc.actions != null && loc.actions.commands != null) {
                for (SpawnPointsConfig.CommandActionEntry cmd : loc.actions.commands) {
                    if (cmd.chanceConditions == null) continue;
                    for (SpawnPointsConfig.ChanceConditionEntry cc : cmd.chanceConditions) {
                        if ("placeholder".equalsIgnoreCase(cc.type)) return true;
                    }
                }
            }
        }
        return false;
    }

// ----- helpers -----

    private static void validateMatchMode(ValidationResult result, String mode, String path) {
        if (mode == null) return;
        Set<String> modes = Set.of("exact", "regex");
        if (!modes.contains(mode.toLowerCase(Locale.ROOT))) {
            result.addError(path, "Invalid match mode. Valid: exact, regex");
        }
    }

    private static void validateConditions(ValidationResult result, SpawnPointsConfig.ConditionsConfig cond, String prefix) {
        if (cond == null) return;

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

    private static void validateActions(ValidationResult result, SpawnPointsConfig.ActionsConfig actions, String prefix) {
        if (actions == null) return;

        if (actions.messages != null) {
            for (int i = 0; i < actions.messages.size(); i++) {
                SpawnPointsConfig.MessageEntry me = actions.messages.get(i);
                String mp = prefix + ".messages[" + i + "]";
                Validators.notBlank(result, mp + ".text", me.text, "Message text cannot be empty");

                // chance
                Validators.min(result, mp + ".chance", me.chance, 0, "Message chance cannot be negative");
                Validators.max(result, mp + ".chance", me.chance, 100, "Message chance cannot exceed 100");

                if (me.phases != null) {
                    for (int j = 0; j < me.phases.size(); j++) {
                        if (me.phases.get(j) == null) {
                            result.addError(mp + ".phases[" + j + "]", "Phase cannot be null (use BEFORE, WAITING_ROOM, AFTER)");
                        }
                    }
                }

                if (me.chanceConditions != null) {
                    for (int j = 0; j < me.chanceConditions.size(); j++) {
                        SpawnPointsConfig.ChanceConditionEntry cc = me.chanceConditions.get(j);
                        String ccp = mp + ".chanceConditions[" + j + "]";

                        Set<String> validTypes = Set.of("permission", "placeholder");
                        if (!validTypes.contains(cc.type)) {
                            result.addError(ccp + ".type", "Invalid condition type. Valid: permission, placeholder");
                        }

                        Validators.notBlank(result, ccp + ".value", cc.value, "Condition value cannot be empty");

                        if (("placeholder".equalsIgnoreCase(cc.type) || "permission".equalsIgnoreCase(cc.type))
                                && PlaceholderUtils.isInvalidLogicalExpression(cc.value)) {
                            result.addError(ccp + ".value", "Invalid logical expression: " + cc.value);
                        }

                        // mode
                        String mode = (cc.mode == null) ? "set" : cc.mode.toLowerCase(Locale.ROOT);
                        if (!Set.of("set", "add", "mul").contains(mode)) {
                            result.addError(ccp + ".mode", "Invalid mode. Valid: set, add, mul");
                        }

                        // bounds per mode
                        switch (mode) {
                            case "set" -> {
                                Validators.min(result, ccp + ".weight", cc.weight, 0, "For mode=set, weight must be within 0..100");
                                Validators.max(result, ccp + ".weight", cc.weight, 100, "For mode=set, weight must be within 0..100");
                            }
                            case "add" -> {
                                // allow +/-; loosely restrict to [-100..100]
                                Validators.min(result, ccp + ".weight", cc.weight, -100, "For mode=add, weight too small (min -100)");
                                Validators.max(result, ccp + ".weight", cc.weight, 100, "For mode=add, weight too large (max 100)");
                            }
                            case "mul" -> {
                                Validators.min(result, ccp + ".weight", cc.weight, 0, "For mode=mul, weight (multiplier) must be >= 0");
                                Validators.max(result, ccp + ".weight", cc.weight, 10, "For mode=mul, weight (multiplier) too large (max 10)");
                            }
                        }
                    }
                }
            }

        }

        if (actions.commands != null) {
            for (int i = 0; i < actions.commands.size(); i++) {
                SpawnPointsConfig.CommandActionEntry cmd = actions.commands.get(i);
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
                        SpawnPointsConfig.ChanceConditionEntry cc = cmd.chanceConditions.get(j);
                        String ccp = cp + ".chanceConditions[" + j + "]";

                        Set<String> validTypes = Set.of("permission", "placeholder");
                        if (!validTypes.contains(cc.type)) {
                            result.addError(ccp + ".type", "Invalid condition type. Valid: permission, placeholder");
                        }

                        Validators.notBlank(result, ccp + ".value", cc.value, "Condition value cannot be empty");

                        if (("placeholder".equalsIgnoreCase(cc.type) || "permission".equalsIgnoreCase(cc.type))
                                && PlaceholderUtils.isInvalidLogicalExpression(cc.value)) {
                            result.addError(ccp + ".value", "Invalid logical expression: " + cc.value);
                        }

                        // mode
                        String mode = (cc.mode == null) ? "set" : cc.mode.toLowerCase(Locale.ROOT);
                        if (!Set.of("set", "add", "mul").contains(mode)) {
                            result.addError(ccp + ".mode", "Invalid mode. Valid: set, add, mul");
                        }

                        // bounds per mode
                        switch (mode) {
                            case "set" -> {
                                Validators.min(result, ccp + ".weight", cc.weight, 0, "For mode=set, weight must be within 0..100");
                                Validators.max(result, ccp + ".weight", cc.weight, 100, "For mode=set, weight must be within 0..100");
                            }
                            case "add" -> {
                                // allow +/-; loosely restrict to [-100..100]
                                Validators.min(result, ccp + ".weight", cc.weight, -100, "For mode=add, weight too small (min -100)");
                                Validators.max(result, ccp + ".weight", cc.weight, 100, "For mode=add, weight too large (max 100)");
                            }
                            case "mul" -> {
                                Validators.min(result, ccp + ".weight", cc.weight, 0, "For mode=mul, weight (multiplier) must be >= 0");
                                Validators.max(result, ccp + ".weight", cc.weight, 10, "For mode=mul, weight (multiplier) too large (max 10)");
                            }
                        }
                    }
                }
            }
        }
    }

    private static void validateLocationOption(ValidationResult result, SpawnPointsConfig.LocationOption loc, String prefix) {
        if (loc == null) {
            result.addError(prefix, "Destination cannot be null");
            return;
        }

        Validators.notBlank(result, prefix + ".world", loc.world, "Destination world cannot be empty");

        boolean hasRects = loc.rects != null && !loc.rects.isEmpty();
        boolean hasAxis = (loc.x != null || loc.y != null || loc.z != null);
        if (hasRects && hasAxis) {
            result.addError(prefix, "LocationOption: use either rects or x/y/z, not both");
        }

        if (hasRects) {
            for (int i = 0; i < loc.rects.size(); i++) {
                var r = loc.rects.get(i);
                String rp = prefix + ".rects[" + i + "]";
                if (r == null) { result.addError(rp, "Rect cannot be null"); continue; }
                if (r.x == null) result.addError(rp + ".x", "Rect.x is required");
                else validateAxisSpec(result, r.x, rp + ".x");
                if (r.z == null) result.addError(rp + ".z", "Rect.z is required");
                else validateAxisSpec(result, r.z, rp + ".z");
                if (r.y != null) validateAxisSpec(result, r.y, rp + ".y");
            }
            if (loc.excludeRects != null) {
                for (int i = 0; i < loc.excludeRects.size(); i++) {
                    var r = loc.excludeRects.get(i);
                    String rp = prefix + ".excludeRects[" + i + "]";
                    if (r == null) { result.addError(rp, "Exclude rect cannot be null"); continue; }
                    if (r.x == null) result.addError(rp + ".x", "Rect.x is required");
                    else validateAxisSpec(result, r.x, rp + ".x");
                    if (r.z == null) result.addError(rp + ".z", "Rect.z is required");
                    else validateAxisSpec(result, r.z, rp + ".z");
                    if (r.y != null) validateAxisSpec(result, r.y, rp + ".y");
                }
            }
        } else {
            // legacy axis rule
            if (loc.x == null) result.addError(prefix + ".x", "X axis is required (value or range)");
            else validateAxisSpec(result, loc.x, prefix + ".x");
            if (loc.z == null) result.addError(prefix + ".z", "Z axis is required (value or range)");
            else validateAxisSpec(result, loc.z, prefix + ".z");

            if (!loc.requireSafe) {
                if (loc.y == null) {
                    result.addError(prefix + ".y", "Y axis is required when requireSafe=false (use value or range)");
                } else {
                    validateAxisSpec(result, loc.y, prefix + ".y");
                }
            } else if (loc.y != null) {
                validateAxisSpec(result, loc.y, prefix + ".y");
            }
        }

        if (loc.yaw != null) validateAxisSpec(result, loc.yaw, prefix + ".yaw");
        if (loc.pitch != null) validateAxisSpec(result, loc.pitch, prefix + ".pitch");

        Validators.min(result, prefix + ".weight", loc.weight, 1, "Weight must be at least 1");

        if (loc.weightConditions != null) {
            for (int i = 0; i < loc.weightConditions.size(); i++) {
                SpawnPointsConfig.WeightConditionEntry wc = loc.weightConditions.get(i);
                String wcp = prefix + ".weightConditions[" + i + "]";

                Set<String> validTypes = Set.of("permission", "placeholder");
                if (!validTypes.contains(wc.type)) {
                    result.addError(wcp + ".type", "Invalid condition type. Valid: permission, placeholder");
                }
                Validators.notBlank(result, wcp + ".value", wc.value, "Condition value cannot be empty");

                if (("placeholder".equalsIgnoreCase(wc.type) || "permission".equalsIgnoreCase(wc.type))
                        && PlaceholderUtils.isInvalidLogicalExpression(wc.value)) {
                    result.addError(wcp + ".value", "Invalid logical expression: " + wc.value);
                }

                String mode = (wc.mode == null) ? "set" : wc.mode.toLowerCase(Locale.ROOT);
                if (!Set.of("set", "add", "mul").contains(mode)) {
                    result.addError(wcp + ".mode", "Invalid mode. Valid: set, add, mul");
                }

                // bounds for weight:
                // set: >=1; add: allow [-10000..10000]; mul: >=0 and <=10
                switch (mode) {
                    case "set" -> Validators.min(result, wcp + ".weight", wc.weight, 1, "For mode=set, weight must be >= 1");
                    case "add" -> {
                        // allow a wide range; final weight clamped in runtime to >=1
                        Validators.min(result, wcp + ".weight", wc.weight, -10000, "For mode=add, weight too small");
                        Validators.max(result, wcp + ".weight", wc.weight, 10000, "For mode=add, weight too large");
                    }
                    case "mul" -> {
                        Validators.min(result, wcp + ".weight", wc.weight, 0, "For mode=mul, weight (multiplier) must be >= 0");
                        Validators.max(result, wcp + ".weight", wc.weight, 10, "For mode=mul, weight (multiplier) too large (max 10)");
                    }
                }
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

        if (loc.groundWhitelist != null) {
            for (int i = 0; i < loc.groundWhitelist.size(); i++) {
                String name = loc.groundWhitelist.get(i);
                try {
                    Material.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    result.addError(prefix + ".groundWhitelist[" + i + "]", "Unknown material: " + name);
                }
            }
        }
    }

    private static void validateAxisSpec(ValidationResult result, SpawnPointsConfig.AxisSpec axis, String prefix) {
        if (axis == null) {
            result.addError(prefix, "AxisSpec cannot be null");
            return;
        }

        boolean isValue = axis.isValue();
        boolean isRange = axis.isRange();

        if (!isValue && !isRange) {
            result.addError(prefix, "Axis must define either 'value' or BOTH 'min' and 'max'.");
            return;
        }
        if (isValue && isRange) {
            result.addError(prefix, "Axis cannot have both 'value' and 'min/max'. Use exactly one mode.");
            return;
        }

        if (isRange) {
            if (Double.compare(axis.min, axis.max) == 0) {
                result.addError(prefix + ".min/max",
                        "Range collapses to a single value (min == max). Use 'value: " + axis.min + "' for a fixed coordinate.");
            } else if (Double.compare(axis.min, axis.max) > 0) {
                result.addError(prefix + ".min/max",
                        "Invalid range: min (" + axis.min + ") must be strictly less than max (" + axis.max + ").");
            }
        }
    }

    private static String str(String s) {
        return (s == null) ? "" : s.toLowerCase(Locale.ROOT);
    }
}