package uz.alex2276564.smartspawnpoint.config;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.model.SpawnAction;
import uz.alex2276564.smartspawnpoint.model.SpawnCondition;
import uz.alex2276564.smartspawnpoint.model.SpawnLocation;
import uz.alex2276564.smartspawnpoint.model.SpawnPoint;
import uz.alex2276564.smartspawnpoint.util.SafeLocationFinder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    private final SmartSpawnPoint plugin;
    private FileConfiguration config;

    @Getter
    private final List<SpawnPoint> regionSpawns;

    @Getter
    private final Map<String, List<SpawnPoint>> worldSpawns;

    @Getter
    private int maxSafeLocationAttempts;

    @Getter
    private int safeLocationRadius;

    @Getter
    private boolean safeCacheEnabled;

    @Getter
    private int safeCacheExpiryTime;

    @Getter
    private int safeCacheMaxSize;

    @Getter
    private boolean safeCacheFixedEnabled;

    @Getter
    private boolean safeCacheFixedPlayerSpecific;

    @Getter
    private boolean safeCacheRandomEnabled;

    @Getter
    private boolean safeCacheRandomPlayerSpecific;

    @Getter
    private boolean safeCacheWeightedEnabled;

    @Getter
    private boolean safeCacheWeightedPlayerSpecific;

    @Getter
    private boolean safeCacheClearOnWorldChange;

    @Getter
    private boolean safeCacheClearPlayerOnWorldChange;

    @Getter
    private boolean safeCacheDebug;

    @Getter
    private boolean debugMode;

    @Getter
    private boolean useWaitingRoom;

    @Getter
    private Location waitingRoomLocation;

    @Getter
    private int asyncSearchTimeout;

    @Getter
    private boolean forceDelayedTeleport;

    // Party system settings
    @Getter
    private boolean partyEnabled;

    @Getter
    private int partyMaxSize;

    @Getter
    private int partyMaxRespawnDistance;

    @Getter
    private int partyRespawnCooldown;

    @Getter
    private int partyInvitationExpiry;

    @Getter
    private Map<String, String> partyMessages;

    @Getter
    private boolean respawnAtDeathEnabled;

    @Getter
    private String respawnAtDeathPermission;

    @Getter
    private String respawnAtDeathMessage;

    @Getter
    private boolean respawnAtDeathRespectRestrictions;

    @Getter
    private String respawnAtDeathRestrictedBehavior;

    @Getter
    private boolean respawnAtDeathCheckDeathLocation;

    @Getter
    private boolean respawnAtDeathCheckTargetLocation;

    @Getter
    private String respawnAtDeathRestrictedMessage;

    @Getter
    private boolean partyCheckDeathLocation;

    @Getter
    private boolean partyCheckTargetLocation;

    @Getter
    private String partyDeathRestrictedBehavior;

    @Getter
    private String partyTargetRestrictedBehavior;

    @Getter
    private String partyBothRestrictedBehavior;

    @Getter
    private boolean partyFindAlternativeTarget;

    @Getter
    private int partyAlternativeTargetAttempts;

    @Getter
    private String partyTargetSelectionPrimary;

    @Getter
    private String partyTargetSelectionFallback;

    @Getter
    private boolean partyPreferSameWorld;

    @Getter
    private boolean partyPreferLeader;

    @Getter
    private boolean partyConsiderWorldPopulation;

    @Getter
    private boolean partyConsiderRegionPopulation;

    @Getter
    private int partyMinPopulationThreshold;

    @Getter
    private int partyMaxAlternativeAttempts;

    @Getter
    private Map<String, String> commandMessages;

    public ConfigManager(SmartSpawnPoint plugin) {
        this.plugin = plugin;
        this.regionSpawns = new ArrayList<>();
        this.worldSpawns = new HashMap<>();
        this.partyMessages = new HashMap<>();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadConfig();
    }

    private void loadConfig() {
        // Clear previous data
        regionSpawns.clear();
        worldSpawns.clear();

        // Load region-based spawns
        loadRegionSpawns();

        // Load world-based spawns
        loadWorldSpawns();

        // Load general settings
        loadSettings();

        // Load party settings
        loadPartySettings();

        // Log loaded config
        if (debugMode) {
            logLoadedConfig();
        }
    }

    private void loadRegionSpawns() {
        List<Map<?, ?>> regionSpawnsList = config.getMapList("region-spawns");
        for (Map<?, ?> spawnMap : regionSpawnsList) {
            String region = String.valueOf(spawnMap.get("region"));
            String regionWorld = spawnMap.containsKey("region-world") ?
                    String.valueOf(spawnMap.get("region-world")) : "*";
            String type = String.valueOf(spawnMap.get("type"));

            SpawnPoint spawnPoint = new SpawnPoint();
            spawnPoint.setRegion(region);
            spawnPoint.setRegionWorld(regionWorld);
            spawnPoint.setType(type);

            // Check if party respawn is disabled
            if (spawnMap.containsKey("party-respawn-disabled")) {
                spawnPoint.setPartyRespawnDisabled(getBooleanValue(spawnMap, "party-respawn-disabled", false));
            }

            // Load location based on type
            if ("fixed".equals(type)) {
                if (spawnMap.containsKey("location")) {
                    Map<?, ?> locationMap = (Map<?, ?>) spawnMap.get("location");
                    SpawnLocation location = parseFixedLocation(locationMap);
                    spawnPoint.setLocation(location);
                }
            } else if ("random".equals(type)) {
                if (spawnMap.containsKey("location")) {
                    Map<?, ?> locationMap = (Map<?, ?>) spawnMap.get("location");
                    SpawnLocation location = parseRandomLocation(locationMap);
                    spawnPoint.setLocation(location);
                }
            } else if ("weighted_random".equals(type)) {
                List<Map<?, ?>> locationsList = (List<Map<?, ?>>) spawnMap.get("locations");
                List<SpawnLocation> weightedLocations = new ArrayList<>();

                if (locationsList != null) {
                    for (Map<?, ?> locationEntry : locationsList) {
                        // Get location type
                        String locationType = locationEntry.containsKey("type") ?
                                String.valueOf(locationEntry.get("type")) : "fixed";

                        SpawnLocation location;
                        if ("fixed".equals(locationType)) {
                            Map<?, ?> locationMap = (Map<?, ?>) locationEntry.get("location");
                            location = parseFixedLocation(locationMap);
                        } else if ("random".equals(locationType)) {
                            Map<?, ?> locationMap = (Map<?, ?>) locationEntry.get("location");
                            location = parseRandomLocation(locationMap);
                        } else {
                            // Default to fixed if unknown type
                            Map<?, ?> locationMap = (Map<?, ?>) locationEntry.get("location");
                            location = parseFixedLocation(locationMap);
                        }

                        // Set weight
                        int weight = locationEntry.containsKey("weight") ?
                                getIntValue(locationEntry, "weight") : 100;
                        location.setWeight(weight);

                        // Load conditional weights
                        if (locationEntry.containsKey("weight-conditions")) {
                            List<Map<?, ?>> weightConditionsList = (List<Map<?, ?>>) locationEntry.get("weight-conditions");
                            List<SpawnCondition> weightConditions = parseWeightConditions(weightConditionsList);
                            location.setWeightConditions(weightConditions);
                        }

                        // Load custom waiting room for this location
                        if (locationEntry.containsKey("waiting-room")) {
                            Map<?, ?> waitingRoomMap = (Map<?, ?>) locationEntry.get("waiting-room");
                            SpawnLocation waitingRoom = parseFixedLocation(waitingRoomMap);
                            location.setWaitingRoom(waitingRoom);
                        }

                        weightedLocations.add(location);
                    }
                }

                spawnPoint.setWeightedLocations(weightedLocations);
            }

            // Load conditions
            if (spawnMap.containsKey("conditions")) {
                Map<?, ?> conditionsMap = (Map<?, ?>) spawnMap.get("conditions");
                List<SpawnCondition> conditions = parseConditions(conditionsMap);
                spawnPoint.setConditions(conditions);
            }

            // Load actions
            if (spawnMap.containsKey("actions")) {
                Map<?, ?> actionsMap = (Map<?, ?>) spawnMap.get("actions");
                List<SpawnAction> actions = parseActions(actionsMap);
                spawnPoint.setActions(actions);
            }

            // Load custom waiting room if specified
            if (spawnMap.containsKey("waiting-room")) {
                Map<?, ?> waitingRoom = (Map<?, ?>) spawnMap.get("waiting-room");
                SpawnLocation waitingLocation = parseFixedLocation(waitingRoom);
                spawnPoint.setWaitingRoom(waitingLocation);
            }

            regionSpawns.add(spawnPoint);
        }
    }

    private void loadWorldSpawns() {
        List<Map<?, ?>> worldSpawnsList = config.getMapList("world-spawns");
        for (Map<?, ?> spawnMap : worldSpawnsList) {
            String world = String.valueOf(spawnMap.get("world"));
            String type = String.valueOf(spawnMap.get("type"));

            SpawnPoint spawnPoint = new SpawnPoint();
            spawnPoint.setWorld(world);
            spawnPoint.setType(type);

            // Check if party respawn is disabled
            if (spawnMap.containsKey("party-respawn-disabled")) {
                spawnPoint.setPartyRespawnDisabled(getBooleanValue(spawnMap, "party-respawn-disabled", false));
            }

            // Load location based on type
            if ("fixed".equals(type)) {
                if (spawnMap.containsKey("location")) {
                    Map<?, ?> locationMap = (Map<?, ?>) spawnMap.get("location");
                    SpawnLocation location = parseFixedLocation(locationMap);
                    spawnPoint.setLocation(location);
                }
            } else if ("random".equals(type)) {
                if (spawnMap.containsKey("location")) {
                    Map<?, ?> locationMap = (Map<?, ?>) spawnMap.get("location");
                    SpawnLocation location = parseRandomLocation(locationMap);
                    spawnPoint.setLocation(location);
                }
            } else if ("weighted_random".equals(type)) {
                List<Map<?, ?>> locationsList = (List<Map<?, ?>>) spawnMap.get("locations");
                List<SpawnLocation> weightedLocations = new ArrayList<>();

                if (locationsList != null) {
                    for (Map<?, ?> locationEntry : locationsList) {
                        // Get location type
                        String locationType = locationEntry.containsKey("type") ?
                                String.valueOf(locationEntry.get("type")) : "fixed";

                        SpawnLocation location;
                        if ("fixed".equals(locationType)) {
                            Map<?, ?> locationMap = (Map<?, ?>) locationEntry.get("location");
                            location = parseFixedLocation(locationMap);
                        } else if ("random".equals(locationType)) {
                            Map<?, ?> locationMap = (Map<?, ?>) locationEntry.get("location");
                            location = parseRandomLocation(locationMap);
                        } else {
                            // Default to fixed if unknown type
                            Map<?, ?> locationMap = (Map<?, ?>) locationEntry.get("location");
                            location = parseFixedLocation(locationMap);
                        }

                        // Set weight
                        int weight = locationEntry.containsKey("weight") ?
                                getIntValue(locationEntry, "weight") : 100;
                        location.setWeight(weight);

                        // Load conditional weights
                        if (locationEntry.containsKey("weight-conditions")) {
                            List<Map<?, ?>> weightConditionsList = (List<Map<?, ?>>) locationEntry.get("weight-conditions");
                            List<SpawnCondition> weightConditions = parseWeightConditions(weightConditionsList);
                            location.setWeightConditions(weightConditions);
                        }

                        // Load custom waiting room for this location
                        if (locationEntry.containsKey("waiting-room")) {
                            Map<?, ?> waitingRoomMap = (Map<?, ?>) locationEntry.get("waiting-room");
                            SpawnLocation waitingRoom = parseFixedLocation(waitingRoomMap);
                            location.setWaitingRoom(waitingRoom);
                        }

                        weightedLocations.add(location);
                    }
                }

                spawnPoint.setWeightedLocations(weightedLocations);
            }

            // Load conditions
            if (spawnMap.containsKey("conditions")) {
                Map<?, ?> conditionsMap = (Map<?, ?>) spawnMap.get("conditions");
                List<SpawnCondition> conditions = parseConditions(conditionsMap);
                spawnPoint.setConditions(conditions);
            }

            // Load actions
            if (spawnMap.containsKey("actions")) {
                Map<?, ?> actionsMap = (Map<?, ?>) spawnMap.get("actions");
                List<SpawnAction> actions = parseActions(actionsMap);
                spawnPoint.setActions(actions);
            }

            // Load custom waiting room if specified
            if (spawnMap.containsKey("waiting-room")) {
                Map<?, ?> waitingRoom = (Map<?, ?>) spawnMap.get("waiting-room");
                SpawnLocation waitingLocation = parseFixedLocation(waitingRoom);
                spawnPoint.setWaitingRoom(waitingLocation);
            }

            // Add to world spawns map
            if (!worldSpawns.containsKey(world)) {
                worldSpawns.put(world, new ArrayList<>());
            }
            worldSpawns.get(world).add(spawnPoint);
        }
    }

    private void loadPartySettings() {
        // Load party system settings
        partyEnabled = config.getBoolean("settings.party.enabled", true);
        partyMaxSize = config.getInt("settings.party.max-size", 10);
        partyMaxRespawnDistance = config.getInt("settings.party.max-respawn-distance", 0);
        partyRespawnCooldown = config.getInt("settings.party.respawn-cooldown", 0);
        partyInvitationExpiry = config.getInt("settings.party.invitation-expiry", 60);

        // Log unlimited settings information
        if (partyMaxRespawnDistance <= 0) {
            plugin.getLogger().info("Party max respawn distance is set to unlimited");
        }

        if (partyRespawnCooldown <= 0) {
            plugin.getLogger().info("Party respawn cooldown is set to unlimited");
        }

        respawnAtDeathEnabled = config.getBoolean("settings.party.respawn-at-death.enabled", false);

        respawnAtDeathPermission = config.getString("settings.party.respawn-at-death.permission", "smartspawnpoint.party.respawnatdeath");

        respawnAtDeathMessage = config.getString("settings.party.respawn-at-death.message", "&aYou have respawned at your death location as a walking spawn point");

        // Load walking spawn point restriction settings
        respawnAtDeathRespectRestrictions = config.getBoolean("settings.party.respawn-at-death.restriction-behavior.respect-restrictions", true);
        respawnAtDeathRestrictedBehavior = config.getString("settings.party.respawn-at-death.restriction-behavior.restricted-area-behavior", "deny");
        respawnAtDeathCheckDeathLocation = config.getBoolean("settings.party.respawn-at-death.restriction-behavior.check-death-location", true);
        respawnAtDeathCheckTargetLocation = config.getBoolean("settings.party.respawn-at-death.restriction-behavior.check-target-location", false);
        respawnAtDeathRestrictedMessage = config.getString("settings.party.respawn-at-death.restriction-behavior.restricted-message", "&cWalking spawn point is disabled in this area.");

        // Load party respawn behavior settings
        partyCheckDeathLocation = config.getBoolean("settings.party.respawn-behavior.check-death-location", true);
        partyCheckTargetLocation = config.getBoolean("settings.party.respawn-behavior.check-target-location", true);
        partyDeathRestrictedBehavior = config.getString("settings.party.respawn-behavior.death-restricted-behavior", "allow");
        partyTargetRestrictedBehavior = config.getString("settings.party.respawn-behavior.target-restricted-behavior", "deny");
        partyBothRestrictedBehavior = config.getString("settings.party.respawn-behavior.both-restricted-behavior", "deny");
        partyFindAlternativeTarget = config.getBoolean("settings.party.respawn-behavior.find-alternative-target", true);
        partyAlternativeTargetAttempts = config.getInt("settings.party.respawn-behavior.alternative-target-attempts", 3);

        // Load target selection settings
        partyTargetSelectionPrimary = config.getString("settings.party.respawn-behavior.target-selection.primary-strategy", "closest_same_world");
        partyTargetSelectionFallback = config.getString("settings.party.respawn-behavior.target-selection.fallback-strategy", "closest_any_world");
        partyPreferSameWorld = config.getBoolean("settings.party.respawn-behavior.target-selection.prefer-same-world", true);
        partyPreferLeader = config.getBoolean("settings.party.respawn-behavior.target-selection.prefer-leader", false);
        partyConsiderWorldPopulation = config.getBoolean("settings.party.respawn-behavior.target-selection.consider-world-population", false);
        partyConsiderRegionPopulation = config.getBoolean("settings.party.respawn-behavior.target-selection.consider-region-population", false);
        partyMinPopulationThreshold = config.getInt("settings.party.respawn-behavior.target-selection.min-population-threshold", 2);
        partyMaxAlternativeAttempts = config.getInt("settings.party.respawn-behavior.target-selection.max-alternative-attempts", 3);

        // Load party messages
        if (config.contains("settings.party.messages")) {
            ConfigurationSection messagesSection = config.getConfigurationSection("settings.party.messages");
            if (messagesSection != null) {
                for (String key : messagesSection.getKeys(false)) {
                    partyMessages.put(key, messagesSection.getString(key, ""));
                }
            }
        }
    }

    private List<SpawnCondition> parseWeightConditions(List<Map<?, ?>> weightConditionsList) {
        List<SpawnCondition> conditions = new ArrayList<>();

        for (Map<?, ?> conditionMap : weightConditionsList) {
            String type = String.valueOf(conditionMap.get("type"));
            String value = String.valueOf(conditionMap.get("value"));
            int weight = conditionMap.containsKey("weight") ?
                    getIntValue(conditionMap, "weight") : 100;

            SpawnCondition condition = new SpawnCondition();
            condition.setType(type);
            condition.setValue(value);
            condition.setWeight(weight);

            conditions.add(condition);
        }

        return conditions;
    }

    private void loadSettings() {
        maxSafeLocationAttempts = config.getInt("settings.max-safe-location-attempts", 20);
        safeLocationRadius = config.getInt("settings.safe-location-radius", 5);
        // Load safe location cache settings
        safeCacheEnabled = config.getBoolean("settings.safe-location-cache.enabled", true);
        safeCacheExpiryTime = config.getInt("settings.safe-location-cache.expiry-time", 60);
        safeCacheMaxSize = config.getInt("settings.safe-location-cache.max-cache-size", 1000);

        safeCacheFixedEnabled = config.getBoolean("settings.safe-location-cache.spawn-type-caching.fixed-spawns.enabled", true);
        safeCacheFixedPlayerSpecific = config.getBoolean("settings.safe-location-cache.spawn-type-caching.fixed-spawns.player-specific", false);

        safeCacheRandomEnabled = config.getBoolean("settings.safe-location-cache.spawn-type-caching.random-spawns.enabled", true);
        safeCacheRandomPlayerSpecific = config.getBoolean("settings.safe-location-cache.spawn-type-caching.random-spawns.player-specific", true);

        safeCacheWeightedEnabled = config.getBoolean("settings.safe-location-cache.spawn-type-caching.weighted-random-spawns.enabled", true);
        safeCacheWeightedPlayerSpecific = config.getBoolean("settings.safe-location-cache.spawn-type-caching.weighted-random-spawns.player-specific", false);

        safeCacheClearOnWorldChange = config.getBoolean("settings.safe-location-cache.advanced.clear-on-world-change", false);
        safeCacheClearPlayerOnWorldChange = config.getBoolean("settings.safe-location-cache.advanced.clear-player-cache-on-world-change", true);
        safeCacheDebug = config.getBoolean("settings.safe-location-cache.advanced.debug-cache", false);

        // Apply cache settings to SafeLocationFinder
        SafeLocationFinder.configureCaching(
                safeCacheEnabled,
                safeCacheExpiryTime * 1000L, // Convert to milliseconds
                safeCacheMaxSize,
                safeCacheDebug
        );

        debugMode = config.getBoolean("settings.debug-mode", false);
        forceDelayedTeleport = config.getBoolean("settings.force-delayed-teleport", true);

        // Load async search settings
        useWaitingRoom = config.getBoolean("settings.waiting-room.enabled", false);
        asyncSearchTimeout = config.getInt("settings.waiting-room.async-search-timeout", 5);

        // Load global waiting room
        if (useWaitingRoom && config.contains("settings.waiting-room.location")) {
            String worldName = config.getString("settings.waiting-room.location.world", "world");
            double x = config.getDouble("settings.waiting-room.location.x", 0);
            double y = config.getDouble("settings.waiting-room.location.y", 64);
            double z = config.getDouble("settings.waiting-room.location.z", 0);
            float yaw = (float) config.getDouble("settings.waiting-room.location.yaw", 0);
            float pitch = (float) config.getDouble("settings.waiting-room.location.pitch", 0);

            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                waitingRoomLocation = new Location(world, x, y, z, yaw, pitch);
                plugin.getLogger().info("Global waiting room loaded at " + worldName + ": " + x + ", " + y + ", " + z);
            } else {
                useWaitingRoom = false;
                plugin.getLogger().warning("Could not find world " + worldName + " for global waiting room. Disabling waiting room.");
            }
        }

        // Load unsafe materials
        List<String> materialList = config.getStringList("settings.unsafe-materials");
        Set<Material> unsafeMaterials = new HashSet<>();

        if (!materialList.isEmpty()) {
            for (String materialName : materialList) {
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    unsafeMaterials.add(material);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown material in unsafe-materials list: " + materialName);
                }
            }

            if (!unsafeMaterials.isEmpty()) {
                // Update the unsafe materials list
                SafeLocationFinder.setUnsafeMaterials(unsafeMaterials);
            }
        }
        // Load command messages
        commandMessages = new HashMap<>();
        if (config.contains("settings.command-messages")) {
            ConfigurationSection commandSection = config.getConfigurationSection("settings.command-messages");
            if (commandSection != null) {
                for (String key : commandSection.getKeys(false)) {
                    commandMessages.put(key, commandSection.getString(key, ""));
                }
            }
        }
    }

    // Add helper method to get command messages
    public String getCommandMessage(String key) {
        return commandMessages.getOrDefault(key, "").replace("&", "§");
    }

    private SpawnLocation parseFixedLocation(Map<?, ?> locationMap) {
        SpawnLocation location = new SpawnLocation();
        location.setLocationType("fixed");

        // Get world name, use "world" as default
        String world = locationMap.containsKey("world") ?
                String.valueOf(locationMap.get("world")) : "world";
        location.setWorld(world);

        // Get coordinates
        double x = getDoubleValue(locationMap, "x", 0.0);
        double y = getDoubleValue(locationMap, "y", 64.0);
        double z = getDoubleValue(locationMap, "z", 0.0);
        location.setX(x);
        location.setY(y);
        location.setZ(z);

        // Get rotation (optional)
        if (locationMap.containsKey("yaw")) {
            float yaw = getFloatValue(locationMap, "yaw", 0.0f);
            location.setYaw(yaw);
        }

        if (locationMap.containsKey("pitch")) {
            float pitch = getFloatValue(locationMap, "pitch", 0.0f);
            location.setPitch(pitch);
        }

        // Check if safe location is required
        boolean requireSafe = getBooleanValue(locationMap, "require-safe", false);
        location.setRequireSafe(requireSafe);

        return location;
    }

    private SpawnLocation parseRandomLocation(Map<?, ?> locationMap) {
        SpawnLocation location = new SpawnLocation();
        location.setLocationType("random");

        // Get world name, use "world" as default
        String world = locationMap.containsKey("world") ?
                String.valueOf(locationMap.get("world")) : "world";
        location.setWorld(world);

        // Get coordinate ranges
        double minX = getDoubleValue(locationMap, "min-x", -100.0);
        double maxX = getDoubleValue(locationMap, "max-x", 100.0);
        double minY = getDoubleValue(locationMap, "min-y", 64.0);
        double maxY = getDoubleValue(locationMap, "max-y", 120.0);
        double minZ = getDoubleValue(locationMap, "min-z", -100.0);
        double maxZ = getDoubleValue(locationMap, "max-z", 100.0);

        location.setMinX(minX);
        location.setMaxX(maxX);
        location.setMinY(minY);
        location.setMaxY(maxY);
        location.setMinZ(minZ);
        location.setMaxZ(maxZ);

        // Check if safe location is required
        boolean requireSafe = getBooleanValue(locationMap, "require-safe", false);
        location.setRequireSafe(requireSafe);

        return location;
    }

    private List<SpawnCondition> parseConditions(Map<?, ?> conditionsMap) {
        List<SpawnCondition> conditions = new ArrayList<>();

        // Parse permission conditions
        if (conditionsMap.containsKey("permissions")) {
            List<?> permissions = (List<?>) conditionsMap.get("permissions");
            for (Object permObj : permissions) {
                String permission = String.valueOf(permObj);
                SpawnCondition condition = new SpawnCondition();
                condition.setType("permission");
                condition.setValue(permission);
                conditions.add(condition);
            }
        }

        // Parse placeholder conditions
        if (conditionsMap.containsKey("placeholders")) {
            List<?> placeholders = (List<?>) conditionsMap.get("placeholders");
            for (Object phObj : placeholders) {
                String placeholder = String.valueOf(phObj);
                SpawnCondition condition = new SpawnCondition();
                condition.setType("placeholder");
                condition.setValue(placeholder);
                conditions.add(condition);
            }
        }

        return conditions;
    }

    private List<SpawnAction> parseActions(Map<?, ?> actionsMap) {
        List<SpawnAction> actions = new ArrayList<>();

        // Parse message actions
        if (actionsMap.containsKey("messages")) {
            List<?> messages = (List<?>) actionsMap.get("messages");
            for (Object msgObj : messages) {
                String message = String.valueOf(msgObj);
                SpawnAction action = new SpawnAction();
                action.setType("message");
                action.setValue(message);
                action.setChance(100); // Default 100% chance
                actions.add(action);
            }
        }

        // Parse command actions
        if (actionsMap.containsKey("commands")) {
            List<Map<?, ?>> commands = (List<Map<?, ?>>) actionsMap.get("commands");
            for (Map<?, ?> commandMap : commands) {
                String command = String.valueOf(commandMap.get("command"));
                int chance = 100; // Default 100% chance

                if (commandMap.containsKey("chance")) {
                    chance = getIntValue(commandMap, "chance");
                }

                SpawnAction action = new SpawnAction();
                action.setType("command");
                action.setValue(command);
                action.setChance(chance);

                // Parse conditional chances
                if (commandMap.containsKey("chance-conditions")) {
                    List<Map<?, ?>> chanceConditionsList = (List<Map<?, ?>>) commandMap.get("chance-conditions");
                    List<SpawnCondition> chanceConditions = parseWeightConditions(chanceConditionsList);
                    action.setChanceConditions(chanceConditions);
                }

                actions.add(action);
            }
        }

        return actions;
    }

    // Helper methods for safe type conversion
    private int getIntValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getDoubleValue(Map<?, ?> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private float getFloatValue(Map<?, ?> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanValue(Map<?, ?> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    private void logLoadedConfig() {
        plugin.getLogger().info("Loaded " + regionSpawns.size() + " region-based spawn points");
        plugin.getLogger().info("Loaded " + worldSpawns.size() + " world-based spawn points");
        plugin.getLogger().info("Max safe location attempts: " + maxSafeLocationAttempts);
        plugin.getLogger().info("Safe location radius: " + safeLocationRadius);
        plugin.getLogger().info("Waiting room enabled: " + useWaitingRoom);
        plugin.getLogger().info("Force delayed teleport: " + forceDelayedTeleport);
        plugin.getLogger().info("Party system enabled: " + partyEnabled);
    }

    // Get a party message with a specific key
    public String getPartyMessage(String key) {
        return partyMessages.getOrDefault(key, "");
    }

    // Format a party message with replacements
    public String formatPartyMessage(String key, Map<String, String> replacements) {
        String message = getPartyMessage(key);

        if (message.isEmpty()) {
            return "";
        }

        // Replace placeholders (we'll do this before the colors so as not to affect the color codes in the substitutions)
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }

        // Replace color codes
        message = message.replace("&", "§");

        // Add prefix if available
        String prefix = getPartyMessage("prefix");
        if (!prefix.isEmpty()) {
            prefix = prefix.replace("&", "§");
            return prefix + message;
        }

        return message;
    }
}