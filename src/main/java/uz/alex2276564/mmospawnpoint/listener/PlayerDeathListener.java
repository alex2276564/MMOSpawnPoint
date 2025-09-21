package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

public class PlayerDeathListener implements Listener {
    private final MMOSpawnPoint plugin;

    public PlayerDeathListener(MMOSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            Player player = event.getEntity();
            // Record death location for later use in respawn event
            plugin.getSpawnManager().recordDeathLocation(player, player.getLocation());

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Recorded death location for " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling player death for " + event.getEntity().getName() + ": " + e.getMessage());
        }
    }
}
