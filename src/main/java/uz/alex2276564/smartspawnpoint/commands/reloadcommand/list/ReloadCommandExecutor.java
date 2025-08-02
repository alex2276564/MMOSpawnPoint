package uz.alex2276564.smartspawnpoint.commands.reloadcommand.list;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.SubCommand;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ReloadCommandExecutor implements SubCommand {

    @Override
    public void onExecute(@NotNull CommandSender sender, @NotNull String[] args) {
        String permission = "smartspawnpoint.reload";

        if (!sender.hasPermission(permission)) {
            sender.sendMessage("§cYou do not have permission to use this command. Missing permission: §e" + permission);
            return;
        }

        SafeLocationFinder.clearCache();

        SmartSpawnPoint.getInstance().getConfigManager().reload();

        sender.sendMessage("§aSmartSpawnPoint configuration successfully reloaded.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull String[] args) {
        // Bukkit does not give out the list of players
        return Collections.emptyList();
    }
}