package uz.alex2276564.smartspawnpoint.commands.partycommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.LongCommandExecutor;
import uz.alex2276564.smartspawnpoint.commands.partycommand.list.*;

import java.util.List;

public class PartyCommand extends LongCommandExecutor {
    private final SmartSpawnPoint plugin;

    public PartyCommand(SmartSpawnPoint plugin) {
        this.plugin = plugin;

        addSubCommand(new InviteCommandExecutor(plugin), new String[] {"invite"}, new Permission("smartspawnpoint.party.invite"));
        addSubCommand(new AcceptCommandExecutor(plugin), new String[] {"accept"}, new Permission("smartspawnpoint.party.accept"));
        addSubCommand(new DenyCommandExecutor(plugin), new String[] {"deny"}, new Permission("smartspawnpoint.party.deny"));
        addSubCommand(new LeaveCommandExecutor(plugin), new String[] {"leave"}, new Permission("smartspawnpoint.party.leave"));
        addSubCommand(new ListCommandExecutor(plugin), new String[] {"list"}, new Permission("smartspawnpoint.party.list"));
        addSubCommand(new RemoveCommandExecutor(plugin), new String[] {"remove"}, new Permission("smartspawnpoint.party.remove"));
        addSubCommand(new SetLeaderCommandExecutor(plugin), new String[] {"setleader"}, new Permission("smartspawnpoint.party.setleader"));
        addSubCommand(new OptionsCommandExecutor(plugin), new String[] {"options"}, new Permission("smartspawnpoint.party.options"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!plugin.getConfigManager().isPartyEnabled()) {
            commandSender.sendMessage("§cParty system is disabled on this server.");
            return true;
        }

        if (args.length < 1) return false;
        final SubCommandWrapper wrapper = getWrapperFromLabel(args[0]);
        if (wrapper == null) return false;

        if (!commandSender.hasPermission(wrapper.getPermission())) {
            commandSender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);
        wrapper.getCommand().onExecute(commandSender, newArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!plugin.getConfigManager().isPartyEnabled()) {
            return null;
        }

        if (args.length == 1) {
            return getFirstAliases();
        }

        final SubCommandWrapper wrapper = getWrapperFromLabel(args[0]);
        if (wrapper == null) return null;

        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);
        return wrapper.getCommand().onTabComplete(commandSender, newArgs);
    }
}