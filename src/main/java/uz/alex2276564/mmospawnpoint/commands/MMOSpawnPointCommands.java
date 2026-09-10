package uz.alex2276564.mmospawnpoint.commands;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.BuiltCommand;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandManager;
import uz.alex2276564.mmospawnpoint.commands.subcommands.cache.CacheSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.help.HelpSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.PartySubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.reload.ReloadSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.SpawnPointSubCommand;

public class MMOSpawnPointCommands {

    public static BuiltCommand createMMOSpawnPointCommand(MMOSpawnPointServices services) {
        CommandBuilder builder = CommandManager.create("mmospawnpoint")
                .permission("mmospawnpoint.command")
                .description("Main MMOSpawnPoint command");

        // Register all subcommands
        new ReloadSubCommand(services).build(builder);
        new HelpSubCommand(services).build(builder);
        new PartySubCommand(services).build(builder);
        new SpawnPointSubCommand(services).build(builder);
        new SimulateSubCommand(services).build(builder);
        new CacheSubCommand(services).build(builder);

        return builder.build();
    }
}