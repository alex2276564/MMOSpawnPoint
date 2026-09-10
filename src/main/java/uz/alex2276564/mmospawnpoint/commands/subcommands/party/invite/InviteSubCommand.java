package uz.alex2276564.mmospawnpoint.commands.subcommands.party.invite;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.*;

public class InviteSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public InviteSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

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

                            var partyManager = services.partyManager();

                            String needle = (partial == null ? "" : partial.toLowerCase(Locale.ROOT));

                            // Collect current party members, if the sender is already in a party.
                            // This allows tab completion to hide players who are already in the same party.
                            Set<UUID> memberIds = null;
                            if (partyManager.isInParty(self.getUniqueId())) {
                                var party = partyManager.getPlayerParty(self.getUniqueId());
                                if (party != null) {
                                    memberIds = new HashSet<>(party.getMembers());
                                }
                            }
                            final Set<UUID> finalMemberIds = memberIds;

                            return Bukkit.getOnlinePlayers().stream()
                                    // If the sender is already in a party, do not suggest players
                                    // who are already members of that same party.
                                    // This automatically hides the sender as well, because they are a party member too.
                                    .filter(target -> finalMemberIds == null || !finalMemberIds.contains(target.getUniqueId()))
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(needle))
                                    .toList();
                        }))
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

                    Player targetPlayer = context.getArgument("player");
                    if (targetPlayer == null) {
                        // Should not happen with ArgumentType.PLAYER, but just in case
                        messageManager.sendMessageKeyed(player, "party.errorOccurred",
                                configManager.getMessagesConfig().party.errorOccurred);
                        return;
                    }

                    // Self-invite as the invite-success flow
                    if (targetPlayer.getUniqueId().equals(player.getUniqueId())) {
                        if (!partyManager.isInParty(player.getUniqueId())) {
                            partyManager.createParty(player);
                            messageManager.sendMessageKeyed(player, "party.joinedParty",
                                    configManager.getMessagesConfig().party.joinedParty);
                            return;
                        }

                        messageManager.sendMessageKeyed(player, "party.inviteFailedAlreadyInParty",
                                configManager.getMessagesConfig().party.inviteFailedAlreadyInParty);
                        return;
                    }

                    // Auto-create party if player is not in one
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        partyManager.createParty(player);
                    }

                    PartyManager.InviteResult result = partyManager.invitePlayer(player, targetPlayer);

                    switch (result) {
                        case SUCCESS -> {
                            String sent = configManager.getMessagesConfig().party.inviteSent;
                            messageManager.sendMessageKeyed(player, "party.inviteSent",
                                    sent, "player", targetPlayer.getName());

                            String recv = configManager.getMessagesConfig().party.inviteReceived;
                            messageManager.sendMessageKeyed(targetPlayer, "party.inviteReceived",
                                    recv, "player", player.getName());
                        }
                        case ALREADY_INVITED -> {
                            String sent = configManager.getMessagesConfig().party.inviteSent;
                            messageManager.sendMessageKeyed(player, "party.inviteSent",
                                    sent, "player", targetPlayer.getName());
                        }
                        case TARGET_ALREADY_IN_PARTY ->
                                messageManager.sendMessageKeyed(player, "party.inviteFailedAlreadyInParty",
                                        configManager.getMessagesConfig().party.inviteFailedAlreadyInParty);
                        case PARTY_FULL -> messageManager.sendMessageKeyed(player, "party.inviteFailedPartyFull",
                                configManager.getMessagesConfig().party.inviteFailedPartyFull);
                        case NOT_LEADER -> messageManager.sendMessageKeyed(player, "party.notLeader",
                                configManager.getMessagesConfig().party.notLeader);
                        case LEADER_NOT_IN_PARTY -> messageManager.sendMessageKeyed(player, "party.notInParty",
                                configManager.getMessagesConfig().party.notInParty);
                        default -> messageManager.sendMessageKeyed(player, "party.errorOccurred",
                                configManager.getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}