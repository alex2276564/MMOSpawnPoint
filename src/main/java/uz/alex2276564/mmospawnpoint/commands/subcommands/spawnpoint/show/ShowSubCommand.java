package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.show;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShowSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public ShowSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("show")
                .permission("mmospawnpoint.spawnpoint.show")
                .description("Show player's bed/anchor spawn")

                .argument(new ArgumentBuilder<>("player", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            for (Player online : Bukkit.getOnlinePlayers()) {
                                String name = online.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }

                            return out;
                        }))

                .executor(this::execute);
    }

    private void execute(CommandSender sender,
                         uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandContext ctx) {

        var configManager = services.configManager();
        var messageManager = services.messageManager();

        var msgs = configManager.getMessagesConfig().commands.spawnpoint.show;

        String[] raw = ctx.getRawArgs();

        Player target;
        if (raw.length >= 1) {
            target = Bukkit.getPlayerExact(raw[0]);
            if (target == null) {
                messageManager.sendMessageKeyed(sender,
                        "commands.spawnpoint.set.playerNotFound",
                        configManager.getMessagesConfig().commands.spawnpoint.set.playerNotFound,
                        "player", raw[0]);
                return;
            }
        } else {
            if (!(sender instanceof Player p)) {
                messageManager.sendMessageKeyed(sender,
                        "commands.spawnpoint.show.consoleNeedsPlayer",
                        msgs.consoleNeedsPlayer);
                return;
            }
            target = p;
        }

        PaperLib.getBedSpawnLocationAsync(target, false).thenAccept(loc -> {
            if (loc == null || loc.getWorld() == null) {
                if (sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId())) {
                    messageManager.sendMessageKeyed(sender,
                            "commands.spawnpoint.show.noSpawn", msgs.noSpawn);
                } else {
                    messageManager.sendMessageKeyed(sender,
                            "commands.spawnpoint.show.noSpawnOther", msgs.noSpawnOther,
                            "player", target.getName());
                }
                return;
            }

            String coords = String.format(Locale.US, "%.1f, %.1f, %.1f",
                    loc.getX(), loc.getY(), loc.getZ());
            String world = loc.getWorld().getName();

            messageManager.sendMessageKeyed(sender,
                    "commands.spawnpoint.show.line", msgs.line,
                    Map.of(
                            "player", target.getName(),
                            "coords", coords,
                            "world", world
                    ));
        });
    }
}