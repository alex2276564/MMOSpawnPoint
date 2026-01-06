package uz.alex2276564.mmospawnpoint.config.configs.mainconfig;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import uz.alex2276564.mmospawnpoint.config.configs.spawnpointsconfig.SpawnPointsConfig;

import java.util.List;

public class MainConfig extends OkaeriConfig {

    @Comment("# ================================================================")
    @Comment("# 🎯 Main Configuration")
    @Comment("# ================================================================")
    @Comment("# 📖 Documentation: https://github.com/alex2276564/MMOSpawnPoint")
    @Comment("# 💬 Support: https://github.com/alex2276564/MMOSpawnPoint/issues")
    @Comment("# ================================================================")
    @Comment("")
    @Comment("⚙️ Core system settings and behavior")
    public SettingsSection settings = new SettingsSection();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# 👥 PARTY SYSTEM")
    @Comment("# ================================================================")
    @Comment("# Allows players to create parties and respawn near party members")
    @Comment("# Scope controls when party system activates (death/join/both)")
    @Comment("# ================================================================")
    @Comment("")
    public PartySection party = new PartySection();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# 🚪 JOIN HANDLING")
    @Comment("# ================================================================")
    @Comment("# Controls player join behavior and resource pack integration")
    @Comment("# ================================================================")
    @Comment("")
    public JoinSection join = new JoinSection();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# 🔗 EXTERNAL INTEGRATIONS")
    @Comment("# ================================================================")
    @Comment("# Enable/disable hooks with other plugins")
    @Comment("# WorldGuard: Required for region-based spawns (kind: region)")
    @Comment("# PlaceholderAPI: Required for placeholder conditions/expressions")
    @Comment("# ================================================================")
    @Comment("")
    public HooksSection hooks = new HooksSection();

    // ================================================================
    // HOOKS SECTION
    // ================================================================

    public static class HooksSection extends OkaeriConfig {
        @Comment("🏛️ WorldGuard Integration")
        @Comment("Enable this to use region-based spawns (kind: region)")
        @Comment("⚠️ IMPORTANT: Required for region-based spawns - plugin will fail validation without it")
        @Comment("Requires WorldGuard plugin to be installed")
        public boolean useWorldGuard = true;

        @Comment("")
        @Comment("🏷️ PlaceholderAPI Integration")
        @Comment("Enable this to use placeholders in conditions and expressions")
        @Comment("Examples: %player_level% > 10, %vault_eco_balance% >= 1000")
        @Comment("⚠️ IMPORTANT: Required for placeholder conditions - plugin will fail validation without it")
        @Comment("Requires PlaceholderAPI plugin to be installed")
        public boolean usePlaceholderAPI = true;
    }

    // ================================================================
    // MAIN SETTINGS SECTION
    // ================================================================

    public static class SettingsSection extends OkaeriConfig {

        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🎯 SPAWN PRIORITIES")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Higher number = higher priority (0-9999)")
        @Comment("# When multiple spawn rules match, highest priority wins")
        @Comment("# coordinate: exact area spawns, region: WorldGuard regions, world: entire worlds")
        @Comment("# ----------------------------------------------------------------")
        public DefaultPrioritiesSection defaultPriorities = new DefaultPrioritiesSection();

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🔍 SAFE LOCATION SEARCH")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# These settings control how the plugin finds safe spawn locations")
        @Comment("# when requireSafe: true is used in spawn configurations")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        @Comment("🔄 Maximum attempts to find safe location for random spawns")
        @Comment("Higher values = more thorough search but potentially slower")
        @Comment("Recommended: 40-100 for normal setups, 20-40 for performance-critical servers")
        public int maxSafeLocationAttempts = 40;

        @Comment("")
        @Comment("📏 Base radius to search for safe locations (blocks)")
        @Comment("Used when searching around a fixed point (x/z have 'value')")
        @Comment("Radius expands automatically if no safe location found initially")
        public int safeLocationRadius = 6;

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 💾 SAFE LOCATION CACHING")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Caching dramatically improves performance for repeated safe searches")
        @Comment("# Different cache profiles for different spawn types")
        @Comment("# ----------------------------------------------------------------")
        public SafeLocationCacheSection safeLocationCache = new SafeLocationCacheSection();

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🌍 TELEPORTATION SYSTEM")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Core teleport behavior and Y-level selection for overworld")
        @Comment("# ----------------------------------------------------------------")
        public TeleportSection teleport = new TeleportSection();

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# ⏳ WAITING ROOM SYSTEM")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Used when requireSafe: true - players wait here during safe search")
        @Comment("# PHASES: BEFORE → waiting room → WAITING_ROOM → final teleport → AFTER")
        @Comment("# ----------------------------------------------------------------")
        public WaitingRoomSection waitingRoom = new WaitingRoomSection();

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🐛 DEBUG & DEVELOPMENT")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        @Comment("🔍 Enable debug mode for detailed console logs")
        @Comment("Shows priority order, spawn selection process, cache operations")
        @Comment("⚠️ Can be verbose - disable on production servers")
        public boolean debugMode = false;

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🚫 GLOBAL BLOCK BLACKLISTS")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# These blocks are globally considered unsafe for spawning")
        @Comment("# Can be overridden per-destination with groundWhitelist")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        @Comment("🔥 Blocks disallowed as ground (block under player's feet)")
        @Comment("These blocks are considered unsafe to stand on")
        public List<String> globalGroundBlacklist = List.of(
                "LAVA", "FIRE", "CACTUS", "WATER", "AIR", "MAGMA_BLOCK",
                "CAMPFIRE", "SOUL_CAMPFIRE", "WITHER_ROSE", "SWEET_BERRY_BUSH"
        );

        @Comment("")
        @Comment("💧 Blocks disallowed for feet/head positions")
        @Comment("These blocks cannot be at the player's feet or head level")
        @Comment("Prevents spawning inside water, lava, etc.")
        public List<String> globalPassableBlacklist = List.of(
                "WATER", "KELP", "KELP_PLANT", "SEAGRASS", "TALL_SEAGRASS",
                "BUBBLE_COLUMN", "LAVA", "POWDER_SNOW"
        );

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# ⚡ PERFORMANCE TUNING")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Batch processing for safe location search across all players")
        @Comment("# ----------------------------------------------------------------")
        public SafeSearchBatchSection safeSearchBatch = new SafeSearchBatchSection();

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🔐 PERMISSIONS & BYPASSES")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Permission nodes for bypassing various restrictions")
        @Comment("# ----------------------------------------------------------------")
        public PermissionsSection permissions = new PermissionsSection();

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🧹 MAINTENANCE & CLEANUP")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Automatic cleanup and file system limits")
        @Comment("# ----------------------------------------------------------------")
        public MaintenanceSection maintenance = new MaintenanceSection();
    }

    // ================================================================
    // SUB-SECTIONS
    // ================================================================

    public static class DefaultPrioritiesSection extends OkaeriConfig {
        @Comment("🎯 Coordinate-based spawns (kind: coordinate)")
        @Comment("Exact area triggers with x/z/y ranges or rects")
        public int coordinate = 100;

        @Comment("")
        @Comment("🏛️ Region-based spawns (kind: region)")
        @Comment("WorldGuard region triggers - requires WorldGuard")
        public int region = 50;

        @Comment("")
        @Comment("🌍 World-based spawns (kind: world)")
        @Comment("Entire world triggers - lowest priority by default")
        public int world = 10;
    }

    public static class SafeSearchBatchSection extends OkaeriConfig {

        @Comment("⚡ Total safe location attempts across ALL players per server tick.")
        @Comment("Minecraft runs at 20 ticks per second (1 tick ≈ 50ms).")
        @Comment("Each attempt is one SafeLocationFinder probe (block checks, world queries, etc.).")
        @Comment("")
        @Comment("Higher values:")
        @Comment("  • Pros: safe locations are found faster, especially with many players.")
        @Comment("  • Cons: more CPU work inside each 50ms tick (risk of short lag spikes).")
        @Comment("")
        @Comment("Paper:")
        @Comment("  • MSP will try up to attemptsPerTick safe probes per tick,")
        @Comment("    but will also respect timeBudgetMillis as a soft time limit.")
        @Comment("Folia:")
        @Comment("  • Each SafeSearchJob does exactly one attempt per tick on its region thread;")
        @Comment("    attemptsPerTick is mostly a global upper bound and rarely needs tuning.")
        public int attemptsPerTick = 200;

        @Comment("")
        @Comment("⏱️ Maximum time budget per tick for safe search (milliseconds, Paper only).")
        @Comment("This is a soft cap: MSP stops doing safe-location attempts in the current tick")
        @Comment("once the total time spent exceeds this value.")
        @Comment("")
        @Comment("Example:")
        @Comment("  • One server tick is 50ms.")
        @Comment("  • timeBudgetMillis = 2 → MSP may use up to ~2ms per tick for safe search,")
        @Comment("    leaving ~48ms for the rest of the server logic.")
        @Comment("")
        @Comment("Folia:")
        @Comment("  • This setting is effectively ignored; each region job is naturally 1 attempt per tick.")
        public int timeBudgetMillis = 2;
    }

    public static class SafeLocationCacheSection extends OkaeriConfig {
        @Comment("💾 Enable caching system for massive performance improvement")
        @Comment("Caches successful safe location searches to avoid repeated calculations.")
        @Comment("Global master switch:")
        @Comment("- If 'enabled=false' here, ALL caching is disabled (per-destination overrides cannot re-enable it).")
        @Comment("Use cases:")
        @Comment("- Keep it enabled in production. Disable only for debugging or highly dynamic maps.")
        public boolean enabled = true;

        @Comment("")
        @Comment("⏰ Cache expiry time in seconds")
        @Comment("How long cached locations remain valid.")
        @Comment("Lower = more accurate but more searches; Higher = better performance but less uniqueness.")
        public int expiryTime = 300;

        @Comment("")
        @Comment("📊 Maximum cached locations before cleanup")
        @Comment("Prevents memory growth on busy servers.")
        @Comment("Higher = better hit rate but more memory usage.")
        public int maxCacheSize = 1000;

        @Comment("")
        @Comment("# Per-spawn-type caching profiles")
        @Comment("# Each type has different access patterns and performance needs.")
        @Comment("# Notes on overrides:")
        @Comment("# - spawnTypeCaching.* controls defaults per type (pointSafe/areaSafeSingle/areaSafeMultiple).")
        @Comment("# - destinations[].cache (per-destination) can override 'enabled' and 'playerSpecific' for that destination only.")
        @Comment("# - However, the global 'safeLocationCache.enabled=false' still disables ALL caching (per-destination cannot force-enable it).")
        public SpawnTypeCachingSection spawnTypeCaching = new SpawnTypeCachingSection();

        @Comment("")
        @Comment("ℹ️ TECHNICAL NOTE:")
        @Comment("Cache keys include the effective Y-selection signature and groundWhitelist hash.")
        @Comment("This prevents incorrect reuse between different spawn configurations.")
        public AdvancedCacheSection advanced = new AdvancedCacheSection();
    }

    public static class SpawnTypeCachingSection extends OkaeriConfig {
        @Comment("🎯 Fixed Point Safe Search")
        @Comment("Used when destination has exact coordinates (x: value, z: value) AND requireSafe=true.")
        @Comment("Search pattern: around that specific point (near-search).")
        @Comment("Hit rate: high (same coordinates → same cache result).")
        public CacheTypeSection pointSafe = new CacheTypeSection(true, false);

        @Comment("")
        @Comment("📦 Area Safe Search - Single Destination")
        @Comment("Used when requireSafe=true with x/z ranges (or rects) and only one destination option.")
        @Comment("Hit rate: medium (same area + same player → same result more often).")
        public CacheTypeSection areaSafeSingle = new CacheTypeSection(true, true);

        @Comment("")
        @Comment("🎲 Area Safe Search - Multiple Destinations")
        @Comment("Used when requireSafe=true with multiple destination options (weighted/random).")
        @Comment("Hit rate: lower (randomness), but cache still provides benefit.")
        public CacheTypeSection areaSafeMultiple = new CacheTypeSection(true, true);
    }

    public static class CacheTypeSection extends OkaeriConfig {
        @Comment("Enable caching for this spawn type (default behavior).")
        @Comment("Note:")
        @Comment("- destinations[].cache.enabled can override this per-destination.")
        public boolean enabled;

        @Comment("Use player-specific cache entries.")
        @Comment("true  = each player gets different cached locations (more realistic).")
        @Comment("false = all players may share cached locations (better hit rate/performance).")
        @Comment("Note:")
        @Comment("- destinations[].cache.playerSpecific can override this per-destination.")
        public boolean playerSpecific;

        public CacheTypeSection() {}

        public CacheTypeSection(boolean enabled, boolean playerSpecific) {
            this.enabled = enabled;
            this.playerSpecific = playerSpecific;
        }
    }

    public static class AdvancedCacheSection extends OkaeriConfig {
        @Comment("🌍 Clear ENTIRE cache when ANY player changes world")
        @Comment("⚠️ Very aggressive; use only for highly dynamic worlds or special events.")
        @Comment("Affects performance significantly.")
        public boolean clearOnWorldChange = false;

        @Comment("")
        @Comment("👤 Clear a specific player's cache when they change worlds")
        @Comment("Balanced approach; removes potentially stale player-specific entries.")
        public boolean clearPlayerCacheOnWorldChange = true;

        @Comment("")
        @Comment("🚪 Clear player's cache when they quit the server")
        @Comment("Saves memory at the cost of losing cache benefit if they rejoin soon.")
        public boolean clearPlayerCacheOnQuit = false;

        @Comment("")
        @Comment("🐛 Debug cache operations in console")
        @Comment("Shows cache hits/misses and performance metrics.")
        public boolean debugCache = false;
    }

    public static class TeleportSection extends OkaeriConfig {
        @Comment("⏱️ Delay before teleport in ticks (20 ticks = 1 second)")
        @Comment("0 = instant, 20+ = noticeable delay")
        @Comment("Note:")
        @Comment("- This is a global visual/UX delay before teleport executes.")
        @Comment("- Death (useSetRespawnLocationForDeath=false) uses post-respawn flow;")
        @Comment("  SpawnManager still respects delayTicks for the final teleport.")
        public int delayTicks = 0;

        @Comment("")
        @Comment("💀 Use setRespawnLocation for death events (recommended)")
        @Comment("true  = set the respawn location during the respawn event (smooth, no flicker)")
        @Comment("false = perform teleport after vanilla respawn (may show a brief vanilla spawn glimpse)")
        @Comment("Tip:")
        @Comment("- If you integrate with complex respawn logic from other plugins,")
        @Comment("  testing both modes is recommended.")
        public boolean useSetRespawnLocationForDeath = true;

        @Comment("")
        @Comment("🚪 Use PlayerSpawnLocationEvent for join events (where available)")
        @Comment("true  = set the spawn location before the player appears in the world (no flicker)")
        @Comment("false = use PlayerJoinEvent + post-join teleport (may show 1-tick vanilla spawn)")
        @Comment("Tip:")
        @Comment("- If you integrate with complex respawn logic from other plugins,")
        @Comment("  testing both modes is recommended.")
        @Comment("Notes:")
        @Comment("- This setting is ignored if join.waitForResourcePack is true; in that case")
        @Comment("  MSP always uses the post-join teleport flow so that the resource pack")
        @Comment("  waiting-room logic can move the player first.")
        @Comment("- On Minecraft 1.21.9+ PlayerSpawnLocationEvent is deprecated/unstable.")
        @Comment("- MMOSpawnPoint will automatically fall back to PlayerJoinEvent-based join flow")
        @Comment("  on these versions, even if this option is set to true.")
        public boolean useSetSpawnLocationForJoin = true;

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 📏 DIMENSION-AWARE Y-LEVEL SELECTION")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Global Y-selection policy per dimension. Can be overridden per destination (spawnpoints).")
        @Comment("# Overworld/End/Custom support: mixed | highest_only | random_only")
        @Comment("# Nether supports: scan | highest_only | random_only")
        @Comment("#   - scan: deterministic column scan for 'solid + 2 air' (recommended for generic Nether)")
        @Comment("#   - respectRange (Nether): if true, scan will be limited to the destination's Y-range (rects)")
        @Comment("# Notes:")
        @Comment("# - Per-destination override is designed for hand-crafted/vertical content.")
        @Comment("# - Global defaults keep behavior predictable for generic worlds.")
        public TeleportYSelection ySelection = new TeleportYSelection();
    }

    public static class TeleportYSelection extends OkaeriConfig {
        @Comment("")
        @Comment("Overworld policy (applies when world.environment == NORMAL; can be overridden per destination)")
        public OverworldSection overworld = new OverworldSection();

        @Comment("")
        @Comment("Nether policy (applies when world.environment == NETHER; can be overridden per destination)")
        public NetherSection nether = new NetherSection();

        @Comment("")
        @Comment("End policy (applies when world.environment == THE_END; can be overridden per destination)")
        public EndSection end = new EndSection();

        @Comment("")
        @Comment("Custom-dimension policy (applies when world.environment == CUSTOM; can be overridden per destination)")
        @Comment("By default behaves like overworld, but can be tuned separately for datapack/custom dimensions.")
        public CustomSection custom = new CustomSection();

        public static class OverworldSection extends OkaeriConfig {
            @Comment("Y-selection mode in Overworld:")
            @Comment("- mixed: hybrid strategy combining 'highest' and 'random'")
            @Comment("- highest_only: always use highest block (surface-first behavior)")
            @Comment("- random_only: uniform random Y within allowed bounds")
            @Comment("Recommendation:")
            @Comment("- mixed is the best default for most servers.")
            public String mode = "mixed";

            @Comment("For mixed:")
            @Comment("- highest: try 'highest' first, then 'random'")
            @Comment("- random: try 'random' first, then 'highest'")
            public String first = "highest";

            @Comment("For mixed:")
            @Comment("- firstShare ∈ [0..1], share of attempts allocated to the FIRST strategy")
            @Comment("- Example: first=highest, firstShare=0.6 → 60% highest, 40% random")
            public double firstShare = 0.6;
        }

        public static class EndSection extends OkaeriConfig {
            @Comment("Y-selection mode in the End:")
            @Comment("- mixed | highest_only | random_only")
            @Comment("Recommendation:")
            @Comment("- highest_only or mixed(first=highest) for a surface-focused behavior on islands.")
            public String mode = "highest_only";

            @Comment("For mixed only: highest | random")
            public String first = "highest";

            @Comment("For mixed only: [0..1] share for the FIRST strategy")
            public double firstShare = 0.6;
        }

        public static class NetherSection extends OkaeriConfig {
            @Comment("Y-selection mode in the Nether:")
            @Comment("- scan: deterministic 'solid + 2 air' column scan (recommended default)")
            @Comment("- highest_only: use highest block (may put you on the roof without whitelist)")
            @Comment("- random_only: uniform random Y (use with explicit Y ranges and whitelists)")
            @Comment("Tip:")
            @Comment("- Prefer scan for typical usage; switch to random_only per destination for hand-crafted vertical content.")
            public String mode = "scan";

            @Comment("If true, the Nether 'scan' mode will be limited to the Y-range of the destination (if provided).")
            @Comment("This is useful for vertical dungeons or constrained regions.")
            public boolean respectRange = false;
        }

        public static class CustomSection extends OkaeriConfig {
            @Comment("Y-selection mode in CUSTOM dimensions (world.environment == CUSTOM):")
            @Comment("- mixed: hybrid strategy combining 'highest' and 'random'")
            @Comment("- highest_only: always use highest block")
            @Comment("- random_only: uniform random Y within allowed bounds")
            @Comment("Recommendation:")
            @Comment("- Start with mixed (like overworld), then tune per-dimension if needed.")
            public String mode = "mixed";

            @Comment("For mixed:")
            @Comment("- highest: try 'highest' first, then 'random'")
            @Comment("- random: try 'random' first, then 'highest'")
            public String first = "highest";

            @Comment("For mixed:")
            @Comment("- firstShare ∈ [0..1], share of attempts allocated to the FIRST strategy")
            public double firstShare = 0.6;
        }
    }

    public static class WaitingRoomSection extends OkaeriConfig {
        @Comment("✅ Enable waiting room system")
        @Comment("Required for requireSafe: true spawns - players wait here during search")
        @Comment("Disable only if you never use requireSafe: true anywhere")
        public boolean enabled = true;

        @Comment("")
        @Comment("⏰ Timeout for safe location search (seconds)")
        @Comment("If no safe location found within this time, search gives up")
        @Comment("Player remains in waiting room - consider manual rescue commands")
        public int asyncSearchTimeout = 10;

        @Comment("")
        @Comment("⏳ Minimum time in waiting room before final teleport (ticks)")
        @Comment("Prevents instant teleport if safe location found immediately")
        @Comment("Gives time for WAITING_ROOM phase messages/commands to display")
        @Comment("💡 Set to 40-60 ticks (2-3 seconds) for good user experience")
        public int minStayTicks = 40;

        @Comment("")
        @Comment("🏠 Global waiting room coordinates")
        @Comment("Used when no custom waiting room specified in spawn configuration")
        @Comment("Should be a safe, enclosed area with no fall damage risk")
        public SpawnPointsConfig.WaitingRoomConfig location = new SpawnPointsConfig.WaitingRoomConfig();
    }

    public static class PermissionsSection extends OkaeriConfig {
        @Comment("🔓 Permission nodes for bypassing various restrictions")
        public BypassSection bypass = new BypassSection();

        public static class BypassSection extends OkaeriConfig {
            @Comment("👥 Party system bypasses")
            public PartyBypassSection party = new PartyBypassSection();

            public static class PartyBypassSection extends OkaeriConfig {
                @Comment("⏰ Bypass party respawn cooldown")
                @Comment("Players with this permission ignore respawn cooldown timers")
                public boolean cooldownEnabled = true;
                public String cooldownNode = "mmospawnpoint.bypass.party.cooldown";

                @Comment("")
                @Comment("🚫 Bypass party restrictions (areas with partyRespawnDisabled: true)")
                public RestrictionsSection restrictions = new RestrictionsSection();

                @Comment("")
                @Comment("🛡️ Bypass walking spawn point restrictions")
                public WalkingSection walking = new WalkingSection();

                public static class RestrictionsSection extends OkaeriConfig {
                    @Comment("Bypass restrictions at death location")
                    public boolean deathEnabled = false;
                    public String deathNode = "mmospawnpoint.bypass.party.restrictions.death";

                    @Comment("")
                    @Comment("Bypass restrictions at target player location")
                    public boolean targetEnabled = false;
                    public String targetNode = "mmospawnpoint.bypass.party.restrictions.target";

                    @Comment("")
                    @Comment("Bypass restrictions when both death and target are restricted")
                    public boolean bothEnabled = false;
                    public String bothNode = "mmospawnpoint.bypass.party.restrictions.both";
                }

                public static class WalkingSection extends OkaeriConfig {
                    @Comment("Bypass walking spawn point area restrictions")
                    @Comment("Allows respawning at death location even in restricted areas")
                    public boolean restrictionsEnabled = false;
                    public String restrictionsNode = "mmospawnpoint.bypass.party.walking.restrictions";
                }
            }
        }
    }

    // ================================================================
    // PARTY SYSTEM
    // ================================================================

    public static class PartySection extends OkaeriConfig {
        @Comment("🎭 Enable entire party system")
        @Comment("Set to false to completely disable party features")
        public boolean enabled = true;

        @Comment("")
        @Comment("🎯 Party system activation scope")
        @Comment("• death: only activate on player death")
        @Comment("• join: only activate on player join")
        @Comment("• both: activate on both death and join events")
        @Comment("💡 Most servers use 'death' for respawn-focused party system")
        public String scope = "death";

        @Comment("")
        @Comment("👥 Maximum players per party (0 = unlimited)")
        @Comment("Large parties can impact performance with complex target selection")
        public int maxSize = 10;

        @Comment("")
        @Comment("📏 Maximum respawn distance in blocks (0 = unlimited)")
        @Comment("Prevents teleporting across entire world for party respawn")
        @Comment("💡 Recommended: 1000-5000 for balanced gameplay")
        public int maxRespawnDistance = 0;

        @Comment("")
        @Comment("⏱️ Cooldown between party respawns per player (seconds, 0 = none)")
        @Comment("Prevents spam respawning at party members")
        @Comment("💡 Recommended: 30-120 seconds for PvP servers, 0 for PvE")
        public int respawnCooldown = 0;

        @Comment("")
        @Comment("📨 Invitation expiry time (seconds)")
        @Comment("How long party invitations remain valid")
        public int invitationExpiry = 120;

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🚶 WALKING SPAWN POINT")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Special feature: respawn at exact death location (if allowed)")
        @Comment("# Takes priority over party member locations when enabled")
        @Comment("# ----------------------------------------------------------------")
        public DeathLocationSpawnSection deathLocationSpawn = new DeathLocationSpawnSection();

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🎯 PARTY RESPAWN BEHAVIOR")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Fine control over how party respawning works with restrictions")
        @Comment("# and target selection algorithms")
        @Comment("# ----------------------------------------------------------------")
        public RespawnBehaviorSection respawnBehavior = new RespawnBehaviorSection();
    }

    public static class DeathLocationSpawnSection extends OkaeriConfig {
        @Comment("🚶 Enable walking spawn point feature")
        @Comment("Allows players to respawn at their exact death location")
        public boolean enabled = true;

        @Comment("")
        @Comment("🔐 Permission required to use walking spawn point")
        @Comment("Players need this permission to respawn at death location")
        public String permission = "mmospawnpoint.party.deathLocationSpawn";

        @Comment("")
        @Comment("🚫 How walking spawn point handles restricted areas")
        public RestrictionBehaviorSection restrictionBehavior = new RestrictionBehaviorSection();
    }

    public static class RestrictionBehaviorSection extends OkaeriConfig {
        @Comment("🔍 Whether walking spawn point respects partyRespawnDisabled areas")
        @Comment("true = check restrictions, false = always allow death location respawn")
        public boolean respectRestrictions = true;

        @Comment("")
        @Comment("⚖️ What to do when trying to use walking spawn in restricted area:")
        @Comment("• deny: block the walking spawn, show restriction message")
        @Comment("• allow: ignore restrictions, always allow death location")
        @Comment("• fallback_to_party: use party member location instead")
        @Comment("• fallback_to_normal_spawn: use normal spawn system")
        public String restrictedAreaBehavior = "deny";

        @Comment("")
        @Comment("📍 Which locations to check for restrictions")
        @Comment("Death location = where player died")
        @Comment("Target location = where party member is (if checkTargetLocation enabled)")
        public boolean checkDeathLocation = true;
        public boolean checkTargetLocation = false;
    }

    public static class RespawnBehaviorSection extends OkaeriConfig {
        @Comment("📍 Location restriction checking")
        @Comment("Checks locations against spawn areas with partyRespawnDisabled: true")
        public boolean checkDeathLocation = true;
        public boolean checkTargetLocation = true;

        @Comment("")
        @Comment("⚖️ Behavior when ONLY death location is restricted:")
        @Comment("• allow: ignore restriction, proceed with party respawn")
        @Comment("• deny: block party respawn, show restriction message")
        @Comment("• fallback_to_normal_spawn: use normal spawn system instead")
        public String deathRestrictedBehavior = "deny";

        @Comment("")
        @Comment("⚖️ Behavior when ONLY target location is restricted:")
        @Comment("• allow: ignore restriction, respawn at target anyway")
        @Comment("• deny: block party respawn, show restriction message")
        @Comment("• find_other_member: try to find unrestricted party member")
        public String targetRestrictedBehavior = "deny";

        @Comment("")
        @Comment("⚖️ Behavior when BOTH locations are restricted:")
        @Comment("• allow: ignore all restrictions")
        @Comment("• deny: block party respawn")
        @Comment("• fallback_to_normal_spawn: use normal spawn system")
        public String bothRestrictedBehavior = "deny";

        @Comment("")
        @Comment("🔍 Try to find alternative party members if target is restricted")
        @Comment("When true, searches for other party members in unrestricted areas")
        public boolean findAlternativeTarget = true;

        @Comment("")
        @Comment("🔢 Maximum attempts to find alternative party member")
        @Comment("Higher = better chance to find valid target, but more processing")
        public int alternativeTargetAttempts = 3;

        @Comment("")
        @Comment("# Target selection AI - how to choose which party member to respawn at")
        public TargetSelectionSection targetSelection = new TargetSelectionSection();
    }

    public static class TargetSelectionSection extends OkaeriConfig {
        @Comment("🎯 Primary target selection strategy:")
        @Comment("• closest_same_world: nearest party member in same world")
        @Comment("• any_world: nearest party member anywhere")
        @Comment("• most_members_world: world with most party members")
        @Comment("• most_members_region: region with most party members")
        @Comment("• random: random party member")
        @Comment("• leader_priority: prefer party leader")
        @Comment("• specific_target_only: only use manually set target")
        public String primaryStrategy = "closest_same_world";

        @Comment("")
        @Comment("🔄 Fallback strategy if primary fails")
        @Comment("Same options as primaryStrategy")
        public String fallbackStrategy = "any_world";

        @Comment("")
        @Comment("🌍 Prefer players in same world over cross-world teleports")
        public boolean preferSameWorld = true;

        @Comment("")
        @Comment("👑 Prefer party leader over regular members")
        @Comment("Useful for leader-focused party gameplay")
        public boolean preferLeader = false;

        @Comment("")
        @Comment("📊 Consider world population when selecting target")
        @Comment("Favors worlds with more party members")
        public boolean considerWorldPopulation = false;

        @Comment("")
        @Comment("🏛️ Consider region population when selecting target")
        @Comment("Favors regions with more party members (requires WorldGuard)")
        public boolean considerRegionPopulation = false;

        @Comment("")
        @Comment("👥 Minimum members required in world/region to prefer it")
        @Comment("Prevents selecting empty areas just because they have 1 member")
        public int minPopulationThreshold = 2;

        @Comment("")
        @Comment("🔄 Maximum attempts to find alternative target")
        @Comment("Used when primary target selection fails")
        public int maxAlternativeAttempts = 3;
    }

    // ================================================================
    // JOIN SYSTEM
    // ================================================================

    public static class JoinSection extends OkaeriConfig {
        @Comment("📦 Wait for resource pack to load before processing join spawns")
        @Comment("⚠️ PERFORMANCE NOTE: Best practice is to pre-load resource packs in hub/lobby")
        @Comment("This feature adds complexity - consider hub-based RP loading instead")
        @Comment("")
        @Comment("❗ Unlike ItemsAdder, this plugin does not freeze players or protect them during RP loading,")
        @Comment("allowing potential abuse (e.g., delaying teleportation time/point). Therefore, it's better to")
        @Comment("disable this and load RP in the hub, or use useWaitingRoomForResourcePack with a delay in")
        @Comment("ItemsAdder-like plugins before protection and sending RP to the player to avoid conflicts")
        public boolean waitForResourcePack = false;

        @Comment("")
        @Comment("⏰ Resource pack loading timeout (seconds)")
        @Comment("Player gets teleported after this time regardless of RP status")
        @Comment("💡 Balance between user experience and preventing stuck players")
        public int resourcePackTimeout = 120;

        @Comment("")
        @Comment("🏠 Move player to waiting room during resource pack download")
        @Comment("Prevents players from wandering around during RP download")
        @Comment("Only works if waitingRoom.enabled = true")
        @Comment("")
        @Comment("⚠️ If enabled, it is recommended to disable join and RP loading protections from")
        @Comment("ItemsAdder and similar plugins, or set a delay on them to prevent conflicts")
        public boolean useWaitingRoomForResourcePack = false;
    }

    // ================================================================
    // MAINTENANCE
    // ================================================================

    public static class MaintenanceSection extends OkaeriConfig {
        @Comment("📁 Maximum folder depth when scanning spawnpoints/ directory")
        @Comment("Prevents infinite recursion and excessive nesting")
        @Comment("💡 Recommended: 5-9 levels for organized spawn configs")
        public int maxFolderDepth = 9;

        @Comment("")
        @Comment("🧹 Party cleanup frequency (ticks)")
        @Comment("How often to remove empty parties and rebuild member maps")
        @Comment("1200 ticks = 60 seconds")
        public int partyCleanupPeriodTicks = 1200;

        @Comment("")
        @Comment("📨 Invitation cleanup frequency (ticks)")
        @Comment("How often to remove expired party invitations")
        @Comment("1200 ticks = 60 seconds")
        public int invitationCleanupPeriodTicks = 1200;
    }
}