package uz.alex2276564.mmospawnpoint.commands.subcommands.party.setleader;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.List;

public class SetLeaderSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("setleader")
                .permission("mmospawnpoint.party.setleader")
                .description("Transfer party leadership")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (!(sender instanceof Player p)) return List.of();
                            var pm = MMOSpawnPoint.getInstance().getPartyManager();
                            var party = pm != null ? pm.getPlayerParty(p.getUniqueId()) : null;
                            if (party == null) return List.of();
                            String needle = partial == null ? "" : partial.toLowerCase();
                            return party.getOnlineMembers().stream()
                                    .filter(m -> !m.getUniqueId().equals(p.getUniqueId()))
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().startsWith(needle))
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

                    PartyManager partyManager = plugin.getPartyManager();

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.notInParty",
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.notLeader",
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        return;
                    }

                    // Get target player
                    Player targetPlayer = context.getArgument("player");

                    // Check if target is in the party
                    if (!party.isMember(targetPlayer.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.playerNotInYourParty",
                                plugin.getConfigManager().getMessagesConfig().party.playerNotInYourParty);
                        return;
                    }

                    // Can't transfer leadership to yourself
                    if (targetPlayer.equals(player)) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.alreadyLeader",
                                plugin.getConfigManager().getMessagesConfig().party.alreadyLeader);
                        return;
                    }

                    // Set new leader
                    boolean success = partyManager.setLeader(player, targetPlayer);

                    if (success) {
                        // Notify party members
                        String leaderMessage = plugin.getConfigManager().getMessagesConfig().party.newLeaderAssigned;
                        for (Player member : party.getOnlineMembers()) {
                            plugin.getMessageManager().sendMessageKeyed(member, "party.newLeaderAssigned", leaderMessage, "player", targetPlayer.getName());
                        }
                    } else {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.errorOccurred",
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}