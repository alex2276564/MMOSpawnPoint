package uz.alex2276564.smartspawnpoint.commands.subcommands.reload;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;

public class ReloadSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("reload")
                .permission("smartspawnpoint.reload")
                .description("Reload plugin configuration")
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

                    try {
                        SafeLocationFinder.clearCache();
                        plugin.getConfigManager().reload();

                        plugin.getMessageManager().sendMessage(sender,
                                "<green>SmartSpawnPoint configuration successfully reloaded.");

                    } catch (Exception e) {
                        plugin.getMessageManager().sendMessage(sender,
                                "<red>Error reloading configuration: " + e.getMessage());
                    }
                });
    }
}