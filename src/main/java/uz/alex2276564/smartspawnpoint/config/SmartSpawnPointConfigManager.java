package uz.alex2276564.smartspawnpoint.config;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Getter;
import org.yaml.snakeyaml.Yaml;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.config.configs.mainconfig.MainConfig;
import uz.alex2276564.smartspawnpoint.config.configs.mainconfig.MainConfigValidator;
import uz.alex2276564.smartspawnpoint.config.configs.messagesconfig.MessagesConfig;
import uz.alex2276564.smartspawnpoint.config.configs.messagesconfig.MessagesConfigValidator;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.*;
import uz.alex2276564.smartspawnpoint.manager.SpawnEntry;
import uz.alex2276564.smartspawnpoint.utils.ResourceUtils;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmartSpawnPointConfigManager {
    private final SmartSpawnPoint plugin;

    @Getter
    private MainConfig mainConfig;

    @Getter
    private MessagesConfig messagesConfig;

    @Getter
    private List<SpawnEntry> allSpawnEntries;

    public SmartSpawnPointConfigManager(SmartSpawnPoint plugin) {
        this.plugin = plugin;
        this.allSpawnEntries = new ArrayList<>();
    }

    public void reload() {
        try {
            loadMainConfig();
            loadMessagesConfig();
            loadSpawnPointConfigs();

            // Apply runtime settings
            applyCacheSettings();

            plugin.getLogger().info("Configuration system reloaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMainConfig() {
        mainConfig = ConfigManager.create(MainConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(new File(plugin.getDataFolder(), "config.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        MainConfigValidator.validate(mainConfig);
        plugin.getLogger().info("Main configuration loaded and validated successfully");
    }

    private void loadMessagesConfig() {
        messagesConfig = ConfigManager.create(MessagesConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(new File(plugin.getDataFolder(), "messages.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        MessagesConfigValidator.validate(messagesConfig);
        plugin.getLogger().info("Messages configuration loaded and validated successfully");
    }

    private void loadSpawnPointConfigs() {
        allSpawnEntries.clear();

        File spawnPointsDir = new File(plugin.getDataFolder(), "spawnpoints");

        if (!spawnPointsDir.exists()) {
            spawnPointsDir.mkdirs();
            createDirectoryStructure();
        } else {
            File examplesFile = new File(spawnPointsDir, "examples.txt");
            ResourceUtils.updateFromResource(plugin, "spawnpoints/examples.txt", examplesFile);
        }

        loadAllSpawnConfigsRecursively(spawnPointsDir, 0);

        // Sort by priority (descending)
        allSpawnEntries.sort((a, b) -> Integer.compare(b.calculatedPriority(), a.calculatedPriority()));

        plugin.getLogger().info("Loaded " + allSpawnEntries.size() + " spawn configuration entries");

        if (mainConfig.settings.debugMode) {
            logSpawnPriorities();
        }
    }

    private void loadAllSpawnConfigsRecursively(File directory, int depth) {
        if (depth > 9) {
            plugin.getLogger().warning("Maximum directory depth (9) exceeded for: " + directory.getPath());
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadAllSpawnConfigsRecursively(file, depth + 1);
            } else if (file.getName().endsWith(".yml") && !file.getName().equals("examples.txt")) {
                loadSpawnConfigFile(file);
            }
        }
    }

    private enum DetectedType {COORDINATE, REGION, WORLD, NONE}

    private void loadSpawnConfigFile(File file) {
        try {
            DetectedType type = detectConfigTypeByContent(file);

            switch (type) {
                case COORDINATE -> loadCoordinateSpawnConfig(file);
                case REGION -> loadRegionSpawnConfig(file);
                case WORLD -> loadWorldSpawnConfig(file);
                case NONE ->
                        plugin.getLogger().warning("Could not auto-detect config type for file: " + file.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load spawn config " + file.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Detect config type by content (first matching top-level key wins).
     * We respect YAML key order (SnakeYAML loads into LinkedHashMap).
     */
    private DetectedType detectConfigTypeByContent(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            Object obj = new Yaml().load(fis);
            if (!(obj instanceof Map<?, ?> map)) {
                return DetectedType.NONE;
            }

            for (Object keyObj : map.keySet()) {
                if (!(keyObj instanceof String key)) continue;
                switch (key) {
                    case "coordinateSpawns" -> {
                        return DetectedType.COORDINATE;
                    }
                    case "regionSpawns" -> {
                        return DetectedType.REGION;
                    }
                    case "worldSpawns" -> {
                        return DetectedType.WORLD;
                    }
                }
            }
            return DetectedType.NONE;
        } catch (Exception e) {
            plugin.getLogger().warning("YAML read error in " + file.getName() + ": " + e.getMessage());
            return DetectedType.NONE;
        }
    }

    private void loadRegionSpawnConfig(File file) {
        RegionSpawnsConfig config = ConfigManager.create(RegionSpawnsConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(file);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        // Validate with your ValidationResult framework
        RegionSpawnsConfigValidator.validate(config, file.getName());

        int filePriority = calculateFilePriority(config.priority, SpawnEntry.Type.REGION);

        for (RegionSpawnsConfig.RegionSpawnEntry entry : config.regionSpawns) {
            int spawnPriority = entry.priority != null ? entry.priority : filePriority;

            SpawnEntry spawnEntry = new SpawnEntry(
                    SpawnEntry.Type.REGION,
                    spawnPriority,
                    config.configType,
                    entry,
                    file.getName()
            );

            allSpawnEntries.add(spawnEntry);
        }

        plugin.getLogger().info("Region spawn configuration loaded: " + file.getName() +
                " (type: " + config.configType + ", priority: " + filePriority + ", entries: " + config.regionSpawns.size() + ")");
    }

    private void loadWorldSpawnConfig(File file) {
        WorldSpawnsConfig config = ConfigManager.create(WorldSpawnsConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(file);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        WorldSpawnsConfigValidator.validate(config, file.getName());

        int filePriority = calculateFilePriority(config.priority, SpawnEntry.Type.WORLD);

        for (WorldSpawnsConfig.WorldSpawnEntry entry : config.worldSpawns) {
            int spawnPriority = entry.priority != null ? entry.priority : filePriority;

            SpawnEntry spawnEntry = new SpawnEntry(
                    SpawnEntry.Type.WORLD,
                    spawnPriority,
                    config.configType,
                    entry,
                    file.getName()
            );

            allSpawnEntries.add(spawnEntry);
        }

        plugin.getLogger().info("World spawn configuration loaded: " + file.getName() +
                " (type: " + config.configType + ", priority: " + filePriority + ", entries: " + config.worldSpawns.size() + ")");
    }

    private void loadCoordinateSpawnConfig(File file) {
        CoordinateSpawnsConfig config = ConfigManager.create(CoordinateSpawnsConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(file);
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        CoordinateSpawnsConfigValidator.validate(config, file.getName());

        int filePriority = calculateFilePriority(config.priority, SpawnEntry.Type.COORDINATE);

        for (CoordinateSpawnsConfig.CoordinateSpawnEntry entry : config.coordinateSpawns) {
            int spawnPriority = entry.priority != null ? entry.priority : filePriority;

            SpawnEntry spawnEntry = new SpawnEntry(
                    SpawnEntry.Type.COORDINATE,
                    spawnPriority,
                    config.configType,
                    entry,
                    file.getName()
            );

            allSpawnEntries.add(spawnEntry);
        }

        plugin.getLogger().info("Coordinate spawn configuration loaded: " + file.getName() +
                " (type: " + config.configType + ", priority: " + filePriority + ", entries: " + config.coordinateSpawns.size() + ")");
    }

    private int calculateFilePriority(Integer configPriority, SpawnEntry.Type type) {
        if (configPriority != null) {
            return configPriority;
        }

        return switch (type) {
            case COORDINATE -> mainConfig.settings.defaultPriorities.coordinate;
            case REGION -> mainConfig.settings.defaultPriorities.region;
            case WORLD -> mainConfig.settings.defaultPriorities.world;
        };
    }

    private void logSpawnPriorities() {
        plugin.getLogger().info("=== Spawn Priority Order ===");
        for (SpawnEntry entry : allSpawnEntries) {
            String spawnName = getSpawnName(entry);

            plugin.getLogger().info(String.format("Priority %d: %s '%s' (%s) from %s",
                    entry.calculatedPriority(),
                    entry.type().name().toLowerCase(),
                    spawnName,
                    entry.configType(),
                    entry.fileName()));
        }
        plugin.getLogger().info("============================");
    }

    private static String getSpawnName(SpawnEntry entry) {
        Object spawnData = entry.spawnData();
        String spawnName = "unknown";

        if (spawnData instanceof RegionSpawnsConfig.RegionSpawnEntry regionEntry) {
            spawnName = regionEntry.region;
        } else if (spawnData instanceof WorldSpawnsConfig.WorldSpawnEntry worldEntry) {
            spawnName = worldEntry.world;
        } else if (spawnData instanceof CoordinateSpawnsConfig.CoordinateSpawnEntry coordEntry) {
            spawnName = coordEntry.coordinates.world + "_coords";
        }
        return spawnName;
    }

    private void createDirectoryStructure() {
        File spawnPointsDir = new File(plugin.getDataFolder(), "spawnpoints");

        File examplesFile = new File(spawnPointsDir, "examples.txt");
        ResourceUtils.updateFromResource(plugin, "spawnpoints/examples.txt", examplesFile);

        File[] existingFiles = spawnPointsDir.listFiles();
        boolean isEmpty = existingFiles == null ||
                (existingFiles.length == 1 && existingFiles[0].getName().equals("examples.txt"));

        if (isEmpty) {
            File dungeonsDir = new File(spawnPointsDir, "dungeons");
            if (!dungeonsDir.exists()) {
                dungeonsDir.mkdirs();

                File forestRegions = new File(dungeonsDir, "forest-regions.yml");
                File desertCoordinates = new File(dungeonsDir, "desert-coordinates.yml");

                ResourceUtils.updateFromResource(plugin, "spawnpoints/exampledungeons/forest/forest-regions.yml", forestRegions);
                ResourceUtils.updateFromResource(plugin, "spawnpoints/exampledungeons/desert/desert-coordinates.yml", desertCoordinates);

                plugin.getLogger().info("Created example dungeon configurations in dungeons/ folder");
            }
        }

        plugin.getLogger().info("Spawn points directory structure ready");
    }

    private void applyCacheSettings() {
        var cacheConfig = mainConfig.settings.safeLocationCache;

        SafeLocationFinder.configureCaching(
                cacheConfig.enabled,
                cacheConfig.expiryTime * 1000L,
                cacheConfig.maxCacheSize,
                cacheConfig.advanced.debugCache
        );

        // apply search radius
        SafeLocationFinder.configureSearchRadius(mainConfig.settings.safeLocationRadius);

        SafeLocationFinder.configureUnsafeMaterials(mainConfig.settings.unsafeMaterials);

        if (mainConfig.settings.debugMode) {
            plugin.getLogger().info("Applied cache settings: enabled=" + cacheConfig.enabled +
                    ", expiry=" + cacheConfig.expiryTime + "s, maxSize=" + cacheConfig.maxCacheSize +
                    ", searchRadius=" + mainConfig.settings.safeLocationRadius);
        }
    }

    public List<SpawnEntry> getSpawnEntriesForEvent(String eventType) {
        return allSpawnEntries.stream()
                .filter(entry -> entry.isForEventType(eventType))
                .toList();
    }

    public List<SpawnEntry> getMatchingSpawnEntries(String eventType, org.bukkit.Location location) {
        return allSpawnEntries.stream()
                .filter(entry -> entry.isForEventType(eventType))
                .filter(entry -> entry.matchesLocation(location))
                .toList();
    }
}