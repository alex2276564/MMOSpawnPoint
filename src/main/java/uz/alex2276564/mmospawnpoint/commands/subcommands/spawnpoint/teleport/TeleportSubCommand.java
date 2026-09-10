package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.teleport;

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

public class TeleportSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public TeleportSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("teleport")
                .permission("mmospawnpoint.spawnpoint.teleport")
                .description("Teleport to player's bed/anchor spawn")

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
        var runner = services.runner();

        var msgs = configManager.getMessagesConfig().commands.spawnpoint.teleport;

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
                        "commands.spawnpoint.teleport.consoleNeedsPlayer",
                        msgs.consoleNeedsPlayer);
                return;
            }
            target = p;
        }

        PaperLib.getBedSpawnLocationAsync(target, false).thenAccept(loc -> {
            if (loc == null || loc.getWorld() == null) {
                if (sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId())) {
                    messageManager.sendMessageKeyed(sender,
                            "commands.spawnpoint.teleport.noSpawn",
                            msgs.noSpawn);
                } else {
                    messageManager.sendMessageKeyed(sender,
                            "commands.spawnpoint.teleport.noSpawnOther",
                            msgs.noSpawnOther,
                            "player", target.getName());
                }
                return;
            }

            // Load chunk then teleport (Folia-safe)
            PaperLib.getChunkAtAsync(loc, true).thenRun(() ->
                    runner.runAtEntity(target, () ->
                            runner.teleportAsync(target, loc).thenAccept(success -> {
                                if (!Boolean.TRUE.equals(success)) return;

                                if (sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId())) {
                                    runner.runAtEntity(sp, () -> messageManager.sendMessageKeyed(
                                            sp,
                                            "commands.spawnpoint.teleport.successSelf",
                                            msgs.successSelf
                                    ));
                                } else {
                                    messageManager.sendMessageKeyed(sender,
                                            "commands.spawnpoint.teleport.successOther",
                                            msgs.successOther,
                                            "player", target.getName());
                                    messageManager.sendMessageKeyed(target,
                                            "commands.spawnpoint.teleport.targetNotified",
                                            msgs.targetNotified);
                                }
                            })));
        });
    }
}