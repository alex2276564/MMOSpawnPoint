package uz.alex2276564.mmospawnpoint.commands.subcommands.help;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;

public class HelpSubCommand implements SubCommandProvider {

    private final MMOSpawnPointServices services;

    public HelpSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("help")
                .permission("mmospawnpoint.command")
                .description("Show help information")
                .executor((sender, context) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();

                    var help = configManager.getMessagesConfig().commands.help;

                    messageManager.sendMessageKeyed(sender, "commands.help.header", help.header);
                    messageManager.sendMessageKeyed(sender, "commands.help.reloadLine", help.reloadLine);
                    messageManager.sendMessageKeyed(sender, "commands.help.partyLine", help.partyLine);
                    messageManager.sendMessageKeyed(sender, "commands.help.simulateLine", help.simulateLine);
                    messageManager.sendMessageKeyed(sender, "commands.help.cacheLine", help.cacheLine);
                    messageManager.sendMessageKeyed(sender, "commands.help.spawnpointLine", help.spawnpointLine);
                    messageManager.sendMessageKeyed(sender, "commands.help.helpLine", help.helpLine);
                });
    }
}