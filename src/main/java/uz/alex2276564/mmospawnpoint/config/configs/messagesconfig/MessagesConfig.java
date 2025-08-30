package uz.alex2276564.mmospawnpoint.config.configs.messagesconfig;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

public class MessagesConfig extends OkaeriConfig {

    @Comment("# ================================================================")
    @Comment("# 📝 MMOSpawnPoint Messages Configuration")
    @Comment("#")
    @Comment("# 💬 All messages support MiniMessage formatting!")
    @Comment("#     → Works best with Paper 1.18+")
    @Comment("#     → Older versions automatically fallback to legacy color formatting")
    @Comment("#     → Examples: <red>Error!</red>, <green>Success!</green>")
    @Comment("#     → Use gradients, hover effects, click events, etc.")
    @Comment("#     → Web editor: https://webui.advntr.dev/")
    @Comment("#")
    @Comment("# 🌍 LOCALIZATION NOTE:")
    @Comment("#     Direct localization is not supported in this plugin.")
    @Comment("#     If you need multi-language support, use Triton plugin:")
    @Comment("#     → https://www.spigotmc.org/resources/triton.30331/")
    @Comment("# ================================================================")
    @Comment("")
    @Comment("Command messages")
    public CommandsSection commands = new CommandsSection();

    @Comment("")
    @Comment("General messages")
    public GeneralSection general = new GeneralSection();

    @Comment("")
    @Comment("Party system messages")
    public PartySection party = new PartySection();

    @Comment("")
    @Comment("Join system messages")
    public JoinsSection joins = new JoinsSection();

    public static class CommandsSection extends OkaeriConfig {
        @Comment("Help command messages")
        public HelpSection help = new HelpSection();

        @Comment("")
        @Comment("Reload command messages")
        public ReloadSection reload = new ReloadSection();

        @Comment("")
        @Comment("Set spawnpoint command messages")
        public SetSpawnPointSection setspawnpoint = new SetSpawnPointSection();

        public static class HelpSection extends OkaeriConfig {
            @Comment("Main help command header")
            public String header = "<gold>=== MMOSpawnPoint Help ===";

            @Comment("Reload command help line")
            public String reloadLine = "<yellow>/msp reload <type> <gray>- Reload the plugin configuration";

            @Comment("Party command help line")
            public String partyLine = "<yellow>/msp party <gray>- Soul binding commands";

            @Comment("Simulation command help line")
            public String simulateLine = "<yellow>/msp simulate <gray>- Simulation tools (death/join/back)";

            @Comment("Cache command help line")
            public String cacheLine = "<yellow>/msp cache <gray>- Safe-location cache tools";

            @Comment("Set spawnpoint command help line")
            public String setspawnpointLine = "<yellow>/msp setspawnpoint <gray>- Set vanilla (bed) respawn point";

            @Comment("Help command help line")
            public String helpLine = "<yellow>/msp help <gray>- Show this help message";
        }

        public static class ReloadSection extends OkaeriConfig {
            @Comment("Reload success message. <type> = config type")
            public String success = "<green>MMOSpawnPoint configuration successfully reloaded (<type>).";

            @Comment("Reload error message. <error> = error details")
            public String error = "<red>Failed to reload configuration: <error>";
        }

        public static class SetSpawnPointSection extends OkaeriConfig {
            @Comment("Shown to console when no player is provided: /msp setspawnpoint <player>")
            public String consoleUsage = "<red>Console must specify a player: <yellow>/msp setspawnpoint <player>";

            @Comment("Shown to a player who sets their OWN spawn (no target provided). <location> = 'x, y, z in world'")
            public String selfSuccess = "<green>Your spawn point has been set to: <yellow><location>";

            @Comment("Shown to the command sender when setting another player's spawn. <player>, <location>")
            public String otherSuccess = "<green>Set spawn point for <yellow><player> <green>to: <yellow><location>";

            @Comment("Notification to the target player when someone else sets their spawn. <setter>, <location>")
            public String targetNotification = "<green>Your spawn point has been set by <yellow><setter> <green>to: <yellow><location>";

            @Comment("Generic failure message (unexpected error). <player>")
            public String failed = "<red>Failed to set spawn point for <player>";

            @Comment("Error when the specified world does not exist. <world>")
            public String invalidWorld = "<red>Unknown world: <yellow><world>";

            @Comment("Error when coordinates cannot be parsed or are incomplete.")
            public String invalidCoords = "<red>Invalid coordinates. Usage: <yellow>/msp setspawnpoint [player] <world> <x> <y> <z> [yaw] [pitch]";

            @Comment("Error when the specified player is not online. <player>")
            public String playerNotFound = "<red>Player not found: <yellow><player>";

            @Comment("Error for console if target player provided but no coordinates.")
            public String consoleNeedsCoords = "<red>Console must specify coordinates for the target player.";

            @Comment("Error for console if using world-first syntax without a player.")
            public String consoleNeedsPlayer = "<red>Console must specify player and coordinates.";
        }

        @Comment("Simulation command messages")
        public SimulateSection simulate = new SimulateSection();

        public static class SimulateSection extends OkaeriConfig {
            @Comment("Help lines for /msp simulate")
            public String helpHeader = "<gold>=== Simulate Commands ===";
            public String helpDeathLine = "<yellow>/msp simulate death [player] <gray>- Simulate death respawn";
            public String helpJoinLine = "<yellow>/msp simulate join [player] <gray>- Simulate join teleport";
            public String helpBackLine = "<yellow>/msp simulate back [player] <gray>- Return to pre-simulation location";

            @Comment("No permission")
            public String noPermission = "<red>You don't have permission.";
            @Comment("Only players can use this")
            public String onlyPlayers = "<red>Only players can use this command.";

            @Comment("Feedback for simulate death/join")
            public String deathSelf = "<yellow>Simulating death respawn...";
            public String deathOther = "<yellow>Simulating death respawn for <white><player></white>...";
            public String joinSelf = "<yellow>Simulating join...";
            public String joinOther = "<yellow>Simulating join for <white><player></white>...";

            @Comment("Simulation failed (no spawn matched)")
            public String simulationFailed = "<red>Simulation failed: no spawn matched.";

            @Comment("Back messages")
            public String backSelf = "<green>Returned to your previous location.";
            public String backOther = "<green>Returned <white><player></white> to their previous location.";
            public String backNone = "<red>No previous location stored.";
        }

        @Comment("Cache command messages")
        public CacheSection cache = new CacheSection();

        public static class CacheSection extends OkaeriConfig {
            @Comment("Help lines for /msp cache")
            public String helpHeader = "<gold>=== Cache Commands ===";
            public String helpStatsLine = "<yellow>/msp cache stats <gray>- Show cache statistics";
            public String helpClearLine = "<yellow>/msp cache clear [player] <gray>- Clear cache (all or player-specific)";

            @Comment("Stats line: <searches> <hits> <misses> <hitRate> <size> <enabled> <expiry> <max>")
            public String statsLine = "<gray>Cache: searches=<yellow><searches></yellow>, hits=<yellow><hits></yellow>, misses=<yellow><misses></yellow>, hitRate=<yellow><hitRate>%</yellow>, size=<yellow><size></yellow>, enabled=<yellow><enabled></yellow>, expiry=<yellow><expiry></yellow>s, max=<yellow><max></yellow>";

            public String clearedAll = "<green>Cleared entire safe-location cache.";
            public String clearedPlayer = "<green>Cleared cache for <yellow><player></yellow>.";
        }
    }

    public static class GeneralSection extends OkaeriConfig {
        @Comment("Message when no spawn location found")
        public String noSpawnFound = "<red>No suitable spawn location found, using server default.";

        @Comment("Message when waiting for resource pack")
        public String waitingForResourcePack = "<yellow>Waiting for resource pack to load...";

        @Comment("Message when resource pack loading completed")
        public String resourcePackLoaded = "<green>Resource pack loaded successfully!";

        @Comment("Message when resource pack loading failed")
        public String resourcePackFailed = "<red>Resource pack failed to load, continuing anyway...";
    }

    public static class PartySection extends OkaeriConfig {
        public String prefix = "<dark_gray>[<red>Death Party<dark_gray>] ";

        // Invite flow
        public String inviteSent = "<gray>Soul bond invitation sent to <red><player>";
        public String inviteReceived = "<gray><player> <gray>invites you to join their <red>death party<gray>. Type <white>/msp party accept <gray>to bind your souls";
        public String invitationDeclined = "<green>You have rejected the call of darkness";
        public String invitationDeclinedToLeader = "<red><player> <gray>has rejected your soul bond";
        public String invitationExpiredOrInvalid = "<red>The soul bond has faded into the void";
        public String inviteFailedPartyFull = "<red>The soul circle is complete - no room for more damned spirits";
        public String inviteFailedAlreadyInParty = "<red>This soul is already bound to another death circle";
        public String noInvitations = "<gray>No soul bonds await your decision";
        public String inviteExpired = "<gray>The soul bond invitation has faded into darkness";

        // Join/Leave/Kick
        public String joinedParty = "<green>Your soul has been bound to the death circle!";
        public String playerJoinedParty = "<red><player><gray>'s soul has joined the death circle";
        public String leftParty = "<green>Your soul has been freed from the death circle";
        public String playerLeftParty = "<red><player><gray>'s soul has departed the death circle";
        public String playerRemoved = "<red>Your soul has been cast out from the death circle";
        public String playerRemovedFromParty = "<red><player> <gray>has been banished from the death circle";
        public String cannotRemoveSelf = "<red>You cannot banish your own soul. Use <yellow>/msp party leave <red>to depart";
        public String partyDisbanded = "<gray>The death party has dissolved into the void";

        // Permissions/system
        public String onlyPlayers = "<red>Only mortals can command the souls of darkness";
        public String systemDisabled = "<red>The necromantic arts are forbidden in this realm";
        public String notInParty = "<gray>Your soul walks alone in darkness";
        public String notLeader = "<red>Only the death party leader can command the souls";
        public String playerNotInYourParty = "<red>This soul is not bound to your death circle";
        public String invalidRespawnMode = "<red>Unknown soul binding ritual. Use 'NORMAL' or 'PARTY_MEMBER'";
        public String errorOccurred = "<red>Dark forces interfered with the ritual. Try again";
        public String respawnedAtMember = "<gray>Your soul has been drawn to <red><player>'s <gray>essence";
        public String respawnModeChanged = "<gray>Death party binding changed to: <red><mode>";
        public String respawnTargetSet = "<gray>Your soul will now seek <red><player> <gray>in death";
        public String respawnDisabledRegion = "<red>The necromantic bonds are severed in this sacred ground";
        public String respawnDisabledWorld = "<red>Death party bonds cannot pierce this realm's barriers";
        public String respawnCooldown = "<gray>Your soul must rest for <red><time> <gray>seconds before the next summoning";
        public String walkingSpawnPointMessage = "<dark_gray>[<red>Death<dark_gray>] <gray>You have risen at your death location as a <red>walking spawn point<gray>.";
        public String walkingSpawnPointRestricted = "<dark_gray>[<red>Death<dark_gray>] <red>Dark forces prevent you from rising here...";
        public String alreadyLeader = "<red>You already command the death party, dark lord";
        public String newLeaderAssigned = "<red><player> <gray>now commands the death party";
        public String respawnTooFar = "<red>You are too far from your party member to respawn there.";

        // List command
        public String listHeader = "<dark_gray>[<red>Bound Souls<dark_gray>]";
        public String listLeader = "<red>👑 <player> <gray>(Death Lord)";
        public String listLeaderMissing = "<red>👑 <gray>(Death Lord lost in the void)";
        public String listMember = "<gray>☠ <player>";
        public String listAnchor = "<dark_red>🔄 <player> <gray>(Soul Anchor)";
        public String listAnchorMissing = "<gray>Soul Anchor: <red>(Lost in darkness)";
        public String listSettingsHeader = "<dark_gray>[<red>Death Circle Settings<dark_gray>]";
        public String listRespawnMode = "<gray>Soul Binding: <red><mode>";
        public String listNoAnchor = "<gray>Soul Anchor: <red>None";
        public String listSeparator = "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";

        @Comment("Party help messages")
        public PartyHelpSection help = new PartyHelpSection();

        public static class PartyHelpSection extends OkaeriConfig {
            public String header = "<dark_gray>[<red>Soul Binding<dark_gray>] <gray>Available death party commands:";
            public String invite = "<gray>» <white>/msp party invite <dark_gray><<red>soul<dark_gray>> <dark_gray>- <gray>Bind a soul to your <red>death party";
            public String accept = "<gray>» <white>/msp party accept <dark_gray>- <gray>Accept a <red>soul bond <gray>invitation";
            public String deny = "<gray>» <white>/msp party deny <dark_gray>- <gray>Reject a <red>soul bond <gray>invitation";
            public String leave = "<gray>» <white>/msp party leave <dark_gray>- <gray>Sever your soul from the <red>death party";
            public String list = "<gray>» <white>/msp party list <dark_gray>- <gray>View all <red>bound souls";
            public String remove = "<gray>» <white>/msp party remove <dark_gray><<red>soul<dark_gray>> <dark_gray>- <gray>Cast out a soul from your <red>death party";
            public String setleader = "<gray>» <white>/msp party setleader <dark_gray><<red>soul<dark_gray>> <dark_gray>- <gray>Transfer <red>dark leadership";
            public String options = "<gray>» <white>/msp party options <dark_gray>- <gray>Configure <red>soul binding <gray>options";
        }
    }

    public static class JoinsSection extends OkaeriConfig {
        @Comment("Message when player joins and gets teleported")
        public String teleportedOnJoin = "<green>Welcome! You have been teleported to your designated location.";

        @Comment("Message when join teleport is skipped because player is dead")
        public String skippedDead = "<gray>Join teleport skipped - you will respawn normally.";

        @Comment("Message when waiting in waiting room for resource pack")
        public String waitingInRoom = "<yellow>Please wait while your resource pack loads...";

        @Comment("Message when resource pack timeout reached")
        public String resourcePackTimeout = "<red>Resource pack loading timed out, teleporting anyway...";
    }
}