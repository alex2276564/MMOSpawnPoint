package uz.alex2276564.mmospawnpoint.config.configs.mainconfig;

import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.mmospawnpoint.config.utils.validation.Validators;

import java.util.Set;

@UtilityClass
public class MainConfigValidator {

    public static void validate(MainConfig config) {
        ValidationResult result = new ValidationResult();

        validateSettingsSection(result, config.settings);
        validatePartySection(result, config.party);
        validateJoinSection(result, config.join);

        result.throwIfInvalid("Main configuration");
    }

    private static void validateSettingsSection(ValidationResult result, MainConfig.SettingsSection settings) {

        // Validate numeric values
        Validators.min(result, "settings.maxSafeLocationAttempts", settings.maxSafeLocationAttempts, 1, "Max safe location attempts must be at least 1");
        Validators.max(result, "settings.maxSafeLocationAttempts", settings.maxSafeLocationAttempts, 200, "Max safe location attempts cannot exceed 200");

        Validators.min(result, "settings.safeLocationRadius", settings.safeLocationRadius, 1, "Safe location radius must be at least 1");
        Validators.max(result, "settings.safeLocationRadius", settings.safeLocationRadius, 50, "Safe location radius cannot exceed 50");

        // Validate cache settings
        validateCacheSection(result, settings.safeLocationCache);

        // Validate Global Passable Blacklist materials
        validateMaterialListOrFail(result, settings.globalPassableBlacklist, "settings.globalPassableBlacklist");

        validateSafeSearchBatch(result, settings.safeSearchBatch);

        validateMaintenanceSection(result, settings.maintenance);

        // Validate teleport settings
        validateTeleportSection(result, settings.teleport);

        // Validate waiting room settings
        validateWaitingRoomSection(result, settings.waitingRoom);

        // Validate Global Ground Blacklist materials
        validateMaterialListOrFail(result, settings.globalGroundBlacklist, "settings.globalGroundBlacklist");
    }

    private static void validateMaterialListOrFail(ValidationResult result, java.util.List<String> list, String pathBase) {
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            String raw = list.get(i);
            Material mat = Material.matchMaterial(raw);
            String path = pathBase + "[" + i + "]";
            if (mat == null) {
                result.addError(path, "Unknown material: " + raw);
                continue;
            }
            // Avoid Material.isLegacy(); use name heuristic instead
            if (mat.name().startsWith("LEGACY_")) {
                result.addError(path, "Legacy material is not supported: " + raw);
            }
        }
    }

    private static void validateCacheSection(ValidationResult result, MainConfig.SafeLocationCacheSection cache) {
        Validators.min(result, "settings.safeLocationCache.expiryTime", cache.expiryTime, 1, "Cache expiry time must be at least 1 second");
        Validators.max(result, "settings.safeLocationCache.expiryTime", cache.expiryTime, 3600, "Cache expiry time cannot exceed 1 hour");

        Validators.min(result, "settings.safeLocationCache.maxCacheSize", cache.maxCacheSize, 10, "Max cache size must be at least 10");
        Validators.max(result, "settings.safeLocationCache.maxCacheSize", cache.maxCacheSize, 10000, "Max cache size cannot exceed 10000");

    }

    private static void validateSafeSearchBatch(ValidationResult result, MainConfig.SafeSearchBatchSection b) {
        Validators.min(result, "settings.safeSearchBatch.attemptsPerTick", b.attemptsPerTick, 10, "attemptsPerTick must be >= 10");
        Validators.max(result, "settings.safeSearchBatch.attemptsPerTick", b.attemptsPerTick, 5000, "attemptsPerTick too high");
        Validators.min(result, "settings.safeSearchBatch.timeBudgetMillis", b.timeBudgetMillis, 1, "timeBudgetMillis must be >= 1");
        Validators.max(result, "settings.safeSearchBatch.timeBudgetMillis", b.timeBudgetMillis, 20, "timeBudgetMillis too high");
    }

    private static void validateMaintenanceSection(ValidationResult result, MainConfig.MaintenanceSection m) {
        Validators.min(result, "settings.maintenance.maxFolderDepth", m.maxFolderDepth, 1, "Max folder depth must be >= 1");
        Validators.min(result, "settings.maintenance.partyCleanupPeriodTicks", m.partyCleanupPeriodTicks, 20, "Party cleanup period must be >= 20 ticks");
        Validators.min(result, "settings.maintenance.invitationCleanupPeriodTicks", m.invitationCleanupPeriodTicks, 20, "Invitation cleanup period must be >= 20 ticks");
    }

    private static void validateTeleportSection(ValidationResult result, MainConfig.TeleportSection teleport) {
        // delayTicks
        Validators.min(result, "settings.teleport.delayTicks", teleport.delayTicks, 0, "Teleport delay must be at least 0 tick");
        Validators.max(result, "settings.teleport.delayTicks", teleport.delayTicks, 200, "Teleport delay cannot exceed 200 ticks (10 seconds)");

        if (teleport.ySelection == null) {
            result.addError("settings.teleport.ySelection", "ySelection section cannot be null");
            return;
        }

        // Overworld
        var ow = teleport.ySelection.overworld;
        if (ow == null) {
            result.addError("settings.teleport.ySelection.overworld", "Overworld ySelection cannot be null");
        } else {
            Set<String> modes = Set.of("mixed", "highest_only", "random_only");
            String mode = ow.mode == null ? "" : ow.mode.toLowerCase();
            if (!modes.contains(mode)) {
                result.addError("settings.teleport.ySelection.overworld.mode", "Invalid mode. Valid: mixed, highest_only, random_only");
            }
            if ("mixed".equals(mode)) {
                Set<String> firsts = Set.of("highest", "random");
                String first = ow.first == null ? "" : ow.first.toLowerCase();
                if (!firsts.contains(first)) {
                    result.addError("settings.teleport.ySelection.overworld.first", "Invalid first group. Valid: highest, random");
                }
                double share = ow.firstShare;
                if (Double.isNaN(share) || Double.isInfinite(share) || share < 0.0 || share > 1.0) {
                    result.addError("settings.teleport.ySelection.overworld.firstShare", "firstShare must be within [0.0 .. 1.0]");
                }
            }
        }

        // End
        var en = teleport.ySelection.end;
        if (en == null) {
            result.addError("settings.teleport.ySelection.end", "End ySelection cannot be null");
        } else {
            Set<String> modes = Set.of("mixed", "highest_only", "random_only");
            String mode = en.mode == null ? "" : en.mode.toLowerCase();
            if (!modes.contains(mode)) {
                result.addError("settings.teleport.ySelection.end.mode", "Invalid mode. Valid: mixed, highest_only, random_only");
            }
            if ("mixed".equals(mode)) {
                Set<String> firsts = Set.of("highest", "random");
                String first = en.first == null ? "" : en.first.toLowerCase();
                if (!firsts.contains(first)) {
                    result.addError("settings.teleport.ySelection.end.first", "Invalid first group. Valid: highest, random");
                }
                double share = en.firstShare;
                if (Double.isNaN(share) || Double.isInfinite(share) || share < 0.0 || share > 1.0) {
                    result.addError("settings.teleport.ySelection.end.firstShare", "firstShare must be within [0.0 .. 1.0]");
                }
            }
        }

        // Nether
        var ne = teleport.ySelection.nether;
        if (ne == null) {
            result.addError("settings.teleport.ySelection.nether", "Nether ySelection cannot be null");
        } else {
            Set<String> modes = Set.of("scan", "highest_only", "random_only");
            String mode = ne.mode == null ? "" : ne.mode.toLowerCase();
            if (!modes.contains(mode)) {
                result.addError("settings.teleport.ySelection.nether.mode", "Invalid mode. Valid: scan, highest_only, random_only");
            }
            // respectRange is boolean, no numeric validation needed
        }

        // Custom
        var cu = teleport.ySelection.custom;
        if (cu == null) {
            result.addError("settings.teleport.ySelection.custom", "Custom ySelection cannot be null");
        } else {
            Set<String> modes = Set.of("mixed", "highest_only", "random_only");
            String mode = cu.mode == null ? "" : cu.mode.toLowerCase();
            if (!modes.contains(mode)) {
                result.addError("settings.teleport.ySelection.custom.mode",
                        "Invalid mode. Valid: mixed, highest_only, random_only");
            }
            if ("mixed".equals(mode)) {
                Set<String> firsts = Set.of("highest", "random");
                String first = cu.first == null ? "" : cu.first.toLowerCase();
                if (!firsts.contains(first)) {
                    result.addError("settings.teleport.ySelection.custom.first",
                            "Invalid first group. Valid: highest, random");
                }
                double share = cu.firstShare;
                if (Double.isNaN(share) || Double.isInfinite(share) || share < 0.0 || share > 1.0) {
                    result.addError("settings.teleport.ySelection.custom.firstShare",
                            "firstShare must be within [0.0 .. 1.0]");
                }
            }
        }
    }

    private static void validateWaitingRoomSection(ValidationResult result, MainConfig.WaitingRoomSection waitingRoom) {
        Validators.min(result, "settings.waitingRoom.asyncSearchTimeout", waitingRoom.asyncSearchTimeout, 1, "Async search timeout must be at least 1 second");
        Validators.max(result, "settings.waitingRoom.asyncSearchTimeout", waitingRoom.asyncSearchTimeout, 60, "Async search timeout cannot exceed 60 seconds");

        Validators.min(result, "settings.waitingRoom.minStayTicks", waitingRoom.minStayTicks, 0, "minStayTicks must be >= 0");
        Validators.max(result, "settings.waitingRoom.minStayTicks", waitingRoom.minStayTicks, 600, "minStayTicks too high");

        // Validate waiting room location
        if (waitingRoom.location != null) {
            Validators.notBlank(result, "settings.waitingRoom.location.world", waitingRoom.location.world, "Waiting room world cannot be empty");
        }
    }

    private static void validatePartySection(ValidationResult result, MainConfig.PartySection party) {
        // Validate scope
        if (party.scope != null) {
            Set<String> validScopes = Set.of("death", "join", "both");
            if (!validScopes.contains(party.scope)) {
                result.addError("party.scope", "Invalid party scope: " + party.scope + ". Valid scopes: death, join, both");
            }
        }

        // Validate numeric values
        Validators.min(result, "party.maxSize", party.maxSize, 0, "Party max size cannot be negative");
        Validators.min(result, "party.maxRespawnDistance", party.maxRespawnDistance, 0, "Party max respawn distance cannot be negative");
        Validators.min(result, "party.respawnCooldown", party.respawnCooldown, 0, "Party respawn cooldown cannot be negative");
        Validators.min(result, "party.invitationExpiry", party.invitationExpiry, 1, "Party invitation expiry must be at least 1 second");

        // Validate death location spawn settings
        if (party.deathLocationSpawn != null) {
            Validators.notBlank(result, "party.deathLocationSpawn.permission", party.deathLocationSpawn.permission, "Death location spawn permission cannot be empty");

            if (party.deathLocationSpawn.restrictionBehavior != null) {
                Set<String> validBehaviors = Set.of("deny", "allow", "fallback_to_party", "fallback_to_normal_spawn");
                if (!validBehaviors.contains(party.deathLocationSpawn.restrictionBehavior.restrictedAreaBehavior)) {
                    result.addError("party.deathLocationSpawn.restrictionBehavior.restrictedAreaBehavior",
                            "Invalid restricted area behavior. Valid options: deny, allow, fallback_to_party, fallback_to_normal_spawn");
                }
            }
        }

        // Validate respawn behavior settings
        if (party.respawnBehavior != null) {
            validateRespawnBehavior(result, party.respawnBehavior);
        }
    }

    private static void validateRespawnBehavior(ValidationResult result, MainConfig.RespawnBehaviorSection behavior) {
        Set<String> validBehaviors = Set.of("allow", "deny", "fallback_to_normal_spawn");
        Set<String> validTargetBehaviors = Set.of("allow", "deny", "find_other_member");

        if (!validBehaviors.contains(behavior.deathRestrictedBehavior)) {
            result.addError("party.respawnBehavior.deathRestrictedBehavior",
                    "Invalid death restricted behavior. Valid options: allow, deny, fallback_to_normal_spawn");
        }

        if (!validTargetBehaviors.contains(behavior.targetRestrictedBehavior)) {
            result.addError("party.respawnBehavior.targetRestrictedBehavior",
                    "Invalid target restricted behavior. Valid options: allow, deny, find_other_member");
        }

        if (!validBehaviors.contains(behavior.bothRestrictedBehavior)) {
            result.addError("party.respawnBehavior.bothRestrictedBehavior",
                    "Invalid both restricted behavior. Valid options: allow, deny, fallback_to_normal_spawn");
        }

        Validators.min(result, "party.respawnBehavior.alternativeTargetAttempts", behavior.alternativeTargetAttempts, 1, "Alternative target attempts must be at least 1");
        Validators.max(result, "party.respawnBehavior.alternativeTargetAttempts", behavior.alternativeTargetAttempts, 10, "Alternative target attempts cannot exceed 10");

        // Validate target selection
        if (behavior.targetSelection != null) {
            validateTargetSelection(result, behavior.targetSelection);
        }
    }

    private static void validateTargetSelection(ValidationResult result, MainConfig.TargetSelectionSection targetSelection) {
        Set<String> validStrategies = Set.of("closest_same_world", "any_world", "most_members_world",
                "most_members_region", "random", "leader_priority", "specific_target_only");

        if (!validStrategies.contains(targetSelection.primaryStrategy)) {
            result.addError("party.respawnBehavior.targetSelection.primaryStrategy",
                    "Invalid primary strategy. Valid strategies: " + String.join(", ", validStrategies));
        }

        if (!validStrategies.contains(targetSelection.fallbackStrategy)) {
            result.addError("party.respawnBehavior.targetSelection.fallbackStrategy",
                    "Invalid fallback strategy. Valid strategies: " + String.join(", ", validStrategies));
        }

        Validators.min(result, "party.respawnBehavior.targetSelection.minPopulationThreshold", targetSelection.minPopulationThreshold, 1, "Min population threshold must be at least 1");
        Validators.min(result, "party.respawnBehavior.targetSelection.maxAlternativeAttempts", targetSelection.maxAlternativeAttempts, 1, "Max alternative attempts must be at least 1");
        Validators.max(result, "party.respawnBehavior.targetSelection.maxAlternativeAttempts", targetSelection.maxAlternativeAttempts, 10, "Max alternative attempts cannot exceed 10");
    }

    private static void validateJoinSection(ValidationResult result, MainConfig.JoinSection join) {
        if (join.waitForResourcePack) {
            Validators.min(result, "join.resourcePackTimeout", join.resourcePackTimeout, 5, "Resource pack timeout must be at least 5 seconds");
            Validators.max(result, "join.resourcePackTimeout", join.resourcePackTimeout, 300, "Resource pack timeout cannot exceed 5 minutes");
        }
    }
}