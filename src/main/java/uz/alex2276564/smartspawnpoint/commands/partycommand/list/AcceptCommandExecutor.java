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
import java.util.UUID;

public class AcceptCommandExecutor implements SubCommand {
    private final SmartSpawnPoint plugin;

    public AcceptCommandExecutor(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        PartyManager partyManager = plugin.getPartyManager();
        UUID playerId = player.getUniqueId();

        // Check if player has a pending invitation
        UUID pendingInvitation = partyManager.getPendingInvitation(playerId);
        if (pendingInvitation == null) {
            player.sendMessage("§cYou don't have any pending party invitations.");
            return;
        }

        // Accept invitation
        boolean success = partyManager.acceptInvitation(player);

        if (success) {
            Party party = partyManager.getPlayerParty(playerId);
            Player leader = party.getLeaderPlayer();

            // Notify other party members
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", player.getName());

            String message = plugin.getConfigManager().formatPartyMessage("party-joined", replacements);
            if (!message.isEmpty()) {
                for (Player member : party.getOnlineMembers()) {
                    if (!member.equals(player)) {
                        member.sendMessage(message);
                    }
                }
            } else {
                for (Player member : party.getOnlineMembers()) {
                    if (!member.equals(player)) {
                        member.sendMessage("§a" + player.getName() + " has joined the party!");
                    }
                }
            }

            // Message to the player who joined
            player.sendMessage("§aYou have joined the party!");
        } else {
            player.sendMessage("§cCouldn't accept invitation. It may have expired.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}