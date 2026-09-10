package uz.alex2276564.mmospawnpoint.commands.subcommands.party.options;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.mode.ModeSubCommand;
import uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.target.TargetSubCommand;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

import java.util.UUID;

public class OptionsSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public OptionsSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        SubCommandBuilder optionsBuilder = parent.subcommand("options")
                .permission("mmospawnpoint.party.options")
                .description("View and change party options")
                .executor((sender, context) -> {
                    var configManager = services.configManager();
                    var messageManager = services.messageManager();
                    PartyManager partyManager = services.partyManager();

                    if (!(sender instanceof Player player)) {
                        messageManager.sendMessageKeyed(sender, "party.onlyPlayers",
                                configManager.getMessagesConfig().party.onlyPlayers);
                        return;
                    }

                    if (!configManager.getMainConfig().party.enabled || partyManager == null) {
                        messageManager.sendMessageKeyed(sender, "party.systemDisabled",
                                configManager.getMessagesConfig().party.systemDisabled);
                        return;
                    }

                    if (!partyManager.isInParty(player.getUniqueId())) {
                        messageManager.sendMessageKeyed(player, "party.notInParty",
                                configManager.getMessagesConfig().party.notInParty);
                        return;
                    }

                    Party party = partyManager.getPlayerParty(player.getUniqueId());

                    // Check if player is party leader
                    if (!party.isLeader(player.getUniqueId())) {
                        messageManager.sendMessageKeyed(player, "party.notLeader",
                                configManager.getMessagesConfig().party.notLeader);
                        return;
                    }

                    // Display current options
                    var options = configManager.getMessagesConfig().party.options;

                    messageManager.sendMessageKeyed(player,
                            "party.options.header",
                            options.header);

                    messageManager.sendMessageKeyed(player,
                            "party.options.respawnMode",
                            options.respawnMode,
                            "mode", party.getRespawnMode().name());

                    UUID targetId = party.getRespawnTarget();
                    if (targetId == null) {
                        messageManager.sendMessageKeyed(player,
                                "party.options.respawnTargetNoneLine",
                                options.respawnTargetNoneLine);
                    } else {
                        var offlineTarget = Bukkit.getOfflinePlayer(targetId);
                        String targetName = offlineTarget.getName();

                        if (targetName == null) {
                            messageManager.sendMessageKeyed(player,
                                    "party.options.respawnTargetNotFoundLine",
                                    options.respawnTargetNotFoundLine);
                        } else if (offlineTarget.isOnline()) {
                            messageManager.sendMessageKeyed(player,
                                    "party.options.respawnTarget",
                                    options.respawnTarget,
                                    "target", targetName);
                        } else {
                            messageManager.sendMessageKeyed(player,
                                    "party.options.respawnTargetOfflineLine",
                                    options.respawnTargetOfflineLine,
                                    "player", targetName);
                        }
                    }

                    PartyManager.PersonalWalkingSpawnPointStatus personalWalkingStatus =
                            partyManager.getPersonalWalkingSpawnPointStatus(player);

                    switch (personalWalkingStatus) {
                        case ACTIVE -> messageManager.sendMessageKeyed(player,
                                "party.options.walkingSpawnPointActive",
                                options.walkingSpawnPointActive);
                        case INACTIVE_MODE_NORMAL -> messageManager.sendMessageKeyed(player,
                                "party.options.walkingSpawnPointInactiveModeNormal",
                                options.walkingSpawnPointInactiveModeNormal);
                        case UNAVAILABLE_GLOBAL_DISABLED -> messageManager.sendMessageKeyed(player,
                                "party.options.walkingSpawnPointUnavailableGlobal",
                                options.walkingSpawnPointUnavailableGlobal);
                        case UNAVAILABLE_NO_PERMISSION -> messageManager.sendMessageKeyed(player,
                                "party.options.walkingSpawnPointUnavailableNoPermission",
                                options.walkingSpawnPointUnavailableNoPermission);
                    }

                    PartyManager.TargetWalkingSpawnPointStatus targetWalkingStatus =
                            partyManager.getTargetWalkingSpawnPointStatus(player);

                    switch (targetWalkingStatus) {
                        case ACTIVE -> messageManager.sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointActive",
                                options.targetWalkingSpawnPointActive);
                        case INACTIVE_MODE_NORMAL -> messageManager.sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointInactiveModeNormal",
                                options.targetWalkingSpawnPointInactiveModeNormal);
                        case UNAVAILABLE_GLOBAL_DISABLED -> messageManager.sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointUnavailableGlobal",
                                options.targetWalkingSpawnPointUnavailableGlobal);
                        case UNAVAILABLE_NO_PERMISSION -> messageManager.sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointUnavailableNoPermission",
                                options.targetWalkingSpawnPointUnavailableNoPermission);
                        case NO_TARGET -> messageManager.sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointNoTarget",
                                options.targetWalkingSpawnPointNoTarget);
                        case TARGET_OFFLINE -> messageManager.sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointTargetOffline",
                                options.targetWalkingSpawnPointTargetOffline);
                        case TARGET_NOT_FOUND -> messageManager.sendMessageKeyed(player,
                                "party.options.targetWalkingSpawnPointTargetMissing",
                                options.targetWalkingSpawnPointTargetMissing);
                    }

                    messageManager.sendMessageKeyed(player,
                            "party.options.separator",
                            options.separator);

                    messageManager.sendMessageKeyed(player,
                            "party.options.modeHelp",
                            options.modeHelp);

                    messageManager.sendMessageKeyed(player,
                            "party.options.targetHelp",
                            options.targetHelp);

                    messageManager.sendMessageKeyed(player,
                            "party.options.walkingSafetyNote",
                            options.walkingSafetyNote);
                });

        // Register nested options subcommands
        new ModeSubCommand(services).build(optionsBuilder);
        new TargetSubCommand(services).build(optionsBuilder);

        return optionsBuilder;
    }
}