---
layout: default
title: Developer Overview
parent: Developer API # **Links it to the Developer Guide parent**
nav_order: 1 
---
# Developer API Overview

CuriosPaper provides a complete **accessory-slot framework** for Paper 1.21+, along with a clean, stable Java API that allows other plugins to:

- register accessories  
- read & manipulate equipped items  
- hook into equip/unequip events  
- inject their own resource-pack assets  
- interact with custom accessory slots  

This page gives you a high-level overview of everything the API can do.  
Use the sidebar to explore each topic in detail.

---

# 🔧 What CuriosPaper Provides

CuriosPaper is a **Curios-style accessory inventory API**.  
It adds:

- A dedicated **accessory GUI**
- Fully configurable **slot types**
- Automatic **resource pack generation**
- Persistent **per-player accessory storage**
- Developer-friendly **Java integration**

This plugin does **not** add items, stats, or gameplay by itself — it is an API layer designed for other plugins.

---

# ✨ Key Features

## 🗂️ 1. Dedicated Accessory GUI
Commands: `/baubles`, `/b`, `/bbag`  
The GUI contains:

- A main menu (slot categories)
- Submenus for each slot type
- Fully configurable icons, titles, and patterns

You can create:

- Backpacks  
- Cloaks  
- Rings  
- Charms  
- Bracelets  
- Custom slot types for your own systems  

Everything is controlled via `config.yml`.

---

## 🎛️ 2. 9 Default Slot Types (Fully Configurable)

Available out of the box:

- `head`
- `necklace`
- `back`
- `body`
- `belt`
- `hands`
- `bracelet`
- `ring`
- `charm`

Each slot type includes:

- `name` (display name)
- `icon` (GUI icon)
- `item-model` (resource pack model)
- `amount` (number of slots)
- `lore`

You may rename, repurpose, or add more slot types as needed.

---

## 🎨 3. Automatic Resource Pack Generation

CuriosPaper builds its own pack using:

- internal models  
- merged assets from other plugins  
- your configured slot icons  

Features:

- Automatic ZIP creation  
- Embedded HTTP server (`host-ip` + `port`)
- External plugin asset merging  
- Optional custom namespaced models  

No manual pack editing required.

---

## 💾 4. Persistent Player Storage

The plugin stores equipped items safely using YAML:

- Auto-save interval
- On-close saving
- Optional backups
- Backup limits
- Per-player files

The system is atomic and crash-safe.

---

## 🚀 5. Performance Controls

CuriosPaper includes fine-grained controls:

- Player data caching  
- Unload-on-quit  
- Safety limit: `max-items-per-slot`  
- Minimal allocation patterns  

Designed for tiny SMPs, giant networks, and everything in between.

---

## 🎚️ 6. Quality-of-Life Features

Toggle via `config.yml`:

- Add lore to accessory items  
- Show/hide empty GUI slots  
- GUI open sound  
- Equip/unequip sounds  
- Elytra support in back slot  

Great for RPG servers and custom plugin developers.

---

## 📦 7. Developer API Highlights

Plugins can:

- Tag items as accessories  
- Read/modify equipped accessories  
- Listen for equip events  
- Create new slot types  
- Register custom resource pack assets  
- Build custom abilities, stat systems, trinkets, etc.

The API exposes a single entry point:

```java
CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();
````

From that point, everything is accessible.

---

# 📜 Requirements

| Component             | Requirement                            |
| --------------------- | -------------------------------------- |
| **Server**            | Paper or Paper-compatible fork         |
| **Minecraft Version** | 1.21+                                  |
| **Client**            | Must accept resource pack (if enabled) |

---

# 🧭 Commands

### `/baubles`

Aliases: `/b`, `/bbag`
Opens the accessory GUI.

No admin commands required — configuration and API code handle the rest.

---

# 🗂️ Configuration Overview

CuriosPaper generates `config.yml` containing:

* **Resource Pack** settings
* **Slot configuration**
* **Storage + Backups**
* **Performance controls**
* **GUI styling**
* **Feature toggles + Debug**

Each of these is documented in the Configuration section of this documentation.

Here is a minimal conceptual outline:

---

## Resource Pack

```yaml
resource-pack:
  enabled: true
  port: 8080
  host-ip: "your.public.ip.or.domain"
  base-material: "PAPER"
```

Controls automatic ZIP generation and hosting.

---

## Slots

```yaml
slots:
  head:
    name: "&e⚜ Head Slot ⚜"
    icon: "GOLDEN_HELMET"
    item-model: "curiospaper:head_slot"
    amount: 1
```

Each top-level key (e.g., `head`) is a **slot type** used in the API.

---

## Storage

```yaml
storage:
  type: "yaml"
  save-interval: 300
  save-on-close: true
  create-backups: false
```

Handles saving and backups.

---

## Performance

```yaml
performance:
  cache-player-data: true
  unload-on-quit: true
  max-items-per-slot: 54
```

Controls memory usage and safety limits.

---

## GUI

```yaml
gui:
  main-title: "&8✦ Accessory Slots ✦"
  slot-title-prefix: "&8Slots: "
  main-gui-size: 54
  use-patterns: true
```

Visual customization.

---

## Features & Debug

```yaml
features:
  add-slot-lore-to-items: true
  show-empty-slots: true

debug:
  enabled: false
  log-api-calls: false
```

Toggles for behavior and troubleshooting.

---

# 🧩 Developer API Summary

After obtaining the API instance:

```java
CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();
```

You can:

### ✔ Tag Items

```java
ItemStack tagged = api.tagAccessoryItem(item, "necklace", true);
```

### ✔ Check Item Slot Type

```java
String slot = api.getAccessorySlotType(itemStack);
```

### ✔ Validate

```java
api.isValidAccessory(itemStack, "ring");
api.isValidSlotType("back");
```

### ✔ Query Equipped Items

```java
List<ItemStack> rings = api.getEquippedItems(player, "ring");
ItemStack backItem = api.getEquippedItem(player, "back", 0);
```

### ✔ Modify Equipped Items

```java
api.setEquippedItem(player, "ring", 0, ringItem);
api.clearEquippedItems(player, "charm");
```

### ✔ Handle Accessory Events

```java
@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) { ... }
```

### ✔ Register Resource Pack Assets

```java
api.registerResourcePackAssetsFromJar(this);
```

---
