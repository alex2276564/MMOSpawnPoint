package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.back;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateContext;

public class SimulateBackSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("back")
                .permission("mmospawnpoint.simulate.back")
                .description("Return to location before simulation")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .optional(null)
                        .dynamicSuggestions(partial ->
                                MMOSpawnPoint.getInstance().getServer().getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                                        .toList()))
                .executor((sender, ctx) -> {
                    var plugin = MMOSpawnPoint.getInstance();
                    var msg = plugin.getConfigManager().getMessagesConfig().commands.simulate;

                    Player target = ctx.getArgument("player");
                    if (target == null) {
                        if (!(sender instanceof Player self)) {
                            plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.onlyPlayers", msg.onlyPlayers);
                            return;
                        }
                        target = self;
                    } else if (!sender.hasPermission("mmospawnpoint.simulate.others")) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.noPermission", msg.noPermission);
                        return;
                    }

                    Location prev = SimulateContext.popPrev(target.getUniqueId());
                    if (prev == null) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.backNone", msg.backNone);
                        return;
                    }

                    plugin.getRunner().teleportAsync(target, prev).thenAccept(success -> {
                    });

                    if (sender.equals(target)) plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.backSelf", msg.backSelf);
                    else plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.backOther", msg.backOther, "player", target.getName());
                });
    }
}