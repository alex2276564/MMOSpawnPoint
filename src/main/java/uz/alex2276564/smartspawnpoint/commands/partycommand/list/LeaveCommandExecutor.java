package uz.alex2276564.smartspawnpoint.commands.partycommand.list;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.SubCommand;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaveCommandExecutor implements SubCommand {
    private final SmartSpawnPoint plugin;

    public LeaveCommandExecutor(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        PartyManager partyManager = plugin.getPartyManager();

        // Check if player is in a party
        if (!partyManager.isInParty(player.getUniqueId())) {
            String message = plugin.getConfigManager().getPartyMessage("not-in-party");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            } else {
                player.sendMessage("§cYou are not in a party.");
            }
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
            String message = plugin.getConfigManager().getPartyMessage("party-left");
            if (!message.isEmpty()) {
                // Replace %player% with the player's name for self-messages
                message = message.replace("%player%", player.getName()).replace("&", "§");
                player.sendMessage(message);
            } else {
                player.sendMessage("§aYou have left the party.");
            }

            // Notify other party members
            for (Player member : partyMembers) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("player", player.getName());
                String notifyMessage = plugin.getConfigManager().formatPartyMessage("party-left", replacements);

                if (!notifyMessage.isEmpty()) {
                    member.sendMessage(notifyMessage);
                } else {
                    member.sendMessage("§c" + player.getName() + " has left the party.");
                }
            }

            // If player was leader and party still exists
            if (isLeader && partyManager.getParty(party.getId()) != null) {
                Player newLeader = party.getLeaderPlayer();
                if (newLeader != null) {
                    for (Player member : party.getOnlineMembers()) {
                        Map<String, String> leaderReplacements = new HashMap<>();
                        leaderReplacements.put("player", newLeader.getName());
                        String leaderMessage = plugin.getConfigManager().formatPartyMessage("leader-changed", leaderReplacements);

                        if (!leaderMessage.isEmpty()) {
                            member.sendMessage(leaderMessage);
                        } else {
                            member.sendMessage("§a" + newLeader.getName() + " is now the party leader.");
                        }
                    }
                }
            }

            // If party was disbanded, notify the last member
            if (isLeader && partyManager.getParty(party.getId()) == null && !partyMembers.isEmpty()) {
                String disbandMessage = plugin.getConfigManager().getPartyMessage("party-disbanded");
                if (!disbandMessage.isEmpty()) {
                    for (Player member : partyMembers) {
                        member.sendMessage(disbandMessage.replace("&", "§"));
                    }
                }
            }
        } else {
            player.sendMessage("§cCouldn't leave the party. An error occurred.");
        }
    }



    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}