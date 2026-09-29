# ULTRAS_TPA

Premium GUI-based TPA / TPAHERE plugin for Paper. Developer: **UC_Hussein**.

## Requirements
- Paper 1.21+ (Folia is not supported)
- Java 21 or newer (runs on Java 25)

## Installation
1. Build the jar (see *Building*) or take it from `build/libs/`.
2. Put `ULTRAS_TPA-<version>.jar` in `plugins/`.
3. Start the server. Config files are created in `plugins/ULTRAS_TPA/`.

## Commands
| Command | Description |
|---|---|
| `/tpa [player]` | Open the player list, or the TPA / TPAHERE menu for a player |
| `/tpa here [player]`, `/tpahere [player]` | Same selection menu |
| `/tpa accept\|deny [player]`, `/tpaccept` (`/tpaaccept`), `/tpadeny` | Open the request menu, the decision is made there |
| `/tpa cancel`, `/tpacancel` | Cancel your pending request |
| `/tpa setting`, `/tpasetting`, `/uc_setting` | Open your settings |
| `/tpauto [on\|off]`, `/tpa auto [on\|off]`, `/uc_tpa auto` | Turn automatic acceptance on or off (no argument = toggle) |
| `/uc_tpa block <player>` | Block or unblock a player |
| `/uc_tpa sounds`, `requests`, `settings` | Toggle sounds, open requests, open settings |
| `/uc_tpa world disable\|enable\|list [world]` | Disable or enable teleporting in a world (saved in config.yml) |
| `/uc_tpa reload` | Reload config, languages, sounds and GUIs |

`/uc_tpa world` without a world name uses the world you are standing in. It also works from the console.

## Permissions
| Permission | Default | Description |
|---|---|---|
| `ultras.tpa.use` | true | Use the commands and menus |
| `ultras.tpa.send` | true | Send requests |
| `ultras.tpa.receive` | true | Receive requests |
| `ultras.tpa.autoaccept` | true | Use automatic acceptance |
| `ultras.tpa.world` | op | Manage disabled worlds |
| `ultras.tpa.reload` | op | Reload the plugin |
| `ultras.tpa.bypass.cooldown` | op | Ignore the send cooldown |
| `ultras.tpa.admin` | op | Reload, world, bypass |

## Disabled worlds
A world in `disabled-worlds` blocks sending a request from it, sending a request to a player in it, accepting, and the final teleport (so a countdown that started earlier still fails safely). Manage it in game with `/uc_tpa world disable <world>` or edit `config.yml` and run `/uc_tpa reload`. Names are case-insensitive. The in-game command writes to `config.yml` with `saveConfig()`; Paper keeps existing comments, but if you want a fully hand-formatted file, edit it by hand.

## Auto accept
A player turns it on with `/tpauto` or in `/tpasetting`. It is **off by default** for everyone and stored per UUID.
When on, incoming requests are accepted immediately and the normal countdown starts. Blocked players, disabled worlds, the receive toggles and the sender cooldown still apply. If the auto accept cannot complete (for example the traveler is already teleporting), the request stays pending as a normal request.
In `config.yml`: `auto-accept.enabled: false` removes the feature, `auto-accept.allow-tpahere: false` keeps TPAHERE manual (recommended if you consider it risky).

## Configuration (`config.yml`)
`request-expiration` (60), `send-cooldown` (15), `teleport-countdown` (3), `cancel-on-move`, `cancel-on-quit`, `disabled-worlds`, `auto-accept.*`, `language.default` / `language.allowed`, `sounds.enabled`, `actionbar.enabled`, `auto-open-gui.default`, `player-info.*`, `command-override.enabled`.

## GUI customisation
`guis/*.yml` hold the layout only: rows, filler, slot, material, glow, on/off variants. Names and lore come from the language files (`gui.<menu>.<item>.name|lore`). Add `name:` or `lore:` to an item in a GUI file to override it for every language. Slots are 0-based.

## Languages
`languages/en.yml` and `languages/ar.yml` (MiniMessage). Each player can switch language in settings. To add one, copy `en.yml` to `xx.yml`, add `XX` to `language.allowed`, and set its `language-name`.

## Sounds
`sounds.yml`: one entry per event with `sound`, `volume`, `pitch`. Players can turn sounds off for themselves.

## Command conflicts
After all plugins are loaded, ULTRAS_TPA re-points the TPA labels in the command map, and also intercepts player execution and tab completion for labels another plugin owned. The other plugins stay enabled. This is best effort: Paper's Brigadier tree may still hold the other plugin's node. If a command still opens the other plugin, remove or disable that command in the other plugin (or use `commands.yml` aliases), and keep `command-override.enabled: true`.

## Troubleshooting
- Nothing happens on `/tpa`: check the console for `Took over conflicting commands`.
- "teleportation is not allowed in this world": check `/uc_tpa world list`.
- Settings are not saved: check write access to `plugins/ULTRAS_TPA/playerdata.yml`.
- Wrong colors or symbols: the client needs a font with small-caps letters (default Minecraft font works).

## Building
```
gradle build
```
Needs a JDK 21+ and network access to the Paper repository. The jar is in `build/libs/`.

---

## ملخص بالعربية
- `/tpauto` أو `/tpasetting` لتفعيل القبول التلقائي (معطّل افتراضيًا).
- `/uc_tpa world disable|enable|list [العالم]` لتعطيل التنقل في عالم محدد (صلاحية `ultras.tpa.world`).
- كل النصوص والأصوات والقوائم قابلة للتعديل من ملفات `languages` و`sounds.yml` و`guis`.
