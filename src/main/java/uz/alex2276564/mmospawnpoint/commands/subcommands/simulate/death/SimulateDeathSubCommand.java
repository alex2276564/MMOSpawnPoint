package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.death;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateContext;

public class SimulateDeathSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public SimulateDeathSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("death")
                .permission("mmospawnpoint.simulate.death")
                .description("Simulate death respawn")
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
                    spawnManager.recordDeathLocation(target, target.getLocation());
                    boolean ok = spawnManager.processDeathSpawn(target);

                    if (ok) {
                        if (sender.equals(target)) {
                            messageManager.sendMessageKeyed(sender, "commands.simulate.deathSelf", msg.deathSelf);
                        } else {
                            messageManager.sendMessageKeyed(sender, "commands.simulate.deathOther",
                                    msg.deathOther, "player", target.getName());
                        }
                    } else {
                        messageManager.sendMessageKeyed(sender, "commands.simulate.simulationFailed", msg.simulationFailed);
                    }
                });
    }
}