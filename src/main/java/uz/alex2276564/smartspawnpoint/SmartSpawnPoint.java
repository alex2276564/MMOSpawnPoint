package uz.alex2276564.smartspawnpoint;

import uz.alex2276564.smartspawnpoint.commands.SmartSpawnPointCommands;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.BuiltCommand;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.MultiCommandManager;
import uz.alex2276564.smartspawnpoint.config.ConfigManager;
import uz.alex2276564.smartspawnpoint.listener.PlayerDeathListener;
import uz.alex2276564.smartspawnpoint.listener.PlayerJoinListener;
import uz.alex2276564.smartspawnpoint.listener.PlayerQuitListener;
import uz.alex2276564.smartspawnpoint.listener.PlayerRespawnListener;
import uz.alex2276564.smartspawnpoint.manager.SpawnManager;
import uz.alex2276564.smartspawnpoint.party.PartyManager;
import uz.alex2276564.smartspawnpoint.utils.adventure.AdventureMessageManager;
import uz.alex2276564.smartspawnpoint.utils.adventure.LegacyMessageManager;
import uz.alex2276564.smartspawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.smartspawnpoint.utils.runner.BukkitRunner;
import uz.alex2276564.smartspawnpoint.utils.runner.Runner;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;
import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import uz.alex2276564.smartspawnpoint.utils.UpdateChecker;

public final class SmartSpawnPoint extends JavaPlugin {
    @Getter
    public static SmartSpawnPoint instance;

    @Getter
    private ConfigManager configManager;

    @Getter
    private SpawnManager spawnManager;

    @Getter
    private PartyManager partyManager;

    @Getter
    private Runner runner;

    @Getter
    private MessageManager messageManager;

    @Getter
    private boolean worldGuardEnabled;

    @Getter
    private boolean placeholderAPIEnabled;

    @Override
    public void onEnable() {
        instance = this;

        try {
            setupRunner();
            setupMessageManager();
            checkDependencies();
            configManager = new ConfigManager(this);
            spawnManager = new SpawnManager(this);
            configManager.reload();

            if (configManager.isPartyEnabled()) {
                partyManager = new PartyManager(this);
                spawnManager.setPartyManager(partyManager);
                getLogger().info("Party system enabled");
            }

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

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerDeathListener(this), this);
        pm.registerEvents(new PlayerRespawnListener(this), this);
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
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

        SafeLocationFinder.clearCache();

        getLogger().info("SmartSpawnPoint has been disabled!");
    }
}