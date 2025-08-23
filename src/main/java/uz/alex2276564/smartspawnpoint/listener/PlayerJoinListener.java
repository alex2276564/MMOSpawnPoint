package uz.alex2276564.smartspawnpoint.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;

public class PlayerJoinListener implements Listener {
    private final SmartSpawnPoint plugin;

    public PlayerJoinListener(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Handle dead players (existing logic)
        if (player.isDead()) {
            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Player " + player.getName() + " joined while dead, handling respawn");
            }
            plugin.getSpawnManager().recordDeathLocation(player, player.getLocation());
            return;
        }

        // Check party scope
        String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
        if (plugin.getConfigManager().getMainConfig().party.enabled &&
                ("joins".equals(partyScope) || "both".equals(partyScope)) && plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Party system active for joins, processing party join spawn for " + player.getName());
        }


        // Handle resource pack waiting
        if (plugin.getConfigManager().getMainConfig().joins.waitForResourcePack) {
            handleResourcePackWait(player);
        } else {
            // Process join spawn immediately
            plugin.getRunner().runDelayed(() -> {
                if (player.isOnline() && !player.isDead()) {
                    processJoinSpawn(player);
                }
            }, 1L);
        }
    }

    private void handleResourcePackWait(Player player) {
        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Waiting for resource pack for " + player.getName());
        }

        // Add player to resource pack waiting list
        PlayerResourcePackListener resourcePackListener = plugin.getResourcePackListener();
        if (resourcePackListener != null) {
            resourcePackListener.addWaitingPlayer(player);
        }

        // Send waiting message
        String waitingMessage = plugin.getConfigManager().getMessagesConfig().general.waitingForResourcePack;
        if (!waitingMessage.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, waitingMessage);
        }

        // Move to waiting room if enabled
        if (plugin.getConfigManager().getMainConfig().joins.useWaitingRoomForResourcePack) {
            moveToWaitingRoom(player);
        }

        // Set timeout
        int timeout = plugin.getConfigManager().getMainConfig().joins.resourcePackTimeout;
        plugin.getRunner().runDelayed(() -> {
            if (player.isOnline() && resourcePackListener != null &&
                    resourcePackListener.isWaitingForResourcePack(player.getUniqueId())) {

                // Timeout reached
                resourcePackListener.removeWaitingPlayer(player.getUniqueId());

                String timeoutMessage = plugin.getConfigManager().getMessagesConfig().joins.resourcePackTimeout;
                if (!timeoutMessage.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, timeoutMessage);
                }

                // Process join spawn anyway
                if (!player.isDead()) {
                    processJoinSpawn(player);
                }
            }
        }, timeout * 20L); // Convert seconds to ticks
    }

    private void moveToWaitingRoom(Player player) {
        if (!plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled) {
            return;
        }

        // Get global waiting room location
        var waitingRoomConfig = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(waitingRoomConfig.world);

        if (world != null) {
            org.bukkit.Location waitingRoom = new org.bukkit.Location(
                    world,
                    waitingRoomConfig.x,
                    waitingRoomConfig.y,
                    waitingRoomConfig.z,
                    waitingRoomConfig.yaw,
                    waitingRoomConfig.pitch
            );

            player.teleport(waitingRoom);

            String waitingMessage = plugin.getConfigManager().getMessagesConfig().joins.waitingInRoom;
            if (!waitingMessage.isEmpty()) {
                plugin.getMessageManager().sendMessage(player, waitingMessage);
            }

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Moved " + player.getName() + " to waiting room for resource pack");
            }
        }
    }

    private void processJoinSpawn(Player player) {
        boolean success = plugin.getSpawnManager().processJoinSpawn(player);
        if (!success && plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Join spawn processing failed for " + player.getName());
        }
    }
}