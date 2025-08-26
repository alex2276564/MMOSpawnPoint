package uz.alex2276564.mmospawnpoint.commands.subcommands.setspawnpoint;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandContext;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;

public class SetSpawnPointSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("setspawnpoint")
                .permission("mmospawnpoint.setspawnpoint")
                .description("Set vanilla respawn point (bed spawn) for a player")
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
                    plugin.getMessageManager().sendMessage(sender, msgs.consoleUsage);
                    return;
                }
                setBedSpawn(plugin, p, p.getLocation(), sender);
                return;
            }

            // /msp setspawnpoint <world> <x> <y> <z> [yaw] [pitch] (world-first)
            World worldFirst = Bukkit.getWorld(args[0]);
            if (worldFirst != null) {
                if (!(sender instanceof Player p)) {
                    plugin.getMessageManager().sendMessage(sender, msgs.consoleNeedsPlayer);
                    return;
                }
                Location loc = parseLocation(worldFirst, args, 1);
                if (loc == null) {
                    plugin.getMessageManager().sendMessage(sender, msgs.invalidCoords);
                    return;
                }
                setBedSpawn(plugin, p, loc, sender);
                return;
            }

            // /msp setspawnpoint <player> [...]
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                plugin.getMessageManager().sendMessage(sender, msgs.playerNotFound, "player", args[0]);
                return;
            }

            // /msp setspawnpoint <player>
            if (args.length == 1) {
                if (!(sender instanceof Player p)) {
                    plugin.getMessageManager().sendMessage(sender, msgs.consoleNeedsCoords);
                    return;
                }
                setBedSpawn(plugin, target, p.getLocation(), sender);
                return;
            }

            // /msp setspawnpoint <player> <world> <x> <y> <z> [yaw] [pitch]
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                plugin.getMessageManager().sendMessage(sender, msgs.invalidWorld, "world", args[1]);
                return;
            }
            Location loc = parseLocation(world, args, 2);
            if (loc == null) {
                plugin.getMessageManager().sendMessage(sender, msgs.invalidCoords);
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
                plugin.getMessageManager().sendMessage(feedback, msgs.selfSuccess, "location", locationStr);
            } else {
                // Other (admin sets player's spawn)
                plugin.getMessageManager().sendMessage(feedback,
                        msgs.otherSuccess.replace("<player>", target.getName()).replace("<location>", locationStr));
                plugin.getMessageManager().sendMessage(target,
                        msgs.targetNotification.replace("<setter>", feedback.getName()).replace("<location>", locationStr));
            }
        } catch (Throwable t) {
            // Extremely редкий кейс, но на всякий
            plugin.getMessageManager().sendMessage(feedback, msgs.failed, "player", target.getName());
        }
    }
}