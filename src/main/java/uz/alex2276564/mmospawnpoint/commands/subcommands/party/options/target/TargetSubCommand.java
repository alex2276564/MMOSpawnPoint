package uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.target;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;

import java.util.List;

public class TargetSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("target")
                .permission("mmospawnpoint.party.options.target")
                .description("Set party respawn target")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (!(sender instanceof Player p)) return List.of();
                            var pm = MMOSpawnPoint.getInstance().getPartyManager();
                            var party = pm != null ? pm.getPlayerParty(p.getUniqueId()) : null;
                            if (party == null) return List.of();

                            String needle = partial == null ? "" : partial.toLowerCase();
                            return party.getOnlineMembers().stream()
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().startsWith(needle))
                                    .toList();
                        }))
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

                    if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.notInParty",
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.notLeader",
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        return;
                    }

                    Player targetPlayer = context.getArgument("player");

                    if (!party.isMember(targetPlayer.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.playerNotInYourParty",
                                plugin.getConfigManager().getMessagesConfig().party.playerNotInYourParty);
                        return;
                    }

                    if (plugin.getPartyManager().setRespawnTarget(player, targetPlayer)) {
                        String targetMessage = plugin.getConfigManager().getMessagesConfig().party.respawnTargetSet;
                        plugin.getMessageManager().sendMessageKeyed(player, "party.respawnTargetSet", targetMessage, "player", targetPlayer.getName());
                    }
                });
    }
}