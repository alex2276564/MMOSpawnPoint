package uz.alex2276564.smartspawnpoint.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.partycommand.PartyCommand;
import uz.alex2276564.smartspawnpoint.commands.reloadcommand.ReloadCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommandExecutor implements TabExecutor {
    private final SmartSpawnPoint plugin;
    private final ReloadCommand reloadCommand;
    private final PartyCommand partyCommand;

    public MainCommandExecutor(SmartSpawnPoint plugin) {
        this.plugin = plugin;
        this.reloadCommand = new ReloadCommand();
        this.partyCommand = new PartyCommand(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§6=== SmartSpawnPoint Help ===");
            sender.sendMessage("§e/ssp reload §7- Reload the plugin configuration");
            sender.sendMessage("§e/ssp party §7- Party commands");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // Handle reload commands
        if (subCommand.equals("reload")) {
            return reloadCommand.onCommand(sender, command, label, args);
        }

        // Handle party commands
        if (subCommand.equals("party") && plugin.getConfigManager().isPartyEnabled()) {
            if (args.length == 1) {
                // Show party help
                sender.sendMessage("§6=== Party Commands ===");
                sender.sendMessage("§e/ssp party invite <player> §7- Invite player to your party");
                sender.sendMessage("§e/ssp party accept §7- Accept a party invitation");
                sender.sendMessage("§e/ssp party deny §7- Decline a party invitation");
                sender.sendMessage("§e/ssp party leave §7- Leave your current party");
                sender.sendMessage("§e/ssp party list §7- List all party members");
                sender.sendMessage("§e/ssp party remove <player> §7- Remove a player from your party");
                sender.sendMessage("§e/ssp party setleader <player> §7- Transfer party leadership");
                sender.sendMessage("§e/ssp party options §7- View and change party options");
                return true;
            }

            return partyCommand.onCommand(sender, command, label, shiftArgs(args));
        }

        // Unknown command
        sender.sendMessage("§cUnknown command. Use /ssp for help.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");

            if (plugin.getConfigManager().isPartyEnabled()) {
                completions.add("party");
            }

            String partial = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }

        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("reload")) {
                return reloadCommand.onTabComplete(sender, command, alias, shiftArgs(args));
            }

            if (args[0].equalsIgnoreCase("party") && plugin.getConfigManager().isPartyEnabled()) {
                return partyCommand.onTabComplete(sender, command, alias, shiftArgs(args));
            }
        }

        return new ArrayList<>();
    }

    private String[] shiftArgs(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }

        String[] shiftedArgs = new String[args.length - 1];
        System.arraycopy(args, 1, shiftedArgs, 0, args.length - 1);
        return shiftedArgs;
    }
}