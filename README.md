# SeeMore

> [!IMPORTANT]
> This is a community fork of [froobynooby/SeeMore](https://github.com/froobynooby/SeeMore). It targets Paper
> 26.1.2 and adds layered permission-based view-distance overrides, input-aware AFK distance reduction, configurable
> permission/AFK checks, effective simulation-distance flooring, and automatic migration of v1/v2/v3 configs.

Upstream project links (these builds do not include the fork changes):
[Source](https://github.com/froobynooby/SeeMore) |
[Hangar](https://hangar.papermc.io/froobynooby/SeeMore) |
[Modrinth](https://modrinth.com/plugin/seemore)

## About
SeeMore is a Paper 26.1.2 plugin that sets each player's server-side view distance from their client settings,
ordered permission overrides, and AFK state. It remains compatible with Folia's scheduler model.

## Configuration

```yaml
# Configuration for SeeMore.

# Please don't change this!
version: 4

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

# Permission overrides are checked from top to bottom. The first matching permission wins.
permissions:
  # Set to disabled to check only when another event recalculates view distance.
  check-interval: 30s
  # Resolution order for the matching group:
  # 1. Exact group world override
  # 2. Exact top-level world setting
  # 3. Group default override
  # 4. Top-level default
  group-overrides:
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

The top-level `world-settings` section is the baseline for every player. For the first matching permission group,
an exact group world entry overrides the matching top-level world entry. If the group does not define that world,
the top-level exact world entry remains in effect. For worlds not named in either place, an optional group `default`
overrides the top-level `default`. A group may omit `default` entirely and define only the worlds it needs to change.

Groups are still checked from top to bottom, and only the first matching group is used. View distance never drops
below the player's effective simulation distance, and SeeMore does not change simulation distance itself.

### Configuration migration

On first startup, version 1, version 2, and version 3 configurations are automatically migrated to version 4.
Existing `update-delay`, `log-changes`, `world-settings`, permission entries, and AFK values remain in place. The
plugin creates a versioned backup such as `config.yml.v3.bak` before changing the file. Version 3's
`permissions.groups` key is automatically renamed to `permissions.group-overrides`.

Version 4 intentionally changes how a group's `default` interacts with explicitly named top-level worlds. A named
top-level world now remains in effect unless the matching group explicitly overrides that world. Review permission
groups after upgrading if they previously relied on a group `default` replacing every top-level world entry.

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
