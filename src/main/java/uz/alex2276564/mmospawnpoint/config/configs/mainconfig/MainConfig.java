package uz.alex2276564.mmospawnpoint.config.configs.mainconfig;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.RegionSpawnsConfig;

import java.util.List;

public class MainConfig extends OkaeriConfig {

    @Comment("# ================================================================")
    @Comment("# MMOSpawnPoint Configuration")
    @Comment("# ================================================================")
    @Comment("")
    @Comment("General settings")
    public SettingsSection settings = new SettingsSection();

    @Comment("")
    @Comment("Party system settings")
    public PartySection party = new PartySection();

    @Comment("")
    @Comment("Join handling settings")
    public JoinsSection joins = new JoinsSection();

    @Comment("")
    @Comment("External hooks (enable/disable integrations)")
    public HooksSection hooks = new HooksSection();

    public static class HooksSection extends OkaeriConfig {
        @Comment("Use WorldGuard integration if installed")
        public boolean useWorldGuard = true;

        @Comment("Use PlaceholderAPI integration if installed")
        public boolean usePlaceholderAPI = true;
    }

    public static class SettingsSection extends OkaeriConfig {
        @Comment("Default priorities for spawn types (higher number = higher priority)")
        @Comment("coordinate: exact area spawns, region: WorldGuard regions, world: entire worlds")
        public DefaultPrioritiesSection defaultPriorities = new DefaultPrioritiesSection();

        @Comment("")
        @Comment("Maximum attempts to find safe location for random spawns")
        public int maxSafeLocationAttempts = 20;

        @Comment("")
        @Comment("Base radius to search for safe locations (used by SafeLocationFinder)")
        public int safeLocationRadius = 5;

        @Comment("")
        @Comment("Safe location caching system")
        public SafeLocationCacheSection safeLocationCache = new SafeLocationCacheSection();

        @Comment("")
        @Comment("Teleport settings")
        public TeleportSection teleport = new TeleportSection();

        @Comment("")
        @Comment("Waiting room settings")
        public WaitingRoomSection waitingRoom = new WaitingRoomSection();

        @Comment("")
        @Comment("Enable debug mode for detailed logs")
        public boolean debugMode = false;

        @Comment("")
        @Comment("List of materials considered unsafe (will avoid spawning on these)")
        public List<String> unsafeMaterials = List.of(
                "LAVA", "FIRE", "CACTUS", "WATER", "AIR", "MAGMA_BLOCK",
                "CAMPFIRE", "SOUL_CAMPFIRE", "WITHER_ROSE", "SWEET_BERRY_BUSH"
        );

        @Comment("")
        @Comment("Passable materials to avoid standing inside (feet/head), e.g. water, lava, etc.")
        public List<String> bannedPassableMaterials = List.of(
                "WATER", "KELP", "KELP_PLANT", "SEAGRASS", "TALL_SEAGRASS", "BUBBLE_COLUMN", "LAVA", "POWDER_SNOW"
        );

        @Comment("")
        @Comment("Overworld Y selection strategy for region safe search:")
        @Comment(" - MIXED: use a split between 'highest block Y' and 'random Y' by ratio (see highestBlockYAttemptRatio).")
        @Comment(" Implementation detail: first N attempts (ratio * attempts) try 'highest block Y', then the rest use 'random Y'.")
        @Comment(" Recommended for most servers: keeps players on ground most of the time, but still allows dungeon-like vertical spawns.")
        @Comment(" - HIGHEST_FIRST: all attempts use 'highest block Y' (identical to HIGHEST_ONLY in current implementation).")
        @Comment(" Good for natural terrain worlds; may be unsuitable for vertical dungeons with multiple floors.")
        @Comment(" - HIGHEST_ONLY: strictly use 'highest block Y' for all attempts (same behavior as HIGHEST_FIRST).")
        @Comment(" Use when you want to ensure ground-level spawns only.")
        public String overworldYStrategy = "MIXED";

        @Comment("")
        @Comment("For MIXED strategy: the ratio of attempts using highest-block Y [0.0 .. 1.0].")
        @Comment("Example: 0.6 -> 60% of attempts use 'highest block Y' (first), 40% use 'random Y' (after).")
        @Comment("Tip: 0.5-0.8 works well for most overworld regions; reduce if you have vertical dungeons.")
        public double highestBlockYAttemptRatio = 0.6;

        @Comment("")
        @Comment("Maintenance and scheduler parameters")
        public MaintenanceSection maintenance = new MaintenanceSection();
    }

    public static class DefaultPrioritiesSection extends OkaeriConfig {
        @Comment("Coordinate-based spawns")
        public int coordinate = 100;

        @Comment("Region-based spawns")
        public int region = 50;

        @Comment("World-based spawns")
        public int world = 10;
    }

    public static class SafeLocationCacheSection extends OkaeriConfig {
        @Comment("Enable caching system for performance")
        public boolean enabled = true;

        @Comment("Cache expiry time in seconds")
        public int expiryTime = 300;

        @Comment("Maximum number of cached locations before cleanup")
        public int maxCacheSize = 1000;

        @Comment("Caching behavior for different spawn types")
        public SpawnTypeCachingSection spawnTypeCaching = new SpawnTypeCachingSection();

        @Comment("Advanced cache options")
        public AdvancedCacheSection advanced = new AdvancedCacheSection();
    }

    public static class SpawnTypeCachingSection extends OkaeriConfig {
        @Comment("Fixed spawn points (exact coordinates)")
        public CacheTypeSection fixedSpawns = new CacheTypeSection(true, false);

        @Comment("Random spawn points (random coordinates in area)")
        public CacheTypeSection randomSpawns = new CacheTypeSection(true, true);

        @Comment("Weighted random spawn points (multiple locations with weights)")
        public CacheTypeSection weightedRandomSpawns = new CacheTypeSection(true, false);
    }

    public static class CacheTypeSection extends OkaeriConfig {
        public boolean enabled;

        @Comment("Whether to use player-specific cache (each player gets different cached location)")
        public boolean playerSpecific;

        public CacheTypeSection() {
        }

        public CacheTypeSection(boolean enabled, boolean playerSpecific) {
            this.enabled = enabled;
            this.playerSpecific = playerSpecific;
        }
    }

    public static class AdvancedCacheSection extends OkaeriConfig {
        @Comment("Clear entire cache when ANY player changes world.")
        @Comment("Use only for highly specialized setups (event worlds, worlds with frequently changing terrain/regions).")
        public boolean clearOnWorldChange = false;

        @Comment("Clear specific player's cache when they change worlds")
        public boolean clearPlayerCacheOnWorldChange = true;

        @Comment("Debug cache operations")
        public boolean debugCache = false;
    }

    public static class TeleportSection extends OkaeriConfig {
        @Comment("Delay before teleport in ticks (20 ticks = 1 second, 1 = almost instant)")
        public int delayTicks = 1;
    }

    public static class WaitingRoomSection extends OkaeriConfig {
        @Comment("Enable the waiting room system - recommended if you use requireSafe: true anywhere")
        public boolean enabled = true;

        @Comment("Timeout for async safe location search (seconds)")
        @Comment("If a safe location isn't found within this time, player stays in waiting room")
        public int asyncSearchTimeout = 5;

        @Comment("Global waiting room location (used if no custom waiting room is specified)")
        public RegionSpawnsConfig.WaitingRoomConfig location = new RegionSpawnsConfig.WaitingRoomConfig();
    }

    public static class PartySection extends OkaeriConfig {
        @Comment("Enable party system")
        public boolean enabled = true;

        @Comment("Party system scope: deaths, joins, both")
        @Comment("deaths - only work on player death")
        @Comment("joins - only work on player join")
        @Comment("both - work on both events")
        public String scope = "deaths";

        @Comment("Maximum number of players in a party (0 for unlimited)")
        public int maxSize = 10;

        @Comment("Maximum distance for party respawn in blocks (0 for unlimited)")
        public int maxRespawnDistance = 0;

        @Comment("Cooldown between party respawn for same player in seconds (0 for no cooldown)")
        public int respawnCooldown = 0;

        @Comment("Invitation expiry time in seconds")
        public int invitationExpiry = 60;

        @Comment("Walking Spawn Point feature")
        public RespawnAtDeathSection respawnAtDeath = new RespawnAtDeathSection();

        @Comment("Party respawn behavior control")
        public RespawnBehaviorSection respawnBehavior = new RespawnBehaviorSection();
    }

    public static class RespawnAtDeathSection extends OkaeriConfig {
        public boolean enabled = true;
        public String permission = "mmospawnpoint.party.respawnatdeath";

        @Comment("How walking spawn point interacts with restrictions")
        public RestrictionBehaviorSection restrictionBehavior = new RestrictionBehaviorSection();
    }

    public static class RestrictionBehaviorSection extends OkaeriConfig {
        @Comment("Whether walking spawn point should respect party-respawn-disabled areas")
        public boolean respectRestrictions = true;

        @Comment("What to do when walking spawn point player dies in restricted area")
        @Comment("Options: deny, allow, fallback_to_party, fallback_to_normal_spawn")
        public String restrictedAreaBehavior = "deny";

        @Comment("Whether to check both death location and target location restrictions")
        public boolean checkDeathLocation = true;
        public boolean checkTargetLocation = false;
    }

    public static class RespawnBehaviorSection extends OkaeriConfig {
        @Comment("Check dying player's location for restrictions")
        public boolean checkDeathLocation = true;

        @Comment("Check target player's location for restrictions")
        public boolean checkTargetLocation = true;

        @Comment("Behavior when only death location is restricted")
        @Comment("Options: allow, deny, fallback_to_normal_spawn")
        public String deathRestrictedBehavior = "deny";

        @Comment("Behavior when only target location is restricted")
        @Comment("Options: allow, deny, find_other_member")
        public String targetRestrictedBehavior = "deny";

        @Comment("Behavior when both locations are restricted")
        public String bothRestrictedBehavior = "deny";

        @Comment("Try to find alternative party members if target is in restricted area")
        public boolean findAlternativeTarget = true;

        @Comment("Maximum attempts to find alternative party member")
        public int alternativeTargetAttempts = 3;

        @Comment("Target selection strategy")
        public TargetSelectionSection targetSelection = new TargetSelectionSection();
    }

    public static class TargetSelectionSection extends OkaeriConfig {
        @Comment("Primary strategy: closest_same_world, closest_any_world, most_members_world,")
        @Comment("most_members_region, random, leader_priority, specific_target_only")
        public String primaryStrategy = "closest_same_world";

        @Comment("Fallback strategy if primary fails")
        public String fallbackStrategy = "closest_any_world";

        @Comment("Whether to prefer players in same world")
        public boolean preferSameWorld = true;

        @Comment("Whether to prefer party leader over other members")
        public boolean preferLeader = false;

        @Comment("Whether to consider world population when selecting target")
        public boolean considerWorldPopulation = false;

        @Comment("Whether to consider region population when selecting target")
        public boolean considerRegionPopulation = false;

        @Comment("Minimum members required in a world/region to prefer it")
        public int minPopulationThreshold = 2;

        @Comment("Maximum attempts to find alternative target")
        public int maxAlternativeAttempts = 3;
    }

    public static class JoinsSection extends OkaeriConfig {
        @Comment("Wait for resource pack to load before processing join spawns")
        @Comment("Performance note: best practice is to load RP in hub/lobby.")
        public boolean waitForResourcePack = false;

        @Comment("Resource pack timeout in seconds")
        @Comment("Player will be teleported after this time regardless of RP status")
        public int resourcePackTimeout = 60;

        @Comment("Use waiting room while waiting for resource pack")
        @Comment("If enabled, player will be moved to waiting room during RP download")
        public boolean useWaitingRoomForResourcePack = false;
    }

    public static class MaintenanceSection extends OkaeriConfig {
        @Comment("Max folder depth when scanning spawnpoints directory")
        public int maxFolderDepth = 9;

        @Comment("Party cleanup period in ticks")
        public int partyCleanupPeriodTicks = 1200;

        @Comment("Invitation cleanup period in ticks")
        public int invitationCleanupPeriodTicks = 1200;
    }
}