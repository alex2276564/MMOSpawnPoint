package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.teleport;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeleportSubCommand implements NestedSubCommandProvider {
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

                            for (Player online : MMOSpawnPoint.getInstance().getServer().getOnlinePlayers()) {
                                String name = online.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }

                            return out;
                        }))

                .executor(this::execute);
    }

    private void execute(CommandSender sender, uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandContext ctx) {
        MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();
        var msgs = plugin.getConfigManager().getMessagesConfig().commands.spawnpoint.teleport;

        String[] raw = ctx.getRawArgs();

        Player target;
        if (raw.length >= 1) {
            target = Bukkit.getPlayerExact(raw[0]);
            if (target == null) {
                plugin.getMessageManager().sendMessageKeyed(sender,
                        "commands.spawnpoint.set.playerNotFound",
                        plugin.getConfigManager().getMessagesConfig().commands.spawnpoint.set.playerNotFound,
                        "player", raw[0]);
                return;
            }
        } else {
            if (!(sender instanceof Player p)) {
                plugin.getMessageManager().sendMessageKeyed(sender,
                        "commands.spawnpoint.teleport.consoleNeedsPlayer",
                        msgs.consoleNeedsPlayer);
                return;
            }
            target = p;
        }

        PaperLib.getBedSpawnLocationAsync(target, false).thenAccept(loc -> {
            if (loc == null || loc.getWorld() == null) {
                if (sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId())) {
                    plugin.getMessageManager().sendMessageKeyed(sender,
                            "commands.spawnpoint.teleport.noSpawn",
                            msgs.noSpawn);
                } else {
                    plugin.getMessageManager().sendMessageKeyed(sender,
                            "commands.spawnpoint.teleport.noSpawnOther",
                            msgs.noSpawnOther,
                            "player", target.getName());
                }
                return;
            }

            // Load chunk then teleport (Folia-safe)
            PaperLib.getChunkAtAsync(loc, true).thenRun(() -> plugin.getRunner().runAtEntity(target, () ->
                    plugin.getRunner().teleportAsync(target, loc).thenAccept(success -> {
                if (!Boolean.TRUE.equals(success)) return;

                // Сообщения — на entity-треде к Player
                if (sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId())) {
                    plugin.getRunner().runAtEntity(sp, () -> plugin.getMessageManager().sendMessageKeyed(
                            sp, "commands.spawnpoint.teleport.successSelf", msgs.successSelf));
                } else {
                    plugin.getMessageManager().sendMessageKeyed(sender,
                            "commands.spawnpoint.teleport.successOther",
                            msgs.successOther,
                            "player", target.getName());
                    plugin.getMessageManager().sendMessageKeyed(target,
                            "commands.spawnpoint.teleport.targetNotified",
                            msgs.targetNotified);
                }
            })));
        });
    }
}