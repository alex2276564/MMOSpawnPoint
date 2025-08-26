package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate;

import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.back.SimulateBackSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.death.SimulateDeathSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.join.SimulateJoinSubCommand;

public class SimulateSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder simulate = parent.subcommand("simulate")
                .permission("mmospawnpoint.simulate")
                .description("Simulation tools")
                .executor((sender, ctx) -> {
                    var plugin = MMOSpawnPoint.getInstance();
                    var help = plugin.getConfigManager().getMessagesConfig().commands.simulate;
                    plugin.getMessageManager().sendMessage(sender, help.helpHeader);
                    plugin.getMessageManager().sendMessage(sender, help.helpDeathLine);
                    plugin.getMessageManager().sendMessage(sender, help.helpJoinLine);
                    plugin.getMessageManager().sendMessage(sender, help.helpBackLine);
                });

        new SimulateDeathSubCommand().build(simulate);
        new SimulateJoinSubCommand().build(simulate);
        new SimulateBackSubCommand().build(simulate);

        return simulate;
    }
}