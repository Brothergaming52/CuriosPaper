---
layout: default
title: Equipped items & Queries
parent: Developer API # **Links it to the Developer Guide parent**
nav_order: 3
---
# Developer API – Equipped Items & Queries

This page explains how to **read** and **modify** what players have equipped in CuriosPaper, using the `CuriosPaperAPI`.

You’ll learn how to:

- Get all equipped items for a given slot type.
- Get a specific item at a given index.
- Check if players have anything equipped.
- Count equipped items.
- Set, replace, and clear equipped accessories.
- Use UUID-based queries (offline-safe).
- Build practical systems on top (requirements, stats, passives).

If you’re not familiar with **slot types** and **tagging items**, read the **Accessory Items & Slot Types** page first.

---

## 1. Basics – What You’re Working With

CuriosPaper internally stores, per **player**:

- A map: `slotType` → `List<ItemStack>`
  - Example: `"ring"` → list of ring items.
  - Example: `"charm"` → list of charm items.

You never touch this map directly.  
You always go through `CuriosPaperAPI` methods like:

- `getEquippedItems(...)`
- `getEquippedItem(...)`
- `hasEquippedItems(...)`
- `countEquippedItems(...)`
- `setEquippedItem(...)`
- `setEquippedItems(...)`
- `removeEquippedItemAt(...)`
- `removeEquippedItem(...)`
- `clearEquippedItems(...)`

All of them use **slot type IDs** like `"head"`, `"back"`, `"ring"`, `"charm"` — same as in `config.yml`.

---

## 2. Reading Equipped Items

### 2.1 Get All Items for a Slot Type

```java
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public void logRings(Player player, CuriosPaperAPI curiosApi) {
    List<ItemStack> rings = curiosApi.getEquippedItems(player, "ring");

    player.sendMessage("§eYou have " + rings.size() + " ring(s) equipped:");
    for (ItemStack ring : rings) {
        if (ring == null || !ring.hasItemMeta()) continue;
        player.sendMessage(" - " + ring.getItemMeta().getDisplayName());
    }
}
````

---

### 2.2 Get a Single Item at a Specific Index

Index is **0-based** and must be `< slot amount`.

```java
ItemStack backItem = curiosApi.getEquippedItem(player, "back", 0);

if (backItem != null) {
    player.sendMessage("§aYou have something on your back.");
} else {
    player.sendMessage("§7Your back slot is empty.");
}
```

If the index is out of bounds or slot is empty, this will return `null`.

---

### 2.3 Check If Any Items Are Equipped

```java
boolean hasBack = curiosApi.hasEquippedItems(player, "back");
boolean hasCharms = curiosApi.hasEquippedItems(player, "charm");

if (!hasBack) {
    player.sendMessage("§7You don't have anything equipped in the back slot.");
}
```

---

### 2.4 Count Equipped Items

```java
int ringCount = curiosApi.countEquippedItems(player, "ring");
int charmCount = curiosApi.countEquippedItems(player, "charm");

player.sendMessage("§eRings: " + ringCount + " | Charms: " + charmCount);
```

---

## 3. UUID-Based Queries (Offline-Safe)

All the main query methods have **UUID variants**, useful when:

* You’re running logic on offline players.
* You want to avoid storing `Player` references.

```java
import java.util.UUID;

UUID uuid = player.getUniqueId();

boolean hasCharms = curiosApi.hasEquippedItems(uuid, "charm");
int charmCount = curiosApi.countEquippedItems(uuid, "charm");
List<ItemStack> backItems = curiosApi.getEquippedItems(uuid, "back");
```

Use UUID variants whenever you’re not in a strictly online-only context.

---

## 4. Modifying Equipped Items

These methods let you **change** what the player has equipped, without touching the GUI directly.

### 4.1 Set One Item in a Slot

```java
ItemStack ringItem = ...; // must be tagged for "ring" slot
curiosApi.setEquippedItem(player, "ring", 0, ringItem);
```

* If there was an item previously at index `0`, it's replaced.
* If the index is out of bounds, the call will be ignored / safely handled.

---

### 4.2 Replace All Items for a Slot Type

```java
List<ItemStack> newBracelets = ...; // validated & tagged
curiosApi.setEquippedItems(player, "bracelet", newBracelets);
```

Use this when you:

* Load your own data format.
* Run mass transformations (e.g., upgrade all items).
* Resync state after some external change.

---

### 4.3 Remove by Index

```java
curiosApi.removeEquippedItemAt(player, "necklace", 0);
```

Removes the item at index 0 (if present).
Remaining items are shifted down.

---

### 4.4 Remove First Matching Item

```java
curiosApi.removeEquippedItem(player, "necklace", itemStack);
```

This will:

* Locate the first occurrence of `itemStack` in the `"necklace"` slot list.
* Remove it if found.

Useful when:

* You have a reference to the actual equipped item.
* You want to unequip “the same item” you previously gave/modified.

---

### 4.5 Clear All Items in a Slot Type

```java
curiosApi.clearEquippedItems(player, "charm");
```

Completely empties the slot type (`charm` in this case).
You can use this for:

* Purge commands.
* Class change / respec logic.
* Special events (e.g., “void storm” stripping accessories).

---

## 5. Recipes – Practical Use Cases

### Recipe 1 – Check Requirement: “Must Have Back Item to Use Ability”

```java
public boolean canUseGlideAbility(Player player, CuriosPaperAPI curiosApi) {
    return curiosApi.hasEquippedItems(player, "back");
}
```

You’d call this in your ability system and block activation when `false`.

---

### Recipe 2 – Count Accessories for a Scaling Bonus

Example: Each **ring** gives +2% crit chance.

```java
public double getCritBonusFromRings(Player player, CuriosPaperAPI curiosApi) {
    int ringCount = curiosApi.countEquippedItems(player, "ring");
    return ringCount * 0.02; // 2% per ring
}
```

Then integrate this into your damage or combat calculations.

---

### Recipe 3 – Summarize All Equipped Accessories

```java
public void showEquippedSummary(Player player, CuriosPaperAPI curiosApi) {
    List<String> slotTypes = curiosApi.getAllSlotTypes();

    player.sendMessage("§8[§dCurios§8] §7Your equipped accessories:");
    for (String slotType : slotTypes) {
        List<ItemStack> items = curiosApi.getEquippedItems(player, slotType);
        if (items.isEmpty()) continue;

        player.sendMessage("§f- " + slotType + " (§a" + items.size() + "§f):");
        for (ItemStack item : items) {
            if (item == null || !item.hasItemMeta()) continue;
            player.sendMessage("   §7• " + item.getItemMeta().getDisplayName());
        }
    }
}
```

This adapts automatically to custom slot types.

---

### Recipe 4 – “Unequip All on Death”

If you want Curios accessories to be removed on death (or dropped / moved elsewhere), you can hook into `PlayerDeathEvent` and use the API:

```java
@EventHandler
public void onDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    CuriosPaperAPI curiosApi = ...;

    List<String> slotTypes = curiosApi.getAllSlotTypes();
    for (String slotType : slotTypes) {
        List<ItemStack> items = curiosApi.getEquippedItems(player, slotType);
        for (ItemStack item : items) {
            if (item == null) continue;
            // Example: drop them on death
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        curiosApi.clearEquippedItems(player, slotType);
    }
}
```

---

### Recipe 5 – Force Equip a Set (Class System / Loadouts)

Example: When a player chooses “Fire Mage” class, equip a specific set:

```java
public void applyFireMageSet(Player player, CuriosPaperAPI curiosApi,
                             ItemStack ring, ItemStack charm, ItemStack backItem) {

    curiosApi.clearEquippedItems(player, "ring");
    curiosApi.clearEquippedItems(player, "charm");
    curiosApi.clearEquippedItems(player, "back");

    curiosApi.setEquippedItem(player, "ring", 0, ring);
    curiosApi.setEquippedItem(player, "charm", 0, charm);
    curiosApi.setEquippedItem(player, "back", 0, backItem);
}
```

---

## 6. Good Practices & Pitfalls

### ✅ Good Practices

* Always use **slot type IDs** that exist in config; check with `isValidSlotType(...)`.
* Use `countEquippedItems`, `hasEquippedItems`, and `getEquippedItems` instead of storing your own duplication of that data.
* Prefer **UUID-based** API when you don’t need the live `Player`.

---

### ❌ Pitfalls to Avoid

* Don’t assume specific slot counts (like “ring slots are always 2”) — read `getSlotAmount("ring")`.
* Don’t manipulate Curios NBT directly. Always use the API.
* Don’t store references to `ItemStack`s and assume they will stay valid forever; state can change via GUI or other plugins.

---

## 7. Where to Go Next

For deeper integration:

* **Accessory Items & Slot Types** – How items become accessories and how slots are defined.
* **Events** – Use `AccessoryEquipEvent` to react on equip/unequip.
* **Resource Pack Integration** – Custom models and icons for your accessory items.
