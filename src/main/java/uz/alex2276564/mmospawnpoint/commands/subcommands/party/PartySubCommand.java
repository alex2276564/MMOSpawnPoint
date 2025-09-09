package uz.alex2276564.mmospawnpoint.commands.subcommands.party;

import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
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

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder partyBuilder = parent.subcommand("party")
                .permission("mmospawnpoint.party")
                .description("Party system commands")
                .executor((sender, context) -> {
                    MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();

                    if (!plugin.getConfigManager().getMainConfig().party.enabled) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "party.systemDisabled",
                                plugin.getConfigManager().getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    // Show party help
                    var messages = plugin.getConfigManager().getMessagesConfig().party.help;
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.header", messages.header);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.invite", messages.invite);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.accept", messages.accept);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.deny", messages.deny);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.leave", messages.leave);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.list", messages.list);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.remove", messages.remove);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.setleader", messages.setleader);
                    plugin.getMessageManager().sendMessageKeyed(sender, "party.help.options", messages.options);
                });

        // Register nested party subcommands
        new InviteSubCommand().build(partyBuilder);
        new AcceptSubCommand().build(partyBuilder);
        new DenySubCommand().build(partyBuilder);
        new LeaveSubCommand().build(partyBuilder);
        new ListSubCommand().build(partyBuilder);
        new RemoveSubCommand().build(partyBuilder);
        new SetLeaderSubCommand().build(partyBuilder);
        new OptionsSubCommand().build(partyBuilder);

        return partyBuilder;
    }
}