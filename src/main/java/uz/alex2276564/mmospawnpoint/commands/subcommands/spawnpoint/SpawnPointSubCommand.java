package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint;

import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.clear.ClearSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.set.SetSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.show.ShowSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.teleport.TeleportSubCommand;

public class SpawnPointSubCommand implements SubCommandProvider {
    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder sp = parent.subcommand("spawnpoint")
                .permission("mmospawnpoint.spawnpoint")
                .description("Bed/anchor spawn management")
                .executor((sender, ctx) -> {
                    var m = MMOSpawnPoint.getInstance().getConfigManager().getMessagesConfig().commands.spawnpoint.help;
                    var mm = MMOSpawnPoint.getInstance().getMessageManager();
                    mm.sendMessageKeyed(sender, "commands.spawnpoint.help.header", m.header);
                    mm.sendMessageKeyed(sender, "commands.spawnpoint.help.setLine", m.setLine);
                    mm.sendMessageKeyed(sender, "commands.spawnpoint.help.clearLine", m.clearLine);
                    mm.sendMessageKeyed(sender, "commands.spawnpoint.help.teleportLine", m.teleportLine);
                    mm.sendMessageKeyed(sender, "commands.spawnpoint.help.showLine", m.showLine);
                });

        new SetSubCommand().build(sp);
        new ClearSubCommand().build(sp);
        new TeleportSubCommand().build(sp);
        new ShowSubCommand().build(sp);

        return sp;
    }
}