package uz.alex2276564.smartspawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;

public class PlayerJoinListener implements Listener {
    private final SmartSpawnPoint plugin;

    public PlayerJoinListener(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player is dead when joining (this can happen after a server restart)
        if (player.isDead()) {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Player " + player.getName() + " joined while dead, handling respawn");
            }

            // Instead of teleporting directly, just record the death location
            // The actual teleport will happen in PlayerRespawnListener when the player respawns
            plugin.getSpawnManager().recordDeathLocation(player, player.getLocation());

            // We don't need to do anything else - the normal respawn event will handle it
        }
    }
}
