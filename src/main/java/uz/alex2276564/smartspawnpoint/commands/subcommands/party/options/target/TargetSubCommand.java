package uz.alex2276564.smartspawnpoint.commands.subcommands.party.options.target;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.party.Party;

public class TargetSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("target")
                .permission("smartspawnpoint.party.options.target")
                .description("Set party respawn target")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions(partial -> {
                            // Suggest only party members
                            if (partial == null) return java.util.List.of();

                            return SmartSpawnPoint.getInstance().getServer().getOnlinePlayers().stream()
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                                    .toList();
                        }))
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

                    if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>You are not in a party.");
                        return;
                    }

                    Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>Only the party leader can change options.");
                        return;
                    }

                    Player targetPlayer = context.getArgument("player");

                    if (!party.isMember(targetPlayer.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>This player is not in your party.");
                        return;
                    }

                    if (plugin.getPartyManager().setRespawnTarget(player, targetPlayer)) {
                        plugin.getMessageManager().sendMessage(player,
                                "<green>You will now respawn at <yellow>" + targetPlayer.getName() + "<green>'s location.");
                    }
                });
    }
}