package uz.alex2276564.smartspawnpoint.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SpawnPoint {
    private String region; // For region-based spawns
    private String regionWorld; // World for region (can be * for all worlds)
    private String world; // For world-based spawns
    private String type; // "fixed", "random", "weighted_random", "none"
    private SpawnLocation location; // For fixed and random
    private List<SpawnLocation> weightedLocations; // For weighted_random
    private List<SpawnCondition> conditions;
    private List<SpawnAction> actions;
    private SpawnLocation waitingRoom; // Custom waiting room for this spawn point
    private boolean partyRespawnDisabled; // Whether party respawn is disabled for this spawn point

    public SpawnPoint() {
        this.conditions = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.weightedLocations = new ArrayList<>();
        this.regionWorld = "*"; // Default to all worlds
        this.partyRespawnDisabled = false; // Default to allowing party respawn
    }

    // Check if this spawn point has a location (teleport destination)
    public boolean hasLocation() {
        if ("none".equals(type)) {
            return false;
        }

        if ("fixed".equals(type) || "random".equals(type)) {
            return location != null;
        }

        if ("weighted_random".equals(type)) {
            return !weightedLocations.isEmpty();
        }

        return false;
    }
}