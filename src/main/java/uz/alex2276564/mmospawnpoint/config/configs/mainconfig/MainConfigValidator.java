package uz.alex2276564.mmospawnpoint.config.configs.mainconfig;

import lombok.experimental.UtilityClass;
import uz.alex2276564.mmospawnpoint.MMOSpawnPoint;
import uz.alex2276564.mmospawnpoint.config.utils.validation.ValidationResult;
import uz.alex2276564.mmospawnpoint.config.utils.validation.Validators;

import java.util.Set;

@UtilityClass
public class MainConfigValidator {

    public static void validate(MainConfig config) {
        ValidationResult result = new ValidationResult();

        validateSettingsSection(result, config.settings);
        validatePartySection(result, config.party);
        validateJoinsSection(result, config.joins);

        result.throwIfInvalid("Main configuration");
    }

    private static void validateSettingsSection(ValidationResult result, MainConfig.SettingsSection settings) {

        // Validate numeric values
        Validators.min(result, "settings.maxSafeLocationAttempts", settings.maxSafeLocationAttempts, 1, "Max safe location attempts must be at least 1");
        Validators.max(result, "settings.maxSafeLocationAttempts", settings.maxSafeLocationAttempts, 100, "Max safe location attempts cannot exceed 100");

        Validators.min(result, "settings.safeLocationRadius", settings.safeLocationRadius, 1, "Safe location radius must be at least 1");
        Validators.max(result, "settings.safeLocationRadius", settings.safeLocationRadius, 50, "Safe location radius cannot exceed 50");

        Validators.min(result, "settings.maxSafeLocationAttempts", settings.maxSafeLocationAttempts, 1, "Max safe location attempts must be at least 1");
        Validators.min(result, "settings.safeLocationRadius", settings.safeLocationRadius, 1, "Safe location radius must be at least 1");

        // Validate cache settings
        validateCacheSection(result, settings.safeLocationCache);

        // Warn unknown Global Passable Blacklist materials, but do not fail
        if (settings.globalPassableBlacklist != null) {
            for (String m : settings.globalPassableBlacklist) {
                if (org.bukkit.Material.matchMaterial(m) == null) {
                    MMOSpawnPoint.getInstance().getLogger().warning("Warning: Unknown material in globalPassableBlacklist: " + m);
                }
            }
        }

        validateMaintenanceSection(result, settings.maintenance);

        // Validate teleport settings
        validateTeleportSection(result, settings.teleport);

        // Validate waiting room settings
        validateWaitingRoomSection(result, settings.waitingRoom);

        // Validate Global Ground Blacklist (just warn about unknown materials)
        if (settings.globalGroundBlacklist != null) {
            for (String material : settings.globalGroundBlacklist) {
                try {
                    org.bukkit.Material.valueOf(material.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Just log warning, don't fail validation
                    MMOSpawnPoint.getInstance().getLogger().warning("Warning: Unknown material in globalGroundBlacklist list: " + material);
                }
            }
        }
    }

    private static void validateCacheSection(ValidationResult result, MainConfig.SafeLocationCacheSection cache) {
        Validators.min(result, "settings.safeLocationCache.expiryTime", cache.expiryTime, 1, "Cache expiry time must be at least 1 second");
        Validators.max(result, "settings.safeLocationCache.expiryTime", cache.expiryTime, 3600, "Cache expiry time cannot exceed 1 hour");

        Validators.min(result, "settings.safeLocationCache.maxCacheSize", cache.maxCacheSize, 10, "Max cache size must be at least 10");
        Validators.max(result, "settings.safeLocationCache.maxCacheSize", cache.maxCacheSize, 10000, "Max cache size cannot exceed 10000");

    }

    private static void validateMaintenanceSection(ValidationResult result, MainConfig.MaintenanceSection m) {
        Validators.min(result, "settings.maintenance.maxFolderDepth", m.maxFolderDepth, 1, "Max folder depth must be >= 1");
        Validators.min(result, "settings.maintenance.partyCleanupPeriodTicks", m.partyCleanupPeriodTicks, 20, "Party cleanup period must be >= 20 ticks");
        Validators.min(result, "settings.maintenance.invitationCleanupPeriodTicks", m.invitationCleanupPeriodTicks, 20, "Invitation cleanup period must be >= 20 ticks");
    }

    private static void validateTeleportSection(ValidationResult result, MainConfig.TeleportSection teleport) {
        // delayTicks
        Validators.min(result, "settings.teleport.delayTicks", teleport.delayTicks, 1, "Teleport delay must be at least 1 tick");
        Validators.max(result, "settings.teleport.delayTicks", teleport.delayTicks, 200, "Teleport delay cannot exceed 200 ticks (10 seconds)");

        // ySelection
        if (teleport.ySelection == null) {
            result.addError("settings.teleport.ySelection", "ySelection section cannot be null");
            return;
        }

        // mode: mixed | highest_only | random_only
        String mode = teleport.ySelection.mode == null ? "" : teleport.ySelection.mode.toLowerCase();
        java.util.Set<String> allowedModes = java.util.Set.of("mixed", "highest_only", "random_only");
        if (!allowedModes.contains(mode)) {
            result.addError("settings.teleport.ySelection.mode",
                    "Invalid mode. Valid: mixed, highest_only, random_only");
        }

        // first: highest | random (only relevant for mixed, but we always validate)
        String first = teleport.ySelection.first == null ? "" : teleport.ySelection.first.toLowerCase();
        java.util.Set<String> allowedFirst = java.util.Set.of("highest", "random");
        if (!allowedFirst.contains(first)) {
            result.addError("settings.teleport.ySelection.first",
                    "Invalid first group. Valid: highest, random");
        }
        // firstShare: [0..1]
        double share = teleport.ySelection.firstShare;
        if (Double.isNaN(share) || Double.isInfinite(share) || share < 0.0 || share > 1.0) {
            result.addError("settings.teleport.ySelection.firstShare",
                    "firstShare must be within [0.0 .. 1.0]");
        }
    }

    private static void validateWaitingRoomSection(ValidationResult result, MainConfig.WaitingRoomSection waitingRoom) {
        Validators.min(result, "settings.waitingRoom.asyncSearchTimeout", waitingRoom.asyncSearchTimeout, 1, "Async search timeout must be at least 1 second");
        Validators.max(result, "settings.waitingRoom.asyncSearchTimeout", waitingRoom.asyncSearchTimeout, 60, "Async search timeout cannot exceed 60 seconds");

        // Validate waiting room location
        if (waitingRoom.location != null) {
            Validators.notBlank(result, "settings.waitingRoom.location.world", waitingRoom.location.world, "Waiting room world cannot be empty");
        }
    }

    private static void validatePartySection(ValidationResult result, MainConfig.PartySection party) {
        // Validate scope
        if (party.scope != null) {
            Set<String> validScopes = Set.of("deaths", "joins", "both");
            if (!validScopes.contains(party.scope)) {
                result.addError("party.scope", "Invalid party scope: " + party.scope + ". Valid scopes: deaths, joins, both");
            }
        }

        // Validate numeric values
        Validators.min(result, "party.maxSize", party.maxSize, 0, "Party max size cannot be negative");
        Validators.min(result, "party.maxRespawnDistance", party.maxRespawnDistance, 0, "Party max respawn distance cannot be negative");
        Validators.min(result, "party.respawnCooldown", party.respawnCooldown, 0, "Party respawn cooldown cannot be negative");
        Validators.min(result, "party.invitationExpiry", party.invitationExpiry, 1, "Party invitation expiry must be at least 1 second");

        // Validate respawn at death settings
        if (party.respawnAtDeath != null) {
            Validators.notBlank(result, "party.respawnAtDeath.permission", party.respawnAtDeath.permission, "Respawn at death permission cannot be empty");

            if (party.respawnAtDeath.restrictionBehavior != null) {
                Set<String> validBehaviors = Set.of("deny", "allow", "fallback_to_party", "fallback_to_normal_spawn");
                if (!validBehaviors.contains(party.respawnAtDeath.restrictionBehavior.restrictedAreaBehavior)) {
                    result.addError("party.respawnAtDeath.restrictionBehavior.restrictedAreaBehavior",
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
        Set<String> validStrategies = Set.of("closest_same_world", "closest_any_world", "most_members_world",
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

    private static void validateJoinsSection(ValidationResult result, MainConfig.JoinsSection joins) {
        if (joins.waitForResourcePack) {
            Validators.min(result, "joins.resourcePackTimeout", joins.resourcePackTimeout, 5, "Resource pack timeout must be at least 5 seconds");
            Validators.max(result, "joins.resourcePackTimeout", joins.resourcePackTimeout, 300, "Resource pack timeout cannot exceed 5 minutes");
        }
    }
}