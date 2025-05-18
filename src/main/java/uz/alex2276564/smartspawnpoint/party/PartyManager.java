package uz.alex2276564.smartspawnpoint.party;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.config.ConfigManager;
import uz.alex2276564.smartspawnpoint.model.SpawnPoint;
import uz.alex2276564.smartspawnpoint.util.WorldGuardUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PartyManager {
    private final SmartSpawnPoint plugin;
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> playerPartyMap = new HashMap<>();
    private final Map<UUID, UUID> pendingInvitations = new HashMap<>();
    private BukkitTask cleanupTask;

    @Getter
    private final int maxPartySize;

    @Getter
    private final int invitationExpiryTime;

    @Getter
    private final int maxRespawnDistance;

    @Getter
    private final int respawnCooldown;

    public PartyManager(SmartSpawnPoint plugin) {
        this.plugin = plugin;
        ConfigManager config = plugin.getConfigManager();

        this.maxPartySize = config.getPartyMaxSize();
        this.invitationExpiryTime = config.getPartyInvitationExpiry();
        this.maxRespawnDistance = config.getPartyMaxRespawnDistance();
        this.respawnCooldown = config.getPartyRespawnCooldown();

        startCleanupTask();
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            cleanupParties();
            cleanupInvitations();
        }, 1200L, 1200L); // Run every minute (20 ticks * 60 seconds)
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        parties.clear();
        playerPartyMap.clear();
        pendingInvitations.clear();
    }

    private void cleanupParties() {
        // Remove empty parties
        parties.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        // Update player-party mapping
        playerPartyMap.clear();
        for (Map.Entry<UUID, Party> entry : parties.entrySet()) {
            Party party = entry.getValue();
            for (UUID memberId : party.getMembers()) {
                playerPartyMap.put(memberId, party.getId());
            }
        }
    }

    private void cleanupInvitations() {
        // Clean up expired invitations in each party
        for (Party party : parties.values()) {
            party.cleanExpiredInvitations();
        }

        // Clean up pending invitations map
        Iterator<Map.Entry<UUID, UUID>> it = pendingInvitations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            UUID partyId = entry.getValue();
            Party party = getParty(partyId);
            if (party == null || !party.hasInvitation(entry.getKey())) {
                it.remove();
            }
        }
    }

    // Create a new party
    public Party createParty(Player leader) {
        UUID leaderId = leader.getUniqueId();

        // Check if player is already in a party
        if (isInParty(leaderId)) {
            return getPlayerParty(leaderId);
        }

        Party party = new Party(leaderId);
        parties.put(party.getId(), party);
        playerPartyMap.put(leaderId, party.getId());

        return party;
    }

    // Invite a player to join a party
    public boolean invitePlayer(Player leader, Player invited) {
        UUID leaderId = leader.getUniqueId();
        UUID invitedId = invited.getUniqueId();

        // Check if leader is in a party
        if (!isInParty(leaderId)) {
            return false;
        }

        Party party = getPlayerParty(leaderId);

        // Check if leader is party leader
        if (!party.isLeader(leaderId)) {
            return false;
        }

        // Check if invited player is already in a party
        if (isInParty(invitedId)) {
            return false;
        }

        // Check party size limit (skip if maxPartySize <= 0 for unlimited)
        if (maxPartySize > 0 && party.size() >= maxPartySize) {
            return false;
        }

        // Send invitation
        party.invite(invitedId, invitationExpiryTime);
        pendingInvitations.put(invitedId, party.getId());

        return true;
    }

    // Accept a party invitation
    public boolean acceptInvitation(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player has a pending invitation
        if (!pendingInvitations.containsKey(playerId)) {
            return false;
        }

        UUID partyId = pendingInvitations.get(playerId);
        Party party = getParty(partyId);

        if (party == null || !party.hasInvitation(playerId)) {
            pendingInvitations.remove(playerId);
            return false;
        }

        // Add player to party
        party.addMember(playerId);
        playerPartyMap.put(playerId, partyId);
        pendingInvitations.remove(playerId);

        return true;
    }

    // Decline a party invitation
    public boolean declineInvitation(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player has a pending invitation
        if (!pendingInvitations.containsKey(playerId)) {
            return false;
        }

        UUID partyId = pendingInvitations.get(playerId);
        Party party = getParty(partyId);

        if (party != null) {
            party.getInvitations().remove(playerId);
        }

        pendingInvitations.remove(playerId);
        return true;
    }

    // Leave a party
    public boolean leaveParty(Player player) {
        UUID playerId = player.getUniqueId();

        // Check if player is in a party
        if (!isInParty(playerId)) {
            return false;
        }

        Party party = getPlayerParty(playerId);
        party.removeMember(playerId);
        playerPartyMap.remove(playerId);

        // If party is now empty, remove it
        if (party.isEmpty()) {
            parties.remove(party.getId());
        }

        return true;
    }

    // Remove a player from a party (kick)
    public boolean removePlayer(Player leader, Player target) {
        UUID leaderId = leader.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Check if leader is in a party
        if (!isInParty(leaderId)) {
            return false;
        }

        Party party = getPlayerParty(leaderId);

        // Check if leader is party leader
        if (!party.isLeader(leaderId)) {
            return false;
        }

        // Check if target is in the same party
        if (!party.isMember(targetId)) {
            return false;
        }

        // Remove player from party
        party.removeMember(targetId);
        playerPartyMap.remove(targetId);

        return true;
    }

    // Set a new party leader
    public boolean setLeader(Player currentLeader, Player newLeader) {
        UUID currentLeaderId = currentLeader.getUniqueId();
        UUID newLeaderId = newLeader.getUniqueId();

        // Check if current leader is in a party
        if (!isInParty(currentLeaderId)) {
            return false;
        }

        Party party = getPlayerParty(currentLeaderId);

        // Check if current leader is party leader
        if (!party.isLeader(currentLeaderId)) {
            return false;
        }

        // Check if new leader is in the same party
        if (!party.isMember(newLeaderId)) {
            return false;
        }

        // Set new leader
        party.setLeader(newLeaderId);

        return true;
    }

    // Set respawn mode for a party
    public boolean setRespawnMode(Player leader, Party.RespawnMode mode) {
        UUID leaderId = leader.getUniqueId();

        // Check if leader is in a party
        if (!isInParty(leaderId)) {
            return false;
        }

        Party party = getPlayerParty(leaderId);

        // Check if leader is party leader
        if (!party.isLeader(leaderId)) {
            return false;
        }

        // Set respawn mode
        party.setRespawnMode(mode);

        return true;
    }

    // Set respawn target for a party
    public boolean setRespawnTarget(Player leader, Player target) {
        UUID leaderId = leader.getUniqueId();
        UUID targetId = target.getUniqueId();

        // Check if leader is in a party
        if (!isInParty(leaderId)) {
            return false;
        }

        Party party = getPlayerParty(leaderId);

        // Check if leader is party leader
        if (!party.isLeader(leaderId)) {
            return false;
        }

        // Check if target is in the same party
        if (!party.isMember(targetId)) {
            return false;
        }

        // Set respawn target
        party.setRespawnTarget(targetId);

        return true;
    }

    // Check if a player is in a party
    public boolean isInParty(UUID playerId) {
        return playerPartyMap.containsKey(playerId);
    }

    // Get a player's party
    public Party getPlayerParty(UUID playerId) {
        UUID partyId = playerPartyMap.get(playerId);
        return parties.get(partyId);
    }

    // Get a party by ID
    public Party getParty(UUID partyId) {
        return parties.get(partyId);
    }

    // Get all parties
    public Collection<Party> getAllParties() {
        return parties.values();
    }

    // Get pending invitation for a player
    public UUID getPendingInvitation(UUID playerId) {
        return pendingInvitations.get(playerId);
    }

    // Find a suitable respawn location within a party
    public Location findPartyRespawnLocation(Player player, Location deathLocation) {
        UUID playerId = player.getUniqueId();

        // Check if player is in a party
        if (!isInParty(playerId)) {
            return null;
        }

        Party party = getPlayerParty(playerId);

        // Check respawn mode
        if (party.getRespawnMode() != Party.RespawnMode.PARTY_MEMBER) {
            return null;
        }

        // Check for "Walking Spawn Point" feature - players with permission respawn at their death location
        if (plugin.getConfigManager().isRespawnAtDeathEnabled() &&
                player.hasPermission(plugin.getConfigManager().getRespawnAtDeathPermission())) {

            // Send message to player
            String message = plugin.getConfigManager().getRespawnAtDeathMessage();
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            }

            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("Player " + player.getName() + " respawning at death location (walking spawn point)");
            }

            // Return death location
            return deathLocation;
        }

        // Check if player is on cooldown (skip if respawnCooldown <= 0 for unlimited)
        if (respawnCooldown > 0 && party.isOnRespawnCooldown(playerId)) {
            long remainingSeconds = party.getRemainingCooldown(playerId);

            // Send cooldown message
            Map<String, String> replacements = new HashMap<>();
            replacements.put("time", String.valueOf(remainingSeconds));
            String message = plugin.getConfigManager().formatPartyMessage("respawn-cooldown", replacements);
            if (!message.isEmpty()) {
                player.sendMessage(message);
            }

            return null;
        }

        // Find target player to respawn near
        Player target = null;
        if (party.getRespawnTarget() != null) {
            target = party.getRespawnTargetPlayer();
        }

        // If no specific target, find any online member
        if (target == null || !target.isOnline()) {
            List<Player> onlineMembers = party.getOnlineMembers();
            onlineMembers.remove(player); // Remove self from list

            if (onlineMembers.isEmpty()) {
                return null;
            }

            // Find closest party member
            Player closest = null;
            double closestDist = Double.MAX_VALUE;

            for (Player member : onlineMembers) {
                if (member.getWorld().equals(deathLocation.getWorld())) {
                    double dist = member.getLocation().distance(deathLocation);
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = member;
                    }
                }
            }

            if (closest != null) {
                target = closest;
            } else {
                // No member in same world, just pick first online member
                target = onlineMembers.get(0);
            }
        }

        if (target == null || !target.isOnline()) {
            return null;
        }

        // Get target location
        Location targetLocation = target.getLocation();

        // Check if target location is in a region/world where party respawn is disabled
        if (isPartyRespawnDisabled(targetLocation)) {
            // Send message
            String message = plugin.getConfigManager().getPartyMessage("respawn-disabled-region");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            }
            return null;
        }

        // Check distance limitation (skip if maxRespawnDistance <= 0 for unlimited)
        if (maxRespawnDistance > 0 && !target.getWorld().equals(deathLocation.getWorld())) {
            // Different world, apply distance check only if same world
            // For different worlds, we always allow teleport regardless of distance
        } else if (maxRespawnDistance > 0 && target.getLocation().distance(deathLocation) > maxRespawnDistance) {
            player.sendMessage("§cThe party member is too far away to respawn at their location.");
            return null;
        }

        // Set cooldown (only if respawnCooldown > 0)
        if (respawnCooldown > 0) {
            party.setRespawnCooldown(playerId, respawnCooldown);
        }

        // Send message
        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", target.getName());
        String message = plugin.getConfigManager().formatPartyMessage("respawned-at-member", replacements);
        if (!message.isEmpty()) {
            player.sendMessage(message);
        }

        return targetLocation;
    }



    // Check if party respawn is disabled in a location (region or world)
    private boolean isPartyRespawnDisabled(Location location) {
        // Check world-based spawn points
        String worldName = location.getWorld().getName();
        List<SpawnPoint> worldSpawns = plugin.getConfigManager().getWorldSpawns().get(worldName);

        if (worldSpawns != null) {
            for (SpawnPoint spawnPoint : worldSpawns) {
                if (spawnPoint.isPartyRespawnDisabled()) {
                    return true;
                }
            }
        }

        // Check region-based spawn points if WorldGuard is enabled
        if (plugin.isWorldGuardEnabled()) {
            Set<String> regions = WorldGuardUtils.getRegionsAt(location);
            if (!regions.isEmpty()) {
                for (SpawnPoint spawnPoint : plugin.getConfigManager().getRegionSpawns()) {
                    if (regions.contains(spawnPoint.getRegion()) &&
                            (spawnPoint.getRegionWorld().equals("*") || worldName.equals(spawnPoint.getRegionWorld())) &&
                            spawnPoint.isPartyRespawnDisabled()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public UUID getLeaderOfParty(UUID partyId) {
        Party party = getParty(partyId);
        return party != null ? party.getLeader() : null;
    }
}