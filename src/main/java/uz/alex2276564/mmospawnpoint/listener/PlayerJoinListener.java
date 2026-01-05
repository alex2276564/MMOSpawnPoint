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
import uz.alex2276564.mmospawnpoint.events.MSPPreTeleportEvent;

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
            plugin.getMessageManager().sendMessageKeyed(player, "join.skippedDead", skipped);
            return;
        }

        var mainConfig = plugin.getConfigManager().getMainConfig();

        // Party scope debug (actual party join spawn is handled either in PlayerSpawnLocationEvent
        // or in processJoinSpawn, depending on config)
        String partyScope = mainConfig.party.scope;
        if (mainConfig.party.enabled
                && ("join".equalsIgnoreCase(partyScope) || "both".equalsIgnoreCase(partyScope))
                && mainConfig.settings.debugMode) {
            plugin.getLogger().info("Party system active for joins for " + player.getName());
        }

        // Resource pack waiting takes priority and always uses post-join teleport flow
        if (mainConfig.join.waitForResourcePack) {
            handleResourcePackWait(player);
            return;
        }

        // If we use PlayerSpawnLocationEvent for join and this MC version supports it,
        // MSP spawn was already handled there (no post-join teleport)
        if (mainConfig.settings.teleport.useSetSpawnLocationForJoin
                && plugin.isSpawnLocationJoinSupported()) {
            if (mainConfig.settings.debugMode) {
                plugin.getLogger().info("Join spawn for " + player.getName()
                        + " is handled via PlayerSpawnLocationEvent (no post-join teleport)");
            }
            return;
        }

        // Legacy / fallback behavior: process join spawn via post-join teleport
        plugin.getRunner().runAtEntityLater(player, () -> {
            if (player.isOnline() && !player.isDead()) {
                processJoinSpawn(player);
            }
        }, 1L);
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
            plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.waiting", waitingMessage);

        // Move to waiting room if enabled
        if (plugin.getConfigManager().getMainConfig().join.useWaitingRoomForResourcePack) {
            moveToWaitingRoom(player);
        }

        // Set timeout
        int timeout = plugin.getConfigManager().getMainConfig().join.resourcePackTimeout;
        plugin.getRunner().runAtEntityLater(player, () -> {
            if (player.isOnline() && resourcePackListener != null &&
                    resourcePackListener.isWaitingForResourcePack(player.getUniqueId())) {

                // Timeout reached
                resourcePackListener.removeWaitingPlayer(player.getUniqueId());

                String timeoutMessage = plugin.getConfigManager().getMessagesConfig().resourcepack.timeout;
                plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.timeout", timeoutMessage);

                // Process join spawn anyway (post-join teleport flow)
                if (!player.isDead()) {
                    processJoinSpawn(player);
                }
            }
        }, timeout * 20L);  // Convert seconds to ticks
    }

    private void moveToWaitingRoom(Player player) {
        if (!plugin.getConfigManager().getMainConfig().settings.waitingRoom.enabled) {
            return;
        }

        // Get global waiting room location
        var waitingRoomConfig = plugin.getConfigManager().getMainConfig().settings.waitingRoom.location;
        World world = Bukkit.getWorld(waitingRoomConfig.world);
        if (world == null) {
            return;
        }

        Location target = new Location(
                world,
                waitingRoomConfig.x,
                waitingRoomConfig.y,
                waitingRoomConfig.z,
                waitingRoomConfig.yaw,
                waitingRoomConfig.pitch
        );

        plugin.getRunner().runAtEntity(player, () -> {
            if (!player.isOnline()) return;

            Location from = player.getLocation().clone();

            // PRE
            MSPPreTeleportEvent pre = new MSPPreTeleportEvent(
                    player, "join", "WAITING_ROOM", from, target.clone()
            );
            Bukkit.getPluginManager().callEvent(pre);
            if (pre.isCancelled()) return;

            Location to = pre.getTo();

            plugin.getRunner().teleportAsync(player, to).thenAccept(success -> {
                if (!Boolean.TRUE.equals(success)) return;

                // POST
                plugin.getRunner().runAtEntity(player, () -> {
                    uz.alex2276564.mmospawnpoint.events.MSPPostTeleportEvent post =
                            new uz.alex2276564.mmospawnpoint.events.MSPPostTeleportEvent(
                                    player, "join", "WAITING_ROOM", from, to
                            );
                    Bukkit.getPluginManager().callEvent(post);

                    String waitingMessage = plugin.getConfigManager().getMessagesConfig().resourcepack.waitingInRoom;
                    if (!waitingMessage.isEmpty()) {
                        plugin.getMessageManager().sendMessageKeyed(player, "resourcepack.waitingInRoom", waitingMessage);
                    }
                    if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                        plugin.getLogger().info("Moved " + player.getName() + " to waiting room for resource pack");
                    }
                });
            });
        });
    }

    private void processJoinSpawn(Player player) {
        boolean success = plugin.getSpawnManager().processJoinSpawn(player);
        if (!success && plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Join spawn processing failed for " + player.getName());
        }
    }
}