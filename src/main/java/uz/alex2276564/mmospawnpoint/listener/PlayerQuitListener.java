package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

import java.util.logging.Logger;

public class PlayerQuitListener implements Listener {

    private final MMOSpawnPointConfigManager configManager;
    private final SpawnManager spawnManager;
    private final PartyManager partyManager;
    private final PlayerResourcePackListener resourcePackListener;
    private final Logger logger;

    public PlayerQuitListener(MMOSpawnPointConfigManager configManager,
                              SpawnManager spawnManager,
                              PartyManager partyManager,
                              PlayerResourcePackListener resourcePackListener,
                              Logger logger) {
        this.configManager = configManager;
        this.spawnManager = spawnManager;
        this.partyManager = partyManager;
        this.resourcePackListener = resourcePackListener;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (configManager.getMainConfig().settings.debugMode) {
            logger.info("Cleaning up data for disconnecting player: " + player.getName());
        }

        // Clean up spawn manager data
        spawnManager.cleanupPlayerData(player.getUniqueId());

        // Clean up party data only if configured
        if (configManager.getMainConfig().party.enabled
                && configManager.getMainConfig().party.removePlayerOnQuit) {
            partyManager.cleanupPlayerData(player.getUniqueId());
        }

        // Clean up resource pack listener data
        if (resourcePackListener != null) {
            resourcePackListener.cleanupPlayer(player.getUniqueId());
        }

        // Clean up safe location cache for this player
        if (configManager.getMainConfig().settings.safeLocationCache.advanced.clearPlayerCacheOnQuit) {
            SafeLocationFinder.clearPlayerCache(player.getUniqueId());
        }
    }
}