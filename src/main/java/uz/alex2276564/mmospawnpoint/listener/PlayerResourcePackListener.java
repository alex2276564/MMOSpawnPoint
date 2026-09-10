package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PlayerResourcePackListener implements Listener {

    private final MMOSpawnPointConfigManager configManager;
    private final SpawnManager spawnManager;
    private final Runner runner;
    private final MessageManager messageManager;
    private final Logger logger;

    // Track players waiting for resource pack
    private final Map<UUID, Boolean> waitingForResourcePack = new ConcurrentHashMap<>();

    public PlayerResourcePackListener(MMOSpawnPointConfigManager configManager,
                                      SpawnManager spawnManager,
                                      Runner runner,
                                      MessageManager messageManager,
                                      Logger logger) {
        this.configManager = configManager;
        this.spawnManager = spawnManager;
        this.runner = runner;
        this.messageManager = messageManager;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!waitingForResourcePack.containsKey(playerId)) {
            return; // Not waiting for this player
        }

        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        if (configManager.getMainConfig().settings.debugMode) {
            logger.info("Resource pack status for " + player.getName() + ": " + status);
        }

        // Process any final status (SUCCESS, FAILED_DOWNLOAD, DECLINED, etc.)
        if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            waitingForResourcePack.remove(playerId);

            // Send appropriate message
            String message;
            if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
                message = configManager.getMessagesConfig().resourcepack.loaded;
                messageManager.sendMessageKeyed(player, "resourcepack.loaded", message);
            } else {
                message = configManager.getMessagesConfig().resourcepack.failed;
                messageManager.sendMessageKeyed(player, "resourcepack.failed", message);
            }

            // Process join spawn after resource pack is ready
            runner.runAtEntityLater(player, () -> {
                if (player.isOnline() && !player.isDead()) {
                    boolean success = spawnManager.processJoinSpawn(player);
                    if (!success && configManager.getMainConfig().settings.debugMode) {
                        logger.info("Resource pack ready - join spawn processing failed for " + player.getName());
                    }
                }
            }, 1L);
        }
    }

    public void addWaitingPlayer(Player player) {
        waitingForResourcePack.put(player.getUniqueId(), true);

        if (configManager.getMainConfig().settings.debugMode) {
            logger.info("Added " + player.getName() + " to resource pack waiting list");
        }
    }

    public boolean isWaitingForResourcePack(UUID playerId) {
        return waitingForResourcePack.containsKey(playerId);
    }

    public void removeWaitingPlayer(UUID playerId) {
        waitingForResourcePack.remove(playerId);
    }

    public void cleanup() {
        waitingForResourcePack.clear();
    }

    // Called when player quits to clean up
    public void cleanupPlayer(UUID playerId) {
        waitingForResourcePack.remove(playerId);
    }
}