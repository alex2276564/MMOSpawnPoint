package uz.alex2276564.smartspawnpoint.commands.subcommands.party.list;

import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.framework.builder.*;
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
                        plugin.getMessageManager().sendMessage(sender, "<red>Only players can use this command!");
                        return;
                    }

                    if (!plugin.getConfigManager().isPartyEnabled()) {
                        plugin.getMessageManager().sendMessage(sender, "<red>Party system is disabled.");
                        return;
                    }

                    PartyManager partyManager = plugin.getPartyManager();

                    // Check if player is in a party
                    if (!partyManager.isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessage(player, "<red>You are not in a party.");
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());

                    // Display party information
                    plugin.getMessageManager().sendMessage(player, "<gold>==== Party Members ====");

                    // Leader first
                    Player leader = party.getLeaderPlayer();
                    if (leader != null) {
                        plugin.getMessageManager().sendMessage(player, "<yellow>👑 " + leader.getName() + " <gray>(Leader)");
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<yellow>👑 Unknown <gray>(Leader, offline)");
                    }

                    // Then other members
                    List<Player> onlineMembers = party.getOnlineMembers();
                    onlineMembers.remove(leader); // Remove leader as we already displayed them

                    for (Player member : onlineMembers) {
                        // Mark the respawn target if set
                        if (party.getRespawnTarget() != null && party.getRespawnTarget().equals(member.getUniqueId())) {
                            plugin.getMessageManager().sendMessage(player, "<green>🔄 " + member.getName() + " <gray>(Respawn Target)");
                        } else {
                            plugin.getMessageManager().sendMessage(player, "<green>" + member.getName());
                        }
                    }

                    // Display party settings
                    plugin.getMessageManager().sendMessage(player, "<gold>==== Party Settings ====");
                    plugin.getMessageManager().sendMessage(player, "<yellow>Respawn Mode: <white>" + party.getRespawnMode().name());

                    if (party.getRespawnTarget() != null) {
                        Player target = party.getRespawnTargetPlayer();
                        if (target != null) {
                            plugin.getMessageManager().sendMessage(player, "<yellow>Respawn Target: <white>" + target.getName());
                        } else {
                            plugin.getMessageManager().sendMessage(player, "<yellow>Respawn Target: <white>(Offline)");
                        }
                    } else {
                        plugin.getMessageManager().sendMessage(player, "<yellow>Respawn Target: <white>None");
                    }

                    plugin.getMessageManager().sendMessage(player, "<gold>====================");
                });
    }
}