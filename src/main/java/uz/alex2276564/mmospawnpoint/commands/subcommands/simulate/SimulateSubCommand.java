package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.back.SimulateBackSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.death.SimulateDeathSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.join.SimulateJoinSubCommand;

public class SimulateSubCommand implements SubCommandProvider {

    private final MMOSpawnPointServices services;

    public SimulateSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder simulate = parent.subcommand("simulate")
                .permission("mmospawnpoint.simulate")
                .description("Simulation tools")
                .executor((sender, ctx) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();
                    var help = configManager.getMessagesConfig().commands.simulate;

                    messageManager.sendMessageKeyed(sender, "commands.simulate.helpHeader", help.helpHeader);
                    messageManager.sendMessageKeyed(sender, "commands.simulate.helpDeathLine", help.helpDeathLine);
                    messageManager.sendMessageKeyed(sender, "commands.simulate.helpJoinLine", help.helpJoinLine);
                    messageManager.sendMessageKeyed(sender, "commands.simulate.helpBackLine", help.helpBackLine);
                });

        new SimulateDeathSubCommand(services).build(simulate);
        new SimulateJoinSubCommand(services).build(simulate);
        new SimulateBackSubCommand(services).build(simulate);

        return simulate;
    }
}