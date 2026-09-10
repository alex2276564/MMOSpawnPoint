package uz.alex2276564.mmospawnpoint;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import uz.alex2276564.mmospawnpoint.commands.MMOSpawnPointCommands;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.BuiltCommand;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.MultiCommandManager;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateContext;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.listener.*;
import uz.alex2276564.mmospawnpoint.manager.SpawnEntry;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.*;
import uz.alex2276564.mmospawnpoint.utils.adventure.AdventureMessageManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.LegacyMessageManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.mmospawnpoint.utils.backup.BackupManager;
import uz.alex2276564.mmospawnpoint.utils.runner.FoliaRunner;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

import java.util.logging.Level;

public final class MMOSpawnPoint extends JavaPlugin {
    @Getter
    private Runner runner;

    @Getter
    private HttpUtils httpUtils;

    @Getter
    private MMOSpawnPointConfigManager configManager;

    @Getter
    private BackupManager backupManager;

    @Getter
    private MessageManager messageManager;

    @Getter
    private UpdateChecker updateChecker;

    @Getter
    private MMOSpawnPointServices services;

    @Getter
    private SpawnManager spawnManager;

    @Getter
    private PartyManager partyManager;

    @Getter
    private boolean worldGuardEnabled;

    @Getter
    private boolean placeholderAPIEnabled;

    @Getter
    private PlayerResourcePackListener resourcePackListener;

    @Getter
    private boolean spawnLocationJoinSupported;

    @Override
    public void onEnable() {
        try {
            setupRunner();
            setupHttpClient();
            setupMessageManager();
            setupConfig();
            PlaceholderUtils.configure(
                    getLogger(),
                    () -> configManager.getMainConfig().settings.debugMode
            );
            WorldGuardUtils.configure(
                    getLogger(),
                    () -> configManager.getMainConfig().settings.debugMode
            );
            SafeLocationFinder.configureLogger(getLogger());
            detectSpawnLocationSupport();
            checkDependencies();
            setupBackupManager();
            setupManagers();
            setupServices();
            setupUpdateChecker();
            registerListeners();
            registerCommands();

            getLogger().info("MMOSpawnPoint has been enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable MMOSpawnPoint", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void setupRunner() {
        runner = new FoliaRunner(this);
        getLogger().info("Initialized " + runner.getPlatformName() + " scheduler support");

        if (runner.isFolia()) {
            getLogger().info("Folia detected - using RegionScheduler and EntityScheduler for optimal performance");
        }
    }

    private void setupHttpClient() {
        this.httpUtils = new HttpUtils();
    }

    private void setupMessageManager() {
        if (isMiniMessageAvailable()) {
            try {
                messageManager = new AdventureMessageManager(runner);
                getLogger().info("Using Adventure MiniMessage for text formatting - full MiniMessage syntax supported");
                return;
            } catch (Exception e) {
                getLogger().warning("Failed to initialize Adventure MiniMessage: " + e.getMessage());
                getLogger().warning("Falling back to Legacy formatting...");
            }
        }

        messageManager = new LegacyMessageManager();
        getLogger().info("Using Legacy ChatColor formatting with MiniMessage-like basic tags");
        getLogger().info("Supported: colors, bold, italic, underlined, strikethrough, obfuscated, reset");
        getLogger().info("Note: Complex features (gradients, hover, click events) are not available on older versions");
    }

    private boolean isMiniMessageAvailable() {
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            return true;
        } catch (ClassNotFoundException e) {
            getLogger().info("MiniMessage library not found - this is normal for Paper versions below 1.18");
            return false;
        }
    }

    private void checkDependencies() {
        boolean wantPapi = configManager.getMainConfig().hooks.usePlaceholderAPI;
        boolean wantWG = configManager.getMainConfig().hooks.useWorldGuard;

        if (wantPapi && getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("Hooked into PlaceholderAPI!");
        } else {
            placeholderAPIEnabled = false;
            if (wantPapi) {
                getLogger().warning("PlaceholderAPI not found! Placeholder conditions will not work.");
            } else {
                getLogger().info("PlaceholderAPI integration disabled by config.");
            }
        }

        if (wantWG && getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("Hooked into WorldGuard!");
        } else {
            worldGuardEnabled = false;
            if (wantWG) {
                getLogger().warning("WorldGuard not found! Region-based spawns will not work.");
            } else {
                getLogger().info("WorldGuard integration disabled by config.");
            }
        }
    }

    private void detectSpawnLocationSupport() {
        String mc = getServer().getMinecraftVersion(); // e.g. "26.1", "1.21.11", "1.16.5"
        int[] parts = parseMinecraftVersion(mc);
        int major = parts[0];
        int minor = parts[1];
        int patch = parts[2];

        // By default we consider spawn-location join support safe
        boolean safe = true;

        // For 1.21.9+ (and any newer minor/major) disable PlayerSpawnLocationEvent usage
        if (major > 1) {
            safe = false;
        } else if (major == 1) {
            if (minor > 21) {
                safe = false;
            } else if (minor == 21 && patch >= 9) {
                safe = false;
            }
        }

        this.spawnLocationJoinSupported = safe;

        if (!safe) {
            getLogger().warning("Detected Minecraft " + mc
                    + " (>= 1.21.9). PlayerSpawnLocationEvent is deprecated/unstable on this version.");
            getLogger().warning("Join spawns will be handled via PlayerJoinEvent teleport flow "
                    + "even if settings.teleport.useSetSpawnLocationForJoin is set to true.");
        } else {
            getLogger().info("Minecraft " + mc
                    + " detected: PlayerSpawnLocationEvent join support is enabled.");
        }
    }

    /**
     * Parse Minecraft version string like "1.21.11" into [major, minor, patch].
     * Non-numeric/missing parts are treated as 0.
     */
    private static int[] parseMinecraftVersion(String version) {
        int major = 0;
        int minor = 0;
        int patch = 0;
        if (version == null || version.isEmpty()) {
            return new int[]{major, minor, patch};
        }

        // Some servers may report versions like "1.21.11-SNAPSHOT"
        String core = version.split("-")[0];
        String[] parts = core.split("\\.");

        try {
            if (parts.length > 0) major = Integer.parseInt(parts[0]);
            if (parts.length > 1) minor = Integer.parseInt(parts[1]);
            if (parts.length > 2) patch = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {
            // Fallback: keep zeros
        }

        return new int[]{major, minor, patch};
    }

    private void setupConfig() {
        configManager = new MMOSpawnPointConfigManager(
                getDataFolder(),
                getLogger(),
                messageManager,
                getClassLoader(),
                getServer().getPluginManager()
        );
        configManager.reload();
    }

    private void setupBackupManager() {
        backupManager = new BackupManager(runner, getLogger(), getDataFolder().toPath());

        // Check for backup need on startup
        backupManager.checkAndBackupAsync();

        // Schedule periodic checks - daily (24 hours)
        long dailySeconds = 24L * 60L * 60L;
        long dailyTicks = Runner.secondsToTicks(dailySeconds);
        runner.runAsyncTimer(() -> backupManager.checkAndBackupAsync(), dailyTicks, dailyTicks);
    }

    private void setupManagers() {
        spawnManager = new SpawnManager(
                configManager,
                runner,
                messageManager,
                getLogger(),
                placeholderAPIEnabled,
                spawnLocationJoinSupported
        );

        if (configManager.getMainConfig().party.enabled) {
            partyManager = new PartyManager(
                    configManager,
                    runner,
                    messageManager,
                    getLogger(),
                    worldGuardEnabled
            );
            spawnManager.setPartyManager(partyManager);
            getLogger().info("Party system enabled with scope: " + configManager.getMainConfig().party.scope);
        }
    }

    private void setupServices() {
        this.services = new MMOSpawnPointServices(
                runner,
                configManager,
                messageManager,
                getLogger(),
                spawnManager,
                partyManager // may be null if party is disabled
        );
    }

    private void setupUpdateChecker() {
        this.updateChecker = new UpdateChecker(
                getDescription().getName(),
                getDescription().getVersion(),
                "alex2276564/MMOSpawnPoint",
                runner,
                httpUtils,
                getLogger()
        );

        updateChecker.checkForUpdates();
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        var cfg = configManager;
        var logger = getLogger();

        pm.registerEvents(new PlayerDeathListener(spawnManager, cfg, logger), this);
        pm.registerEvents(new PlayerRespawnListener(cfg, spawnManager, partyManager, runner, logger), this);
        pm.registerEvents(new PlayerWorldChangeListener(cfg, logger), this);

        if (cfg.getMainConfig().join.waitForResourcePack) {
            resourcePackListener = new PlayerResourcePackListener(cfg, spawnManager, runner, messageManager, logger);
            pm.registerEvents(resourcePackListener, this);
        }

        pm.registerEvents(new PlayerQuitListener(cfg, spawnManager, partyManager, resourcePackListener, logger), this);

        pm.registerEvents(new PlayerJoinListener(
                cfg,
                spawnManager,
                runner,
                messageManager,
                logger,
                resourcePackListener,
                spawnLocationJoinSupported
        ), this);

        if (spawnLocationJoinSupported) {
            pm.registerEvents(new PlayerSpawnLocationListener(cfg, spawnManager, runner, messageManager, logger), this);
        }
    }

    private void registerCommands() {
        MultiCommandManager multiManager = new MultiCommandManager(this, services);

        BuiltCommand mmoSpawnPointCommand = MMOSpawnPointCommands.createMMOSpawnPointCommand(services);
        multiManager.registerCommand(mmoSpawnPointCommand);
    }

    @Override
    public void onDisable() {
        if (partyManager != null) {
            partyManager.shutdown();
        }

        if (spawnManager != null) {
            spawnManager.cleanup();
        }

        if (resourcePackListener != null) {
            resourcePackListener.cleanup();
        }

        SafeLocationFinder.cleanup();
        SafeLocationFinder.clearCache();
        SpawnEntry.clearPatternCache();
        SimulateContext.clearPREV();

        if (runner != null) {
            runner.cancelAllTasks();
        }
    }
}