package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

public class PlayerSpawnLocationListener implements Listener {

    private final MMOSpawnPoint plugin;

    public PlayerSpawnLocationListener(MMOSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        try {
            var mainConfig = plugin.getConfigManager().getMainConfig();

            // Disabled in config -> keep old join-teleport flow
            if (!mainConfig.settings.teleport.useSetSpawnLocationForJoin) {
                return;
            }

            // Resource-pack waiting uses PlayerJoinEvent + teleport flow by design
            if (mainConfig.join.waitForResourcePack) {
                if (mainConfig.settings.debugMode) {
                    plugin.getLogger().info(
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
            Location resolved = plugin.getSpawnManager()
                    .resolveJoinSpawnLocationForSpawnEvent(player, baseSpawn);

            if (resolved == null) {
                // No MSP override -> keep vanilla spawnLocation
                plugin.getRunner().runAtEntityLater(player, () -> {
                    if (!player.isOnline()) return;

                    String msg = plugin.getConfigManager().getMessagesConfig().general.noSpawnFound;
                    plugin.getMessageManager().sendMessageKeyed(player, "general.noSpawnFound", msg);
                }, 1L);

                return;
            }

            event.setSpawnLocation(resolved);

        } catch (Exception e) {
            plugin.getLogger().severe("Error in PlayerSpawnLocationListener: " + e.getMessage());
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                e.printStackTrace();
            }
        }
    }
}