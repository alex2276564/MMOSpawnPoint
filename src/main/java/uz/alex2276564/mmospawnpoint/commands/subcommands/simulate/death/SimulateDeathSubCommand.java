package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.death;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateContext;

public class SimulateDeathSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("death")
                .permission("mmospawnpoint.simulate.death")
                .description("Simulate death respawn")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .optional(null)
                        .dynamicSuggestions((CommandSender sender, String partial, String[] soFar) ->
                                MMOSpawnPoint.getInstance().getServer().getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(n -> partial == null || n.toLowerCase().startsWith(partial.toLowerCase()))
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

                    SimulateContext.setPrev(target.getUniqueId(), target.getLocation().clone());
                    plugin.getSpawnManager().recordDeathLocation(target, target.getLocation());
                    boolean ok = plugin.getSpawnManager().processDeathSpawn(target);

                    if (ok) {
                        if (sender.equals(target))
                            plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.deathSelf", msg.deathSelf);
                        else
                            plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.deathOther", msg.deathOther, "player", target.getName());
                    } else {
                        plugin.getMessageManager().sendMessageKeyed(sender, "commands.simulate.simulationFailed", msg.simulationFailed);
                    }
                });
    }
}