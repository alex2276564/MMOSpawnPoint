package uz.alex2276564.smartspawnpoint.commands.partycommand.list;

import org.bukkit.Bukkit;
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

public class DenyCommandExecutor implements SubCommand {
    private final SmartSpawnPoint plugin;

    public DenyCommandExecutor(SmartSpawnPoint plugin) {
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

        // Get party and leader before declining
        Party party = partyManager.getParty(pendingInvitation);
        Player leader = party != null ? party.getLeaderPlayer() : null;

        // Decline invitation
        boolean success = partyManager.declineInvitation(player);

        if (success) {
            // Message to the player who declined
            player.sendMessage("§aYou have declined the party invitation.");

            // Notify party leader
            if (leader != null && leader.isOnline()) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("player", player.getName());
                String message = plugin.getConfigManager().formatPartyMessage("invite-declined", replacements);
                if (!message.isEmpty()) {
                    leader.sendMessage(message);
                } else {
                    leader.sendMessage("§c" + player.getName() + " has declined your party invitation.");
                }
            }
        } else {
            player.sendMessage("§cCouldn't decline invitation. It may have expired.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}