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

  allow-minecraft-namespace: false

  allow-namespace-conflicts: false

# Slot configurations - 9 total slot types
slots:
  # Head slot for helmets, crowns, circlets
  head:
    name: "&e⚜ Head Slot ⚜"
    icon: "GOLDEN_HELMET"
    item-model: "curiospaper:head_slot"
    amount: 1
    lore:
      - "&7Equip crowns, circlets, or magical headpieces."
      - "&7Enhances mental abilities."

  # Necklace slot for amulets and pendants
  necklace:
    name: "&b✦ Necklace Slot ✦"
    icon: "NAUTILUS_SHELL"
    item-model: "curiospaper:necklace_slot"
    amount: 1
    lore:
      - "&7Wear powerful amulets and pendants."
      - "&7Grants mystical protection."

  # Back slot for capes and cloaks
  back:
    name: "&5☾ Back Slot ☾"
    icon: "ELYTRA"
    item-model: "curiospaper:back_slot"
    amount: 1
    lore:
      - "&7Equip magical capes and cloaks."
      - "&7Provides mobility and defense."

  # Body slot for chest accessories
  body:
    name: "&c❖ Body Slot ❖"
    icon: "DIAMOND_CHESTPLATE"
    item-model: "curiospaper:body_slot"
    amount: 1
    lore:
      - "&7Wear chest talismans and armor attachments."
      - "&7Strengthens your core vitality."

  # Belt slot for utility items
  belt:
    name: "&6⚔ Belt Slot ⚔"
    icon: "LEATHER"
    item-model: "curiospaper:belt_slot"
    amount: 1
    lore:
      - "&7Equip utility belts and sashes."
      - "&7Increases carrying capacity."

  # Hands/Gloves slots
  hands:
    name: "&f✋ Hand Slots ✋"
    icon: "LEATHER_CHESTPLATE"
    item-model: "curiospaper:hands_slot"
    amount: 2
    lore:
      - "&7Magical gloves for both hands."
      - "&7Enhances dexterity and grip strength."

  # Bracelet slots for wrists
  bracelet:
    name: "&3◈ Bracelet Slots ◈"
    icon: "CHAIN"
    item-model: "curiospaper:bracelet_slot"
    amount: 2
    lore:
      - "&7Wear enchanted bracelets and bangles."
      - "&7Mystical bands of power."

  # Ring slots for fingers
  ring:
    name: "&6◆ Ring Slots ◆"
    icon: "GOLD_NUGGET"
    item-model: "curiospaper:ring_slot"
    amount: 2
    lore:
      - "&7Holds powerful magical rings."
      - "&7Ancient bands of arcane energy."

  # Charm slots for misc trinkets
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
  # Add lore line to tagged items showing required slot
  add-slot-lore-to-items: true

  # Allow elytra equipping in back slots
  allow-elytra-on-back-slot: true

  # Allow players to see empty accessory slots in GUI
  show-empty-slots: true

  # Play sound when opening GUI
  play-gui-sound: true
  gui-sound: "BLOCK_CHEST_OPEN"

  # Play sound when equipping item
  play-equip-sound: true
  equip-sound: "ENTITY_ITEM_PICKUP"

  # Play sound when unequipping item
  play-unequip-sound: true
  unequip-sound: "ENTITY_ITEM_PICKUP"

# Storage settings
storage:
  # Storage type (currently only 'yaml' is supported)
  type: "yaml"

  # Auto-save interval in seconds (300 = 5 minutes)
  # Set to 0 to disable auto-save
  save-interval: 300

  # Save data when inventory is closed
  save-on-close: true

  # Create backup before saving
  create-backups: false
  backup-interval: 3600  # seconds (1 hour)
  max-backups: 5

# Performance settings
performance:
  # Cache player data in memory
  cache-player-data: true

  # Unload player data after disconnect (saves memory)
  unload-on-quit: true

  # Maximum items per slot type (safety limit)
  max-items-per-slot: 54

# GUI customization
gui:
  # Main menu title (Tier 1)
  main-title: "&8✦ Accessory Slots ✦"

  # Slot inventory title prefix (Tier 2)
  slot-title-prefix: "&8Slots: "

  # Filler material for GUI decoration
  filler-material: "GRAY_STAINED_GLASS_PANE"

  # Border material for GUI edges
  border-material: "BLACK_STAINED_GLASS_PANE"

  # Filler item display name
  filler-name: "&r"

  # Main GUI always uses double chest (54 slots)
  main-gui-size: 54

  # Use beautiful patterns for slot arrangements
  use-patterns: true

# Debug settings
debug:
  # Enable debug logging
  enabled: false

  # Log all API calls
  log-api-calls: false

  # Log inventory interactions
  log-inventory-events: false

  # Log slot position calculations
  log-slot-positions: false
```
