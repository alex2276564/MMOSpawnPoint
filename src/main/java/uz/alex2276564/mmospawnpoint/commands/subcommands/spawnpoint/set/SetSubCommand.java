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

import java.util.*;

public class SetSubCommand implements NestedSubCommandProvider {

    private enum Mode { NONE, PLAYER, WORLD }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("set")
                .permission("mmospawnpoint.spawnpoint.set")
                .description("Set player's bed/anchor spawn with conditions")

                // ARG 0: player | world | flag
                .argument(new ArgumentBuilder<>("player_or_world_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            // flags (filtered)
                            addFlagSuggestions(out, partial, soFar);

                            String p = norm(partial);
                            // players
                            for (Player online : MMOSpawnPoint.getInstance().getServer().getOnlinePlayers()) {
                                String name = online.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }
                            // worlds
                            for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                String name = w.getName();
                                if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                            }
                            return out;
                        }))

                // ARG 1: depends on mode: (PLAYER → world) (WORLD → x) (NONE → player/world)
                .argument(new ArgumentBuilder<>("world_or_x_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            addFlagSuggestions(out, partial, soFar);

                            Mode mode = detectMode(soFar);
                            String p = norm(partial);

                            if (mode == Mode.PLAYER) {
                                // if world not set yet -> suggest worlds, otherwise suggest X
                                World givenWorld = detectWorld(soFar, mode);
                                if (givenWorld == null) {
                                    for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                        String name = w.getName();
                                        if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                                    }
                                } else {
                                    addXSuggestions(sender, out, partial);
                                }
                            } else if (mode == Mode.WORLD) {
                                addXSuggestions(sender, out, partial);
                            } else { // NONE
                                // still no non-flag token: propose players/worlds
                                for (Player online : MMOSpawnPoint.getInstance().getServer().getOnlinePlayers()) {
                                    String name = online.getName();
                                    if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                                }
                                for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                    String name = w.getName();
                                    if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                                }
                            }
                            return out;
                        }))

                // ARG 2: depends on mode: (PLAYER → x) (WORLD → y) (NONE → player/world)
                .argument(new ArgumentBuilder<>("x_or_y_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            addFlagSuggestions(out, partial, soFar);

                            Mode mode = detectMode(soFar);
                            String p = norm(partial);

                            if (mode == Mode.PLAYER) {
                                World givenWorld = detectWorld(soFar, mode);
                                if (givenWorld == null) {
                                    // still missing world -> suggest worlds
                                    for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                        String name = w.getName();
                                        if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                                    }
                                } else {
                                    addXSuggestions(sender, out, partial);
                                }
                            } else if (mode == Mode.WORLD) {
                                addYSuggestions(sender, out, partial);
                            } else {
                                // NONE: still propose players/worlds
                                for (Player online : MMOSpawnPoint.getInstance().getServer().getOnlinePlayers()) {
                                    String name = online.getName();
                                    if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                                }
                                for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                    String name = w.getName();
                                    if (name.toLowerCase(Locale.ROOT).startsWith(p)) out.add(name);
                                }
                            }
                            return out;
                        }))

                // ARG 3: depends on mode: (PLAYER → y) (WORLD → z)
                .argument(new ArgumentBuilder<>("y_or_z_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            addFlagSuggestions(out, partial, soFar);

                            Mode mode = detectMode(soFar);
                            if (mode == Mode.PLAYER) {
                                addYSuggestions(sender, out, partial);
                            } else if (mode == Mode.WORLD) {
                                addZSuggestions(sender, out, partial);
                            } else {
                                // NONE: nothing strong to suggest; fallback to coords
                                addYSuggestions(sender, out, partial);
                                addZSuggestions(sender, out, partial);
                            }
                            return out;
                        }))

                // ARG 4: depends on mode: (PLAYER → z) (WORLD → yaw)
                .argument(new ArgumentBuilder<>("z_or_yaw_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            addFlagSuggestions(out, partial, soFar);

                            Mode mode = detectMode(soFar);
                            if (mode == Mode.PLAYER) {
                                addZSuggestions(sender, out, partial);
                            } else if (mode == Mode.WORLD) {
                                addYawSuggestions(sender, out, partial);
                            } else {
                                addZSuggestions(sender, out, partial);
                                addYawSuggestions(sender, out, partial);
                            }
                            return out;
                        }))

                // ARG 5: yaw_or_pitch_or_flag (both optional)
                .argument(new ArgumentBuilder<>("yaw_or_pitch_or_flag", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            List<String> out = new ArrayList<>();
                            addFlagSuggestions(out, partial, soFar);

                            addYawSuggestions(sender, out, partial);
                            addPitchSuggestions(sender, out, partial);
                            return out;
                        }))

                .executor(this::execute);
    }

    // ========== dynamic helpers ==========

    private static final String[] FLAGS = {
            "--if-has",
            "--if-missing",
            "--only-if-incorrect",
            "--require-valid-bed",
            "--dry-run"
    };

    private static boolean isFlag(String s) {
        return s != null && s.startsWith("--");
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static void addFlagSuggestions(List<String> out, String partial, String[] soFar) {
        String p = norm(partial);
        Set<String> used = new HashSet<>();
        if (soFar != null) {
            for (String s : soFar) if (isFlag(s)) used.add(s.toLowerCase(Locale.ROOT));
        }
        for (String flag : FLAGS) {
            if (!used.contains(flag) && flag.startsWith(p)) out.add(flag);
        }
    }

    private static void addXSuggestions(CommandSender sender, List<String> out, String partial) {
        addCoordSuggestions(sender, out, partial, Axis.X);
    }

    private static void addYSuggestions(CommandSender sender, List<String> out, String partial) {
        addCoordSuggestions(sender, out, partial, Axis.Y);
    }

    private static void addZSuggestions(CommandSender sender, List<String> out, String partial) {
        addCoordSuggestions(sender, out, partial, Axis.Z);
    }

    private static void addYawSuggestions(CommandSender sender, List<String> out, String partial) {
        String p = norm(partial);
        List<String> base = Arrays.asList("0", "90", "180", "270");
        if (sender instanceof Player pl) {
            base = new ArrayList<>(base);
            base.add(String.format(Locale.US, "%.0f", pl.getLocation().getYaw()));
        }
        for (String s : base) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
    }

    private static void addPitchSuggestions(CommandSender sender, List<String> out, String partial) {
        String p = norm(partial);
        List<String> base = Arrays.asList("-30", "0", "30");
        if (sender instanceof Player pl) {
            base = new ArrayList<>(base);
            base.add(String.format(Locale.US, "%.0f", pl.getLocation().getPitch()));
        }
        for (String s : base) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
    }

    private enum Axis { X, Y, Z }
    private static void addCoordSuggestions(CommandSender sender, List<String> out, String partial, Axis axis) {
        String p = norm(partial);
        List<String> base = new ArrayList<>(Arrays.asList("0", "64", "100", "-100"));
        if (sender instanceof Player pl) {
            Location l = pl.getLocation();
            switch (axis) {
                case X -> base.add(String.format(Locale.US, "%.1f", l.getX()));
                case Y -> base.add(String.format(Locale.US, "%.1f", l.getY()));
                case Z -> base.add(String.format(Locale.US, "%.1f", l.getZ()));
            }
        }
        for (String s : base) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
    }

    private static Mode detectMode(String[] soFar) {
        List<String> nonFlags = Arrays.stream(soFar == null ? new String[0] : soFar)
                .filter(s -> s != null && !isFlag(s))
                .toList();
        if (nonFlags.isEmpty()) return Mode.NONE;

        String first = nonFlags.get(0);
        if (Bukkit.getWorld(first) != null) return Mode.WORLD;
        if (Bukkit.getPlayerExact(first) != null) return Mode.PLAYER;
        return Mode.NONE;
    }

    private static World detectWorld(String[] soFar, Mode mode) {
        List<String> nonFlags = Arrays.stream(soFar == null ? new String[0] : soFar)
                .filter(s -> s != null && !isFlag(s))
                .toList();
        if (nonFlags.isEmpty()) return null;

        if (mode == Mode.WORLD) {
            return Bukkit.getWorld(nonFlags.get(0));
        }
        if (mode == Mode.PLAYER) {
            if (nonFlags.size() >= 2) {
                return Bukkit.getWorld(nonFlags.get(1));
            }
        }
        return null;
    }

    // ========== execute (как у тебя) ==========

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

        if (pos.length == 0) {
            plugin.getMessageManager().sendMessageKeyed(sender,"commands.spawnpoint.set.consoleUsage", msgs.consoleUsage);
            return;
        }

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