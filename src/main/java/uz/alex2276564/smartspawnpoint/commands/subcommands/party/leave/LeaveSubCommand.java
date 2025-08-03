package uz.alex2276564.smartspawnpoint.commands.subcommands.party.leave;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.ArrayList;
import java.util.List;

public class LeaveSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("leave")
                .permission("smartspawnpoint.party.leave")
                .description("Leave your current party")
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

                    if (!(sender instanceof Player player)) {
                        plugin.getMessageManager().sendMessage(sender, "<red>Only players can use this command!");
                        return;
                    }

                    if (!plugin.getConfigManager().isPartyEnabled()) {
                        plugin.getMessageManager().sendMessage(sender, "<red>Party system is disabled.");
                        return;
                    }

                    PartyManager partyManager = plugin.getPartyManager();

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>You are not in a party.");
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
                        plugin.getMessageManager().sendMessage(player, "<green>You have left the party.");

                        // Notify other party members
                        for (Player member : partyMembers) {
                            plugin.getMessageManager().sendMessage(member,
                                    "<red>" + player.getName() + " has left the party.");
                        }

                        // If player was leader and party still exists
                        if (isLeader && partyManager.getParty(party.getId()) != null) {
                            Player newLeader = party.getLeaderPlayer();
                            if (newLeader != null) {
                                for (Player member : party.getOnlineMembers()) {
                                    plugin.getMessageManager().sendMessage(member,
                                            "<green>" + newLeader.getName() + " is now the party leader.");
                                }
                            }
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<red>Couldn't leave the party. An error occurred.");
                    }
                });
    }
}