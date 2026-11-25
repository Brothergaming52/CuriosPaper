<div align="center">

<img src="https://cdn.modrinth.com/data/JLMPHrRw/images/907375d54ee434019af260f5a126f361d4add7b3.png" alt="CuriosPaper Banner Logo" width="100%">

# ✦ CuriosPaper ✦

**Modern Accessory & Curios Inventory Engine for Paper & Folia 1.20.6+**

[![CuriosPaper Version 3.0](https://img.shields.io/badge/version-3.0-blueviolet?style=for-the-badge)](https://github.com/Brothergaming52/CuriosPaper/releases)
[![Paper / Folia 1.20.6 to 1.21+](https://img.shields.io/badge/Paper%20%7C%20Folia-1.20.6%20--%201.21+-green?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCI+PHBhdGggZmlsbD0id2hpdGUiIGQ9Ik0xMiAyQzYuNSAyIDIgNi41IDIgMTJzNC41IDEwIDEwIDEwLTQuNSAxMC0xMFMxNy41IDIgMTIgMnoiLz48L3N2Zz4=)](https://papermc.io/)
[![Discord Community](https://img.shields.io/discord/1456137607569346739?label=Discord&style=for-the-badge&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/r5YXqgrGya)
[![bStats Live Metrics](https://img.shields.io/badge/bStats-live-blue?style=for-the-badge)](https://bstats.org/plugin/bukkit/CuriosPaper/29508)
[![Support on Ko-fi](https://img.shields.io/badge/Ko--fi-Support%20Me-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/brothergaming52)

**Rings · Necklaces · Capes · Crowns · Belts · Charms · Gloves · Bracelets**
<br>
Server-side accessory inventory with packet 3D models, modular item engine support, and native Folia multi-threading.
<br>
Pure modern Paper & Folia API. Zero NMS reliance. Zero client mods required.

---

*Think of it as **Baubles / Curios** for Paper and Folia 1.20.6+, featuring clientbound packet 3D models, modular item engine integrations (Synthetix — not yet approved on Modrinth, join [Discord](https://discord.gg/r5YXqgrGya) for access; Nexo, ItemsAdder, Oraxen, CraftEngine), Citizens NPC support, and a developer API.*

</div>

> **Documentation Notice:** The online documentation site (`curiospaper.run.place`) is currently unavailable while undergoing restructuring. Please refer to this README and join our [Discord Community](https://discord.gg/r5YXqgrGya) for setup guides, questions, and support.

---

## 🧩 What is CuriosPaper?

CuriosPaper provides a customizable **accessory inventory engine** for Minecraft servers running Paper, Purpur, or Folia. Players can equip rings, necklaces, capes, belts, charms, and other accessories through a clean, tiered GUI. Version 3.0 delegates custom item creation to popular item plugins while handling accessory slots, packet-based 3D models, and equip mechanics with zero-overhead regional thread scheduling.

<div align="center">

![Main Accessory Menu](https://cdn.modrinth.com/data/JLMPHrRw/images/ba1dd02389e84a1308e8df35ccb36f4948b112c2.png)

*The main accessory menu with custom slot types*

</div>

---

## ⚡ Features Overview

| | Feature | Description |
|:---:|---|---|
| ⚡ | **Folia Multi-Threading** | Native support for Folia multi-threaded regional ticking via universal `PlatformScheduler`. Entity mutations and flight checks dispatch directly to owning regional threads with zero cross-region desyncs. |
| 💍 | **9 Slot Types** | Head, Necklace, Back, Body, Belt, Hands, Bracelet, Ring, and Charm |
| 📦 | **Item Engine Support** | Works with Synthetix *(join [Discord](https://discord.gg/r5YXqgrGya) — not yet approved on Modrinth)*, Nexo, ItemsAdder, Oraxen, CraftEngine, or vanilla items |
| 🎭 | **Packet 3D Models** | Clientbound packet armor stands (`PacketModelManager`) without persistent server entities |
| ⚡ | **Quick Equip** | **Shift + Right-Click** to equip accessories directly from hand or hotbar |
| 🎹 | **Universal Keybinds** | Sneak double-tap, single sneak, or hold sneak via Synthetix Keybind API *(join [Discord](https://discord.gg/r5YXqgrGya) for Synthetix)* |
| 🪂 | **Elytra Back Slot** | Equip elytra in back slots with isolated flight logic |
| ☠️ | **Death Behavior** | Configurable `Auto` (respects `keepInventory` and graves), `Always`, or `Never` drop on death |
| 👥 | **Citizens NPC Support** | Equip accessories and render 3D models on Citizens NPCs via `/curios npc` |
| 🧩 | **PlaceholderAPI** | Placeholders for equipped item names, counts, and slot status |
| 🌐 | **Multi-Language** | Adventure MiniMessage formatting (`<gradient>`, `<rainbow>`, hex) in `languages/en_US.yml` |

<div align="center">

![Slot Inventory](https://cdn.modrinth.com/data/JLMPHrRw/images/b0242820880d1239aa2c896fc7453c470de0c752.png)

*Slot inventory view with accessories equipped*

</div>

<details>
<summary>🎨 <b>Click to Expand: 3D Model System Details</b></summary>

Attach **custom 3D models** directly to the player's body when accessories are equipped. CuriosPaper 3.0 renders models using **clientbound packets**, avoiding persistent armor stand entities in the world:

- 🔄 Synchronizes with movement, sneaking, swimming, flying, and spectator mode.
- 👁️ Pitch-angle culling hides models from the wearer's first-person view.
- 🔱 Trident & Elytra compatibility temporarily hides models during throws, riptide, and gliding.
- 🧟 Supports mob and Citizens NPC models.
- 🚪 Automatically hides models while inventories are open to prevent GUI visual overlap.
- ⚙️ Rendering modes in `config.yml`: `auto` (packet mode with fallback), `packet`, or `entity`.

<div align="center">

![Packet 3D Models](https://cdn.modrinth.com/data/JLMPHrRw/images/e0a9fbdba61bb46dd3dacb6a92c1927f53e48daf.png)

*In-game packet 3D body-mounted models*

</div>

</details>

<details>
<summary>📦 <b>Click to Expand: Item Engine Integrations & Resource Pack Info</b></summary>

CuriosPaper 3.0 delegates item creation, recipes, and abilities to specialized item engines:

| Engine | Integration Highlights |
|---|---|
| **Synthetix** | Native integration, unified `AddonDataStore` database support, Universal Keybinds hook *(Not yet approved on Modrinth — join our [Discord](https://discord.gg/r5YXqgrGya) to get it!)* |
| **Nexo** | Automatic custom item detection, custom model data resolution, slot mapping |
| **ItemsAdder** | Automatic ItemsAdder item and equippable model resolution |
| **Oraxen** | Automatic Oraxen item detection, model mapping, and attribute synchronization |
| **CraftEngine** | Modular custom item support with automatic slot detection |
| **Vanilla / In-Game** | Tag any item or vanilla material in-game using `/curios tag <slot>` |

**Zero-Port Resource Pack Injection:**
CuriosPaper extracts its custom GUI icons and equipment assets on boot and registers them directly into your server's active pack host (`Synthetix` → `Nexo` → `ItemsAdder` → `Oraxen` → `CraftEngine` → `RSPM`). No secondary HTTP ports or firewall rules are required.

</details>

<details>
<summary>🔌 <b>Click to Expand: Developer API & Custom Events</b></summary>

A thread-safe API for external plugins:

```java
CuriosPaperAPI api = CuriosPaperAPI.get();

// Check what a player is wearing
boolean hasRing = api.hasEquipped(player, "ring", "synthetix:ruby_ring");
List<ItemStack> rings = api.getEquippedItems(player, "ring");
List<ItemStack> allCurios = api.getAllEquippedList(player);

// Tag an ItemStack as an accessory
ItemStack taggedRing = api.tagAccessoryItem(myItem, "ring");

// Programmatically equip accessories
api.setEquippedItem(player, "ring", 0, taggedRing);

// Equip 3D models onto mobs or NPCs
api.equipMobModel(zombie, "head", crownItem);

// Access the runtime slot registry
SlotRegistry registry = api.getSlotRegistry();
```

**Custom Events:**

| Event | When It Fires |
|---|---|
| `AccessoryEquipEvent` | Fired when an accessory is equipped, unequipped, or swapped |
| `CuriosModelEquipEvent` | Fired before a 3D model is displayed on a player (allows modifying model/CMD) |
| `CuriosMobModelEquipEvent` | Fired before a 3D model is displayed on a mob or NPC |
| `PlayerDeathCurioDropEvent` | Fired on death to filter, keep, or modify which accessories drop |

</details>

---

## ⌨️ Commands & Permissions

<details>
<summary>📋 <b>Click to Expand: Commands & Permissions Table</b></summary>

### Commands

| Command | Permission | Description |
|---|---|---|
| `/curios open [player]` | `curiospaper.use` / `curiospaper.admin` | Opens accessory menu for yourself or another player |
| `/baubles` | `curiospaper.use` | Shortcut to open your accessory inventory |
| `/curios inspect <player>` | `curiospaper.admin` | Live inspection & modification GUI for player accessories |
| `/curios tag <slot>` | `curiospaper.admin` | Tags the held item as a valid accessory for the specified slot |
| `/curios untag` | `curiospaper.admin` | Removes the accessory tag from the held item |
| `/curios give <player> <item> [amount]` | `curiospaper.admin` | Gives a registered custom accessory item to a player |
| `/curios clear <player> [slot]` | `curiospaper.admin` | Clears all or specific accessory slots for a player |
| `/curios reload [type]` | `curiospaper.admin` | Reloads `config`, `slots`, `languages`, or `all` |
| `/curios rp <info\|inject>` | `curiospaper.admin` | Views resource pack status or triggers pack injection |
| `/curios npc <id> [clear]` | `curiospaper.admin` | Opens accessory GUI or clears accessories for a Citizens NPC |
| `/curios debug <player\|item>` | `curiospaper.admin` | Displays debug diagnostics for player accessories or held item |

<div align="center">

![Admin Inspect GUI](https://cdn.modrinth.com/data/JLMPHrRw/images/10065dc33eb072688b041d4cb871c068dcec598e.png)

*Live inspection & modification GUI (/curios inspect <player>)*

<br>

![Citizens NPC Accessories](https://cdn.modrinth.com/data/JLMPHrRw/images/bc06b84c42002a116a050d418eaf3264b0885e1d.png)

*Equipping accessories on Citizens NPCs (/curios npc <id>)*

</div>

### Permissions

| Permission | Default | Description |
|---|---|---|
| `curiospaper.use` | `true` | Allows players to open and use the accessory inventory |
| `curiospaper.admin` | `op` | Access to administrative commands (inspect, tag, give, reload) |
| `curiospaper.slot.*` | `true` | Access to all accessory slots |
| `curiospaper.slot.<type>` | `true` | Access to a specific accessory slot (e.g. `curiospaper.slot.ring`) |

</details>

---

## 🛠️ Configuration Showcase

<details>
<summary>⚙️ <b>Click to Expand: Configuration Files (`config.yml`, `slots.yml`, `en_US.yml`)</b></summary>

### 1. Main Plugin Config (`/plugins/CuriosPaper/config.yml`)

```yaml
# ==============================================================================
#                      CuriosPaper 3.0 Configuration
# ==============================================================================

language: "en_US"

# Resource Pack Settings (injects into active host plugin)
resource-pack:
  host-plugin: "AUTO" # AUTO, SYNTHETIX, NEXO, ITEMSADDER, ORAXEN, CRAFTENGINE, RSPM, NONE

features:
  allow-elytra-on-back-slot: true
  quick-equip-right-click: true

  # Keybind system (hooks into Synthetix Universal Keybinds when present)
  hotkey:
    enabled: true
    slot: 8               # Hotbar slot 1-9 (8 = slot 8, -1 = any slot)
    sneak-type: "double"  # "double", "single", or "hold"
    sneak-hold-duration: 3

  # Curios inventory death drop behavior: "Auto", "Always", or "Never"
  keep-curio-inventory:
    type: "Auto"

# 3D Accessory Model Rendering Engine
models:
  enabled: true
  mode: "auto" # "auto", "packet", or "entity"
  tracking-distance: 48.0
  update-interval-ticks: 1
  hide-in-first-person: false
  auto-render-engine-models: true

# Storage Settings (Synthetix AddonDataStore or standalone YAML flatfiles)
storage:
  auto-save-interval: 5 # Minutes
```

### 2. Slot Configuration (`/plugins/CuriosPaper/slots.yml`)

```yaml
# ==============================================================================
#                      CuriosPaper 3.0 Slot Configuration
# ==============================================================================

slots:
  head:
    name: "Head"
    icon: "GOLDEN_HELMET"
    item-model: "curiospaper:head_slot"
    custom-model-data: 10001
    amount: 1
    gui-slot: 10
    permission: "curiospaper.slot.head"
    lore:
      - "&7Equip crowns, circlets, or magical headpieces."

  necklace:
    name: "Necklace"
    icon: "NAUTILUS_SHELL"
    item-model: "curiospaper:necklace_slot"
    custom-model-data: 10002
    amount: 1
    gui-slot: 11
    permission: "curiospaper.slot.necklace"

  back:
    name: "Back"
    icon: "ELYTRA"
    item-model: "curiospaper:back_slot"
    custom-model-data: 10003
    amount: 1
    gui-slot: 12
    permission: "curiospaper.slot.back"
    pitch-up-limit: 45.0
    pitch-down-limit: 30.0

  body:
    name: "Body"
    icon: "DIAMOND_CHESTPLATE"
    item-model: "curiospaper:body_slot"
    custom-model-data: 10004
    amount: 1
    gui-slot: 13
    permission: "curiospaper.slot.body"

  belt:
    name: "Belt"
    icon: "LEATHER"
    item-model: "curiospaper:belt_slot"
    custom-model-data: 10005
    amount: 1
    gui-slot: 14
    permission: "curiospaper.slot.belt"

  hands:
    name: "Hands"
    icon: "LEATHER_CHESTPLATE"
    item-model: "curiospaper:hands_slot"
    custom-model-data: 10006
    amount: 2
    gui-slot: 15
    permission: "curiospaper.slot.hands"

  bracelet:
    name: "Bracelet"
    icon: "CHAIN"
    item-model: "curiospaper:bracelet_slot"
    custom-model-data: 10007
    amount: 2
    gui-slot: 16
    permission: "curiospaper.slot.bracelet"

  ring:
    name: "Ring"
    icon: "GOLD_NUGGET"
    item-model: "curiospaper:ring_slot"
    custom-model-data: 10008
    amount: 2
    gui-slot: 19
    permission: "curiospaper.slot.ring"

  charm:
    name: "Charm"
    icon: "EMERALD"
    item-model: "curiospaper:charm_slot"
    custom-model-data: 10009
    amount: 4
    gui-slot: 20
    permission: "curiospaper.slot.charm"
```

### 3. Language File (`/plugins/CuriosPaper/languages/en_US.yml`)

```yaml
prefix: "<gradient:#FFB300:#FF5500>[CuriosPaper]</gradient> "

commands:
  usage-header: "<gold>CuriosPaper Commands:</gold>"
  usage-open: "<yellow>/{label} open [player]</yellow> <gray>- Open accessory inventory</gray>"
  usage-inspect: "<yellow>/{label} inspect <player></yellow> <gray>- Inspect and modify player's curios</gray>"
  usage-tag: "<yellow>/{label} tag <slot></yellow> <gray>- Tag held item for an accessory slot</gray>"
  usage-npc: "<yellow>/{label} npc <id></yellow> <gray>- Manage NPC accessories and 3D models</gray>"
```

</details>

---

## 🎮 How It Works

```
1. /curios open (or /baubles) → Opens the accessory menu
2. Click a slot icon          → Opens that slot's inventory
3. Place an accessory         → Abilities & 3D models activate!
```

- **Quick Equip:** Hold a tagged item + **Shift + Right-Click** to equip directly.
- **Hotkey:** Select hotbar slot 8 + **Double-Sneak** to open the menu without typing commands.

---

## 🔧 Requirements & Compatibility

| | Requirements |
|---|---|
| **Minecraft** | 1.20.6 — 1.21+ |
| **Server Software** | Paper, Purpur, Folia *(Experimental Support)* |
| **Java** | 21 LTS+ |
| **Supported Item Engines** | Synthetix *(join [Discord](https://discord.gg/r5YXqgrGya) — not yet approved on Modrinth)*, Nexo, ItemsAdder, Oraxen, CraftEngine |
| **Integrations** | Citizens 2, PlaceholderAPI |
| **Resource Pack** | Automatically injected into host plugin |

> [!WARNING]
> **Experimental Folia Support:** Folia regional multi-threading support is experimental—many errors, desyncs, and unexpected bugs can happen when running on Folia servers.

---

## 🆕 What's New in v3.0

- ⚡ **Full Folia & Multi-Threading Support** — Engineered with universal `PlatformScheduler` for native Folia multi-threaded regional ticking. Thread-independent logic executes asynchronously while entity/region mutations (abilities, flight checks, sneak hotkeys) are safely dispatched to their owning regional threads.
- 🚀 **Logic & Performance Optimizations** — Overhauled internal pipelines to eliminate legacy bloat (-25k lines), implemented concurrent keybind tracking, offloaded packet models and data stores completely off the main thread, and decoupled flight checks for smooth gameplay without server tick drops.
- 🧩 **Item Engine Delegation** — Delegated item creation, abilities, and recipes to modern item plugins (Synthetix [join Discord — not approved on Modrinth yet], Nexo, ItemsAdder, Oraxen, CraftEngine).
- 🎭 **Packet-Based 3D Models (`PacketModelManager`)** — Body-mounted 3D accessory models are rendered using clientbound packets, avoiding persistent world entities.
- ⚙️ **Dedicated `slots.yml`** — Migrated slot configuration to a dedicated file covering all 9 slot types with per-slot permissions (`curiospaper.slot.<id>`).
- 🌐 **MiniMessage Localization** — Adventure MiniMessage support with external language files in `languages/en_US.yml`.
- ⌨️ **Universal Keybinds** — Hooks into the Synthetix Keybind API (available via our [Discord](https://discord.gg/r5YXqgrGya) as it is not yet approved on Modrinth) for configurable sneak hotkeys.
- 👥 **Citizens NPC Support** — Manage NPC accessories via `/curios npc <id>`.
- 🧩 **PlaceholderAPI Expansion** — Placeholders for equipped items and slot status.
- 📦 **Automatic Resource Pack Injection** — Injects assets directly into active pack host plugins without requiring secondary HTTP ports.
- 🛠️ **Compatibility Fixes** — Resolved smithing table conflicts (ValhallaMMO) and fixed elytra back-slot interactions with custom flight chestplates.
- 📢 **Documentation Notice** — `curiospaper.run.place` is currently undergoing restructuring; join Discord for guides and support.

Full changelog → [**CHANGELOGS.md**](CHANGELOGS.md)

---

## 📊 Live Statistics

<p align="center">
  <a href="https://bstats.org/plugin/bukkit/CuriosPaper/29508">
    <img src="https://bstats.org/signatures/bukkit/CuriosPaper.svg" alt="CuriosPaper bStats Metrics Graph" width="500">
  </a>
</p>

---

## 💬 Community & Support

<div align="center">

[![Join the Discord](https://img.shields.io/discord/1456137607569346739?label=Join%20the%20Discord&style=for-the-badge&logo=discord&logoColor=white&color=5865F2)](https://discord.gg/r5YXqgrGya)

**[discord.gg/r5YXqgrGya](https://discord.gg/r5YXqgrGya)**

Get help · Report bugs · Suggest features · Stay updated

<br>

[![Buy Me a Coffee on Ko-fi](https://img.shields.io/badge/Ko--fi-Buy%20me%20a%20coffee-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/brothergaming52)

**[ko-fi.com/brothergaming52](https://ko-fi.com/brothergaming52)**

</div>

---

<details>
<summary>🤝 <b>For Developers (Maven Dependency)</b></summary>

### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>3.0</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml
```yaml
depend: [CuriosPaper]
```

</details>

---

<div align="center">

**Made with ❤️ by Brothergaming52**

[Discord](https://discord.gg/r5YXqgrGya) · [Ko-fi](https://ko-fi.com/brothergaming52) · [Changelog](CHANGELOGS.md) · [bStats](https://bstats.org/plugin/bukkit/CuriosPaper/29508) · [GitHub](https://github.com/Brothergaming52/CuriosPaper)

</div>
