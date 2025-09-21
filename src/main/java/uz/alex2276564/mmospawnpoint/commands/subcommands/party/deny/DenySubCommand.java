package uz.alex2276564.mmospawnpoint.commands.subcommands.party.deny;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.UUID;

public class DenySubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("deny")
                .permission("mmospawnpoint.party.deny")
                .description("Decline a party invitation")
                .executor((sender, context) -> {
                    MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();

                    if (!(sender instanceof Player player)) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "party.onlyPlayers",
                                plugin.getConfigManager().getMessagesConfig().party.onlyPlayers);
                        return;
                    }

                    if (!plugin.getConfigManager().getMainConfig().party.enabled) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "party.systemDisabled",
                                plugin.getConfigManager().getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    PartyManager partyManager = plugin.getPartyManager();
                    UUID playerId = player.getUniqueId();

                    // Check if player has a pending invitation
                    UUID pendingInvitation = partyManager.getPendingInvitation(playerId);
                    if (pendingInvitation == null) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.noInvitations",
                                plugin.getConfigManager().getMessagesConfig().party.noInvitations);
                        return;
                    }

                    // Get party and leader before declining
                    Party party = partyManager.getParty(pendingInvitation);
                    Player leader = party != null ? party.getLeaderPlayer() : null;

                    // Decline invitation
                    boolean success = partyManager.declineInvitation(player);

                    if (success) {
                        // Message to the player who declined
                        plugin.getMessageManager().sendMessageKeyed(player, "party.invitationDeclined",
                                plugin.getConfigManager().getMessagesConfig().party.invitationDeclined);

                        // Notify party leader
                        if (leader != null && leader.isOnline()) {
                            String declineMessage = plugin.getConfigManager().getMessagesConfig().party.invitationDeclinedToLeader;
                            plugin.getMessageManager().sendMessageKeyed(leader, "party.invitationDeclinedToLeader", declineMessage, "player", player.getName());
                        }
                    } else {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.invitationExpiredOrInvalid",
                                plugin.getConfigManager().getMessagesConfig().party.invitationExpiredOrInvalid);
                    }
                });
    }
}