package uz.alex2276564.mmospawnpoint.commands.subcommands.party.invite;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

public class InviteSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("invite")
                .permission("mmospawnpoint.party.invite")
                .description("Invite a player to your party")
                .argument(new ArgumentBuilder<>("player", ArgumentType.PLAYER)
                        .dynamicSuggestions(partial ->
                                Bukkit.getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                                        .toList()))
                .executor((sender, context) -> {
                    MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();

                    if (!(sender instanceof Player player)) {
                        plugin.getMessageManager().sendMessage(sender,
                                plugin.getConfigManager().getMessagesConfig().party.onlyPlayers);
                        return;
                    }

                    if (!plugin.getConfigManager().getMainConfig().party.enabled) {
                        plugin.getMessageManager().sendMessage(sender,
                                plugin.getConfigManager().getMessagesConfig().party.systemDisabled);
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
                        String message = plugin.getConfigManager().getMessagesConfig().party.inviteSent;
                        plugin.getMessageManager().sendMessage(player, message, "player", targetPlayer.getName());

                        String inviteMessage = plugin.getConfigManager().getMessagesConfig().party.inviteReceived;
                        plugin.getMessageManager().sendMessage(targetPlayer, inviteMessage, "player", player.getName());
                    } else {
                        // Check specific reason for failure
                        if (partyManager.isInParty(targetPlayer.getUniqueId())) {
                            plugin.getMessageManager().sendMessage(player,
                                    plugin.getConfigManager().getMessagesConfig().party.inviteFailedAlreadyInParty);
                        } else {
                            plugin.getMessageManager().sendMessage(player,
                                    plugin.getConfigManager().getMessagesConfig().party.inviteFailedPartyFull);
                        }
                    }
                });
    }
}