package uz.alex2276564.mmospawnpoint.commands.subcommands.party.deny;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.UUID;

public class DenySubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public DenySubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("deny")
                .permission("mmospawnpoint.party.deny")
                .description("Decline a party invitation")
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

                    UUID playerId = player.getUniqueId();

                    // Check if player has a pending invitation
                    UUID pendingInvitation = partyManager.getPendingInvitation(playerId);
                    if (pendingInvitation == null) {
                        messageManager.sendMessageKeyed(player, "party.noInvitations",
                                configManager.getMessagesConfig().party.noInvitations);
                        return;
                    }

                    // Get party and leader before declining
                    Party party = partyManager.getParty(pendingInvitation);
                    Player leader = party != null ? party.getLeaderPlayer() : null;

                    // Decline invitation
                    boolean success = partyManager.declineInvitation(player);

                    if (success) {
                        // Message to the player who declined
                        messageManager.sendMessageKeyed(player, "party.invitationDeclined",
                                configManager.getMessagesConfig().party.invitationDeclined);

                        // Notify party leader
                        if (leader != null && leader.isOnline()) {
                            String declineMessage = configManager.getMessagesConfig().party.invitationDeclinedToLeader;
                            messageManager.sendMessageKeyed(leader, "party.invitationDeclinedToLeader",
                                    declineMessage, "player", player.getName());
                        }
                    } else {
                        messageManager.sendMessageKeyed(player, "party.invitationExpiredOrInvalid",
                                configManager.getMessagesConfig().party.invitationExpiredOrInvalid);
                    }
                });
    }
}