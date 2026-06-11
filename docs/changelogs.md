# Changelogs

See the full changelogs in the [CHANGELOGS.md](https://github.com/Brothergaming52/CuriosPaper/blob/main/CHANGELOGS.md) file.

## Latest: v1.3.2

### Highlights

- **Admin Inspect Command (`/curios inspect`)** — Admins can now inspect and manage any online or offline player's equipped accessories directly via the command or a 2-tier GUI.
- **NBT & Enchants Editor GUI** — Manage item PDC/NBT tags (`key = type:value`), edit enchants with levels, hide enchants (glint only), and toggle unbreakable and placeable flags in-game.
- **RTP Dismount Compatibility** — Interactively record portal/teleport triggers and automatically dismount 3D model stands to avoid passenger glitches.
- **Hosting Modes & Cache-Busting** — Configure resource pack delivery via `SELF` (embedded server), `LINK` (external host), or `NONE`, with automatic cache-busting query parameter injection.
- **Exact Choice Recipes** — Upgraded custom recipe resolution to use `ExactChoice` for perfect item ingredient matching, with strict vanilla crafting blocker.
- **Base64 Head Skins** — Native base64/URL skin texture support on `PLAYER_HEAD` custom accessories programmatically and via YAML.

## v1.3.1

### Highlights

- **Consolidated Commands** — The `/edit` command has been merged into `/curios` for a more streamlined administrative experience.
- **New Messaging System** — All plugin messages are now fully configurable and localizable in the new `messages.yml`.
- **Integrated Item Management** — New paginated GUIs for listing custom items, viewing recipes, and managing inventories directly from the `/curios list` command.
- **Enhanced Event System** — Added `CuriosCraftEvent` for final result modification and `CuriosModelEquipEvent` / `CuriosMobModelEquipEvent` for dynamic 3D model customization.
- **Auto-Update Checker** — Built-in version checking to keep your server up to date with the latest features and bug fixes.
- **Performance & Logic Refinement** — Significant refactoring of configuration management, item data handling, and 3D model synchronization.

### Breaking Changes

- `/edit` command has been removed; use `/curios <create|edit|delete|list|give>` instead.
- `curiospaper.edit` permission has been merged into `curiospaper.admin`.
- `config.yml` now focuses on functional settings, with all text moved to `messages.yml`.

## v1.3.0

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
