---
layout: default
title: Developer Overview
parent: Developer API # **Links it to the Developer Guide parent**
nav_order: 1 
---
---

# **Developer API – Overview**

CuriosPaper is a **slot-based accessory API for Paper 1.21+**.
It provides clean entrypoints for:

* Tagging + defining accessory items
* Reading & modifying equipped items
* Listening to equip / unequip events
* Registering resource-pack assets
* Using the built-in slot system (9 types by default)
* Integrating with the back-slot elytra pipeline

**It is NOT an item/modifier system.**
Your addon provides the items, logic, effects, and models.
CuriosPaper only provides the *slots, data, and API layer*.

---

# **Getting CuriosPaper as a Dependency**

## Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>Tag</version> <!-- replace with a release/tag -->
</dependency>
```

## Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

```groovy
dependencies {
    implementation 'com.github.Brothergaming52:CuriosPaper:Tag'
}
```

---

# **How Addons Actually Use CuriosPaper**

If you want to know how to build a proper CuriosPaper addon, look at **HeadBound**:

* Uses an enum (`HeadItems`) to define all its items
* Tags items via CuriosPaper API (`tagAccessoryItem`)
* Listens to `AccessoryEquipEvent` to enable effects
* Registers its custom models through the CuriosPaper resource-pack API
* Places assets into `resources/assets/curiospaper/…`
* Relies entirely on CuriosPaper’s slot validation + GUI
* NEVER touches NBT manually
* NEVER builds its own accessory GUI

This is the correct development pattern.

---

# **Developer Features (API-Focused)**

## **✔ Slot System**

CuriosPaper exposes all configured slots automatically:

```
head, necklace, back, body,
belt, hands, bracelet, charm, ring
```

You reference these IDs from Java:

```java
api.tagAccessoryItem(item, "head", true);
```

Everything — counts, GUI layout, naming — is controlled by the server’s config.

---

## **✔ Accessory Tagging**

Every accessory must be tagged with the slot type.

### Correct:

```java
ItemStack item = ...;
ItemStack tagged = curiosApi.tagAccessoryItem(item, "ring", true);
```

### HeadBound example (simplified):

```java
ItemStack lens = ItemUtil.buildItem(HeadItems.SCOUTS_LENS);
curiosApi.tagAccessoryItem(lens, "head", true);
```

**Don’t reinvent your own NBT.
Don’t create fake lore tags.
Always use the API.**

---

## **✔ Reading Equipped Items**

```java
List<ItemStack> items = curiosApi.getEquippedItems(player, "back");
```

Examples from HeadBound (real pattern):

* Effects check if an item is equipped every tick
* Apply/remove PotionEffects or statuses
* Use CuriosPaper’s slot queries, never the player’s own inventory

---

## **✔ Modifying Equipped Items**

```java
curiosApi.setEquippedItem(player, "ring", 0, ringItem);
curiosApi.clearEquippedItems(player, "charm");
```

Used in addons to:

* Replace items after upgrades
* Remove items on ability cooldown
* Force-slot constraints

---

## **✔ Equip / Unequip Events**

```java
@EventHandler
public void onEquip(AccessoryEquipEvent event) {
    Player p = event.getPlayer();
    ItemStack item = event.getItem();
    String slot = event.getSlotType();
}
```

HeadBound uses this to:

* Activate abilities
* Trigger particles/sounds
* Apply attributes or potion effects

You should too.

---

## **✔ Resource Pack Injection**

CuriosPaper exposes an API for addon assets:

```java
File root = curiosApi.registerResourcePackSource(this, getFile());
```

If you ship:

```
resources/assets/myaddon/models/item/my_item.json
resources/assets/myaddon/textures/item/my_item.png
```

CuriosPaper automatically merges it into the server’s pack.

HeadBound example:

```
resources/assets/curiospaper/items/scouts-lens.json
resources/assets/curiospaper/models/item/scouts-lens.json
```

Because it places everything into the `curiospaper:` namespace,
CuriosPaper merges those assets seamlessly.

---

## **✔ Full Elytra / Back-Slot Integration**

Your API handles:

* Elytra as back-slot items
* Auto-adding gliding capability
* Auto-swapping chestplate model to custom 3D models
* Full trim-aware assets (all materials + trims)
* Durability rules
* Preventing dupes, resets, and invalid swaps

Addons can react or override behavior using `AccessoryEquipEvent`.

---

# **Core API Access**

Get the API instance:

```java
CuriosPaperAPI api = CuriosPaper.getInstance().getCuriosPaperAPI();
```

Once you have it → you can:

### Tag items

```java
api.tagAccessoryItem(item, "back", true);
```

### Check what slot an item belongs to

```java
String slot = api.getAccessorySlotType(stack);
```

### Validate items / slots

```java
api.isValidAccessory(stack, "belt");
api.isValidSlotType("ring");
```

### Query equipped accessories

```java
api.getEquippedItems(player, "charm");
api.hasEquippedItems(player, "back");
api.countEquippedItems(player, "ring");
```

### Modify what a player has equipped

```java
api.setEquippedItem(player, "head", 0, item);
api.clearEquippedItems(player, "bracelet");
```

### Register resource pack sources

```java
api.registerResourcePackAssetsFromJar(plugin);
```

---

# **Developer Requirements**

| Requirement                                   | Reason                                       |
| --------------------------------------------- | -------------------------------------------- |
| Paper 1.21+                                   | CuriosPaper uses Data Components + 1.21 APIs |
| Must not bypass CuriosPaper’s slot rules      | Causing corruption or dupe bugs              |
| Must register assets using CuriosPaper        | Otherwise models won’t load                  |
| Plugin namespace must be unique               | Conflict system will block you               |
| Must wrap all items using CuriosPaper tag API | Otherwise GUI won’t accept them              |

---

# **Quick Guidelines for Building Addons**

If you want your addon to not be trash:

### 1. Define your items cleanly

Use an enum like HeadBound (`HeadItems`).

### 2. Build your items *then tag*

Always tag AFTER creating the `ItemStack`.

### 3. Use `AccessoryEquipEvent`

Handle everything on equip/unequip.
Do NOT jankily poll player inventories.

### 4. Put all your models inside:

```
resources/assets/<your-namespace>/
```

### 5. Register your pack folder via API

Do NOT hand-build your pack.

### 6. Never use `minecraft:` namespace

Unless you're intentionally overriding vanilla assets.

---

# **Where to go next**

Developer API Sections:

* **Quickstart**
* **Accessory Items & Slot Types**
* **Equipping & Querying Items**
* **Events**
* **Resource Pack Integration**
* **Full Example Addon (HeadBound)**

---
