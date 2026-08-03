<div align="center">

<img src="https://cdn.modrinth.com/data/JLMPHrRw/images/907375d54ee434019af260f5a126f361d4add7b3.png" alt="CuriosPaper Banner" width="100%">

# ✦ CuriosPaper ✦

### The Ultimate Custom Accessory Inventory System for Paper & Spigot

[![Version](https://img.shields.io/badge/version-2.0.1-blueviolet?style=for-the-badge)](https://github.com/Brothergaming52/CuriosPaper/releases)
[![Minecraft](https://img.shields.io/badge/minecraft-1.14.4%20—%201.21+-green?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0xMiAyQzYuNSAyIDIgNi41IDIgMTJzNC41IDEwIDEwIDEwIDEwLTQuNSAxMC0xMFMxNy41IDIgMTIgMnoiLz48L3N2Zz4=)](https://www.spigotmc.org/)
[![Discord](https://img.shields.io/discord/1456137607569346739?label=Discord&style=for-the-badge&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/r5YXqgrGya)
[![bStats](https://img.shields.io/badge/bStats-live-blue?style=for-the-badge)](https://bstats.org/plugin/bukkit/CuriosPaper/29508)
[![Docs](https://img.shields.io/badge/docs-curiospaper.run.place-orange?style=for-the-badge)](http://www.curiospaper.run.place/)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Me-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/brothergaming52)

**Rings · Necklaces · Capes · Crowns · Belts · Charms — and more.**
<br>
Fully server-side. No mods required. Bedrock Edition compatible. Powerful developer API.

---

*Think of it as **Baubles / Curios** — but for Paper & Spigot, with 3D models, loot tables, GUI editors, Bedrock container support, and a full plugin API.*

</div>

---

## 🧩 What is CuriosPaper?

CuriosPaper adds a complete **accessory slot system** to your Minecraft server. Players can equip rings, necklaces, capes, belts, charms, and more through a clean, tiered GUI — with full support for Java and Bedrock Edition players.

<div align="center">

<!-- 📸 IMAGE: Main GUI screenshot -->
![Main Accessory Menu](https://cdn.modrinth.com/data/JLMPHrRw/images/fbd5dd0b5bc5446b5368a19e321b34585811f23f.png)

*The main accessory menu with all 9 slot types*

</div>

---

## ⚡ Features

### 🎒 Core Accessory System

| | Feature | Description |
|:---:|---|---|
| 💍 | **9 Slot Types** | Head, Necklace, Back, Body, Belt, Hands, Bracelet, Ring, Charm |
| ✨ | **Ability System** | Potion effects & player attribute modifiers with custom operations |
| ⚡ | **Quick Equip** | **Shift + Right-Click** to instantly equip accessories from your hand |
| 🎹 | **Hotkey System** | Configurable sneak keybind to open the accessory GUI — no commands needed |
| 🪂 | **Elytra Back Slot** | Equip elytra in the back accessory slot (1.21.3+) |
| ☠️ | **Death Behavior** | Keep, drop, or auto-detect accessories on death — fully configurable |
| 📱 | **Bedrock Support** | Native Geyser & Floodgate support with custom Anvil & Smithing GUIs |

### 🎨 3D Model System

Attach **custom 3D models** directly to the player's body when accessories are equipped. Models are rendered using synchronized invisible armor stands — visible to all nearby players.

- 🔄 Tracks player movement, rotation, sneaking, swimming, flying, and spectator mode
- 👁️ Smart visibility culling — hide models from wearer's first-person view or during flight/dismount
- 🔱 Trident & Elytra compatibility — models auto-hide during throws, riptide, and elytra flight
- 🧟 Mob models — mobs can visually wear items they'll drop
- 🎮 Per-item toggle — players can right-click to show/hide their models
- 🚪 GUI Dismount Protection — models temporarily dismount when opening inventories to prevent visual overlap

### 🛠️ In-Game GUI Editors

Create and configure everything visually — no manual YAML editing required:

| Editor | What It Does |
|---|---|
| **Item Editor** | Set name, material, lore, model data, slot type |
| **Ability Editor** | Configure potion effects & attribute modifiers with operations (`ADD_NUMBER`, `ADD_SCALAR`, `MULTIPLY_SCALAR_1`) |
| **Recipe Editor** | Shaped, shapeless, furnace, blast, smoker, anvil & smithing recipes |
| **Loot Table Editor** | Browse all server loot tables, add items to dungeon chests with presets |
| **Mob Drop Editor** | Configure which mobs drop your items and at what chance |
| **Trade Editor** | Set up villager trades with professions and price ranges |
| **3D Model Editor** | Configure body-mounted 3D models with pitch limits |

<!-- 📸 IMAGE: Tier 2 GUI screenshot -->
<div align="center">

![Slot Inventory](https://cdn.modrinth.com/data/JLMPHrRw/images/697c0f0ad8deb605b86cb18ac5295e6b030004ae.png)

*Slot inventory with accessories equipped*

</div>

### 📦 Item Distribution

| Channel | Description |
|---|---|
| **Crafting Recipes** | 7 recipe types — shaped, shapeless, furnace, blast, smoker, anvil, smithing |
| **Loot Tables** | Items appear in dungeon chests, mineshafts, temples, strongholds, and more |
| **Mob Drops** | Any mob type can drop accessories with configurable chance & amount |
| **Villager Trades** | Accessories appear in villager trade pools by profession and level |
| **Commands** | `/curios give <item> [player] [amount]` for direct distribution |

### 🔌 Developer API

A comprehensive API for other plugins to integrate with CuriosPaper:

```java
CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();

// Tag any item as an accessory
ItemStack ring = api.tagAccessoryItem(myItem, "ring");

// Check what a player is wearing
List<ItemStack> rings = api.getEquippedItems(player, "ring");
boolean hasRing = api.hasEquippedItems(player, "ring");
int count = api.countEquippedItems(player, "ring");

// Register slots, items, recipes, loot tables at runtime
api.registerSlot("earring", "Earring", "DIAMOND", 2);
api.registerItemLootTable("my_ring", new LootTableData("minecraft:chests/simple_dungeon", 0.25));
api.setItemModelConfig("my_cape", true, "LEATHER_HORSE_ARMOR", null, "myplugin:cape", 45f, 30f);

// Contribute resource pack assets from your plugin's JAR
api.registerResourcePackAssetsFromJar(myPlugin);
```

**Custom Events:**

| Event | When It Fires |
|---|---|
| `AccessoryEquipEvent` | Player equips, unequips, or swaps an accessory |
| `CuriosLootGenerateEvent` | Custom item generated in a loot container (cancellable) |
| `CuriosMobDropEvent` | Custom item dropped by a mob (cancellable) |
| `CuriosRecipeTransferEvent` | Custom item crafted via a recipe (data transfer) |
| `CuriosCraftEvent` | Custom item created via craft/smelt/smith (final) |
| `CuriosModelEquipEvent` | 3D model about to be displayed on player |
| `CuriosMobModelEquipEvent` | 3D model about to be displayed on mob |
| `PlayerDeathCurioDropEvent` | Fired on player death to filter/cancel curio drops |

---

## 🛠️ Configuration Showcase & Setup Instructions

<details>
<summary>⚙️ Click to Expand Setup & Configuration Files</summary>

### 1. Installation & Initial Setup
1. Download `CuriosPaper.jar` and place it into your server's `plugins/` directory.
2. Start the server to generate default configuration files (`config.yml`, `messages.yml`, `items.yml`, etc.).
3. Choose your desired **Resource Pack Delivery Mode** in `config.yml`:
   - `SELF`: CuriosPaper hosts the resource pack on an embedded HTTP server (`port: 8080`).
   - `LINK`: Serve your resource pack via an external URL (e.g. Modrinth, Dropbox, or web host).
   - `NONE`: Disable automatic resource pack prompt completely.

### 2. Bedrock & Geyser Integration Setup
CuriosPaper includes automatic detection and support for **Bedrock Edition** players connecting via **GeyserMC / Floodgate**.

- **Automatic Setup (Geyser on same server):**
  CuriosPaper automatically detects Geyser, extracts `CuriosPaper_Geyser.zip` and `CuriosPaper_mappings.json` into `plugins/CuriosPaper/geyser/`, and copies them to Geyser's `packs/` and `custom_mappings/` folders.
- **Bedrock Custom Containers:**
  Bedrock players cannot natively use Java Anvil/Smithing container packets without glitches. CuriosPaper provides native 3-row Chest GUIs (`BedrockAnvilGUI` and `BedrockSmithingGUI`) for Bedrock players, supporting 1.20+ template smithing and XP repair/renaming.

### 3. Main Plugin Config (`/plugins/CuriosPaper/config.yml`)

```yaml
# Configuration for CuriosPaper Custom Accessory Slots
version: "2.0.1"

# Update Checker
updates:
  check-for-updates: true

# Storage Engine Settings
storage:
  # Selected engine: YAML (flatfile), SQLITE, MYSQL, or MONGODB
  engine: "YAML"
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: ""
    table-prefix: "curios_"
  mongodb:
    connection-string: "mongodb://localhost:27017"
    database: "minecraft"
    collection: "curios_playerdata"

# Resource Pack Settings
resource-pack:
  # Mode: SELF (embedded HTTP host), LINK (external URL), or NONE
  mode: SELF
  port: 8080
  host-ip: "123.45.67.89"
  url: ""
  hash: ""
  force-pack: false
  prompt-message: "Please accept the resource pack to view custom accessories!"

# Feature Toggles
features:
  # Allow elytra equipping in back slots
  allow-elytra-on-back-slot: true

  # Custom Bedrock GUIs for Anvils and Smithing Tables
  use-custom-smithing-gui: true
  use-custom-anvil-gui: true

  # Display empty accessory slots in GUI
  show-empty-slots: true

  # Keep Curios on Death: Always, Auto (follows keepInventory), or Never
  keep-curio-inventory:
    type: "Auto"

  # Sneak keybind hotkey to open accessory menu
  hotkey:
    enabled: true
    slot: 8
    sneak-type: "double" # single, double, or hold
```

### 4. Messages Configuration (`/plugins/CuriosPaper/messages.yml`)

```yaml
# Message customization with legacy (&a) and HEX (<#FF5555>) color support
prefix: "&6[CuriosPaper] "

items:
  quick-equip-lore: "&8▶ Shift + Right Click to equip"
  slot-lore: "&6Slot: &e{slots}"
  ability-when-worn: "&7When worn:"
  ability-potion-effect: "&9{effect} {level}"
  
  # Attribute Modifier Operation Lore Formatting (v2.0.1)
  ability-attribute-positive: "&9+{value} {attribute}"
  ability-attribute-negative: "&c-{value} {attribute}"
  ability-attribute-scalar-positive: "&9+{value}% {attribute}"
  ability-attribute-scalar-negative: "&c-{value}% {attribute}"
  ability-attribute-multiply-positive: "&9x{value} {attribute}"
  ability-attribute-multiply-negative: "&cx{value} {attribute}"
```

</details>

---

## 🎮 How It Works

```
1. /baubles              →  Opens the accessory menu
2. Click a slot icon     →  Opens that slot's inventory
3. Place an accessory    →  Abilities activate instantly!
```

**Or use Quick Equip:** Hold a tagged item + **Shift + Right-Click** to equip instantly.

**Or use the Hotkey:** Select a hotbar slot + **Double-Sneak** to open the menu.

---

## 📊 Stats

<p align="center">
  <a href="https://bstats.org/plugin/bukkit/CuriosPaper/29508">
    <img src="https://bstats.org/signatures/bukkit/CuriosPaper.svg" width="500">
  </a>
</p>

---

## 🔧 Compatibility

| | Requirements |
|---|---|
| **Minecraft** | 1.14.4 — 1.21+ |
| **Server** | Spigot, Paper, Purpur, Folia* |
| **Java** | 8+ |
| **GeyserMC** | Supported (custom Bedrock resource pack & mappings included) |
| **Dependencies** | None (standalone) |

> *Folia support is experimental

### 🌐 Geyser & Bedrock Edition Support

CuriosPaper provides full GUI slot icon and custom container support for Bedrock Edition players joining via GeyserMC.

- **Automatic Setup:** If Geyser is running on the same server, CuriosPaper will automatically detect Geyser's folder on startup, extract the mappings and resource pack, and copy them directly to Geyser's `packs/` and `custom_mappings/` folders.
- **Custom Bedrock GUIs (Anvil & Smithing):** Bedrock players get dedicated 3-row chest interfaces for Anvils (repair, rename, combine) and Smithing Tables (supports 1.20+ Netheriite templates and legacy recipes).
- **Manual Setup (Proxy/Separate Server):**
  1. Retrieve files from your server's `plugins/CuriosPaper/geyser/` directory.
  2. Copy `CuriosPaper_Geyser.zip` into your Geyser `packs/` folder.
  3. Copy `CuriosPaper_mappings.json` into your Geyser `custom_mappings/` folder.
  4. Ensure `enable-custom-content` is set to `true` in Geyser's `config.yml` and restart Geyser.

---

## 📥 Quick Start

```bash
# 1. Download CuriosPaper.jar from Modrinth or GitHub Releases
# 2. Drop it into your server's plugins/ folder
# 3. Start the server
# 4. Done! Use /baubles in-game
```

### First Accessory in 60 Seconds

```
/curios create speed_ring          # Create a new item
```
In the GUI: set the name, material, slot type → add a Speed ability → add a recipe → done.

```
/curios give speed_ring             # Get the item
/baubles                          # Open accessory menu
```
Place it in a ring slot — Speed I activates! 🏃

---

## 📖 Documentation

<div align="center">

### 🌐 [**curiospaper.run.place**](http://www.curiospaper.run.place/)

</div>

The full documentation site covers:

| Section | Topics |
|---|---|
| **Getting Started** | Installation, concepts, first accessory walkthrough |
| **Configuration** | Slots, features, abilities, performance, databases & Bedrock GUIs |
| **Systems** | Accessories, abilities, recipes, loot tables, mob drops, trades, 3D models, Bedrock support, hotkeys, death behavior |
| **GUI Editors** | Item, ability, recipe, loot table, mob drop, trade, 3D model editors |
| **Developer API** | Getting the API, accessories, loot tables, 3D models, resource packs, events |
| **Examples** | Complete plugin examples with loot tables, abilities, and recipes |

---

## 🆕 What's New in v2.0.1

- 📱 **Bedrock Anvil & Smithing GUIs** — Dedicated 3-row chest interfaces for Bedrock Edition players to access Anvil (combining, repairing, renaming with XP cost) and Smithing Tables (1.20+ templates & legacy smithing).
- 📊 **Attribute Modifier Operations** — Configure player modifier abilities with specific operations: `ADD_NUMBER` (flat value), `ADD_SCALAR` (percentage `%`), and `MULTIPLY_SCALAR_1` (multiplier `x`).
- ✈️ **Flight & Spectator Model Auto-Hiding** — ArmorStand 3D models automatically hide when players toggle flight (`isFlying`), enter spectator mode, or enter special poses (`SWIMMING`, `FALL_FLYING`, `SPIN_ATTACK`).
- 🚪 **Inventory Dismount Safety** — 3D models temporarily dismount when opening inventories and auto-remount 2 ticks after close to eliminate visual GUI overlaps.
- 🛠️ **Flexible Server Version Parsing** — Improved version parsing regex in `VersionUtil` for non-standard Paper dev builds (e.g. `26.2.build.48-alpha`).
- 💬 **Command & GUI Improvements** — Fixed `give` command messaging targeting and case-insensitive color-stripped GUI title matching.

Full changelog → [**CHANGELOGS.md**](CHANGELOGS.md)

---

## 💬 Community

<div align="center">

[![Discord](https://img.shields.io/discord/1456137607569346739?label=Join%20the%20Discord&style=for-the-badge&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/r5YXqgrGya)

**[discord.gg/r5YXqgrGya](https://discord.gg/r5YXqgrGya)**

Get help · Report bugs · Suggest features · Stay updated

</div>

---

## ☕ Support the Project

<div align="center">

If CuriosPaper has been useful to you, consider leaving a tip — it helps keep the project alive!

[![Ko-fi](https://img.shields.io/badge/Ko--fi-Buy%20me%20a%20coffee-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/brothergaming52)

**[ko-fi.com/brothergaming52](https://ko-fi.com/brothergaming52)**

</div>

---

## 🤝 For Developers

### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>2.0.1</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml
```yaml
depend: [CuriosPaper]
```

Full API docs → [**curiospaper.run.place/api**](http://www.curiospaper.run.place/api/)

---

<div align="center">

**Made with ❤️ by Brothergaming52**

[Documentation](http://www.curiospaper.run.place/) · [Discord](https://discord.gg/r5YXqgrGya) · [Ko-fi](https://ko-fi.com/brothergaming52) · [Changelog](CHANGELOGS.md) · [bStats](https://bstats.org/plugin/bukkit/CuriosPaper/29508) · [GitHub](https://github.com/Brothergaming52/CuriosPaper)

</div>
