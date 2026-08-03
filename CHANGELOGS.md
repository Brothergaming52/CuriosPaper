# Changelogs

## v2.0.1

**Release Date:** 2026-08-03

### 📱 Bedrock Edition & Geyser Integration

CuriosPaper now includes native container support and automatic detection for Bedrock Edition players connecting via GeyserMC and Floodgate:

- **Automatic Geyser Detection & Setup (`BedrockUtil`):** Detects Bedrock players via Floodgate API, Geyser API, or offline Floodgate UUID prefixes (`00000000-0000-0000-...`). On startup, CuriosPaper automatically extracts and copies `CuriosPaper_Geyser.zip` and `CuriosPaper_mappings.json` into Geyser's `packs/` and `custom_mappings/` folders.
- **Custom Bedrock Anvil GUI (`BedrockAnvilGUI`):** Bedrock players opening an Anvil receive a dedicated 3-row Chest GUI allowing combination, repair, and renaming with accurate XP cost and level calculation.
- **Custom Bedrock Smithing GUI (`BedrockSmithingGUI`):** Bedrock players opening a Smithing Table receive a dedicated 3-row Chest GUI supporting 1.20+ Template Smithing (Template + Base + Addition) and pre-1.20 legacy Smithing (Base + Addition).
- **New Feature Config Toggles:** Added `features.use-custom-anvil-gui` (default: `true`) and `features.use-custom-smithing-gui` (default: `true`) in `config.yml`.

### 📊 Attribute Modifier Operations & Lore Formatting

- **Attribute Operations Support:** `AbilityData` and `AbilityEditorGUI` now support three distinct attribute modifier operations:
  - `ADD_NUMBER` — Flat value addition/subtraction (e.g. `+5` or `-5`).
  - `ADD_SCALAR` — Percentage scalar based on base attribute (e.g. `+0.20` formats as `+20%`, `-0.15` formats as `-15%`).
  - `MULTIPLY_SCALAR_1` — Multiplier size (e.g. `1.20` formats as `x1.20`).
- **Lore Formatting & Message Customization:** Updated `updateAbilityLore` to format positive and negative values according to the operation type, adding `%` for scalar operations and `x` multiplier prefixes for multiply operations.
- **New Message Keys in `messages.yml`:** Added `items.ability-attribute-scalar-positive`, `items.ability-attribute-scalar-negative`, `items.ability-attribute-multiply-positive`, and `items.ability-attribute-multiply-negative`.

### ✈️ 3D Model Display (ArmorStand) Improvements & Dismount Safety

- **Flight & Spectator Model Auto-Hiding:** ArmorStand models automatically hide when players toggle flight (`isFlying()`, `PlayerToggleFlightEvent`), enter Spectator mode (`GameMode.SPECTATOR`), or perform special poses (`Pose.SWIMMING`, `Pose.FALL_FLYING`, `Pose.SPIN_ATTACK`).
- **Inventory Dismount Safety:** 3D models temporarily dismount when players open any inventory (`InventoryOpenEvent`) and automatically remount 2 ticks after inventory closure (`InventoryCloseEvent`), preventing visual model overlaps in GUIs.

### 🛠️ Compatibility, Performance & Bug Fixes

- **Flexible Version Parsing (`VersionUtil`):** Upgraded version parsing regex (`(\\d+)\\.(\\d+)(?:\\.(\\d+))?`) to gracefully handle non-standard Paper build strings (e.g. Paper dev builds like `26.2.build.48-alpha` or custom server forks).
- **Case-Insensitive GUI Title Matching (`AccessoryGUI`):** Fixed title comparison logic to use color-stripped case-insensitive matching (`equalsIgnoreCase`), preventing GUI title bugs caused by legacy color codes.
- **Give Command Target Message Fix (`CuriosCommand`):** Fixed output message target calculation when giving items to other players.

---

## v2.0.0

**Release Date:** 2026-07-01

### 🗄️ Multi-Database Storage Support

CuriosPaper now supports multiple robust database engines for player accessory storage alongside standard flat-file YAML:

- **SQLite Backend:** Perfect for single-server setups, storing accessory data in a local, optimized database file (`storage/curiospaper.db`).
- **MySQL Backend:** Recommended for BungeeCord/Velocity proxy networks to share accessory inventories across multiple server instances with HikariCP connection pooling.
- **MongoDB Backend:** Recommended for large-scale deployments, using a native document-oriented Mongo client.
- **Automatic Migration:** On first boot with a database backend configured, CuriosPaper will automatically parse and migrate all legacy `.yml` files in the `playerdata/` folder into the database tables, renaming migrated files to `.yml.migrated`.

### 🧬 Dynamic Resource Pack & Model Customization

- **Programmatic Custom Model Overrides:** Added `CuriosPaperAPI#registerItemModelOverride(String material, int customModelData, String modelPath)` allowing other plugins to dynamically inject model predicates (e.g. into `carrot_on_a_stick.json`) without editing raw assets by hand.
- **Combined Elytra Asset Generator:** If a custom Elytra item (possessing a custom equippable asset ID) is placed in the back slot, CuriosPaper scans the resource pack and automatically builds merged humanoid/wings JSON files combining chestplate textures with the custom wings layer. This ensures proper custom textures are displayed on the player model during flight.

### 🔌 API & Event System Extensions

- **New PlayerDeathCurioDropEvent:** Fired when a player dies. Implements `Cancellable`, permitting developers to prevent specific accessories from dropping on death, keeping them equipped (e.g., for keep-inventory perks or specific soulbound items).
- **Addon Storage API Hook:** Added `CuriosPaperAPI#getStorageAPI()` which returns the `CuriosStorageAPI` instance. This allows third-party addon plugins to save, query, and delete custom data asynchronously using CuriosPaper's configured database backend.

### ⚡ Performance & Quality of Life

- **Asynchronous Resource Pack Rebuilds:** The `/curios rp rebuild` command execution has been offloaded to an asynchronous Bukkit task to prevent main-thread freezing on servers with large packs or extensive asset overrides.
- **Enhanced Elytra Slot Listeners:** Added additional `InventoryDragEvent` and `InventoryCloseEvent` logic to protect slot 38 (armor slot) from ghost items, duplications, and glitched secret elytras.
- **Paper AsyncScheduler Support:** Uses Paper's native region-aware threaded `AsyncScheduler` via reflection on Minecraft 1.20.6+ servers, falling back to standard Bukkit schedulers on older servers.
- **Improved JVM Version Detection:** Version utility improved to correctly resolve Java execution runtimes for compatibility handling.
- **Mob Drop Model Cleanup:** Fixed passenger armor stands occasionally leaving persistent ghost stands upon entity death.

---

## v1.3.2

**Release Date:** 2026-06-11

### 🛠️ Administrative Improvements

#### Admin Inspect Command (`/curios inspect`)
- **NEW:** Added `/curios inspect <player> [slot]` to inspect and manage other players' accessories (works for both online and offline players).
- If no slot is specified, it opens a read-only **Overview GUI** showcasing all equipped slots and their item counts. Admins can click a slot button to open the detailed slot GUI.
- If a slot is specified, opens a **Slot Detail GUI** where admins can add, remove, or swap accessories directly. Saving automatically invokes equip events and updates 3D models for online players.
- Requires `curiospaper.admin` permission.

#### Custom NBT & Enchants Editor GUI
- **NEW:** Added an **NBT & Enchants Editor** button (Command Block) in the main Custom Item Editor (slot 21).
- **Manage NBT (PDC Keys):** Interactively add or delete custom NBT tags stored in the item's Persistent Data Container (PDC) via simple chat input (`key = type:value`, e.g., `myplugin:power = int:42`).
- **NBT Key Suggester:** Browse and select existing NBT keys from standard Minecraft data components, other custom items, or the admin's inventory.
- Supported PDC types: `string`, `int`, `double`, `float`, `byte`, `short`, `long`.
- **Manage Enchantments:** Add or remove enchantments on the custom item with configurable levels.
- **Hide Enchantments:** Toggle whether enchantment tooltips are hidden from lore (glint only) via `ItemFlag.HIDE_ENCHANTS`.
- **Unbreakable Toggle:** Easily make the custom item unbreakable.
- **Placeable Toggle:** Prevent players from placing custom blocks or custom player heads on the ground (cancelled via `BlockPlaceEvent`).

### 📦 Compatibility & Networking

#### Random Teleport (RTP) Compatibility / Dismount System
- **NEW:** Added a temporary dismount mechanism to resolve passenger teleportation failures during random teleports (RTP) or portal transitions.
- **RTP Command Recording:** Interactively record sequences (commands, stepped-on blocks, clicked levers/buttons, entity/NPC clicks, and GUI clicks) using `/curios recordrtp`.
- When a player triggers an RTP interaction, their 3D model armor stands are temporarily dismounted. Once the teleport completes and the player moves again, the armor stands are seamlessly remounted at the destination.
- Configurable under `features.rtp` in `config.yml`. Can be disabled entirely via `features.rtp.enabled: false`.

#### Resource Pack Hosting Modes
- **NEW:** Added `resource-pack.mode` setting supporting three hosting modes:
  - `SELF` — Host the pack locally on the Minecraft server using the built-in Netty HTTP server.
  - `LINK` — Provide a direct download link (`resource-pack.url`) for external hosting.
  - `NONE` — Completely disable automatic resource pack hosting/delivery.
- **Query Parameter Cache-Busting:** On player join, the resource pack URL is automatically appended with `?v=<hash>` (or `&v=<hash>` if a query string already exists) to bust the client cache and force the Minecraft client to redownload the resource pack when it is updated.

### 🧪 Crafting & Recipe Logic

#### Strict Crafting with ExactChoice
- Upgraded custom recipe ingredient resolution to use `RecipeChoice.ExactChoice` for shaped, furnace, smoker, blasting, campfire, smithing, and shapeless recipes (if supported by server).
- This ensures recipes strictly require the exact custom Curios item instead of just any item matching the base material.
- Added strict crafting protection: custom Curios items can no longer be used in vanilla recipes (clears craft results or cancels `CraftItemEvent`/`PrepareItemCraftEvent`).

### ⚡ Performance & API Enhancements

#### Ability Modifier Reconciliation Task
- **NEW:** Added a background task (`ModifierReconciliationTask`) that runs every 5 seconds to automatically detect and remove orphaned/stale attribute modifiers from players.
- Fully strips all CuriosPaper-related modifiers (`curiospaper_ability_`) from players on plugin shutdown/reload to prevent persistent attributes across reloads.
- Upgraded item ID lookup in `AbilityListener` to prioritize PDC metadata (`curiospaper:item_id`) over display name lookup, making tracking much more robust.

#### Custom PLAYER_HEAD Skin Support
- **NEW:** Setting `item-model` to a base64 skin texture or skin URL on a `PLAYER_HEAD` custom item now automatically downloads and applies the texture using reflection (supporting 1.14-1.21+).
- Added `ItemStack createBase64Skull(String base64)` to `CuriosPaperAPI` for developers to programmatically create player heads with custom skins.

---

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
