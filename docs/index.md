---
layout: default
title: Home
nav_order: 1 # This controls its position in the sidebar
---
# **CuriosPaper**

**CuriosPaper** is a **Curios-style accessory inventory API for Paper 1.21+**.
It adds a **dedicated accessory equipment system** (head, back, rings, charms, etc.) and exposes a clean **Java API** so other plugins can register items, define slots, add models, or implement gameplay effects — *without ever touching NBT or hacking Minecraft inventories*.

> ⚠ **This plugin is an API.**
> CuriosPaper **does NOT add items, stats, or abilities**.
> Addons (like your *HeadBound* plugin) provide the actual gear and gameplay.

---

## **Why CuriosPaper exists**

Paper plugins have:

* No native accessory system
* No safe way to bind items to custom slot types
* No built-in resource-pack merging
* No clean API for other plugins to extend equipment

**CuriosPaper fixes all of that** by providing:

* **Dedicated Accessory GUI**
  9 slot types by default (fully configurable), each with its own page.

* **Full customization**
  Slot names, icons, counts, item models, GUI layout, borders — all configurable.

* **Automatic Resource Pack Pipeline**
  Built-in:

    * Pack builder
    * Namespace conflict checker
    * File merge rules
    * HTTP server for hosting
    * Automatic combination of external packs (add-on plugins)

* **Elytra–Back-Slot System**
  Advanced support for giving chestplates gliding capability,
  custom asset IDs per armor material, and full trim-aware 3D models.

* **Addon-Ready API**
  Register accessories, listen to equip events, add effects, or inject models.
  (*HeadBound* is a real example of a complete addon.)

---

## **Fast Overview**

| Category                 | Details                                                        |
| ------------------------ | -------------------------------------------------------------- |
| **Minecraft Version**    | 1.21+                                                          |
| **Platform**             | Paper / Folia-compatible forks                                 |
| **Slot Types (default)** | head, necklace, back, body, belt, hands, bracelet, charm, ring |
| **GUI Access**           | `/baubles`, `/b`, `/bbag`                                      |
| **Resource Pack**        | Built-in HTTP server + automatic merging                       |
| **Data Storage**         | YAML with caching + optional timed backups                     |
| **API Access**           | `CuriosPaperAPI` (auto registered)                             |
| **Addon Example**        | *HeadBound* uses this API for real items, effects & models     |

---

## **Quick Start (Server Admins)**

1. Drop **CuriosPaper.jar** into `plugins/`.
2. Start server → `config.yml` is created.
3. Configure resource-pack host IP & port if needed.
4. Restart. Players can now open `/baubles`.
5. Install addon plugins (e.g., HeadBound) to add actual items.

---

## **Quick Start (Developers)**

1. Add CuriosPaper as a dependency.
2. Grab the API using:

   ```java
   CuriosPaperAPI api = CuriosPaperAPI.get();
   ```
3. Tag an item:

   ```java
   api.tagAccessory(item, "head");
   ```
4. Listen for events:

   ```java
   @EventHandler
   void onEquip(AccessoryEquipEvent event) { ... }
   ```
5. Add models:
   Place your assets under
   `resources/assets/curiospaper/`
   and CuriosPaper merges them automatically.

This is exactly how **HeadBound** implements its items, models, effects, and droptables.

---

## **Documentation Roadmap**

* **Configuration**
  Slot editing, counts, GUI, RP hosting, backups, debug logging.

* **Resource Pack Guide**
  Namespaces, merging rules, file conflicts, custom slot icons, elytra/chestplate models.

* **Developer API Guide**
  Tagging items, querying slots, equip/unequip events, player data, custom models.

* **Addon Example: HeadBound**
  Real-world reference implementation using CuriosPaper’s full feature set.

---

## **Downloads**

* GitHub Repo: *[Brothergaming52/CuriosPaper](https://github.com/Brothergaming52/CuriosPaper)*
* Releases: latest builds + source code
* SpigotMc: [CuriosPaper](spigotmc.org/resources/curiospaper.130346/)
* Modrinth: [CuriosPaper](https://modrinth.com/plugin/curiospaper)
* PMC: [CuriosPaper](planetminecraft.com/mod/plugin-curiospaper/)

---

## **Before You Continue**

If you're a server owner:
→ Read **[Configuration docs](https://brothergaming52.github.io/CuriosPaper/configuration.html)** first.
A wrong host IP or blocked port = broken icons for every player.

If you're a developer:
→ Read **[Developer API docs](https://brothergaming52.github.io/CuriosPaper/developer.html)** + **Resource Pack** sections.
It prevents 90% of mistakes addons usually make.

---
