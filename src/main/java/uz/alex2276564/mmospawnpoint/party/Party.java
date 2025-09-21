package uz.alex2276564.mmospawnpoint.party;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Party {
    private UUID id;
    private UUID leader;
    private LinkedHashSet<UUID> members; // Preserve join order for deterministic next-leader selection
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
        this.members = new LinkedHashSet<>();
        this.members.add(leaderId);
        this.invitations = new ConcurrentHashMap<>();
        this.respawnMode = RespawnMode.NORMAL;
        this.respawnCooldowns = new ConcurrentHashMap<>();
    }

    public boolean isLeader(UUID playerId) {
        return leader.equals(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean hasInvitation(UUID playerId) {
        return invitations.containsKey(playerId);
    }

    public void invite(UUID playerId, long expiryTimeSeconds) {
        invitations.put(playerId, System.currentTimeMillis() + (expiryTimeSeconds * 1000));
    }

    public void addMember(UUID playerId) {
        // LinkedHashSet preserves insertion order; adding existing does nothing
        this.members.add(playerId);
        this.invitations.remove(playerId);
    }

    public void removeMember(UUID playerId) {
        boolean wasLeader = playerId.equals(this.leader);
        this.members.remove(playerId);

        // Reset target if it was this player
        if (playerId.equals(this.respawnTarget)) {
            this.respawnTarget = null;
        }

        if (wasLeader) {
            pickNewLeaderAfterRemoval();
        }
    }

    private void pickNewLeaderAfterRemoval() {
        // Deterministic: the oldest remaining member becomes leader
        if (!this.members.isEmpty()) {
            this.leader = this.members.iterator().next();
        }
    }

    public void setLeader(UUID playerId) {
        if (this.members.contains(playerId)) {
            this.leader = playerId;
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