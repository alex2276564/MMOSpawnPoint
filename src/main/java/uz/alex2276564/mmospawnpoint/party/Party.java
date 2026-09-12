package uz.alex2276564.mmospawnpoint.party;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Party model.
 * <p>
 * Note:
 * - We use Lombok @Data for boilerplate (equals/hashCode/toString + basic accessors).
 * - For mutable collections (members/invitations/respawnCooldowns) we override
 * the default Lombok behavior:
 * - members: custom getter returns an unmodifiable view.
 * - invitations/respawnCooldowns: no public getters; access only via methods
 * like hasInvitation(), invite(), removeInvitation(), setRespawnCooldown(), etc.
 * This keeps internal representation encapsulated and avoids CodeQL warnings
 * about exposing mutable fields.
 */
@Data
public class Party {

    private final UUID id;
    private UUID leader;

    // Preserve join order for deterministic next-leader selection
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final LinkedHashSet<UUID> members;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<UUID, Long> invitations;

    private RespawnMode respawnMode;
    private UUID respawnTarget;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<UUID, Long> respawnCooldowns;

    public enum RespawnMode {
        NORMAL,      // Normal spawn point logic
        PARTY_MEMBER // Respawn near a party member
    }

    public Party(UUID leaderId) {
        this.id = UUID.randomUUID();
        this.leader = leaderId;
        this.members = new LinkedHashSet<>();
        this.members.add(leaderId);
        this.invitations = new ConcurrentHashMap<>();
        this.respawnCooldowns = new ConcurrentHashMap<>();
        this.respawnMode = RespawnMode.NORMAL;
    }

    /**
     * Immutable view of party members.
     * Overrides Lombok-generated getter to avoid exposing the mutable set.
     */
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
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
        invitations.put(playerId, System.currentTimeMillis() + (expiryTimeSeconds * 1000L));
    }

    public void addMember(UUID playerId) {
        // LinkedHashSet preserves insertion order; adding existing does nothing
        this.members.add(playerId);
        this.invitations.remove(playerId);
    }

    public void removeMember(UUID playerId) {
        boolean wasLeader = playerId.equals(this.leader);
        this.members.remove(playerId);

        this.invitations.remove(playerId);

        // Reset target if it was this player
        if (playerId.equals(this.respawnTarget)) {
            this.respawnTarget = null;
        }

        if (wasLeader) {
            pickNewLeaderAfterRemoval();
        }
    }

    private void pickNewLeaderAfterRemoval() {
        for (UUID memberId : this.members) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                this.leader = memberId;
                return;
            }
        }

        // Fallback: oldest remaining member, even if offline
        if (!this.members.isEmpty()) {
            this.leader = this.members.iterator().next();
        }
    }

    /**
     * Custom setter to enforce that leader is always a party member.
     * This overrides Lombok's default setter for 'leader'.
     */
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

    /**
     * Remove a single pending invitation for the given player, if present.
     */
    public void removeInvitation(UUID playerId) {
        invitations.remove(playerId);
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

    /**
     * Custom setter for respawnMode to avoid nulls.
     * Lombok will not generate another setter because this one exists.
     */
    public void setRespawnMode(RespawnMode respawnMode) {
        if (respawnMode == null) {
            throw new IllegalArgumentException("respawnMode cannot be null");
        }
        this.respawnMode = respawnMode;
    }

    public void setRespawnCooldown(UUID playerId, long cooldownSeconds) {
        respawnCooldowns.put(playerId, System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }

    public boolean isOnRespawnCooldown(UUID playerId) {
        Long end = respawnCooldowns.get(playerId);
        return end != null && System.currentTimeMillis() < end;
    }

    public long getRemainingCooldown(UUID playerId) {
        Long end = respawnCooldowns.get(playerId);
        if (end == null) {
            return 0L;
        }
        long remaining = end - System.currentTimeMillis();
        return Math.max(0L, remaining / 1000L); // Convert to seconds
    }

    public void clearRespawnCooldown(UUID playerId) {
        this.respawnCooldowns.remove(playerId);
    }
}