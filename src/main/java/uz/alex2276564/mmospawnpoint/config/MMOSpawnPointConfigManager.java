package uz.alex2276564.mmospawnpoint.config;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Getter;
import org.yaml.snakeyaml.Yaml;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.configs.mainconfig.MainConfig;
import uz.alex2276564.mmospawnpoint.config.configs.mainconfig.MainConfigValidator;
import uz.alex2276564.mmospawnpoint.config.configs.messagesconfig.MessagesConfig;
import uz.alex2276564.mmospawnpoint.config.configs.messagesconfig.MessagesConfigValidator;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.AxisSpecSerde;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfig;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfigValidator;
import uz.alex2276564.mmospawnpoint.manager.SpawnEntry;
import uz.alex2276564.mmospawnpoint.utils.ResourceUtils;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MMOSpawnPointConfigManager {
    private final MMOSpawnPoint plugin;

    @Getter
    private MainConfig mainConfig;

    @Getter
    private MessagesConfig messagesConfig;

    @Getter
    private List<SpawnEntry> allSpawnEntries;

    public MMOSpawnPointConfigManager(MMOSpawnPoint plugin) {
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
        int maxDepth = mainConfig.settings.maintenance.maxFolderDepth;
        if (depth > maxDepth) {
            plugin.getLogger().warning("Maximum directory depth (" + maxDepth + ") exceeded for: " + directory.getPath());
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadAllSpawnConfigsRecursively(file, depth + 1);
            } else if (file.getName().endsWith(".yml") && !file.getName().equalsIgnoreCase("examples.txt")) {
                loadSpawnConfigFile(file);
            }
        }
    }

    private void loadSpawnConfigFile(File file) {
        try {
            // Quick YAML check: require 'spawns' key
            try (FileInputStream fis = new FileInputStream(file)) {
                Object obj = new Yaml().load(fis);
                if (!(obj instanceof Map<?, ?> map)) {
                    plugin.getLogger().warning("Skipping non-YAML or empty file: " + file.getName());
                    return;
                }
                if (!map.containsKey("spawns")) {
                    plugin.getLogger().warning("No 'spawns' key found in " + file.getName() + " — skipping");
                    return;
                }
            } catch (Exception ignored) {
            }

            // Load unified config (no saveDefaults/removeOrphans for user files)
            SpawnPointsConfig config = ConfigManager.create(SpawnPointsConfig.class, it -> {
                it.withConfigurer(new YamlSnakeYamlConfigurer());
                // Register our AxisSpec serde for compact x/y/z syntax
                it.getConfigurer().getRegistry().register(new AxisSpecSerde());
                it.withBindFile(file);
                it.withRemoveOrphans(false);
                it.load();
            });

            SpawnPointsConfigValidator.validate(config, file.getName());

            int added = 0;
            for (SpawnPointsConfig.SpawnPointEntry entry : config.spawns) {
                SpawnEntry.Type type = switch (entry.kind.toLowerCase()) {
                    case "region" -> SpawnEntry.Type.REGION;
                    case "world" -> SpawnEntry.Type.WORLD;
                    case "coordinate" -> SpawnEntry.Type.COORDINATE;
                    default -> null;
                };
                if (type == null) {
                    plugin.getLogger().warning("Unknown kind in " + file.getName() + " — skipping entry.");
                    continue;
                }

                int spawnPriority = (entry.priority != null)
                        ? entry.priority
                        : switch (type) {
                    case COORDINATE -> mainConfig.settings.defaultPriorities.coordinate;
                    case REGION -> mainConfig.settings.defaultPriorities.region;
                    case WORLD -> mainConfig.settings.defaultPriorities.world;
                };

                SpawnEntry spawnEntry = new SpawnEntry(
                        type,
                        spawnPriority,
                        entry.event,
                        entry,
                        file.getName()
                );
                allSpawnEntries.add(spawnEntry);
                added++;
            }

            plugin.getLogger().info("Loaded spawn config: " + file.getName() + " (entries: " + added + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load spawn config " + file.getName() + ": " + e.getMessage());
            if (mainConfig.settings.debugMode) e.printStackTrace();
        }
    }

    private void logSpawnPriorities() {
        plugin.getLogger().info("=== Spawn Priority Order ===");
        for (SpawnEntry entry : allSpawnEntries) {
            String spawnName = getSpawnName(entry);
            plugin.getLogger().info(String.format(
                    "Priority %d: %s '%s' (%s) from %s",
                    entry.calculatedPriority(),
                    entry.type().name().toLowerCase(),
                    spawnName,
                    entry.event(),
                    entry.fileName()
            ));
        }
        plugin.getLogger().info("============================");
    }

    private static String getSpawnName(SpawnEntry entry) {
        SpawnPointsConfig.SpawnPointEntry data = entry.spawnData();
        if (data == null) return "unknown";
        return switch (entry.type()) {
            case REGION -> data.region != null ? data.region : "region";
            case WORLD -> data.world != null ? data.world : "world";
            case COORDINATE -> (data.triggerArea != null && data.triggerArea.world != null)
                    ? (data.triggerArea.world + "_coords")
                    : "world_coords";
        };
    }

    private void createDirectoryStructure() {
        File spawnPointsDir = new File(plugin.getDataFolder(), "spawnpoints");

        File examplesFile = new File(spawnPointsDir, "examples.txt");
        ResourceUtils.updateFromResource(plugin, "spawnpoints/examples.txt", examplesFile);

        File[] existingFiles = spawnPointsDir.listFiles();
        boolean isEmpty = existingFiles == null ||
                (existingFiles.length == 1 && existingFiles[0].getName().equals("examples.txt"));

        if (isEmpty) {
            File starterDir = new File(spawnPointsDir, "starter");
            if (!starterDir.exists()) {
                starterDir.mkdirs();

                File hubSpawn = new File(starterDir, "hub-spawn.yml");
                File pvpZones = new File(starterDir, "pvp-zones.yml");
                File dungeonExample = new File(starterDir, "dungeon-example.yml");

                ResourceUtils.updateFromResource(plugin, "spawnpoints/starter/hub-spawn.yml", hubSpawn);
                ResourceUtils.updateFromResource(plugin, "spawnpoints/starter/pvp-zones.yml", pvpZones);
                ResourceUtils.updateFromResource(plugin, "spawnpoints/starter/dungeon-example.yml", dungeonExample);

                plugin.getLogger().info("Created starter configuration examples in starter/ folder");
            }
        }

        plugin.getLogger().info("Spawn points directory structure ready");
    }

    private void applyCacheSettings() {
        var cfg = mainConfig.settings;
        var cacheConfig = cfg.safeLocationCache;

        // Cache toggles
        SafeLocationFinder.configureCaching(
                cacheConfig.enabled,
                cacheConfig.expiryTime * 1000L,
                cacheConfig.maxCacheSize,
                cacheConfig.advanced.debugCache
        );

        // Search radius
        SafeLocationFinder.configureSearchRadius(cfg.safeLocationRadius);

        // Global block rules
        SafeLocationFinder.configureGlobalGroundBlacklist(cfg.globalGroundBlacklist);
        SafeLocationFinder.configureGlobalPassableBlacklist(cfg.globalPassableBlacklist);

        // Overworld Y selection (moved under teleport)
        var ysel = cfg.teleport.ySelection;
        SafeLocationFinder.configureOverworldYSelection(
                ysel.mode,
                ysel.first,
                ysel.firstShare
        );

        if (cfg.debugMode) {
            plugin.getLogger().info(
                    "Applied cache/safe-location settings: " +
                            "enabled=" + cacheConfig.enabled +
                            ", expiry=" + cacheConfig.expiryTime + "s" +
                            ", maxSize=" + cacheConfig.maxCacheSize +
                            ", searchRadius=" + cfg.safeLocationRadius +
                            ", ySelection={mode=" + ysel.mode +
                            ", first=" + ysel.first +
                            ", firstShare=" + ysel.firstShare + "}"
            );
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