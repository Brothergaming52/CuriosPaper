---
layout: default
title: Developer Overview
parent: Developer API # **Links it to the Developer Guide parent**
nav_order: 1 
---
# Developer API – Overview

CuriosPaper is a **Curios-style accessory inventory API for Paper** (`api-version: 1.21`).

It provides:

- A **separate accessory GUI** with configurable slot types (back, rings, charms, etc.).
- A clean **Java API** so other plugins can register and manage accessories **without touching NBT or custom inventories**.

> CuriosPaper **does not add its own items or stats**.  
> It is an **API layer** for servers and plugins that want extra equipment slots.

---

## Getting CuriosPaper as a Dependency

CuriosPaper is distributed via **JitPack**.

### Maven

**Step 1 – Add the JitPack repository**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
````

**Step 2 – Add the dependency**

```xml
<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>Tag</version> <!-- replace with release tag -->
</dependency>
```

---

### Gradle (Groovy)

**Step 1 – Add JitPack to repositories** (usually in `settings.gradle` or `build.gradle`):

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2 – Add the dependency**

```groovy
dependencies {
    implementation 'com.github.Brothergaming52:CuriosPaper:Tag' // replace Tag with version
}
```

---

## Features (Developer-Focused)

### ✅ Dedicated Accessory GUI

Commands: `/baubles`, `/b`, `/bbag`

* Tiered menus:

  * Main accessory menu → per-slot pages.
* Fully configurable:

  * Titles, filler items, borders, layout, patterns.

---

### ✅ 9 Slot Types by Default (Fully Configurable)

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

Each slot type can be renamed, repurposed, or expanded via config.

---

### ✅ Config-Driven Slot Behavior

Per slot type:

* `name` – GUI display name.
* `icon` – base material used for that slot’s icon in the main GUI.
* `item-model` – model ID used in the resource pack.
* `amount` – how many internal slots the player gets for that type.
* `lore` – tooltip text.

Example:

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
```

---

### ✅ Automatic Resource Pack Generation & Hosting

CuriosPaper can:

* Build its own resource pack from:

  * `resources/assets/curiospaper/` (plugin’s built-in assets).
  * Additional assets provided by other plugins via API.
* Host the final ZIP via an embedded HTTP server.

Key config:

```yaml
resource-pack:
  enabled: true
  port: 8080
  host-ip: "your.public.ip.or.domain"
  base-material: "PAPER"
```

---

### ✅ Persistent Player Data

* Storage type: `yaml` (per-player files).
* Auto-save interval in seconds.
* Optional backup system:

  * `backup-interval`
  * `max-backups`

Example:

```yaml
storage:
  type: "yaml"
  save-interval: 300
  save-on-close: true
  create-backups: false
  backup-interval: 3600
  max-backups: 5
```

---

### ✅ Performance Controls

* `cache-player-data` – hold player accessory data in memory while online.
* `unload-on-quit` – clear cached data when the player leaves.
* `max-items-per-slot` – safety cap per slot type.

```yaml
performance:
  cache-player-data: true
  unload-on-quit: true
  max-items-per-slot: 54
```

---

### ✅ Quality-of-Life Toggles

* Add slot lore to items.
* Show or hide empty slots in GUI.
* Sounds on open/equip/unequip.

```yaml
features:
  add-slot-lore-to-items: true
  show-empty-slots: true
  play-gui-sound: true
  gui-sound: "BLOCK_CHEST_OPEN"
  play-equip-sound: true
  equip-sound: "ENTITY_ITEM_PICKUP"
  play-unequip-sound: true
  unequip-sound: "ENTITY_ITEM_PICKUP"
```

---

### ✅ Debug / Dev Tools

Debug logging for:

* API calls.
* Inventory interactions.
* Slot position calculations.

```yaml
debug:
  enabled: false
  log-api-calls: false
  log-inventory-events: false
  log-slot-positions: false
```

Useful when developing addons or diagnosing conflicts with other plugins.

---

## Requirements

* **Server:** Paper or any Paper-compatible fork.
* **Minecraft:** **1.21+** (`api-version: 1.21`).
* **Client:** Must accept the server resource pack if `resource-pack.enabled = true`.

---

## Commands

### `/baubles`

* **Aliases:** `/b`, `/bbag`
* **Action:** Opens the player’s **accessory GUI**.
* All other behavior (which slots exist, how many, what they do) is controlled via:

  * `config.yml`
  * CuriosPaper API.

There are no complicated admin commands; integration happens via code.

---

## Configuration Overview (For Developers)

As a plugin developer, you mainly care about:

1. **Slot IDs**
   The keys under `slots:` (`head`, `back`, `ring`, etc.) are the **slot type IDs** you’ll pass into the API:

   ```java
   // Example slot types:
   "head", "necklace", "back", "body", "belt",
   "hands", "bracelet", "charm", "ring"
   ```

2. **Slot Counts**
   `amount` defines how many internal slots that type has:

   ```yaml
   ring:
     amount: 2
   ```

   This value is used when you:

   * Check how many items are equipped.
   * Validate slot indices in your code.

3. **Resource Pack Integration**
   The `item-model` field connects config to visual models.
   Your plugin can register its own models via the CuriosPaper API and refer to them in config.

For full details, see the dedicated config pages:

* **Configuration / Slots & GUI**
* **Configuration / Resource Pack**
* **Configuration / Storage & Backups**
* **Configuration / Performance & Caching**
* **Configuration / Features & Debug**

---

## Developer API – High-Level Overview

CuriosPaper exposes:

* Main plugin class:
  `org.bg52.curiospaper.CuriosPaper`
* API interface:
  `org.bg52.curiospaper.api.CuriosPaperAPI`

### Getting the API Instance

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

Once you have `curiosApi`, you can:

* **Tag items** as accessories for a specific slot type:

  ```java
  ItemStack tagged = curiosApi.tagAccessoryItem(item, "necklace", true);
  ```
* **Inspect** which slot an item belongs to:

  ```java
  String slotType = curiosApi.getAccessorySlotType(itemStack);
  ```
* **Validate** slot types & accessories:

  ```java
  boolean valid = curiosApi.isValidAccessory(itemStack, "ring");
  boolean exists = curiosApi.isValidSlotType("back");
  int maxSlotCount = curiosApi.getSlotAmount("ring");
  ```
* **Query equipped items**:

  ```java
  List<ItemStack> rings = curiosApi.getEquippedItems(player, "ring");
  boolean hasBackItem = curiosApi.hasEquippedItems(player, "back");
  int charmCount = curiosApi.countEquippedItems(player, "charm");
  ```
* **Modify equipped items**:

  ```java
  curiosApi.setEquippedItem(player, "ring", 0, ringItem);
  curiosApi.clearEquippedItems(player, "charm");
  ```
* **React to equip/unequip events** via `AccessoryEquipEvent`.
* **Register extra resource pack assets** from your plugin JAR.

Detailed usage and examples are covered in the dedicated Developer API pages:

* **Quickstart**
* **Accessory Items & Slot Types**
* **Equipped Items & Queries**
* **Events**
* **Resource Pack Integration**

---
```
```
