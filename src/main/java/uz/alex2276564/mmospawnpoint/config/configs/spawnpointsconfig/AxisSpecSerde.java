package uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig;

import eu.okaeri.configs.schema.GenericsDeclaration;
import eu.okaeri.configs.serdes.DeserializationData;
import eu.okaeri.configs.serdes.ObjectSerializer;
import eu.okaeri.configs.serdes.SerializationData;
import lombok.NonNull;

/**
 * Compact ser/de for SpawnPointsConfig.AxisSpec on Okaeri 5.0.9:
 * - scalar number -> value
 * - section with {min, max} -> range
 * - section with {value} -> fixed value
 */
public class AxisSpecSerde implements ObjectSerializer<SpawnPointsConfig.AxisSpec> {

    @Override
    public boolean supports(@NonNull Class<? super SpawnPointsConfig.AxisSpec> type) {
        return SpawnPointsConfig.AxisSpec.class.isAssignableFrom(type);
    }

    @Override
    public void serialize(@NonNull SpawnPointsConfig.AxisSpec object,
                          @NonNull SerializationData data,
                          @NonNull GenericsDeclaration generics) {

        if (object.isValue()) {
            data.setValue(object.value);
            return;
        }

        if (object.isRange()) {
            data.add("min", object.min);
            data.add("max", object.max);
            return;
        }

        // Fallback to explicit form if invalid state (should be prevented by validator)
        if (object.value != null) data.add("value", object.value);
        if (object.min != null) data.add("min", object.min);
        if (object.max != null) data.add("max", object.max);
    }

    @Override
    public SpawnPointsConfig.AxisSpec deserialize(@NonNull DeserializationData data,
                                                  @NonNull GenericsDeclaration generics) {

        SpawnPointsConfig.AxisSpec axis = new SpawnPointsConfig.AxisSpec();

        // Scalar number => value
        if (data.isValue()) {
            axis.value = data.getValue(Double.class);
            return axis;
        }

        // Section => try {value} or {min,max}
        if (data.containsKey("value")) {
            axis.value = data.get("value", Double.class);
            return axis;
        }

        Double min = data.containsKey("min") ? data.get("min", Double.class) : null;
        Double max = data.containsKey("max") ? data.get("max", Double.class) : null;

        axis.min = min;
        axis.max = max;
        return axis;
    }
}