package uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.back;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.simulate.SimulateContext;

public class SimulateBackSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public SimulateBackSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("back")
                .permission("mmospawnpoint.simulate.back")
                .description("Return to location before simulation")
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
                    var runner = services.runner();
                    var msg = configManager.getMessagesConfig().commands.simulate;

                    Player target = ctx.getArgument("player");
                    if (target == null) {
                        if (!(sender instanceof Player self)) {
                            messageManager.sendMessageKeyed(sender,
                                    "commands.simulate.onlyPlayers", msg.onlyPlayers);
                            return;
                        }
                        target = self;
                    } else if (!sender.hasPermission("mmospawnpoint.simulate.others")) {
                        messageManager.sendMessageKeyed(sender,
                                "commands.simulate.noPermission", msg.noPermission);
                        return;
                    }

                    Location prev = SimulateContext.popPrev(target.getUniqueId());
                    if (prev == null) {
                        messageManager.sendMessageKeyed(sender,
                                "commands.simulate.backNone", msg.backNone);
                        return;
                    }

                    runner.teleportAsync(target, prev).thenAccept(success -> {
                    });

                    if (sender.equals(target)) {
                        messageManager.sendMessageKeyed(sender,
                                "commands.simulate.backSelf", msg.backSelf);
                    } else {
                        messageManager.sendMessageKeyed(sender,
                                "commands.simulate.backOther", msg.backOther,
                                "player", target.getName());
                    }
                });
    }
}