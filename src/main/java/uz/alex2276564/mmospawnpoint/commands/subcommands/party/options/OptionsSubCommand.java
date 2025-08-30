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

                    // Display current options
                    plugin.getMessageManager().sendMessage(player, "<dark_gray>[<red>Death Circle Options<dark_gray>]");
                    plugin.getMessageManager().sendMessage(player, "<gray>Soul Binding Mode: <red><mode>", "mode", party.getRespawnMode().name());
                    plugin.getMessageManager().sendMessage(player, "<gray>Soul Target: <red><target>", "target", getRespawnTargetDisplayName(party));
                    plugin.getMessageManager().sendMessage(player, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    plugin.getMessageManager().sendMessage(player, "<gray>Use <white>/msp party options mode <red><normal|party_member><gray> to change binding");
                    plugin.getMessageManager().sendMessage(player, "<gray>Use <white>/msp party options target <red><soul><gray> to set target soul");
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