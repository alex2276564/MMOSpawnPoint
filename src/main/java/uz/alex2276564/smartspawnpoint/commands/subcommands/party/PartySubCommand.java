package uz.alex2276564.smartspawnpoint.commands.subcommands.party;

import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.invite.InviteSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.accept.AcceptSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.deny.DenySubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.leave.LeaveSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.list.ListSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.remove.RemoveSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.setleader.SetLeaderSubCommand;
import uz.alex2276564.smartspawnpoint.commands.subcommands.party.options.OptionsSubCommand;

public class PartySubCommand implements SubCommandProvider {

    @Override
    public SubCommandBuilder build(CommandBuilder parent) {
        SubCommandBuilder partyBuilder = parent.subcommand("party")
                .permission("smartspawnpoint.party")
                .description("Party system commands")
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

                    if (!plugin.getConfigManager().isPartyEnabled()) {
                        plugin.getMessageManager().sendMessage(sender, "<red>Party system is disabled on this server.");
                        return;
                    }

                    // Show party help
                    plugin.getMessageManager().sendMessage(sender, "<gold>=== Party Commands ===");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party invite <player> <gray>- Invite player to your party");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party accept <gray>- Accept a party invitation");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party deny <gray>- Decline a party invitation");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party leave <gray>- Leave your current party");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party list <gray>- List all party members");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party remove <player> <gray>- Remove a player from your party");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party setleader <player> <gray>- Transfer party leadership");
                    plugin.getMessageManager().sendMessage(sender, "<yellow>/ssp party options <gray>- View and change party options");
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