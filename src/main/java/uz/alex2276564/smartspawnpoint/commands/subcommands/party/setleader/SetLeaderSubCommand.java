package uz.alex2276564.smartspawnpoint.commands.subcommands.party.setleader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

public class SetLeaderSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("setleader")
                .permission("smartspawnpoint.party.setleader")
                .description("Transfer party leadership")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions(partial ->
                                Bukkit.getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                                        .toList()))
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
                        plugin.getMessageManager().sendMessage(player, "<red>Only the party leader can transfer leadership.");
                        return;
                    }

                    // Get target player
                    Player targetPlayer = context.getArgument("player");

                    // Check if target is in the party
                    if (!party.isMember(targetPlayer.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>This player is not in your party.");
                        return;
                    }

                    // Can't transfer leadership to yourself
                    if (targetPlayer.equals(player)) {
                        plugin.getMessageManager().sendMessage(player, "<red>You are already the party leader.");
                        return;
                    }

                    // Set new leader
                    boolean success = partyManager.setLeader(player, targetPlayer);

                    if (success) {
                        // Notify party members
                        for (Player member : party.getOnlineMembers()) {
                            plugin.getMessageManager().sendMessage(member,
                                    "<green>" + targetPlayer.getName() + " is now the party leader.");
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<red>Couldn't transfer leadership. An error occurred.");
                    }
                });
    }
}