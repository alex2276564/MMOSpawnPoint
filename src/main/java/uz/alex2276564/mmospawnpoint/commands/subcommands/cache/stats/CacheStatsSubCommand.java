package uz.alex2276564.mmospawnpoint.commands.subcommands.cache.stats;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

public class CacheStatsSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public CacheStatsSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("stats")
                .permission("mmospawnpoint.cache.stats")
                .description("Show cache statistics")
                .executor((sender, ctx) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();
                    var msg = configManager.getMessagesConfig().commands.cache;

                    var snap = SafeLocationFinder.SafeLocationFinderExports.snapshot();
                    double rate = snap.searches() > 0 ? (snap.hits() * 100.0) / snap.searches() : 0.0;

                    String line = msg.statsLine
                            .replace("<searches>", String.valueOf(snap.searches()))
                            .replace("<hits>", String.valueOf(snap.hits()))
                            .replace("<misses>", String.valueOf(snap.misses()))
                            .replace("<hitRate>", String.format("%.1f", rate))
                            .replace("<size>", String.valueOf(snap.size()))
                            .replace("<enabled>", String.valueOf(snap.enabled()))
                            .replace("<expiry>", String.valueOf(snap.expirySeconds()))
                            .replace("<max>", String.valueOf(snap.maxSize()));

                    messageManager.sendMessageKeyed(sender, "commands.cache.statsLine", line);
                });
    }
}