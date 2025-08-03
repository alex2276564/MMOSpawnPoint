package uz.alex2276564.smartspawnpoint.commands.subcommands.party.deny;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.UUID;

public class DenySubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("deny")
                .permission("smartspawnpoint.party.deny")
                .description("Decline a party invitation")
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
                    UUID playerId = player.getUniqueId();

                    // Check if player has a pending invitation
                    UUID pendingInvitation = partyManager.getPendingInvitation(playerId);
                    if (pendingInvitation == null) {
                        plugin.getMessageManager().sendMessage(player, "<red>You don't have any pending party invitations.");
                        return;
                    }

                    // Get party and leader before declining
                    Party party = partyManager.getParty(pendingInvitation);
                    Player leader = party != null ? party.getLeaderPlayer() : null;

                    // Decline invitation
                    boolean success = partyManager.declineInvitation(player);

                    if (success) {
                        // Message to the player who declined
                        plugin.getMessageManager().sendMessage(player, "<green>You have declined the party invitation.");

                        // Notify party leader
                        if (leader != null && leader.isOnline()) {
                            plugin.getMessageManager().sendMessage(leader,
                                    "<red>" + player.getName() + " has declined your party invitation.");
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<red>Couldn't decline invitation. It may have expired.");
                    }
                });
    }
}