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

        public static class HelpSection extends OkaeriConfig {
            @Comment("Main help command header")
            public String header = "<gold>=== MMOSpawnPoint Help ===";

            @Comment("Reload command help line")
            public String reloadLine = "<yellow>/ssp reload <type> <gray>- Reload the plugin configuration";

            @Comment("Party command help line")
            public String partyLine = "<yellow>/ssp party <gray>- Soul binding commands";

            @Comment("Help command help line")
            public String helpLine = "<yellow>/ssp help <gray>- Show this help message";
        }

        public static class ReloadSection extends OkaeriConfig {
            @Comment("Reload success message. <type> = config type")
            public String success = "<green>MMOSpawnPoint configuration successfully reloaded (<type>).";

            @Comment("Reload error message. <error> = error details")
            public String error = "<red>Failed to reload configuration: <error>";
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
        public String inviteReceived = "<gray><player> <gray>invites you to join their <red>death party<gray>. Type <white>/ssp party accept <gray>to bind your souls";
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
        public String cannotRemoveSelf = "<red>You cannot banish your own soul. Use <yellow>/ssp party leave <red>to depart";
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
            public String invite = "<gray>» <white>/ssp party invite <dark_gray><<red>soul<dark_gray>> <dark_gray>- <gray>Bind a soul to your <red>death party";
            public String accept = "<gray>» <white>/ssp party accept <dark_gray>- <gray>Accept a <red>soul bond <gray>invitation";
            public String deny = "<gray>» <white>/ssp party deny <dark_gray>- <gray>Reject a <red>soul bond <gray>invitation";
            public String leave = "<gray>» <white>/ssp party leave <dark_gray>- <gray>Sever your soul from the <red>death party";
            public String list = "<gray>» <white>/ssp party list <dark_gray>- <gray>View all <red>bound souls";
            public String remove = "<gray>» <white>/ssp party remove <dark_gray><<red>soul<dark_gray>> <dark_gray>- <gray>Cast out a soul from your <red>death party";
            public String setleader = "<gray>» <white>/ssp party setleader <dark_gray><<red>soul<dark_gray>> <dark_gray>- <gray>Transfer <red>dark leadership";
            public String options = "<gray>» <white>/ssp party options <dark_gray>- <gray>Configure <red>soul binding <gray>options";
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