package uz.alex2276564.smartspawnpoint.commands.subcommands.party.accept;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandBuilder;
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
                    UUID playerId = player.getUniqueId();

                    // Check if player has a pending invitation
                    UUID pendingInvitation = partyManager.getPendingInvitation(playerId);
                    if (pendingInvitation == null) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.noInvitations);
                        return;
                    }

                    // Accept invitation
                    boolean success = partyManager.acceptInvitation(player);

                    if (success) {
                        Party party = partyManager.getPlayerParty(playerId);

                        // Notify other party members
                        String joinMessage = plugin.getConfigManager().getMessagesConfig().party.playerJoinedParty;

                        for (Player member : party.getOnlineMembers()) {
                            if (!member.equals(player)) {
                                plugin.getMessageManager().sendMessage(member, joinMessage, "player", player.getName());
                            }
                        }

                        // Message to the player who joined
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.joinedParty);
                    } else {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.invitationExpiredOrInvalid);
                    }
                });
    }
}