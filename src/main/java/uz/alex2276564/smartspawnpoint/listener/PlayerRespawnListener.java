package uz.alex2276564.smartspawnpoint.listener;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {
    private final SmartSpawnPoint plugin;

    public PlayerRespawnListener(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location respawnLocation = plugin.getSpawnManager().getSpawnLocation(player);

        if (respawnLocation != null) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Setting respawn location for " + player.getName() + " to: " +
                        respawnLocation.getWorld().getName() + " " +
                        respawnLocation.getX() + "," +
                        respawnLocation.getY() + "," +
                        respawnLocation.getZ());
            }

            // Set the respawn location
            event.setRespawnLocation(respawnLocation);

            // For safety, teleport the player after the next tick
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.teleport(respawnLocation);
                }
            });
        } else if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().warning("No respawn location found for " + player.getName() + ", using server default");
        }
    }
}