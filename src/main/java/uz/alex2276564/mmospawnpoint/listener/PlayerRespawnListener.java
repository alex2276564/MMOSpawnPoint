package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

public class PlayerRespawnListener implements Listener {
    private final MMOSpawnPoint plugin;

    public PlayerRespawnListener(MMOSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        try {
            Player player = event.getPlayer();

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Processing respawn for " + player.getName());
            }

            boolean success = plugin.getSpawnManager().processDeathSpawn(player);

            if (!success) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().warning("No death spawn location found for " + player.getName() + ", using server default");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player respawn for " + event.getPlayer().getName() + ": " + e.getMessage());
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                e.printStackTrace();
            }
        }
    }
}
