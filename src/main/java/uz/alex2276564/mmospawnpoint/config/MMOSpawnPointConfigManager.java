package uz.alex2276564.mmospawnpoint.config;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;
import org.yaml.snakeyaml.Yaml;
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
import uz.alex2276564.mmospawnpoint.utils.adventure.MessageManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MMOSpawnPointConfigManager {

    private final File dataFolder;
    private final Logger logger;
    private final MessageManager messageManager;
    private final ClassLoader resourceLoader;
    private final PluginManager pluginManager;

    @Getter
    private MainConfig mainConfig;

    @Getter
    private MessagesConfig messagesConfig;

    @Getter
    private List<SpawnEntry> allSpawnEntries;

    public MMOSpawnPointConfigManager(File dataFolder,
                                      Logger logger,
                                      MessageManager messageManager,
                                      ClassLoader resourceLoader,
                                      PluginManager pluginManager) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.messageManager = messageManager;
        this.resourceLoader = resourceLoader;
        this.pluginManager = pluginManager;
        this.allSpawnEntries = new ArrayList<>();
    }

    public void reload() {
        try {
            loadMainConfig();
            loadMessagesConfig();
            loadSpawnPointConfigs();

            // Apply runtime settings
            applyCacheSettings();

            logger.info("Configuration system reloaded successfully!");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to reload configuration", e);
        }
    }

    private void loadMainConfig() {
        mainConfig = ConfigManager.create(MainConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(new File(dataFolder, "config.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        MainConfigValidator.validate(mainConfig);
        logger.info("Main configuration loaded and validated successfully");
    }

    private void loadMessagesConfig() {
        messagesConfig = ConfigManager.create(MessagesConfig.class, it -> {
            it.withConfigurer(new YamlSnakeYamlConfigurer());
            it.withBindFile(new File(dataFolder, "messages.yml"));
            it.withRemoveOrphans(true);
            it.saveDefaults();
            it.load(true);
        });

        MessagesConfigValidator.validate(messagesConfig);
        applyPartyPrefixToken(messagesConfig);
        messageManager.configureDisabledKeysProvider(() -> getMessagesConfig().disabledKeys);
        logger.info("Messages configuration loaded and validated successfully");
    }

    private void applyPartyPrefixToken(MessagesConfig cfg) {
        String px = (cfg.party != null && cfg.party.prefix != null) ? cfg.party.prefix : "";
        Function<String, String> inject = s -> (s == null) ? null : s.replace("<prefix>", px);

        var p = cfg.party;
        if (p == null) return;

        // List of fields where we want to insert the prefix
        p.inviteSent = inject.apply(p.inviteSent);
        p.inviteReceived = inject.apply(p.inviteReceived);
        p.invitationDeclined = inject.apply(p.invitationDeclined);
        p.invitationDeclinedToLeader = inject.apply(p.invitationDeclinedToLeader);
        p.invitationExpiredOrInvalid = inject.apply(p.invitationExpiredOrInvalid);
        p.inviteFailedPartyFull = inject.apply(p.inviteFailedPartyFull);
        p.inviteFailedAlreadyInParty = inject.apply(p.inviteFailedAlreadyInParty);
        p.noInvitations = inject.apply(p.noInvitations);
        p.inviteExpired = inject.apply(p.inviteExpired);

        p.joinedParty = inject.apply(p.joinedParty);
        p.playerJoinedParty = inject.apply(p.playerJoinedParty);
        p.leftParty = inject.apply(p.leftParty);
        p.playerLeftParty = inject.apply(p.playerLeftParty);
        p.playerRemoved = inject.apply(p.playerRemoved);
        p.playerRemovedFromParty = inject.apply(p.playerRemovedFromParty);
        p.cannotRemoveSelf = inject.apply(p.cannotRemoveSelf);
        p.partyDisbanded = inject.apply(p.partyDisbanded);

        p.onlyPlayers = inject.apply(p.onlyPlayers);
        p.systemDisabled = inject.apply(p.systemDisabled);
        p.notInParty = inject.apply(p.notInParty);
        p.notLeader = inject.apply(p.notLeader);
        p.playerNotInYourParty = inject.apply(p.playerNotInYourParty);
        p.invalidRespawnMode = inject.apply(p.invalidRespawnMode);
        p.errorOccurred = inject.apply(p.errorOccurred);
        p.respawnedAtMember = inject.apply(p.respawnedAtMember);
        p.respawnModeChanged = inject.apply(p.respawnModeChanged);
        p.respawnTargetSet = inject.apply(p.respawnTargetSet);
        p.respawnDisabledRegion = inject.apply(p.respawnDisabledRegion);
        p.respawnDisabledWorld = inject.apply(p.respawnDisabledWorld);
        p.respawnCooldown = inject.apply(p.respawnCooldown);
        p.walkingSpawnPointMessage = inject.apply(p.walkingSpawnPointMessage);
        p.walkingSpawnPointRestricted = inject.apply(p.walkingSpawnPointRestricted);
        p.alreadyLeader = inject.apply(p.alreadyLeader);
        p.newLeaderAssigned = inject.apply(p.newLeaderAssigned);
        p.respawnTooFar = inject.apply(p.respawnTooFar);

        p.listHeader = inject.apply(p.listHeader);
        p.listLeader = inject.apply(p.listLeader);
        p.listLeaderMissing = inject.apply(p.listLeaderMissing);
        p.listMember = inject.apply(p.listMember);
        p.listAnchor = inject.apply(p.listAnchor);
        p.listAnchorMissing = inject.apply(p.listAnchorMissing);
        p.listSettingsHeader = inject.apply(p.listSettingsHeader);
        p.listRespawnMode = inject.apply(p.listRespawnMode);
        p.listNoAnchor = inject.apply(p.listNoAnchor);
        p.listSeparator = inject.apply(p.listSeparator);
    }

    private void loadSpawnPointConfigs() {
        allSpawnEntries.clear();

        SpawnEntry.clearPatternCache();

        File spawnPointsDir = new File(dataFolder, "spawnpoints");

        if (!spawnPointsDir.exists()) {
            spawnPointsDir.mkdirs();
            createDirectoryStructure(spawnPointsDir);
        } else {
            File examplesFile = new File(spawnPointsDir, "examples.txt");
            ResourceUtils.updateFromResource(resourceLoader, logger, "spawnpoints/examples.txt", examplesFile);
        }

        loadAllSpawnConfigsRecursively(spawnPointsDir, 0);

        // Sort by priority (descending)
        allSpawnEntries.sort((a, b) -> Integer.compare(b.calculatedPriority(), a.calculatedPriority()));

        logger.info("Loaded " + allSpawnEntries.size() + " spawn configuration entries");

        if (mainConfig.settings.debugMode) {
            logSpawnPriorities();
        }
    }

    private void loadAllSpawnConfigsRecursively(File directory, int depth) {
        int maxDepth = mainConfig.settings.maintenance.maxFolderDepth;
        if (depth > maxDepth) {
            logger.warning("Maximum directory depth (" + maxDepth + ") exceeded for: " + directory.getPath());
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadAllSpawnConfigsRecursively(file, depth + 1);
            } else if (file.getName().endsWith(".yml")) {
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
                    logger.warning("Skipping non-YAML or empty file: " + file.getName());
                    return;
                }
                if (!map.containsKey("spawns")) {
                    logger.warning("No 'spawns' key found in " + file.getName() + " — skipping");
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

            boolean wgConfigured = mainConfig.hooks.useWorldGuard;
            boolean wgPresent = pluginManager.getPlugin("WorldGuard") != null;
            boolean papiConfigured = mainConfig.hooks.usePlaceholderAPI;
            boolean papiPresent = pluginManager.getPlugin("PlaceholderAPI") != null;

            SpawnPointsConfigValidator.validate(
                    config,
                    file.getName(),
                    wgConfigured,
                    wgPresent,
                    papiConfigured,
                    papiPresent
            );

            int added = 0;
            for (SpawnPointsConfig.SpawnPointEntry entry : config.spawns) {
                SpawnEntry.Type type = switch (entry.kind.toLowerCase()) {
                    case "region" -> SpawnEntry.Type.REGION;
                    case "world" -> SpawnEntry.Type.WORLD;
                    case "coordinate" -> SpawnEntry.Type.COORDINATE;
                    default -> null;
                };
                if (type == null) {
                    logger.warning("Unknown kind in " + file.getName() + " — skipping entry.");
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

            logger.info("Loaded spawn config: " + file.getName() + " (entries: " + added + ")");
        } catch (Exception e) {
            logger.warning("Failed to load spawn config " + file.getName() + ": " + e.getMessage());
            if (mainConfig.settings.debugMode) {
                logger.log(
                        Level.WARNING,
                        "Detailed exception while loading spawn config " + file.getName(),
                        e
                );
            }
        }
    }

    private void logSpawnPriorities() {
        logger.info("=== Spawn Priority Order ===");
        for (SpawnEntry entry : allSpawnEntries) {
            String spawnName = getSpawnName(entry);
            logger.info(String.format(
                    "Priority %d: %s '%s' (%s) from %s",
                    entry.calculatedPriority(),
                    entry.type().name().toLowerCase(),
                    spawnName,
                    entry.event(),
                    entry.fileName()
            ));
        }
        logger.info("============================");
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

    private void createDirectoryStructure(File spawnPointsDir) {
        File examplesFile = new File(spawnPointsDir, "examples.txt");
        ResourceUtils.updateFromResource(resourceLoader, logger, "spawnpoints/examples.txt", examplesFile);

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

                ResourceUtils.updateFromResource(resourceLoader, logger, "spawnpoints/starter/hub-spawn.yml", hubSpawn);
                ResourceUtils.updateFromResource(resourceLoader, logger, "spawnpoints/starter/pvp-zones.yml", pvpZones);
                ResourceUtils.updateFromResource(resourceLoader, logger, "spawnpoints/starter/dungeon-example.yml", dungeonExample);

                logger.info("Created starter configuration examples in starter/ folder");
            }
        }

        logger.info("Spawn points directory structure ready");
    }

    private void applyCacheSettings() {
        var cfg = mainConfig.settings;
        var cacheConfig = cfg.safeLocationCache;

        SafeLocationFinder.clearCache();

        // Cache toggles
        SafeLocationFinder.configureCaching(
                cacheConfig.enabled,
                cacheConfig.expiryTime * 1000L,
                cacheConfig.maxCacheSize,
                cacheConfig.advanced.debugCache
        );

        // Global block rules
        SafeLocationFinder.configureGlobalGroundBlacklist(cfg.globalGroundBlacklist);
        SafeLocationFinder.configureGlobalPassableBlacklist(cfg.globalPassableBlacklist);

        // Dimension-aware Y selection
        var ys = cfg.teleport.ySelection;
        // Overworld
        SafeLocationFinder.configureOverworldYSelection(
                ys.overworld.mode,
                ys.overworld.first,
                ys.overworld.firstShare
        );
        // End
        SafeLocationFinder.configureEndYSelection(
                ys.end.mode,
                ys.end.first,
                ys.end.firstShare
        );
        // Nether
        SafeLocationFinder.configureNetherYSelection(
                ys.nether.mode,
                ys.nether.respectRange
        );
        // Custom
        SafeLocationFinder.configureCustomYSelection(
                ys.custom.mode,
                ys.custom.first,
                ys.custom.firstShare
        );

        if (cfg.debugMode) {
            logger.info(
                    "Applied cache/safe-location settings: enabled=" + cacheConfig.enabled +
                            ", expiry=" + cacheConfig.expiryTime + "s, maxSize=" + cacheConfig.maxCacheSize +
                            ", ySelection={overworld=" + ys.overworld.mode + "/" + ys.overworld.first + "/" + ys.overworld.firstShare +
                            ", nether=" + ys.nether.mode + "/respectRange=" + ys.nether.respectRange +
                            ", end=" + ys.end.mode + "/" + ys.end.first + "/" + ys.end.firstShare +
                            ", custom=" + ys.custom.mode + "/" + ys.custom.first + "/" + ys.custom.firstShare + "}"
            );
        }
    }

    public List<SpawnEntry> getSpawnEntriesForEvent(String eventType) {
        return allSpawnEntries.stream()
                .filter(entry -> entry.isForEventType(eventType))
                .toList();
    }

    public List<SpawnEntry> getMatchingSpawnEntries(String eventType, Location location) {
        return allSpawnEntries.stream()
                .filter(entry -> entry.isForEventType(eventType))
                .filter(entry -> entry.matchesLocation(location))
                .toList();
    }
}