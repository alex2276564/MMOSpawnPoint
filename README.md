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

[Click to view the default configuration](https://github.com/alex2276564/SmartSpawnPoint/blob/master/src/main/resources/config.yml)

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

* Send players who die in a PvP arena directly to a spectator area
* Teleport players who die in a dungeon to a recovery zone with healing effects
* Make players who die in the wilderness respawn at random locations for added challenge

### 🎲 Weighted Random Spawns

* Create multiple possible spawn points with different probabilities
* Adjust spawn chances based on player permissions or PlaceholderAPI conditions
* Give VIP players better spawn locations with higher probability

### 👥 Party System

* Allow players to form groups and respawn together
* Perfect for adventure maps, dungeons, and RPG servers
* Special "Walking Spawn Point" feature for content creators to keep their followers nearby

### 🎬 Content Creator Features

* Give your YouTubers and streamers the ability to create spawn point parties
* When they die, they respawn at their death location and act as spawn points for their followers
* Creates amazing opportunities for content creation and community engagement

### 🔄 Asynchronous Safe Location Finding

* The waiting room system prevents lag when finding safe spawn locations
* Players are temporarily teleported to a waiting area while the plugin searches for a safe location
* Perfect for random spawn points in unpredictable terrain

### 🎮 Enhanced Gameplay Experiences

* Send players to the Nether as punishment for dying in certain areas
* Create hardcore-like experiences where players respawn in completely random locations
* Design custom respawn experiences for different player ranks or achievements

### 🔰 Permission-Based Features

SmartSpawnPoint allows you to create tiered access to features based on player ranks or donation levels. Here's a complete list of permissions that you can assign in LuckPerms:

#### All Available Permissions

Copy these permissions into your LuckPerms editor (`/lp editor`) and assign them to appropriate groups:

```text
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

  ```text
  smartspawnpoint.command
  smartspawnpoint.party.accept
  smartspawnpoint.party.deny
  smartspawnpoint.party.leave
  smartspawnpoint.party.list
  ```

- **VIP Players** (Middle tier):

  ```text
  smartspawnpoint.party.invite
  smartspawnpoint.party.remove
  smartspawnpoint.party.setleader
  smartspawnpoint.party.options
  smartspawnpoint.vip
  ```

- **Premium/Content Creators** (Top tier):

  ```text
  smartspawnpoint.party.respawnatdeath
  smartspawnpoint.premium
  ```

- **Administrators**:

  ```text
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

5. **Waiting Room Design Considerations**: The waiting room feature is not just a temporary holding area - it's a fallback spawn location where players might remain if:

    - A safe location cannot be found within the configured timeout period
    - The server restarts during the asynchronous location search
    - An error occurs during the teleportation process

   ⚠️ **Don't Trap Players!** For this reason, waiting rooms should be:

    - **Fully functional areas**: Players should be able to move around and interact
    - **Escape-enabled**: Include a way out (NPCs, pressure plates, portals, commands)
    - **Strategically located**: Consider placing them near cities, markets, or hubs
    - **Properly protected**: Use WorldGuard to prevent griefing or damage
    - **Well-designed**: Include basic amenities and clear signage explaining the situation

   🏙️ **Themed Integration**: On RPG servers, consider creating themed "recovery zones" that fit your lore while serving as waiting rooms. For example, a temple of healing, a traveler's respite, or a dimensional nexus.

   > If you're using SmartSpawnPoint on a large server with many regions or factions, it's a good idea to theme each waiting room to match the area the player died in.

6. **Action Execution Timing**: Actions (commands and messages) are only executed after a player has been successfully teleported to their final respawn location:
    - If using the waiting room system, actions will NOT execute while the player is in the waiting room
    - Actions will only trigger once the player reaches their actual spawn point
    - This ensures that effects, items, and messages are applied at the appropriate time

7. **Vanilla vs SmartSpawnPoint Respawn Mechanics**:
    - **Vanilla Minecraft** (without any plugins) follows a strict respawn priority:
        1. First checks for a valid respawn anchor in the Nether dimension (if properly charged with glowstone)
            - Respawn anchors only work when placed in the Nether dimension
        2. Then checks for a valid bed in the Overworld dimension (if not broken or obstructed)
            - Beds only function as spawn points when placed in the Overworld
        3. If neither exists or they're obstructed, sends player to the world spawn point (usually near coordinates 0,0 or wherever /setworldspawn was set)
        4. In the End dimension, players always return to the Overworld spawn regardless of beds or anchors

    - **SmartSpawnPoint** completely transforms this with a more flexible priority system:
        1. Region-Based Spawns 🥇 (Highest Priority): Checks if player died in a configured WorldGuard region
        2. World-Based Spawns 🥈 (Secondary Priority): If no region match, checks for world-specific spawn rules
        3. Fallback to Vanilla (Lowest Priority): Only if no SmartSpawnPoint rules match
        4. Party System Overrides 🎉: Can override all above rules if enabled and conditions are met
        5. Weighted Random Logic ⚖️: Dynamically calculates spawn probabilities based on player attributes

   **Respawn Flow Summary:**
   Player Dies → Check Region Rules → If match, apply region spawn → If no match, check World Rules →
   If match, apply world spawn → If no match, use Vanilla Logic →
   Party Respawn overrides all if applicable

   **Important:** For any worlds or regions not configured in SmartSpawnPoint, the plugin will automatically fall back to vanilla respawn behavior. This allows you to selectively enhance only certain areas of your server while leaving others with default mechanics.

   This enhanced flow gives server owners unprecedented control over the respawn experience while maintaining compatibility with vanilla mechanics when desired. Note that for SmartSpawnPoint to work properly, respawn handling should be disabled in other plugins like CMI or EssentialsX as mentioned in note #3.

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