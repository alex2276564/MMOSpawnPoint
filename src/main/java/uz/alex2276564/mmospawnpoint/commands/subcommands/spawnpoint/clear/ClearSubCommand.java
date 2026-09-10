package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.clear;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.SpawnpointFlags;

import java.util.*;

public class ClearSubCommand implements NestedSubCommandProvider {

    private static final String[] FLAGS = {"--if-has", "--dry-run"};

    private final MMOSpawnPointServices services;

    public ClearSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("clear")
                .permission("mmospawnpoint.spawnpoint.clear")
                .description("Clear player's bed/anchor spawn")

                .argument(new ArgumentBuilder<>("player_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            addFlagSuggestions(out, partial, soFar);

                            String p = (partial == null ? "" : partial.toLowerCase(Locale.ROOT));
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                String name = online.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }
                            return out;
                        }))

                .argument(new ArgumentBuilder<>("extra_flags", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            addFlagSuggestions(out, partial, soFar);
                            return out;
                        }))

                .executor(this::execute);
    }

    private static void addFlagSuggestions(List<String> out, String partial, String[] soFar) {
        String p = (partial == null ? "" : partial.toLowerCase(Locale.ROOT));
        Set<String> used = new HashSet<>();
        if (soFar != null) {
            for (String s : soFar) {
                if (s != null && s.startsWith("--")) used.add(s.toLowerCase(Locale.ROOT));
            }
        }
        for (String f : FLAGS) {
            if (!used.contains(f) && f.startsWith(p)) out.add(f);
        }
    }

    private void execute(CommandSender sender,
                         uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandContext ctx) {

        var configManager = services.configManager();
        var messageManager = services.messageManager();
        var runner = services.runner();

        var msgs = configManager.getMessagesConfig().commands.spawnpoint.clear;

        String[] raw = ctx.getRawArgs();
        SpawnpointFlags flags = SpawnpointFlags.parse(raw);
        String[] pos = SpawnpointFlags.stripFlags(raw);

        Player target;
        if (pos.length >= 1) {
            target = Bukkit.getPlayerExact(pos[0]);
            if (target == null) {
                messageManager.sendMessageKeyed(sender,
                        "commands.spawnpoint.set.playerNotFound",
                        configManager.getMessagesConfig().commands.spawnpoint.set.playerNotFound,
                        "player", pos[0]);
                return;
            }
        } else {
            if (!(sender instanceof Player p)) {
                messageManager.sendMessageKeyed(sender,
                        "commands.spawnpoint.clear.consoleNeedsPlayer",
                        msgs.consoleNeedsPlayer);
                return;
            }
            target = p;
        }

        PaperLib.getBedSpawnLocationAsync(target, false).thenAccept(curr -> {
            if (flags.ifHas && curr == null) {
                messageManager.sendMessageKeyed(sender,
                        "commands.spawnpoint.clear.noSpawn", msgs.noSpawn);
                return;
            }

            if (flags.dryRun) {
                messageManager.sendMessageKeyed(sender,
                        "commands.spawnpoint.clear.dryRun", msgs.dryRun,
                        Map.of("player", target.getName()));
                return;
            }

            runner.runAtEntity(target, () -> {
                try {
                    target.setBedSpawnLocation(null, true);
                } catch (Throwable t) {
                    messageManager.sendMessageKeyed(sender,
                            "commands.spawnpoint.clear.failed", msgs.failed,
                            Map.of("player", target.getName()));
                    return;
                }

                PaperLib.getBedSpawnLocationAsync(target, false).thenAccept(after -> {
                    if (after != null) {
                        messageManager.sendMessageKeyed(sender,
                                "commands.spawnpoint.clear.failed", msgs.failed,
                                Map.of("player", target.getName()));
                        return;
                    }

                    if (sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId())) {
                        messageManager.sendMessageKeyed(
                                sp, "commands.spawnpoint.clear.successSelf", msgs.successSelf);
                    } else {
                        messageManager.sendMessageKeyed(sender,
                                "commands.spawnpoint.clear.successOther", msgs.successOther,
                                Map.of("player", target.getName()));
                        messageManager.sendMessageKeyed(
                                target, "commands.spawnpoint.clear.targetNotified", msgs.targetNotified,
                                Map.of("setter", sender.getName())
                        );
                    }
                });
            });
        });
    }
}