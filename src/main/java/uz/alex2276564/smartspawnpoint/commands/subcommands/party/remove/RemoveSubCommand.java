package uz.alex2276564.smartspawnpoint.commands.subcommands.party.remove;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

public class RemoveSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("remove")
                .permission("smartspawnpoint.party.remove")
                .description("Remove a player from your party")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions(partial ->
                                // Only suggest party members
                                Bukkit.getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                                        .toList()
                        ))
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

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        return;
                    }

                    // Get target player
                    Player targetPlayer = context.getArgument("player");

                    // Check if target is in the party
                    if (party.isNotMember(targetPlayer.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.playerNotInYourParty);
                        return;
                    }

                    // Can't remove yourself (use leave instead)
                    if (targetPlayer.equals(player)) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.cannotRemoveSelf);
                        return;
                    }

                    // Remove player from party
                    boolean success = partyManager.removePlayer(player, targetPlayer);

                    if (success) {
                        // Notify target player
                        plugin.getMessageManager().sendMessage(targetPlayer,
                                plugin.getConfigManager().getMessagesConfig().party.playerRemoved);

                        // Notify party members
                        String removedMessage = plugin.getConfigManager().getMessagesConfig().party.playerRemovedFromParty;

                        for (Player member : party.getOnlineMembers()) {
                            if (!member.equals(targetPlayer)) {
                                plugin.getMessageManager().sendMessage(member, removedMessage, "player", targetPlayer.getName());
                            }
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}