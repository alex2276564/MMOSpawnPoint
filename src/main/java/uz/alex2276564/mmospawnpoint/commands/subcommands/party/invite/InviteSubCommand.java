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
                    if (targetPlayer == null) {
                        // Should not happen with ArgumentType.PLAYER, but just in case
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                        return;
                    }

                    PartyManager partyManager = plugin.getPartyManager();

                    // Auto-create party if player is not in one (keep your previous UX)
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        partyManager.createParty(player);
                    }

                    PartyManager.InviteResult result = partyManager.invitePlayer(player, targetPlayer);

                    switch (result) {
                        case SUCCESS -> {
                            String sent = plugin.getConfigManager().getMessagesConfig().party.inviteSent;
                            plugin.getMessageManager().sendMessage(player, sent, "player", targetPlayer.getName());

                            String recv = plugin.getConfigManager().getMessagesConfig().party.inviteReceived;
                            plugin.getMessageManager().sendMessage(targetPlayer, recv, "player", player.getName());
                        }
                        case ALREADY_INVITED -> {
                            // No dedicated message in config — reuse "inviteSent" as an idempotent feedback
                            String sent = plugin.getConfigManager().getMessagesConfig().party.inviteSent;
                            plugin.getMessageManager().sendMessage(player, sent, "player", targetPlayer.getName());
                        }
                        case TARGET_ALREADY_IN_PARTY -> plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.inviteFailedAlreadyInParty);
                        case PARTY_FULL -> plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.inviteFailedPartyFull);
                        case NOT_LEADER -> plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        case LEADER_NOT_IN_PARTY -> plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        default -> plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.errorOccurred);
                    }
                });
    }
}