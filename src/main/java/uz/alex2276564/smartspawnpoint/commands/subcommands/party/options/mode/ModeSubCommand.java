package uz.alex2276564.smartspawnpoint.commands.subcommands.party.options.mode;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
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

                    String mode = context.getArgument("mode");
                    try {
                        Party.RespawnMode respawnMode = Party.RespawnMode.valueOf(mode.toUpperCase());
                        plugin.getPartyManager().setRespawnMode(player, respawnMode);

                        plugin.getMessageManager().sendMessage(player,
                                "<green>Party respawn mode set to: <yellow>" + respawnMode.name());
                    } catch (IllegalArgumentException e) {
                        plugin.getMessageManager().sendMessage(player, "<red>Invalid respawn mode. Use 'NORMAL' or 'PARTY_MEMBER'.");
                    }
                });
    }
}