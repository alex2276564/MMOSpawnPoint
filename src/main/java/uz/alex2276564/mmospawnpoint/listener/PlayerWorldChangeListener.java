package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

public class PlayerWorldChangeListener implements Listener {
    private final MMOSpawnPoint plugin;

    public PlayerWorldChangeListener(MMOSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        var adv = plugin.getConfigManager().getMainConfig().settings.safeLocationCache.advanced;

        // Clear entire cache on any world change (if enabled)
        if (adv.clearOnWorldChange) {
            SafeLocationFinder.clearCache();
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("[SafeLocationFinder] Cleared ENTIRE cache due to world change by " + event.getPlayer().getName());
            }
        }

        // Clear specific player's cache on world change (if enabled)
        if (adv.clearPlayerCacheOnWorldChange) {
            SafeLocationFinder.clearPlayerCache(event.getPlayer().getUniqueId());
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("[SafeLocationFinder] Cleared PLAYER cache for " + event.getPlayer().getName() + " due to world change");
            }
        }
    }
}