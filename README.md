# SeeMore

> [!IMPORTANT]
> This is a community fork of [froobynooby/SeeMore](https://github.com/froobynooby/SeeMore). It targets Paper
> 26.1.2 and adds ordered permission-based view-distance profiles, input-aware AFK distance reduction, configurable
> permission/AFK checks, effective simulation-distance flooring, and automatic migration of upstream v1/v2 configs.

Upstream project links (these builds do not include the fork changes):
[Source](https://github.com/froobynooby/SeeMore) |
[Hangar](https://hangar.papermc.io/froobynooby/SeeMore) |
[Modrinth](https://modrinth.com/plugin/seemore)

## About
SeeMore is a Paper 26.1.2 plugin that sets each player's server-side view distance from their client settings,
ordered permission profiles, and AFK state. It remains compatible with Folia's scheduler model.

## Configuration

```yaml
# Configuration for SeeMore.

# Please don't change this!
version: 3

# The delay (in ticks) before a player's view distance is lowered after their client settings change.
#  * This stops players overloading the server by constantly changing their view distance.
update-delay: 600

# Whether the plugin should log to the console when it changes a player's view distance.
log-changes: true

# These settings can be specified per world.
#  * Note: If a world is not listed here or if a setting is missing, it will use the settings listed under the default
#    section.
world-settings:
  default:
    # The maximum view distance a player in this world can have.
    # Set to -1 to use the server's configured view distance for this world.
    maximum-view-distance: -1

# Permission profiles are checked from top to bottom. The first matching permission wins.
permissions:
  # Set to disabled to check only when another event recalculates view distance.
  check-interval: 30s
  groups:
    - name: admin
      permission: seemore.view-distance.admin
      world-settings:
        default:
          maximum-view-distance: -1
        world_nether:
          maximum-view-distance: 10

    - name: donor
      permission: seemore.view-distance.donor
      world-settings:
        default:
          maximum-view-distance: 18
        world_nether:
          maximum-view-distance: 10

# Passive position changes, such as water in an AFK pool, are not activity.
afk:
  enabled: true
  check-interval: 10s
  timeout: 10m
  maximum-view-distance: 8
  wake-up:
    minimum-look-change: 2.0
    required-look-events: 2
    look-event-window: 2s
```

The existing `world-settings` section remains the fallback for players without a matching permission. Each profile
must contain its own `default` world entry. View distance never drops below the player's effective simulation
distance, and SeeMore does not change simulation distance itself.

### Configuration migration

On first startup, version 1 and version 2 configurations are automatically migrated to version 3. Existing
`update-delay`, `log-changes`, and `world-settings` values remain in place. The plugin creates a backup named
`config.yml.v1.bak` or `config.yml.v2.bak` before changing the file, then appends the new permission and AFK defaults.

## Commands

| Command            | Permission                | Description                                             |
|--------------------|---------------------------|---------------------------------------------------------|
| `/seemore reload`  | `seemore.command.reload`  | Reload the plugin's configuration.                      |
| `/seemore average` | `seemore.command.average` | Show the effective average view distance of the worlds. |
| `/seemore players` | `seemore.command.players` | Show the server-side view distance of all players.      |

## Building
Building requires Java 25.

1\. Clone SeeMore and build
```bash
git clone https://github.com/UPSOKen/SeeMore
cd SeeMore
./gradlew clean build
```

2\. Find the shaded jar in `SeeMore/build/libs`.
