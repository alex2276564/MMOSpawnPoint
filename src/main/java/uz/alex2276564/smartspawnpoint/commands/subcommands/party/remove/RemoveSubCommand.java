package uz.alex2276564.smartspawnpoint.commands.subcommands.party.remove;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
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
                        plugin.getMessageManager().sendMessage(sender, "<red>Only players can use this command!");
                        return;
                    }

                    if (!plugin.getConfigManager().isPartyEnabled()) {
                        plugin.getMessageManager().sendMessage(sender, "<red>Party system is disabled.");
                        return;
                    }

                    PartyManager partyManager = plugin.getPartyManager();

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>You are not in a party.");
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>Only the party leader can remove players.");
                        return;
                    }

                    // Get target player
                    Player targetPlayer = context.getArgument("player");

                    // Check if target is in the party
                    if (!party.isMember(targetPlayer.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>This player is not in your party.");
                        return;
                    }

                    // Can't remove yourself (use leave instead)
                    if (targetPlayer.equals(player)) {
                        plugin.getMessageManager().sendMessage(player, "<red>You cannot remove yourself from the party. Use <yellow>/ssp party leave <red>instead.");
                        return;
                    }

                    // Remove player from party
                    boolean success = partyManager.removePlayer(player, targetPlayer);

                    if (success) {
                        // Notify target player
                        plugin.getMessageManager().sendMessage(targetPlayer, "<red>You have been removed from the party.");

                        // Notify party members
                        for (Player member : party.getOnlineMembers()) {
                            if (!member.equals(targetPlayer)) {
                                plugin.getMessageManager().sendMessage(member,
                                        "<red>" + targetPlayer.getName() + " has been removed from the party.");
                            }
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<red>Couldn't remove player from the party. An error occurred.");
                    }
                });
    }
}