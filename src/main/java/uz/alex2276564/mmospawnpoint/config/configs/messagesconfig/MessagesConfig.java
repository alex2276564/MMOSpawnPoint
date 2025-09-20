package uz.alex2276564.mmospawnpoint.config.configs.messagesconfig;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

import java.util.HashSet;
import java.util.Set;

public class MessagesConfig extends OkaeriConfig {

    @Comment("# ================================================================")
    @Comment("# 💬 Messages Configuration")
    @Comment("# ================================================================")
    @Comment("# 🎨 TEXT FORMATTING:")
    @Comment("# • Full MiniMessage support on Paper 1.18+")
    @Comment("# • Automatic fallback to legacy colors on older versions")
    @Comment("# • Examples: <red>Error!</red>, <green>Success!</green>")
    @Comment("# • Advanced: gradients, hover effects, click events")
    @Comment("# • Web editor: https://webui.advntr.dev/")
    @Comment("#")
    @Comment("# 🌍 LOCALIZATION:")
    @Comment("# • This plugin doesn't include built-in multi-language support")
    @Comment("# • For multiple languages, use Triton plugin:")
    @Comment("#   → https://www.spigotmc.org/resources/triton.30331/")
    @Comment("# ================================================================")
    @Comment("")

    @Comment("# ================================================================")
    @Comment("# 🔇 MESSAGE CONTROL SYSTEM")
    @Comment("# ================================================================")
    @Comment("# You can selectively disable individual messages by adding their")
    @Comment("# keys to the list below. This is useful for customizing user")
    @Comment("# experience without editing every message.")
    @Comment("#")
    @Comment("# 📝 HOW TO USE:")
    @Comment("# 1. Find the message you want to disable in this config")
    @Comment("# 2. Copy its full path using dot notation")
    @Comment("# 3. Add the path to disabledKeys list below")
    @Comment("# 4. Reload the plugin")
    @Comment("#")
    @Comment("# 🎯 EXAMPLES:")
    @Comment("# To disable specific command feedback:")
    @Comment("# - 'commands.reload.success'")
    @Comment("# - 'commands.help.header'")
    @Comment("#")
    @Comment("# To disable general system messages:")
    @Comment("# - 'general.noSpawnFound'")
    @Comment("# - 'general.systemDisabled'")
    @Comment("#")
    @Comment("# ⚠️ IMPORTANT NOTES:")
    @Comment("# • Keys are case-sensitive and must match exactly")
    @Comment("# • This affects ALL recipients (players AND console)")
    @Comment("# • Some messages may be important for debugging")
    @Comment("# • Also keep in mind that some messages are hardcoded")
    @Comment("# • into the plugin logic and cannot be disabled")
    @Comment("# • Do NOT use empty strings (\"\") to disable messages")
    @Comment("# • Do NOT delete message entries - use this system instead")
    @Comment("#")
    @Comment("# 🔍 FINDING KEYS:")
    @Comment("# Structure follows: section.subsection.messageKey")
    @Comment("# Check the organization below to find the correct path")
    @Comment("# ================================================================")
    @Comment("")
    public Set<String> disabledKeys = new HashSet<>();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# 🔧 COMMAND SYSTEM")
    @Comment("# ================================================================")
    @Comment("# All command-related feedback messages")
    @Comment("# Used by: /msp help, /msp reload, /msp simulate, etc.")
    @Comment("# ================================================================")
    @Comment("")
    public CommandsSection commands = new CommandsSection();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# ⚙️ CORE SYSTEM MESSAGES")
    @Comment("# ================================================================")
    @Comment("# General system notifications and error messages")
    @Comment("# ================================================================")
    @Comment("")
    public GeneralSection general = new GeneralSection();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# 👥 PARTY SYSTEM")
    @Comment("# ================================================================")
    @Comment("# Complete party/soul binding system messages")
    @Comment("# Includes: invitations, management, respawning, restrictions")
    @Comment("# Theme: Dark/gothic style matching the 'death party' concept")
    @Comment("# ================================================================")
    @Comment("")
    public PartySection party = new PartySection();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# 📦 RESOURCE PACK SYSTEM")
    @Comment("# ================================================================")
    @Comment("# Resource pack loading and waiting room messages")
    @Comment("# Used when: join.waitForResourcePack = true")
    @Comment("# ================================================================")
    @Comment("")
    public ResourcePackSection resourcepack = new ResourcePackSection();

    @Comment("")
    @Comment("# ================================================================")
    @Comment("# 🚪 JOIN SYSTEM")
    @Comment("# ================================================================")
    @Comment("# Player join handling and teleportation messages")
    @Comment("# ================================================================")
    @Comment("")
    public JoinSection join = new JoinSection();

    // ================================================================
    // COMMAND SYSTEM SUBSECTIONS
    // ================================================================

    public static class CommandsSection extends OkaeriConfig {

        @Comment("📋 Help command (/msp help)")
        public HelpSection help = new HelpSection();

        @Comment("")
        @Comment("🔄 Reload command (/msp reload)")
        public ReloadSection reload = new ReloadSection();

        @Comment("")
        @Comment("📍 Spawnpoint management commands (set/clear/teleport/show)")
        public SpawnPointSection spawnpoint = new SpawnPointSection();

        @Comment("")
        @Comment("🧪 Simulation tools (/msp simulate)")
        public SimulateSection simulate = new SimulateSection();

        @Comment("")
        @Comment("💾 Cache management (/msp cache)")
        public CacheSection cache = new CacheSection();

        public static class HelpSection extends OkaeriConfig {
            public String header = "<gold>=== MMOSpawnPoint Help ===";
            public String reloadLine = "<yellow>/msp reload <type> <gray>- Reload the plugin configuration";
            public String partyLine = "<yellow>/msp party <gray>- Soul binding commands";
            public String simulateLine = "<yellow>/msp simulate <gray>- Simulation tools (death/join/back)";
            public String cacheLine = "<yellow>/msp cache <gray>- Safe-location cache tools";
            public String spawnpointLine = "<yellow>/msp spawnpoint <gray>- Manage bed/anchor spawn (set/clear/teleport/show)";
            public String helpLine = "<yellow>/msp help <gray>- Show this help message";
        }

        public static class ReloadSection extends OkaeriConfig {
            @Comment("Success message. Placeholder: <type> = what was reloaded")
            public String success = "<green>MMOSpawnPoint configuration successfully reloaded (<type>).";

            @Comment("Error message. Placeholder: <error> = error details")
            public String error = "<red>Failed to reload configuration: <error>";
        }

        public static class SpawnPointSection extends OkaeriConfig {

            public Help help = new Help();
            public Set set = new Set();
            public Clear clear = new Clear();
            public Teleport teleport = new Teleport();
            public Show show = new Show();

            public static class Help extends OkaeriConfig {
                public String header = "<gold>=== SpawnPoint Commands ===";
                public String setLine = "<yellow>/msp spawnpoint set [player] <world> <x> <y> <z> [yaw] [pitch] [--if-has|--if-missing] [--only-if-incorrect] [--require-valid-bed] [--dry-run] <gray>- Set bed/anchor spawn";
                public String clearLine = "<yellow>/msp spawnpoint clear [player] [--if-has] [--dry-run] <gray>- Clear bed/anchor spawn";
                public String teleportLine = "<yellow>/msp spawnpoint teleport [player] <gray>- Teleport to bed/anchor spawn";
                public String showLine = "<yellow>/msp spawnpoint show [player] <gray>- Show bed/anchor spawn with a clickable teleport";
            }

            public static class Set extends OkaeriConfig {
                @Comment("Console usage instructions")
                public String consoleUsage = "<red>Usage:</red> <yellow>/msp spawnpoint set [player] <world> <x> <y> <z> [yaw] [pitch] [--if-has|--if-missing] [--only-if-incorrect] [--require-valid-bed] [--dry-run]";

                @Comment("Self-success message. Placeholder: <location>")
                public String selfSuccess = "<green>Your spawn point has been set to:</green> <yellow><location>";

                @Comment("Other player success. Placeholders: <player>, <location>")
                public String otherSuccess = "<green>Set spawn point for <yellow><player></yellow> to:</green> <yellow><location>";

                @Comment("Target notification. Placeholders: <setter>, <location>")
                public String targetNotification = "<green>Your spawn point was set by <yellow><setter></yellow> to:</green> <yellow><location>";

                @Comment("Failure message. Placeholder: <player>")
                public String failed = "<red>Failed to set spawn point for <yellow><player></yellow>.";

                @Comment("Error message. Placeholder: <error>")
                public String error = "<red>Error:</red> <gray><error>";

                @Comment("Invalid world error. Placeholder: <world>")
                public String invalidWorld = "<red>Unknown world:</red> <yellow><world>";

                @Comment("Invalid coords")
                public String invalidCoords = "<red>Invalid coordinates.";

                @Comment("Player not found. Placeholder: <player>")
                public String playerNotFound = "<red>Player not found:</red> <yellow><player>";

                @Comment("Skipped: --if-has but missing")
                public String skippedIfHas = "<yellow>Skipped:</yellow> player does not currently have a valid bed/anchor spawn.";

                @Comment("Skipped: --if-missing but present")
                public String skippedIfMissing = "<yellow>Skipped:</yellow> player already has a bed/anchor spawn.";

                @Comment("Skipped: --only-if-incorrect and current equals desired")
                public String skippedIfCorrect = "<yellow>Skipped:</yellow> desired spawn matches current one.";

                @Comment("Skipped: --require-valid-bed but location has no valid bed/anchor")
                public String skippedNoValidBed = "<yellow>Skipped:</yellow> --require-valid-bed: location has no valid bed/anchor.";

                @Comment("Dry-run message. Placeholders: <player>, <location>")
                public String dryRun = "<yellow>[DRY-RUN]</yellow> Would set spawn for <white><player></white> to <yellow><location></yellow>";
            }

            public static class Clear extends OkaeriConfig {
                @Comment("Console needs player for this command")
                public String consoleNeedsPlayer = "<red>Console must specify a player:</red> <yellow>/msp spawnpoint clear <player>";

                @Comment("Success (self)")
                public String successSelf = "<green>Your bed/anchor spawn has been cleared.";

                @Comment("Success (other). Placeholder: <player>")
                public String successOther = "<green>Cleared bed/anchor spawn for <yellow><player></yellow>.";

                @Comment("Target notification. Placeholder: <setter>")
                public String targetNotified = "<yellow>Your bed/anchor spawn was cleared by <white><setter></white>.";

                @Comment("Nothing to clear")
                public String noSpawn = "<yellow>Nothing to clear: no bed/anchor spawn.";

                @Comment("Failed. Placeholder: <player>")
                public String failed = "<red>Failed to clear bed/anchor spawn for <yellow><player></yellow>.";

                @Comment("Dry-run. Placeholder: <player>")
                public String dryRun = "<yellow>[DRY-RUN]</yellow> Would clear spawn for <white><player></white>";
            }

            public static class Teleport extends OkaeriConfig {
                @Comment("Console needs player for this command")
                public String consoleNeedsPlayer = "<red>Console must specify a player:</red> <yellow>/msp spawnpoint teleport <player>";

                @Comment("Success (self)")
                public String successSelf = "<green>Teleported to your bed/anchor spawn.";

                @Comment("Success (other). Placeholder: <player>")
                public String successOther = "<green>Teleported <yellow><player></yellow> to their bed/anchor spawn.";

                @Comment("Target notified")
                public String targetNotified = "<yellow>You were teleported to your bed/anchor spawn.";

                @Comment("No spawn")
                public String noSpawn = "<red>No valid bed/anchor spawn found.";

                @Comment("No spawn (other). Placeholder: <player>")
                public String noSpawnOther = "<red>No valid bed/anchor spawn for <yellow><player></yellow>.";
            }

            public static class Show extends OkaeriConfig {
                @Comment("Console needs player for this command")
                public String consoleNeedsPlayer = "<red>Console must specify a player:</red> <yellow>/msp spawnpoint show <player>";

                @Comment("No spawn (self)")
                public String noSpawn = "<yellow>No bed/anchor spawn set.";

                @Comment("No spawn (other). Placeholder: <player>")
                public String noSpawnOther = "<yellow>No bed/anchor spawn set for <white><player></white>.";

                @Comment("Fancy line with MiniMessage (placeholders: <player> <coords> <world>)")
                public String line =
                        "<dark_gray>[<gold>SpawnPoint</gold>]</dark_gray> " +
                                "<gray><player> → </gray>" +
                                "<white><coords></white> <gray>in</gray> <yellow><world></yellow>  " +
                                "<click:run_command:'/msp spawnpoint teleport <player>'>" +
                                "<hover:show_text:'<yellow>Click to teleport to <player> spawn</yellow>'>" +
                                "<green>[Teleport]</green></hover></click>";
            }
        }

        public static class SimulateSection extends OkaeriConfig {
            @Comment("Help system for simulation commands")
            public String helpHeader = "<gold>=== Simulate Commands ===";
            public String helpDeathLine = "<yellow>/msp simulate death [player] <gray>- Simulate death respawn";
            public String helpJoinLine = "<yellow>/msp simulate join [player] <gray>- Simulate join teleport";
            public String helpBackLine = "<yellow>/msp simulate back [player] <gray>- Return to pre-simulation location";

            @Comment("Permission and access messages")
            public String noPermission = "<red>You don't have permission.";
            public String onlyPlayers = "<red>Only players can use this command.";

            @Comment("Simulation feedback. Placeholder: <player> for 'Other' variants")
            public String deathSelf = "<yellow>Simulating death respawn...";
            public String deathOther = "<yellow>Simulating death respawn for <white><player></white>...";
            public String joinSelf = "<yellow>Simulating join...";
            public String joinOther = "<yellow>Simulating join for <white><player></white>...";
            public String simulationFailed = "<red>Simulation failed: no spawn matched.";

            @Comment("Back command feedback. Placeholder: <player> for 'Other' variant")
            public String backSelf = "<green>Returned to your previous location.";
            public String backOther = "<green>Returned <white><player></white> to their previous location.";
            public String backNone = "<red>No previous location stored.";
        }

        public static class CacheSection extends OkaeriConfig {
            @Comment("Help system for cache commands")
            public String helpHeader = "<gold>=== Cache Commands ===";
            public String helpStatsLine = "<yellow>/msp cache stats <gray>- Show cache statistics";
            public String helpClearLine = "<yellow>/msp cache clear [player] <gray>- Clear cache (all or player-specific)";

            @Comment("Statistics display. Placeholders: <searches>, <hits>, <misses>, <hitRate>, <size>, <enabled>, <expiry>, <max>")
            public String statsLine = "<gray>Cache: searches=<yellow><searches></yellow>, hits=<yellow><hits></yellow>, misses=<yellow><misses></yellow>, hitRate=<yellow><hitRate>%</yellow>, size=<yellow><size></yellow>, enabled=<yellow><enabled></yellow>, expiry=<yellow><expiry></yellow>s, max=<yellow><max></yellow>";

            public String clearedAll = "<green>Cleared entire safe-location cache.";

            @Comment("Player-specific clear. Placeholder: <player>")
            public String clearedPlayer = "<green>Cleared cache for <yellow><player></yellow>.";
        }
    }

    // ================================================================
    // CORE SYSTEM
    // ================================================================

    public static class GeneralSection extends OkaeriConfig {
        public String noSpawnFound = "<red>No suitable spawn location found, using server default.";
    }

    // ================================================================
    // PARTY SYSTEM (EXTENSIVE)
    // ================================================================

    public static class PartySection extends OkaeriConfig {

        @Comment("🎨 Party message prefix (use <prefix> in messages below)")
        @Comment("Theme: Dark/gothic to match 'death party' concept")
        public String prefix = "<dark_gray>[<red>Death Party<dark_gray>]";

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 📨 INVITATION SYSTEM")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Handles party invites, accepts, declines, and expiration")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        @Comment("Invitation flow. Placeholders: <player> where noted")
        public String inviteSent = "<prefix> <gray>Soul bond invitation sent to <red><player>";
        public String inviteReceived = "<prefix> <gray><player> <gray>invites you to join their <red>death party</red><gray>. Type <white>/msp party accept <gray>to bind your souls </white> or click [<click:run_command:'/msp party accept'><green>Accept</green></click><gray>|</gray><click:run_command:'/msp party deny'><red>Deny</red></click>]";
        public String invitationDeclined = "<prefix> <green>You have rejected the call of darkness";
        public String invitationDeclinedToLeader = "<prefix> <red><player> <gray>has rejected your soul bond";
        public String invitationExpiredOrInvalid = "<prefix> <red>The soul bond has faded into the void";
        public String inviteFailedPartyFull = "<prefix> <red>The soul circle is complete - no room for more damned spirits";
        public String inviteFailedAlreadyInParty = "<prefix> <red>This soul is already bound to another death circle";
        public String noInvitations = "<prefix> <gray>No soul bonds await your decision";
        public String inviteExpired = "<prefix> <gray>The soul bond invitation has faded into darkness";

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 👥 PARTY MANAGEMENT")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Join, leave, kick, and disbanding messages")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        public String joinedParty = "<prefix> <green>Your soul has been bound to the death circle!";
        public String playerJoinedParty = "<prefix> <red><player><gray>'s soul has joined the death circle";
        public String leftParty = "<prefix> <green>Your soul has been freed from the death circle";
        public String playerLeftParty = "<prefix> <red><player><gray>'s soul has departed the death circle";
        public String playerRemoved = "<prefix> <red>Your soul has been cast out from the death circle";
        public String playerRemovedFromParty = "<prefix> <red><player> <gray>has been banished from the death circle";
        public String cannotRemoveSelf = "<prefix> <red>You cannot banish your own soul. Use <yellow>/msp party leave <red>to depart";
        public String partyDisbanded = "<prefix> <gray>The death party has dissolved into the void";

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 🔐 PERMISSIONS & SYSTEM")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Access control and system state messages")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        public String onlyPlayers = "<prefix> <red>Only mortals can command the souls of darkness";
        public String systemDisabled = "<prefix> <red>The necromantic arts are forbidden in this realm";
        public String notInParty = "<prefix> <gray>Your soul walks alone in darkness";
        public String notLeader = "<prefix> <red>Only the death party leader can command the souls";
        public String playerNotInYourParty = "<prefix> <red>This soul is not bound to your death circle";
        public String invalidRespawnMode = "<prefix> <red>Unknown soul binding ritual. Use 'NORMAL' or 'PARTY_MEMBER'";
        public String errorOccurred = "<prefix> <red>Dark forces interfered with the ritual. Try again";

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 👑 LEADERSHIP & OPTIONS")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Leader transfer and party configuration")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        public String alreadyLeader = "<prefix> <red>You already command the death party, dark lord";
        public String newLeaderAssigned = "<prefix> <red><player> <gray>now commands the death party";
        public String respawnModeChanged = "<prefix> <gray>Death party binding changed to: <red><mode>";
        public String respawnTargetSet = "<prefix> <gray>Your soul will now seek <red><player> <gray>in death";

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# ⚰️ RESPAWN SYSTEM")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Death, respawning, restrictions, and walking spawn points")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
        public String respawnedAtMember = "<prefix> <gray>Your soul has been drawn to <red><player>'s <gray>essence";
        public String respawnDisabledRegion = "<prefix> <red>The necromantic bonds are severed in this sacred ground";
        public String respawnDisabledWorld = "<prefix> <red>Death party bonds cannot pierce this realm's barriers";

        @Comment("Cooldown message. Placeholder: <time> = seconds remaining")
        public String respawnCooldown = "<prefix> <gray>Your soul must rest for <red><time> <gray>seconds before the next summoning";
        public String respawnTooFar = "<prefix> <red>You are too far from your party member to respawn there.";

        @Comment("")
        @Comment("🚶 Walking spawn point (respawn at death location)")
        public String walkingSpawnPointMessage = "<prefix> <dark_gray>[<red>Death<dark_gray>] <gray>You have risen at your death location as a <red>walking spawn point<gray>.";
        public String walkingSpawnPointRestricted = "<prefix> <dark_gray>[<red>Death<dark_gray>] <red>Dark forces prevent you from rising here...";

        @Comment("")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# 📋 PARTY LIST DISPLAY")
        @Comment("# ----------------------------------------------------------------")
        @Comment("# Used by /msp party list command")
        @Comment("# ----------------------------------------------------------------")
        @Comment("")
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

        @Comment("")
        @Comment("⚙️ Party options configuration (/msp party options)")
        public PartyOptionsSection options = new PartyOptionsSection();

        @Comment("")
        @Comment("📖 Party help system (/msp party help)")
        public PartyHelpSection help = new PartyHelpSection();

        public static class PartyOptionsSection extends OkaeriConfig {
            public String header = "<dark_gray>[<red>Death Circle Options<dark_gray>]";
            public String respawnMode = "<gray>Soul Binding Mode: <red><mode>";
            public String respawnTarget = "<gray>Soul Target: <red><target>";
            public String respawnTargetNotFound = "<gray>(Wandering in the void)</gray>";
            public String separator = "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            public String modeHelp = "<gray>Use <white>/msp party options mode <red><normal|party_member><gray> to change binding";
            public String targetHelp = "<gray>Use <white>/msp party options target <red><soul><gray> to set target soul";
        }

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

    // ================================================================
    // RESOURCE PACK SYSTEM
    // ================================================================

    public static class ResourcePackSection extends OkaeriConfig {
        @Comment("🎯 Used when join.waitForResourcePack = true")
        public String waiting = "<yellow>Waiting for resource pack to load...";
        public String loaded = "<green>Resource pack loaded successfully!";
        public String failed = "<red>Resource pack failed to load, continuing anyway...";
        public String waitingInRoom = "<yellow>Please wait while your resource pack loads...";
        public String timeout = "<red>Resource pack loading timed out, teleporting anyway...";
    }

    // ================================================================
    // JOIN SYSTEM
    // ================================================================

    public static class JoinSection extends OkaeriConfig {
        public String teleportedOnJoin = "<green>Welcome! You have been teleported to your designated location.";

        @Comment("🔄 Shown when join teleport is skipped due to player being dead")
        public String skippedDead = "<gray>Join teleport skipped - you will respawn normally.";
    }
}