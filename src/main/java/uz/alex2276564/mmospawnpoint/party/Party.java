package uz.alex2276564.mmospawnpoint.party;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

@Data
public class Party {
    private UUID id;
    private UUID leader;
    private Set<UUID> members;
    private Map<UUID, Long> invitations;
    private RespawnMode respawnMode;
    private UUID respawnTarget;
    private Map<UUID, Long> respawnCooldowns;

    public enum RespawnMode {
        NORMAL, // Normal spawn point logic
        PARTY_MEMBER // Respawn near a party member
    }

    public Party(UUID leaderId) {
        this.id = UUID.randomUUID();
        this.leader = leaderId;
        this.members = new HashSet<>();
        this.members.add(leaderId);
        this.invitations = new HashMap<>();
        this.respawnMode = RespawnMode.NORMAL;
        this.respawnCooldowns = new HashMap<>();
    }

    public boolean isLeader(UUID playerId) {
        return leader.equals(playerId);
    }

    public boolean isNotMember(UUID playerId) {
        return !members.contains(playerId);
    }

    public boolean hasNoInvitation(UUID playerId) {
        return !invitations.containsKey(playerId);
    }

    public void invite(UUID playerId, long expiryTimeSeconds) {
        invitations.put(playerId, System.currentTimeMillis() + (expiryTimeSeconds * 1000));
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
        invitations.remove(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
        if (playerId.equals(leader) && !members.isEmpty()) {
            // Assign a new leader
            leader = members.iterator().next();
        }

        // If respawn target was this player, reset it
        if (playerId.equals(respawnTarget)) {
            respawnTarget = null;
        }
    }

    public void setLeader(UUID playerId) {
        if (members.contains(playerId)) {
            leader = playerId;
        }
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public void cleanExpiredInvitations() {
        long currentTime = System.currentTimeMillis();
        invitations.entrySet().removeIf(entry -> entry.getValue() < currentTime);
    }

    public int size() {
        return members.size();
    }

    public List<Player> getOnlineMembers() {
        List<Player> online = new ArrayList<>();
        for (UUID memberId : members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }

    public Player getLeaderPlayer() {
        return Bukkit.getPlayer(leader);
    }

    public Player getRespawnTargetPlayer() {
        if (respawnTarget == null) {
            return null;
        }
        return Bukkit.getPlayer(respawnTarget);
    }

    public void setRespawnCooldown(UUID playerId, long cooldownSeconds) {
        respawnCooldowns.put(playerId, System.currentTimeMillis() + (cooldownSeconds * 1000));
    }

    public boolean isOnRespawnCooldown(UUID playerId) {
        if (!respawnCooldowns.containsKey(playerId)) {
            return false;
        }

        long cooldownEnd = respawnCooldowns.get(playerId);
        return System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown(UUID playerId) {
        if (!respawnCooldowns.containsKey(playerId)) {
            return 0;
        }

        long cooldownEnd = respawnCooldowns.get(playerId);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining / 1000); // Convert to seconds
    }
}