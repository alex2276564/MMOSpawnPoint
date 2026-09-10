package uz.alex2276564.mmospawnpoint.commands.subcommands.party.list;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.UUID;

public class ListSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public ListSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("list")
                .permission("mmospawnpoint.party.list")
                .description("List all party members")
                .executor((sender, context) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();
                    PartyManager partyManager = services.partyManager();

                    if (!(sender instanceof Player player)) {
                        messageManager.sendMessageKeyed(sender, "party.onlyPlayers",
                                configManager.getMessagesConfig().party.onlyPlayers);
                        return;
                    }

                    if (!configManager.getMainConfig().party.enabled || partyManager == null) {
                        messageManager.sendMessageKeyed(sender, "party.systemDisabled",
                                configManager.getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    if (!partyManager.isInParty(player.getUniqueId())) {
                        messageManager.sendMessageKeyed(player, "party.notInParty",
                                configManager.getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());
                    var messages = configManager.getMessagesConfig().party;

                    messageManager.sendMessageKeyed(player, "party.listHeader", messages.listHeader);

                    UUID leaderId = party.getLeader();
                    UUID targetId = party.getRespawnTarget();

                    var leaderOffline = Bukkit.getOfflinePlayer(leaderId);
                    String leaderName = leaderOffline.getName();

                    if (leaderName != null) {
                        if (leaderOffline.isOnline()) {
                            messageManager.sendMessageKeyed(player, "party.listLeader",
                                    messages.listLeader, "player", leaderName);
                        } else {
                            messageManager.sendMessageKeyed(player, "party.listLeaderOffline",
                                    messages.listLeaderOffline, "player", leaderName);
                        }
                    } else {
                        messageManager.sendMessageKeyed(player, "party.listLeaderMissing",
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
                                messageManager.sendMessageKeyed(player, "party.listAnchorMissing",
                                        messages.listAnchorMissing);
                            } else {
                                messageManager.sendMessageKeyed(player, "party.listMemberMissing",
                                        messages.listMemberMissing);
                            }
                            continue;
                        }

                        if (isTarget) {
                            if (offlineMember.isOnline()) {
                                messageManager.sendMessageKeyed(player, "party.listAnchor",
                                        messages.listAnchor, "player", memberName);
                            } else {
                                messageManager.sendMessageKeyed(player, "party.listAnchorOffline",
                                        messages.listAnchorOffline, "player", memberName);
                            }
                        } else {
                            if (offlineMember.isOnline()) {
                                messageManager.sendMessageKeyed(player, "party.listMember",
                                        messages.listMember, "player", memberName);
                            } else {
                                messageManager.sendMessageKeyed(player, "party.listMemberOffline",
                                        messages.listMemberOffline, "player", memberName);
                            }
                        }
                    }

                    messageManager.sendMessageKeyed(player, "party.listSettingsHeader",
                            messages.listSettingsHeader);

                    messageManager.sendMessageKeyed(player, "party.listRespawnMode",
                            messages.listRespawnMode, "mode", party.getRespawnMode().name());

                    if (targetId != null) {
                        var targetOffline = Bukkit.getOfflinePlayer(targetId);
                        String targetName = targetOffline.getName();

                        if (targetName != null) {
                            if (targetOffline.isOnline()) {
                                messageManager.sendMessageKeyed(player, "party.listAnchor",
                                        messages.listAnchor, "player", targetName);
                            } else {
                                messageManager.sendMessageKeyed(player, "party.listAnchorOffline",
                                        messages.listAnchorOffline, "player", targetName);
                            }
                        } else {
                            messageManager.sendMessageKeyed(player, "party.listAnchorMissing",
                                    messages.listAnchorMissing);
                        }
                    } else {
                        messageManager.sendMessageKeyed(player, "party.listNoAnchor",
                                messages.listNoAnchor);
                    }

                    PartyManager.PersonalWalkingSpawnPointStatus personalWalkingStatus =
                            partyManager.getPersonalWalkingSpawnPointStatus(player);

                    switch (personalWalkingStatus) {
                        case ACTIVE -> messageManager.sendMessageKeyed(player,
                                "party.listWalkingSpawnPointActive",
                                messages.listWalkingSpawnPointActive);
                        case INACTIVE_MODE_NORMAL -> messageManager.sendMessageKeyed(player,
                                "party.listWalkingSpawnPointInactiveModeNormal",
                                messages.listWalkingSpawnPointInactiveModeNormal);
                        case UNAVAILABLE_GLOBAL_DISABLED -> messageManager.sendMessageKeyed(player,
                                "party.listWalkingSpawnPointUnavailableGlobal",
                                messages.listWalkingSpawnPointUnavailableGlobal);
                        case UNAVAILABLE_NO_PERMISSION -> messageManager.sendMessageKeyed(player,
                                "party.listWalkingSpawnPointUnavailableNoPermission",
                                messages.listWalkingSpawnPointUnavailableNoPermission);
                    }

                    PartyManager.TargetWalkingSpawnPointStatus targetWalkingStatus =
                            partyManager.getTargetWalkingSpawnPointStatus(player);

                    switch (targetWalkingStatus) {
                        case ACTIVE -> messageManager.sendMessageKeyed(player,
                                "party.listTargetWalkingSpawnPointActive",
                                messages.listTargetWalkingSpawnPointActive);
                        case INACTIVE_MODE_NORMAL -> messageManager.sendMessageKeyed(player,
                                "party.listTargetWalkingSpawnPointInactiveModeNormal",
                                messages.listTargetWalkingSpawnPointInactiveModeNormal);
                        case UNAVAILABLE_GLOBAL_DISABLED -> messageManager.sendMessageKeyed(player,
                                "party.listTargetWalkingSpawnPointUnavailableGlobal",
                                messages.listTargetWalkingSpawnPointUnavailableGlobal);
                        case UNAVAILABLE_NO_PERMISSION -> messageManager.sendMessageKeyed(player,
                                "party.listTargetWalkingSpawnPointUnavailableNoPermission",
                                messages.listTargetWalkingSpawnPointUnavailableNoPermission);
                        case NO_TARGET -> messageManager.sendMessageKeyed(player,
                                "party.listTargetWalkingSpawnPointNoTarget",
                                messages.listTargetWalkingSpawnPointNoTarget);
                        case TARGET_OFFLINE -> messageManager.sendMessageKeyed(player,
                                "party.listTargetWalkingSpawnPointTargetOffline",
                                messages.listTargetWalkingSpawnPointTargetOffline);
                        case TARGET_NOT_FOUND -> messageManager.sendMessageKeyed(player,
                                "party.listTargetWalkingSpawnPointTargetMissing",
                                messages.listTargetWalkingSpawnPointTargetMissing);
                    }

                    messageManager.sendMessageKeyed(player, "party.listSeparator",
                            messages.listSeparator);
                });
    }
}