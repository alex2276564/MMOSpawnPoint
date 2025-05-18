package uz.alex2276564.smartspawnpoint;

import uz.alex2276564.smartspawnpoint.config.ConfigManager;
import uz.alex2276564.smartspawnpoint.listener.PlayerDeathListener;
import uz.alex2276564.smartspawnpoint.listener.PlayerJoinListener;
import uz.alex2276564.smartspawnpoint.listener.PlayerRespawnListener;
import uz.alex2276564.smartspawnpoint.manager.SpawnManager;
import uz.alex2276564.smartspawnpoint.party.PartyManager;
import uz.alex2276564.smartspawnpoint.runner.BukkitRunner;
import uz.alex2276564.smartspawnpoint.runner.Runner;
import uz.alex2276564.smartspawnpoint.commands.MainCommandExecutor;
import uz.alex2276564.smartspawnpoint.util.SafeLocationFinder;
import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import uz.alex2276564.smartspawnpoint.util.UpdateChecker;

public final class SmartSpawnPoint extends JavaPlugin {
    @Getter
    private static SmartSpawnPoint instance;

    @Getter
    private ConfigManager configManager;

    @Getter
    private SpawnManager spawnManager;

    @Getter
    private PartyManager partyManager;

    @Getter
    private Runner runner;

    @Getter
    private boolean worldGuardEnabled;

    @Getter
    private boolean placeholderAPIEnabled;

    @Override
    public void onEnable() {
        instance = this;

        setupRunner();

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

        getLogger().info("SmartSpawnPoint has been enabled!");
    }

    private void setupRunner() {
        runner = new BukkitRunner(this);
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
    }

    private void registerCommands() {
        // Register main command executor that handles all subcommands
        getCommand("smartspawnpoint").setExecutor(new MainCommandExecutor(this));
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