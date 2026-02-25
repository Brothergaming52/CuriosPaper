# Listeners

CuriosPaper registers several Bukkit event listeners to handle game interactions.

## InventoryListener

Handles all inventory-related interactions with the accessory GUI.

| Event | Purpose |
|---|---|
| `InventoryClickEvent` | Process equip/unequip/swap in accessory GUI slots |
| `InventoryCloseEvent` | Save player data if `save-on-close` is enabled |
| `InventoryDragEvent` | Prevent dragging items across GUI slots |

## AbilityListener

Manages the application and removal of item abilities.

| Event | Purpose |
|---|---|
| `AccessoryEquipEvent` | Apply/remove abilities on equip/unequip |
| Scheduled task | Refresh `WHILE_EQUIPPED` effects |

### Lifecycle

1. On **EQUIP**: Applies `EQUIP` trigger abilities, starts `WHILE_EQUIPPED` timers
2. On **UNEQUIP**: Applies `DE_EQUIP` trigger abilities, removes active effects
3. On **SWAP**: Removes old abilities, applies new ones
4. On **shutdown**: Cleans up all active ability tasks

## RecipeListener

Manages custom recipe registration and crafting events.

| Event | Purpose |
|---|---|
| `CraftItemEvent` | Validate custom recipe ingredients |
| `PrepareItemCraftEvent` | Set correct result item for custom recipes |
| `InventoryClickEvent` (SMITHING) | Handle smithing table recipes via reflection |

### Features

- Registers all custom item recipes on startup
- Unregisters all recipes on shutdown
- Handles recipe validation for custom ingredients
- Fires `CuriosRecipeTransferEvent` for data transfer

## PlayerDataListener

Handles player join/quit events for data management.

| Event | Purpose |
|---|---|
| `PlayerJoinEvent` | Load player accessory data, send resource pack |
| `PlayerQuitEvent` | Save player data, unload from memory |

## MobDropListener

Handles mob death events for custom item drops.

| Event | Purpose |
|---|---|
| `EntityDeathEvent` | Check configured mob drops and spawn items |

## VillagerTradeListener

Handles villager profession and level changes for custom trades.

| Event | Purpose |
|---|---|
| Villager events | Add custom trades based on profession and level |

## ElytraBackSlotHandler

A specialized listener/handler for the elytra back slot feature.

| Event | Purpose |
|---|---|
| `AccessoryEquipEvent` | Handle elytra equip/unequip in back slot |
| `EntityPickupItemEvent` | Auto-tag picked-up Elytras |
| `PlayerInteractEvent` | Handle chestplate right-click interactions |
| `InventoryClickEvent` | Protect secret elytra from being moved |
| `PlayerItemDamageEvent` | Redirect durability to back-slot elytra |
| `PlayerDeathEvent` | Clean up secret elytra from death drops |
