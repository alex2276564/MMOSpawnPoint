package uz.alex2276564.smartspawnpoint.commands.subcommands.party.options.mode;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.smartspawnpoint.party.Party;

public class ModeSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("mode")
                .permission("smartspawnpoint.party.options.mode")
                .description("Change party respawn mode")
                .argument(new ArgumentBuilder<>("mode", ArgumentType.STRING)
                        .suggestions("NORMAL", "PARTY_MEMBER"))
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

                    if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        return;
                    }

                    String mode = context.getArgument("mode");
                    try {
                        Party.RespawnMode respawnMode = Party.RespawnMode.valueOf(mode.toUpperCase());
                        plugin.getPartyManager().setRespawnMode(player, respawnMode);

                        String modeMessage = plugin.getConfigManager().getMessagesConfig().party.respawnModeChanged;
                        plugin.getMessageManager().sendMessage(player, modeMessage, "mode", respawnMode.name());
                    } catch (IllegalArgumentException e) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.invalidRespawnMode);
                    }
                });
    }
}