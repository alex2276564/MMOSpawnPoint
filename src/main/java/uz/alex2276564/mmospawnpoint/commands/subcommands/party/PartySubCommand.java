package uz.alex2276564.mmospawnpoint.commands.subcommands.party;

import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.CommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.accept.AcceptSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.deny.DenySubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.invite.InviteSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.leave.LeaveSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.list.ListSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.OptionsSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.remove.RemoveSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.setleader.SetLeaderSubCommand;

public class PartySubCommand implements SubCommandProvider {

    private final MMOSpawnPointServices services;

    public PartySubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder partyBuilder = parent.subcommand("party")
                .permission("mmospawnpoint.party")
                .description("Party system commands")
                .executor((sender, context) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();

                    if (!configManager.getMainConfig().party.enabled) {
                        messageManager.sendMessageKeyed(sender, "party.systemDisabled",
                                configManager.getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    // Show party help
                    var messages = configManager.getMessagesConfig().party.help;
                    messageManager.sendMessageKeyed(sender, "party.help.header", messages.header);
                    messageManager.sendMessageKeyed(sender, "party.help.invite", messages.invite);
                    messageManager.sendMessageKeyed(sender, "party.help.accept", messages.accept);
                    messageManager.sendMessageKeyed(sender, "party.help.deny", messages.deny);
                    messageManager.sendMessageKeyed(sender, "party.help.leave", messages.leave);
                    messageManager.sendMessageKeyed(sender, "party.help.list", messages.list);
                    messageManager.sendMessageKeyed(sender, "party.help.remove", messages.remove);
                    messageManager.sendMessageKeyed(sender, "party.help.setleader", messages.setleader);
                    messageManager.sendMessageKeyed(sender, "party.help.options", messages.options);
                });

        // Register nested party subcommands
        new InviteSubCommand(services).build(partyBuilder);
        new AcceptSubCommand(services).build(partyBuilder);
        new DenySubCommand(services).build(partyBuilder);
        new LeaveSubCommand(services).build(partyBuilder);
        new ListSubCommand(services).build(partyBuilder);
        new RemoveSubCommand(services).build(partyBuilder);
        new SetLeaderSubCommand(services).build(partyBuilder);
        new OptionsSubCommand(services).build(partyBuilder);

        return partyBuilder;
    }
}