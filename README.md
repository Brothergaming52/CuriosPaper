<div align="center">

<img src="https://cdn.modrinth.com/data/JLMPHrRw/images/907375d54ee434019af260f5a126f361d4add7b3.png" alt="CuriosPaper Banner" width="100%">

# ✦ CuriosPaper ✦

### The Ultimate Custom Accessory Inventory System for Paper & Spigot

[![Version](https://img.shields.io/badge/version-1.3.0-blueviolet?style=for-the-badge)](https://github.com/Brothergaming52/CuriosPaper/releases)
[![Minecraft](https://img.shields.io/badge/minecraft-1.14.4%20—%201.21+-green?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0xMiAyQzYuNSAyIDIgNi41IDIgMTJzNC41IDEwIDEwIDEwIDEwLTQuNSAxMC0xMFMxNy41IDIgMTIgMnoiLz48L3N2Zz4=)](https://www.spigotmc.org/)
[![Discord](https://img.shields.io/discord/1456137607569346739?label=Discord&style=for-the-badge&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/r5YXqgrGya)
[![bStats](https://img.shields.io/badge/bStats-live-blue?style=for-the-badge)](https://bstats.org/plugin/bukkit/CuriosPaper/29508)
[![Docs](https://img.shields.io/badge/docs-curiospaper.run.place-orange?style=for-the-badge)](http://www.curiospaper.run.place/)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Me-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/brothergaming52)

**Rings · Necklaces · Capes · Crowns · Belts · Charms — and more.**
<br>
Fully server-side. No mods required. Powerful developer API.

---

*Think of it as **Baubles / Curios** — but for Paper & Spigot, with 3D models, loot tables, GUI editors, and a full plugin API.*

</div>

---

## 🧩 What is CuriosPaper?

CuriosPaper adds a complete **accessory slot system** to your Minecraft server. Players can equip rings, necklaces, capes, belts, charms, and more through a clean, tiered GUI — no client mods required.

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
| ✨ | **Ability System** | Potion effects & attribute modifiers that activate when equipped |
| ⚡ | **Quick Equip** | **Shift + Right-Click** to instantly equip accessories from your hand |
| 🎹 | **Hotkey System** | Configurable sneak keybind to open the accessory GUI — no commands needed |
| 🪂 | **Elytra Back Slot** | Equip elytra in the back accessory slot (1.21.3+) |
| ☠️ | **Death Behavior** | Keep, drop, or auto-detect accessories on death — fully configurable |

### 🎨 3D Model System <sup>NEW in v1.3.0</sup>

Attach **custom 3D models** directly to the player's body when accessories are equipped. Models are rendered using synchronized invisible armor stands — visible to all nearby players.

- 🔄 Tracks player movement, rotation, sneaking, swimming, and flying
- 👁️ Smart visibility culling — hide models from the wearer's first-person view
- 🔱 Trident compatibility — models auto-hide during throws and riptide
- 🧟 Mob models — mobs can visually wear items they'll drop
- 🎮 Per-item toggle — players can right-click to show/hide their models

### 🛠️ In-Game GUI Editors

Create and configure everything visually — no YAML editing required:

| Editor | What It Does |
|---|---|
| **Item Editor** | Set name, material, lore, model data, slot type |
| **Ability Editor** | Configure potion effects & attribute modifiers with triggers |
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
| **Commands** | `/edit give <item> [player] [amount]` for direct distribution |

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
| `CuriosRecipeTransferEvent` | Custom item crafted via a recipe |

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
| **Dependencies** | None (standalone) |

> *Folia support is experimental

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
/edit create speed_ring          # Create a new item
```
In the GUI: set the name, material, slot type → add a Speed ability → add a recipe → done.

```
/edit give speed_ring             # Get the item
/baubles                          # Open accessory menu
```
Place it in a ring slot — Speed I activates! 🏃

---

## ⚙️ Configuration Highlights

```yaml
# Accessory slots — fully customizable
slots:
  ring:
    name: "Ring"
    icon: "GOLD_NUGGET"
    amount: 2

# Hotkey to open GUI without commands
features:
  hotkey:
    enabled: true
    slot: 8
    sneak-type: "double"       # single, double, or hold

  # What happens to accessories on death?
  keep-curio-inventory:
    type: "Auto"               # Always, Auto, or Never

# Resource pack with external pack merging
resource-pack:
  enabled: true
  combine-external-rp: false
```

Full configuration reference → [**curiospaper.run.place**](http://www.curiospaper.run.place/)

---

## 📖 Documentation

<div align="center">

### 🌐 [**curiospaper.run.place**](http://www.curiospaper.run.place/)

</div>

The full documentation site covers:

| Section | Topics |
|---|---|
| **Getting Started** | Installation, concepts, first accessory walkthrough |
| **Configuration** | Slots, features, abilities, performance & storage |
| **Systems** | Accessories, abilities, recipes, loot tables, mob drops, trades, 3D models, hotkeys, death behavior |
| **GUI Editors** | Item, ability, recipe, loot table, mob drop, trade, 3D model editors |
| **Developer API** | Getting the API, accessories, loot tables, 3D models, resource packs, events |
| **Examples** | Complete plugin examples with loot tables, abilities, and recipes |

---

## 🆕 What's New in v1.3.0

- ✨ **3D Model System** — Custom models mounted on the player's body via armor stands
- ⚡ **Quick Equip** — Shift+Right-Click to equip accessories instantly
- 🎹 **Hotkey System** — Single/double/hold sneak keybinds
- ☠️ **Keep Inventory on Death** — Always / Auto / Never modes
- 📋 **Loot Table Editor** — Full 3-screen GUI with browser, search, and presets
- 🧟 **Mob 3D Models** — Mobs visually wear items they can drop
- 📢 **New Events** — `CuriosLootGenerateEvent` & `CuriosMobDropEvent`
- 🔧 **15+ New API Methods** — 3D models, loot tables, resource pack assets, and more
- 📦 **External Resource Pack Merging** — Combine other plugins' packs into one

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
    <version>1.3.0</version>
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
