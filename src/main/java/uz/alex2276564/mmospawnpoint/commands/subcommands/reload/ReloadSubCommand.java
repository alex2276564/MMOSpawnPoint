package uz.alex2276564.mmospawnpoint.commands.subcommands.reload;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.config.configs.messagesconfig.MessagesConfig;

public class ReloadSubCommand implements SubCommandProvider {

    private final MMOSpawnPointServices services;

    public ReloadSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        return parent.subcommand("reload")
                .permission("mmospawnpoint.reload")
                .description("Reload plugin configuration")
                .executor((sender, context) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();

                    MessagesConfig msg = configManager.getMessagesConfig();
                    try {
                        configManager.reload();

                        String message = msg.commands.reload.success;
                        messageManager.sendMessageKeyed(
                                sender,
                                "commands.reload.success",
                                message,
                                "type", "all configurations"
                        );

                    } catch (Exception e) {
                        String error = (e.getMessage() != null) ? e.getMessage() : "unknown";
                        messageManager.sendMessageKeyed(
                                sender,
                                "commands.reload.error",
                                msg.commands.reload.error,
                                "error", error
                        );
                    }
                });
    }
}