package uz.alex2276564.smartspawnpoint;

import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import uz.alex2276564.smartspawnpoint.commands.SmartSpawnPointCommands;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.BuiltCommand;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.MultiCommandManager;
import uz.alex2276564.smartspawnpoint.config.SmartSpawnPointConfigManager;
import uz.alex2276564.smartspawnpoint.listener.*;
import uz.alex2276564.smartspawnpoint.manager.SpawnManager;
import uz.alex2276564.smartspawnpoint.party.PartyManager;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;
import uz.alex2276564.smartspawnpoint.utils.UpdateChecker;
import uz.alex2276564.smartspawnpoint.utils.adventure.AdventureMessageManager;
import uz.alex2276564.smartspawnpoint.utils.adventure.LegacyMessageManager;
import uz.alex2276564.smartspawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.smartspawnpoint.utils.backup.BackupManager;
import uz.alex2276564.smartspawnpoint.utils.runner.BukkitRunner;
import uz.alex2276564.smartspawnpoint.utils.runner.Runner;

public final class SmartSpawnPoint extends JavaPlugin {
    @Getter
    private static SmartSpawnPoint instance;

    @Getter
    private Runner runner;

    @Getter
    private SmartSpawnPointConfigManager configManager;

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
            checkDependencies();
            setupConfig();
            setupBackupManager();
            setupManagers();
            registerListeners();
            registerCommands();
            checkUpdates();

            getLogger().info("SmartSpawnPoint has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable SmartSpawnPoint: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void setupRunner() {
        runner = new BukkitRunner(this);
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
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("Hooked into PlaceholderAPI!");
        } else {
            placeholderAPIEnabled = false;
            getLogger().warning("PlaceholderAPI not found! Placeholder conditions will not work.");
        }

        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            getLogger().info("Hooked into WorldGuard!");
        } else {
            worldGuardEnabled = false;
            getLogger().warning("WorldGuard not found! Region-based spawns will not work.");
        }
    }

    private void setupConfig() {
        configManager = new SmartSpawnPointConfigManager(this);
        configManager.reload();
    }

    private void setupBackupManager() {
        backupManager = new BackupManager(this);

        // Check for backup need on startup
        backupManager.checkAndBackupAsync();

        // Schedule periodic checks
        runner.runPeriodicalAsync(() -> backupManager.checkAndBackupAsync(),
                20L * 60 * 60 * 24, // Check daily
                20L * 60 * 60 * 24); // Every 24 hours
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

        if (configManager.getMainConfig().joins.waitForResourcePack) {
            resourcePackListener = new PlayerResourcePackListener(this);
            pm.registerEvents(resourcePackListener, this);
        }
    }

    private void registerCommands() {
        MultiCommandManager multiManager = new MultiCommandManager(this);

        BuiltCommand smartSpawnPointCommand = SmartSpawnPointCommands.createSmartSpawnPointCommand();
        multiManager.registerCommand(smartSpawnPointCommand);
    }

    private void checkUpdates() {
        UpdateChecker updateChecker = new UpdateChecker(this, "alex2276564/SmartSpawnPoint", runner);
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

        if (runner != null) {
            runner.cancelTasks();
        }

        if (resourcePackListener != null) {
            resourcePackListener.cleanup();
        }

        SafeLocationFinder.clearCache();
    }
}