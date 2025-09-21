package uz.alex2276564.mmospawnpoint.commands.subcommands.party.invite;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.List;

public class InviteSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("invite")
                .permission("mmospawnpoint.party.invite")
                .description("Invite a player to your party")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (!(sender instanceof Player p)) return List.of();
                            var pm = MMOSpawnPoint.getInstance().getPartyManager();
                            var party = (pm != null ? pm.getPlayerParty(p.getUniqueId()) : null);

                            String needle = partial == null ? "" : partial.toLowerCase();

                            if (party == null) {
                                // not in party -> suggest all online + yourself
                                return MMOSpawnPoint.getInstance().getServer().getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(name -> name.toLowerCase().startsWith(needle))
                                        .toList();
                            } else {
                                // in party -> suggest NON-members + yourself
                                var memberIds = party.getMembers(); // Set<UUID>
                                return MMOSpawnPoint.getInstance().getServer().getOnlinePlayers().stream()
                                        .filter(pl -> pl.getUniqueId().equals(p.getUniqueId()) || !memberIds.contains(pl.getUniqueId()))
                                        .map(Player::getName)
                                        .filter(name -> name.toLowerCase().startsWith(needle))
                                        .toList();
                            }
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
                        case TARGET_ALREADY_IN_PARTY -> plugin.getMessageManager().sendMessageKeyed(player, "party.inviteFailedAlreadyInParty",
                                plugin.getConfigManager().getMessagesConfig().party.inviteFailedAlreadyInParty);
                        case PARTY_FULL -> plugin.getMessageManager().sendMessageKeyed(player, "party.inviteFailedPartyFull",
                                plugin.getConfigManager().getMessagesConfig().party.inviteFailedPartyFull);
                        case NOT_LEADER -> plugin.getMessageManager().sendMessageKeyed(player, "party.notLeader",
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        case LEADER_NOT_IN_PARTY -> plugin.getMessageManager().sendMessageKeyed(player, "party.notInParty",
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        default -> plugin.getMessageManager().sendMessageKeyed(player, "party.errorOccurred",
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}