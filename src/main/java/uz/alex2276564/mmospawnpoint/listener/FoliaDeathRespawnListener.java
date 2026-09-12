package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Folia-compatible death respawn handler using InventoryCloseEvent hack.
 * <p>
 * CONTEXT:
 * - On Folia >= 1.20, PlayerRespawnEvent is unreliable/awkward:
 * the target region for the respawn position is not owned during
 * event invocation, so modifying the respawn location is unsafe.
 * - See: https://github.com/PaperMC/Folia/issues/105
 * <p>
 * WORKAROUND (from kerudion's comment):
 * - Detect the "death screen" closing via InventoryCloseEvent on
 * the player CRAFTING inventory while the player is still dead.
 * - Then schedule our post-respawn teleport logic via Runner.
 * <p>
 * BEHAVIOR:
 * - On Folia we ignore settings.teleport.useSetRespawnLocationForDeath
 * and always use the post-teleport flow (SpawnManager.processDeathSpawn).
 * - On Paper, normal PlayerRespawnEvent-based logic is used instead.
 */
public class FoliaDeathRespawnListener implements Listener {

    private final MMOSpawnPointConfigManager configManager;
    private final SpawnManager spawnManager;
    private final Runner runner;
    private final Logger logger;

    public FoliaDeathRespawnListener(MMOSpawnPointConfigManager configManager,
                                     SpawnManager spawnManager,
                                     Runner runner,
                                     Logger logger) {
        this.configManager = configManager;
        this.spawnManager = spawnManager;
        this.runner = runner;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Only death screen / player inventory close
        if (event.getInventory().getType() != InventoryType.CRAFTING) {
            return;
        }

        // We only care about the moment when the player is still "dead"
        // but connected, with health <= 0. This mirrors the hack from
        // https://github.com/PaperMC/Folia/issues/105#issuecomment-2270697815
        if (!player.isDead()) return;
        if (!player.isOnline()) return;
        if (player.getHealth() > 0.0) return;

        // From this point we know: Folia, player death flow, "respawn" about to happen.
        if (configManager.getMainConfig().settings.debugMode) {
            logger.info("[MMOSpawnPoint] Folia death-respawn detected for " + player.getName()
                    + " via InventoryCloseEvent hack");
        }

        // On Folia we ALWAYS use the post-respawn teleport flow, because
        // PlayerRespawnEvent and setRespawnLocation are not reliable/safe.
        runner.runAtEntityLater(player, () -> {
            try {
                boolean ok = spawnManager.processDeathSpawn(player);
                if (!ok && configManager.getMainConfig().settings.debugMode) {
                    logger.info("[MMOSpawnPoint] Folia death-respawn: MSP/party teleport not applied for " + player.getName());
                }
            } catch (Exception ex) {
                logger.severe("[MMOSpawnPoint] Error in Folia death-respawn teleport for "
                        + player.getName() + ": " + ex.getMessage());
                if (configManager.getMainConfig().settings.debugMode) {
                    logger.log(
                            Level.SEVERE,
                            "[MMOSpawnPoint] Detailed exception in Folia death-respawn teleport for "
                                    + player.getName(),
                            ex
                    );
                }
            }
        }, 1L);
    }
}