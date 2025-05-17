package uz.alex2276564.smartspawnpoint.model;

import lombok.Data;

@Data
public class SpawnCondition {
    private String type; // "permission" or "placeholder"
    private String value;
    private int weight; // Used for conditional weights
}