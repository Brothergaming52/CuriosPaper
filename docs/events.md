---
layout: default
title: Events
parent: Developer API # **Links it to the Developer Guide parent**
nav_order: 4
---
# Developer API – Events

CuriosPaper exposes events so your plugin can **react** when players interact with accessories.

Right now the primary event is:

- `AccessoryEquipEvent` – fired whenever an accessory is equipped, unequipped, or swapped in a Curios slot.

This page explains:

- When the event fires.
- What data it gives you.
- How cancellation works.
- Common patterns and advanced usage.

---

## 1. `AccessoryEquipEvent` Overview

**Package:**

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
````

This event is fired when:

* A player equips an accessory into a Curios slot.
* A player unequips an accessory from a Curios slot.
* A player swaps one accessory for another in the same slot.

It is:

* **Synchronous** – runs on the main server thread.
* **Cancellable** – you can prevent equipping/unequipping.
* Fully integrated with CuriosPaper’s own GUI and API.

---

## 2. Event Structure

Key methods (simplified):

```java
public class AccessoryEquipEvent extends Event implements Cancellable {

    public enum Action {
        EQUIP,   // Putting a new item into an empty slot
        UNEQUIP, // Removing an item, leaving slot empty
        SWAP     // Replacing one item with another
    }

    public Player getPlayer();

    public String getSlotType();   // e.g. "ring", "back", "charm"
    public int getSlotIndex();     // 0-based index within that slotType

    public ItemStack getPreviousItem(); // item before the action (may be null)
    public ItemStack getNewItem();      // item after the action (may be null)

    public Action getAction();

    public boolean isCancelled();
    public void setCancelled(boolean cancel);
}
```

### Semantics

* **EQUIP**

  * `previousItem` = `null`
  * `newItem` = item being equipped
* **UNEQUIP**

  * `previousItem` = item being removed
  * `newItem` = `null`
* **SWAP**

  * `previousItem` = old item in the slot
  * `newItem` = new item being placed there

If you cancel the event:

* The GUI/action is reverted.
* Player inventory + Curios slots stay unchanged.

---

## 3. Basic Listener Setup

Standard Bukkit listener:

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.event.AccessoryEquipEvent.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CuriosEventsListener implements Listener {

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        String slotType = event.getSlotType();
        int slotIndex = event.getSlotIndex();
        Action action = event.getAction();

        ItemStack oldItem = event.getPreviousItem();
        ItemStack newItem = event.getNewItem();

        player.sendMessage("§7Curios: " + action + " on slot " + slotType + "[" + slotIndex + "]");
    }
}
```

Register the listener in your plugin’s `onEnable`:

```java
getServer().getPluginManager().registerEvents(new CuriosEventsListener(), this);
```

---

## 4. Common Use Cases

### 4.1 Enforcing Restrictions

Block certain items from being equipped in specific slots:

```java
@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) {
    if (event.getAction() != AccessoryEquipEvent.Action.EQUIP
            && event.getAction() != AccessoryEquipEvent.Action.SWAP) {
        return;
    }

    String slotType = event.getSlotType();
    ItemStack newItem = event.getNewItem();
    if (newItem == null || !newItem.hasItemMeta()) return;

    // Example: no "Cursed" items allowed in charm slots
    if ("charm".equalsIgnoreCase(slotType)) {
        String displayName = newItem.getItemMeta().getDisplayName();
        if (displayName.contains("Cursed")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot equip cursed charms.");
        }
    }
}
```

---

### 4.2 Triggering Effects on Equip / Unequip

Apply buffs when an item is equipped, and remove them when unequipped.

```java
@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) {
    Player player = event.getPlayer();
    AccessoryEquipEvent.Action action = event.getAction();
    ItemStack newItem = event.getNewItem();
    ItemStack oldItem = event.getPreviousItem();

    // Example: Ring of Haste → speed boost while equipped
    if (action == AccessoryEquipEvent.Action.EQUIP || action == AccessoryEquipEvent.Action.SWAP) {
        if (isRingOfHaste(newItem)) {
            applyHaste(player);
        }
    }

    if (action == AccessoryEquipEvent.Action.UNEQUIP || action == AccessoryEquipEvent.Action.SWAP) {
        if (isRingOfHaste(oldItem)) {
            removeHaste(player);
        }
    }
}
```

Where:

```java
private boolean isRingOfHaste(ItemStack item) {
    if (item == null || !item.hasItemMeta()) return false;
    String name = item.getItemMeta().getDisplayName();
    return name.contains("Ring of Haste");
}
```

You can manage your own buff tracking / stacking as needed.

---

### 4.3 Limiting Unique Accessories

Enforce “only 1 of this legendary item equipped at a time”.

```java
@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) {
    if (event.getAction() == AccessoryEquipEvent.Action.UNEQUIP) return;

    Player player = event.getPlayer();
    ItemStack newItem = event.getNewItem();
    if (!isLegendaryAmulet(newItem)) return;

    // Count how many legendary amulets are already equipped
    int count = 0;
    for (ItemStack item : curiosApi.getEquippedItems(player, "necklace")) {
        if (isLegendaryAmulet(item)) {
            count++;
        }
    }

    if (count > 1) {
        event.setCancelled(true);
        player.sendMessage("§cYou can only wear one legendary amulet.");
    }
}
```

---

### 4.4 Logging & Analytics

Track what players actually use.

```java
@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) {
    Player player = event.getPlayer();
    ItemStack newItem = event.getNewItem();

    if (event.getAction() == AccessoryEquipEvent.Action.EQUIP && newItem != null) {
        plugin.getLogger().info(player.getName() + " equipped " +
                newItem.getType() + " in slot " + event.getSlotType());
    }
}
```

---

## 5. Event Flow & Order

`AccessoryEquipEvent` is fired **after** CuriosPaper has:

* Validated the action.
* Determined the resulting state (previous/new).

But **before**:

* The final state is committed (if you cancel it).
* Any downstream listeners relying on final state should run.

Use `EventPriority` if you need strict ordering:

```java
@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
public void onAccessoryEquip(AccessoryEquipEvent event) {
    // run after LOW/NORMAL handlers, and only if not already cancelled
}
```

---

## 6. Best Practices

### ✅ Do

* Use `slotType` to branch logic (`"ring"`, `"back"`, `"charm"`, etc.).
* Use `Action` to distinguish EQUIP/UNEQUIP/SWAP clearly.
* Respect `ignoreCancelled = true` if your logic depends on success.
* Keep heavy logic out of the event if possible (no silly expensive work per click).

### ❌ Don’t

* Don’t modify Curios internal data manually – use the API if you need to change equipment.
* Don’t assume a fixed number of slots per type – use `getSlotAmount(slotType)` in your logic if needed.
* Don’t spam the player with messages on every tiny action.

---

## 7. Combining Events with Queries

Events tell you **when** something changed.
The query API tells you **what** the full state looks like.

Example: Recalculate stats whenever any accessory changes:

```java
@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) {
    if (event.isCancelled()) return;

    Player player = event.getPlayer();
    recalculateAccessoryStats(player);
}

private void recalculateAccessoryStats(Player player) {
    // Use curiosApi.getEquippedItems(player, slotType) over all slotTypes
    // and rebuild your stat profile from scratch.
}
```

That pattern is robust and avoids incremental state bugs.

---

## 8. Summary

* `AccessoryEquipEvent` is your main hook to react to Curios inventory changes.
* It gives you:

  * Player, slot type, slot index.
  * Previous item, new item.
  * Action (EQUIP / UNEQUIP / SWAP).
* It is **cancellable**, so you can enforce rules.
* Combine it with the query API (`getEquippedItems`, `countEquippedItems`, etc.) to build:

  * Stat systems
  * Class systems
  * Ability gating
  * Unique item restrictions
  * Logging / analytics
