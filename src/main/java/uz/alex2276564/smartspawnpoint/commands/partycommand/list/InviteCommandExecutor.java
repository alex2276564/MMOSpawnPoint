package uz.alex2276564.smartspawnpoint.commands.partycommand.list;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.SubCommand;
import uz.alex2276564.smartspawnpoint.party.PartyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InviteCommandExecutor implements SubCommand {
    private final SmartSpawnPoint plugin;

    public InviteCommandExecutor(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /smartspawnpoint party invite <player>");
            return;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null || !targetPlayer.isOnline()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", targetName);
            String message = plugin.getConfigManager().formatPartyMessage("player-not-found", replacements);
            if (!message.isEmpty()) {
                player.sendMessage(message);
            } else {
                player.sendMessage("§cPlayer not found or offline.");
            }
            return;
        }

        PartyManager partyManager = plugin.getPartyManager();

        // Create party if player is not in one
        if (!partyManager.isInParty(player.getUniqueId())) {
            partyManager.createParty(player);
        }

        // Send invitation
        boolean success = partyManager.invitePlayer(player, targetPlayer);

        if (success) {
            // Send message to inviter
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", targetPlayer.getName());
            String message = plugin.getConfigManager().formatPartyMessage("invite-sent", replacements);
            if (!message.isEmpty()) {
                player.sendMessage(message);
            } else {
                player.sendMessage("§aInvitation sent to " + targetPlayer.getName());
            }

            // Send message to invited player
            replacements = new HashMap<>();
            replacements.put("player", player.getName());
            message = plugin.getConfigManager().formatPartyMessage("invite-received", replacements);
            if (!message.isEmpty()) {
                targetPlayer.sendMessage(message);
            } else {
                targetPlayer.sendMessage("§aYou've been invited to join " + player.getName() + "'s party. Type /smartspawnpoint party accept to join.");
            }
        } else {
            player.sendMessage("§cCouldn't invite player. They might already be in a party or your party is full.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}