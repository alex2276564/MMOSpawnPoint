package uz.alex2276564.mmospawnpoint.commands.subcommands.party.options.mode;

import org.bukkit.entity.Player;
import uz.alex2276564.mmospawnpoint.MMOSpawnPointServices;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentBuilder;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.ArgumentType;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.NestedSubCommandProvider;
import uz.alex2276564.mmospawnpoint.commands.framework.builder.SubCommandBuilder;
import uz.alex2276564.mmospawnpoint.party.Party;
import uz.alex2276564.mmospawnpoint.party.PartyManager;

public class ModeSubCommand implements NestedSubCommandProvider {

    private final MMOSpawnPointServices services;

    public ModeSubCommand(MMOSpawnPointServices services) {
        this.services = services;
    }

    @Override
    public SubCommandBuilder build(SubCommandBuilder parent) {
        return parent.subcommand("mode")
                .permission("mmospawnpoint.party.options.mode")
                .description("Change party respawn mode")
                .argument(new ArgumentBuilder<>("mode", ArgumentType.STRING)
                        .suggestions("NORMAL", "PARTY_MEMBER"))
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

                    String mode = context.getArgument("mode");
                    try {
                        Party.RespawnMode respawnMode = Party.RespawnMode.valueOf(mode.toUpperCase());
                        partyManager.setRespawnMode(player, respawnMode);

                        String modeMessage =
                                configManager.getMessagesConfig().party.respawnModeChanged;
                        messageManager.sendMessageKeyed(player, "party.respawnModeChanged",
                                modeMessage, "mode", respawnMode.name());
                    } catch (IllegalArgumentException e) {
                        messageManager.sendMessageKeyed(player, "party.invalidRespawnMode",
                                configManager.getMessagesConfig().party.invalidRespawnMode);
                    }
                });
    }
}