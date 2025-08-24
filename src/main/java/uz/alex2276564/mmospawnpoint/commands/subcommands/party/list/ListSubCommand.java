package uz.alex2276564.mmospawnpoint.commands.subcommands.party.list;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;

import java.util.List;

public class ListSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("list")
                .permission("mmospawnpoint.party.list")
                .description("List all party members")
                .executor((sender, context) -> {
                    MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();

                    if (!(sender instanceof Player player)) {
                        plugin.getMessageManager().sendMessage(sender,
                                plugin.getConfigManager().getMessagesConfig().party.onlyPlayers);
                        return;
                    }

                    if (!plugin.getConfigManager().getMainConfig().party.enabled) {
                        plugin.getMessageManager().sendMessage(sender,
                                plugin.getConfigManager().getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());

                    plugin.getMessageManager().sendMessage(player,
                            plugin.getConfigManager().getMessagesConfig().party.listHeader);

                    Player leader = party.getLeaderPlayer();
                    if (leader != null) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.listLeader,
                                "player", leader.getName());
                    } else {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.listLeaderMissing);
                    }

                    List<Player> onlineMembers = party.getOnlineMembers();
                    onlineMembers.remove(leader);

                    for (Player member : onlineMembers) {
                        if (party.getRespawnTarget() != null && party.getRespawnTarget().equals(member.getUniqueId())) {
                            plugin.getMessageManager().sendMessage(player,
                                    plugin.getConfigManager().getMessagesConfig().party.listAnchor,
                                    "player", member.getName());
                        } else {
                            plugin.getMessageManager().sendMessage(player,
                                    plugin.getConfigManager().getMessagesConfig().party.listMember,
                                    "player", member.getName());
                        }
                    }

                    plugin.getMessageManager().sendMessage(player,
                            plugin.getConfigManager().getMessagesConfig().party.listSettingsHeader);

                    plugin.getMessageManager().sendMessage(player,
                            plugin.getConfigManager().getMessagesConfig().party.listRespawnMode,
                            "mode", party.getRespawnMode().name());

                    if (party.getRespawnTarget() != null) {
                        Player target = party.getRespawnTargetPlayer();
                        if (target != null) {
                            plugin.getMessageManager().sendMessage(player,
                                    plugin.getConfigManager().getMessagesConfig().party.listAnchor,
                                    "player", target.getName());
                        } else {
                            plugin.getMessageManager().sendMessage(player,
                                    plugin.getConfigManager().getMessagesConfig().party.listAnchorMissing);
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.listNoAnchor);
                    }

                    plugin.getMessageManager().sendMessage(player,
                            plugin.getConfigManager().getMessagesConfig().party.listSeparator);
                });
    }
}