package uz.alex2276564.smartspawnpoint.commands.subcommands.party.invite;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

public class InviteSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("invite")
                .permission("smartspawnpoint.party.invite")
                .description("Invite a player to your party")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions(partial ->
                                Bukkit.getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                                        .toList()))
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

                    if (!(sender instanceof Player player)) {
                        plugin.getMessageManager().sendMessage(sender, "<red>Only players can use this command!");
                        return;
                    }

                    if (!plugin.getConfigManager().isPartyEnabled()) {
                        plugin.getMessageManager().sendMessage(sender, "<red>Party system is disabled.");
                        return;
                    }

                    Player targetPlayer = context.getArgument("player");
                    PartyManager partyManager = plugin.getPartyManager();

                    // Create party if player is not in one
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        partyManager.createParty(player);
                    }

                    // Send invitation
                    boolean success = partyManager.invitePlayer(player, targetPlayer);

                    if (success) {
                        plugin.getMessageManager().sendMessage(player,
                                "<green>Invitation sent to <yellow>" + targetPlayer.getName());

                        plugin.getMessageManager().sendMessage(targetPlayer,
                                "<green>You've been invited to join <yellow>" + player.getName() +
                                        "<green>'s party. Type <yellow>/ssp party accept <green>to join.");
                    } else {
                        plugin.getMessageManager().sendMessage(player,
                                "<red>Couldn't invite player. They might already be in a party or your party is full.");
                    }
                });
    }
}