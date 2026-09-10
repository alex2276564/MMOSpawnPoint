package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

import java.util.logging.Logger;

public class PlayerWorldChangeListener implements Listener {

    private final MMOSpawnPointConfigManager configManager;
    private final Logger logger;

    public PlayerWorldChangeListener(MMOSpawnPointConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        var adv = configManager.getMainConfig().settings.safeLocationCache.advanced;

        // Clear entire cache on any world change (if enabled)
        if (adv.clearOnWorldChange) {
            SafeLocationFinder.clearCache();
            if (configManager.getMainConfig().settings.debugMode) {
                logger.info("[SafeLocationFinder] Cleared ENTIRE cache due to world change by " + event.getPlayer().getName());
            }
        }

        // Clear specific player's cache on world change (if enabled)
        if (adv.clearPlayerCacheOnWorldChange) {
            SafeLocationFinder.clearPlayerCache(event.getPlayer().getUniqueId());
            if (configManager.getMainConfig().settings.debugMode) {
                logger.info("[SafeLocationFinder] Cleared PLAYER cache for " + event.getPlayer().getName() + " due to world change");
            }
        }
    }
}