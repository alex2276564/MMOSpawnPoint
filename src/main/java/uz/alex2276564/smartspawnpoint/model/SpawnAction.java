package uz.alex2276564.smartspawnpoint.model;

import lombok.Data;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Data
public class SpawnAction {
    private String type; // "message" or "command"
    private String value;
    private int chance; // Percentage (0-100)
    private List<SpawnCondition> chanceConditions;

    public SpawnAction() {
        this.chanceConditions = new ArrayList<>();
    }

    // Get effective chance based on conditions
    public int getEffectiveChance(Player player) {
        if (chanceConditions.isEmpty()) {
            return chance;
        }

        // Check if any condition matches
        for (SpawnCondition condition : chanceConditions) {
            if (condition.getType().equals("permission") && player.hasPermission(condition.getValue())) {
                return condition.getWeight();
            } else if (condition.getType().equals("placeholder")) {
                String[] parts = condition.getValue().split(":", 2);
                if (parts.length == 2) {
                    String placeholderCondition = parts[0];
                    if (uz.alex2276564.smartspawnpoint.util.PlaceholderUtils.checkPlaceholderCondition(player, placeholderCondition)) {
                        return condition.getWeight();
                    }
                }
            }
        }

        // Default to base chance if no conditions match
        return chance;
    }
}