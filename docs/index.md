---
layout: default
title: Home
nav_order: 1 # This controls its position in the sidebar
---
# CuriosPaper

**CuriosPaper** is a **Curios-style accessory inventory API for Paper (Minecraft 1.21+)**.  
It provides a **dedicated accessory GUI** (rings, charms, back-slot, etc.) and a clean, plugin-friendly **Java API** — letting server owners and plugin developers manage extra equipment slots *without messing with NBT or custom inventories*.

> ⚠️ *CuriosPaper does **not** add its own items or stats — it’s an API layer.*  
> If you want actual gear or stat-boosting items, you (or another plugin) need to supply them.*

---

## Why use CuriosPaper?

- ✅ Adds extra equipment slot support to your server — head, neck, back, rings, charms, etc.  
- ✅ Full customization: slot types, names, icons, GUI layout, slot counts.  
- ✅ Automatic resource-pack generation + hosting — no manual pack building required.  
- ✅ Persistent player accessory data with optional backups and config-driven save intervals.  
- ✅ Lightweight and plugin-friendly: other devs can hook in via API.  

---

## At a glance

| Category | Info |
| -------- | ---- |
| **Supported Minecraft version** | 1.21+ |
| **Server platform** | Paper or any compatible fork |
| **Default slot types** | head, necklace, back, body, belt, hands, bracelet, charm, ring |
| **Installation** | Drop the JAR into `plugins/`, then restart or reload server |
| **Access GUI** | `/baubles`, `/b`, `/bbag` |
| **Config file** | Auto-generated `config.yml` on first run |
| **Data storage** | YAML (with caching + optional backups) |
| **Resource-pack hosting** | Built-in HTTP server (configurable IP/port) |

---

## Quick Start

1. Install on a Paper 1.21+ server → drop JAR.  
2. Restart server — `config.yml` is generated.  
3. Configure resource-pack host IP/port (if needed).  
4. Players run `/baubles` (or alias) to open the accessory GUI.  
5. Plugin developers: hook into `CuriosPaperAPI` to tag items or query equipped accessories.  

---

## Where to go from here 📚

- **Configuration** — tweak slot types, slot counts, GUI layout, pack hosting, storage & performance settings.  
- **Developer API** — learn how to tag items, equip accessories programmatically, listen for equip/unequip events, and integrate resource-pack assets.  
- (Future — if you add an **editor GUI addon**: docs for “in-game slot/item editor + resource-pack builder”)

---

## Get CuriosPaper

- GitHub repo: [Brothergaming52/CuriosPaper](https://github.com/Brothergaming52/CuriosPaper)  
- Release downloads & plugin builds: check the Releases section on GitHub or the platform where you publish your plugin  

--- 

> 💡 _Pro tip for server admins or devs:_ read the [Configuration docs] and [Developer API docs] **before you touch anything**, especially the resource-pack settings. A wrong IP or port will break icon loading for all players.

