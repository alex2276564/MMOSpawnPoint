package uz.alex2276564.smartspawnpoint.commands.subcommands.reload;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.smartspawnpoint.config.configs.messagesconfig.MessagesConfig;
import uz.alex2276564.smartspawnpoint.utils.SafeLocationFinder;

public class ReloadSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("reload")
                .permission("smartspawnpoint.reload")
                .description("Reload plugin configuration")
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

                    MessagesConfig msg = SmartSpawnPoint.getInstance().getConfigManager().getMessagesConfig();
                    try {
                        SafeLocationFinder.clearCache();
                        plugin.getConfigManager().reload();

                        String message = msg.commands.reload.success;
                        plugin.getMessageManager().sendMessage(sender, message, "type", "all configurations");

                    } catch (Exception e) {
                        plugin.getMessageManager().sendMessage(sender, msg.commands.reload.error, "error", e.getMessage());
                    }
                });
    }
}