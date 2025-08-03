package uz.alex2276564.smartspawnpoint.commands.subcommands.party.options;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.options.mode.ModeSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.options.target.TargetSubCommand;
import uz.alex2276564.smartspawnpoint.party.Party;

public class OptionsSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        SubCommandBuilder optionsBuilder = parent.subcommand("options")
                .permission("smartspawnpoint.party.options")
                .description("View and change party options")
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

                    // Display current options
                    plugin.getMessageManager().sendMessage(player, "<gold>==== Party Options ====");
                    plugin.getMessageManager().sendMessage(player, "<yellow>Respawn Mode: <white>" + party.getRespawnMode().name());
                    plugin.getMessageManager().sendMessage(player, "<yellow>Respawn Target: <white>" + getRespawnTargetDisplayName(party));
                    plugin.getMessageManager().sendMessage(player, "<gold>====================");
                    plugin.getMessageManager().sendMessage(player, "<gray>Use <white>/ssp party options mode <normal|party_member><gray> to change respawn mode");
                    plugin.getMessageManager().sendMessage(player, "<gray>Use <white>/ssp party options target <player><gray> to set respawn target");
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
        return targetPlayer != null ? targetPlayer.getName() : "Unknown";
    }
}