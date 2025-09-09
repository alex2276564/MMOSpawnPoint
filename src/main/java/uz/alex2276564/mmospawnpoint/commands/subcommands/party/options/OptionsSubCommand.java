package uz.alex2276564.mmospawnpoint.commands.subcommands.party.options;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.mode.ModeSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.target.TargetSubCommand;
import uz.alex2276564.mmospawnpoint.party.Party;

public class OptionsSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        SubCommandBuilder optionsBuilder = parent.subcommand("options")
                .permission("mmospawnpoint.party.options")
                .description("View and change party options")
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

                    // Display current options
                    var options = plugin.getConfigManager().getMessagesConfig().party.options;
                    plugin.getMessageManager().sendMessageKeyed(player, "party.options.header", options.header);
                    plugin.getMessageManager().sendMessageKeyed(player, "party.options.respawnMode", options.respawnMode, "mode", party.getRespawnMode().name());
                    plugin.getMessageManager().sendMessageKeyed(player, "party.options.respawnTarget", options.respawnTarget, "target", getRespawnTargetDisplayName(party));
                    plugin.getMessageManager().sendMessageKeyed(player, "party.options.separator", options.separator);
                    plugin.getMessageManager().sendMessageKeyed(player, "party.options.modeHelp", options.modeHelp);
                    plugin.getMessageManager().sendMessageKeyed(player, "party.options.targetHelp", options.targetHelp);
                });

        // Register nested options subcommands
        new ModeSubCommand().build(optionsBuilder);
        new TargetSubCommand().build(optionsBuilder);

        return optionsBuilder;
    }

    private String getRespawnTargetDisplayName(Party party) {
        if (party.getRespawnTarget() == null) {
            return "None";
        }
        Player targetPlayer = party.getRespawnTargetPlayer();
        return targetPlayer != null ? targetPlayer.getName() : "<gray>(Wandering in the void)";
    }
}