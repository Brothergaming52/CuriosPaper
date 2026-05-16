# Changelogs

## v1.3.1

**Release Date:** 2026-05-16

### 🛠️ Administrative Improvements

#### Consolidated Command System
- **NEW:** The `/edit` command has been merged into `/curios` for a more unified administrative experience.
- All management actions are now subcommands of `/curios`:
  - `/curios list` — Opens the new Paginated Item Browser
  - `/curios create <id>` — Create a new custom item
  - `/curios edit <id>` — Open the editor for an existing item
  - `/curios delete <id>` — Delete a custom item
  - `/curios give <id> [player] [amount]` — Distribute custom items
  - `/curios reload` — Reload configuration and messages
- Command aliases updated: `/cp`, `/curiospaper`.
- Permission node `curiospaper.edit` has been merged into `curiospaper.admin`.

#### Paginated Item Browser (`ItemListGUI`)
- **NEW:** A dedicated GUI to browse all custom items in a paginated view.
- Supports left-click to view item recipes and right-click to jump directly into the item editor.
- Navigation buttons for previous/next pages and a close button.

#### Localization & Messaging (`MessagesManager`)
- **NEW:** Added `messages.yml` for full customization of all plugin messages and GUI titles.
- Supports Hex colors and legacy color codes.
- Messages can be reloaded on the fly using `/curios reload`.

#### Auto-Update Checker (`UpdateChecker`)
- **NEW:** Built-in update checker that notifies administrators of new releases on startup and login.
- Can be toggled in `config.yml`.

### ✨ API & Event Enhancements

#### New Events
- **NEW:** `CuriosCraftEvent` — Fired when a custom item is created via crafting, smelting, smithing, or anvil repair. Allows final modification of the result item.
- **NEW:** `CuriosModelEquipEvent` — Fired when a 3D model is about to be displayed on a player. Allows modifying the model material, CMD, or item model on the fly.
- **NEW:** `CuriosMobModelEquipEvent` — Fired when a 3D model is about to be displayed on a mob.

#### API Improvements
- Added `CuriosPaperAPI#reload()` to trigger a full plugin reload from other plugins.
- Enhanced `ItemData` with visibility flags to support hidden items in the browser.

### ⚙️ Configuration Changes

#### New Config File: `messages.yml`
- Contains all user-facing strings, categorized by system (commands, GUI, errors, etc.).

#### Updated `config.yml`
- Added `features.update-checker` toggle.
- Removed hardcoded message strings (moved to `messages.yml`).

### 🐛 Bug Fixes & Refinement
- Fixed an issue where the Elytra model would occasionally fail to sync after teleportation.
- Improved `ModelStandManager` performance during high-frequency movement.
- Standardized all GUI titles to use the new messaging system.
- Refactored `CuriosCommand` to handle the expanded subcommand set with better tab-completion.

---

## v1.3.0

**Release Date:** 2026-04-27

### ✨ New Features

#### 3D Model System (`ModelStandManager`)
- **NEW:** Full 3D model rendering system using invisible armor stands mounted on players
- Body-mounted 3D models appear when an accessory is equipped in a curios slot
- Configurable pitch up/down limits to auto-hide models from the wearer's view
- Handles player movement, rotation, sneaking, swimming, gliding, and teleportation
- Automatic scale synchronization when the player is shrunk or enlarged
- Trident compatibility — models are temporarily removed during trident throws and riptide launches, then restored
- Models are protected from damage, targeting, and player interaction
- Automatic cleanup on player death, disconnect, world change, and game mode change
- Players can toggle 3D model visibility per-item by right-clicking an equipped accessory in the GUI

#### Quick Equip System (`QuickEquipListener`)
- **NEW:** Shift + Right-Click while holding a tagged accessory item to instantly equip it into the first available slot
- Supports multi-slot tags (e.g., items tagged for `ring, charm` will try each slot type)
- Plays equip sound and sends confirmation message
- Cancels the interact event to prevent unintended block placement

#### Accessory Hotkey System (`AccessoryHotkeyListener`)
- **NEW:** Configurable hotkey to open the accessory GUI without commands
- Three sneak detection modes:
  - `single` — Open on a single sneak toggle
  - `double` — Open on double-sneak within 500ms
  - `hold` — Open when sneaking is held for a configurable duration
- Requires a specific hotbar slot to be selected (configurable, default slot 9)
- Fully configurable via `config.yml` under `features.hotkey`

#### Keep Curio Inventory on Death (`PlayerDeathListener`)
- **NEW:** Configurable behavior for curio inventory on player death
- Three modes:
  - `Always` — Always keep curio inventory, even when `keepInventory` gamerule is `false`
  - `Auto` — Follow the vanilla `keepInventory` gamerule (default)
  - `Never` — Always drop curio inventory, even when `keepInventory` is `true`
- Configured via `features.keep-curio-inventory.type` in `config.yml`

#### Loot Table Editor GUI (Complete Rewrite of `LootTableBrowser`)
- **NEW:** Full 3-screen loot table editor GUI:
  1. **Main Screen** — Lists existing loot table entries with Add, Edit, Delete, Save, and Back buttons
  2. **Browser Screen** — Paginated, searchable browser of all server loot table keys with filtering and refresh
  3. **Quick Config Screen** — Preset chance/amount selection (10%, 25%, 50%, 100%) and custom chat input
- Consistent visual style with gray glass filler, black bottom bar, and `§8` title prefixes
- Selection-based editing: click an entry to select it, then use Edit or Delete buttons

#### 3D Model Config GUI (`ModelConfigGUI`)
- **NEW:** In-game GUI editor for configuring 3D model settings on custom items
- Accessible from the Edit GUI via the Armor Stand button (slot 43)
- Settings include: toggle enable/disable, model material, CustomModelData, item model component (1.21.4+), pitch up limit, pitch down limit
- Live preview of configured model in the header slot

#### Mob Drop Model Config GUI (`MobDropModelConfigGUI`)
- **NEW:** In-game GUI editor for configuring 3D models that mobs wear when they spawn with a custom item drop
- Accessible from the Mob Drop Editor
- Settings include: toggle enable/disable, model material, CustomModelData, item model component

#### Custom Events
- **NEW:** `CuriosLootGenerateEvent` — Fired when a custom item is generated in a loot table (chest, barrel, brushable block). Cancellable, allows modifying the generated item.
- **NEW:** `CuriosMobDropEvent` — Fired when a custom item is dropped by a mob. Cancellable, allows modifying the dropped item.

### 🔧 API Additions

#### New Methods on `CuriosPaperAPI`

| Method | Description |
|---|---|
| `countEquippedItems(UUID, String)` | Count non-empty slots for a player by UUID |
| `registerSlot(... defaultSlotPosition)` | Register a custom slot type with an optional default GUI position |
| `unregisterSlot(String)` | Remove a custom slot type at runtime |
| `registerItemRecipe(String, RecipeData)` | Register a crafting recipe for a custom item |
| `registerItemLootTable(String, LootTableData)` | Register a loot table entry for a custom item |
| `registerItemMobDrop(String, MobDropData)` | Register a mob drop for a custom item |
| `registerItemVillagerTrade(String, VillagerTradeData)` | Register a villager trade for a custom item |
| `getItemData(String)` | Get the `ItemData` for a custom item |
| `createItem(String)` | Create a new custom item |
| `createItem(Plugin, String)` | Create a new custom item with plugin ownership |
| `saveItemData(String)` | Save item data to disk |
| `deleteItem(String)` | Delete a custom item |
| `setItemModelConfig(...)` | Configure 3D model settings for an item (enabled, material, CMD, item model, pitch limits) |
| `setMobDropModelConfig(...)` | Configure 3D model settings for a mob drop entry |
| `registerResourcePackAssets(Plugin, File)` | Register a folder of resource pack assets to be merged into the generated pack |
| `registerResourcePackAssetsFromJar(Plugin)` | Extract and register resource pack assets from a plugin's JAR `resources/` folder |

#### New Data Classes
- `LootTableData` — `lootTableType`, `chance`, `minAmount`, `maxAmount` with serialization and validation
- `MobDropData` — Now includes `modelEnabled`, `modelItem`, `modelCustomModelData`, `modelItemModel` fields
- `ItemData` — Now includes `modelEnabled`, `modelItem`, `modelCustomModelData`, `modelItemModel`, `pitchUpLimit`, `pitchDownLimit` fields

### 🖥️ GUI Changes

- **Loot Table Browser** completely rewritten to a 3-screen editor (Main → Browser → Quick Config)
- **Mob Drop Editor** now includes a 3D Model button for configuring mob visual models
- **Edit GUI** now includes a 3D Model Settings button (Armor Stand icon at slot 43)
- **Edit Menu GUI** — Slot rearrangement logic improved with layout position fallback

### ⚙️ Configuration Changes

#### New Config Options
```yaml
features:
  hotkey:
    enabled: true
    slot: 8                    # Hotbar slot (1-9) that must be selected
    sneak-type: "double"       # Options: single, double, hold
    sneak-hold-duration: 3     # Seconds to hold sneak (for 'hold' type)

  keep-curio-inventory:
    type: "Auto"               # Options: Always, Auto, Never

resource-pack:
  combine-external-rp: false   # Merge external resource pack ZIPs into the main pack
```

#### Changed Config Values
- Slot display names simplified (removed decorative Unicode symbols):
  - `"&e⚜ Head Slot ⚜"` → `"Head"`
  - `"&b✦ Necklace Slot ✦"` → `"Necklace"`
  - `"&5☾ Back Slot ☾"` → `"Back"`
  - `"&c❖ Body Slot ❖"` → `"Body"`
  - `"&6⚔ Belt Slot ⚔"` → `"Belt"`
  - `"&f✋ Hand Slots ✋"` → `"Hands"`
  - `"&3◈ Bracelet Slots ◈"` → `"Bracelet"`
  - `"&6◆ Ring Slots ◆"` → `"Ring"`
  - `"&d✧ Charm Slots ✧"` → `"Charm"`
- Main GUI title simplified: `"§8✦ Accessory Slots ✦"` → `"§8Accessory Slots"`
- `allow-minecraft-namespace` changed from `false` → `true`

#### Removed Config Options
- `gui.main-slots` — GUI size is now automatically calculated
- `gui.main-layout` — Layout is now managed via the `/curios editmenu` command or automatic placement

### 🐛 Bug Fixes & Improvements

- **Loot Table Listener** — Enhanced with `CuriosLootGenerateEvent` firing, improved loot injection logic
- **Mob Drop Listener** — Enhanced with `CuriosMobDropEvent` firing, improved drop logic
- **Resource Pack Manager** — Improved external resource pack combining support
- **Version Utility** — Expanded version compatibility utilities
- **Code Formatting** — Entire codebase reformatted from 4-space to 2-space indentation for consistency
- **Auto-save task** — Minor improvements

### 📄 Documentation

- **NEW:** `docs/systems/3d-model-system.md` — Documentation for the 3D model system
- **NEW:** `docs/gui-editors/3d-model-editor.md` — Documentation for the 3D model editor GUI
- Updated `mkdocs.yml` navigation to include new pages
- Updated `docs/gui-editors/index.md` with new editor entries
- Updated `docs/api/creating-accessories.md` with new API references

### 📦 Build

- Version bumped from `1.2.0` → `1.3.0` in `pom.xml`
- Updated `.gitignore`

---

## v1.2.0

Initial public release with core accessory system, slot management, ability system, recipe system, mob drop system, villager trade system, resource pack generation, and developer API.
