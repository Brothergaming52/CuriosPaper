# Commands

CuriosPaper adds three commands to your server. All commands support tab completion.

CuriosPaper provides three main commands for players and administrators.

## Player Commands

### `/baubles`

Opens the accessory inventory GUI.

| | |
|---|---|
| **Aliases** | `/b`, `/bbag` |
| **Permission** | None (all players) |
| **Usage** | `/baubles` |
| **Player Only** | Yes |

![Player using the /baubles command to open the accessory GUI](../images/baubles-command.png)

## Admin Commands

### `/curios`

Administrative, debug, and management commands for CuriosPaper.

| | |
|---|---|
| **Aliases** | `/cp`, `/curiospaper` |
| **Permission** | `curiospaper.admin` |
| **Usage** | `/curios <rp|debug|reload|list|give|create|edit|delete|inspect|recordrtp>` |

![Player using the /curios command to open the accessory GUI](../images/curios-command.png)

#### Management Subcommands

| Command | Description |
|---|---|
| `/curios create <itemId>` | Create a new custom item and open the Edit GUI |
| `/curios edit <itemId>` | Open the Edit GUI for an existing item |
| `/curios delete <itemId>` | Delete a custom item |
| `/curios list` | List all custom items in a paginated GUI |
| `/curios give <itemId> [player] [amount]` | Give a custom item to a player |
| `/curios inspect <player> [slot]` | Inspect and manage an online or offline player's accessories. If `slot` is omitted, opens a slot overview. Otherwise, opens a slot edit GUI. |
| `/curios recordrtp` | Toggle interactive recording of Random Teleport (RTP) sequences (commands, blocks, entities, GUIs) |
| `/curios reload` | Reload the plugin configuration and messages |

#### Resource Pack Subcommands

| Command | Description |
|---|---|
| `/curios rp info` | Display resource pack status, file size, hash, namespaces, and conflicts |
| `/curios rp rebuild` | Regenerate the resource pack and re-send it to all online players |
| `/curios rp conflicts` | List all file and namespace conflicts from the last build |

#### GUI Customization Subcommands

| Command | Description |
|---|---|
| `/curios editmenu` | Opens a GUI to rearrange accessory slots in the main (Tier 1) menu. Changes are saved to `config.yml`. |

#### Debug Subcommands

| Command | Permission | Description |
|---|---|---|
| `/curios debug player <name>` | `curiospaper.debug` | Inspect a player's equipped accessories, PDC data, and slot info |
| `/curios debug item` | `curiospaper.debug` | Inspect the held item's accessory tag, slot type, and PDC keys |

!!! tip "Tab Completion"
    All commands support tab completion for subcommands, item IDs, and player names.
