package uz.alex2276564.smartspawnpoint.party;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.CoordinateSpawnsConfig;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.RegionSpawnsConfig;
import uz.alex2276564.smartspawnpoint.config.configs.spawnpointsconfig.WorldSpawnsConfig;
import uz.alex2276564.smartspawnpoint.manager.SpawnEntry;
import uz.alex2276564.smartspawnpoint.utils.WorldGuardUtils;

import java.util.*;

public class PartyManager {
    // Special marker location to indicate fallback to normal spawn
    public static final Location FALLBACK_TO_NORMAL_SPAWN_MARKER = new Location(null, 0, 0, 0);

    private final SmartSpawnPoint plugin;
    private final Map<UUID, Party> parties = new HashMap<>();
    private final Map<UUID, UUID> playerPartyMap = new HashMap<>();
    private final Map<UUID, UUID> pendingInvitations = new HashMap<>();

    private final Random random = new Random();

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

        this.maxPartySize = plugin.getConfigManager().getMainConfig().party.maxSize;
        this.invitationExpiryTime = plugin.getConfigManager().getMainConfig().party.invitationExpiry;
        this.maxRespawnDistance = plugin.getConfigManager().getMainConfig().party.maxRespawnDistance;
        this.respawnCooldown = plugin.getConfigManager().getMainConfig().party.respawnCooldown;

        startCleanupTask();
    }

    private void startCleanupTask() {
        plugin.getRunner().runPeriodical(() -> {
            cleanupParties();
            cleanupInvitations();
        }, 1200L, 1200L); // Run every minute (20 ticks * 60 seconds)
    }

    public void shutdown() {
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
            if (party == null || party.hasNoInvitation(entry.getKey())) {
                it.remove();
            }
        }
    }

    public void cleanupPlayerData(UUID playerId) {
        try {
            // Remove from any party
            if (isInParty(playerId)) {
                Party party = getPlayerParty(playerId);
                if (party != null) {
                    party.removeMember(playerId);
                    playerPartyMap.remove(playerId);

                    // If party is now empty, remove it
                    if (party.isEmpty()) {
                        parties.remove(party.getId());
                    }

                    if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                        plugin.getLogger().info("Removed player " + playerId + " from party");
                    }
                }
            }

            // Remove pending invitations FROM this player
            pendingInvitations.remove(playerId);

            // Remove any invitations sent TO this player
            for (Party party : parties.values()) {
                party.getInvitations().remove(playerId);
            }

            if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                plugin.getLogger().info("Cleaned up party data for player: " + playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up party data for " + playerId + ": " + e.getMessage());
        }
    }

    // ============================= PARTY MANAGEMENT =============================

    // Create a new party
    public void createParty(Player leader) {
        UUID leaderId = leader.getUniqueId();

        // Check if player is already in a party
        if (isInParty(leaderId)) {
            return; // Already in party, nothing to do
        }

        Party party = new Party(leaderId);
        parties.put(party.getId(), party);
        playerPartyMap.put(leaderId, party.getId());
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

        if (party == null || party.hasNoInvitation(playerId)) {
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
        if (party.isNotMember(targetId)) {
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
        if (party.isNotMember(newLeaderId)) {
            return false;
        }

        // Set new leader
        party.setLeader(newLeaderId);

        return true;
    }

    // Set respawn mode for a party
    public void setRespawnMode(Player leader, Party.RespawnMode mode) {
        UUID leaderId = leader.getUniqueId();

        // Check if leader is in a party
        if (!isInParty(leaderId)) {
            return;
        }

        Party party = getPlayerParty(leaderId);

        // Check if leader is party leader
        if (!party.isLeader(leaderId)) {
            return;
        }

        // Set respawn mode
        party.setRespawnMode(mode);
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
        if (party.isNotMember(targetId)) {
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

    // Get pending invitation for a player
    public UUID getPendingInvitation(UUID playerId) {
        return pendingInvitations.get(playerId);
    }

    // ============================= DEATH RESPAWN LOGIC =============================

    // Find a suitable respawn location within a party for deaths
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

        // Check if player is on cooldown
        if (respawnCooldown > 0 && party.isOnRespawnCooldown(playerId)) {
            long remainingSeconds = party.getRemainingCooldown(playerId);

            String message = plugin.getConfigManager().getMessagesConfig().party.respawnCooldown;
            message = message.replace("{time}", String.valueOf(remainingSeconds));
            if (!message.isEmpty()) {
                plugin.getMessageManager().sendMessage(player, message);
            }
            return null;
        }

        // Check for "Walking Spawn Point" feature
        if (plugin.getConfigManager().getMainConfig().party.respawnAtDeath.enabled &&
                player.hasPermission(plugin.getConfigManager().getMainConfig().party.respawnAtDeath.permission)) {

            return handleWalkingSpawnPoint(player, deathLocation);
        }

        // Continue with normal party respawn logic...
        boolean deathRestricted = plugin.getConfigManager().getMainConfig().party.respawnBehavior.checkDeathLocation &&
                isPartyRespawnDisabled(deathLocation);

        Player target = findBestTarget(party, player, deathLocation);
        if (target == null) {
            return null;
        }

        Location targetLocation = target.getLocation();
        boolean targetRestricted = plugin.getConfigManager().getMainConfig().party.respawnBehavior.checkTargetLocation &&
                isPartyRespawnDisabled(targetLocation);

        // Handle restriction logic based on configuration
        if (deathRestricted && targetRestricted) {
            return handleBothRestricted(player);
        } else if (deathRestricted) {
            return handleDeathRestricted(player, target);
        } else if (targetRestricted) {
            return handleTargetRestricted(player, party);
        }

        // Check distance limitation
        if (maxRespawnDistance > 0 && !target.getWorld().equals(deathLocation.getWorld())) {
            // Different world allowed
        } else if (maxRespawnDistance > 0 && target.getLocation().distance(deathLocation) > maxRespawnDistance) {
            String message = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
            if (!message.isEmpty()) {
                plugin.getMessageManager().sendMessage(player, message);
            }
            return null;
        }

        // Set cooldown and send success message
        if (respawnCooldown > 0) {
            party.setRespawnCooldown(playerId, respawnCooldown);
        }

        String message = plugin.getConfigManager().getMessagesConfig().party.respawnedAtMember;
        message = message.replace("{player}", target.getName());
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, message);
        }

        return targetLocation;
    }

    // ============================= JOIN RESPAWN LOGIC =============================

    // Find a suitable respawn location within a party for joins
    public Location findPartyJoinLocation(Player player) {
        UUID playerId = player.getUniqueId();

        if (!isInParty(playerId)) {
            return null;
        }

        Party party = getPlayerParty(playerId);

        if (party.getRespawnMode() != Party.RespawnMode.PARTY_MEMBER) {
            return null;
        }

        // Check if joins are allowed for this party based on scope
        String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
        if (!"joins".equals(partyScope) && !"both".equals(partyScope)) {
            return null;
        }

        // Check for "Walking Spawn Point" feature for joins
        if (plugin.getConfigManager().getMainConfig().party.respawnAtDeath.enabled &&
                player.hasPermission(plugin.getConfigManager().getMainConfig().party.respawnAtDeath.permission)) {

            return handleWalkingSpawnPointForJoin(player);
        }

        // Find suitable party member for join spawn
        Player target = findBestTargetForJoin(party, player);
        if (target == null) {
            return null;
        }

        Location targetLocation = target.getLocation();

        // Check if join location is in restricted area
        boolean targetRestricted = plugin.getConfigManager().getMainConfig().party.respawnBehavior.checkTargetLocation &&
                isPartyRespawnDisabled(targetLocation);

        if (targetRestricted) {
            return handleJoinTargetRestricted(player, party);
        }

        String message = plugin.getConfigManager().getMessagesConfig().party.respawnedAtMember;
        message = message.replace("{player}", target.getName());
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, message);
        }

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Player " + player.getName() + " joining at party member " + target.getName() + "'s location");
        }

        return targetLocation;
    }

    // ============================= WALKING SPAWN POINT LOGIC =============================

    private Location handleWalkingSpawnPoint(Player player, Location deathLocation) {
        // Check if walking spawn point should respect restrictions
        if (!plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior.respectRestrictions) {
            // Old behavior - always allow walking spawn point, ignore restrictions
            sendWalkingSpawnPointMessage(player);
            return deathLocation;
        }

        // Check restrictions based on configuration
        boolean deathRestricted = plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior.checkDeathLocation &&
                isPartyRespawnDisabled(deathLocation);

        boolean targetRestricted = false;
        if (plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior.checkTargetLocation) {
            // For walking spawn point, "target" is also the death location
            targetRestricted = isPartyRespawnDisabled(deathLocation);
        }

        // If no restrictions, allow walking spawn point
        if (!deathRestricted && !targetRestricted) {
            sendWalkingSpawnPointMessage(player);
            return deathLocation;
        }

        // Handle restricted walking spawn point based on configuration
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior.restrictedAreaBehavior;

        switch (behavior.toLowerCase()) {
            case "allow":
                // Force allow walking spawn point even in restricted areas
                sendWalkingSpawnPointMessage(player);
                return deathLocation;

            case "fallback_to_party":
                // Try normal party respawn instead
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Walking spawn point denied for " + player.getName() +
                            ", falling back to party respawn");
                }
                return null; // This will continue to normal party logic

            case "fallback_to_normal_spawn":
                // Skip party system entirely, use normal spawn logic
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Walking spawn point denied for " + player.getName() +
                            ", falling back to normal spawn");
                }
                // Return a special marker that tells the main spawn system to ignore party
                return FALLBACK_TO_NORMAL_SPAWN_MARKER;

            case "deny":
            default:
                // Deny walking spawn point and send message
                String restrictedMessage = plugin.getConfigManager().getMessagesConfig().party.walkingSpawnPointRestricted;
                if (!restrictedMessage.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, restrictedMessage);
                }

                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Walking spawn point denied for " + player.getName() +
                            " due to restrictions");
                }

                return null; // This will continue to normal party logic
        }
    }

    private Location handleWalkingSpawnPointForJoin(Player player) {
        // For joins, "walking spawn point" means spawn at current location
        Location currentLocation = player.getLocation();

        // Check restrictions if enabled
        if (plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior.respectRestrictions) {
            boolean locationRestricted = plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior.checkTargetLocation &&
                    isPartyRespawnDisabled(currentLocation);

            if (locationRestricted) {
                String behavior = plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior.restrictedAreaBehavior;

                switch (behavior.toLowerCase()) {
                    case "allow":
                        break; // Continue with walking spawn point
                    case "fallback_to_party":
                        return null; // Fall back to normal party join logic
                    case "fallback_to_normal_spawn":
                        return FALLBACK_TO_NORMAL_SPAWN_MARKER;
                    case "deny":
                    default:
                        String restrictedMessage = plugin.getConfigManager().getMessagesConfig().party.walkingSpawnPointRestricted;
                        if (!restrictedMessage.isEmpty()) {
                            plugin.getMessageManager().sendMessage(player, restrictedMessage);
                        }
                        return null;
                }
            }
        }

        // Send walking spawn point message
        String message = plugin.getConfigManager().getMessagesConfig().party.walkingSpawnPointMessage;
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, message);
        }

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Player " + player.getName() + " using walking spawn point for join at current location");
        }

        return currentLocation;
    }

    private void sendWalkingSpawnPointMessage(Player player) {
        String message = plugin.getConfigManager().getMessagesConfig().party.walkingSpawnPointMessage;
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, message);
        }

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Player " + player.getName() + " respawning at death location (walking spawn point)");
        }
    }

    // ============================= TARGET FINDING LOGIC =============================

    private Player findBestTarget(Party party, Player excludePlayer, Location deathLocation) {
        // Try specific target first if set
        if (party.getRespawnTarget() != null) {
            Player specificTarget = party.getRespawnTargetPlayer();
            if (specificTarget != null && specificTarget.isOnline() && !specificTarget.equals(excludePlayer)) {
                return specificTarget;
            }
        }

        // Get all online party members except the dying player
        List<Player> candidates = party.getOnlineMembers();
        candidates.remove(excludePlayer);

        if (candidates.isEmpty()) {
            return null;
        }

        // Apply primary strategy
        Player target = selectTargetByStrategy(candidates, deathLocation,
                plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.primaryStrategy);

        // If primary strategy failed, try fallback
        if (target == null) {
            target = selectTargetByStrategy(candidates, deathLocation,
                    plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.fallbackStrategy);
        }

        // If still no target, just return first available
        return target != null ? target : candidates.get(0);
    }

    private Player findBestTargetForJoin(Party party, Player excludePlayer) {
        List<Player> candidates = party.getOnlineMembers();
        candidates.remove(excludePlayer);

        if (candidates.isEmpty()) {
            return null;
        }

        // For joins, we can use the current player location as reference
        Location referenceLocation = excludePlayer.getLocation();

        // Apply primary strategy
        Player target = selectTargetByStrategyForJoin(candidates, referenceLocation,
                plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.primaryStrategy);

        // If primary strategy failed, try fallback
        if (target == null) {
            target = selectTargetByStrategyForJoin(candidates, referenceLocation,
                    plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.fallbackStrategy);
        }

        return target != null ? target : candidates.get(0);
    }

    private Player selectTargetByStrategy(List<Player> candidates, Location deathLocation, String strategy) {
        if (candidates.isEmpty()) {
            return null;
        }

        return switch (strategy.toLowerCase()) {
            case "closest_same_world" -> findClosestInSameWorld(candidates, deathLocation);
            case "closest_any_world" -> findClosestAnyWorld(candidates, deathLocation);
            case "most_members_world" -> findTargetInMostPopulatedWorld(candidates);
            case "most_members_region" -> findTargetInMostPopulatedRegion(candidates);
            case "random" -> candidates.get(this.random.nextInt(candidates.size()));
            case "leader_priority" -> findWithLeaderPriority(candidates);
            case "specific_target_only" ->
                // Only use specifically set target, no fallback
                    null;
            default -> findClosestInSameWorld(candidates, deathLocation);
        };
    }

    private Player selectTargetByStrategyForJoin(List<Player> candidates, Location referenceLocation, String strategy) {
        return switch (strategy.toLowerCase()) {
            case "closest_same_world" -> findClosestInSameWorldForJoin(candidates, referenceLocation);
            case "closest_any_world" -> findClosestAnyWorldForJoin(candidates, referenceLocation);
            case "most_members_world" -> findTargetInMostPopulatedWorld(candidates);
            case "most_members_region" -> findTargetInMostPopulatedRegion(candidates);
            case "random" -> candidates.get(this.random.nextInt(candidates.size()));
            case "leader_priority" -> findWithLeaderPriority(candidates);
            case "specific_target_only" -> null; // Only use specifically set target
            default -> findClosestInSameWorldForJoin(candidates, referenceLocation);
        };
    }

    private Player findClosestInSameWorld(List<Player> candidates, Location deathLocation) {
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player candidate : candidates) {
            if (candidate.getWorld().equals(deathLocation.getWorld())) {
                double dist = candidate.getLocation().distance(deathLocation);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = candidate;
                }
            }
        }

        return closest;
    }

    private Player findClosestAnyWorld(List<Player> candidates, Location deathLocation) {
        if (plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.preferSameWorld) {
            Player sameWorldTarget = findClosestInSameWorld(candidates, deathLocation);
            if (sameWorldTarget != null) {
                return sameWorldTarget;
            }
        }

        // If no same-world target or not preferring same world, find closest regardless of world
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player candidate : candidates) {
            if (candidate.getWorld().equals(deathLocation.getWorld())) {
                double dist = candidate.getLocation().distance(deathLocation);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = candidate;
                }
            }
        }

        // If no same-world players, just return first from different world
        if (closest == null) {
            for (Player candidate : candidates) {
                if (!candidate.getWorld().equals(deathLocation.getWorld())) {
                    return candidate;
                }
            }
        }

        return closest;
    }

    private Player findClosestInSameWorldForJoin(List<Player> candidates, Location referenceLocation) {
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player candidate : candidates) {
            if (candidate.getWorld().equals(referenceLocation.getWorld())) {
                double dist = candidate.getLocation().distance(referenceLocation);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = candidate;
                }
            }
        }

        return closest;
    }

    private Player findClosestAnyWorldForJoin(List<Player> candidates, Location referenceLocation) {
        if (plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.preferSameWorld) {
            Player sameWorldTarget = findClosestInSameWorldForJoin(candidates, referenceLocation);
            if (sameWorldTarget != null) {
                return sameWorldTarget;
            }
        }

        // Find closest in same world first
        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player candidate : candidates) {
            if (candidate.getWorld().equals(referenceLocation.getWorld())) {
                double dist = candidate.getLocation().distance(referenceLocation);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = candidate;
                }
            }
        }

        // If no same-world players, return first from different world
        if (closest == null) {
            for (Player candidate : candidates) {
                if (!candidate.getWorld().equals(referenceLocation.getWorld())) {
                    return candidate;
                }
            }
        }

        return closest;
    }

    private Player findTargetInMostPopulatedWorld(List<Player> candidates) {
        Map<String, List<Player>> worldGroups = new HashMap<>();

        // Group candidates by world
        for (Player candidate : candidates) {
            String worldName = candidate.getWorld().getName();
            worldGroups.computeIfAbsent(worldName, k -> new ArrayList<>()).add(candidate);
        }

        // Find world with most party members
        String bestWorld = null;
        int maxMembers = 0;

        for (Map.Entry<String, List<Player>> entry : worldGroups.entrySet()) {
            int memberCount = entry.getValue().size();
            if (memberCount > maxMembers && memberCount >= plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.minPopulationThreshold) {
                maxMembers = memberCount;
                bestWorld = entry.getKey();
            }
        }

        if (bestWorld != null) {
            List<Player> worldCandidates = worldGroups.get(bestWorld);
            return worldCandidates.get(this.random.nextInt(worldCandidates.size()));
        }

        return null;
    }

    private Player findTargetInMostPopulatedRegion(List<Player> candidates) {
        if (!plugin.isWorldGuardEnabled()) {
            // Fallback to world population if WorldGuard not available
            return findTargetInMostPopulatedWorld(candidates);
        }

        Map<String, List<Player>> regionGroups = new HashMap<>();

        // Group candidates by their current region
        for (Player candidate : candidates) {
            Set<String> regions = WorldGuardUtils.getRegionsAt(candidate.getLocation());
            String regionKey = regions.isEmpty() ? "wilderness" : String.join(",", regions);
            regionGroups.computeIfAbsent(regionKey, k -> new ArrayList<>()).add(candidate);
        }

        // Find region with most party members
        String bestRegion = null;
        int maxMembers = 0;

        for (Map.Entry<String, List<Player>> entry : regionGroups.entrySet()) {
            int memberCount = entry.getValue().size();
            if (memberCount > maxMembers && memberCount >= plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.minPopulationThreshold) {
                maxMembers = memberCount;
                bestRegion = entry.getKey();
            }
        }

        if (bestRegion != null) {
            List<Player> regionCandidates = regionGroups.get(bestRegion);
            return regionCandidates.get(this.random.nextInt(regionCandidates.size()));
        }

        return null;
    }

    private Player findWithLeaderPriority(List<Player> candidates) {
        if (plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.preferLeader) {
            // Find the party leader among candidates
            for (Player candidate : candidates) {
                Party party = getPlayerParty(candidate.getUniqueId());
                if (party != null && party.isLeader(candidate.getUniqueId())) {
                    return candidate;
                }
            }
        }

        // If leader not found or not preferring leader, return random
        return candidates.get(this.random.nextInt(candidates.size()));
    }

    // ============================= RESTRICTION HANDLING =============================

    private Location handleBothRestricted(Player player) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.bothRestrictedBehavior;

        switch (behavior.toLowerCase()) {
            case "allow":
                // Force allow anyway (admin choice)
                Party party = getPlayerParty(player.getUniqueId());
                Player target = findBestTarget(party, player, player.getLocation());
                return target != null ? target.getLocation() : null;

            case "fallback_to_normal_spawn":
                // Let normal spawn system handle it
                return null;

            case "deny":
            default:
                String message = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
                if (!message.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, message);
                }
                return null;
        }
    }

    private Location handleDeathRestricted(Player player, Player target) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.deathRestrictedBehavior;

        switch (behavior.toLowerCase()) {
            case "deny":
                String message = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
                if (!message.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, message);
                }
                return null;

            case "fallback_to_normal_spawn":
                return null;

            case "allow":
            default:
                return target.getLocation();
        }
    }

    private Location handleTargetRestricted(Player player, Party party) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetRestrictedBehavior;

        switch (behavior.toLowerCase()) {
            case "allow":
                Player target = findBestTarget(party, player, player.getLocation());
                return target != null ? target.getLocation() : null;

            case "find_other_member":
                if (plugin.getConfigManager().getMainConfig().party.respawnBehavior.findAlternativeTarget) {
                    return findAlternativeTarget(player, party);
                }
                return null;

            case "deny":
            default:
                String message = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
                if (!message.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, message);
                }
                return null;
        }
    }

    private Location handleJoinTargetRestricted(Player player, Party party) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetRestrictedBehavior;

        switch (behavior.toLowerCase()) {
            case "allow":
                Player target = findBestTargetForJoin(party, player);
                return target != null ? target.getLocation() : null;

            case "find_other_member":
                if (plugin.getConfigManager().getMainConfig().party.respawnBehavior.findAlternativeTarget) {
                    return findAlternativeTargetForJoin(player, party);
                }
                return null;

            case "deny":
            default:
                String message = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
                if (!message.isEmpty()) {
                    plugin.getMessageManager().sendMessage(player, message);
                }
                return null;
        }
    }

    private Location findAlternativeTarget(Player player, Party party) {
        List<Player> onlineMembers = party.getOnlineMembers();
        onlineMembers.remove(player);

        int attempts = plugin.getConfigManager().getMainConfig().party.respawnBehavior.alternativeTargetAttempts;

        for (int i = 0; i < Math.min(attempts, onlineMembers.size()); i++) {
            Player member = onlineMembers.get(i);
            if (!isPartyRespawnDisabled(member.getLocation())) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Found alternative party target: " + member.getName());
                }
                return member.getLocation();
            }
        }

        String message = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, message);
        }
        return null;
    }

    private Location findAlternativeTargetForJoin(Player player, Party party) {
        List<Player> onlineMembers = party.getOnlineMembers();
        onlineMembers.remove(player);

        int attempts = plugin.getConfigManager().getMainConfig().party.respawnBehavior.alternativeTargetAttempts;

        for (int i = 0; i < Math.min(attempts, onlineMembers.size()); i++) {
            Player member = onlineMembers.get(i);
            if (!isPartyRespawnDisabled(member.getLocation())) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Found alternative party target for join: " + member.getName());
                }
                return member.getLocation();
            }
        }

        String message = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
        if (!message.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, message);
        }
        return null;
    }

    // Check if party respawn is disabled in a location (region or world)
    private boolean isPartyRespawnDisabled(Location location) {
        // Get all spawn entries for deaths event type
        List<SpawnEntry> deathEntries = plugin.getConfigManager().getSpawnEntriesForEvent("deaths");

        for (SpawnEntry entry : deathEntries) {
            // Check if this entry matches the location
            if (!entry.matchesLocation(location)) {
                continue;
            }

            // Check if party respawn is disabled for this entry
            Object spawnData = entry.spawnData();
            boolean partyDisabled = false;

            if (spawnData instanceof RegionSpawnsConfig.RegionSpawnEntry regionEntry) {
                partyDisabled = regionEntry.partyRespawnDisabled;
            } else if (spawnData instanceof WorldSpawnsConfig.WorldSpawnEntry worldEntry) {
                partyDisabled = worldEntry.partyRespawnDisabled;
            } else if (spawnData instanceof CoordinateSpawnsConfig.CoordinateSpawnEntry coordEntry) {
                partyDisabled = coordEntry.partyRespawnDisabled;
            }

            if (partyDisabled) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Party respawn disabled for location due to spawn entry from " + entry.fileName());
                }
                return true;
            }
        }

        return false;
    }
}