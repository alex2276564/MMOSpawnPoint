package uz.alex2276564.mmospawnpoint;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import uz.alex2276564.mmospawnpoint.commands.MMOSpawnPointCommands;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.BuiltCommand;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.MultiCommandManager;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.listener.*;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.UpdateChecker;
import uz.alex2276564.mmospawnpoint.utils.adventure.AdventureMessageManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.LegacyMessageManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.mmospawnpoint.utils.backup.BackupManager;
import uz.alex2276564.mmospawnpoint.utils.runner.FoliaRunner;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

public final class MMOSpawnPoint extends JavaPlugin {
    @Getter
    private static MMOSpawnPoint instance;

    @Getter
    private Runner runner;

    @Getter
    private MMOSpawnPointConfigManager configManager;

    @Getter
    private BackupManager backupManager;

    @Getter
    private MessageManager messageManager;

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

    @Override
    public void onEnable() {
        instance = this;

        try {
            setupRunner();
            setupMessageManager();
            setupConfig();
            checkDependencies();
            setupBackupManager();
            setupManagers();
            registerListeners();
            registerCommands();
            checkUpdates();

            getLogger().info("MMOSpawnPoint has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable MMOSpawnPoint: " + e.getMessage());
            e.printStackTrace();
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

    private void setupMessageManager() {
        if (isMiniMessageAvailable()) {
            try {
                messageManager = new AdventureMessageManager();
                getLogger().info("Using Adventure MiniMessage for text formatting - full MiniMessage syntax supported");
                return;
            } catch (Exception e) {
                getLogger().warning("Failed to initialize Adventure MiniMessage: " + e.getMessage());
                getLogger().warning("Falling back to Legacy formatting...");
            }
        }

        messageManager = new LegacyMessageManager();
        getLogger().info("Using Legacy ChatColor formatting with MiniMessage syntax compatibility");
        getLogger().info("You can continue using MiniMessage syntax in your config - basic tags will be converted automatically");
        getLogger().info("Supported: colors, bold, italic, underlined, strikethrough, obfuscated, reset");
        getLogger().warning("Note: Legacy mode uses regex processing which may have slight performance overhead");
        getLogger().info("Note: Complex features (gradients, hover, click events) are not available on older server versions");
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

    private void setupConfig() {
        configManager = new MMOSpawnPointConfigManager(this);
        configManager.reload();
    }

    private void setupBackupManager() {
        backupManager = new BackupManager(this);

        // Check for backup need on startup
        backupManager.checkAndBackupAsync();

        // Schedule periodic checks - daily (24 hours)
        long dailyTicks = Runner.secondsToTicks(24 * 60 * 60);
        runner.runAsyncTimer(() -> backupManager.checkAndBackupAsync(), dailyTicks, dailyTicks);
    }

    private void setupManagers() {
        spawnManager = new SpawnManager(this);

        if (configManager.getMainConfig().party.enabled) {
            partyManager = new PartyManager(this);
            spawnManager.setPartyManager(partyManager);
            getLogger().info("Party system enabled with scope: " + configManager.getMainConfig().party.scope);
        }
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new PlayerRespawnListener(this), this);
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new PlayerWorldChangeListener(this), this);

        if (configManager.getMainConfig().join.waitForResourcePack) {
            resourcePackListener = new PlayerResourcePackListener(this);
            pm.registerEvents(resourcePackListener, this);
        }
    }

    private void registerCommands() {
        MultiCommandManager multiManager = new MultiCommandManager(this);

        BuiltCommand mmoSpawnPointCommand = MMOSpawnPointCommands.createMMOSpawnPointCommand();
        multiManager.registerCommand(mmoSpawnPointCommand);
    }

    private void checkUpdates() {
        UpdateChecker updateChecker = new UpdateChecker(this, "alex2276564/MMOSpawnPoint", runner);
        updateChecker.checkForUpdates();
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

        if (runner != null) {
            runner.cancelAllTasks();
        }
    }
}