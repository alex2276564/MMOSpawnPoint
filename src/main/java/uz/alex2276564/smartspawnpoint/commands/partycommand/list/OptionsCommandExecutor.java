package uz.alex2276564.smartspawnpoint.commands.partycommand.list;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import uz.alex2276564.smartspawnpoint.SmartSpawnPoint;
import uz.alex2276564.smartspawnpoint.commands.SubCommand;
import uz.alex2276564.smartspawnpoint.party.Party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OptionsCommandExecutor implements SubCommand {
    private final SmartSpawnPoint plugin;

    public OptionsCommandExecutor(SmartSpawnPoint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return;
        }

        if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
            String message = plugin.getConfigManager().getPartyMessage("not-in-party");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            } else {
                player.sendMessage("§cYou are not in a party.");
            }
            return;
        }

        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());

        // Check if player is party leader
        if (!party.isLeader(player.getUniqueId())) {
            String message = plugin.getConfigManager().getPartyMessage("not-leader");
            if (!message.isEmpty()) {
                player.sendMessage(message.replace("&", "§"));
            } else {
                player.sendMessage("§cOnly the party leader can change options.");
            }
            return;
        }

        if (args.length == 0) {
            // Display current options
            player.sendMessage("§6==== Party Options ====");
            player.sendMessage("§eRespawn Mode: §f" + party.getRespawnMode().name());
            player.sendMessage("§eRespawn Target: §f" + (party.getRespawnTarget() != null ?
                    (party.getRespawnTargetPlayer() != null ? party.getRespawnTargetPlayer().getName() : "Unknown") : "None"));
            player.sendMessage("§6====================");
            player.sendMessage("§7Use §f/smartspawnpoint party options mode <normal|party_member>§7 to change respawn mode");
            player.sendMessage("§7Use §f/smartspawnpoint party options target <player>§7 to set respawn target");
            return;
        }

        String subCommand = args[0].toLowerCase();

        if ("mode".equals(subCommand)) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /smartspawnpoint party options mode <normal|party_member>");
                return;
            }

            String mode = args[1].toUpperCase();
            try {
                Party.RespawnMode respawnMode = Party.RespawnMode.valueOf(mode);
                plugin.getPartyManager().setRespawnMode(player, respawnMode);

                Map<String, String> replacements = new HashMap<>();
                replacements.put("mode", respawnMode.name());
                String message = plugin.getConfigManager().formatPartyMessage("respawn-mode-changed", replacements);
                if (!message.isEmpty()) {
                    player.sendMessage(message);
                } else {
                    player.sendMessage("§aParty respawn mode set to: " + respawnMode.name());
                }
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid respawn mode. Use 'NORMAL' or 'PARTY_MEMBER'.");
            }
        } else if ("target".equals(subCommand)) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /smartspawnpoint party options target <player>");
                return;
            }

            String targetName = args[1];
            Player targetPlayer = plugin.getServer().getPlayer(targetName);

            if (targetPlayer == null || !targetPlayer.isOnline()) {
                String message = plugin.getConfigManager().getPartyMessage("player-not-found");
                if (!message.isEmpty()) {
                    player.sendMessage(message.replace("&", "§"));
                } else {
                    player.sendMessage("§cPlayer not found or offline.");
                }
                return;
            }

            if (!party.isMember(targetPlayer.getUniqueId())) {
                String message = plugin.getConfigManager().getPartyMessage("player-not-in-party");
                if (!message.isEmpty()) {
                    player.sendMessage(message.replace("&", "§"));
                } else {
                    player.sendMessage("§cThis player is not in your party.");
                }
                return;
            }

            if (plugin.getPartyManager().setRespawnTarget(player, targetPlayer)) {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("player", targetPlayer.getName());
                String message = plugin.getConfigManager().formatPartyMessage("respawn-target-set", replacements);
                if (!message.isEmpty()) {
                    player.sendMessage(message);
                } else {
                    player.sendMessage("§aYou will now respawn at " + targetPlayer.getName() + "'s location.");
                }
            }
        } else {
            player.sendMessage("§cUnknown option: " + subCommand);
            player.sendMessage("§7Available options: mode, target");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("mode", "target");
        } else if (args.length == 2) {
            if ("mode".equals(args[0].toLowerCase())) {
                return List.of("NORMAL", "PARTY_MEMBER");
            } else if ("target".equals(args[0].toLowerCase())) {
                if (sender instanceof Player player && plugin.getPartyManager().isInParty(player.getUniqueId())) {
                    Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
                    return party.getOnlineMembers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>();
    }
}