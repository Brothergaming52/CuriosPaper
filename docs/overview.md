---
layout: default
title: Config Overview
parent: Configuration # **CRITICAL: Links it to the parent page**
nav_order: 1 # Position within the Configuration drop-down
---
# Configuration Overview

CuriosPaper ships with a single main configuration file: **`config.yml`**, generated automatically on first startup.

This page gives you a **high-level understanding** of how the config is structured and what each major section controls.  
If you want deeper explanations, examples, and screenshots, check the dedicated pages in the Configuration section.

---

## 🔧 Configuration Structure (Top-Level)

The config is split into the following major sections:

| Section | Purpose |
|--------|---------|
| **resource-pack** | Controls automatic resource-pack generation and hosting. |
| **slots** | Defines all accessory slot types and how they appear in the GUI. |
| **features** | Toggles quality-of-life and behavior features. |
| **storage** | Controls how player data is saved and backed up. |
| **performance** | Caching and memory-management settings. |
| **gui** | Customization options for menu titles, borders, icons, patterns, etc. |
| **debug** | Development/debug logging for troubleshooting. |

Below is the full default config for reference.

---

## 📄 Full Default Config (`config.yml`)

```yaml
# Configuration for CuriosPaper Custom Accessory Slots

# Resource Pack Settings
resource-pack:
  # Enable automatic resource pack generation and serving
  enabled: true
  # Port for the embedded HTTP server
  # You need a seperate port using the default port will not work and may break things if tried
  port: 8080
  # Public IP or Hostname of the server (for players to download the pack)
  host-ip: "localhost"
  # Base material for custom icons (usually PAPER or LEATHER_HORSE_ARMOR)
  base-material: "PAPER"

# Slot configurations - 9 total slot types
slots:
  head:
    name: "&e⚜ Head Slot ⚜"
    icon: "GOLDEN_HELMET"
    item-model: "curiospaper:head_slot"
    amount: 1
    lore:
      - "&7Equip crowns, circlets, or magical headpieces."
      - "&7Enhances mental abilities."

  necklace:
    name: "&b✦ Necklace Slot ✦"
    icon: "NAUTILUS_SHELL"
    item-model: "curiospaper:necklace_slot"
    amount: 1
    lore:
      - "&7Wear powerful amulets and pendants."
      - "&7Grants mystical protection."

  back:
    name: "&5☾ Back Slot ☾"
    icon: "ELYTRA"
    item-model: "curiospaper:back_slot"
    amount: 1
    lore:
      - "&7Equip magical capes and cloaks."
      - "&7Provides mobility and defense."

  body:
    name: "&c❖ Body Slot ❖"
    icon: "DIAMOND_CHESTPLATE"
    item-model: "curiospaper:body_slot"
    amount: 1
    lore:
      - "&7Wear chest talismans and armor attachments."
      - "&7Strengthens your core vitality."

  belt:
    name: "&6⚔ Belt Slot ⚔"
    icon: "LEATHER"
    item-model: "curiospaper:belt_slot"
    amount: 1
    lore:
      - "&7Equip utility belts and sashes."
      - "&7Increases carrying capacity."

  hands:
    name: "&f✋ Hand Slots ✋"
    icon: "LEATHER_CHESTPLATE"
    item-model: "curiospaper:hands_slot"
    amount: 2
    lore:
      - "&7Magical gloves for both hands."
      - "&7Enhances dexterity and grip strength."

  bracelet:
    name: "&3◈ Bracelet Slots ◈"
    icon: "CHAIN"
    item-model: "curiospaper:bracelet_slot"
    amount: 2
    lore:
      - "&7Wear enchanted bracelets and bangles."
      - "&7Mystical bands of power."

  ring:
    name: "&6◆ Ring Slots ◆"
    icon: "GOLD_NUGGET"
    item-model: "curiospaper:ring_slot"
    amount: 2
    lore:
      - "&7Holds powerful magical rings."
      - "&7Ancient bands of arcane energy."

  charm:
    name: "&d✧ Charm Slots ✧"
    icon: "EMERALD"
    item-model: "curiospaper:charm_slot"
    amount: 4
    lore:
      - "&7Holds small, potent charms and trinkets."
      - "&7Mystical keepsakes and lucky tokens."

# Feature toggles
features:
  add-slot-lore-to-items: true
  allow-elytra-on-back-slot: true
  show-empty-slots: true
  play-gui-sound: true
  gui-sound: "BLOCK_CHEST_OPEN"
  play-equip-sound: true
  equip-sound: "ENTITY_ITEM_PICKUP"
  play-unequip-sound: true
  unequip-sound: "ENTITY_ITEM_PICKUP"

# Storage settings
storage:
  type: "yaml"
  save-interval: 300
  save-on-close: true
  create-backups: false
  backup-interval: 3600
  max-backups: 5

# Performance settings
performance:
  cache-player-data: true
  unload-on-quit: true
  max-items-per-slot: 54

# GUI customization
gui:
  main-title: "&8✦ Accessory Slots ✦"
  slot-title-prefix: "&8Slots: "
  filler-material: "GRAY_STAINED_GLASS_PANE"
  border-material: "BLACK_STAINED_GLASS_PANE"
  filler-name: "&r"
  main-gui-size: 54
  use-patterns: true

# Debug settings
debug:
  enabled: false
  log-api-calls: false
  log-inventory-events: false
  log-slot-positions: false
```
