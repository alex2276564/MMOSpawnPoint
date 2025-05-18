# SmartSpawnPoint 🌟

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5+-brightgreen)](https://papermc.io/software/paper)
[![Java Version](https://img.shields.io/badge/java-16+-orange)](https://adoptium.net/installation/linux/)
[![GitHub Release](https://img.shields.io/github/v/release/alex2276564/SmartSpawnPoint?color=blue)](https://github.com/alex2276564/SmartSpawnPoint/releases/latest)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

**SmartSpawnPoint** is a powerful and flexible Minecraft plugin that revolutionizes how player respawning works on your server. With region-based, world-based, and condition-based spawn points, you can create immersive experiences for your players that go far beyond the vanilla respawn mechanics.

## ✨ Features

* **Multiple Spawn Types:** Configure fixed, random, or weighted random spawn points
* **Region-Based Spawns:** Set different spawn points for different WorldGuard regions
* **World-Based Spawns:** Configure unique spawn points for each world
* **Conditional Spawns:** Use permissions and PlaceholderAPI to determine where players respawn
* **Safe Location Finding:** Automatically find safe locations for players to respawn
* **Waiting Room System:** Reduce lag with asynchronous safe location searching
* **Party System:** Allow players to form groups and respawn together
* **Walking Spawn Points:** Special players can respawn at their death location and serve as spawn points for others
* **Customizable Actions:** Execute commands and send messages on respawn
* **Highly Configurable:** Extensive configuration options to suit any server's needs
* **Auto-Update Check:** On server start, the plugin checks for updates. If a new version is available, a notification is displayed in the console

## 📥 Installation

1. **Download:** Download the latest version of SmartSpawnPoint from the [Releases](https://github.com/alex2276564/SmartSpawnPoint/releases) page.
2. **Install:** Place the `.jar` file into your server's `plugins` folder.
3. **Optional Dependencies:**
   - [WorldGuard](https://dev.bukkit.org/projects/worldguard) - For region-based spawn points
   - [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) - For condition-based spawn points
4. **Restart:** Restart your server to load the plugin.

## 🛠️ Configuration

SmartSpawnPoint offers extensive configuration options to customize the respawn experience on your server.

<details>
<summary>Click to view the default configuration</summary>

```yaml
# SmartSpawnPoint Configuration

# Region-based spawns (priority)
region-spawns:
  # Example for "spawn" region
  - region: "spawn"
    # World where this region is located. Use "*" for all worlds.
    region-world: "world"
    # Type: "fixed" for exact coordinates, "random" for random location, "weighted_random" for weighted chances,
    # "none" to just apply actions without teleporting
    type: "fixed"
    # Location where player will respawn
    location:
      world: "world"
      x: 0
      y: 64
      z: 0
      yaw: 0
      pitch: 0
      require-safe: false  # Set to false for known safe locations to improve performance
    # Custom waiting room for this spawn point (optional)
    # If waiting-room is enabled in settings and require-safe is true,
    # player will be teleported here temporarily while a safe location is found
    waiting-room:
      world: "world"
      x: 0
      y: 100
      z: 0
      yaw: 0
      pitch: 0
    # Conditions - all must be true for this spawn to be used
    # Note: Players with OP status or "*" permission will automatically pass permission checks
    conditions:
      # Permission conditions
      permissions:
        - "smartspawnpoint.vip"
      # PlaceholderAPI conditions (placeholder operator value)
      # Supported operators: =, ==, !=, >, >=, <, <=
      placeholders:
        - "%player_level% > 10"
    # Actions executed on respawn
    actions:
      # Messages to send to player
      messages:
        - "You have respawned at the spawn region!"
      # Commands to execute (as console)
      commands:
        # Multiple commands with different chances and conditions
        - command: "effect give %player% minecraft:resistance 30 1"
          chance: 100  # 100% chance to execute

        - command: "give %player% minecraft:golden_apple 1"
          chance: 50   # 50% chance to execute
          # Conditional chances based on player permissions or placeholders
          chance-conditions:
            - type: "permission"
              value: "smartspawnpoint.vip"
              weight: 80  # 80% chance for VIP players
            - type: "placeholder"
              value: "%player_level% > 20"  # For players with level > 20
              weight: 100

        - command: "give %player% minecraft:golden_apple 2"
          chance: 25   # 25% chance to execute
          # Conditional chances based on player permissions or placeholders
          chance-conditions:
            - type: "permission"
              value: "smartspawnpoint.vip"
              weight: 40  # 40% chance for VIP players
            - type: "placeholder"
              value: "%player_level% > 20"  # For players with level > 20
              weight: 50

  # Example for PvP arena with disabled party respawn
  - region: "pvp_arena"
    region-world: "*"  # Will work in any world
    type: "random"
    # Disable party respawn in this region
    party-respawn-disabled: true
    location:
      world: "world"
      min-x: -100
      max-x: 100
      min-y: 64
      max-y: 80
      min-z: -100
      max-z: 100
      require-safe: false  # Disable safe location check for better performance
    # Custom waiting room (only used if require-safe is true)
    waiting-room:
      world: "world"
      x: 0
      y: 90
      z: 0

  # Example for wilderness region with mixed location types
  - region: "wilderness"
    region-world: "world"
    type: "weighted_random"
    locations:
      # Fixed location with 70% chance
      - type: "fixed"
        location:
          world: "world"
          x: 100
          y: 64
          z: 100
          require-safe: false
        weight: 70  # 70% chance
        weight-conditions:
          - type: "permission"
            value: "smartspawnpoint.safe"
            weight: 90  # 90% chance for players with this permission
        # Custom waiting room for this specific location option
        waiting-room:
          world: "world"
          x: 200
          y: 100
          z: 200
          yaw: 90
          pitch: 0
      # Random location with 30% chance
      - type: "random"
        location:
          world: "world"
          min-x: -200
          max-x: -100
          min-y: 64
          max-y: 80
          min-z: -200
          max-z: -100
          require-safe: false
        weight: 30  # 30% chance
        weight-conditions:
          - type: "permission"
            value: "smartspawnpoint.adventure"
            weight: 60  # 60% chance for adventure players
        # Custom waiting room for this specific location option
        waiting-room:
          world: "world"
          x: 300
          y: 100
          z: 300

# World-based spawns (secondary priority)
world-spawns:
  # Default world
  - world: "world"
    type: "fixed"
    location:
      x: 0
      y: 64
      z: 0
      require-safe: false
    # Custom waiting room for this world spawn
    waiting-room:
      world: "world"
      x: 0
      y: 120
      z: 0
    actions:
      messages:
        - "You have respawned in the overworld!"
      commands:
        - command: "effect give %player% minecraft:regeneration 5 1"
          chance: 100
        - command: "give %player% minecraft:bread 3"
          chance: 100

  # Nether world example with mixed location types and disabled party respawn
  - world: "world_nether"
    type: "weighted_random"
    # Disable party respawn in this world
    party-respawn-disabled: true
    locations:
      # Option 1: Return to main world
      - type: "fixed"
        location:
          world: "world"
          x: 0
          y: 64
          z: 0
          require-safe: false
        weight: 80
        # Custom waiting room for this specific location option
        waiting-room:
          world: "world"
          x: 100
          y: 90
          z: 100
      # Option 2: Random location in the nether (more dangerous)
      - type: "random"
        location:
          world: "world_nether"
          min-x: -100
          max-x: 100
          min-y: 30
          max-y: 100
          min-z: -100
          max-z: 100
          require-safe: true
        weight: 20
        weight-conditions:
          - type: "permission"
            value: "smartspawnpoint.nether"
            weight: 50  # Higher chance for players with nether permission
        # Custom waiting room for this specific location option
        waiting-room:
          world: "world_nether"
          x: 0
          y: 100
          z: 0
    # Custom waiting room for the whole spawn point (used if no custom location waiting room)
    waiting-room:
      world: "world"
      x: 0
      y: 150
      z: 0
    actions:
      messages:
        - "You died in the Nether!"
      commands:
        - command: "effect give %player% minecraft:fire_resistance 60 1"
          chance: 100
        - command: "give %player% minecraft:golden_carrot 2"
          chance: 50
          chance-conditions:
            - type: "permission"
              value: "smartspawnpoint.nether"
              weight: 100  # 100% chance for nether players

  # End world example
  - world: "world_the_end"
    type: "fixed"
    location:
      world: "world"  # Respawn in overworld
      x: 0
      y: 64
      z: 0
      require-safe: false
    # Custom waiting room
    waiting-room:
      world: "world"
      x: 0
      y: 150
      z: 0
    actions:
      messages:
        - "You died in the End! Respawning in the overworld..."
      commands:
        - command: "effect give %player% minecraft:slow_falling 30 1"
          chance: 100
        - command: "effect give %player% minecraft:night_vision 30 1"
          chance: 70
        - command: "clear %player%" # Clear inventory on death in End
          chance: 10 # 10% chance to lose all items
          chance-conditions:
            - type: "permission"
              value: "smartspawnpoint.end.protect"
              weight: 0 # 0% chance for players with protection permission

# General settings
settings:
  # Maximum attempts to find safe location for random spawns
  max-safe-location-attempts: 20

  # Radius to search for safe locations (smaller = better performance)
  safe-location-radius: 5

  # Enable debug mode for detailed logs
  debug-mode: true

  # Force a delayed teleport even if respawn event has been processed
  force-delayed-teleport: true

  # Waiting room settings
  # The waiting room system helps reduce lag by allowing the plugin to search for
  # safe spawn locations asynchronously. When a player dies and requires a safe location,
  # they'll be temporarily teleported to the waiting room while the plugin searches
  # for an appropriate safe location in the background.
  #
  # You can specify waiting rooms at three levels of precedence:
  # 1. Individual weighted location waiting room (highest priority)
  # 2. Spawn point waiting room (used if no location-specific room)
  # 3. Global waiting room (lowest priority, used as fallback)
  waiting-room:
    # Enable the waiting room system - recommended if you use require-safe: true anywhere
    enabled: true

    # Timeout for async safe location search (seconds)
    # If a safe location isn't found within this time, player stays in waiting room
    async-search-timeout: 5

    # Global waiting room location (used if no custom waiting room is specified)
    location:
      world: "world"
      x: 0
      y: 100
      z: 0
      yaw: 0
      pitch: 0

  # Party system settings
  # The party system allows players to form groups and respawn near each other after death.
  party:
    # Enable party system
    enabled: true

    # Maximum number of players in a party
    # Set to 0 for unlimited
    max-size: 10

    # Maximum distance for party respawn (blocks)
    # Set to 0 for unlimited distance
    max-respawn-distance: 0

    # Cooldown between party respawn for same player (seconds)
    # Set to 0 for no cooldown
    respawn-cooldown: 0

    # "Walking Spawn Point" feature
    # Players with this permission will respawn at their death location
    # and serve as spawn points for other party members
    # This is useful for content creators or special players
    respawn-at-death:
      enabled: true
      permission: "smartspawnpoint.party.respawnatdeath"
      message: "&aYou have respawned at your death location as a walking spawn point"

    # Invitation expiry time (seconds)
    invitation-expiry: 60

    # Message settings
    messages:
      # Colors: You can use '&' for color codes
      prefix: "&8[&bParty&8] &r"
      invite-sent: "&aInvitation sent to %player%"
      invite-received: "&aYou've been invited to join %player%'s party. Type /smartspawnpoint party accept to join"
      invite-declined: "&c%player% has declined your party invitation"
      invite-expired: "&cParty invitation expired"
      party-joined: "&a%player% has joined your party!"
      party-left: "&c%player% has left the party"
      player-kicked: "&c%player% has been removed from the party"
      player-not-found: "&cPlayer not found or offline"
      not-in-party: "&cYou are not in a party"
      not-leader: "&cOnly the party leader can do this"
      player-not-in-party: "&cThis player is not in your party"
      leader-changed: "&a%player% is now the party leader"
      party-disbanded: "&cThe party has been disbanded"
      respawned-at-member: "&aYou have respawned at %player%'s location"
      respawn-mode-changed: "&aParty respawn mode set to: %mode%"
      respawn-target-set: "&aYou will now respawn at %player%'s location"
      respawn-disabled-region: "&cParty respawn is disabled in this region"
      respawn-disabled-world: "&cParty respawn is disabled in this world"
      respawn-cooldown: "&cYou must wait %time% seconds before using party respawn again"

  # List of materials considered unsafe (will avoid spawning on these)
  unsafe-materials:
    - "LAVA"
    - "FIRE"
    - "CACTUS"
    - "WATER"
    - "AIR"
    - "MAGMA_BLOCK"
    - "CAMPFIRE"
    - "SOUL_CAMPFIRE"
    - "WITHER_ROSE"
    - "SWEET_BERRY_BUSH"
```
</details>

## 📜 Commands

SmartSpawnPoint supports both the full command `/smartspawnpoint` and the shorter alias `/ssp` for all commands.

- `/smartspawnpoint reload` - Reloads the plugin configuration (requires `smartspawnpoint.reload` permission)
- `/smartspawnpoint party invite <player>` - Invites a player to your party (requires `smartspawnpoint.party.invite` permission)
- `/smartspawnpoint party accept` - Accepts a party invitation (requires `smartspawnpoint.party.accept` permission)
- `/smartspawnpoint party deny` - Declines a party invitation (requires `smartspawnpoint.party.deny` permission)
- `/smartspawnpoint party leave` - Leaves your current party (requires `smartspawnpoint.party.leave` permission)
- `/smartspawnpoint party list` - Lists all members in your party (requires `smartspawnpoint.party.list` permission)
- `/smartspawnpoint party remove <player>` - Removes a player from your party (requires `smartspawnpoint.party.remove` permission)
- `/smartspawnpoint party setleader <player>` - Sets a new party leader (requires `smartspawnpoint.party.setleader` permission)
- `/smartspawnpoint party options` - Configures party options (requires `smartspawnpoint.party.options` permission)

## 🔄 How It Works

Are you still using spawn points from CMI or EssentialsX? Their spawn point systems were designed over a decade ago and haven't evolved much since. Isn't it time for something better?

Are you tired of players always respawning at the server spawn or their bed after death? Don't you think this mechanic is outdated and limiting for modern Minecraft servers?

SmartSpawnPoint revolutionizes player respawning with features like:

### 🌍 Region-Based Respawning
- Send players who die in a PvP arena directly to a spectator area
- Teleport players who die in a dungeon to a recovery zone with healing effects
- Make players who die in the wilderness respawn at random locations for added challenge

### 🎲 Weighted Random Spawns
- Create multiple possible spawn points with different probabilities
- Adjust spawn chances based on player permissions or PlaceholderAPI conditions
- Give VIP players better spawn locations with higher probability

### 👥 Party System
- Allow players to form groups and respawn together
- Perfect for adventure maps, dungeons, and RPG servers
- Special "Walking Spawn Point" feature for content creators to keep their followers nearby

### 🎬 Content Creator Features
- Give your YouTubers and streamers the ability to create spawn point parties
- When they die, they respawn at their death location and act as spawn points for their followers
- Creates amazing opportunities for content creation and community engagement

### 🔄 Asynchronous Safe Location Finding
- The waiting room system prevents lag when finding safe spawn locations
- Players are temporarily teleported to a waiting area while the plugin searches for a safe location
- Perfect for random spawn points in unpredictable terrain

### 🎮 Enhanced Gameplay Experiences
- Send players to the Nether as punishment for dying in certain areas
- Create hardcore-like experiences where players respawn in completely random locations
- Design custom respawn experiences for different player ranks or achievements

### 🔰 Permission-Based Features

SmartSpawnPoint allows you to create tiered access to features based on player ranks or donation levels. Here's a complete list of permissions that you can assign in LuckPerms:

#### All Available Permissions

Copy these permissions into your LuckPerms editor (`/lp editor`) and assign them to appropriate groups:

```
smartspawnpoint.command
smartspawnpoint.reload
smartspawnpoint.party.invite
smartspawnpoint.party.accept
smartspawnpoint.party.deny
smartspawnpoint.party.leave
smartspawnpoint.party.list
smartspawnpoint.party.remove
smartspawnpoint.party.setleader
smartspawnpoint.party.options
smartspawnpoint.party.respawnatdeath
```

#### Suggested Permission Structure

- **Basic Players** (Default tier):
  ```
  smartspawnpoint.command
  smartspawnpoint.party.accept
  smartspawnpoint.party.deny
  smartspawnpoint.party.leave
  smartspawnpoint.party.list
  ```

- **VIP Players** (Middle tier):
  ```
  smartspawnpoint.party.invite
  smartspawnpoint.party.remove
  smartspawnpoint.party.setleader
  smartspawnpoint.party.options
  smartspawnpoint.vip
  ```

- **Premium/Content Creators** (Top tier):
  ```
  smartspawnpoint.party.respawnatdeath
  smartspawnpoint.premium
  ```

- **Administrators**:
  ```
  smartspawnpoint.reload
  ```

This tiered permission system creates progression and incentives for player ranks, enhancing both gameplay and monetization opportunities for your server. Players will have a reason to upgrade their rank to access more advanced party features and spawn benefits.

You can also configure special spawn points or weighted chances in your config based on these permissions:

```yaml
# Example of VIP-only spawn point
conditions:
  permissions:
    - "smartspawnpoint.vip"

# Example of weighted chances for premium players
weight-conditions:
  - type: "permission"
    value: "smartspawnpoint.premium"
    weight: 100  # 100% chance for premium players
```

SmartSpawnPoint isn't just a spawn point plugin - it's a complete respawn management system that opens up endless possibilities for your server!

## ⚠️ Important Notes

1. **Nether Teleportation**: If you want to teleport players to the Nether, refer to the example in the configuration. Make sure to set `require-safe: true` to avoid players spawning in dangerous locations.

2. **Region Entry vs Respawn**: SmartSpawnPoint only handles player respawning after death, not entry into regions or worlds. For region entry commands, use WorldGuard flags like `entry-command` or `entry-deny`. For first-join teleportation, consider using Multiverse-Core's `firstspawnoverride` setting.

3. **Compatibility with Other Plugins**: SmartSpawnPoint can potentially work alongside respawn handling from CMI or EssentialsX if properly configured, but it's recommended to disable their respawn handling for the best experience:
   - For EssentialsX: Set `respawn-at-home: false` in the essentials config.yml
   - For CMI: Set `respawn.enabled: false` in the CMI config.yml

4. **Safe Location Finding**: The `require-safe` option should be set to `false` for known safe locations to improve performance. Only enable it when spawning in potentially dangerous areas.

## 🛠️ Troubleshooting

If you encounter issues with the plugin:

1. **Check your configuration**: Validate your YAML syntax using [YAMLLint](http://www.yamllint.com/)
2. **Enable debug mode**: Set `debug-mode: true` in the config to get detailed logs
3. **Check for conflicts**: Ensure no other plugins are handling respawn events
4. **Verify dependencies**: Make sure you have the correct versions of WorldGuard and PlaceholderAPI if you're using those features
5. **Plugin conflicts**: Avoid configuring SmartSpawnPoint respawn mechanics in regions or worlds that are already managed by plugins like MythicDungeons. These plugins often have their own respawn handling, which can conflict with SmartSpawnPoint.
6. **Delayed teleportation**: If you're experiencing issues with other plugins that handle teleportation or respawning, try setting `force-delayed-teleport: true` in the config. This can help resolve timing conflicts by ensuring SmartSpawnPoint's teleportation happens after other plugins have processed the respawn event.

## 📦 Compatibility

- **Minecraft Versions:** 1.16.5 to the latest release
- **Server Software:** [Paper](https://papermc.io/) (1.16.5 and newer)
- **Java Version:** Java 16 or higher
- **Optional Dependencies:**
  - WorldGuard 7.0.5+ (for region-based spawns)
  - PlaceholderAPI 2.11.6+ (for condition-based spawns)

## 🆘 Support

If you encounter any issues or have suggestions for improving the plugin, please create an [issue](https://github.com/alex2276564/SmartSpawnPoint/issues) in this repository.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

[Alex] - [https://github.com/alex2276564]

We appreciate your contribution to the project! If you like this plugin, please give it a star on GitHub.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/alex2276564/SmartSpawnPoint/issues).

### How to Contribute

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/YourFeature`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature/YourFeature`).
5. Open a Pull Request.

---

Thank you for using **SmartSpawnPoint**! We hope it enhances your server's gameplay experience. 🎮🌟