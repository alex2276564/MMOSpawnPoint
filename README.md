# MMOSpawnPoint 🏰

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5+-brightgreen)](https://papermc.io/software/paper)
[![Java Version](https://img.shields.io/badge/java-17+-orange)](https://adoptium.net/installation/linux/)
[![GitHub Release](https://img.shields.io/github/v/release/alex2276564/MMOSpawnPoint?color=blue)](https://github.com/alex2276564/MMOSpawnPoint/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Text Formatting](https://img.shields.io/badge/Text%20Formatting-🌈%20MiniMessage-ff69b4)](https://docs.advntr.dev/minimessage/)

**MMOSpawnPoint** is an advanced MMO spawn system with region-based spawns, party respawn mechanics, safe location finding, resource pack integration, and comprehensive condition-based teleportation. Features walking spawn points, waiting rooms, partial async location search, advanced caching, and extensive customization for MMO servers.

## ✨ Features

* **Advanced Spawn Types:** Configure fixed, random, weighted random, or safe location search spawn points
* **Region-Based Spawns:** Set different spawn points for different WorldGuard regions with regex pattern matching
* **World-Based Spawns:** Configure unique spawn points for each world with pattern support
* **Coordinate-Based Spawns:** Define precise trigger areas with flexible axis constraints
* **Conditional Spawns:** Use permissions and PlaceholderAPI with full logical expression support
* **Partial asynchronous Safe Location Finding:** Advanced caching system with configurable search strategies
* **Waiting Room System:** Professional partial async processing with customizable waiting areas
* **Advanced Party System:** Complete party mechanics with respawn, cooldowns, restrictions, and target selection
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
- `/msp help` - Show help information (requires `mmospawnpoint.command`)
- `/msp reload [all|config|messages|spawnpoints]` - Reload plugin configuration (requires `mmospawnpoint.reload`)

### Party System Commands
- `/msp party` - Show party help (requires `mmospawnpoint.party`)
- `/msp party invite <player>` - Invite a player to your party (requires `mmospawnpoint.party.invite`)
- `/msp party accept` - Accept a party invitation (requires `mmospawnpoint.party.accept`)
- `/msp party deny` - Decline a party invitation (requires `mmospawnpoint.party.deny`)
- `/msp party leave` - Leave your current party (requires `mmospawnpoint.party.leave`)
- `/msp party list` - List all members in your party (requires `mmospawnpoint.party.list`)
- `/msp party remove <player>` - Remove a player from your party (requires `mmospawnpoint.party.remove`)
- `/msp party setleader <player>` - Transfer party leadership (requires `mmospawnpoint.party.setleader`)
- `/msp party options` - View and change party options (requires `mmospawnpoint.party.options`)
- `/msp party options mode <normal|party_member>` - Change party respawn mode (requires `mmospawnpoint.party.options.mode`)
- `/msp party options target <player>` - Set party respawn target (requires `mmospawnpoint.party.options.target`)

### Admin & Debug Commands
- `/msp setspawnpoint [player] [world] [x] [y] [z] [yaw] [pitch]` - Set vanilla respawn point (requires `mmospawnpoint.setspawnpoint`)
- `/msp simulate` - Show simulation help (requires `mmospawnpoint.simulate`)
- `/msp simulate death [player]` - Simulate death respawn (requires `mmospawnpoint.simulate.death`)
- `/msp simulate join [player]` - Simulate join teleport (requires `mmospawnpoint.simulate.join`)
- `/msp simulate back [player]` - Return to pre-simulation location (requires `mmospawnpoint.simulate.back`)
- `/msp cache` - Show cache help (requires `mmospawnpoint.cache`)
- `/msp cache stats` - View cache statistics (requires `mmospawnpoint.cache.stats`)
- `/msp cache clear [player]` - Clear cache (requires `mmospawnpoint.cache.clear`)

### All Available Permissions

```text
# Basic Access
mmospawnpoint.command                   # Basic command access
mmospawnpoint.reload                    # Reload configurations

# Party System
mmospawnpoint.party                     # Basic party commands
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
mmospawnpoint.party.respawnatdeath      # Walking spawn point ability

# Admin Tools
mmospawnpoint.setspawnpoint             # Set vanilla respawn points
mmospawnpoint.simulate                  # Access simulation tools
mmospawnpoint.simulate.death            # Simulate death respawn
mmospawnpoint.simulate.join             # Simulate join teleport
mmospawnpoint.simulate.back             # Return to pre-simulation location
mmospawnpoint.simulate.others           # Simulate for other players
mmospawnpoint.cache                     # Access cache tools
mmospawnpoint.cache.stats               # View cache statistics
mmospawnpoint.cache.clear               # Clear cache

# Bypass Permissions
mmospawnpoint.bypass.party.cooldown                    # Bypass party respawn cooldown
mmospawnpoint.bypass.party.restrictions.death          # Bypass death location restrictions
mmospawnpoint.bypass.party.restrictions.target         # Bypass target location restrictions
mmospawnpoint.bypass.party.restrictions.both           # Bypass both location restrictions
mmospawnpoint.bypass.party.walking.restrictions        # Bypass walking spawn restrictions
```

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
* Multiple target selection strategies (closest, most populated, leader priority)
* Perfect for adventure maps, dungeons, and RPG servers
* Walking Spawn Point feature for content creators and streamers

### 🎬 Content Creator Features

* Give your YouTubers and streamers the ability to create spawn point parties
* When they die, they respawn at their death location and act as spawn points for their followers
* Creates amazing opportunities for content creation and community engagement
* Advanced restriction controls for balanced gameplay

### ⚡ Partial Asynchronous Safe Location Finding

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
mmospawnpoint.party.respawnatdeath
mmospawnpoint.bypass.party.cooldown
```

#### Administrators
```text
mmospawnpoint.reload
mmospawnpoint.setspawnpoint
mmospawnpoint.simulate.*
mmospawnpoint.cache.*
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

- **Default Priorities:** Map creators often rely on default priority values (coordinate: 100, region: 50, world: 10) when designing their spawn systems
- **Y-Selection Strategy:** Maps may be designed around the default "mixed" mode with "highest" first strategy
- **Cache Settings:** Default cache behavior is optimized for most use cases

**Safe approach:** Only modify default values if you fully understand how they might affect installed maps. Consider creating separate configuration files for different map areas instead of changing global defaults.

### Triggering MSP without death/join events

By design, MMOSpawnPoint does not implement arbitrary “teleport me now” entry points beyond death/join events. However, you can integrate MSP with portals or NPC scripts as a workaround:

- Create a portal with AdvancedPortals (or similar).
- Configure MSP to target the portal’s location in a coordinate-based rule (e.g., event: death for that area).
- In the portal, execute: /msp simulate death <player> (or run the command via an NPC/trigger system).
- The player will be processed by MSP as if they had died in the configured triggerArea, and teleported according to your MSP rules.

This lets you reuse all MSP features (party, waiting room, safe search, weights) for custom flows such as portals, checkpoints, scripted events, etc.

### Safe Location Finding & Performance Optimization

The `requireSafe` option should be set to `false` for known safe locations to improve performance. Only enable it when spawning in potentially dangerous areas. The plugin includes advanced Y-selection strategies that you should optimize for your server type:

**Y-Selection Strategy Configuration** (config.yml → settings.teleport.ySelection):

- **mode: "mixed"** - Combines highest-block and random-Y searches (recommended for most servers)
  - **first: "highest"** - Try surface spawns first, then underground (good for survival servers)
  - **first: "random"** - Try random heights first, then surface (better for dungeon/vertical servers)
  - **firstShare: 0.6** - Fraction of attempts for first strategy (0.6 = 60% first, 40% second)

- **mode: "highest_only"** - Always spawn on surface (best for flat terrain, city servers)
- **mode: "random_only"** - Always use random Y within bounds (best for underground/cave servers)

**Choose based on your server style:**
- **Survival/Towny servers:** Use "mixed" with first="highest" for surface-focused gameplay
- **Dungeon/RPG servers:** Use "mixed" with first="random" for vertical exploration
- **Skyblock/flat worlds:** Use "highest_only" for consistent surface spawning
- **Mining/cave servers:** Use "random_only" for distributed underground spawning

**Region Entry vs Respawn**: 
- MMOSpawnPoint only handles player respawning after death, not entry into regions or worlds. For region entry commands, use WorldGuard flags like `entry-command` or `entry-deny`.

**This plugin uses a batched safe-search approach**:
- Paper: N attempts per tick on the main thread within a configurable time budget. The search is spread across ticks (partial async feeling, no big stalls).
- Folia: exactly one attempt per tick scheduled on the correct region thread (true region-aware parallelism).
- Teleport to the waiting room happens immediately (if requireSafe=true), while the actual safe spot is being searched in the background.

**About cache “near” searches**:
- For fixed-point requireSafe=true searches (near X/Z), the cache key does not include the current radius. If radius expands over time, the cache still reuses the location found with the previous radius. This is a stability/performance optimization.

###  Party self-invite
The plugin allows inviting yourself. This is intentional: it lets a single player form a party quickly (e.g., to use the walking spawn point feature) without logging a second account.

### Waiting Room Design Considerations

The waiting room feature is not just a temporary holding area - it's a fallback spawn location where players might remain if:

- A safe location cannot be found within the configured timeout period
- The server restarts during the location search
- An error occurs during the teleportation process

⚠️ **Don't Trap Players!** For this reason, waiting rooms should be:

- **Fully functional areas**: Players should be able to move around and interact
- **Escape-enabled**: Include a way out (NPCs, pressure plates, portals, commands)
- **Strategically located**: Consider placing them near cities, markets, or hubs
- **Properly protected**: Use WorldGuard to prevent griefing or damage
- **Well-designed**: Include basic amenities and clear signage explaining the situation

🏙️ **Themed Integration**: On RPG servers, consider creating themed "recovery zones" that fit your lore while serving as waiting rooms. For example, a temple of healing, a traveler's respite, or a dimensional nexus.

**⏱️ Minimum Stay Time Configuration:** You can configure `settings.waitingRoom.minStayTicks` in config.yml to control how long players must stay in the waiting room. If you've created an elaborate waiting room experience, consider increasing this value so players can appreciate your design. However, **don't set it too high** - the longer players stay in waiting rooms, the higher the chance they'll disconnect or lose patience. Recommended range: 20-100 ticks (1-5 seconds).

> If you're using MMOSpawnPoint on a large server with many regions or factions, it's a good idea to theme each waiting room to match the area the player died in.

### Walking Spawn Point Safety

**⚠️ DANGER ZONES:** Walking Spawn Points can be dangerous if content creators die in inescapable locations (lava pits, void areas, enclosed spaces, etc.). 

**Solution:** Instruct players with `mmospawnpoint.party.respawnatdeath` permission to temporarily switch their party mode when exploring dangerous areas:

```
/msp party options mode normal    (before entering dangerous areas)
/msp party options mode party_member    (after leaving dangerous areas)
```

This prevents party members from being teleported to death traps while still allowing the walking spawn point feature in safe areas.

### Vanilla vs MMOSpawnPoint Respawn Mechanics

Vanilla Minecraft (no plugins) follows a strict respawn priority:
1. First checks for a valid respawn anchor in the Nether dimension (if properly charged with glowstone)
    - Respawn anchors only work when placed in the Nether dimension
2. Then checks for a valid bed in the Overworld dimension (if not broken or obstructed)
    - Beds only function as spawn points when placed in the Overworld
3. If neither exists or they're obstructed, sends player to the world spawn point (usually near coordinates 0,0 or wherever /setworldspawn was set)
4. In the End dimension, players always return to the Overworld spawn regardless of beds or anchors

MMOSpawnPoint (priority-based logic)
- 👥 Party first (if enabled)
    - If the party system is enabled and conditions are met, the player can be teleported to a party member before any spawn rules are checked.

- 🧠 Priority decides everything next
    - The plugin collects all spawn entries that match the current event (death/join/both), the player’s location, and your conditions (permissions/placeholders).
    - It then sorts those entries by priority (highest → lowest) and uses the first one that matches.
    - There is no hard-coded “type order.” Only priority matters.

- ⚙️ Default priorities (from config.yml → settings.defaultPriorities)
    - coordinate: 100
    - region: 50
    - world: 10
      By default this “feels like”: coordinate (100) → region (50) → world (10). You can change these values or set explicit priorities per entry.

- 🎯 Destination selection (inside a matched entry)
    - If there is only one destination, it’s used.
    - If there are multiple, weights (and weightConditions) decide which one is picked.
    - If requireSafe: true, a waiting room is used while the plugin searches for a safe spot partial asynchronously.

- 🧭 Easy flow (at a glance)
  Player dies/joins  
  → 👥 Party (if enabled)  
  → 🔍 Sort matching entries by priority (high → low)  
  → ✅ First entry that matches event + location + conditions  
  → 🎯 Pick destination (weights) → 🚀 Teleport (waiting room if requireSafe)  
  → ❌ If nothing matched → Vanilla respawn

- 🔧 Want “regions first” (or any other order)?
    - Just give region entries higher priorities than coordinate/world.
    - Example:
        - region rules: 800+
        - coordinate rules: 300–700
        - world rules: 10–200

- 🛟 Fallback
    - If no MMOSpawnPoint entries match, the game falls back to Vanilla behavior (anchor/bed/world spawn).

### Compatibility with Other Plugins

MMOSpawnPoint can potentially work alongside respawn handling from CMI or EssentialsX if properly configured, but it's recommended to disable their respawn handling for the best experience:

- **For EssentialsX:** Set `respawn-at-home: false` in the essentials config.yml
- **For CMI:** Set `respawn.enabled: false` in the CMI config.yml

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
    partyRespawnDisabled: true
```

**For region-based dungeon plugins**, prefer coordinate-based matching with rects and excludeRects:

Example: one playable “dungeon courtyard” area, but excluding a boss room; then a second explicit entry that marks the boss room as “no party respawn”.

ASCII map (top view, X horizontal, Z vertical):
- █ = included rects
- ░ = excluded (carved out)

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

Note: Legacy x/y/z axes on destinations or triggerAreas are internally mapped to a single rect at runtime. You can keep writing simple xyz if you want; under the hood it becomes a rect.

1) Playable area with an excluded boss room

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

2) Boss room marked as “party respawn disabled” (no teleport, actions only)

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
    partyRespawnDisabled: true
```

This pattern easily extends to any event arenas, minigame areas, temporary zones, etc.

**Note:** You can also simply exclude an area if not need disable party respawn and not define any MSP rule for it at all. In that case:
- Either the world-level MSP fallback (if any) will catch the player,
- Or vanilla respawn will apply,
- Or the dungeon/minigames plugin you use might handle the respawn itself.
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

- Also, if your players can create WG regions, avoid regex matches that might include player-created names. Use explicit region names or restrict regex-based entries to admin-managed worlds. Similarly for world patterns (rare case).

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
    partyRespawnDisabled: true
```

2. **Dynamic Dungeon Worlds:** Essential for plugins like MythicDungeons
```yaml
spawns:
  - kind: world
    world: "MythicDungeonWorld_.*"
    worldMatchMode: regex
    destinations: []
    partyRespawnDisabled: true
```

3. **Temporary Event Areas:** When you have many similar temporary regions
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
  partyRespawnDisabled: true
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

- **Paper 1.18+:** Full native MiniMessage support with all features including gradients, hover effects, click events, and advanced formatting
- **Paper 1.16-1.17:** Partial support with automatic conversion to legacy ChatColor codes. Supported features include basic colors (`<red>`, `<blue>`, etc.), text styles (`<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>`), and reset tags (`<reset>`). Advanced features like gradients and hover effects are automatically stripped without causing errors.

You can use the [MiniMessage Web Editor](https://webui.advntr.dev/) to test and preview your formatting. The plugin will automatically adapt the formatting to your server's capabilities, so you can use the same configuration across different server versions.

## 🛠️ Troubleshooting

If you encounter issues with the plugin:

1. **Check your configuration:** Validate your YAML syntax using [YAMLLint](http://www.yamllint.com/)
2. **Enable debug modes:**
    - Set `settings.debugMode: true` in config.yml for detailed logs
    - Set `settings.safeLocationCache.advanced.debugCache: true` for cache debugging
3. **Use simulation tools:** Test your spawn points with `/msp simulate death` and `/msp simulate join`
4. **Check cache performance:** Monitor cache statistics with `/msp cache stats`
5. **Verify dependencies:** Make sure you have the correct versions of WorldGuard and PlaceholderAPI if using those features
6. **Check for conflicts:** Ensure no other plugins are handling respawn events
7. **Plugin conflicts:** Disable party respawn in worlds managed by dungeon plugins (MythicDungeons, DungeonsXL, etc.)

## 🛠️ Compatibility

- **Minecraft Versions:** 1.16.5 to the latest release
- **Server Software:**
    - ✅ [Paper](https://papermc.io/) (1.16.5 and newer) - **Fully Supported**
    - ✅ [Folia](https://papermc.io/software/folia) - **Fully Supported** with optimized region-aware scheduling
    - ❌ Spigot - Not supported
- **Java Version:** Java 17 or higher
- **Optional Dependencies:**
    - WorldGuard 7.0.5+ (for region-based spawns)
    - PlaceholderAPI 2.11.6+ (for condition-based spawns)

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
- **Claude (Anthropic)** - Advanced system architecture, complex algorithm implementation, and comprehensive documentation
- **ChatGPT (OpenAI)** - Feature design, code optimization, and configuration systems

*The majority of the plugin's sophisticated features, including the advanced party system, partial asynchronous safe location finding, and comprehensive configuration validation, were implemented through AI-assisted development.*

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

*Essential for professional servers • Required for map creators • The spawn system that powers the best Minecraft experiences*