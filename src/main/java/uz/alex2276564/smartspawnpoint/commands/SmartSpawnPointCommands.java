package uz.alex2276564.smartspawnpoint.commands;

import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.commands.subcommands.reload.ReloadSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.help.HelpSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.PartySubCommand;

public class SmartSpawnPointCommands {

    public static BuiltCommand createSmartSpawnPointCommand() {
        CommandBuilder builder = CommandManager.create("smartspawnpoint")
                .permission("smartspawnpoint.command")
                .description("Main SmartSpawnPoint command");

        // Register all subcommands
        new ReloadSubCommand().build(builder);
        new HelpSubCommand().build(builder);
        new PartySubCommand().build(builder);

        return builder.build();
    }
}