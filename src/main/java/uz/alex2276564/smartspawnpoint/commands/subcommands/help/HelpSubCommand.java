package uz.alex2276564.smartspawnpoint.commands.subcommands.help;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;

public class HelpSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("help")
                .permission("smartspawnpoint.command")
                .description("Show help information")
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

                    plugin.getMessageManager().sendMessage(sender, "<gold>=== SmartSpawnPoint Help ===");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp reload <gray>- Reload the plugin configuration");

                    if (plugin.getConfigManager().isPartyEnabled()) {
                        plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party <gray>- Party commands");
                    }
                });
    }
}