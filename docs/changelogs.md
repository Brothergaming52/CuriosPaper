# Changelogs

See the full changelogs in the [CHANGELOGS.md](https://github.com/Brothergaming52/CuriosPaper/blob/main/CHANGELOGS.md) file.

## Latest: v1.3.0

### Highlights

- **3D Model System** — Attach custom 3D models to player bodies when accessories are equipped, using synchronized armor stands
- **Quick Equip** — Shift+Right-Click to instantly equip accessories without opening the GUI
- **Hotkey System** — Configurable sneak-based keybind to open the accessory GUI (single/double/hold modes)
- **Keep Inventory on Death** — Configurable behavior (Always/Auto/Never) for curio slots on player death
- **Loot Table Editor** — Complete 3-screen GUI editor for managing loot table entries with browser and quick-config
- **3D Model Config GUIs** — In-game editors for configuring 3D models on items and mob drops
- **New Events** — `CuriosLootGenerateEvent` and `CuriosMobDropEvent` for intercepting loot and drops
- **Expanded API** — 15+ new methods for 3D models, loot tables, mob drops, villager trades, resource pack assets, and more

### Breaking Changes

- `gui.main-slots` and `gui.main-layout` config options have been removed (layout is now auto-calculated or managed via `/curios editmenu`)
- Slot display names simplified (Unicode decorations removed from defaults)
- Maven dependency version should be updated from `1.2.0` to `1.3.0`

## v1.2.0

Initial public release with core accessory system, slot management, ability system, recipe system, mob drop system, villager trade system, resource pack generation, and developer API.
