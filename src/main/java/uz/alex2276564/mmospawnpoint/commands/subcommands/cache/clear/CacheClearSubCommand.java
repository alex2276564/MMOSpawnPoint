package uz.alex2276564.mmospawnpoint.commands.subcommands.cache.clear;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

public class CacheClearSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("clear")
                .permission("mmospawnpoint.cache.clear")
                .description("Clear cache (all or for a specific player)")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .optional(null)
                        .dynamicSuggestions(partial ->
                                MMOSpawnPoint.getInstance().getServer().getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                                        .toList()))
                .executor((sender, ctx) -> {
                    var plugin = MMOSpawnPoint.getInstance();
                    var msg = plugin.getConfigManager().getMessagesConfig().commands.cache;

                    Player p = ctx.getArgument("player");
                    if (p == null) {
                        SafeLocationFinder.clearCache();
                        plugin.getMessageManager().sendMessageKeyed(sender, "commands.cache.clearedAll", msg.clearedAll);
                    } else {
                        SafeLocationFinder.clearPlayerCache(p.getUniqueId());
                        plugin.getMessageManager().sendMessageKeyed(sender, "commands.cache.clearedPlayer", msg.clearedPlayer, "player", p.getName());
                    }
                });
    }
}