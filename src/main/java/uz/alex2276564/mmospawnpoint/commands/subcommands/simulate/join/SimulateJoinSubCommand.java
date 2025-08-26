package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.join;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateContext;

public class SimulateJoinSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("join")
                .permission("mmospawnpoint.simulate.join")
                .description("Simulate join teleport")
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
                            plugin.getMessageManager().sendMessage(sender, msg.onlyPlayers);
                            return;
                        }
                        target = self;
                    } else if (!sender.hasPermission("mmospawnpoint.simulate.others")) {
                        plugin.getMessageManager().sendMessage(sender, msg.noPermission);
                        return;
                    }

                    SimulateContext.setPrev(target.getUniqueId(), target.getLocation().clone());
                    boolean ok = plugin.getSpawnManager().processJoinSpawn(target);

                    if (ok) {
                        if (sender.equals(target)) plugin.getMessageManager().sendMessage(sender, msg.joinSelf);
                        else plugin.getMessageManager().sendMessage(sender, msg.joinOther, "player", target.getName());
                    } else {
                        plugin.getMessageManager().sendMessage(sender, msg.simulationFailed);
                    }
                });
    }
}