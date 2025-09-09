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

public class SetSpawnPointSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("setspawnpoint")
                .permission("mmospawnpoint.setspawnpoint")
                .description("Set vanilla respawn point (bed) for a player")
                // Arg1: player name or world name
                .argument(new ArgumentBuilder<>("target_or_world", ArgumentType.STRING)
                        .optional(null)
                        .dynamicSuggestions(partial -> {
                            String p = (partial == null) ? "" : partial.toLowerCase();
                            List<String> out = new ArrayList<>();
                            // players
                            for (Player online : MMOSpawnPoint.getInstance().getServer().getOnlinePlayers()) {
                                String name = online.getName();
                                if (name.toLowerCase().startsWith(p)) out.add(name);
                            }
                            // worlds
                            for (World w : MMOSpawnPoint.getInstance().getServer().getWorlds()) {
                                String name = w.getName();
                                if (name.toLowerCase().startsWith(p)) out.add(name);
                            }
                            return out;
                        }))
                // X/Y/Z/Yaw/Pitch as optional hints (for both world-first and player-first syntaxes)
                .argument(new ArgumentBuilder<>("x", ArgumentType.STRING)
                        .optional(null)
                        .suggestions("-100", "-50", "0", "50", "100"))
                .argument(new ArgumentBuilder<>("y", ArgumentType.STRING)
                        .optional(null)
                        .suggestions("64", "80", "100"))
                .argument(new ArgumentBuilder<>("z", ArgumentType.STRING)
                        .optional(null)
                        .suggestions("-100", "-50", "0", "50", "100"))
                .argument(new ArgumentBuilder<>("yaw", ArgumentType.STRING)
                        .optional(null)
                        .suggestions("0", "90", "180", "270"))
                .argument(new ArgumentBuilder<>("pitch", ArgumentType.STRING)
                        .optional(null)
                        .suggestions("-30", "0", "30"))
                .executor(this::execute);
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