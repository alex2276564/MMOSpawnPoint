package uz.alex2276564.smartspawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;

public class PlayerQuitListener implements Listener {
    private final SmartSpawnPoint plugin;

    public PlayerQuitListener(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("Cleaning up data for disconnecting player: " + player.getName());
        }

        // Clean up spawn manager data
        plugin.getSpawnManager().cleanupPlayerData(player.getUniqueId());

        // Clean up party data if party system is enabled
        if (plugin.getConfigManager().isPartyEnabled() && plugin.getPartyManager() != null) {
            plugin.getPartyManager().cleanupPlayerData(player.getUniqueId());
        }

        // Clean up safe location cache for this player
        if (plugin.getConfigManager().isSafeCacheClearPlayerOnWorldChange()) {
            SafeLocationFinder.clearPlayerCache(player.getUniqueId());
        }
    }
}
