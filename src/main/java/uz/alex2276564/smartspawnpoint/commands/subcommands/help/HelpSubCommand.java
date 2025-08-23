package uz.alex2276564.smartspawnpoint.commands.subcommands.help;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandProvider;

public class HelpSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("help")
                .permission("smartspawnpoint.command")
                .description("Show help information")
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

                    var messages = plugin.getConfigManager().getMessagesConfig().commands.help;
                    plugin.getMessageManager().sendMessage(sender, messages.header);
                    plugin.getMessageManager().sendMessage(sender, messages.reloadLine);

                    if (plugin.getConfigManager().getMainConfig().party.enabled) {
                        plugin.getMessageManager().sendMessage(sender, messages.partyLine);
                    }

                    plugin.getMessageManager().sendMessage(sender, messages.helpLine);
                });
    }
}