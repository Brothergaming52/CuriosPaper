# GUI Overview

CuriosPaper uses a **two-tier GUI system** for the accessory inventory.

## Tier 1: Main Menu

Opened with `/baubles` (or `/b`, `/bbag`), the main menu shows all configured slot types in a 54-slot (double chest) inventory.

<!-- TODO: Add image - In-game screenshot of the main accessory GUI (Tier 1) showing all 9 slot type icons arranged in the double-chest with filler glass panes and border pattern -->
![Main accessory menu showing all 9 slot type icons](../images/main-gui-preview.png)

### Layout

- Each slot type is displayed as an icon item with its configured name and lore
- A decorative filler pattern surrounds the slot icons
- Clicking a slot type icon opens the Tier 2 view for that category

### Customization

The main menu appearance is controlled in `config.yml`:

```yaml
gui:
  main-title: "&8✦ Accessory Slots ✦"
  main-gui-size: 54
  filler-material: "GRAY_STAINED_GLASS_PANE"
  border-material: "BLACK_STAINED_GLASS_PANE"
  filler-name: "&r"
  use-patterns: true
```

## Tier 2: Slot Inventory

Clicking a slot type in the main menu opens the **Slot Inventory**, which shows the actual accessory slots for that category.

<!-- TODO: Add image - In-game screenshot of the Tier 2 slot inventory open for 'Ring Slots' showing 2 ring slots (one with an item equipped, one empty with placeholder icon) -->
![Back slot inventory with one item equipped](../images/gui-comparison.png)

### Features

- Displays the configured number of slots for that type (e.g., 2 for rings, 4 for charms)
- Empty slots show a placeholder icon if `show-empty-slots` is enabled
- Players can **click to place** or **click to remove** accessories
- Slot validation ensures only correctly tagged items can be placed

### Interaction

| Action | Result |
|---|---|
| Click empty slot with tagged item on cursor | Equip the accessory |
| Click equipped accessory | Unequip (return to cursor) |
| Click equipped accessory with different tagged item | Swap accessories |

## Sound Effects

The GUI can play sounds for various actions:

```yaml
features:
  play-gui-sound: true
  gui-sound: "BLOCK_CHEST_OPEN"

  play-equip-sound: true
  equip-sound: "ENTITY_ITEM_PICKUP"

  play-unequip-sound: true
  unequip-sound: "ENTITY_ITEM_PICKUP"
```
