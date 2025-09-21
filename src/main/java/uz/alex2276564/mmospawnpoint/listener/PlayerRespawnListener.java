package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

public class PlayerRespawnListener implements Listener {
    private final MMOSpawnPoint plugin;

    public PlayerRespawnListener(MMOSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        try {
            Player player = event.getPlayer();
            var tele = plugin.getConfigManager().getMainConfig().settings.teleport;
            if (!tele.useSetRespawnLocationForDeath) {
                // Post-respawn flow (teleport after vanilla respawn)
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Respawn: using post-teleport flow for " + player.getName());
                }

                // Schedule processing on next tick (SpawnManager will apply delayTicks by itself)
                plugin.getRunner().runAtEntityLater(player, () -> {
                    try {
                        boolean ok = plugin.getSpawnManager().processDeathSpawn(player);
                        if (!ok && plugin.getConfigManager().getMainConfig().settings.debugMode) {
                            plugin.getLogger().info("Post-respawn teleport not applied for " + player.getName());
                        }
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Error in post-respawn teleport for " + player.getName() + ": " + ex.getMessage());
                        if (plugin.getConfigManager().getMainConfig().settings.debugMode) ex.printStackTrace();
                    }
                }, 1L);
                return;
            }

            Location deathLoc = player.getLocation();

            // Party first
            String scope = plugin.getConfigManager().getMainConfig().party.scope;
            if (plugin.getConfigManager().getMainConfig().party.enabled &&
                    plugin.getPartyManager() != null && ("death".equals(scope) || "both".equals(scope))) {
                Location partyLoc = plugin.getPartyManager().findPartyRespawnLocation(player, deathLoc);
                if (partyLoc != null && partyLoc != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    event.setRespawnLocation(partyLoc);

                    // run AFTER (if any pending) on next tick, since vanilla respawn will move the player
                    plugin.getRunner().runAtEntityLater(player, () -> plugin.getSpawnManager().runAfterPhaseIfPending(player), 1L);

                    return;
                }
            }

            // Resolve MSP spawn (this returns either final or waiting room if requireSafe)
            Location loc = plugin.getSpawnManager().findSpawnLocationByPriority("death", deathLoc, player);
            if (loc != null) {
                event.setRespawnLocation(loc);

                // run AFTER (if any pending) on next tick for non-waiting-room flows
                // (for waiting-room flows, AFTER will be executed by SafeSearchJob.finish())
                plugin.getRunner().runAtEntityLater(player, () -> plugin.getSpawnManager().runAfterPhaseIfPending(player), 1L);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player respawn for " + event.getPlayer().getName() + ": " + e.getMessage());
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) e.printStackTrace();
        }
    }
}
