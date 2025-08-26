package uz.alex2276564.mmospawnpoint.commands;

import uz.alex2276564.mmospawnpoint.commands.framework.builder.BuiltCommand;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandManager;
import uz.alex2276564.mmospawnpoint.commands.subcommands.cache.CacheSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.help.HelpSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.PartySubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.reload.ReloadSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.setspawnpoint.SetSpawnPointSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateSubCommand;

public class MMOSpawnPointCommands {

    public static BuiltCommand createMMOSpawnPointCommand() {
        CommandBuilder builder = CommandManager.create("mmospawnpoint")
                .permission("mmospawnpoint.command")
                .description("Main MMOSpawnPoint command");

        // Register all subcommands
        new ReloadSubCommand().build(builder);
        new HelpSubCommand().build(builder);
        new PartySubCommand().build(builder);
        new SetSpawnPointSubCommand().build(builder);
        new SimulateSubCommand().build(builder);
        new CacheSubCommand().build(builder);

        return builder.build();
    }
}