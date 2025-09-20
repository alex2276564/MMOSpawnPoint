package uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.set;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.spawnpoint.SpawnpointFlags;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SetSubCommand implements NestedSubCommandProvider {
    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("set")
                .permission("mmospawnpoint.spawnpoint.set")
                .description("Set player's bed/anchor spawn with conditions")

                // ARG 0: player_or_world_or_flag
                .argument(new ArgumentBuilder<>("player_or_world_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            // Flags
                            if ("--if-has".startsWith(p)) out.add("--if-has");
                            if ("--if-missing".startsWith(p)) out.add("--if-missing");
                            if ("--only-if-incorrect".startsWith(p)) out.add("--only-if-incorrect");
                            if ("--require-valid-bed".startsWith(p)) out.add("--require-valid-bed");
                            if ("--dry-run".startsWith(p)) out.add("--dry-run");

                            // Игроки
                            for (Player online : MMOSpawnPoint.getInstance().getServer().getOnlinePlayers()) {
                                String name = online.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }

                            // Миры
                            for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                String name = w.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }
                            return out;
                        }))

                // ARG 1: world_or_x_or_flag
                .argument(new ArgumentBuilder<>("world_or_x_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            // Flags
                            if ("--if-has".startsWith(p)) out.add("--if-has");
                            if ("--if-missing".startsWith(p)) out.add("--if-missing");
                            if ("--only-if-incorrect".startsWith(p)) out.add("--only-if-incorrect");
                            if ("--require-valid-bed".startsWith(p)) out.add("--require-valid-bed");
                            if ("--dry-run".startsWith(p)) out.add("--dry-run");

                            // Worlds or coordinates
                            for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                String name = w.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }

                            // X coordinates
                            if (sender instanceof Player pl) {
                                out.add(String.format(Locale.US, "%.1f", pl.getLocation().getX()));
                            }
                            out.add("0");
                            out.add("100");
                            out.add("-100");

                            return out;
                        }))

                // ARG 2: x_or_y_or_flag
                .argument(new ArgumentBuilder<>("x_or_y_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            // Flags
                            if ("--if-has".startsWith(p)) out.add("--if-has");
                            if ("--if-missing".startsWith(p)) out.add("--if-missing");
                            if ("--only-if-incorrect".startsWith(p)) out.add("--only-if-incorrect");
                            if ("--require-valid-bed".startsWith(p)) out.add("--require-valid-bed");
                            if ("--dry-run".startsWith(p)) out.add("--dry-run");

                            // Coordinates
                            if (sender instanceof Player pl) {
                                out.add(String.format(Locale.US, "%.1f", pl.getLocation().getX()));
                                out.add(String.format(Locale.US, "%.1f", pl.getLocation().getY()));
                            }
                            out.add("0");
                            out.add("64");
                            out.add("100");

                            return out;
                        }))

                // ARG 3: y_or_z_or_flag
                .argument(new ArgumentBuilder<>("y_or_z_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            // Flags
                            if ("--if-has".startsWith(p)) out.add("--if-has");
                            if ("--if-missing".startsWith(p)) out.add("--if-missing");
                            if ("--only-if-incorrect".startsWith(p)) out.add("--only-if-incorrect");
                            if ("--require-valid-bed".startsWith(p)) out.add("--require-valid-bed");
                            if ("--dry-run".startsWith(p)) out.add("--dry-run");

                            // Coordinates
                            if (sender instanceof Player pl) {
                                out.add(String.format(Locale.US, "%.1f", pl.getLocation().getY()));
                                out.add(String.format(Locale.US, "%.1f", pl.getLocation().getZ()));
                            }
                            out.add("64");
                            out.add("80");
                            out.add("100");

                            return out;
                        }))

                // ARG 4: z_or_yaw_or_flag
                .argument(new ArgumentBuilder<>("z_or_yaw_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            // Flags
                            if ("--if-has".startsWith(p)) out.add("--if-has");
                            if ("--if-missing".startsWith(p)) out.add("--if-missing");
                            if ("--only-if-incorrect".startsWith(p)) out.add("--only-if-incorrect");
                            if ("--require-valid-bed".startsWith(p)) out.add("--require-valid-bed");
                            if ("--dry-run".startsWith(p)) out.add("--dry-run");

                            // Coordinates Z или Yaw
                            if (sender instanceof Player pl) {
                                out.add(String.format(Locale.US, "%.1f", pl.getLocation().getZ()));
                                out.add(String.format(Locale.US, "%.0f", pl.getLocation().getYaw()));
                            }
                            out.add("0");
                            out.add("90");
                            out.add("180");
                            out.add("270");

                            return out;
                        }))

                // ARG 5+: other arguments and flags
                .argument(new ArgumentBuilder<>("yaw_or_pitch_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            // Flags
                            if ("--if-has".startsWith(p)) out.add("--if-has");
                            if ("--if-missing".startsWith(p)) out.add("--if-missing");
                            if ("--only-if-incorrect".startsWith(p)) out.add("--only-if-incorrect");
                            if ("--require-valid-bed".startsWith(p)) out.add("--require-valid-bed");
                            if ("--dry-run".startsWith(p)) out.add("--dry-run");

                            // Yaw/Pitch
                            if (sender instanceof Player pl) {
                                out.add(String.format(Locale.US, "%.0f", pl.getLocation().getYaw()));
                                out.add(String.format(Locale.US, "%.0f", pl.getLocation().getPitch()));
                            }
                            out.add("0");
                            out.add("-30");
                            out.add("30");

                            return out;
                        }))

                .argument(new ArgumentBuilder<>("extra_flags", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();

                            // Only Flags
                            if ("--if-has".startsWith(p)) out.add("--if-has");
                            if ("--if-missing".startsWith(p)) out.add("--if-missing");
                            if ("--only-if-incorrect".startsWith(p)) out.add("--only-if-incorrect");
                            if ("--require-valid-bed".startsWith(p)) out.add("--require-valid-bed");
                            if ("--dry-run".startsWith(p)) out.add("--dry-run");

                            return out;
                        }))

                .executor(this::execute);
    }

    private void execute(CommandSender sender, uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandContext ctx) {
        MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();
        var msgs = plugin.getConfigManager().getMessagesConfig().commands.spawnpoint.set;

        String[] raw = ctx.getRawArgs();
        SpawnpointFlags flags = SpawnpointFlags.parse(raw);
        try {
            flags.validateMutual();
        } catch (IllegalArgumentException ex) {
            plugin.getMessageManager().sendMessage(sender, "<red>" + ex.getMessage());
            return;
        }
        String[] pos = SpawnpointFlags.stripFlags(raw);


        int i;
        Player target;
        World world;

        Player p0 = Bukkit.getPlayerExact(pos[0]);
        World  w0 = Bukkit.getWorld(pos[0]);

        if (p0 != null) {
            // expected: [player] <world> <x> <y> <z> [yaw] [pitch]
            if (pos.length < 6) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.consoleUsage", msgs.consoleUsage);
                return;
            }
            target = p0;
            world = Bukkit.getWorld(pos[1]);
            if (world == null) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.invalidWorld", msgs.invalidWorld, "world", pos[1]);
                return;
            }
            i = 2;
        } else if (w0 != null) {
            // expected: <world> <x> <y> <z> [yaw] [pitch] (only if sender is a player)
            if (!(sender instanceof Player self)) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.consoleUsage", msgs.consoleUsage);
                return;
            }
            if (pos.length < 4) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.consoleUsage", msgs.consoleUsage);
                return;
            }
            target = self;
            world  = w0;
            i = 1;
        } else {
            // first token — player name (not online) or garbage → show normal error
            plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.playerNotFound", msgs.playerNotFound, "player", pos[0]);
            return;
        }

        // parse coords
        Location loc = parseLocation(world, pos, i);
        if (loc == null) {
            plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.invalidCoords", msgs.invalidCoords);
            return;
        }

        // Check current bed spawn asynchronously
        PaperLib.getBedSpawnLocationAsync(target, false).thenAccept(curr -> {
            // Conditions
            if (flags.ifHas && curr == null) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.skippedIfHas", msgs.skippedIfHas);
                return;
            }
            if (flags.ifMissing && curr != null) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.skippedIfMissing", msgs.skippedIfMissing);
                return;
            }
            if (flags.onlyIfIncorrect && isSameSpawn(curr, loc)) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.skippedIfCorrect", msgs.skippedIfCorrect);
                return;
            }

            String locationStr = fmt(loc);

            if (flags.dryRun) {
                plugin.getMessageManager().sendMessageKeyed(
                        sender, "commands.spawnpoint.set.dryRun", msgs.dryRun,
                        Map.of("player", target.getName(), "location", locationStr)
                );
                return;
            }

            boolean force = !flags.requireValidBed;

            PaperLib.getChunkAtAsync(loc, true).thenRun(() -> plugin.getRunner().runAtEntity(target, () -> {
                try {
                    target.setBedSpawnLocation(loc, force);
                } catch (Throwable t) {
                    plugin.getMessageManager().sendMessageKeyed(
                            sender, "commands.spawnpoint.set.error", msgs.error,
                            "error", (t.getMessage() == null ? "unknown" : t.getMessage())
                    );
                    return;
                }

                // Re-check the effective bed spawn for honest feedback
                PaperLib.getBedSpawnLocationAsync(target, false).thenAccept(after -> {
                    if (flags.requireValidBed && after == null) {
                        plugin.getMessageManager().sendMessageKeyed(sender,
                                "commands.spawnpoint.set.skippedNoValidBed", msgs.skippedNoValidBed);
                        return;
                    }

                    // feedback to sender
                    if (sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(
                                sp, "commands.spawnpoint.set.selfSuccess", msgs.selfSuccess,
                                Map.of("location", locationStr)
                        );
                    } else {
                        plugin.getMessageManager().sendMessageKeyed(
                                sender, "commands.spawnpoint.set.otherSuccess", msgs.otherSuccess,
                                Map.of("player", target.getName(), "location", locationStr)
                        );
                        plugin.getRunner().runAtEntity(target, () -> plugin.getMessageManager().sendMessageKeyed(
                                target, "commands.spawnpoint.set.targetNotification", msgs.targetNotification,
                                Map.of("setter", sender.getName(), "location", locationStr)
                        ));
                    }
                }).exceptionally(ex -> {
                    plugin.getMessageManager().sendMessageKeyed(
                            sender, "commands.spawnpoint.set.error", msgs.error,
                            "error", ex.getMessage()
                    );
                    return null;
                });
            }));
        }).exceptionally(ex -> {
            plugin.getMessageManager().sendMessageKeyed(sender, "commands.spawnpoint.set.error", msgs.error, "error", ex.getMessage());
            return null;
        });
    }

    private static Location parseLocation(World world, String[] pos, int index) {
        try {
            if (pos.length < index + 3) return null;
            double x = Double.parseDouble(pos[index]);
            double y = Double.parseDouble(pos[index + 1]);
            double z = Double.parseDouble(pos[index + 2]);
            float yaw = 0f;
            float pitch = 0f;
            if (pos.length >= index + 4) yaw = Float.parseFloat(pos[index + 3]);
            if (pos.length >= index + 5) pitch = Float.parseFloat(pos[index + 4]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isSameSpawn(Location a, Location b) {
        if (a == null || b == null) return false;
        if (a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().getName().equals(b.getWorld().getName())) return false;
        // Compare by block to avoid floating precision issues
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private static String fmt(Location loc) {
        return String.format(Locale.US, "%.1f, %.1f, %.1f in %s (yaw=%.0f, pitch=%.0f)",
                loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName(), loc.getYaw(), loc.getPitch());
    }
}