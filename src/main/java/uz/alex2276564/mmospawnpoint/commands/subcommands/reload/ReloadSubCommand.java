package uz.alex2276564.mmospawnpoint.commands.subcommands.reload;

import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.config.configs.messagesconfig.MessagesConfig;
import uz.alex2276564.mmospawnpoint.utils.SafeLocationFinder;

public class ReloadSubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("reload")
                .permission("mmospawnpoint.reload")
                .description("Reload plugin configuration")
                .executor((sender, context) -> {
                    MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();

                    MessagesConfig msg = MMOSpawnPoint.getInstance().getConfigManager().getMessagesConfig();
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