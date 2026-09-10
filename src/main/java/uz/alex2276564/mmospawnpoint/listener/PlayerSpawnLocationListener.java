package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerSpawnLocationListener implements Listener {

    private final MMOSpawnPointConfigManager configManager;
    private final SpawnManager spawnManager;
    private final Runner runner;
    private final MessageManager messageManager;
    private final Logger logger;

    public PlayerSpawnLocationListener(MMOSpawnPointConfigManager configManager,
                                       SpawnManager spawnManager,
                                       Runner runner,
                                       MessageManager messageManager,
                                       Logger logger) {
        this.configManager = configManager;
        this.spawnManager = spawnManager;
        this.runner = runner;
        this.messageManager = messageManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        try {
            var mainConfig = configManager.getMainConfig();

            // Disabled in config -> keep old join-teleport flow
            if (!mainConfig.settings.teleport.useSetSpawnLocationForJoin) {
                return;
            }

            // Resource-pack waiting uses PlayerJoinEvent + teleport flow by design
            if (mainConfig.join.waitForResourcePack) {
                if (mainConfig.settings.debugMode) {
                    logger.info(
                            "Skipping PlayerSpawnLocationEvent handling for " + event.getPlayer().getName()
                                    + " because join.waitForResourcePack is enabled"
                    );
                }
                return;
            }

            Player player = event.getPlayer();
            Location baseSpawn = event.getSpawnLocation();
            if (baseSpawn.getWorld() == null) {
                return;
            }

            // Resolve MSP/party join spawn location for spawn-location event
            Location resolved = spawnManager.resolveJoinSpawnLocationForSpawnEvent(player, baseSpawn);

            if (resolved == null) {
                // No MSP override -> keep vanilla spawnLocation
                runner.runAtEntityLater(player, () -> {
                    if (!player.isOnline()) return;

                    String msg = configManager.getMessagesConfig().general.noSpawnFound;
                    messageManager.sendMessageKeyed(player, "general.noSpawnFound", msg);
                }, 1L);

                return;
            }

            event.setSpawnLocation(resolved);

        } catch (Exception e) {
            logger.severe("Error in PlayerSpawnLocationListener: " + e.getMessage());
            if (configManager.getMainConfig().settings.debugMode) {
                logger.log(
                        Level.SEVERE,
                        "Detailed exception in PlayerSpawnLocationListener",
                        e
                );
            }
        }
    }
}