package uz.alex2276564.mmospawnpoint.commands.subcommands.party.accept;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.UUID;

public class AcceptSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("accept")
                .permission("mmospawnpoint.party.accept")
                .description("Accept a party invitation")
                .executor((sender, context) -> {
                    MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();

                    if (!(sender instanceof Player player)) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "party.onlyPlayers",
                                plugin.getConfigManager().getMessagesConfig().party.onlyPlayers);
                        return;
                    }

                    if (!plugin.getConfigManager().getMainConfig().party.enabled) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "party.systemDisabled",
                                plugin.getConfigManager().getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    PartyManager partyManager = plugin.getPartyManager();
                    UUID playerId = player.getUniqueId();

                    // Check if player has a pending invitation
                    UUID pendingInvitation = partyManager.getPendingInvitation(playerId);
                    if (pendingInvitation == null) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.noInvitations",
                                plugin.getConfigManager().getMessagesConfig().party.noInvitations);
                        return;
                    }

                    if (partyManager.isOnMembershipChangeCooldown(playerId)) {
                        long remaining = partyManager.getRemainingMembershipChangeCooldown(playerId);
                        String msg = plugin.getConfigManager().getMessagesConfig().party.membershipChangeCooldown;
                        plugin.getMessageManager().sendMessageKeyed(player,
                                "party.membershipChangeCooldown",
                                msg,
                                "time",
                                String.valueOf(remaining));
                        return;
                    }

                    // Accept invitation
                    boolean success = partyManager.acceptInvitation(player);

                    if (success) {
                        Party party = partyManager.getPlayerParty(playerId);
                        var messages = plugin.getConfigManager().getMessagesConfig().party;

                        // Notify other party members
                        String joinMessage = messages.playerJoinedParty;
                        for (Player member : party.getOnlineMembers()) {
                            if (!member.equals(player)) {
                                plugin.getMessageManager().sendMessageKeyed(member, "party.playerJoinedParty",
                                        joinMessage, "player", player.getName());
                            }
                        }

                        // Message to the player who joined
                        plugin.getMessageManager().sendMessageKeyed(player, "party.joinedParty",
                                messages.joinedParty);

                        // Show current party info to the player who joined
                        plugin.getMessageManager().sendMessageKeyed(player, "party.listHeader", messages.listHeader);

                        UUID leaderId = party.getLeader();
                        UUID targetId = party.getRespawnTarget();

                        var leaderOffline = Bukkit.getOfflinePlayer(leaderId);
                        String leaderName = leaderOffline.getName();

                        if (leaderName != null) {
                            if (leaderOffline.isOnline()) {
                                plugin.getMessageManager().sendMessageKeyed(player, "party.listLeader",
                                        messages.listLeader, "player", leaderName);
                            } else {
                                plugin.getMessageManager().sendMessageKeyed(player, "party.listLeaderOffline",
                                        messages.listLeaderOffline, "player", leaderName);
                            }
                        } else {
                            plugin.getMessageManager().sendMessageKeyed(player, "party.listLeaderMissing",
                                    messages.listLeaderMissing);
                        }

                        for (UUID memberId : party.getMembers()) {
                            if (memberId.equals(leaderId)) {
                                continue;
                            }

                            boolean isTarget = targetId != null && targetId.equals(memberId);
                            var offlineMember = Bukkit.getOfflinePlayer(memberId);
                            String memberName = offlineMember.getName();

                            if (memberName == null) {
                                if (isTarget) {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchorMissing",
                                            messages.listAnchorMissing);
                                } else {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listMemberMissing",
                                            messages.listMemberMissing);
                                }
                                continue;
                            }

                            if (isTarget) {
                                if (offlineMember.isOnline()) {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchor",
                                            messages.listAnchor, "player", memberName);
                                } else {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchorOffline",
                                            messages.listAnchorOffline, "player", memberName);
                                }
                            } else {
                                if (offlineMember.isOnline()) {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listMember",
                                            messages.listMember, "player", memberName);
                                } else {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listMemberOffline",
                                            messages.listMemberOffline, "player", memberName);
                                }
                            }
                        }

                        plugin.getMessageManager().sendMessageKeyed(player, "party.listSettingsHeader",
                                messages.listSettingsHeader);

                        plugin.getMessageManager().sendMessageKeyed(player, "party.listRespawnMode",
                                messages.listRespawnMode, "mode", party.getRespawnMode().name());

                        if (targetId != null) {
                            var targetOffline = Bukkit.getOfflinePlayer(targetId);
                            String targetName = targetOffline.getName();

                            if (targetName != null) {
                                if (targetOffline.isOnline()) {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchor",
                                            messages.listAnchor, "player", targetName);
                                } else {
                                    plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchorOffline",
                                            messages.listAnchorOffline, "player", targetName);
                                }
                            } else {
                                plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchorMissing",
                                        messages.listAnchorMissing);
                            }
                        } else {
                            plugin.getMessageManager().sendMessageKeyed(player, "party.listNoAnchor",
                                    messages.listNoAnchor);
                        }

                        PartyManager.PersonalWalkingSpawnPointStatus personalWalkingStatus =
                                plugin.getPartyManager().getPersonalWalkingSpawnPointStatus(player);

                        switch (personalWalkingStatus) {
                            case ACTIVE -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointActive",
                                    messages.listWalkingSpawnPointActive);
                            case INACTIVE_MODE_NORMAL -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointInactiveModeNormal",
                                    messages.listWalkingSpawnPointInactiveModeNormal);
                            case UNAVAILABLE_GLOBAL_DISABLED -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointUnavailableGlobal",
                                    messages.listWalkingSpawnPointUnavailableGlobal);
                            case UNAVAILABLE_NO_PERMISSION -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointUnavailableNoPermission",
                                    messages.listWalkingSpawnPointUnavailableNoPermission);
                        }

                        PartyManager.TargetWalkingSpawnPointStatus targetWalkingStatus =
                                plugin.getPartyManager().getTargetWalkingSpawnPointStatus(player);

                        switch (targetWalkingStatus) {
                            case ACTIVE -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointActive",
                                    messages.listTargetWalkingSpawnPointActive);
                            case INACTIVE_MODE_NORMAL -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointInactiveModeNormal",
                                    messages.listTargetWalkingSpawnPointInactiveModeNormal);
                            case UNAVAILABLE_GLOBAL_DISABLED -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointUnavailableGlobal",
                                    messages.listTargetWalkingSpawnPointUnavailableGlobal);
                            case UNAVAILABLE_NO_PERMISSION -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointUnavailableNoPermission",
                                    messages.listTargetWalkingSpawnPointUnavailableNoPermission);
                            case NO_TARGET -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointNoTarget",
                                    messages.listTargetWalkingSpawnPointNoTarget);
                            case TARGET_OFFLINE -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointTargetOffline",
                                    messages.listTargetWalkingSpawnPointTargetOffline);
                            case TARGET_NOT_FOUND -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointTargetMissing",
                                    messages.listTargetWalkingSpawnPointTargetMissing);
                        }

                        plugin.getMessageManager().sendMessageKeyed(player, "party.listSeparator",
                                messages.listSeparator);

                    } else {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.invitationExpiredOrInvalid",
                                plugin.getConfigManager().getMessagesConfig().party.invitationExpiredOrInvalid);
                    }
                });
    }
}