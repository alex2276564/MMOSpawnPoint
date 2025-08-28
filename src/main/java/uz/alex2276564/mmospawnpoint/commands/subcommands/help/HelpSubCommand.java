package uz.alex2276564.mmospawnpoint.commands.subcommands.help;

import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;

public class HelpSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("help")
                .permission("mmospawnpoint.command")
                .description("Show help information")
                .executor((sender, context) -> {
                    var plugin = MMOSpawnPoint.getInstance();
                    var help = plugin.getConfigManager().getMessagesConfig().commands.help;

                    plugin.getMessageManager().sendMessage(sender, help.header);
                    plugin.getMessageManager().sendMessage(sender, help.reloadLine);
                    plugin.getMessageManager().sendMessage(sender, help.partyLine);
                    plugin.getMessageManager().sendMessage(sender, help.simulateLine);
                    plugin.getMessageManager().sendMessage(sender, help.cacheLine);
                    plugin.getMessageManager().sendMessage(sender, help.setspawnpointLine);
                    plugin.getMessageManager().sendMessage(sender, help.helpLine);
                });
    }
}