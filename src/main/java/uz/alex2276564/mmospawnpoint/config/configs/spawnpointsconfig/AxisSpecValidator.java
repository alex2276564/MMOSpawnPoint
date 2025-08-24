package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;

@UtilityClass
public final class AxisSpecValidator {

    public static void validateAxisSpec(ValidationResult result, RegionSpawnsConfig.AxisSpec axis, String prefix) {
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
            // Range sanity
            if (Double.compare(axis.min, axis.max) == 0) {
                result.addError(prefix + ".min/max",
                        "Range collapses to a single value (min == max). Use 'value: " + axis.min + "' for a fixed coordinate.");
            } else if (Double.compare(axis.min, axis.max) > 0) {
                result.addError(prefix + ".min/max",
                        "Invalid range: min (" + axis.min + ") must be strictly less than max (" + axis.max + "). " +
                                "Note: with negative numbers, -188 < -187.");
            }
        }
    }
}