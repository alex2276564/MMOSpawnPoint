package uz.alex2276564.mmospawnpoint.commands.subcommands.party.leave;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.ArrayList;
import java.util.List;

public class LeaveSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public LeaveSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("leave")
                .permission("mmospawnpoint.party.leave")
                .description("Leave your current party")
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

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        messageManager.sendMessageKeyed(player, "party.notInParty",
                                configManager.getMessagesConfig().party.notInParty);
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
                        messageManager.sendMessageKeyed(player, "party.leftParty",
                                configManager.getMessagesConfig().party.leftParty);

                        // Notify other party members
                        String leftMessage = configManager.getMessagesConfig().party.playerLeftParty;

                        for (Player member : partyMembers) {
                            messageManager.sendMessageKeyed(member, "party.playerLeftParty",
                                    leftMessage, "player", player.getName());
                        }

                        // If player was leader and party still exists
                        if (isLeader && partyManager.getParty(party.getId()) != null) {
                            Player newLeader = party.getLeaderPlayer();
                            if (newLeader != null) {
                                String newLeaderMessage =
                                        configManager.getMessagesConfig().party.newLeaderAssigned;

                                for (Player member : party.getOnlineMembers()) {
                                    messageManager.sendMessageKeyed(member, "party.newLeaderAssigned",
                                            newLeaderMessage, "player", newLeader.getName());
                                }
                            }
                        }
                    } else {
                        messageManager.sendMessageKeyed(player, "party.errorOccurred",
                                configManager.getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}