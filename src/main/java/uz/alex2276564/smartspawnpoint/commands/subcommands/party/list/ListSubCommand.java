package uz.alex2276564.smartspawnpoint.commands.subcommands.party.list;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.List;

public class ListSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("list")
                .permission("smartspawnpoint.party.list")
                .description("List all party members")
                .executor((sender, context) -> {
                    SmartSpawnPoint plugin = SmartSpawnPoint.getInstance();

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

                    PartyManager partyManager = plugin.getPartyManager();

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player,
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());

                    // Display party information
                    plugin.getMessageManager().sendMessage(player, "<dark_gray>[<red>Bound Souls<dark_gray>]");

                    // Leader first
                    Player leader = party.getLeaderPlayer();
                    if (leader != null) {
                        plugin.getMessageManager().sendMessage(player, "<red>👑 <player> <gray>(Death Lord)", "player", leader.getName());
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<red>👑 <gray>(Death Lord lost in the void)");
                    }

                    // Then other members
                    List<Player> onlineMembers = party.getOnlineMembers();
                    onlineMembers.remove(leader); // Remove leader as we already displayed them

                    for (Player member : onlineMembers) {
                        // Mark the respawn target if set
                        if (party.getRespawnTarget() != null && party.getRespawnTarget().equals(member.getUniqueId())) {
                            plugin.getMessageManager().sendMessage(player, "<dark_red>🔄 <player> <gray>(Soul Anchor)", "player", member.getName());
                        } else {
                            plugin.getMessageManager().sendMessage(player, "<gray>☠ <player>", "player", member.getName());
                        }
                    }

                    // Display party settings
                    plugin.getMessageManager().sendMessage(player, "<dark_gray>[<red>Death Circle Settings<dark_gray>]");
                    plugin.getMessageManager().sendMessage(player, "<gray>Soul Binding: <red><respawnmode>", "respawnmode", party.getRespawnMode().name());

                    if (party.getRespawnTarget() != null) {
                        Player target = party.getRespawnTargetPlayer();
                        if (target != null) {
                            plugin.getMessageManager().sendMessage(player, "<gray>Soul Anchor: <red><player>", "player", target.getName());
                        } else {
                            plugin.getMessageManager().sendMessage(player, "<gray>Soul Anchor: <red>(Lost in darkness)");
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<gray>Soul Anchor: <red>None");
                    }

                    plugin.getMessageManager().sendMessage(player, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                });
    }
}