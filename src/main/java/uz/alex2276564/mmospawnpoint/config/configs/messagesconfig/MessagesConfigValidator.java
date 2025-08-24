package uz.alex2276564.mmospawnpoint.config.configs.messagesconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.mmospawnpoint.config.utils.validation.Validators;

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

    // ============================= COMMANDS =============================

    private static void validateCommandsSection(ValidationResult result, MessagesConfig.CommandsSection commands) {
        // commands.help
        Validators.notBlank(result, "commands.help.header", commands.help.header, "Help header cannot be empty");
        Validators.notBlank(result, "commands.help.reloadLine", commands.help.reloadLine, "Help reload line cannot be empty");
        Validators.notBlank(result, "commands.help.partyLine", commands.help.partyLine, "Help party line cannot be empty");
        Validators.notBlank(result, "commands.help.helpLine", commands.help.helpLine, "Help help line cannot be empty");

        // commands.reload
        Validators.notBlank(result, "commands.reload.success", commands.reload.success, "Reload success message cannot be empty");
        Validators.notBlank(result, "commands.reload.error", commands.reload.error, "Reload error message cannot be empty");
        if (!commands.reload.success.contains("<type>")) {
            result.addError("commands.reload.success", "Reload success message must contain <type> placeholder");
        }
        if (!commands.reload.error.contains("<error>")) {
            result.addError("commands.reload.error", "Reload error message must contain <error> placeholder");
        }
    }

    // ============================= GENERAL =============================

    private static void validateGeneralSection(ValidationResult result, MessagesConfig.GeneralSection general) {
        Validators.notBlank(result, "general.noSpawnFound", general.noSpawnFound, "No spawn found message cannot be empty");
        Validators.notBlank(result, "general.waitingForResourcePack", general.waitingForResourcePack, "Waiting for resource pack message cannot be empty");
        Validators.notBlank(result, "general.resourcePackLoaded", general.resourcePackLoaded, "Resource pack loaded message cannot be empty");
        Validators.notBlank(result, "general.resourcePackFailed", general.resourcePackFailed, "Resource pack failed message cannot be empty");
    }

    // ============================= PARTY =============================

    private static void validatePartySection(ValidationResult result, MessagesConfig.PartySection party) {
        // Prefix
        Validators.notBlank(result, "party.prefix", party.prefix, "Party prefix cannot be empty");

        // Invite flow
        Validators.notBlank(result, "party.inviteSent", party.inviteSent, "Invite sent message cannot be empty");
        Validators.notBlank(result, "party.inviteReceived", party.inviteReceived, "Invite received message cannot be empty");
        Validators.notBlank(result, "party.invitationDeclined", party.invitationDeclined, "Invitation declined message cannot be empty");
        Validators.notBlank(result, "party.invitationDeclinedToLeader", party.invitationDeclinedToLeader, "Invitation declined (leader) cannot be empty");
        Validators.notBlank(result, "party.invitationExpiredOrInvalid", party.invitationExpiredOrInvalid, "Invitation expired/invalid message cannot be empty");
        Validators.notBlank(result, "party.inviteFailedPartyFull", party.inviteFailedPartyFull, "Invite failed (party full) cannot be empty");
        Validators.notBlank(result, "party.inviteFailedAlreadyInParty", party.inviteFailedAlreadyInParty, "Invite failed (already in party) cannot be empty");
        Validators.notBlank(result, "party.noInvitations", party.noInvitations, "No invitations message cannot be empty");
        Validators.notBlank(result, "party.inviteExpired", party.inviteExpired, "Invite expired message cannot be empty");

        // Placeholders for invite
        if (!party.inviteSent.contains("<player>")) {
            result.addError("party.inviteSent", "Invite sent message must contain <player> placeholder");
        }
        if (!party.inviteReceived.contains("<player>")) {
            result.addError("party.inviteReceived", "Invite received message must contain <player> placeholder");
        }
        if (!party.invitationDeclinedToLeader.contains("<player>")) {
            result.addError("party.invitationDeclinedToLeader", "Invitation declined (leader) must contain <player> placeholder");
        }

        // Join/Leave/Kick
        Validators.notBlank(result, "party.joinedParty", party.joinedParty, "Joined party message cannot be empty");
        Validators.notBlank(result, "party.playerJoinedParty", party.playerJoinedParty, "Player joined party message cannot be empty");
        Validators.notBlank(result, "party.leftParty", party.leftParty, "Left party message cannot be empty");
        Validators.notBlank(result, "party.playerLeftParty", party.playerLeftParty, "Player left party message cannot be empty");
        Validators.notBlank(result, "party.playerRemoved", party.playerRemoved, "Player removed message cannot be empty");
        Validators.notBlank(result, "party.playerRemovedFromParty", party.playerRemovedFromParty, "Player removed from party message cannot be empty");
        Validators.notBlank(result, "party.cannotRemoveSelf", party.cannotRemoveSelf, "Cannot remove self message cannot be empty");
        Validators.notBlank(result, "party.partyDisbanded", party.partyDisbanded, "Party disbanded message cannot be empty");

        // Placeholders for join/leave/kick
        if (!party.playerJoinedParty.contains("<player>")) {
            result.addError("party.playerJoinedParty", "Player joined party message must contain <player> placeholder");
        }
        if (!party.playerLeftParty.contains("<player>")) {
            result.addError("party.playerLeftParty", "Player left party message must contain <player> placeholder");
        }
        if (!party.playerRemovedFromParty.contains("<player>")) {
            result.addError("party.playerRemovedFromParty", "Player removed from party message must contain <player> placeholder");
        }

        // Permissions/system
        Validators.notBlank(result, "party.onlyPlayers", party.onlyPlayers, "Only players message cannot be empty");
        Validators.notBlank(result, "party.systemDisabled", party.systemDisabled, "System disabled message cannot be empty");
        Validators.notBlank(result, "party.notInParty", party.notInParty, "Not in party message cannot be empty");
        Validators.notBlank(result, "party.notLeader", party.notLeader, "Not leader message cannot be empty");
        Validators.notBlank(result, "party.playerNotInYourParty", party.playerNotInYourParty, "Player not in your party message cannot be empty");
        Validators.notBlank(result, "party.invalidRespawnMode", party.invalidRespawnMode, "Invalid respawn mode message cannot be empty");
        Validators.notBlank(result, "party.errorOccurred", party.errorOccurred, "Error occurred message cannot be empty");

        // Leader / Options
        Validators.notBlank(result, "party.newLeaderAssigned", party.newLeaderAssigned, "New leader assigned message cannot be empty");
        Validators.notBlank(result, "party.respawnModeChanged", party.respawnModeChanged, "Respawn mode changed message cannot be empty");
        Validators.notBlank(result, "party.respawnTargetSet", party.respawnTargetSet, "Respawn target set message cannot be empty");
        Validators.notBlank(result, "party.alreadyLeader", party.alreadyLeader, "Already leader message cannot be empty");

        if (!party.newLeaderAssigned.contains("<player>")) {
            result.addError("party.newLeaderAssigned", "New leader assigned must contain <player> placeholder");
        }
        if (!party.respawnModeChanged.contains("<mode>")) {
            result.addError("party.respawnModeChanged", "Respawn mode changed must contain <mode> placeholder");
        }
        if (!party.respawnTargetSet.contains("<player>")) {
            result.addError("party.respawnTargetSet", "Respawn target set must contain <player> placeholder");
        }

        // Respawn flow
        Validators.notBlank(result, "party.respawnedAtMember", party.respawnedAtMember, "Respawned at member message cannot be empty");
        Validators.notBlank(result, "party.respawnDisabledRegion", party.respawnDisabledRegion, "Respawn disabled (region) cannot be empty");
        Validators.notBlank(result, "party.respawnDisabledWorld", party.respawnDisabledWorld, "Respawn disabled (world) cannot be empty");
        Validators.notBlank(result, "party.respawnCooldown", party.respawnCooldown, "Respawn cooldown message cannot be empty");
        Validators.notBlank(result, "party.walkingSpawnPointMessage", party.walkingSpawnPointMessage, "Walking spawn point message cannot be empty");
        Validators.notBlank(result, "party.walkingSpawnPointRestricted", party.walkingSpawnPointRestricted, "Walking spawn point restricted cannot be empty");

        if (!party.respawnedAtMember.contains("<player>")) {
            result.addError("party.respawnedAtMember", "Respawned at member must contain <player> placeholder");
        }
        if (!party.respawnCooldown.contains("<time>")) {
            result.addError("party.respawnCooldown", "Respawn cooldown must contain <time> placeholder");
        }

        // List command
        Validators.notBlank(result, "party.listHeader", party.listHeader, "List header cannot be empty");
        Validators.notBlank(result, "party.listLeader", party.listLeader, "List leader cannot be empty");
        Validators.notBlank(result, "party.listLeaderMissing", party.listLeaderMissing, "List leader missing cannot be empty");
        Validators.notBlank(result, "party.listMember", party.listMember, "List member cannot be empty");
        Validators.notBlank(result, "party.listAnchor", party.listAnchor, "List anchor cannot be empty");
        Validators.notBlank(result, "party.listAnchorMissing", party.listAnchorMissing, "List anchor missing cannot be empty");
        Validators.notBlank(result, "party.listSettingsHeader", party.listSettingsHeader, "List settings header cannot be empty");
        Validators.notBlank(result, "party.listRespawnMode", party.listRespawnMode, "List respawn mode cannot be empty");
        Validators.notBlank(result, "party.listNoAnchor", party.listNoAnchor, "List no anchor cannot be empty");
        Validators.notBlank(result, "party.listSeparator", party.listSeparator, "List separator cannot be empty");

        if (!party.listLeader.contains("<player>")) {
            result.addError("party.listLeader", "List leader must contain <player> placeholder");
        }
        if (!party.listMember.contains("<player>")) {
            result.addError("party.listMember", "List member must contain <player> placeholder");
        }
        if (!party.listAnchor.contains("<player>")) {
            result.addError("party.listAnchor", "List anchor must contain <player> placeholder");
        }
        if (!party.listRespawnMode.contains("<mode>")) {
            result.addError("party.listRespawnMode", "List respawn mode must contain <mode> placeholder");
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

    // ============================= JOINS =============================

    private static void validateJoinsSection(ValidationResult result, MessagesConfig.JoinsSection joins) {
        Validators.notBlank(result, "joins.teleportedOnJoin", joins.teleportedOnJoin, "Teleported on join message cannot be empty");
        Validators.notBlank(result, "joins.skippedDead", joins.skippedDead, "Skipped dead message cannot be empty");
        Validators.notBlank(result, "joins.waitingInRoom", joins.waitingInRoom, "Waiting in room message cannot be empty");
        Validators.notBlank(result, "joins.resourcePackTimeout", joins.resourcePackTimeout, "Resource pack timeout message cannot be empty");
    }
}