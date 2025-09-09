package uz.alex2276564.mmospawnpoint.commands.subcommands.cache;

import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.subcommands.cache.clear.CacheClearSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.cache.stats.CacheStatsSubCommand;

public class CacheSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder cache = parent.subcommand("cache")
                .permission("mmospawnpoint.cache")
                .description("Safe-location cache utilities")
                .executor((sender, ctx) -> {
                    var plugin = MMOSpawnPoint.getInstance();
                    var help = plugin.getConfigManager().getMessagesConfig().commands.cache;
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.cache.helpHeader", help.helpHeader);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.cache.helpStatsLine", help.helpStatsLine);
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.cache.helpClearLine", help.helpClearLine);
                });

        new CacheStatsSubCommand().build(cache);
        new CacheClearSubCommand().build(cache);

        return cache;
    }
}