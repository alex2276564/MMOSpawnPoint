package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerResourcePackListener implements Listener {
    private final MMOSpawnPoint plugin;

    // Track players waiting for resource pack
    private final Map<UUID, Boolean> waitingForResourcePack = new ConcurrentHashMap<>();

    public PlayerResourcePackListener(MMOSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (!waitingForResourcePack.containsKey(playerId)) {
            return; // Not waiting for this player
        }

        PlayerResourcePackStatusEvent.Status status = event.getStatus();

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Resource pack status for " + player.getName() + ": " + status);
        }

        // Process any final status (SUCCESS, FAILED_DOWNLOAD, DECLINED, etc.)
        if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            waitingForResourcePack.remove(playerId);

            // Send appropriate message
            String message;
            if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
                message = plugin.getConfigManager().getMessagesConfig().resourcepack.loaded;
                plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.loaded", message);
            } else {
                message = plugin.getConfigManager().getMessagesConfig().resourcepack.failed;
                plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.failed", message);
            }

            // Process join spawn after resource pack is ready
            plugin.getRunner().runGlobalLater(() -> {
                if (player.isOnline() && !player.isDead()) {
                    boolean success = plugin.getSpawnManager().processJoinSpawn(player);
                    if (!success && plugin.getConfigManager().getMainConfig().settings.debugMode) {
                        plugin.getLogger().info("Resource pack ready - join spawn processing failed for " + player.getName());
                    }
                }
            }, 1L);
        }
    }

    public void addWaitingPlayer(Player player) {
        waitingForResourcePack.put(player.getUniqueId(), true);

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Added " + player.getName() + " to resource pack waiting list");
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