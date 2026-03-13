package uz.alex2276564.mmospawnpoint.commands.subcommands.party.invite;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.*;

public class InviteSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("invite")
                .permission("mmospawnpoint.party.invite")
                .description("Invite a player to your party")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (!(sender instanceof Player self)) {
                                return List.of();
                            }

                            MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();
                            PartyManager pm = plugin.getPartyManager();

                            String needle = (partial == null ? "" : partial.toLowerCase(Locale.ROOT));

                            // Collect current party members, if the sender is already in a party.
                            // This allows tab completion to hide players who are already in the same party.
                            Set<UUID> memberIds = null;
                            if (pm != null && pm.isInParty(self.getUniqueId())) {
                                var party = pm.getPlayerParty(self.getUniqueId());
                                if (party != null) {
                                    memberIds = new HashSet<>(party.getMembers());
                                }
                            }
                            final Set<UUID> finalMemberIds = memberIds;

                            return plugin.getServer().getOnlinePlayers().stream()
                                    // If the sender is already in a party, do not suggest players
                                    // who are already members of that same party.
                                    // This automatically hides the sender as well, because they are a party member too.
                                    .filter(target -> finalMemberIds == null || !finalMemberIds.contains(target.getUniqueId()))
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(needle))
                                    .toList();
                        }))
                .executor((sender, context) -> {
                    var plugin = MMOSpawnPoint.getInstance();

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

                    Player targetPlayer = context.getArgument("player");
                    if (targetPlayer == null) {
                        // Should not happen with ArgumentType.PLAYER, but just in case
                        plugin.getMessageManager().sendMessageKeyed(player, "party.errorOccurred",
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                        return;
                    }

                    PartyManager partyManager = plugin.getPartyManager();

                    // Self-invite as the invite-success flow
                    if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
                        if (!partyManager.isInParty(player.getUniqueId())) {
                            partyManager.createParty(player);
                            plugin.getMessageManager().sendMessageKeyed(player, "party.joinedParty",
                                    plugin.getConfigManager().getMessagesConfig().party.joinedParty);
                            return;
                        }

                        plugin.getMessageManager().sendMessageKeyed(player, "party.inviteFailedAlreadyInParty",
                                plugin.getConfigManager().getMessagesConfig().party.inviteFailedAlreadyInParty);
                        return;
                    }

                    // Auto-create party if player is not in one
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        partyManager.createParty(player);
                    }

                    PartyManager.InviteResult result = partyManager.invitePlayer(player, targetPlayer);

                    switch (result) {
                        case SUCCESS -> {
                            String sent = plugin.getConfigManager().getMessagesConfig().party.inviteSent;
                            plugin.getMessageManager().sendMessageKeyed(player, "party.inviteSent", sent, "player", targetPlayer.getName());

                            String recv = plugin.getConfigManager().getMessagesConfig().party.inviteReceived;
                            plugin.getMessageManager().sendMessageKeyed(targetPlayer, "party.inviteReceived", recv, "player", player.getName());
                        }
                        case ALREADY_INVITED -> {
                            String sent = plugin.getConfigManager().getMessagesConfig().party.inviteSent;
                            plugin.getMessageManager().sendMessageKeyed(player, "party.inviteSent", sent, "player", targetPlayer.getName());
                        }
                        case TARGET_ALREADY_IN_PARTY ->
                                plugin.getMessageManager().sendMessageKeyed(player, "party.inviteFailedAlreadyInParty",
                                        plugin.getConfigManager().getMessagesConfig().party.inviteFailedAlreadyInParty);
                        case PARTY_FULL ->
                                plugin.getMessageManager().sendMessageKeyed(player, "party.inviteFailedPartyFull",
                                        plugin.getConfigManager().getMessagesConfig().party.inviteFailedPartyFull);
                        case NOT_LEADER -> plugin.getMessageManager().sendMessageKeyed(player, "party.notLeader",
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        case LEADER_NOT_IN_PARTY ->
                                plugin.getMessageManager().sendMessageKeyed(player, "party.notInParty",
                                        plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        default -> plugin.getMessageManager().sendMessageKeyed(player, "party.errorOccurred",
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}