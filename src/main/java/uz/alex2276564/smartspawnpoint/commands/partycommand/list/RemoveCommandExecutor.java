package uz.alex2276564.smartspawnpoint.commands.partycommand.list;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.SubCommand;
import uz.alex2276564.smartspawnpoint.party.Party;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoveCommandExecutor implements SubCommand {
    private final SmartSpawnPoint plugin;

    public RemoveCommandExecutor(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /smartspawnpoint party remove <player>");
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

        // Check if player is party leader
        if (!party.isLeader(player.getUniqueId())) {
            String message = plugin.getConfigManager().getPartyMessage("not-leader");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            } else {
                player.sendMessage("§cOnly the party leader can remove players.");
            }
            return;
        }

        // Get target player
        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            String message = plugin.getConfigManager().getPartyMessage("player-not-found");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            } else {
                player.sendMessage("§cPlayer not found or offline.");
            }
            return;
        }

        // Check if target is in the party
        if (!party.isMember(targetPlayer.getUniqueId())) {
            String message = plugin.getConfigManager().getPartyMessage("player-not-in-party");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            } else {
                player.sendMessage("§cThis player is not in your party.");
            }
            return;
        }

        // Can't remove yourself (use leave instead)
        if (targetPlayer.equals(player)) {
            player.sendMessage("§cYou cannot remove yourself from the party. Use /smartspawnpoint party leave instead.");
            return;
        }

        // Remove player from party
        boolean success = partyManager.removePlayer(player, targetPlayer);

        if (success) {
            // Notify target player
            String targetMessage = plugin.getConfigManager().getPartyMessage("player-kicked");
            if (!targetMessage.isEmpty()) {
                // Replace %player% with the player's name for self-messages
                targetMessage = targetMessage.replace("%player%", targetPlayer.getName()).replace("&", "§");
                targetPlayer.sendMessage(targetMessage);
            } else {
                targetPlayer.sendMessage("§cYou have been removed from the party.");
            }

            // Notify party members
            for (Player member : party.getOnlineMembers()) {
                if (!member.equals(targetPlayer)) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("player", targetPlayer.getName());
                    String message = plugin.getConfigManager().formatPartyMessage("player-kicked", replacements);

                    if (!message.isEmpty()) {
                        member.sendMessage(message);
                    } else {
                        member.sendMessage("§c" + targetPlayer.getName() + " has been removed from the party.");
                    }
                }
            }
        } else {
            player.sendMessage("§cCouldn't remove player from the party. An error occurred.");
        }
    }



    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            // Only show players in the same party
            if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
                Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
                String partial = args[0].toLowerCase();

                return party.getOnlineMembers().stream()
                        .filter(member -> !member.equals(player)) // Exclude self
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }
}