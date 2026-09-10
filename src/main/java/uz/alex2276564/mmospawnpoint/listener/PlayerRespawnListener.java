package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.party.PartyManager;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerRespawnListener implements Listener {

    private final MMOSpawnPointConfigManager configManager;
    private final SpawnManager spawnManager;
    private final PartyManager partyManager;
    private final Runner runner;
    private final Logger logger;

    public PlayerRespawnListener(MMOSpawnPointConfigManager configManager,
                                 SpawnManager spawnManager,
                                 PartyManager partyManager,
                                 Runner runner,
                                 Logger logger) {
        this.configManager = configManager;
        this.spawnManager = spawnManager;
        this.partyManager = partyManager;
        this.runner = runner;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        try {
            Player player = event.getPlayer();
            var tele = configManager.getMainConfig().settings.teleport;
            if (!tele.useSetRespawnLocationForDeath) {
                // Post-respawn flow (teleport after vanilla respawn)
                if (configManager.getMainConfig().settings.debugMode) {
                    logger.info("Respawn: using post-teleport flow for " + player.getName());
                }

                // Schedule processing on next tick (SpawnManager will apply delayTicks by itself)
                runner.runAtEntityLater(player, () -> {
                    try {
                        boolean ok = spawnManager.processDeathSpawn(player);
                        if (!ok && configManager.getMainConfig().settings.debugMode) {
                            logger.info("Post-respawn teleport not applied for " + player.getName());
                        }
                    } catch (Exception ex) {
                        logger.severe("Error in post-respawn teleport for " + player.getName() + ": " + ex.getMessage());
                        if (configManager.getMainConfig().settings.debugMode) {
                            logger.log(
                                    Level.SEVERE,
                                    "Detailed exception in post-respawn teleport for " + player.getName(),
                                    ex
                            );
                        }
                    }
                }, 1L);
                return;
            }

            Location deathLoc = player.getLocation();

            // Party first
            String scope = configManager.getMainConfig().party.scope;
            if (configManager.getMainConfig().party.enabled &&
                    partyManager != null &&
                    ("death".equals(scope) || "both".equals(scope))) {

                Location partyLoc = partyManager.findPartyRespawnLocation(player, deathLoc);
                if (partyLoc != null && partyLoc != PartyManager.FALLBACK_TO_NORMAL_SPAWN_MARKER) {
                    event.setRespawnLocation(partyLoc);

                    // run AFTER (if any pending) on next tick, since vanilla respawn will move the player
                    runner.runAtEntityLater(player, () -> spawnManager.runAfterPhaseIfPending(player, "death"), 1L);

                    return;
                }
            }

            // Resolve MSP spawn (this returns either final or waiting room if requireSafe)
            Location loc = spawnManager.findSpawnLocationByPriority("death", deathLoc, player);
            if (loc != null) {
                event.setRespawnLocation(loc);

                // run AFTER (if any pending) on next tick for non-waiting-room flows
                // (for waiting-room flows, AFTER will be executed by SafeSearchJob.finish())
                runner.runAtEntityLater(player, () -> spawnManager.runAfterPhaseIfPending(player, "death"), 1L);
            }
        } catch (Exception e) {
            logger.severe("Error handling player respawn for " + event.getPlayer().getName() + ": " + e.getMessage());
            if (configManager.getMainConfig().settings.debugMode) {
                logger.log(
                        Level.SEVERE,
                        "Detailed exception while handling player respawn for " + event.getPlayer().getName(),
                        e
                );
            }
        }
    }
}