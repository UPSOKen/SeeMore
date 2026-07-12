# SeeMore

> [!IMPORTANT]
> This is a community fork of [froobynooby/SeeMore](https://github.com/froobynooby/SeeMore). It targets Paper
> 26.1.2 and adds layered permission-based view-distance overrides, input-aware AFK distance reduction, configurable
> AFK checks, experimental underground distance reduction, effective simulation-distance flooring, and automatic
> migration of v1/v2/v3/v4/v5/v6/v7 configs.

Upstream project links (these builds do not include the fork changes):
[Source](https://github.com/froobynooby/SeeMore) |
[Hangar](https://hangar.papermc.io/froobynooby/SeeMore) |
[Modrinth](https://modrinth.com/plugin/seemore)

## About
SeeMore is a Paper 26.1.2 plugin that sets each player's server-side view distance from their client settings,
ordered permission overrides, AFK state, and sustained underground state. It remains compatible with Folia's
scheduler model.

## Configuration

```yaml
# Configuration for SeeMore.

# Please don't change this!
version: 8

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
  check-interval: 1m
  timeout: 15m
  maximum-view-distance: 10
  minimum-reduction: 3
  # Blank values send no message. Non-blank values support Adventure MiniMessage formatting.
  alerts:
    reduced-message: ""
    restoring-message: ""
  wake-up:
    minimum-look-change: 2.0
    required-look-events: 2
    look-event-window: 2s

# Experimental and disabled by default. Active players who remain underground can use a lower view-distance cap.
underground:
  enabled: false
  # Allow seemore.underground.bypass to exempt individual players.
  enable-bypass-permission: false
  world-list-mode: whitelist
  worlds:
    - world
  check-interval: 5s
  enter-after: 2m
  exit-after: 5s
  minimum-depth: 10
  exit-depth: 5
  maximum-view-distance: 8
  natural-ceiling:
    enabled: true
    search-distance: 32
    minimum-thickness: 2
    additional-materials: []
    excluded-materials: []
```

The top-level `world-settings` section is the baseline for every player. For the first matching permission group,
an exact group world entry overrides the matching top-level world entry. If the group does not define that world,
the top-level exact world entry remains in effect. For worlds not named in either place, an optional group `default`
overrides the top-level `default`. A group may omit `default` entirely and define only the worlds it needs to change.

Groups are still checked from top to bottom, and only the first matching group is used. View distance never drops
below the player's effective simulation distance, and SeeMore does not change simulation distance itself. A player's
permission profile is selected on join and refreshed only on world change or `/seemore reload`; AFK, underground, and
client-setting changes reuse the cached profile.

### Configuration migration

On first startup, version 1 through version 7 configurations are automatically migrated to version 8.
Existing `update-delay`, `log-changes`, `world-settings`, permission entries, and AFK values remain in place. The
plugin creates a versioned backup such as `config.yml.v3.bak` before changing the file. Version 3's
`permissions.groups` key is automatically renamed to `permissions.group-overrides`.

Version 8 removes interval permission polling and the experimental version 7 chunk-refresh switch. Permission profiles
are now cached by lifecycle event, preventing background permission checks from recalculating player distances. Fresh
configurations use a 15-minute AFK timeout, a one-minute check interval, a view-distance cap of 10, and require at least
three chunks of useful radius reduction. Existing AFK timing and cap values are preserved during migration. AFK alert
messages are blank and silent by default; configured messages support Adventure MiniMessage formatting.

Version 6 adds conservative natural-ceiling evidence to underground detection. After the local-depth check passes,
SeeMore ignores passable blocks above the player and searches at most 32 blocks for the first solid ceiling. That
ceiling and the configured thickness behind it must match natural stone, ore, or selected geological materials.
Constructed ceilings stop the search and prevent underground mode. Administrators can add or exclude material enum
names for custom terrain and builds. The ceiling requirement also applies in eligible worlds without skylight;
disabling it restores version 5's whole-dimension behavior. Existing version 5 underground settings are preserved
during migration.

Version 5 adds underground detection with conservative, disabled-by-default settings. It performs a cached local
surface-height lookup for eligible active players, enters underground mode only after continuous qualifying time,
and restores the normal distance after continuous surface evidence. In eligible worlds without skylight, such as
the Nether, the entire world qualifies as underground. AFK mode takes precedence. To exempt individual players,
set `underground.enable-bypass-permission` to `true` and grant `seemore.underground.bypass`. The config option and
permission both default to false, including for operators.

Version 4 intentionally changes how a group's `default` interacts with explicitly named top-level worlds. A named
top-level world now remains in effect unless the matching group explicitly overrides that world. Review permission
groups after upgrading if they previously relied on a group `default` replacing every top-level world entry.

## Commands

| Command            | Permission                | Description                                             |
|--------------------|---------------------------|---------------------------------------------------------|
| `/seemore reload`  | `seemore.command.reload`  | Reload the plugin's configuration.                      |
| `/seemore average` | `seemore.command.average` | Show the effective average view distance of the worlds. |
| `/seemore players` | `seemore.command.players` | Show the server-side view distance of all players.      |
| `/seemore info [world]` | `seemore.command.info` | Show server, world, profile, and underground settings. |
| `/seemore status [player]` | `seemore.command.status` | Show one player's live and target distance state. |

Inspecting another player with `/seemore status <player>` additionally requires
`seemore.command.status.others`.

## Building
Building requires Java 25.

1\. Clone SeeMore and build
```bash
git clone https://github.com/UPSOKen/SeeMore
cd SeeMore
./gradlew clean build
```

2\. Find the shaded jar in `SeeMore/build/libs`.
