package uz.alex2276564.mmospawnpoint.commands.subcommands.cache.clear;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

public class CacheClearSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public CacheClearSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("clear")
                .permission("mmospawnpoint.cache.clear")
                .description("Clear cache (all or for a specific player)")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .optional(null)
                        .dynamicSuggestions((CommandSender sender, String partial, String[] soFar) ->
                                Bukkit.getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(n -> partial == null || n.toLowerCase().startsWith(partial.toLowerCase()))
                                        .toList()))
                .executor((sender, ctx) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();
                    var msgs = configManager.getMessagesConfig().commands.cache;

                    Player p = ctx.getArgument("player");
                    if (p == null) {
                        SafeLocationFinder.clearCache();
                        messageManager.sendMessageKeyed(sender, "commands.cache.clearedAll", msgs.clearedAll);
                    } else {
                        SafeLocationFinder.clearPlayerCache(p.getUniqueId());
                        messageManager.sendMessageKeyed(sender, "commands.cache.clearedPlayer", msgs.clearedPlayer, "player", p.getName());
                    }
                });
    }
}