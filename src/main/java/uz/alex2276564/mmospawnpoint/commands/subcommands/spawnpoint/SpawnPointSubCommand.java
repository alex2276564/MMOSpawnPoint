package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.clear.ClearSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.set.SetSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.show.ShowSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.teleport.TeleportSubCommand;

public class SpawnPointSubCommand implements SubCommandProvider {

    private final MMOSpawnPointServices services;

    public SpawnPointSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder sp = parent.subcommand("spawnpoint")
                .permission("mmospawnpoint.spawnpoint")
                .description("Bed/anchor spawn management")
                .executor((sender, ctx) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();

                    var m = configManager.getMessagesConfig().commands.spawnpoint.help;
                    messageManager.sendMessageKeyed(sender, "commands.spawnpoint.help.header", m.header);
                    messageManager.sendMessageKeyed(sender, "commands.spawnpoint.help.setLine", m.setLine);
                    messageManager.sendMessageKeyed(sender, "commands.spawnpoint.help.clearLine", m.clearLine);
                    messageManager.sendMessageKeyed(sender, "commands.spawnpoint.help.teleportLine", m.teleportLine);
                    messageManager.sendMessageKeyed(sender, "commands.spawnpoint.help.showLine", m.showLine);
                });

        new SetSubCommand(services).build(sp);
        new ClearSubCommand(services).build(sp);
        new TeleportSubCommand(services).build(sp);
        new ShowSubCommand(services).build(sp);

        return sp;
    }
}