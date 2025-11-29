---
layout: default
title: Equipped Items & Queries
parent: Developer API
nav_order: 4
---
# Developer API – Equipped Items & Queries

This page covers how to **read** and **modify** what players have equipped in CuriosPaper using `CuriosPaperAPI`.

You’ll learn how to:

- Get all equipped items for a given slot type.
- Get a specific item at a given index.
- Check if players have anything equipped.
- Count equipped items.
- Set, replace, and clear equipped accessories.
- Use UUID-based (offline-safe) queries.
- Build real systems on top (requirements, stats, passives, class builds).

If you don’t understand **slot types** or **tagging items**, read **Accessory Items & Slot Types** first.

---

## 1. What CuriosPaper Stores Internally

For each **player**, CuriosPaper maintains something conceptually like:

```text
slotType -> List<ItemStack>
````

Examples:

* `"ring"` → `[ring1, ring2, ...]`
* `"charm"` → `[charm1, charm2, charm3, charm4]`

You never touch this directly.
You use `CuriosPaperAPI` methods:

* Reads:

    * `getEquippedItems(...)`
    * `getEquippedItem(...)`
    * `hasEquippedItems(...)`
    * `countEquippedItems(...)`
* Writes:

    * `setEquippedItem(...)`
    * `setEquippedItems(...)`
    * `removeEquippedItemAt(...)`
    * `removeEquippedItem(...)`
    * `clearEquippedItems(...)`

All of these take **slot type IDs** like `"head"`, `"back"`, `"ring"`, `"charm"`.

---

## 2. Reading Equipped Items

### 2.1 Get all items for a slot type

```java
public void logRings(Player player, CuriosPaperAPI curiosApi) {
    List<ItemStack> rings = curiosApi.getEquippedItems(player, "ring");

    player.sendMessage("§eYou have " + rings.size() + " ring(s) equipped:");
    for (ItemStack ring : rings) {
        if (ring == null || !ring.hasItemMeta()) continue;
        player.sendMessage(" §7• " + ring.getItemMeta().getDisplayName());
    }
}
```

This is the pattern HeadBound’s effect handlers use internally:
**pull equipped items → inspect → apply logic.**

---

### 2.2 Get a single item at a specific index

Index is **0-based** and must be `< slot capacity`.

```java
ItemStack backItem = curiosApi.getEquippedItem(player, "back", 0);

if (backItem != null) {
    player.sendMessage("§aYou have something on your back.");
} else {
    player.sendMessage("§7Your back slot is empty.");
}
```

If the index is out of bounds or slot is empty, you get `null`.

---

### 2.3 Check if any items are equipped

```java
boolean hasBack = curiosApi.hasEquippedItems(player, "back");
boolean hasCharms = curiosApi.hasEquippedItems(player, "charm");

if (!hasBack) {
    player.sendMessage("§7You don't have anything equipped in the back slot.");
}
```

This is cheap and should be your default “do they have X at all?” check.

---

### 2.4 Count equipped items

```java
int ringCount  = curiosApi.countEquippedItems(player, "ring");
int charmCount = curiosApi.countEquippedItems(player, "charm");

player.sendMessage("§eRings: " + ringCount + " | Charms: " + charmCount);
```

Use this when you want **scaling bonuses** (e.g., stats that increase per accessory).

---

## 3. UUID-Based Queries (Offline-Safe)

Most read methods have **UUID variants**. Use them when:

* You’re running scheduled tasks not bound to an online `Player`.
* You’re calculating stats for stored data (not just live players).
* You don’t want to keep `Player` references around.

```java
UUID uuid = player.getUniqueId();

boolean hasCharms  = curiosApi.hasEquippedItems(uuid, "charm");
int charmCount     = curiosApi.countEquippedItems(uuid, "charm");
List<ItemStack> backItems = curiosApi.getEquippedItems(uuid, "back");
```

If you’re not tied to events like `AccessoryEquipEvent`, prefer UUID access.

---

## 4. Modifying Equipped Items

These API calls **change** what CuriosPaper has stored.
Use them instead of trying to hack the GUI inventory.

### 4.1 Set one item in a specific slot index

```java
ItemStack ringItem = ...; // must be a tagged "ring" accessory
curiosApi.setEquippedItem(player, "ring", 0, ringItem);
```

* Replaces the item at index `0` if present.
* If index >= capacity, call is ignored / safely handled.

Typical uses:

* Class loadouts
* Auto-equipping rewards
* Scripted transformations

---

### 4.2 Replace all items for a slot type

```java
List<ItemStack> newBracelets = ...; // tagged & validated
curiosApi.setEquippedItems(player, "bracelet", newBracelets);
```

Use this when:

* Loading an external save format.
* Migrating item data.
* Performing “mass upgrades” (all items converted in one shot).

---

### 4.3 Remove by index

```java
curiosApi.removeEquippedItemAt(player, "necklace", 0);
```

* Removes the item at index `0` if present.
* Elements after index 0 shift down.

Good for “unequip the first necklace” type logic.

---

### 4.4 Remove first matching item

```java
curiosApi.removeEquippedItem(player, "necklace", itemStack);
```

* Searches the `"necklace"` slot list for the first occurrence of `itemStack`.
* Removes it if found.

Use when:

* You have a direct reference to the equipped item.
* You’re undoing a previous equip/change done by your plugin.

---

### 4.5 Clear all items in a slot type

```java
curiosApi.clearEquippedItems(player, "charm");
```

Blows away all equipped charms.

Use cases:

* Reset commands (`/curios reset` style).
* Class respecs (unequip everything before applying new set).
* Special events (“curse strip all your charms” etc.).

---

## 5. Recipes – Real Use Cases

### 5.1 Requirement: “Must have back item to use ability”

```java
public boolean canUseGlideAbility(Player player, CuriosPaperAPI curiosApi) {
    return curiosApi.hasEquippedItems(player, "back");
}
```

Hook this into your ability/skills system.
If `false`, block the action.

---

### 5.2 Scaling bonus based on amount equipped

Example: each **ring** gives +2% crit chance:

```java
public double getCritBonusFromRings(Player player, CuriosPaperAPI curiosApi) {
    int ringCount = curiosApi.countEquippedItems(player, "ring");
    return ringCount * 0.02; // 2% per ring
}
```

Integrate this into your damage or combat calc.

HeadBound does the same type of thing but per-item:
each effect handler checks “is my item equipped?” and applies flat or scaling bonuses.

---

### 5.3 Full equipped summary (config-aware)

```java
public void showEquippedSummary(Player player, CuriosPaperAPI curiosApi) {
    List<String> slotTypes = curiosApi.getAllSlotTypes();

    player.sendMessage("§8[§dCurios§8] §7Your equipped accessories:");
    for (String slotType : slotTypes) {
        List<ItemStack> items = curiosApi.getEquippedItems(player, slotType);
        if (items.isEmpty()) continue;

        player.sendMessage("§f- §e" + slotType + " §7(" + items.size() + "):");
        for (ItemStack item : items) {
            if (item == null || !item.hasItemMeta()) continue;
            player.sendMessage("   §7• " + item.getItemMeta().getDisplayName());
        }
    }
}
```

No hardcoded slot IDs. Works with any custom config.

---

### 5.4 Unequip all on death (or move elsewhere)

If you want Curios accessories to **drop** on death, or be moved to a special inventory, hook `PlayerDeathEvent`:

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
            // Example: drop them
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }

        curiosApi.clearEquippedItems(player, slotType);
    }
}
```

You can easily swap “drop” for:

* Store into a custom death chest
* Store into a database
* Wipe them completely

---

### 5.5 Force equip a build (class / kit / loadout)

```java
public void applyFireMageSet(
        Player player,
        CuriosPaperAPI curiosApi,
        ItemStack ring,
        ItemStack charm,
        ItemStack backItem
) {
    curiosApi.clearEquippedItems(player, "ring");
    curiosApi.clearEquippedItems(player, "charm");
    curiosApi.clearEquippedItems(player, "back");

    curiosApi.setEquippedItem(player, "ring", 0, ring);
    curiosApi.setEquippedItem(player, "charm", 0, charm);
    curiosApi.setEquippedItem(player, "back", 0, backItem);
}
```

HeadBound could use this pattern for “sets” or rituals; your addon can too.

---

### 5.6 Back-slot Elytra synergy (high level)

If you’re integrating with CuriosPaper’s **elytra/back-slot system**:

* You’ll typically check if a back-slot accessory is the one that grants gliding.
* Then let CuriosPaper’s internal elytra handler handle data components, durability, and asset IDs.

Example (pseudo):

```java
public boolean hasGliderBackItem(Player player, CuriosPaperAPI curiosApi) {
    ItemStack backItem = curiosApi.getEquippedItem(player, "back", 0);
    // your identification logic (display name, key, PDC, etc.)
    return isYourGlider(backItem);
}
```

You don’t reimplement gliding logic; you just decide **which** back items should count as gliders.

---

## 6. Good Practices vs Trash Practices

### ✅ Good practices

* Always check the slot configuration via:

    * `isValidSlotType(slotType)`
    * `getSlotAmount(slotType)`
* Use the Curios API as the **single source of truth** for equipped accessories.
* When working with offline players or background tasks, use the **UUID-based** methods.

---

### ❌ Trash practices (don’t do this)

* Hardcoding assumptions like “ring slots are always 2” instead of reading `getSlotAmount("ring")`.
* Bypassing Curios and trying to use the GUI inventory directly.
* Caching `ItemStack` references long-term and assuming they never change.
* Modifying Curios-related NBT or persistent data yourself instead of using the API.

You do any of that, you’re just setting yourself up for bugs.

---

## 7. Next Steps

To fully wire your addon like HeadBound:

* Use **Events** (especially `AccessoryEquipEvent`) to react instantly when equipment changes.
* Use **Resource Pack Integration** to give your accessories proper models.
* Combine **tagging**, **equipped item queries**, and **events** to implement actual gameplay effects cleanly.

