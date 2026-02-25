# Architecture Overview

CuriosPaper follows a modular architecture with clear separation of concerns across its packages.

## Package Structure

```
org.bg52.curiospaper
├── api/            → Public API interface + implementation
├── command/        → Command executors (/curios, /baubles, /edit)
├── config/         → Configuration loading and validation
├── data/           → Data models (ItemData, AbilityData, RecipeData, etc.)
├── event/          → Custom Bukkit events
├── handler/        → Feature handlers (ElytraBackSlotHandler)
├── inventory/      → GUI implementations (AccessoryGUI, EditGUI)
├── listener/       → Bukkit event listeners
├── manager/        → Core managers (SlotManager, ItemDataManager)
├── resourcepack/   → Resource pack generation and hosting
└── util/           → Version utilities and helpers
```

## Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                  CuriosPaper.java                  │
│                 (Main Plugin Class)                │
└────────────────────────┬────────────────────────────────┘
                       │ initializes
        ┌───────────────┼───────────────┐
        ▼              ▼              ▼
┌──────────────┐ ┌──────────────┐ ┌───────────────────┐
│ConfigManager│ │ SlotManager │ │ ItemDataManager │
│(config.yml) │ │(player data)│ │ (custom items)  │
└──────────────┘ └──────────────┘ └───────────────────┘
        │               │               │
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌───────────────────┐
│AccessoryGUI │ │  Listeners  │ │ResourcePackMgr  │
│EditGUI      │ │(events,inv) │ │(HTTP + pack gen)│
└──────────────┘ └──────────────┘ └───────────────────┘
        │               │
        ▼               ▼
┌───────────────────────────────────────────────────────┐
│             CuriosPaperAPI (public API)          │
│              CuriosPaperAPIImpl                  │
└───────────────────────────────────────────────────────┘
```

## Initialization Order

1. `ConfigManager` — Loads `config.yml` and slot configurations
2. `SlotManager` — Initializes player data storage
3. `ItemDataManager` — Loads custom items from `items/` (if item editor enabled)
4. `ChatInputManager` — Registers for chat input capture (if item editor enabled)
5. `ResourcePackManager` — Generates and hosts the resource pack
6. `CuriosPaperAPIImpl` — Creates the API implementation
7. GUIs — `AccessoryGUI` and `EditGUI` instances
8. Listeners — `InventoryListener`, `AbilityListener`, `RecipeListener`, etc.
9. `ElytraBackSlotHandler` — Registers if feature enabled and version supported
10. Commands — `/curios`, `/baubles`, `/edit` command executors
11. Auto-save task — Periodic player data saves
12. bStats — Anonymous usage metrics

## Shutdown Order

1. Cancel auto-save task
2. Unregister crafting recipes
3. Clean up external items
4. Shut down resource pack HTTP server
5. Shut down ability listener
6. Save all player data
