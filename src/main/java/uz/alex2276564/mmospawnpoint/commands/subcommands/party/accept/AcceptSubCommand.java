package uz.alex2276564.mmospawnpoint.commands.subcommands.party.accept;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.List;
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

                    // Accept invitation
                    boolean success = partyManager.acceptInvitation(player);

                    if (success) {
                        Party party = partyManager.getPlayerParty(playerId);

                        // Notify other party members
                        String joinMessage = plugin.getConfigManager().getMessagesConfig().party.playerJoinedParty;

                        for (Player member : party.getOnlineMembers()) {
                            if (!member.equals(player)) {
                                plugin.getMessageManager().sendMessageKeyed(member, "party.playerJoinedParty", joinMessage, "player", player.getName());
                            }
                        }

                        // Message to the player who joined
                        plugin.getMessageManager().sendMessageKeyed(player, "party.joinedParty",
                                plugin.getConfigManager().getMessagesConfig().party.joinedParty);

                        // Show current party info to the player who joined
                        plugin.getMessageManager().sendMessageKeyed(player, "party.listHeader",
                                plugin.getConfigManager().getMessagesConfig().party.listHeader);

                        Player leader = party.getLeaderPlayer();
                        if (leader != null) {
                            plugin.getMessageManager().sendMessageKeyed(player, "party.listLeader",
                                    plugin.getConfigManager().getMessagesConfig().party.listLeader,
                                    "player", leader.getName());
                        } else {
                            plugin.getMessageManager().sendMessageKeyed(player, "party.listLeaderMissing",
                                    plugin.getConfigManager().getMessagesConfig().party.listLeaderMissing);
                        }

                        List<Player> onlineMembers = party.getOnlineMembers();
                        if (leader != null) {
                            onlineMembers.remove(leader);
                        }

                        for (Player member : onlineMembers) {
                            if (party.getRespawnTarget() != null && party.getRespawnTarget().equals(member.getUniqueId())) {
                                plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchor",
                                        plugin.getConfigManager().getMessagesConfig().party.listAnchor,
                                        "player", member.getName());
                            } else {
                                plugin.getMessageManager().sendMessageKeyed(player, "party.listMember",
                                        plugin.getConfigManager().getMessagesConfig().party.listMember,
                                        "player", member.getName());
                            }
                        }

                        plugin.getMessageManager().sendMessageKeyed(player, "party.listSettingsHeader",
                                plugin.getConfigManager().getMessagesConfig().party.listSettingsHeader);

                        plugin.getMessageManager().sendMessageKeyed(player, "party.listRespawnMode",
                                plugin.getConfigManager().getMessagesConfig().party.listRespawnMode,
                                "mode", party.getRespawnMode().name());

                        if (party.getRespawnTarget() != null) {
                            Player target = party.getRespawnTargetPlayer();
                            if (target != null) {
                                plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchor",
                                        plugin.getConfigManager().getMessagesConfig().party.listAnchor,
                                        "player", target.getName());
                            } else {
                                plugin.getMessageManager().sendMessageKeyed(player, "party.listAnchorMissing",
                                        plugin.getConfigManager().getMessagesConfig().party.listAnchorMissing);
                            }
                        } else {
                            plugin.getMessageManager().sendMessageKeyed(player, "party.listNoAnchor",
                                    plugin.getConfigManager().getMessagesConfig().party.listNoAnchor);
                        }

                        PartyManager.PersonalWalkingSpawnPointStatus personalWalkingStatus =
                                plugin.getPartyManager().getPersonalWalkingSpawnPointStatus(player);

                        switch (personalWalkingStatus) {
                            case ACTIVE -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointActive",
                                    plugin.getConfigManager().getMessagesConfig().party.listWalkingSpawnPointActive);
                            case INACTIVE_MODE_NORMAL -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointInactiveModeNormal",
                                    plugin.getConfigManager().getMessagesConfig().party.listWalkingSpawnPointInactiveModeNormal);
                            case UNAVAILABLE_GLOBAL_DISABLED -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointUnavailableGlobal",
                                    plugin.getConfigManager().getMessagesConfig().party.listWalkingSpawnPointUnavailableGlobal);
                            case UNAVAILABLE_NO_PERMISSION -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listWalkingSpawnPointUnavailableNoPermission",
                                    plugin.getConfigManager().getMessagesConfig().party.listWalkingSpawnPointUnavailableNoPermission);
                        }

                        PartyManager.TargetWalkingSpawnPointStatus targetWalkingStatus =
                                plugin.getPartyManager().getTargetWalkingSpawnPointStatus(player);

                        switch (targetWalkingStatus) {
                            case ACTIVE -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointActive",
                                    plugin.getConfigManager().getMessagesConfig().party.listTargetWalkingSpawnPointActive);
                            case INACTIVE_MODE_NORMAL -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointInactiveModeNormal",
                                    plugin.getConfigManager().getMessagesConfig().party.listTargetWalkingSpawnPointInactiveModeNormal);
                            case UNAVAILABLE_GLOBAL_DISABLED -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointUnavailableGlobal",
                                    plugin.getConfigManager().getMessagesConfig().party.listTargetWalkingSpawnPointUnavailableGlobal);
                            case UNAVAILABLE_NO_PERMISSION -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointUnavailableNoPermission",
                                    plugin.getConfigManager().getMessagesConfig().party.listTargetWalkingSpawnPointUnavailableNoPermission);
                            case NO_TARGET -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointNoTarget",
                                    plugin.getConfigManager().getMessagesConfig().party.listTargetWalkingSpawnPointNoTarget);
                            case TARGET_NOT_FOUND -> plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.listTargetWalkingSpawnPointTargetMissing",
                                    plugin.getConfigManager().getMessagesConfig().party.listTargetWalkingSpawnPointTargetMissing);
                        }

                        plugin.getMessageManager().sendMessageKeyed(player, "party.listSeparator",
                                plugin.getConfigManager().getMessagesConfig().party.listSeparator);

                    } else {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.invitationExpiredOrInvalid",
                                plugin.getConfigManager().getMessagesConfig().party.invitationExpiredOrInvalid);
                    }
                });
    }
}