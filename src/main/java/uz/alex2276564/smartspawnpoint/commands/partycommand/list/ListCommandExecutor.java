package uz.alex2276564.smartspawnpoint.commands.partycommand.list;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.SubCommand;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.ArrayList;
import java.util.List;

public class ListCommandExecutor implements SubCommand {
    private final SmartSpawnPoint plugin;

    public ListCommandExecutor(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        PartyManager partyManager = plugin.getPartyManager();

        // Check if player is in a party
        if (!partyManager.isInParty(player.getUniqueId())) {
            String message = plugin.getConfigManager().getPartyMessage("not-in-party");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            } else {
                player.sendMessage("§cYou are not in a party.");
            }
            return;
        }

        Party party = partyManager.getPlayerParty(player.getUniqueId());

        // Display party information
        player.sendMessage("§6==== Party Members ====");

        // Leader first
        Player leader = party.getLeaderPlayer();
        if (leader != null) {
            player.sendMessage("§e👑 " + leader.getName() + " §7(Leader)");
        } else {
            player.sendMessage("§e👑 Unknown §7(Leader, offline)");
        }

        // Then other members
        List<Player> onlineMembers = party.getOnlineMembers();
        onlineMembers.remove(leader); // Remove leader as we already displayed them

        for (Player member : onlineMembers) {
            // Mark the respawn target if set
            if (party.getRespawnTarget() != null && party.getRespawnTarget().equals(member.getUniqueId())) {
                player.sendMessage("§a🔄 " + member.getName() + " §7(Respawn Target)");
            } else {
                player.sendMessage("§a" + member.getName());
            }
        }

        // Display party settings
        player.sendMessage("§6==== Party Settings ====");
        player.sendMessage("§eRespawn Mode: §f" + party.getRespawnMode().name());

        if (party.getRespawnTarget() != null) {
            Player target = party.getRespawnTargetPlayer();
            if (target != null) {
                player.sendMessage("§eRespawn Target: §f" + target.getName());
            } else {
                player.sendMessage("§eRespawn Target: §f(Offline)");
            }
        } else {
            player.sendMessage("§eRespawn Target: §fNone");
        }

        player.sendMessage("§6====================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}