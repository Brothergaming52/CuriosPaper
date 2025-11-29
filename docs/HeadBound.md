---
layout: default
title: HeadBound
parent: Public Addons # **CRITICAL: Links it to the parent page**
nav_order: 1 # Position within the Configuration drop-down
---
---

# HeadBound — CuriosPaper addon for head-slot accessories

**HeadBound** is a full-featured addon for CuriosPaper that adds a suite of custom **head-accessories** (masks, hoods, circlets, helmets, etc.) to your server — each carrying its own passive or conditional ability. Think “gear” that fits into the head slot, but works out-of-the-box with CuriosPaper’s slot system and resource-pack merging.

> 🔗 Requires: Paper 1.21+ and CuriosPaper (hard dependency).

---

## 🎯 What HeadBound Brings You

* **11+ unique head accessories**, each with distinct effects.
* Passive or conditional bonuses that blend naturally with vanilla gameplay (mining aid, night vision while caving, mobility buffs, mob detection vision, loot boosts, etc.)
* Fully configurable via `config.yml`: enable/disable items, adjust effect parameters, set drop/loot/trade chances, toggle cosmetic & debug options.
* Multiple acquisition methods out-of-the-box:

  * Mob drops (e.g. from Endermen, Drowned, Strays)
  * Structure loot (mineshafts, temples, ruins, fortresses, etc.)
  * Villager trades (wandering traders, master librarians, etc.)
* No extra UI, no commands required for player use — integrates directly with CuriosPaper’s head-slot GUI.

---

## 🛡️ Default Head Items (Examples)

| Item                    | Effect / Use                                                                                                    |
| ----------------------- | --------------------------------------------------------------------------------------------------------------- |
| **Scout’s Lens**        | Reveals nearby mobs via glowing outlines — ideal for scouting and early warning.                |
| **Miner’s Charmband**   | Grants night vision under a configured Y-level — perfect for deep cave exploration.             |
| **Third Eye Circlet**   | Enhanced vision + glow detection — good for dangerous areas or mob-heavy zones.                 |
| **Drowned Crown Shard** | Provides swim-speed boost + reduced drowning damage — helpful for underwater or sea adventures. |
| **Blazing Mask**        | Mitigates fire damage — useful in Nether, lava zones or fire-heavy combats.                     |
| **Serpent Hood**        | Applies configurable poison to enemies — for offensive PvE or PvP builds.                       |
| **Wanderer’s Hood**     | Affects wandering-trader behavior (e.g. longer stay, trade discounts) — more immersive economy. |
| **Stormcaller Cap**     | Improves trident loyalty (faster return) — great for ranged-combat or ocean explorations.       |
| **Frostbound Circlet**  | Reduces freeze buildup from powdered snow — good for cold-biome survival.                       |
| **Ghastbone Mask**      | Reduces explosion knockback — useful for PvE or ghast/creeper-heavy zones.                      |
| **Scholar’s Laurel**    | Increases XP gain — nice for XP farms or enchant-heavy servers.                                 |

(*These are default items — server owners can disable any, change acquisition rules, or tune effects via config.*)

---

## 🧩 How Players Get HeadBound Accessories

HeadBound supports **multiple acquisition paths** — configurable to fit your server style:

* **Mob Drops** — certain mobs have a chance to drop specific head items.
* **Structure Loot** — generate accessories as loot in natural structures (mineshafts, temples, ruins, fortresses, etc.).
* **Villager / Trader Trades** — wandering traders or villagers (e.g. librarians) can offer accessories in exchange for emeralds or custom costs.

All drop/loot/trade chances, item enable/disable toggles, and item-specific settings are exposed in config, giving full control to server admins.

---

## ⚙️ Admin & Server Owner Control

HeadBound ships with a comprehensive `config.yml`:

* Enable or disable **each individual accessory**.
* Fine-tune **drop / loot / trade probabilities**.
* Adjust effect parameters: ranges, durations, multipliers, intervals, Y-level thresholds, etc.
* Toggle **debug mode**, **cosmetic effects** (particles, sounds), or **disable accessories completely** if desired.
* Commands for server ops:

  * `/headbound reload` — reload config without server restart.
  * `/headbound list` — list all registered accessories and their enabled/disabled status.

---

## ✅ Why HeadBound Fits Perfectly as a CuriosPaper Addon

* Built **directly on CuriosPaper’s head slot** — no custom inventories or hacks.
* Fully integrates with CuriosPaper’s **resource-pack merging**, so icons/models load automatically for players.
* Provides **ready-to-use gear + gameplay effects** — ideal for servers wanting an easy, powerful accessory system.
* Highly configurable — easy to balance or customize to your server’s style.
* Compatible with Paper 1.21+ — modern Minecraft version support.

---

## 📥 Get HeadBound

* Download from SpigotMC, Modrinth or PMC (CuriosPaper required): “HeadBound – CuriosPaper”.
* Drop the JAR into your `plugins/` folder along with CuriosPaper.
* Restart or reload. Players will receive the pack automatically; accessories start spawning according to config rules.

---
