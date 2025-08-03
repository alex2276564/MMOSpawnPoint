package uz.alex2276564.smartspawnpoint.commands.subcommands.party.accept;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.UUID;

public class AcceptSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("accept")
                .permission("smartspawnpoint.party.accept")
                .description("Accept a party invitation")
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

                    // Accept invitation
                    boolean success = partyManager.acceptInvitation(player);

                    if (success) {
                        Party party = partyManager.getPlayerParty(playerId);

                        // Notify other party members
                        for (Player member : party.getOnlineMembers()) {
                            if (!member.equals(player)) {
                                plugin.getMessageManager().sendMessage(member,
                                        "<green>" + player.getName() + " has joined the party!");
                            }
                        }

                        // Message to the player who joined
                        plugin.getMessageManager().sendMessage(player, "<green>You have joined the party!");
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<red>Couldn't accept invitation. It may have expired.");
                    }
                });
    }
}