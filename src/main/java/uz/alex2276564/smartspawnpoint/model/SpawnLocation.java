package uz.alex2276564.smartspawnpoint.model;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
public class SpawnLocation {
    private static final Random RANDOM = new Random();

    private String locationType; // "fixed" or "random"
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    // For random locations
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double minZ;
    private double maxZ;

    // For weighted random
    private int weight;
    private List<SpawnCondition> weightConditions;

    // Custom waiting room for this specific location
    private SpawnLocation waitingRoom;

    // Whether this location requires safe spawn checking
    private boolean requireSafe = false;

    public SpawnLocation() {
        this.weightConditions = new ArrayList<>();
    }

    public Location toBukkitLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }

        if ("fixed".equals(locationType)) {
            return new Location(bukkitWorld, x, y, z, yaw, pitch);
        } else if ("random".equals(locationType)) {
            double randomX = minX + RANDOM.nextDouble() * (maxX - minX);
            double randomY = minY + RANDOM.nextDouble() * (maxY - minY);
            double randomZ = minZ + RANDOM.nextDouble() * (maxZ - minZ);
            return new Location(bukkitWorld, randomX, randomY, randomZ);
        }

        return null;
    }

    // Calculate effective weight based on conditions
    public int getEffectiveWeight(org.bukkit.entity.Player player) {
        if (weightConditions.isEmpty()) {
            return weight;
        }

        // Check if any condition matches
        for (SpawnCondition condition : weightConditions) {
            if (condition.getType().equals("permission") && player.hasPermission(condition.getValue())) {
                return condition.getWeight();
            } else if (condition.getType().equals("placeholder")) {
                String[] parts = condition.getValue().split(":", 2);
                if (parts.length == 2) {
                    String placeholderCondition = parts[0];
                    if (uz.alex2276564.smartspawnpoint.utils.PlaceholderUtils.checkPlaceholderCondition(player, placeholderCondition)) {
                        return condition.getWeight();
                    }
                }
            }
        }

        // Default to base weight if no conditions match
        return weight;
    }

    // Get waiting room location if available
    public Location getWaitingRoomLocation() {
        if (waitingRoom != null) {
            return waitingRoom.toBukkitLocation();
        }
        return null;
    }
}