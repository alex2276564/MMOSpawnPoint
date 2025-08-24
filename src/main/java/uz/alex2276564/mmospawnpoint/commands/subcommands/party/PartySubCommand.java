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
                        plugin.getMessageManager().sendMessage(sender,
                                plugin.getConfigManager().getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    // Show party help
                    var messages = plugin.getConfigManager().getMessagesConfig().party.help;
                    plugin.getMessageManager().sendMessage(sender, messages.header);
                    plugin.getMessageManager().sendMessage(sender, messages.invite);
                    plugin.getMessageManager().sendMessage(sender, messages.accept);
                    plugin.getMessageManager().sendMessage(sender, messages.deny);
                    plugin.getMessageManager().sendMessage(sender, messages.leave);
                    plugin.getMessageManager().sendMessage(sender, messages.list);
                    plugin.getMessageManager().sendMessage(sender, messages.remove);
                    plugin.getMessageManager().sendMessage(sender, messages.setleader);
                    plugin.getMessageManager().sendMessage(sender, messages.options);
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