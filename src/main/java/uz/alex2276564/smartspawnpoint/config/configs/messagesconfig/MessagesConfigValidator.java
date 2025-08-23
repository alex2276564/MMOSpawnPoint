package uz.alex2276564.smartspawnpoint.config.configs.messagesconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.smartspawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.smartspawnpoint.config.utils.validation.Validators;

@UtilityClass
public class MessagesConfigValidator {

    public static void validate(MessagesConfig config) {
        ValidationResult result = new ValidationResult();

        validateCommandsSection(result, config.commands);
        validateGeneralSection(result, config.general);
        validatePartySection(result, config.party);
        validateJoinsSection(result, config.joins);

        result.throwIfInvalid("Messages configuration");
    }

    private static void validateCommandsSection(ValidationResult result, MessagesConfig.CommandsSection commands) {
        // Help section validation
        Validators.notBlank(result, "commands.help.header", commands.help.header, "Help header cannot be empty");
        Validators.notBlank(result, "commands.help.reloadLine", commands.help.reloadLine, "Help reload line cannot be empty");
        Validators.notBlank(result, "commands.help.partyLine", commands.help.partyLine, "Help party line cannot be empty");
        Validators.notBlank(result, "commands.help.helpLine", commands.help.helpLine, "Help help line cannot be empty");

        // Reload section validation
        Validators.notBlank(result, "commands.reload.success", commands.reload.success, "Reload success message cannot be empty");
        Validators.notBlank(result, "commands.reload.error", commands.reload.error, "Reload error message cannot be empty");

        // Check for required placeholders
        if (!commands.reload.success.contains("<type>")) {
            result.addError("commands.reload.success", "Reload success message must contain <type> placeholder");
        }
        if (!commands.reload.error.contains("<error>")) {
            result.addError("commands.reload.error", "Reload error message must contain <error> placeholder");
        }
    }

    private static void validateGeneralSection(ValidationResult result, MessagesConfig.GeneralSection general) {
        Validators.notBlank(result, "general.noSpawnFound", general.noSpawnFound, "No spawn found message cannot be empty");
        Validators.notBlank(result, "general.waitingForResourcePack", general.waitingForResourcePack, "Waiting for resource pack message cannot be empty");
        Validators.notBlank(result, "general.resourcePackLoaded", general.resourcePackLoaded, "Resource pack loaded message cannot be empty");
        Validators.notBlank(result, "general.resourcePackFailed", general.resourcePackFailed, "Resource pack failed message cannot be empty");
    }

    private static void validatePartySection(ValidationResult result, MessagesConfig.PartySection party) {
        Validators.notBlank(result, "party.prefix", party.prefix, "Party prefix cannot be empty");
        Validators.notBlank(result, "party.inviteSent", party.inviteSent, "Invite sent message cannot be empty");
        Validators.notBlank(result, "party.inviteReceived", party.inviteReceived, "Invite received message cannot be empty");
        Validators.notBlank(result, "party.respawnedAtMember", party.respawnedAtMember, "Respawned at member message cannot be empty");
        Validators.notBlank(result, "party.walkingSpawnPointMessage", party.walkingSpawnPointMessage, "Walking spawn point message cannot be empty");

        // Check for required placeholders in key messages
        if (!party.inviteSent.contains("{player}")) {
            result.addError("party.inviteSent", "Invite sent message must contain {player} placeholder");
        }
        if (!party.respawnedAtMember.contains("{player}")) {
            result.addError("party.respawnedAtMember", "Respawned at member message must contain {player} placeholder");
        }
        if (!party.respawnCooldown.contains("{time}")) {
            result.addError("party.respawnCooldown", "Respawn cooldown message must contain {time} placeholder");
        }

        // Validate help section
        validatePartyHelpSection(result, party.help);
    }

    private static void validatePartyHelpSection(ValidationResult result, MessagesConfig.PartySection.PartyHelpSection help) {
        Validators.notBlank(result, "party.help.header", help.header, "Party help header cannot be empty");
        Validators.notBlank(result, "party.help.invite", help.invite, "Party help invite cannot be empty");
        Validators.notBlank(result, "party.help.accept", help.accept, "Party help accept cannot be empty");
        Validators.notBlank(result, "party.help.deny", help.deny, "Party help deny cannot be empty");
        Validators.notBlank(result, "party.help.leave", help.leave, "Party help leave cannot be empty");
        Validators.notBlank(result, "party.help.list", help.list, "Party help list cannot be empty");
        Validators.notBlank(result, "party.help.remove", help.remove, "Party help remove cannot be empty");
        Validators.notBlank(result, "party.help.setleader", help.setleader, "Party help setleader cannot be empty");
        Validators.notBlank(result, "party.help.options", help.options, "Party help options cannot be empty");
    }

    private static void validateJoinsSection(ValidationResult result, MessagesConfig.JoinsSection joins) {
        Validators.notBlank(result, "joins.teleportedOnJoin", joins.teleportedOnJoin, "Teleported on join message cannot be empty");
        Validators.notBlank(result, "joins.skippedDead", joins.skippedDead, "Skipped dead message cannot be empty");
        Validators.notBlank(result, "joins.waitingInRoom", joins.waitingInRoom, "Waiting in room message cannot be empty");
        Validators.notBlank(result, "joins.resourcePackTimeout", joins.resourcePackTimeout, "Resource pack timeout message cannot be empty");
    }
}