package uz.alex2276564.mmospawnpoint.commands.subcommands.party.options;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.mode.ModeSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.target.TargetSubCommand;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

public class OptionsSubCommand implements NestedSubCommandProvider {

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        SubCommandBuilder optionsBuilder = parent.subcommand("options")
                .permission("mmospawnpoint.party.options")
                .description("View and change party options")
                .executor((sender, context) -> {
                    MMOSpawnPoint plugin = MMOSpawnPoint.getInstance();

                    if (!(sender instanceof Player player)) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "party.onlyPlayers",
                                plugin.getConfigManager().getMessagesConfig().party.onlyPlayers);
                        return;
                    }

                    if (!plugin.getConfigManager().getMainConfig().party.enabled) {
                        plugin.getMessageManager().sendMessageKeyed(sender, "party.systemDisabled",
                                plugin.getConfigManager().getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.notInParty",
                                plugin.getConfigManager().getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        plugin.getMessageManager().sendMessageKeyed(player, "party.notLeader",
                                plugin.getConfigManager().getMessagesConfig().party.notLeader);
                        return;
                    }

                    // Display current options
                    var options = plugin.getConfigManager().getMessagesConfig().party.options;

                    plugin.getMessageManager().sendMessageKeyed(player,
                            "party.options.header",
                            options.header);

                    plugin.getMessageManager().sendMessageKeyed(player,
                            "party.options.respawnMode",
                            options.respawnMode,
                            "mode", party.getRespawnMode().name());

                    if (party.getRespawnTarget() == null) {
                        plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.respawnTargetNoneLine",
                                options.respawnTargetNoneLine);
                    } else {
                        Player targetPlayer = party.getRespawnTargetPlayer();
                        if (targetPlayer != null) {
                            plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.options.respawnTarget",
                                    options.respawnTarget,
                                    "target", targetPlayer.getName());
                        } else {
                            plugin.getMessageManager().sendMessageKeyed(player,
                                    "party.options.respawnTargetNotFoundLine",
                                    options.respawnTargetNotFoundLine);
                        }
                    }

                    PartyManager.PersonalWalkingSpawnPointStatus personalWalkingStatus =
                            plugin.getPartyManager().getPersonalWalkingSpawnPointStatus(player);

                    switch (personalWalkingStatus) {
                        case ACTIVE -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.walkingSpawnPointActive",
                                options.walkingSpawnPointActive);
                        case INACTIVE_MODE_NORMAL -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.walkingSpawnPointInactiveModeNormal",
                                options.walkingSpawnPointInactiveModeNormal);
                        case UNAVAILABLE_GLOBAL_DISABLED -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.walkingSpawnPointUnavailableGlobal",
                                options.walkingSpawnPointUnavailableGlobal);
                        case UNAVAILABLE_NO_PERMISSION -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.walkingSpawnPointUnavailableNoPermission",
                                options.walkingSpawnPointUnavailableNoPermission);
                    }

                    PartyManager.TargetWalkingSpawnPointStatus targetWalkingStatus =
                            plugin.getPartyManager().getTargetWalkingSpawnPointStatus(player);

                    switch (targetWalkingStatus) {
                        case ACTIVE -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointActive",
                                options.targetWalkingSpawnPointActive);
                        case INACTIVE_MODE_NORMAL -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointInactiveModeNormal",
                                options.targetWalkingSpawnPointInactiveModeNormal);
                        case UNAVAILABLE_GLOBAL_DISABLED -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointUnavailableGlobal",
                                options.targetWalkingSpawnPointUnavailableGlobal);
                        case UNAVAILABLE_NO_PERMISSION -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointUnavailableNoPermission",
                                options.targetWalkingSpawnPointUnavailableNoPermission);
                        case NO_TARGET -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointNoTarget",
                                options.targetWalkingSpawnPointNoTarget);
                        case TARGET_NOT_FOUND -> plugin.getMessageManager().sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointTargetMissing",
                                options.targetWalkingSpawnPointTargetMissing);
                    }

                    plugin.getMessageManager().sendMessageKeyed(player,
                            "party.options.separator",
                            options.separator);

                    plugin.getMessageManager().sendMessageKeyed(player,
                            "party.options.modeHelp",
                            options.modeHelp);

                    plugin.getMessageManager().sendMessageKeyed(player,
                            "party.options.targetHelp",
                            options.targetHelp);
                    
                    plugin.getMessageManager().sendMessageKeyed(player,
                            "party.options.walkingSafetyNote",
                            options.walkingSafetyNote);
                });

        // Register nested options subcommands
        new ModeSubCommand().build(optionsBuilder);
        new TargetSubCommand().build(optionsBuilder);

        return optionsBuilder;
    }
}