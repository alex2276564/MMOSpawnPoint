package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;

import java.util.logging.Logger;

public class PlayerDeathListener implements Listener {

    private final SpawnManager spawnManager;
    private final MMOSpawnPointConfigManager configManager;
    private final Logger logger;

    public PlayerDeathListener(SpawnManager spawnManager,
                               MMOSpawnPointConfigManager configManager,
                               Logger logger) {
        this.spawnManager = spawnManager;
        this.configManager = configManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            Player player = event.getEntity();
            // Record death location for later use in respawn event
            spawnManager.recordDeathLocation(player, player.getLocation());

            if (configManager.getMainConfig().settings.debugMode) {
                logger.info("Recorded death location for " + player.getName());
            }
        } catch (Exception e) {
            logger.warning("Error handling player death for " + event.getEntity().getName() + ": " + e.getMessage());
        }
    }
}
