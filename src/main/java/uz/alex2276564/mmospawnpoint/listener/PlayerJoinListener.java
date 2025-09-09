package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;

public class PlayerJoinListener implements Listener {
    private final MMOSpawnPoint plugin;

    public PlayerJoinListener(MMOSpawnPoint plugin) {
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

            // Notify the player that join teleport is skipped because they are dead
            String skipped = plugin.getConfigManager().getMessagesConfig().join.skippedDead;
            if (skipped != null && !skipped.isEmpty()) {
                plugin.getMessageManager().sendMessageKeyed(player, "join.skippedDead", skipped);
            }

            return;
        }

        // Check party scope
        String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
        if (plugin.getConfigManager().getMainConfig().party.enabled &&
                ("join".equals(partyScope) || "both".equals(partyScope)) && plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Party system active for joins, processing party join spawn for " + player.getName());
        }


        // Handle resource pack waiting
        if (plugin.getConfigManager().getMainConfig().join.waitForResourcePack) {
            handleResourcePackWait(player);
        } else {
            // Process join spawn immediately
            plugin.getRunner().runGlobalLater(() -> {
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
        String waitingMessage = plugin.getConfigManager().getMessagesConfig().resourcepack.waiting;
        if (!waitingMessage.isEmpty()) {
            plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.waiting", waitingMessage);
        }

        // Move to waiting room if enabled
        if (plugin.getConfigManager().getMainConfig().join.useWaitingRoomForResourcePack) {
            moveToWaitingRoom(player);
        }

        // Set timeout
        int timeout = plugin.getConfigManager().getMainConfig().join.resourcePackTimeout;
        plugin.getRunner().runGlobalLater(() -> {
            if (player.isOnline() && resourcePackListener != null &&
                    resourcePackListener.isWaitingForResourcePack(player.getUniqueId())) {

                // Timeout reached
                resourcePackListener.removeWaitingPlayer(player.getUniqueId());

                String timeoutMessage = plugin.getConfigManager().getMessagesConfig().resourcepack.timeout;
                if (!timeoutMessage.isEmpty()) {
                    plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.timeout", timeoutMessage);
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
        World world = Bukkit.getWorld(waitingRoomConfig.world);

        if (world != null) {
            Location waitingRoom = new org.bukkit.Location(
                    world,
                    waitingRoomConfig.x,
                    waitingRoomConfig.y,
                    waitingRoomConfig.z,
                    waitingRoomConfig.yaw,
                    waitingRoomConfig.pitch
            );

            plugin.getRunner().teleportAsync(player, waitingRoom)
                    .thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            String waitingMessage = plugin.getConfigManager().getMessagesConfig().resourcepack.waitingInRoom;
                            if (!waitingMessage.isEmpty()) {
                                plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.waitingInRoom", waitingMessage);
                            }
                            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                                plugin.getLogger().info("Moved " + player.getName() + " to waiting room for resource pack");
                            }
                        }
                    });
        }
    }

    private void processJoinSpawn(Player player) {
        boolean success = plugin.getSpawnManager().processJoinSpawn(player);
        if (!success && plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Join spawn processing failed for " + player.getName());
        }
    }
}