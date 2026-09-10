package uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.target;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.List;

public class TargetSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public TargetSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("target")
                .permission("mmospawnpoint.party.options.target")
                .description("Set party respawn target")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (!(sender instanceof Player p)) return List.of();
                            PartyManager pm = services.partyManager();
                            var configManager = services.configManager();

                            if (!configManager.getMainConfig().party.enabled || pm == null) {
                                return List.of();
                            }

                            var party = pm.getPlayerParty(p.getUniqueId());
                            if (party == null) return List.of();

                            String needle = partial == null ? "" : partial.toLowerCase();
                            return party.getOnlineMembers().stream()
                                    .map(Player::getName)
                                    .filter(name -> name.toLowerCase().startsWith(needle))
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

                    if (!partyManager.isInParty(player.getUniqueId())) {
                        messageManager.sendMessageKeyed(player, "party.notInParty",
                                configManager.getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        messageManager.sendMessageKeyed(player, "party.notLeader",
                                configManager.getMessagesConfig().party.notLeader);
                        return;
                    }

                    Player targetPlayer = context.getArgument("player");

                    if (!party.isMember(targetPlayer.getUniqueId())) {
                        messageManager.sendMessageKeyed(player, "party.playerNotInYourParty",
                                configManager.getMessagesConfig().party.playerNotInYourParty);
                        return;
                    }

                    if (partyManager.setRespawnTarget(player, targetPlayer)) {
                        String targetMessage =
                                configManager.getMessagesConfig().party.respawnTargetSet;
                        messageManager.sendMessageKeyed(player, "party.respawnTargetSet",
                                targetMessage, "player", targetPlayer.getName());
                    }
                });
    }
}