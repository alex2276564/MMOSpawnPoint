# MMOSpawnPoint 🏰

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5+-brightgreen)](https://papermc.io/software/paper)
[![Java Version](https://img.shields.io/badge/java-17+-orange)](https://adoptium.net/installation/linux/)
[![GitHub Release](https://img.shields.io/github/v/release/alex2276564/MMOSpawnPoint?color=blue)](https://github.com/alex2276564/MMOSpawnPoint/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Text Formatting](https://img.shields.io/badge/Text%20Formatting-🌈%20MiniMessage-ff69b4)](https://docs.advntr.dev/minimessage/)

**MMOSpawnPoint** is an advanced MMO spawn system with region-based spawns, party respawn mechanics, safe location finding, resource pack integration, and comprehensive condition-based teleportation. Features walking spawn points, waiting rooms, batched safe location search, advanced caching, and extensive customization for MMO servers.

## ✨ Features

* **Advanced Spawn Types:** Configure fixed, random, multiple random, or safe location search spawn points
* **Region-Based Spawns:** Set different spawn points for different WorldGuard regions with regex pattern matching
* **World-Based Spawns:** Configure unique spawn points for each world with pattern support
* **Coordinate-Based Spawns:** Define precise trigger areas with flexible axis constraints
* **Conditional Spawns:** Use permissions and PlaceholderAPI with full logical expression support
* **Batched safe search (tick-budgeted on Paper, region-thread on Folia) Location Finding:** Advanced caching system with configurable search strategies
* **Waiting Room System:** Professional partial async processing with customizable waiting areas
* **Advanced Party System:** Complete party mechanics with respawn, cooldowns, restrictions, and respawn target
* **Walking Spawn Points:** Content creators can respawn at death location and serve as party anchor points
* **Resource Pack Integration:** Seamless resource pack loading with waiting room support
* **Comprehensive Actions:** Execute commands, send messages, with conditional chances and phase control
* **Extensive Customization:** Priority-based matching, ground whitelists, Y-selection strategies
* **Professional Admin Tools:** Simulation system, cache management, debug utilities
* **Auto-Update Check:** Automatic version checking with GitHub integration
* **Modern Text Rendering:** Uses Adventure MiniMessage for sleek formatting on supported servers (Paper 1.18+), with automatic fallback on older versions.

## 📥 Installation

1. **Download:** Download the latest version of MMOSpawnPoint from the [Releases](https://github.com/alex2276564/MMOSpawnPoint/releases) page.
2. **Install:** Place the `.jar` file into your server's `plugins` folder.
3. **Optional Dependencies:**
    - [WorldGuard](https://dev.bukkit.org/projects/worldguard) - For region-based spawn points
    - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) - For condition-based spawn points
4. **Restart:** Restart your server to load the plugin.

## 📜 Commands & Permissions

MMOSpawnPoint supports both the full command `/mmospawnpoint` and the shorter alias `/msp` for all commands (requires `mmospawnpoint.command` permission).

### Main Commands

* `/msp help` - Show help information (requires `mmospawnpoint.command`)
* `/msp reload` - Reload plugin configuration (requires `mmospawnpoint.reload`)

### Party System Commands

* `/msp party` - Show party help (requires `mmospawnpoint.party`)
* `/msp party invite [player]` - Invite a player to your party (requires `mmospawnpoint.party.invite`)
* `/msp party accept` - Accept a party invitation (requires `mmospawnpoint.party.accept`)
* `/msp party deny` - Decline a party invitation (requires `mmospawnpoint.party.deny`)
* `/msp party leave` - Leave your current party (requires `mmospawnpoint.party.leave`)
* `/msp party list` - List all members in your party (requires `mmospawnpoint.party.list`)
* `/msp party remove [player]` - Remove a player from your party (requires `mmospawnpoint.party.remove`)
* `/msp party setleader [player]` - Transfer party leadership (requires `mmospawnpoint.party.setleader`)
* `/msp party options` - View and change party options (requires `mmospawnpoint.party.options`)
* `/msp party options mode <normal|party_member>` - Change party respawn mode (requires `mmospawnpoint.party.options.mode`)
* `/msp party options target [player]` - Set party respawn target (requires `mmospawnpoint.party.options.target`)

### Admin & Debug Commands

* `/msp simulate` - Show simulation help (requires `mmospawnpoint.simulate`)
* `/msp simulate death [player]` - Simulate death respawn (requires `mmospawnpoint.simulate.death`)
* `/msp simulate join [player]` - Simulate join teleport (requires `mmospawnpoint.simulate.join`)
* `/msp simulate back [player]` - Return to pre-simulation location (requires `mmospawnpoint.simulate.back`)
* `/msp cache` - Show cache help (requires `mmospawnpoint.cache`)
* `/msp cache stats` - View cache statistics (requires `mmospawnpoint.cache.stats`)
* `/msp cache clear [player]` - Clear cache (requires `mmospawnpoint.cache.clear`)
* `/msp spawnpoint set [player] [world] [x] [y] [z] [yaw] [pitch] [--if-has|--if-missing] [--only-if-incorrect] [--require-valid-bed] [--dry-run]` — Set bed/anchor spawn (requires mmospawnpoint.spawnpoint.set)
* `/msp spawnpoint clear [player] [--if-has] [--dry-run]` — Clear bed/anchor spawn (requires mmospawnpoint.spawnpoint.clear)
* `/msp spawnpoint teleport [player]`— Teleport to bed/anchor spawn (requires mmospawnpoint.spawnpoint.teleport)
* `/msp spawnpoint show [player]` — Show bed/anchor spawn; includes clickable teleport (requires mmospawnpoint.spawnpoint.show)

Differences from vanilla /spawnpoint and /msp spawnpoint commands:

* Works consistently on 1.16.5+ with conditional flags (--if-has, --if-missing, --only-if-incorrect, --require-valid-bed, --dry-run)
* Automatically loads destination chunk at async before teleport
* Pitch support: vanilla /spawnpoint gained per-dimension worldspawn pitch only in 1.21.9+; with MSP you can store and report yaw/pitch in legacy; however, note that vanilla bed/anchor respawn may ignore yaw/pitch — MSP can adjust orientation on final teleport (when MSP handles respawn), but not on pure vanilla respawn

### All Available Permissions

```text
# Basic Access
mmospawnpoint.command                    # Basic command access
mmospawnpoint.reload                    # Reload configurations

# Party System
mmospawnpoint.party                      # Basic party commands
mmospawnpoint.party.invite              # Invite players to party
mmospawnpoint.party.accept              # Accept party invitations
mmospawnpoint.party.deny                # Decline party invitations
mmospawnpoint.party.leave               # Leave current party
mmospawnpoint.party.list                # List party members
mmospawnpoint.party.remove              # Remove players from party
mmospawnpoint.party.setleader           # Transfer party leadership
mmospawnpoint.party.options             # View/change party options
mmospawnpoint.party.options.mode        # Change party respawn mode
mmospawnpoint.party.options.target      # Set party respawn target

# Advanced Party Features
mmospawnpoint.party.deathLocationSpawn  # Walking spawn point ability

# Admin Tools
mmospawnpoint.simulate                   # Access simulation tools
mmospawnpoint.simulate.death            # Simulate death respawn
mmospawnpoint.simulate.join             # Simulate join teleport
mmospawnpoint.simulate.back             # Return to pre-simulation location
mmospawnpoint.simulate.others           # Simulate for other players
mmospawnpoint.cache                      # Access cache tools
mmospawnpoint.cache.stats               # View cache statistics
mmospawnpoint.cache.clear               # Clear cache
mmospawnpoint.spawnpoint                 # Base node for /msp spawnpoint
mmospawnpoint.spawnpoint.set            # Set bed/anchor spawn
mmospawnpoint.spawnpoint.clear          # Clear bed/anchor spawn
mmospawnpoint.spawnpoint.teleport       # Teleport to bed/anchor spawn
mmospawnpoint.spawnpoint.show           # Show bed/anchor spawn

# Bypass Permissions
mmospawnpoint.bypass.party.cooldown                    # Bypass party respawn cooldown
mmospawnpoint.bypass.party.restrictions.death          # Bypass death location restrictions
mmospawnpoint.bypass.party.restrictions.target         # Bypass target location restrictions
mmospawnpoint.bypass.party.restrictions.both           # Bypass both location restrictions
mmospawnpoint.bypass.party.walking.restrictions        # Bypass walking spawn restrictions
```

## 🤖 AI-assisted configuration (optional)

If you plan to use an AI assistant (e.g., DeepSeek, Grok, etc.) to speed up and structure your spawn rules, we strongly recommend feeding it these three files first (as context):

* Your config.yml
* README (plain): <https://github.com/alex2276564/MMOSpawnPoint/blob/main/README.md?plain=1>
* Examples (plain): <https://github.com/alex2276564/MMOSpawnPoint/blob/main/src/main/resources/spawnpoints/examples.txt>

Once the AI has this context, you can ask it to draft spawnpoints tailored to your worlds/regions:

* Tell it your world names, region names, and event types (death/join/both).
* Prefer explicit rules over regex unless truly necessary.
* Ask it to include: priority, requireSafe, waitingRoom, rects/excludeRects, weightConditions (permission/placeholder), and actions with phases (BEFORE/WAITING_ROOM/AFTER).
* Always validate the result:
    * YAML: [yamllint.com](http://www.yamllint.com/)
    * In-game: /msp simulate death and /msp simulate join
* Keep a low-priority global fallback only if it's needed to catch edge cases.

## 🏗️ Why Traditional Spawn Plugins Are Outdated

### The Core Problem: Replace vs Extend

**Good architecture:** New code layers **on top** of existing systems, not replaces them.

**MSP approach:** Respects and extends vanilla mechanics.  
**CMI/Essentials/SetSpawn approach:** Overrides vanilla entirely.

This isn't just technical—it's a fundamental design philosophy difference.

---

### The Issues

**MSP respects and extends vanilla mechanics.** Traditional plugins override them completely.

These plugins are so outdated that **even vanilla Minecraft 1.21.9+ can do more**—and that's not even mentioning other problems:

**Why handle firstjoin at all?** This should be vanilla's job (`/setworldspawn`) or a world manager like Multiverse-Core.

**Why globally override death respawn?** Vanilla + world managers already work correctly—even with per-dimension spawns on modern versions (and where this is not enough, solutions such as MSP are usually already needed).

**Why should firstjoin and death spawns even be the same location?** Different events need different logic—forcing them to share one spawn point is a design limitation, not a feature.

**And many other questions:** Plugin compatibility issues? Gameplay problems? Config readability? Manual plugin integrations? Scalability? The list goes on.

---

### Why Is It Like This?

You might wonder: *"Why are things so bad?"*

We wonder too. We can only guess at the reasons:

- Support for very old Minecraft versions
- Support for outdated Spigot/CraftBukkit (not Paper)
- Unwillingness to change API/behavior (too many existing tutorials)
- Unwillingness to rewrite legacy code (+ all those economy/kits/homes integrations)
- Unwillingness to break thousands of existing server setups

In short: **they probably have their reasons** (backward compatibility = prison), and we have ours.

**Which approach to choose for your server?** That's up to you.

## 🔄 How It Works

Are you still using spawn points from CMI or EssentialsX? Their spawn point systems were designed over a decade ago and haven't evolved much since. Isn't it time for something better?

Are you tired of players always respawning at the server spawn or their bed after death? Don't you think this mechanic is outdated and limiting for modern Minecraft servers?

MMOSpawnPoint revolutionizes player respawning with features like:

### 🏆 Essential for Professional Servers

**Servers like Wynncraft and other professional MMO servers couldn't exist without advanced spawn systems like MMOSpawnPoint.** Creating immersive adventure maps, RPG worlds, or complex dungeon systems is virtually impossible without proper spawn point management.

### 🗺️ Map Creators' Paradise

**For map creators, MMOSpawnPoint is absolutely essential** - without it, creating a full MMORPG experience is simply not possible. This plugin is a **must-have tool** for any serious map creator because:

* **Checkpoint Systems:** Create sophisticated checkpoint-based progression through your adventure maps
* **Story Integration:** Respawn players at narrative-appropriate locations to maintain immersion
* **Difficulty Zones:** Design areas with different respawn mechanics based on challenge level
* **Quest Flow Control:** Prevent players from breaking quest sequences by respawning in wrong locations
* **Professional Quality:** Match the spawn mechanics found in commercial MMORPGs and professional servers

**Without MMOSpawnPoint, your maps will feel amateur and break player immersion.** Professional map creators consider spawn point management as critical as terrain design and quest scripting.

### 🌍 Region-Based Respawning

* Send players who die in a PvP arena directly to a spectator area
* Teleport players who die in a dungeon to a recovery zone with healing effects
* Make players who die in the wilderness respawn at random locations for added challenge
* Support for regex pattern matching for complex region naming schemes

### 📐 Coordinate-Based Precision

* Define exact trigger areas with flexible X/Y/Z constraints
* Perfect for precise dungeon entrances, boss rooms, or special zones
* Advanced axis specification with ranges or fixed values
* Omit Y-axis for multi-floor compatibility

### 🎲 Advanced Weighted Random Spawns

* Create multiple possible spawn points with different probabilities
* Adjust spawn chances based on player permissions or PlaceholderAPI conditions
* Give VIP players better spawn locations with higher probability
* Dynamic weight calculation based on real-time conditions

### 👥 Professional Party System

* Allow players to form groups and respawn together with advanced mechanics
* Configurable cooldowns, distance restrictions, and bypass permissions
* Multiple respawn target strategies (closest, most populated, leader priority)
* Perfect for adventure maps, dungeons, and RPG servers
* Walking Spawn Point feature for content creators and streamers

### 🎬 Content Creator Features

* Give your YouTubers and streamers the ability to create spawn point parties
* When they die, they respawn at their death location and act as spawn points for their followers
* Creates amazing opportunities for content creation and community engagement
* Advanced restriction controls for balanced gameplay

### ⚡ Batched Safe Location Finding

* Advanced caching system with configurable expiry and size limits
* Professional waiting room system prevents lag during location searches
* Multiple search strategies for different environments (Nether, End, Overworld)
* Player-specific and global cache options for optimal performance

### 🎮 Enhanced Gameplay Experiences

* Send players to the Nether as punishment for dying in certain areas
* Create hardcore-like experiences where players respawn in completely random locations
* Design custom respawn experiences for different player ranks or achievements
* Resource pack integration with waiting room support during loading

### 🔰 Suggested Permission Structure

#### Basic Players (Default)

```text
mmospawnpoint.command
mmospawnpoint.party.accept
mmospawnpoint.party.deny
mmospawnpoint.party.leave
mmospawnpoint.party.list
```

#### VIP Players (Paid Rank)

```text
mmospawnpoint.party.invite
mmospawnpoint.party.remove
mmospawnpoint.party.setleader
mmospawnpoint.party.options
```

#### Content Creators/Premium (Top Tier)

```text
mmospawnpoint.party.deathLocationSpawn
mmospawnpoint.bypass.party.cooldown
```

#### Administrators

```text
mmospawnpoint.reload
mmospawnpoint.simulate.*
mmospawnpoint.cache.*
mmospawnpoint.spawnpoint.*
```

### 💰 Monetization & Player Progression

This tiered permission system creates progression and incentives for player ranks, enhancing both gameplay and monetization opportunities for your server. Players will have a reason to upgrade their rank to access more advanced party features and spawn benefits.

You can also configure special spawn points or weighted chances in your config based on these permissions:

```yaml
# Example of VIP-only spawn point
spawns:
  - kind: world
    event: death
    world: world
    conditions:
      permissions:
        - "mmospawnpoint.vip"
    destinations:
      - world: world
        x: 100
        y: 64
        z: 100
```

```yaml
# Example of weighted chances for premium players
spawns:
  - kind: region
    event: death
    region: premium_area
    destinations:
      - world: world
        requireSafe: true
        x:
          min: 50
          max: 100
        z:
          min: 50
          max: 100
        weight: 50
        weightConditions:
          - type: permission
            value: "mmospawnpoint.premium"
            weight: 100  # 100% chance for premium players
```

## ⚠️ Important Notes

### Configuration Safety for Map Makers

**⚠️ WARNING: Do not modify default configuration values if you install community maps!** Many map creators design their spawn systems around MMOSpawnPoint's default settings:

* **Default Priorities:** Map creators often rely on default priority values (coordinate: 100, region: 50, world: 10) when designing their spawn systems
* **Y-Selection Strategy:** Maps may be designed around the default "mixed" mode with "highest" first strategy
* **Cache Settings:** Default cache behavior is optimized for most use cases

**Safe approach:** Only modify default values if you fully understand how they might affect installed maps. Consider creating separate configuration files for different map areas instead of changing global defaults.

### Vanilla vs MMOSpawnPoint Respawn Mechanics

**Vanilla Minecraft** follows a simple "last used spawn point" logic. The player has only one active spawn point at any time - either a bed or a respawn anchor, whichever was used most recently.

**How Vanilla Respawn Works:**

**Versions 1.16.5 - 1.21.8:**

1. **Active spawn point priority**: The game uses whichever spawn point the player set most recently:
    * If a **respawn anchor** is active (has charges and placed in the Nether), the player respawns there regardless of where they died (Overworld, Nether, or End). Each respawn consumes one charge.
    * If a **bed** is active (placed in the Overworld), the player respawns there regardless of where they died.

2. **Fallback to world spawn**: If the active spawn point is unavailable (anchor destroyed/uncharged/obstructed, or bed broken/obstructed), the player respawns at the **Overworld world spawn** (coordinates set by `/setworldspawn` or near 0,0).

3. **End portal exception**: Exiting through an End portal teleports the player to their active spawn point without consuming anchor charges.

**Versions 1.21.9 and above:**

The logic remains the same, except for the fallback behavior:

* **Fallback to world spawn**: If the active spawn point is unavailable, the player respawns at the **current dimension's world spawn** (if set via `/setworldspawn` in that dimension), otherwise falls back to the Overworld world spawn.
* This means players can now respawn in the Nether, End, or custom dimensions if a world spawn is set there.

**Important notes:**

* Respawn anchors only function in the Nether dimension
* Beds only function as spawn points in the Overworld dimension
* Anchors explode if used in the Overworld or End (when trying to set spawn or charge them)
* There's no "backup" system - if your active spawn point fails, you go to world spawn, not to an old bed/anchor

---

**MMOSpawnPoint** (priority-based logic) works completely differently:

* 👥 Party first (if enabled)
    * If the party system is enabled and conditions are met, the player can be teleported to a party member before any spawn rules are checked.

* 🧠 Priority decides everything next
    * The plugin collects all spawn entries that match the current event (death/join/both), the player's location, and your conditions (permissions/placeholders).
    * It then sorts those entries by priority (highest → lowest) and uses the first one that matches.
    * There is no hard-coded "type order." Only priority matters.

* ⚙️ Default priorities (from config.yml → settings.defaultPriorities)
    * coordinate: 100
    * region: 50
    * world: 10

By default this "feels like": coordinate (100) → region (50) → world (10). You can change these values or set explicit priorities per entry.

* 🎯 Destination selection (inside a matched entry)
    * If there is only one destination, it's used.
    * If there are multiple, weights (and weightConditions) decide which one is picked.
    * If requireSafe: true, a waiting room is used while the plugin searches for a safe spot.

* 🧭 Easy flow (at a glance)

Player dies/joins
→ 👥 Party (if enabled)
→ 🔍 Sort matching entries by priority (high → low)
→ ✅ First entry that matches event + location + conditions
→ 🎯 Pick destination (weights) → 🚀 Teleport (waiting room if requireSafe)
→ ❌ If nothing matched → Vanilla respawn

* 🔧 Want "regions first" (or any other order)?
    * Just give region entries higher priorities than coordinate/world.
    * Example:
        * region rules: 800+
        * coordinate rules: 300–700
        * world rules: 10–200

* 🛟 Fallback
    * If no MMOSpawnPoint entries match, the game falls back to Vanilla behavior (anchor/bed/world spawn).

### Triggering MSP without death/join events

By design, MMOSpawnPoint does not implement arbitrary "teleport me now" entry points beyond death/join events. However, you can integrate MSP with portals or NPC scripts as a workaround:

* Create a portal with AdvancedPortals (or similar).
* Configure MSP to target the portal's location in a coordinate-based rule (e.g., event: death for that area).
* In the portal, execute: /msp simulate death [player] (or run the command via an NPC/trigger system).
* The player will be processed by MSP as if they had died in the configured triggerArea, and teleported according to your MSP rules.

This lets you reuse all MSP features (party, waiting room, safe search, weights) for custom flows such as portals, checkpoints, scripted events, etc.

### Safe Location Finding & Performance Optimization

The `requireSafe` option should be set to `false` for known safe locations to improve performance. Only enable it when spawning in potentially dangerous areas. The plugin includes advanced Y-selection strategies that you should optimize for your server type:

**Y-Selection Strategy Configuration** (config.yml → settings.teleport.ySelection):

Dimension-aware global policy (can be overridden per destination):

* Overworld:
    * mode: mixed | highest_only | random_only
    * first: highest | random (for mixed)
    * firstShare: 0.0..1.0
* Nether:
    * mode: scan | highest_only | random_only
    * respectRange: true|false (for scan: whether to limit the scan to the destination Y-range)
* End:
    * mode: mixed | highest_only | random_only (highest_only recommended)

Guidance:

* Survival/Towny (Overworld): mixed with first=highest (surface-first)
* Dungeon/RPG (Overworld): mixed with first=random (vertical variation)
* Nether (generic): scan (deterministic "solid + 2 air" search) — recommended default
* Nether (hand-made vertical content): per-destination ySelection override with random_only and an explicit Y range
* End: highest_only or mixed(first=highest)

Per-destination override (spawnpoints/*.yml → destinations[].ySelection):

* mode: mixed | highest_only | random_only | scan (Nether only)
* first/firstShare: for mixed only
* respectRange: for Nether scan; also useful for documentation with random_only when you rely on explicit ranges

What you need to know (Near vs Range behavior in the Nether):

* RespectRange applies to area searches (rects) in Nether when using scan. Fixed-point near searches do not carry a Y range; they always use world bounds for scan.
* If you need to constrain Nether near spawns to a certain altitude, consider area-based destinations (rects with y range) or use groundWhitelist to exclude undesired surfaces (e.g., BEDROCK).

About cache "near" searches:

* For fixed-point requireSafe=true searches (near X/Z), the cache key does not include the radius and also ignores the base Y. This increases cache hit rate across repeated lookups around the same X/Z.
* The effective Y-selection signature (dimension-aware policy) and groundWhitelist hash are still included in the cache key to prevent incorrect reuse across different spawn configurations.

**Region Entry vs Respawn:**

* MMOSpawnPoint only handles player respawning after death, not entry into regions or worlds. For region entry commands, use WorldGuard flags like `entry-command` or `entry-deny`.

**This plugin uses a batched safe-search approach**:

* Paper: N attempts per tick on the main thread within a configurable time budget. The search is spread across ticks (partial async feeling, no big stalls).
* Folia: exactly one attempt per tick scheduled on the correct region thread (true region-aware parallelism).
* Teleport to the waiting room happens immediately (if requireSafe=true), while the actual safe spot is being searched in the background.

### Party self-invite

The plugin allows inviting yourself. This is intentional: it lets a single player form a party quickly (e.g., to use the walking spawn point feature) without logging a second account.

### Waiting Room Design Considerations

The waiting room feature is not just a temporary holding area - it's a fallback spawn location where players might remain if:

* A safe location cannot be found within the configured timeout period
* The server restarts during the location search
* An error occurs during the teleportation process

⚠️ **Don't Trap Players!** For this reason, waiting rooms should be:

* **Fully functional areas**: Players should be able to move around and interact
* **Escape-enabled**: Include a way out (NPCs, pressure plates, portals, commands)
* **Strategically located**: Consider placing them near cities, markets, or hubs
* **Properly protected**: Use WorldGuard to prevent griefing or damage
* **Well-designed**: Include basic amenities and clear signage explaining the situation

🏙️ **Themed Integration**: On RPG servers, consider creating themed "recovery zones" that fit your lore while serving as waiting rooms. For example, a temple of healing, a traveler's respite, or a dimensional nexus.

**⏱️ Minimum Stay Time Configuration:** You can configure `settings.waitingRoom.minStayTicks` in config.yml to control how long players must stay in the waiting room. If you've created an elaborate waiting room experience, consider increasing this value so players can appreciate your design. However, **don't set it too high** - the longer players stay in waiting rooms, the higher the chance they'll disconnect or lose patience. Recommended range: 20-100 ticks (1-5 seconds).

> If you're using MMOSpawnPoint on a large server with many regions or factions, it's a good idea to theme each waiting room to match the area the player died in.

### Walking Spawn Point Safety

**⚠️ DANGER ZONES:** Walking Spawn Points can be dangerous if content creators die in inescapable locations (lava pits, void areas, enclosed spaces, etc.).

**Solution:** Instruct players with `mmospawnpoint.party.deathLocationSpawn` permission to temporarily switch their party mode when exploring dangerous areas:

```text
/msp party options mode normal    (before entering dangerous areas)
/msp party options mode party_member    (after leaving dangerous areas)
```

This prevents party members from being teleported to death traps while still allowing the walking spawn point feature in safe areas.

### Rects vs legacy axes: when to use and how to keep configs sane

* Prefer legacy axes for simple shapes:
    * Use x/y/z axis specs whenever a single rectangle is enough. It keeps files shorter, easier to read, and easier to reason about.
* Don't glue far-apart areas into one entry:
    * Two distant boss rooms? Don't hack them into one entry via rects. Make one entry per room (using legacy axes), each with its own priority and actions.
* The more rects, the harder the debugging:
    * Complex rect lists increase mental load and the chance of mistakes. Keep rect counts low; split logic into multiple entries if needed.
* Use single-line rects for readability:
    * x: { min: 1000, max: 2000 } is much easier to scan in large YAMLs than expanded multi-line fields.
* Keep rects close to each other:
    * If rects are far apart (different wings/levels), it's usually a sign you need separate entries.
* ASCII diagrams help humans:
    * For non-trivial shapes, include an ASCII map at the top of the file with a legend and coordinate ticks.
    * If you iterate a lot, ask your AI assistant to redraw the diagram whenever rects change. Not mandatory for simple cases, but very helpful for complex layouts.
* Test and iterate:
    * Always include a low-priority fallback and validate with /msp simulate death/join.
    * Keep a "boss room" entry with a higher priority than the surrounding area (or disable party respawn there).

Example pyramid.yml (diagram at the top and 2 entries)

**Note:** For different types of locations, you can choose different ways of drawing the diagram (usually it is best to do it however you find most convenient). Below is an example of one such method, and there will be more in examples.txt and starter folder.

File: spawnpoints/world/dungeons/pyramid.yml

```yaml
# ================================================================
# Pyramid (top-down)
# Legend:
#   # = playable outer ring (hallways)
#   B = boss room (center)
#   . = outside / not part of this file
#
# Axes:
#   X+ → right,  Z+ → down
#
# Coords (example):
#   Whole pyramid: X=200..300, Z=200..300
#   Boss room:     X=240..260, Z=240..260
#
#  Z=200     210      220      230      240      250      260      270      280      290      300
#    ┌──────────────────────────────────────────────────────────────────────────────────────────┐
# 200 │#############################    OUTER RING (PLAYABLE)    #############################│
# 210 │############################# ################################ ########################│
# 220 │############################# ############################ ###########################│
# 230 │############################# ##############    ########## ###########################│
# 240 │############################# ###########  BBBB  ######### ###########################│
# 250 │############################# ###########  BBBB  ######### ###########################│
# 260 │############################# ###########  BBBB  ######### ###########################│
# 270 │############################# ##############    ########## ###########################│
# 280 │############################# ############################ ###########################│
# 290 │############################# ################################ ########################│
# 300 │#############################    OUTER RING (PLAYABLE)    #############################│
#    └──────────────────────────────────────────────────────────────────────────────────────────┘
#                  X=200                       240      260                       300
#
# Notes:
# - Entry #1 (boss room) has higher priority and can disable party respawns.
# - Entry #2 (outer ring) covers everything else (four rects as a frame).
# - Y-range is clamped (60..80) to work with vertical dungeons.
# ================================================================

spawns:
  # ------------------------------------------------
  # Entry #1: BOSS ROOM (center, high priority)
  # ------------------------------------------------
  - kind: coordinate
    event: death
    priority: 1800
    triggerArea:
      world: world
      rects:
        - x: { min: 240, max: 260 }
          z: { min: 240, max: 260 }
          y: { min: 60, max: 80 }  # vertical dungeon slice
    destinations:
      - world: world
        requireSafe: true
        rects:
          - x: { min: 240, max: 260 }
            z: { min: 240, max: 260 }
            y: { min: 60, max: 80 }
        groundWhitelist:
          - STONE
          - CHISELED_STONE_BRICKS
          - POLISHED_ANDESITE
        actions:
          messages:
            - text: "<gray>Boss room respawn (party respawn disabled)"
              phases:
                - BEFORE
            - text: "<yellow>⏳ Searching a safe spot inside the boss room..."
              phases:
                - WAITING_ROOM
    party:
      respawnDisabled: true

  # ------------------------------------------------
  # Entry #2: OUTER RING (frame around the boss room)
  # Four rects: top, bottom, left, right — form the ring
  # ------------------------------------------------
  - kind: coordinate
    event: death
    priority: 1600
    triggerArea:
      world: world
      rects:
        - x: { min: 200, max: 300 }
          z: { min: 200, max: 300 }
          # y omitted → any height triggers this entry
    destinations:
      - world: world
        requireSafe: true
        rects:
          # top
          - x: { min: 200, max: 300 }
            z: { min: 200, max: 240 }
            y: { min: 60, max: 80 }
          # bottom
          - x: { min: 200, max: 300 }
            z: { min: 260, max: 300 }
            y: { min: 60, max: 80 }
          # left
          - x: { min: 200, max: 240 }
            z: { min: 240, max: 260 }
            y: { min: 60, max: 80 }
          # right
          - x: { min: 260, max: 300 }
            z: { min: 240, max: 260 }
            y: { min: 60, max: 80 }
        groundWhitelist:
          - STONE
          - STONE_BRICKS
          - POLISHED_ANDESITE
        actions:
          messages:
            - text: "<gray>Outer ring respawn"
              phases:
                - BEFORE
            - text: "<yellow>⏳ Finding a safe spot inside the ring..."
              phases:
                - WAITING_ROOM
```

### Placeholder expression case sensitivity

* PlaceholderUtils compares strings case-sensitively.
* Example: "%player_gamemode% == 'SURVIVAL'" works, while "%player_gamemode% == 'Survival'" will not.
* Recommendation: normalize your placeholder values or use consistent upper-case in expressions.

### OP and "*" bypass for permission conditions

* For conditions.permissions, operators (server OP) or players with the wildcard permission "*" bypass permission checks by design.
* This is intended for admins and map makers. If you need strict checks, do not give OP/"*" to players who must not bypass spawn conditions.

### Compatibility with Other Plugins

MMOSpawnPoint can potentially work alongside respawn handling from CMI or EssentialsX if properly configured, but it's recommended to disable their respawn handling for the best experience:

* **For EssentialsX:** Set `respawn-at-home: false` in the essentials config.yml
* **For CMI:** Set `respawn.enabled: false` in the CMI config.yml

**Multiverse-Core Integration:** MMOSpawnPoint is fully compatible with Multiverse-Core's `firstspawnoverride` feature. The plugins work together without conflicts, and you can:

1. **Keep both enabled:** Use Multiverse for first-join spawns and MMOSpawnPoint for death/subsequent join spawns
2. **Replace firstspawnoverride:** If you want MMOSpawnPoint to handle first-join spawns, you can disable `firstspawnoverride` and create a coordinate-based trigger area where new players spawn:

```yaml
spawns:
  - kind: coordinate
    event: join
    priority: 1500
    triggerArea:
      world: world
      x:
        min: -5
        max: 5
      z:
        min: -5
        max: 5
      y:
        min: 60
        max: 70
    destinations:
      - world: hub_world
        x: 100
        y: 64
        z: 100
```

**Note:** If using this approach, ensure players can't repeatedly trigger the join spawn by returning to that area, unless your actions are safe to execute multiple times.

**Important Note on First-Join Handling:** MMOSpawnPoint does not include a default firstjoin listener—only a standard join listener. If you need to handle first-join spawns (e.g., for new players), it's recommended to configure this separately through your world manager, such as Multiverse-Core, or by setting the world spawn point (e.g., via `/setworldspawnpoint` in the world where players appear). This approach is more appropriate and avoids potential conflicts, as MSP focuses on death and subsequent join spawns. Always ensure that your firstjoin setup doesn't interfere with MSP's functionality.

### Dungeon Plugin Compatibility

**For MythicDungeons v2** (which creates worlds like `MythicDungeonWorld_0`, `MythicDungeonWorld_1`, etc.), disable party respawn with regex matching:

```yaml
spawns:
  - kind: world
    event: death
    priority: 2000  # High priority to override other rules
    world: "MythicDungeonWorld_.*"
    worldMatchMode: regex
    destinations: []  # Actions only, no teleport
    party:
      respawnDisabled: true
```

**For region-based dungeon plugins**, prefer coordinate-based matching with rects and excludeRects:

Example: one playable "dungeon courtyard" area, but excluding a boss room; then a second explicit entry that marks the boss room as "no party respawn".

ASCII map (top view, X horizontal, Z vertical):

* █ = included rects
* ░ = excluded (carved out)

```text
            Z+
        1000 ┌───────────────────────────┐
             │███████████████████████████│  ← include: rect #1 (1000..1160 x 1000..1160)
             │███████████████████████████│
             │███████████████████████████│
             │███████████████████████████│
        1160 └───────────────────────────┘
                      ┌───────────────┐
                      │███████████████│  ← include: rect #2 (1080..1200 x 1040..1160)
                      │███████████████│
                      │███████████████│
                      └───────────────┘
                                ┌───────┐
                                │░░░░░░░│  ← exclude: boss room (1160..1200 x 1160..1200)
                                │░░░░░░░│
                                └───────┘
                 X+         1000             1160             1200
```

Note: Legacy x/y/z axes on destinations or triggerAreas are internally mapped to a single rect at runtime. You can keep writing simple xyz if you want; under the hood it becomes a rect.

1. Playable area with an excluded boss room

```yaml
spawns:
  - kind: coordinate
    event: death
    priority: 1800
    triggerArea:
      world: dungeon_world
      # You may use legacy axes here – internally they become one rect:
      x: { min: 950, max: 1250 }
      z: { min: 950, max: 1250 }
      # y omitted → any height
      
    destinations:
      - world: dungeon_world
        requireSafe: true
        rects:
          - x: { min: 1000, max: 1160 }
            z: { min: 1000, max: 1160 }
            y: { min: 60, max: 90 }
          - x: { min: 1080, max: 1200 }
            z: { min: 1040, max: 1160 }
            y: { min: 60, max: 90 }
        excludeRects:
          - x: { min: 1160, max: 1200 }
            z: { min: 1160, max: 1200 }
            y: { min: 60, max: 90 }
        weight: 100
        actions:
          messages:
            - text: "<gray>Dungeon courtyard respawn"
              phases:
                - BEFORE
```

1. Boss room marked as "party respawn disabled" (no teleport, actions only)

```yaml
spawns:
  - kind: coordinate
    event: death
    priority: 2000  # High to override general rules
    triggerArea:
      world: dungeon_world
      rects:
        - x: { min: 1160, max: 1200 }
          z: { min: 1160, max: 1200 }
          y: { min: 60, max: 90 }
    destinations: []   # no teleport, actions only
    actions:
      messages:
        - text: "<red>Party respawn is disabled in this boss room."
          phases:
            - BEFORE
    party:
      respawnDisabled: true
```

This pattern easily extends to any event arenas, minigame areas, temporary zones, etc.

**Note:** You can also simply exclude an area if not need disable party respawn and not define any MSP rule for it at all. In that case:

* Either the world-level MSP fallback (if any) will catch the player,
* Or vanilla respawn will apply,
* Or the dungeon/minigames plugin you use might handle the respawn itself.
  This is often the safest choice when other plugins own the death/join flow for that area.

### Regex Pattern Matching: When to Use and When to Avoid

**⚠️ Generally NOT Recommended:** While MMOSpawnPoint supports regex pattern matching for regions and worlds, **avoid using it in most cases** as it can significantly complicate debugging and priority management:

```yaml
# ❌ AVOID: Hard to debug, unclear priorities
spawns:
  - kind: region
    region: "shop_.*"   # Matches shop_weapons, shop_armor, shop_food
    # Which shop has higher priority? Unclear!
```

Also, if your players can create WG regions, avoid regex matches that might include player-created names. Use explicit region names or restrict regex-based entries to admin-managed worlds. Similarly for world patterns (rare case).

**✅ Recommended Approach:** Use separate entries for better control:

```yaml
# ✅ BETTER: Clear priorities, easy debugging
spawns:
  - kind: region
    priority: 100
    region: shop_weapons
    regionWorld: spawn     # restrict entries to "spawn" world
    destinations: [...]

  - kind: region
    priority: 90
    region: shop_armor
    regionWorld: spawn     # restrict entries to "spawn" world
    destinations: [...]
```

**🎯 Acceptable Regex Use Cases:**

1. **Identical PvP Arenas:** When all areas need identical spawn behavior

```yaml
spawns:
  - kind: region
    region: "pvparena_.*"  # pvparena_1, pvparena_2, etc.
    regionMatchMode: regex
    regionWorld: spawn     # restrict regex-based entries to admin-managed worlds
    destinations: []       # Disable party spawns in all arenas
    party:
      respawnDisabled: true
```

1. **Dynamic Dungeon Worlds:** Essential for plugins like MythicDungeons

```yaml
spawns:
  - kind: world
    world: "MythicDungeonWorld_.*"
    worldMatchMode: regex
    destinations: []
    party:
      respawnDisabled: true
```

1. **Temporary Event Areas:** When you have many similar temporary regions

```yaml
spawns:
  - kind: region
    region: "event_christmas_.*"
    regionMatchMode: regex
    regionWorld: event_christmas # restrict regex-based entries to admin-managed worlds
    destinations:
      - world: hub
        x: 0
        y: 64
        z: 0
```

**💡 Reducing Configuration Boilerplate:**

Instead of regex, use **YAML Anchors** for DRY (Don't Repeat Yourself) configuration:

```yaml
# Define reusable configuration blocks
pvp_config: &pvp_settings
  regionWorld: spawn     # restrict entries to "spawn" world
  destinations: []
  party:
      respawnDisabled: true
  actions:
    - command: "title %player% times 10 70 20"
    - command: "title %player% title \"<red>You Died!\""

spawns:
  - kind: region
    region: pvparena_1
    <<: *pvp_settings    # Reuse the configuration
    
  - kind: region
    region: pvparena_2
    <<: *pvp_settings    # Same configuration, clear priorities
    
  - kind: region
    region: battleground_main
    <<: *pvp_settings    # Consistent behavior across PvP areas
```

**🔍 Debugging Tip:** When troubleshooting spawn issues, regex patterns make it much harder to identify which specific region or world triggered a spawn rule. Always prefer explicit configuration when possible.

### Modern Text Formatting

**Native MiniMessage Support:** Plugin uses only native Kyori Adventure MiniMessage implementation without any backporting or compatibility layers:

* **Paper 1.18+:** Full native MiniMessage support with all features including gradients, hover effects, click events, and advanced formatting
* **Paper 1.16-1.17:** Partial support with automatic conversion to legacy ChatColor codes. Supported features include basic colors (`<red>`, `<blue>`, etc.), text styles (`<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`), and reset tags (`<reset>`). Advanced features like gradients and hover effects are automatically stripped without causing errors.

You can use the [MiniMessage Web Editor](https://webui.advntr.dev/) to test and preview your formatting. The plugin will automatically adapt the formatting to your server's capabilities, so you can use the same configuration across different server versions.

> Note: On Paper 1.16–1.17 there is no perfect way to get every MiniMessage feature without adding extra, version‑sensitive libraries. This plugin intentionally does **not** use full Adventure backports such as `BukkitAudiences` (they require constant updates and can conflict with complex plugins like ViaVersion). Instead, legacy servers get a simple MiniMessage‑like formatter (colors/styles only), while modern servers use the native Paper Adventure stack with full features.

## 🛠️ Troubleshooting

If you encounter issues with the plugin:

1. **Check your configuration:** Validate your YAML syntax using [YAMLLint](http://www.yamllint.com/)
2. **Enable debug modes:**
    * Set `settings.debugMode: true` in config.yml for detailed logs
    * Set `settings.safeLocationCache.advanced.debugCache: true` for cache debugging
3. **Use simulation tools:** Test your spawn points with `/msp simulate death` and `/msp simulate join`
4. **Check cache performance:** Monitor cache statistics with `/msp cache stats`
5. **Verify dependencies:** Make sure you have the correct versions of WorldGuard and PlaceholderAPI if using those features
6. **Check for conflicts:** Ensure no other plugins are handling respawn events
7. **Plugin conflicts:** Disable party respawn in worlds managed by dungeon plugins (MythicDungeons, DungeonsXL, etc.)

### Paper 1.21.9+ warning about PlayerSpawnLocationEvent

On Paper 1.21.9+ you will see a warning like:

> "MMOSpawnPoint has registered a listener for PlayerSpawnLocationEvent ...  
> Listening to this event causes the player to be created early.  
> Prefer AsyncPlayerSpawnLocationEvent."

MSP intentionally uses `PlayerSpawnLocationEvent` to provide **flicker-free join spawns**
while still supporting complex logic (regions/worlds, PlaceholderAPI, batched safe search, etc.).

The recommended `AsyncPlayerSpawnLocationEvent` cannot safely perform this kind of world-dependent
logic yet — it is strictly asynchronous and forbids nearly all Bukkit world access.

For this reason, MSP currently treats `PlayerSpawnLocationEvent` as a *legacy but still required*
entry point for join spawns. You can safely ignore this warning for now; once a realistic
replacement exists, MSP will migrate off this event in a future major version.

## 🛠️ Compatibility

* **Minecraft Versions:** 1.16.5 to the latest release
* **Server Software:**
    * ✅ [Paper](https://papermc.io/) (1.16.5 and newer) - **Fully Supported**
    * ⚠️ [Folia](https://papermc.io/software/folia) - **Partially Supported** with optimized region-aware scheduling
    * ❌ Spigot - Not supported
* **Java Version:** Java 17 or higher
* **Optional Dependencies:**
    * WorldGuard 7.0.5+ (for region-based spawns)
    * PlaceholderAPI latest version (for condition-based spawns)

## 📦 Other Plugins

> 🔍 **You can find more of my Minecraft plugins here:**  
> [https://github.com/alex2276564?tab=repositories](https://github.com/alex2276564?tab=repositories)

## 🆘 Support

If you encounter any issues or have suggestions for improving the plugin, please create an [issue](https://github.com/alex2276564/MMOSpawnPoint/issues) in this repository.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Authors

**Primary Developer:** [alex2276564](https://github.com/alex2276564)

**LLM Co-Authors:** This plugin was developed with significant assistance from AI language models:

* **Claude (Anthropic)** - Advanced system architecture, complex algorithm implementation, and comprehensive documentation
* **ChatGPT (OpenAI)** - Feature design, code optimization, and configuration systems

Essential for professional servers

*The majority of the plugin's sophisticated features, including the advanced party system, batched safe search (tick-budgeted on Paper, region-thread on Folia) location finding, and comprehensive configuration validation, were implemented through AI-assisted development.*

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/alex2276564/MMOSpawnPoint/issues).

### How to Contribute

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request.

---

**Thank you for using MMOSpawnPoint!** 🏰✨

Essential for professional servers • Required for map creators • The spawn system that powers the best Minecraft experiences
