package uz.alex2276564.mmospawnpoint.party;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.manager.SpawnEntry;
import uz.alex2276564.mmospawnpoint.utils.WorldGuardUtils;

import java.util.*;

public class PartyManager {

    public static final Location FALLBACK_TO_NORMAL_SPAWN_MARKER = new Location(null, 0, 0, 0);

    private enum RestrictionReason {NONE, WORLD, REGION_OR_COORDINATE}

    public enum InviteResult {
        SUCCESS,
        LEADER_NOT_IN_PARTY,
        NOT_LEADER,
        TARGET_ALREADY_IN_PARTY,
        PARTY_FULL,
        ALREADY_INVITED
    }

    private final MMOSpawnPoint plugin;
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

    public PartyManager(MMOSpawnPoint plugin) {
        this.plugin = plugin;
        this.maxPartySize = plugin.getConfigManager().getMainConfig().party.maxSize;
        this.invitationExpiryTime = plugin.getConfigManager().getMainConfig().party.invitationExpiry;
        this.maxRespawnDistance = plugin.getConfigManager().getMainConfig().party.maxRespawnDistance;
        this.respawnCooldown = plugin.getConfigManager().getMainConfig().party.respawnCooldown;
        startCleanupTask();
    }

    private void startCleanupTask() {
        int partyPeriod = plugin.getConfigManager().getMainConfig().settings.maintenance.partyCleanupPeriodTicks;
        int invitePeriod = plugin.getConfigManager().getMainConfig().settings.maintenance.invitationCleanupPeriodTicks;

        plugin.getRunner().runPeriodical(this::cleanupParties, partyPeriod, partyPeriod);
        plugin.getRunner().runPeriodical(this::cleanupInvitations, invitePeriod, invitePeriod);
    }

    public void shutdown() {
        parties.clear();
        playerPartyMap.clear();
        pendingInvitations.clear();
    }

    private void cleanupParties() {
        // Remove empty parties
        parties.entrySet().removeIf(e -> e.getValue().isEmpty());
        // Rebuild player->party map
        playerPartyMap.clear();
        for (Party p : parties.values()) {
            for (UUID memberId : p.getMembers()) {
                playerPartyMap.put(memberId, p.getId());
            }
        }
    }

    private void cleanupInvitations() {
        // 1) Let each party clean its own expired invitations
        for (Party p : parties.values()) {
            p.cleanExpiredInvitations();
        }
        // 2) Sync pendingInvitations map and notify leader
        Iterator<Map.Entry<UUID, UUID>> it = pendingInvitations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, UUID> e = it.next();
            Party party = parties.get(e.getValue());
            if (party == null || party.hasNoInvitation(e.getKey())) {
                it.remove();
                if (party != null) {
                    Player leader = party.getLeaderPlayer();
                    if (leader != null && leader.isOnline()) {
                        String msg = plugin.getConfigManager().getMessagesConfig().party.inviteExpired;
                        if (msg != null && !msg.isEmpty()) {
                            plugin.getMessageManager().sendMessage(leader, msg);
                        }
                    }
                }
            }
        }
    }

    public void cleanupPlayerData(UUID playerId) {
        try {
            if (isInParty(playerId)) {
                Party party = getPlayerParty(playerId);
                if (party != null) {
                    party.removeMember(playerId);
                    playerPartyMap.remove(playerId);
                    if (party.isEmpty()) {
                        parties.remove(party.getId());
                    }
                }
            }
            pendingInvitations.remove(playerId);
            for (Party p : parties.values()) {
                p.getInvitations().remove(playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up party data for " + playerId + ": " + e.getMessage());
        }
    }

    // ============================= PARTY MANAGEMENT =============================

    public void createParty(Player leader) {
        UUID leaderId = leader.getUniqueId();
        if (isInParty(leaderId)) return;
        Party party = new Party(leaderId);
        parties.put(party.getId(), party);
        playerPartyMap.put(leaderId, party.getId());
    }

    public InviteResult invitePlayer(Player leader, Player invited) {
        UUID leaderId = leader.getUniqueId();
        UUID invitedId = invited.getUniqueId();

        // Leader must be in a party
        if (!isInParty(leaderId)) {
            return InviteResult.LEADER_NOT_IN_PARTY;
        }

        Party party = getPlayerParty(leaderId);

        // Only leader can invite
        if (!party.isLeader(leaderId)) {
            return InviteResult.NOT_LEADER;
        }

        // Target already in any party
        if (isInParty(invitedId)) {
            return InviteResult.TARGET_ALREADY_IN_PARTY;
        }

        // Party full
        if (maxPartySize > 0 && party.size() >= maxPartySize) {
            return InviteResult.PARTY_FULL;
        }

        // Already invited to THIS party
        UUID pending = pendingInvitations.get(invitedId);
        if (pending != null && pending.equals(party.getId()) && !party.hasNoInvitation(invitedId)) {
            // refresh/extend expiration
            party.invite(invitedId, invitationExpiryTime);
            return InviteResult.ALREADY_INVITED;
        }

        // Issue a new invitation
        party.invite(invitedId, invitationExpiryTime);
        pendingInvitations.put(invitedId, party.getId());
        return InviteResult.SUCCESS;
    }

    public boolean acceptInvitation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!pendingInvitations.containsKey(playerId)) return false;

        UUID partyId = pendingInvitations.get(playerId);
        Party party = getParty(partyId);
        if (party == null || party.hasNoInvitation(playerId)) {
            pendingInvitations.remove(playerId);
            return false;
        }
        party.addMember(playerId);
        playerPartyMap.put(playerId, partyId);
        pendingInvitations.remove(playerId);
        return true;
    }

    public boolean declineInvitation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!pendingInvitations.containsKey(playerId)) return false;

        UUID partyId = pendingInvitations.get(playerId);
        Party party = getParty(partyId);
        if (party != null) {
            party.getInvitations().remove(playerId);
        }
        pendingInvitations.remove(playerId);
        return true;
    }

    public boolean leaveParty(Player player) {
        UUID playerId = player.getUniqueId();
        if (!isInParty(playerId)) return false;

        Party party = getPlayerParty(playerId);
        party.removeMember(playerId);
        playerPartyMap.remove(playerId);

        // If disbanded
        if (party.isEmpty()) {
            parties.remove(party.getId());
            String msg = plugin.getConfigManager().getMessagesConfig().party.partyDisbanded;
            if (msg != null && !msg.isEmpty()) {
                plugin.getMessageManager().sendMessage(player, msg);
            }
        }
        return true;
    }

    public boolean removePlayer(Player leader, Player target) {
        UUID leaderId = leader.getUniqueId();
        UUID targetId = target.getUniqueId();

        if (!isInParty(leaderId)) return false;
        Party party = getPlayerParty(leaderId);
        if (!party.isLeader(leaderId)) return false;
        if (party.isNotMember(targetId)) return false;

        party.removeMember(targetId);
        playerPartyMap.remove(targetId);

        if (party.isEmpty()) {
            parties.remove(party.getId());
            String msg = plugin.getConfigManager().getMessagesConfig().party.partyDisbanded;
            if (msg != null && !msg.isEmpty() && leader.isOnline()) {
                plugin.getMessageManager().sendMessage(leader, msg);
            }
        }
        return true;
    }

    public boolean setLeader(Player currentLeader, Player newLeader) {
        UUID currentLeaderId = currentLeader.getUniqueId();
        UUID newLeaderId = newLeader.getUniqueId();

        if (!isInParty(currentLeaderId)) return false;
        Party party = getPlayerParty(currentLeaderId);
        if (!party.isLeader(currentLeaderId)) return false;
        if (party.isNotMember(newLeaderId)) return false;

        party.setLeader(newLeaderId);
        // Commands already broadcast newLeaderAssigned — avoid duplicate here
        return true;
    }

    public void setRespawnMode(Player leader, Party.RespawnMode mode) {
        UUID leaderId = leader.getUniqueId();
        if (!isInParty(leaderId)) return;
        Party party = getPlayerParty(leaderId);
        if (!party.isLeader(leaderId)) return;
        party.setRespawnMode(mode);
    }

    public boolean setRespawnTarget(Player leader, Player target) {
        UUID leaderId = leader.getUniqueId();
        UUID targetId = target.getUniqueId();
        if (!isInParty(leaderId)) return false;
        Party party = getPlayerParty(leaderId);
        if (!party.isLeader(leaderId)) return false;
        if (party.isNotMember(targetId)) return false;
        party.setRespawnTarget(targetId);
        return true;
    }

    public boolean isInParty(UUID playerId) {
        return playerPartyMap.containsKey(playerId);
    }

    public Party getPlayerParty(UUID playerId) {
        UUID partyId = playerPartyMap.get(playerId);
        return parties.get(partyId);
    }

    public Party getParty(UUID partyId) {
        return parties.get(partyId);
    }

    public UUID getPendingInvitation(UUID playerId) {
        return pendingInvitations.get(playerId);
    }

    // ============================= RESPAWN LOGIC =============================

    public Location findPartyRespawnLocation(Player player, Location deathLocation) {
        UUID playerId = player.getUniqueId();
        if (!isInParty(playerId)) return null;

        Party party = getPlayerParty(playerId);
        if (party.getRespawnMode() != Party.RespawnMode.PARTY_MEMBER) return null;

        // Bypass config/permissions (cooldown, restrictions)
        var bypass = plugin.getConfigManager().getMainConfig().settings.permissions.bypass.party;
        boolean bypassCooldown = bypass.cooldownEnabled && player.hasPermission(bypass.cooldownNode);
        boolean bypassDeathRestrictions = bypass.restrictions.deathEnabled && player.hasPermission(bypass.restrictions.deathNode);
        boolean bypassTargetRestrictions = bypass.restrictions.targetEnabled && player.hasPermission(bypass.restrictions.targetNode);
        boolean bypassBothRestrictions = bypass.restrictions.bothEnabled && player.hasPermission(bypass.restrictions.bothNode);

        // Cooldown
        if (respawnCooldown > 0 && !bypassCooldown && party.isOnRespawnCooldown(playerId)) {
            long remaining = party.getRemainingCooldown(playerId);
            String msg = plugin.getConfigManager().getMessagesConfig().party.respawnCooldown;
            if (msg != null && !msg.isEmpty()) {
                plugin.getMessageManager().sendMessage(player, msg.replace("{time}", String.valueOf(remaining)));
            }
            return null;
        }

        // Walking spawn point (death location)
        if (plugin.getConfigManager().getMainConfig().party.respawnAtDeath.enabled &&
                player.hasPermission(plugin.getConfigManager().getMainConfig().party.respawnAtDeath.permission)) {
            Location walk = handleWalkingSpawnPoint(player, deathLocation);
            if (walk != null) {
                return (walk == FALLBACK_TO_NORMAL_SPAWN_MARKER) ? null : walk;
            }
        }

        RestrictionReason deathReason = plugin.getConfigManager().getMainConfig().party.respawnBehavior.checkDeathLocation
                ? getRestrictionReason(deathLocation)
                : RestrictionReason.NONE;

        Player target = findBestTargetOverlayed(party, player, deathLocation, false);
        if (target == null) return null;

        Location targetLocation = target.getLocation();
        RestrictionReason targetReason = plugin.getConfigManager().getMainConfig().party.respawnBehavior.checkTargetLocation
                ? getRestrictionReason(targetLocation)
                : RestrictionReason.NONE;

        // Apply bypasses to restrictions
        if (bypassBothRestrictions && deathReason != RestrictionReason.NONE && targetReason != RestrictionReason.NONE) {
            deathReason = RestrictionReason.NONE;
            targetReason = RestrictionReason.NONE;
        } else {
            if (bypassDeathRestrictions) deathReason = RestrictionReason.NONE;
            if (bypassTargetRestrictions) targetReason = RestrictionReason.NONE;
        }

        boolean deathRestricted = deathReason != RestrictionReason.NONE;
        boolean targetRestricted = targetReason != RestrictionReason.NONE;

        if (deathRestricted && targetRestricted) {
            return handleBothRestricted(player, deathReason);
        } else if (deathRestricted) {
            return handleDeathRestricted(player, target, deathReason);
        } else if (targetRestricted) {
            return handleTargetRestricted(player, party, targetReason);
        }

       // Max distance restriction
        if (maxRespawnDistance > 0
                && target.getWorld().equals(deathLocation.getWorld())
                && target.getLocation().distance(deathLocation) > maxRespawnDistance) {
            String msg = plugin.getConfigManager().getMessagesConfig().party.respawnTooFar;
            if (msg != null && !msg.isEmpty()) plugin.getMessageManager().sendMessage(player, msg);
            return null;
        }

        // Set cooldown only if not bypassed
        if (respawnCooldown > 0 && !bypassCooldown) {
            party.setRespawnCooldown(playerId, respawnCooldown);
        }

        String respawnMsg = plugin.getConfigManager().getMessagesConfig().party.respawnedAtMember;
        if (respawnMsg != null && !respawnMsg.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, respawnMsg.replace("{player}", target.getName()));
        }

        return targetLocation;
    }

    public Location findPartyJoinLocation(Player player) {
        UUID playerId = player.getUniqueId();
        if (!isInParty(playerId)) return null;

        Party party = getPlayerParty(playerId);
        if (party.getRespawnMode() != Party.RespawnMode.PARTY_MEMBER) return null;

        String partyScope = plugin.getConfigManager().getMainConfig().party.scope;
        if (!"joins".equals(partyScope) && !"both".equals(partyScope)) {
            return null;
        }

        // Walking spawn at join (current location)
        if (plugin.getConfigManager().getMainConfig().party.respawnAtDeath.enabled &&
                player.hasPermission(plugin.getConfigManager().getMainConfig().party.respawnAtDeath.permission)) {
            Location loc = handleWalkingSpawnPointForJoin(player);
            if (loc != null) {
                return loc;
            }
        }

        Player target = findBestTargetOverlayed(party, player, player.getLocation(), true);
        if (target == null) return null;

        Location targetLoc = target.getLocation();
        RestrictionReason targetReason = plugin.getConfigManager().getMainConfig().party.respawnBehavior.checkTargetLocation
                ? getRestrictionReason(targetLoc)
                : RestrictionReason.NONE;

        if (targetReason != RestrictionReason.NONE) {
            return handleJoinTargetRestricted(player, party, targetReason);
        }

        String msg = plugin.getConfigManager().getMessagesConfig().party.respawnedAtMember;
        if (msg != null && !msg.isEmpty())
            plugin.getMessageManager().sendMessage(player, msg.replace("{player}", target.getName()));

        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Player " + player.getName() + " joining at party member " + target.getName());
        }

        return targetLoc;
    }

    // ============================= WALKING SPAWN POINT =============================

    private Location handleWalkingSpawnPoint(Player player, Location deathLocation) {
        var cfg = plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior;

        // Bypass walking restrictions (ignore all restrictions for walking spawn)
        var bypass = plugin.getConfigManager().getMainConfig().settings.permissions.bypass.party;
        if (bypass.walking.restrictionsEnabled
                && player.hasPermission(bypass.walking.restrictionsNode)) {
            sendWalkingMessage(player);
            return deathLocation;
        }

        if (!cfg.respectRestrictions) {
            sendWalkingMessage(player);
            return deathLocation;
        }

        boolean deathRestricted = cfg.checkDeathLocation && getRestrictionReason(deathLocation) != RestrictionReason.NONE;
        boolean targetRestricted = false;
        if (cfg.checkTargetLocation) {
            Party party = getPlayerParty(player.getUniqueId());
            if (party != null) {
                Player target = findBestTargetOverlayed(party, player, deathLocation, false);
                if (target != null) {
                    targetRestricted = getRestrictionReason(target.getLocation()) != RestrictionReason.NONE;
                }
            }
        }

        if (!deathRestricted && !targetRestricted) {
            sendWalkingMessage(player);
            return deathLocation;
        }

        String behavior = cfg.restrictedAreaBehavior.toLowerCase(Locale.ROOT);
        switch (behavior) {
            case "allow":
                sendWalkingMessage(player);
                return deathLocation;
            case "fallback_to_party":
                return null; // Let party logic handle
            case "fallback_to_normal_spawn":
                return FALLBACK_TO_NORMAL_SPAWN_MARKER;
            default:
                String restrictedMsg = plugin.getConfigManager().getMessagesConfig().party.walkingSpawnPointRestricted;
                if (restrictedMsg != null && !restrictedMsg.isEmpty())
                    plugin.getMessageManager().sendMessage(player, restrictedMsg);
                return null;
        }
    }

    private Location handleWalkingSpawnPointForJoin(Player player) {
        Location current = player.getLocation();
        var cfg = plugin.getConfigManager().getMainConfig().party.respawnAtDeath.restrictionBehavior;

        if (cfg.respectRestrictions) {
            boolean locationRestricted = cfg.checkTargetLocation && getRestrictionReason(current) != RestrictionReason.NONE;
            if (locationRestricted) {
                String behavior = cfg.restrictedAreaBehavior.toLowerCase(Locale.ROOT);
                switch (behavior) {
                    case "allow":
                        break;
                    case "fallback_to_party":
                        return null;
                    case "fallback_to_normal_spawn":
                        return FALLBACK_TO_NORMAL_SPAWN_MARKER;
                    default:
                        String restrictedMsg = plugin.getConfigManager().getMessagesConfig().party.walkingSpawnPointRestricted;
                        if (restrictedMsg != null && !restrictedMsg.isEmpty())
                            plugin.getMessageManager().sendMessage(player, restrictedMsg);
                        return null;
                }
            }
        }
        sendWalkingMessage(player);
        if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
            plugin.getLogger().info("Player " + player.getName() + " using walking spawn point for join at current location");
        }
        return current;
    }

    private void sendWalkingMessage(Player player) {
        String msg = plugin.getConfigManager().getMessagesConfig().party.walkingSpawnPointMessage;
        if (msg != null && !msg.isEmpty())
            plugin.getMessageManager().sendMessage(player, msg);
    }

    // ============================= TARGET SELECTION =============================

    private Player findBestTargetOverlayed(Party party, Player exclude, Location ref, boolean joinMode) {
        var sel = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection;

        if (sel.considerWorldPopulation) {
            Player p = findTargetInMostPopulatedWorld(party, exclude);
            if (p != null) return p;
        }
        if (sel.considerRegionPopulation) {
            Player p = findTargetInMostPopulatedRegion(party, exclude);
            if (p != null) return p;
        }

        Player target = joinMode
                ? selectTargetByStrategyForJoin(party, exclude, ref, sel.primaryStrategy)
                : selectTargetByStrategy(party, exclude, ref, sel.primaryStrategy);
        if (target != null) return target;

        return joinMode
                ? selectTargetByStrategyForJoin(party, exclude, ref, sel.fallbackStrategy)
                : selectTargetByStrategy(party, exclude, ref, sel.fallbackStrategy);
    }

    private Player selectTargetByStrategy(Party party, Player excludePlayer, Location deathLocation, String strategy) {
        List<Player> candidates = party.getOnlineMembers().stream()
                .filter(p -> !p.equals(excludePlayer))
                .toList();
        if (candidates.isEmpty()) return null;

        switch (strategy.toLowerCase(Locale.ROOT)) {
            case "closest_same_world":
                return findClosestInSameWorld(candidates, deathLocation);
            case "any_world":
                return findClosestAnyWorld(candidates, deathLocation);
            case "most_members_world":
                return findTargetInMostPopulatedWorld(candidates);
            case "most_members_region":
                return findTargetInMostPopulatedRegion(candidates);
            case "random":
                return candidates.get(this.random.nextInt(candidates.size()));
            case "leader_priority":
                return findWithLeaderPriority(candidates);
            case "specific_target_only":
                return null;
            default:
                return findClosestInSameWorld(candidates, deathLocation);
        }
    }

    private Player selectTargetByStrategyForJoin(Party party, Player excludePlayer, Location ref, String strategy) {
        List<Player> candidates = party.getOnlineMembers().stream()
                .filter(p -> !p.equals(excludePlayer))
                .toList();
        if (candidates.isEmpty()) return null;

        switch (strategy.toLowerCase(Locale.ROOT)) {
            case "closest_same_world":
                return findClosestInSameWorldForJoin(candidates, ref);
            case "any_world":
                return findClosestAnyWorldForJoin(candidates, ref);
            case "most_members_world":
                return findTargetInMostPopulatedWorld(candidates);
            case "most_members_region":
                return findTargetInMostPopulatedRegion(candidates);
            case "random":
                return candidates.get(this.random.nextInt(candidates.size()));
            case "leader_priority":
                return findWithLeaderPriority(candidates);
            case "specific_target_only":
                return null;
            default:
                return findClosestInSameWorldForJoin(candidates, ref);
        }
    }

    private Player findClosestInSameWorld(List<Player> candidates, Location deathLocation) {
        Player closest = null;
        double dmin = Double.MAX_VALUE;
        for (Player c : candidates) {
            if (c.getWorld().equals(deathLocation.getWorld())) {
                double d = c.getLocation().distance(deathLocation);
                if (d < dmin) {
                    dmin = d;
                    closest = c;
                }
            }
        }
        return closest;
    }

    private Player findClosestAnyWorld(List<Player> candidates, Location deathLocation) {
        var sel = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection;
        if (sel.preferSameWorld) {
            Player p = findClosestInSameWorld(candidates, deathLocation);
            if (p != null) return p;
        }
        Player same = null;
        double dmin = Double.MAX_VALUE;
        for (Player c : candidates) {
            if (c.getWorld().equals(deathLocation.getWorld())) {
                double d = c.getLocation().distance(deathLocation);
                if (d < dmin) {
                    dmin = d;
                    same = c;
                }
            }
        }
        if (same == null && !candidates.isEmpty()) return candidates.get(0);
        return same;
    }

    private Player findClosestInSameWorldForJoin(List<Player> candidates, Location ref) {
        Player closest = null;
        double dmin = Double.MAX_VALUE;
        for (Player c : candidates) {
            if (c.getWorld().equals(ref.getWorld())) {
                double d = c.getLocation().distance(ref);
                if (d < dmin) {
                    dmin = d;
                    closest = c;
                }
            }
        }
        return closest;
    }

    private Player findClosestAnyWorldForJoin(List<Player> candidates, Location ref) {
        var sel = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection;
        if (sel.preferSameWorld) {
            Player p = findClosestInSameWorldForJoin(candidates, ref);
            if (p != null) return p;
        }
        Player same = null;
        double dmin = Double.MAX_VALUE;
        for (Player c : candidates) {
            if (c.getWorld().equals(ref.getWorld())) {
                double d = c.getLocation().distance(ref);
                if (d < dmin) {
                    dmin = d;
                    same = c;
                }
            }
        }
        if (same == null && !candidates.isEmpty()) return candidates.get(0);
        return same;
    }

    private Player findTargetInMostPopulatedWorld(Party party, Player exclude) {
        List<Player> candidates = new ArrayList<>(party.getOnlineMembers());
        candidates.remove(exclude);
        return findTargetInMostPopulatedWorld(candidates);
    }

    private Player findTargetInMostPopulatedWorld(List<Player> candidates) {
        if (candidates.isEmpty()) return null;
        Map<String, List<Player>> worldGroups = new HashMap<>();
        for (Player c : candidates) {
            String worldName = c.getWorld().getName();
            worldGroups.computeIfAbsent(worldName, k -> new ArrayList<>()).add(c);
        }
        int minPop = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.minPopulationThreshold;
        String bestWorld = null;
        int max = 0;
        for (var e : worldGroups.entrySet()) {
            int size = e.getValue().size();
            if (size > max && size >= minPop) {
                max = size;
                bestWorld = e.getKey();
            }
        }
        if (bestWorld != null) {
            List<Player> list = worldGroups.get(bestWorld);
            return list.get(this.random.nextInt(list.size()));
        }
        return null;
    }

    private Player findTargetInMostPopulatedRegion(Party party, Player exclude) {
        List<Player> candidates = new ArrayList<>(party.getOnlineMembers());
        candidates.remove(exclude);
        return findTargetInMostPopulatedRegion(candidates);
    }

    private Player findTargetInMostPopulatedRegion(List<Player> candidates) {
        if (!plugin.isWorldGuardEnabled()) {
            return findTargetInMostPopulatedWorld(candidates);
        }
        Map<String, List<Player>> regionGroups = new HashMap<>();
        for (Player c : candidates) {
            Set<String> regions = WorldGuardUtils.getRegionsAt(c.getLocation());
            String key;
            if (regions.isEmpty()) {
                key = "wilderness";
            } else {
                List<String> sorted = new ArrayList<>(regions);
                Collections.sort(sorted);
                key = String.join(",", sorted);
            }
            regionGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        int minPop = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection.minPopulationThreshold;
        String bestRegion = null;
        int max = 0;
        for (var e : regionGroups.entrySet()) {
            int size = e.getValue().size();
            if (size > max && size >= minPop) {
                max = size;
                bestRegion = e.getKey();
            }
        }
        if (bestRegion != null) {
            List<Player> list = regionGroups.get(bestRegion);
            return list.get(this.random.nextInt(list.size()));
        }
        return null;
    }

    private Player findWithLeaderPriority(List<Player> candidates) {
        var sel = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetSelection;
        if (sel.preferLeader) {
            for (Player c : candidates) {
                Party p = getPlayerParty(c.getUniqueId());
                if (p != null && p.isLeader(c.getUniqueId())) {
                    return c;
                }
            }
        }
        return candidates.get(this.random.nextInt(candidates.size()));
    }

    // ============================= RESTRICTIONS & REASONS =============================

    private RestrictionReason getRestrictionReason(Location location) {
        // Only consider 'deaths' entries for party respawn restrictions
        List<SpawnEntry> entries = plugin.getConfigManager().getSpawnEntriesForEvent("deaths");
        for (SpawnEntry e : entries) {
            if (!e.matchesLocation(location)) continue;

            var data = e.spawnData();
            if (data != null && data.partyRespawnDisabled) {
                return (e.type() == SpawnEntry.Type.WORLD)
                        ? RestrictionReason.WORLD
                        : RestrictionReason.REGION_OR_COORDINATE;
            }
        }
        return RestrictionReason.NONE;
    }

    private void sendRestrictedMessage(Player player, RestrictionReason reason) {
        String msg = (reason == RestrictionReason.WORLD)
                ? plugin.getConfigManager().getMessagesConfig().party.respawnDisabledWorld
                : plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
        if (msg != null && !msg.isEmpty()) plugin.getMessageManager().sendMessage(player, msg);
    }

    private Location handleBothRestricted(Player player, RestrictionReason reason) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.bothRestrictedBehavior.toLowerCase(Locale.ROOT);
        switch (behavior) {
            case "allow":
                Party party = getPlayerParty(player.getUniqueId());
                Player target = findBestTargetOverlayed(party, player, player.getLocation(), false);
                return target != null ? target.getLocation() : null;
            case "fallback_to_normal_spawn":
                return null;
            default:
                sendRestrictedMessage(player, reason);
                return null;
        }
    }

    private Location handleDeathRestricted(Player player, Player target, RestrictionReason deathReason) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.deathRestrictedBehavior.toLowerCase(Locale.ROOT);
        return switch (behavior) {
            case "deny" -> {
                sendRestrictedMessage(player, deathReason);
                yield null;
            }
            case "fallback_to_normal_spawn" -> null;
            default -> target.getLocation();
        };
    }

    private Location handleTargetRestricted(Player player, Party party, RestrictionReason targetReason) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetRestrictedBehavior.toLowerCase(Locale.ROOT);
        return switch (behavior) {
            case "allow" -> {
                Player t = findBestTargetOverlayed(party, player, player.getLocation(), false);
                yield t != null ? t.getLocation() : null;
            }
            case "find_other_member" -> {
                if (plugin.getConfigManager().getMainConfig().party.respawnBehavior.findAlternativeTarget) {
                    yield findAlternativeTarget(player, party);
                }
                yield null;
            }
            default -> {
                sendRestrictedMessage(player, targetReason);
                yield null;
            }
        };
    }

    private Location handleJoinTargetRestricted(Player player, Party party, RestrictionReason targetReason) {
        String behavior = plugin.getConfigManager().getMainConfig().party.respawnBehavior.targetRestrictedBehavior.toLowerCase(Locale.ROOT);
        return switch (behavior) {
            case "allow" -> {
                Player t = findBestTargetOverlayed(party, player, player.getLocation(), true);
                yield t != null ? t.getLocation() : null;
            }
            case "find_other_member" -> {
                if (plugin.getConfigManager().getMainConfig().party.respawnBehavior.findAlternativeTarget) {
                    yield findAlternativeTargetForJoin(player, party);
                }
                yield null;
            }
            default -> {
                sendRestrictedMessage(player, targetReason);
                yield null;
            }
        };
    }

    private Location findAlternativeTarget(Player player, Party party) {
        List<Player> list = new ArrayList<>(party.getOnlineMembers());
        list.remove(player);
        int attempts = plugin.getConfigManager().getMainConfig().party.respawnBehavior.alternativeTargetAttempts;
        for (int i = 0; i < Math.min(attempts, list.size()); i++) {
            Player m = list.get(i);
            if (getRestrictionReason(m.getLocation()) == RestrictionReason.NONE) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Found alternative party target: " + m.getName());
                }
                return m.getLocation();
            }
        }
        String msg = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
        if (msg != null && !msg.isEmpty()) plugin.getMessageManager().sendMessage(player, msg);
        return null;
    }

    private Location findAlternativeTargetForJoin(Player player, Party party) {
        List<Player> list = new ArrayList<>(party.getOnlineMembers());
        list.remove(player);
        int attempts = plugin.getConfigManager().getMainConfig().party.respawnBehavior.alternativeTargetAttempts;
        for (int i = 0; i < Math.min(attempts, list.size()); i++) {
            Player m = list.get(i);
            if (getRestrictionReason(m.getLocation()) == RestrictionReason.NONE) {
                if (plugin.getConfigManager().getMainConfig().settings.debugMode) {
                    plugin.getLogger().info("Found alternative party target for join: " + m.getName());
                }
                return m.getLocation();
            }
        }
        String msg = plugin.getConfigManager().getMessagesConfig().party.respawnDisabledRegion;
        if (msg != null && !msg.isEmpty()) plugin.getMessageManager().sendMessage(player, msg);
        return null;
    }
}