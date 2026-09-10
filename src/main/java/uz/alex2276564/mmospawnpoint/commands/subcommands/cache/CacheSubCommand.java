package uz.alex2276564.mmospawnpoint.commands.subcommands.cache;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.subcommands.cache.clear.CacheClearSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.cache.stats.CacheStatsSubCommand;

public class CacheSubCommand implements SubCommandProvider {

    private final MMOSpawnPointServices services;

    public CacheSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder cache = parent.subcommand("cache")
                .permission("mmospawnpoint.cache")
                .description("Safe-location cache utilities")
                .executor((sender, ctx) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();
                    var help = configManager.getMessagesConfig().commands.cache;

                    messageManager.sendMessageKeyed(sender, "commands.cache.helpHeader", help.helpHeader);
                    messageManager.sendMessageKeyed(sender, "commands.cache.helpStatsLine", help.helpStatsLine);
                    messageManager.sendMessageKeyed(sender, "commands.cache.helpClearLine", help.helpClearLine);
                });

        new CacheStatsSubCommand(services).build(cache);
        new CacheClearSubCommand(services).build(cache);

        return cache;
    }
}