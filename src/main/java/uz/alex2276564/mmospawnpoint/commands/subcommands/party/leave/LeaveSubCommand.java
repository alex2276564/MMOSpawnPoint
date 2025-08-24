package uz.alex2276564.mmospawnpoint.commands.subcommands.party.leave;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.ArrayList;
import java.util.List;

public class LeaveSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("leave")
                .permission("mmospawnpoint.party.leave")
                .description("Leave your current party")
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

                    PartyManager partyManager = plugin.getPartyManager();

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());
                    boolean isLeader = party.isLeader(player.getUniqueId());

                    // Store party members before the player leaves
                    List<Player> partyMembers = new ArrayList<>(party.getOnlineMembers());
                    partyMembers.remove(player); // Remove self from notification list

                    // Leave the party
                    boolean success = partyManager.leaveParty(player);

                    if (success) {
                        // Message to the player who left
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.leftParty);

                        // Notify other party members
                        String leftMessage = plugin.getConfigManager().getMessagesConfig().party.playerLeftParty;

                        for (Player member : partyMembers) {
                            plugin.getMessageManager().sendMessage(member, leftMessage, "player", player.getName());
                        }

                        // If player was leader and party still exists
                        if (isLeader && partyManager.getParty(party.getId()) != null) {
                            Player newLeader = party.getLeaderPlayer();
                            if (newLeader != null) {
                                String newLeaderMessage = plugin.getConfigManager().getMessagesConfig().party.newLeaderAssigned;

                                for (Player member : party.getOnlineMembers()) {
                                    plugin.getMessageManager().sendMessage(member, newLeaderMessage, "player", newLeader.getName());
                                }
                            }
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}