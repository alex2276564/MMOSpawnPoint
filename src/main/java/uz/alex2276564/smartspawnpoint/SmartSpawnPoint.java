package uz.alex2276564.smartspawnpoint;

import uz.alex2276564.smartspawnpoint.config.ConfigManager;
import uz.alex2276564.smartspawnpoint.listener.PlayerDeathListener;
import uz.alex2276564.smartspawnpoint.listener.PlayerRespawnListener;
import uz.alex2276564.smartspawnpoint.manager.SpawnManager;
import uz.alex2276564.smartspawnpoint.runner.BukkitRunner;
import uz.alex2276564.smartspawnpoint.runner.Runner;
import uz.alex2276564.smartspawnpoint.commands.reloadcommand.ReloadCommand;
import uz.alex2276564.smartspawnpoint.util.SafeLocationFinder;
import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartSpawnPoint extends JavaPlugin {
    @Getter
    private static SmartSpawnPoint instance;

    @Getter
    private ConfigManager configManager;

    @Getter
    private SpawnManager spawnManager;

    @Getter
    private Runner runner;

    @Getter
    private boolean worldGuardEnabled;

    @Getter
    private boolean placeholderAPIEnabled;

    @Override
    public void onEnable() {
        instance = this;

        // Setup runner
        setupRunner();

        // Check dependencies
        checkDependencies();

        // Initialize managers
        configManager = new ConfigManager(this);
        spawnManager = new SpawnManager(this);

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Load configuration
        configManager.reload();

        getLogger().info("SmartSpawnPoint has been enabled!");
    }

    private void setupRunner() {
        runner = new BukkitRunner(this);
    }

    private void checkDependencies() {
        // Check PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("Hooked into PlaceholderAPI!");
        } else {
            placeholderAPIEnabled = false;
            getLogger().warning("PlaceholderAPI not found! Placeholder conditions will not work.");
        }

        // Check WorldGuard
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
    }

    private void registerCommands() {
        getCommand("smartspawnpoint").setExecutor(new ReloadCommand());
    }

    @Override
    public void onDisable() {
        if (spawnManager != null) {
            spawnManager.cleanup();
        }

        if (runner != null) {
            runner.cancelTasks();
        }

        // Clear caches
        SafeLocationFinder.clearCache();

        getLogger().info("SmartSpawnPoint has been disabled!");
    }
}