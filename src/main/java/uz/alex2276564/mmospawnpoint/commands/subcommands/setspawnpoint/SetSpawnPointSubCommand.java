package uz.alex2276564.mmospawnpoint.commands.subcommands.setspawnpoint;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SetSpawnPointSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("setspawnpoint")
                .permission("mmospawnpoint.setspawnpoint")
                .description("Set vanilla respawn point (bed) for a player")

                // ARG 0: target_or_world
                .argument(new ArgumentBuilder<>("target_or_world", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                            List<String> out = new ArrayList<>();
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

                // ARG 1: “x” by name, but semantically:
                // - player-first: this is actually <world>
                // - world-first: this is X
                .argument(new ArgumentBuilder<>("x", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (soFar.length < 1) return List.of(); // no first token yet

                            String first = soFar[0];
                            boolean firstIsPlayer = isOnlinePlayer(first);
                            boolean firstIsWorld = isWorld(first);

                            if (firstIsPlayer) {
                                // Suggest worlds
                                String p = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
                                List<String> worlds = new ArrayList<>();
                                for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                    String name = w.getName();
                                    if (name.toLowerCase(Locale.ROOT).startsWith(p)) worlds.add(name);
                                }
                                return worlds;
                            }

                            if (firstIsWorld) {
                                // Suggest X
                                return suggestCoordX(sender, partial);
                            }

                            // If unknown first token: no strong hint
                            return List.of();
                        }))

                // ARG 2: “y” by name, but semantically:
                // - player-first: X
                // - world-first: Y
                .argument(new ArgumentBuilder<>("y", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (soFar.length < 1) return List.of();

                            String first = soFar[0];
                            boolean firstIsPlayer = isOnlinePlayer(first);
                            boolean firstIsWorld = isWorld(first);

                            if (firstIsPlayer) {
                                // now we expect X
                                return suggestCoordX(sender, partial);
                            }
                            if (firstIsWorld) {
                                // now we expect Y
                                return suggestCoordY(sender, partial);
                            }
                            return List.of();
                        }))

                // ARG 3: “z” by name, but semantically:
                // - player-first: Y
                // - world-first: Z
                .argument(new ArgumentBuilder<>("z", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (soFar.length < 1) return List.of();

                            String first = soFar[0];
                            boolean firstIsPlayer = isOnlinePlayer(first);
                            boolean firstIsWorld = isWorld(first);

                            if (firstIsPlayer) {
                                // now expect Y
                                return suggestCoordY(sender, partial);
                            }
                            if (firstIsWorld) {
                                // now expect Z
                                return suggestCoordZ(sender, partial);
                            }
                            return List.of();
                        }))

                // ARG 4: “yaw” by name, but semantically:
                // - player-first: Z
                // - world-first: yaw
                .argument(new ArgumentBuilder<>("yaw", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (soFar.length < 1) return List.of();

                            String first = soFar[0];
                            boolean firstIsPlayer = isOnlinePlayer(first);
                            boolean firstIsWorld = isWorld(first);

                            if (firstIsPlayer) {
                                // now expect Z
                                return suggestCoordZ(sender, partial);
                            }
                            if (firstIsWorld) {
                                // now expect yaw
                                return suggestYaw(sender, partial);
                            }
                            return List.of();
                        }))

                // ARG 5: “pitch” by name, but semантически:
                // - player-first: yaw
                // - world-first: pitch
                .argument(new ArgumentBuilder<>("pitch", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions((sender, partial, soFar) -> {
                            if (soFar.length < 1) return List.of();

                            String first = soFar[0];
                            boolean firstIsPlayer = isOnlinePlayer(first);
                            boolean firstIsWorld = isWorld(first);

                            if (firstIsPlayer) {
                                // now expect yaw
                                return suggestYaw(sender, partial);
                            }
                            if (firstIsWorld) {
                                // now expect pitch
                                return suggestPitch(sender, partial);
                            }
                            return List.of();
                        }))
                .executor(this::execute);
    }

    // ---- helpers (suggestions) ----
    private static boolean isOnlinePlayer(String name) {
        if (name == null || name.isEmpty()) return false;
        return Bukkit.getPlayerExact(name) != null;
    }

    private static boolean isWorld(String w) {
        if (w == null || w.isEmpty()) return false;
        return Bukkit.getWorld(w) != null;
    }

    private static List<String> suggestCoordX(CommandSender sender, String partial) {
        if (sender instanceof Player pl) {
            return List.of(String.format(Locale.US, "%.1f", pl.getLocation().getX()));
        }
        return List.of("0", "100", "-100");
    }

    private static List<String> suggestCoordY(CommandSender sender, String partial) {
        if (sender instanceof Player pl) {
            return List.of(String.format(Locale.US, "%.1f", pl.getLocation().getY()), "64", "80", "100");
        }
        return List.of("64", "80", "100");
    }

    private static List<String> suggestCoordZ(CommandSender sender, String partial) {
        if (sender instanceof Player pl) {
            return List.of(String.format(Locale.US, "%.1f", pl.getLocation().getZ()));
        }
        return List.of("0", "100", "-100");
    }

    private static List<String> suggestYaw(CommandSender sender, String partial) {
        if (sender instanceof Player pl) {
            return List.of(
                    String.format(Locale.US, "%.0f", pl.getLocation().getYaw()),
                    "0", "90", "180", "270"
            );
        }
        return List.of("0", "90", "180", "270");
    }

    private static List<String> suggestPitch(CommandSender sender, String partial) {
        if (sender instanceof Player pl) {
            return List.of(
                    String.format(Locale.US, "%.0f", pl.getLocation().getPitch()),
                    "-30", "0", "30"
            );
        }
        return List.of("-30", "0", "30");
    }

    private void execute(CommandSender sender, CommandContext ctx) {
        MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();
        var msgs = plugin.getConfigManager().getMessagesConfig().commands.setspawnpoint;

        String[] args = ctx.getRawArgs();

        try {
            // /msp setspawnpoint (self, sender must be player)
            if (args.length == 0) {
                if (!(sender instanceof Player p)) {
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.setspawnpoint.consoleUsage", msgs.consoleUsage);
                    return;
                }
                setBedSpawn(plugin, p, p.getLocation(), sender);
                return;
            }

            // /msp setspawnpoint <world> <x> <y> <z> [yaw] [pitch] (world-first)
            World worldFirst = Bukkit.getWorld(args[0]);
            if (worldFirst != null) {
                if (!(sender instanceof Player p)) {
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.setspawnpoint.consoleNeedsPlayer", msgs.consoleNeedsPlayer);
                    return;
                }
                Location loc = parseLocation(worldFirst, args, 1);
                if (loc == null) {
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.setspawnpoint.invalidCoords", msgs.invalidCoords);
                    return;
                }
                setBedSpawn(plugin, p, loc, sender);
                return;
            }

            // /msp setspawnpoint <player> [...]
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.setspawnpoint.playerNotFound", msgs.playerNotFound, "player", args[0]);
                return;
            }

            // /msp setspawnpoint <player>
            if (args.length == 1) {
                if (!(sender instanceof Player p)) {
                    plugin.getMessageManager().sendMessageKeyed(sender, "commands.setspawnpoint.consoleNeedsCoords", msgs.consoleNeedsCoords);
                    return;
                }
                setBedSpawn(plugin, target, p.getLocation(), sender);
                return;
            }

            // /msp setspawnpoint <player> <world> <x> <y> <z> [yaw] [pitch]
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.setspawnpoint.invalidWorld", msgs.invalidWorld, "world", args[1]);
                return;
            }
            Location loc = parseLocation(world, args, 2);
            if (loc == null) {
                plugin.getMessageManager().sendMessageKeyed(sender, "commands.setspawnpoint.invalidCoords", msgs.invalidCoords);
                return;
            }
            setBedSpawn(plugin, target, loc, sender);

        } catch (Exception e) {
            // Fallback error
            plugin.getMessageManager().sendMessage(sender, "<red>Error: <gray>" + (e.getMessage() == null ? "unknown" : e.getMessage()));
        }
    }

    private Location parseLocation(World world, String[] args, int offset) {
        if (args.length < offset + 3) return null;
        try {
            double x = Double.parseDouble(args[offset]);
            double y = Double.parseDouble(args[offset + 1]);
            double z = Double.parseDouble(args[offset + 2]);
            float yaw = 0f;
            float pitch = 0f;
            if (args.length >= offset + 4) yaw = Float.parseFloat(args[offset + 3]);
            if (args.length >= offset + 5) pitch = Float.parseFloat(args[offset + 4]);
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setBedSpawn(MMOSpawnPoint plugin, Player target, Location loc, CommandSender feedback) {
        var msgs = plugin.getConfigManager().getMessagesConfig().commands.setspawnpoint;

        try {
            target.setBedSpawnLocation(loc, true);

            String locationStr = String.format("%.1f, %.1f, %.1f in %s",
                    loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());

            if (feedback instanceof Player senderP && senderP.getUniqueId().equals(target.getUniqueId())) {
                // Self
                plugin.getMessageManager().sendMessageKeyed(feedback, "commands.setspawnpoint.selfSuccess", msgs.selfSuccess, "location", locationStr);
            } else {
                // Other (admin sets player's spawn)
                plugin.getMessageManager().sendMessageKeyed(feedback, "commands.setspawnpoint.otherSuccess",
                        msgs.otherSuccess.replace("<player>", target.getName()).replace("<location>", locationStr));
                plugin.getMessageManager().sendMessageKeyed(target, "commands.setspawnpoint.targetNotification",
                        msgs.targetNotification.replace("<setter>", feedback.getName()).replace("<location>", locationStr));
            }
        } catch (Throwable t) {
            // An extremely rare case, but just in case
            plugin.getMessageManager().sendMessageKeyed(feedback, "commands.setspawnpoint.failed", msgs.failed, "player", target.getName());
        }
    }
}