---
layout: default
title: QuickStart
parent: Developer API # **Links it to the Developer Guide parent**
nav_order: 2
---
# Developer API – Quickstart

This page is for **plugin developers** who want to hook into CuriosPaper as fast as possible.

By the end of this page you will:

- Add CuriosPaper as a dependency (Maven / Gradle).
- Declare it as a plugin dependency in `plugin.yml`.
- Get a `CuriosPaperAPI` instance.
- Tag an item as an accessory.
- Read and modify equipped accessories.
- Listen to `AccessoryEquipEvent`.

If you’re looking for **what CuriosPaper is** or **how to configure it**, see the **Overview** and **Configuration** docs instead.

---

## 1. Add CuriosPaper via JitPack

CuriosPaper is available through **JitPack**.

### Maven

**Step 1 – Add JitPack repository**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
````

**Step 2 – Add CuriosPaper dependency**

```xml
<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>Tag</version> <!-- Replace with a release tag -->
</dependency>
```

---

### Gradle (Groovy)

**Step 1 – Add JitPack to repositories**

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2 – Add CuriosPaper dependency**

```groovy
dependencies {
    implementation 'com.github.Brothergaming52:CuriosPaper:Tag' // Replace Tag with a release version
}
```

---

## 2. Declare Dependency in `plugin.yml`

CuriosPaper must be loaded **before** your plugin.

```yaml
name: MyCuriosAddon
main: com.example.mycuriosaddon.MyCuriosAddon
version: 1.0.0
api-version: '1.21'

depend:
  - CuriosPaper
```

Use `depend` instead of `softdepend` if your plugin **requires** CuriosPaper to function.

---

## 3. Getting the `CuriosPaperAPI`

CuriosPaper exposes:

* Main plugin: `org.bg52.curiospaper.CuriosPaper`
* API: `org.bg52.curiospaper.api.CuriosPaperAPI`

Get the API once on enable and store it:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bukkit.plugin.java.JavaPlugin;

public class MyCuriosAddon extends JavaPlugin {

    private CuriosPaperAPI curiosApi;

    @Override
    public void onEnable() {
        CuriosPaper curiosPaper = CuriosPaper.getInstance();
        this.curiosApi = curiosPaper.getCuriosPaperAPI();

        if (curiosApi == null) {
            getLogger().severe("CuriosPaper API not available! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // register listeners, commands, etc. here
    }

    public CuriosPaperAPI getCuriosApi() {
        return curiosApi;
    }
}
```

From here on, you’ll use `curiosApi` for everything.

---

## 4. Tagging Items as Accessories

Core concept:
You **tag** an `ItemStack` as belonging to a slot type defined in `config.yml` (`head`, `necklace`, `back`, `ring`, `charm`, etc.).

Example: create a custom necklace item and tag it:

```java
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public void giveStarterNecklace(Player player, CuriosPaperAPI curiosApi) {
    // 1) Create base item
    ItemStack base = new ItemStack(Material.NAUTILUS_SHELL);
    ItemMeta meta = base.getItemMeta();
    meta.setDisplayName("§bStarter Amulet");
    base.setItemMeta(meta);

    // 2) Tag as accessory for "necklace" slot (must match config.yml key)
    ItemStack tagged = curiosApi.tagAccessoryItem(base, "necklace", true);
    // third param: whether to auto-add slot lore (if configured)

    // 3) Give to player
    player.getInventory().addItem(tagged);
}
```

You can inspect which slot type an item was tagged for:

```java
String slotType = curiosApi.getAccessorySlotType(itemStack); // may be null if not an accessory
```

---

## 5. Checking & Validating Slot Types

Before using a slot type ID, validate it:

```java
boolean slotExists = curiosApi.isValidSlotType("ring");
int ringSlots = curiosApi.getSlotAmount("ring"); // how many ring slots exist per player
```

Validate an accessory item against a slot type:

```java
boolean validRingAccessory = curiosApi.isValidAccessory(itemStack, "ring");
```

---

## 6. Reading Equipped Accessories

Use the API to see what a player has equipped.

```java
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public void logPlayerAccessories(Player player, CuriosPaperAPI curiosApi) {
    // All rings
    List<ItemStack> rings = curiosApi.getEquippedItems(player, "ring");

    // Single back-slot item (index 0)
    ItemStack backItem = curiosApi.getEquippedItem(player, "back", 0);

    boolean hasBack = curiosApi.hasEquippedItems(player, "back");
    int ringCount = curiosApi.countEquippedItems(player, "ring");

    player.sendMessage("You have " + ringCount + " ring(s) equipped.");
}
```

There are UUID-based variants if you don’t have a live `Player` instance:

```java
UUID uuid = player.getUniqueId();
boolean hasCharms = curiosApi.hasEquippedItems(uuid, "charm");
int charmCount = curiosApi.countEquippedItems(uuid, "charm");
```

---

## 7. Modifying Equipped Accessories

You can programmatically equip/unequip items without touching CuriosPaper’s GUI logic.

```java
// Equip into a specific slot index
curiosApi.setEquippedItem(player, "ring", 0, ringItem);

// Equip multiple bracelets at once
curiosApi.setEquippedItems(player, "bracelet", braceletList);

// Remove a specific index
curiosApi.removeEquippedItemAt(player, "necklace", 0);

// Remove the first occurrence of a specific item
curiosApi.removeEquippedItem(player, "necklace", targetItem);

// Clear all charms
curiosApi.clearEquippedItems(player, "charm");
```

This is where you plug in:

* Stat systems
* Class systems
* Custom ability unlocks
* Accessories-as-keys or tokens

---

## 8. Listening to `AccessoryEquipEvent`

Use `AccessoryEquipEvent` to react when accessories are equipped, swapped, or removed.

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.event.AccessoryEquipEvent.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CuriosListener implements Listener {

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        String slotType = event.getSlotType();
        int slotIndex = event.getSlotIndex();

        ItemStack previous = event.getPreviousItem();
        ItemStack current  = event.getNewItem();
        Action action      = event.getAction(); // EQUIP, UNEQUIP, SWAP

        // Example: simple restriction
        if (action == Action.EQUIP && isForbidden(current)) {
            player.sendMessage("§cYou cannot equip that accessory.");
            event.setCancelled(true);
            return;
        }

        // Example: notify on equip
        if (action == Action.EQUIP) {
            player.sendMessage("§aEquipped " + current.getItemMeta().getDisplayName()
                    + " in " + slotType + " slot.");
        }
    }

    private boolean isForbidden(ItemStack item) {
        // your logic
        return false;
    }
}
```

Register the listener in `onEnable`:

```java
getServer().getPluginManager().registerEvents(new CuriosListener(), this);
```

---

## 9. Minimal “Hello Curios” Example

Putting it all together:

* Tag a custom ring.
* Give it to the player.
* Apply a small effect when equipped.

```java
public void giveAndHookRing(Player player, CuriosPaperAPI curiosApi) {
    ItemStack base = new ItemStack(Material.GOLD_NUGGET);
    ItemMeta meta = base.getItemMeta();
    meta.setDisplayName("§6Ring of Example");
    base.setItemMeta(meta);

    ItemStack ring = curiosApi.tagAccessoryItem(base, "ring", true);
    player.getInventory().addItem(ring);
}
```

Then, in your listener:

```java
@EventHandler
public void onAccessoryEquip(AccessoryEquipEvent event) {
    if (event.getAction() != Action.EQUIP) return;
    if (!"ring".equalsIgnoreCase(event.getSlotType())) return;

    ItemStack newItem = event.getNewItem();
    if (newItem == null || !newItem.hasItemMeta()) return;

    String name = newItem.getItemMeta().getDisplayName();
    if (!name.contains("Ring of Example")) return;

    // Example effect: give temporary speed
    Player player = event.getPlayer();
    player.sendMessage("§eYou feel lighter on your feet.");
    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 0)); // 10 seconds
}
```
```
