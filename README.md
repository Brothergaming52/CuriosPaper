---

# CuriosPaper

CuriosPaper is a **Curios-style accessory inventory API for Paper** (api-version `1.21`).
It adds a **separate accessory GUI** with configurable slots (back, rings, charms, etc.) and exposes a clean **Java API** so other plugins can register and manage accessories without touching NBT or custom inventories themselves.

This plugin **does not add its own items or stats** – it is an **API layer** for other plugins and servers that want extra equipment slots.

---

## Features

* ✅ **Dedicated accessory GUI** (`/baubles`, `/b`, `/bbag`)

  * Tiered menus: main accessory menu → per-slot pages
  * Configurable titles, filler items, borders, and layout
* ✅ **9 slot types by default (fully configurable)**
  All defined in `config.yml`:

  * `head`
  * `necklace`
  * `back`
  * `body`
  * `belt`
  * `hands`
  * `bracelet`
  * `charm`
  * `ring`
* ✅ **Config-driven slot behavior**

  * Per-slot:

    * Display name (`name`)
    * Icon material (`icon`)
    * Item model (`item-model`) for resource pack
    * Number of internal slots (`amount`)
    * Slot lore (`lore`)
* ✅ **Automatic resource pack generation & hosting**

  * Builds its own pack from `resources/assets/curiospaper/`
  * Merges additional assets from other plugins via API
  * Serves final ZIP via embedded HTTP server (`host-ip` + `port`)
* ✅ **Persistent player data**

  * Storage type: `yaml` (currently only option)
  * Auto-save interval in seconds
  * Optional backups with configurable interval & max backups
* ✅ **Performance controls**

  * Cache player data in memory
  * Unload on quit (to save RAM)
  * Safety cap: `max-items-per-slot`
* ✅ **Quality-of-life toggles**

  * Auto-add lore to tagged items (shows which slot they belong to)
  * Show empty slots in GUI or hide them
  * Sound on GUI open & equip
* ✅ **Developer API**

  * Tag items as accessories for a specific slot type
  * Read / modify equipped accessories per player
  * Listen for `AccessoryEquipEvent` (EQUIP, UNEQUIP, SWAP)
  * Register extra resource pack assets (folders or from your own JAR)

---

## Requirements

* **Server:** Paper (or any Paper-compatible fork)
* **Minecraft:** 1.21+ (plugin `api-version: 1.21`)
* **Client:** Must accept the server resource pack (if resource-pack is enabled)

---

## Commands

**/baubles**

* **Aliases:** `/b`, `/bbag`
* **Description:** Opens the **accessory inventory GUI** for the player.

There are no complicated admin commands: everything else is handled via **config** and **API**.

---

## Configuration Overview (`config.yml`)

The plugin generates `config.yml` in `plugins/CuriosPaper/` on first run.

### 1. Resource Pack

```yaml
resource-pack:
  enabled: true
  port: 8080
  host-ip: "your.public.ip.or.domain"
  base-material: "PAPER" # base item for slot icons
```

* `enabled`: turn automatic pack generation + hosting on/off.
* `port`: port used by the embedded HTTP server.
* `host-ip`: public address players connect to (used in resource-pack URL).
* `base-material`: base item type used for slot-icon items in the GUI.

### 2. Slots

Simplified example (the actual config defines all 9):

```yaml
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
      - "&7Capes, cloaks and wings belong here."
```

* **Key** (`head`, `necklace`, `back`, etc.) is the **slot type ID** used in the API.
* `amount` = how many internal slots of that type a player gets.
* `item-model` refers to the item model in the generated resource pack.

### 3. Storage

```yaml
storage:
  type: "yaml"
  save-interval: 300        # seconds, 0 = disable autosave
  save-on-close: true
  create-backups: false
  backup-interval: 3600     # seconds
  max-backups: 5
```

Controls how often and how safely accessory data is saved.

### 4. Performance

```yaml
performance:
  cache-player-data: true
  unload-on-quit: true
  max-items-per-slot: 54
```

Safety and memory tuning.

### 5. GUI

```yaml
gui:
  main-title: "&8✦ Accessory Slots ✦"
  slot-title-prefix: "&8Slots: "
  filler-material: "GRAY_STAINED_GLASS_PANE"
  border-material: "BLACK_STAINED_GLASS_PANE"
  filler-name: "&r"
  main-gui-size: 54   # main menu size (double chest)
  use-patterns: true  # fancy slot patterns instead of just a line
```

Visual configuration only; you can make the GUI look however you want.

### 6. Features & Debug

```yaml
features:
  add-slot-lore-to-items: true
  show-empty-slots: true
  play-gui-sound: true
  gui-sound: "BLOCK_CHEST_OPEN"
  play-equip-sound: true
  equip-sound: "ENTITY_ITEM_PICKUP"

debug:
  enabled: false
  log-api-calls: false
  log-inventory-events: false
  log-slot-positions: false
```

Turn these on if you’re debugging another plugin that uses CuriosPaper.

---

## Developer API

CuriosPaper exposes a single API class:

* `org.bg52.curiospaper.api.CuriosPaperAPI`

And the main plugin:

* `org.bg52.curiospaper.CuriosPaper`

### Getting the API

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;

public class MyPlugin extends JavaPlugin {

    private CuriosPaperAPI curiosApi;

    @Override
    public void onEnable() {
        CuriosPaper curiosPaper = CuriosPaper.getInstance();
        this.curiosApi = curiosPaper.getCuriosPaperAPI();
    }
}
```

Once you have `CuriosPaperAPI`, you can:

---

### 1. Tagging Items as Accessories

Use this to mark an `ItemStack` as belonging to a specific accessory slot type.

```java
ItemStack raw = ...; // your custom item
String slotType = "necklace"; // must match config.yml key

ItemStack tagged = curiosApi.tagAccessoryItem(raw, slotType, true);
// boolean flag is typically "add slot lore to item"
```

* `slotType` must be one of the configured keys: `head`, `necklace`, `back`, `body`, `belt`, `hands`, `bracelet`, `charm`, `ring` (or any custom ones you add to config).
* The returned `ItemStack` will carry the slot info in its metadata/NBT so CuriosPaper knows where it can be equipped.

You can also **inspect** what slot an item is tagged for:

```java
String slotType = curiosApi.getAccessorySlotType(itemStack); // may be null if not tagged
```

---

### 2. Validating Accessories

Before you try to equip or handle an item, you can sanity-check it:

```java
boolean valid = curiosApi.isValidAccessory(itemStack, "ring");
```

This checks both the item’s internal tagging and whether the slot type exists and is allowed.

You can also validate just the slot type key:

```java
boolean slotExists = curiosApi.isValidSlotType("back");
int maxSlots = curiosApi.getSlotAmount("ring"); // how many ring slots the player can have
```

---

### 3. Querying Equipped Accessories

CuriosPaper stores per-player accessories and exposes them via the API.

Typical patterns:

```java
Player player = ...;

// Get all currently equipped items for a slot type
List<ItemStack> rings = curiosApi.getEquippedItems(player, "ring");

// Get a single item at a specific index
ItemStack backItem = curiosApi.getEquippedItem(player, "back", 0);

// Does the player have anything in that slot type?
boolean hasBack = curiosApi.hasEquippedItems(player, "back");
int ringCount = curiosApi.countEquippedItems(player, "ring");
```

There are also UUID-based variants so you don’t need a live Player instance:

```java
UUID uuid = player.getUniqueId();
boolean hasCharms = curiosApi.hasEquippedItems(uuid, "charm");
int charmCount = curiosApi.countEquippedItems(uuid, "charm");
```

---

### 4. Modifying Equipped Accessories

You can directly change what is stored in the accessory inventory.

```java
// Set specific slot
curiosApi.setEquippedItem(player, "ring", 0, ringItem);

// Replace the entire list for a slot type
curiosApi.setEquippedItems(player, "bracelet", myBraceletList);

// Remove a specific index
curiosApi.removeEquippedItemAt(player, "necklace", 0);

// Remove the first matching item from that slot type
curiosApi.removeEquippedItem(player, "necklace", itemStack);

// Clear all items from a slot type
curiosApi.clearEquippedItems(player, "charm");
```

Use this to build custom effects, stat systems, etc., without ever touching the GUI logic.

---

### 5. Slot Type Metadata

You can get a `NamespacedKey` for a slot type (useful if you integrate with NBT/persistent data containers):

```java
NamespacedKey key = curiosApi.getSlotTypeKey();
```

And you can list all configured slot type IDs:

```java
List<String> slotTypes = curiosApi.getAllSlotTypes();
```

---

### 6. Accessory Equip Event

CuriosPaper fires `AccessoryEquipEvent` whenever an item is equipped, unequipped, or swapped in the accessory GUI.

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.event.AccessoryEquipEvent.Action;

@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) {
    Player player = event.getPlayer();
    String slotType = event.getSlotType();
    int slotIndex = event.getSlotIndex();

    ItemStack previous = event.getPreviousItem();
    ItemStack current = event.getNewItem();
    Action action = event.getAction(); // EQUIP, UNEQUIP, SWAP

    // Example: prevent equipping more than one specific unique item
    if (action == Action.EQUIP && isForbidden(current)) {
        event.setCancelled(true);
    }
}
```

* The event is **cancellable**.
* Use this to enforce custom rules, trigger buffs, etc.

---

### 7. Resource Pack Integration for Other Plugins

If your plugin ships extra item models/textures, you can let CuriosPaper merge them into its server resource pack.

**Directly from your JAR (using an embedded `/resources` folder):**

```java
File extractedRoot = curiosApi.registerResourcePackAssetsFromJar(this);
// Put your pack.mcmeta and assets/ inside src/main/resources/resources/...
```

CuriosPaper will:

* Extract/merge your assets into its `resource-pack-build` directory
* Rebuild the ZIP and update the hosted resource pack
---
