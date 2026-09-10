package uz.alex2276564.mmospawnpoint.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import uz.alex2276564.mmospawnpoint.config.MMOSpawnPointConfigManager;
import uz.alex2276564.mmospawnpoint.events.MSPPreTeleportEvent;
import uz.alex2276564.mmospawnpoint.manager.SpawnManager;
import uz.alex2276564.mmospawnpoint.utils.adventure.MessageManager;
import uz.alex2276564.mmospawnpoint.utils.runner.Runner;

import java.util.logging.Logger;

public class PlayerJoinListener implements Listener {

    private final MMOSpawnPointConfigManager configManager;
    private final SpawnManager spawnManager;
    private final Runner runner;
    private final MessageManager messageManager;
    private final Logger logger;
    private final PlayerResourcePackListener resourcePackListener;
    private final boolean spawnLocationJoinSupported;

    public PlayerJoinListener(MMOSpawnPointConfigManager configManager,
                              SpawnManager spawnManager,
                              Runner runner,
                              MessageManager messageManager,
                              Logger logger,
                              PlayerResourcePackListener resourcePackListener,
                              boolean spawnLocationJoinSupported) {
        this.configManager = configManager;
        this.spawnManager = spawnManager;
        this.runner = runner;
        this.messageManager = messageManager;
        this.logger = logger;
        this.resourcePackListener = resourcePackListener;
        this.spawnLocationJoinSupported = spawnLocationJoinSupported;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Handle dead players (existing logic)
        if (player.isDead()) {
            if (configManager.getMainConfig().settings.debugMode) {
                logger.info("Player " + player.getName() + " joined while dead, handling respawn");
            }
            spawnManager.recordDeathLocation(player, player.getLocation());

            // Notify the player that join teleport is skipped because they are dead
            String skipped = configManager.getMessagesConfig().join.skippedDead;
            messageManager.sendMessageKeyed(player, "join.skippedDead", skipped);
            return;
        }

        var mainConfig = configManager.getMainConfig();

        // Party scope debug (actual party join spawn is handled either in PlayerSpawnLocationEvent
        // or in processJoinSpawn, depending on config)
        String partyScope = mainConfig.party.scope;
        if (mainConfig.party.enabled
                && ("join".equalsIgnoreCase(partyScope) || "both".equalsIgnoreCase(partyScope))
                && mainConfig.settings.debugMode) {
            logger.info("Party system active for joins for " + player.getName());
        }

        // Resource pack waiting takes priority and always uses post-join teleport flow
        if (mainConfig.join.waitForResourcePack) {
            handleResourcePackWait(player);
            return;
        }

        // If we use PlayerSpawnLocationEvent for join and this MC version supports it,
        // MSP spawn was already handled there (no post-join teleport)
        if (mainConfig.settings.teleport.useSetSpawnLocationForJoin
                && spawnLocationJoinSupported) {

            if (mainConfig.settings.debugMode) {
                logger.info("Join spawn for " + player.getName()
                        + " is handled via PlayerSpawnLocationEvent (no post-join teleport)");
            }

            // Handle all deferred join phases (BEFORE/WAITING_ROOM/AFTER) after the player has fully joined.
            runner.runAtEntityLater(player, () -> {
                if (!player.isOnline() || player.isDead()) {
                    if (mainConfig.settings.debugMode) {
                        logger.info("Skipping join phases for " + player.getName()
                                + " because the player is no longer online or is dead.");
                    }
                    return;
                }

                spawnManager.runJoinPhasesAfterSpawn(player);
            }, 1L);

            return;
        }

        // Legacy / fallback behavior: process join spawn via post-join teleport
        runner.runAtEntityLater(player, () -> {
            if (player.isOnline() && !player.isDead()) {
                processJoinSpawn(player);
            }
        }, 1L);
    }

    private void handleResourcePackWait(Player player) {
        if (configManager.getMainConfig().settings.debugMode) {
            logger.info("Waiting for resource pack for " + player.getName());
        }

        // Add player to resource pack waiting list
        if (resourcePackListener != null) {
            resourcePackListener.addWaitingPlayer(player);
        }

        // Send waiting message
        String waitingMessage = configManager.getMessagesConfig().resourcepack.waiting;
        messageManager.sendMessageKeyed(player, "resourcepack.waiting", waitingMessage);

        // Move to waiting room if enabled
        if (configManager.getMainConfig().join.useWaitingRoomForResourcePack) {
            moveToWaitingRoom(player);
        }

        // Set timeout
        int timeout = configManager.getMainConfig().join.resourcePackTimeout;
        runner.runAtEntityLater(player, () -> {
            if (player.isOnline()
                    && resourcePackListener != null
                    && resourcePackListener.isWaitingForResourcePack(player.getUniqueId())) {

                // Timeout reached
                resourcePackListener.removeWaitingPlayer(player.getUniqueId());

                String timeoutMessage = configManager.getMessagesConfig().resourcepack.timeout;
                messageManager.sendMessageKeyed(player, "resourcepack.timeout", timeoutMessage);

                // Process join spawn anyway (post-join teleport flow)
                if (!player.isDead()) {
                    processJoinSpawn(player);
                }
            }
        }, timeout * 20L); // Convert seconds to ticks
    }

    private void moveToWaitingRoom(Player player) {
        if (!configManager.getMainConfig().settings.waitingRoom.enabled) {
            return;
        }

        // Get global waiting room location
        var waitingRoomConfig = configManager.getMainConfig().settings.waitingRoom.location;
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

        runner.runAtEntity(player, () -> {
            if (!player.isOnline()) return;

            Location from = player.getLocation().clone();

            // PRE
            MSPPreTeleportEvent pre = new MSPPreTeleportEvent(
                    player, "join", "WAITING_ROOM", from, target.clone()
            );
            Bukkit.getPluginManager().callEvent(pre);
            if (pre.isCancelled()) return;

            Location to = pre.getTo();

            runner.teleportAsync(player, to).thenAccept(success -> {
                if (!Boolean.TRUE.equals(success)) return;

                // POST
                runner.runAtEntity(player, () -> {
                    uz.alex2276564.mmospawnpoint.events.MSPPostTeleportEvent post =
                            new uz.alex2276564.mmospawnpoint.events.MSPPostTeleportEvent(
                                    player, "join", "WAITING_ROOM", from, to
                            );
                    Bukkit.getPluginManager().callEvent(post);

                    String waitingMessage = configManager.getMessagesConfig().resourcepack.waitingInRoom;
                    if (!waitingMessage.isEmpty()) {
                        messageManager.sendMessageKeyed(player, "resourcepack.waitingInRoom", waitingMessage);
                    }
                    if (configManager.getMainConfig().settings.debugMode) {
                        logger.info("Moved " + player.getName() + " to waiting room for resource pack");
                    }
                });
            });
        });
    }

    private void processJoinSpawn(Player player) {
        boolean success = spawnManager.processJoinSpawn(player);
        if (!success && configManager.getMainConfig().settings.debugMode) {
            logger.info("Join spawn processing failed for " + player.getName());
        }
    }
}