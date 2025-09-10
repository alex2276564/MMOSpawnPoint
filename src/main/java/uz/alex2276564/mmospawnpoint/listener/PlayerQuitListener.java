package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

public class PlayerQuitListener implements Listener {
    private final MMOSpawnPoint plugin;

    public PlayerQuitListener(MMOSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Cleaning up data for disconnecting player: " + player.getName());
        }

        // Clean up spawn manager data
        plugin.getSpawnManager().cleanupPlayerData(player.getUniqueId());

        // Clean up party data if party system is enabled
        if (plugin.getConfigManager().getMainConfig().party.enabled && plugin.getPartyManager() != null) {
            plugin.getPartyManager().cleanupPlayerData(player.getUniqueId());
        }

        // Clean up resource pack listener data
        PlayerResourcePackListener resourcePackListener = plugin.getResourcePackListener();
        if (resourcePackListener != null) {
            resourcePackListener.cleanupPlayer(player.getUniqueId());
        }

        // Clean up safe location cache for this player
        if (plugin.getConfigManager().getMainConfig().settings.safeLocationCache.advanced.clearPlayerCacheOnQuit) {
            SafeLocationFinder.clearPlayerCache(player.getUniqueId());
        }
    }
}