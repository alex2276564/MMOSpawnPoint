package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.join;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateContext;

public class SimulateJoinSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public SimulateJoinSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("join")
                .permission("mmospawnpoint.simulate.join")
                .description("Simulate join teleport")
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
                    var spawnManager = services.spawnManager();
                    var msg = configManager.getMessagesConfig().commands.simulate;

                    Player target = ctx.getArgument("player");
                    if (target == null) {
                        if (!(sender instanceof Player self)) {
                            messageManager.sendMessageKeyed(sender, "commands.simulate.onlyPlayers", msg.onlyPlayers);
                            return;
                        }
                        target = self;
                    } else if (!sender.hasPermission("mmospawnpoint.simulate.others")) {
                        messageManager.sendMessageKeyed(sender, "commands.simulate.noPermission", msg.noPermission);
                        return;
                    }

                    SimulateContext.setPrev(target.getUniqueId(), target.getLocation().clone());
                    boolean ok = spawnManager.processJoinSpawn(target);

                    if (ok) {
                        if (sender.equals(target)) {
                            messageManager.sendMessageKeyed(sender, "commands.simulate.joinSelf", msg.joinSelf);
                        } else {
                            messageManager.sendMessageKeyed(sender, "commands.simulate.joinOther",
                                    msg.joinOther, "player", target.getName());
                        }
                    } else {
                        messageManager.sendMessageKeyed(sender, "commands.simulate.simulationFailed", msg.simulationFailed);
                    }
                });
    }
}