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

                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.help.header", help.header);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.help.reloadLine", help.reloadLine);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.help.partyLine", help.partyLine);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.help.simulateLine", help.simulateLine);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.help.cacheLine", help.cacheLine);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.help.setspawnpointLine", help.setspawnpointLine);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.help.helpLine", help.helpLine);
                });
    }
}