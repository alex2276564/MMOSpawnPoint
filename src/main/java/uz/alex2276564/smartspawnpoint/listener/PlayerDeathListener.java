package uz.alex2276564.smartspawnpoint.listener;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    private final SmartSpawnPoint plugin;

    public PlayerDeathListener(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Record death location for later use in respawn event
        plugin.getSpawnManager().recordDeathLocation(player, player.getLocation());
    }
}