---
layout: default
title: QuickStart
parent: Developer API
nav_order: 2
---
# Developer API – Quickstart

This page is for **addon developers** who want to hook into CuriosPaper **right now**.

By the end, you will:

- Add CuriosPaper as a **build dependency** (Maven / Gradle).
- Declare it as a **plugin dependency** in `plugin.yml`.
- Grab a `CuriosPaperAPI` instance.
- Tag items as accessories for specific slots.
- Read & modify equipped accessories.
- Listen to `AccessoryEquipEvent`.
- Follow the same structural patterns used by **HeadBound**.

If you need the big-picture explanation, read **Developer Overview** first.

---

## 1. Add CuriosPaper via JitPack

CuriosPaper is published on **JitPack**.

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
````

```xml
<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>Tag</version> <!-- Replace with a release tag -->
</dependency>
```

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Brothergaming52:CuriosPaper:Tag' // Replace Tag with a release version
}
```

---

## 2. Declare CuriosPaper in `plugin.yml`

CuriosPaper **must load before** your addon.

```yaml
name: MyCuriosAddon
main: com.example.mycuriosaddon.MyCuriosAddon
version: 1.0.0
api-version: '1.21'

depend:
  - CuriosPaper
```

Use `depend`, not `softdepend`, if your plugin is useless without CuriosPaper (like HeadBound).

---

## 3. Getting the `CuriosPaperAPI`

You only need to grab the API **once** and reuse it.

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

        // Register listeners, commands, etc.
        getServer().getPluginManager().registerEvents(new CuriosListener(curiosApi), this);
    }

    public CuriosPaperAPI getCuriosApi() {
        return curiosApi;
    }
}
```

HeadBound does the same thing: grabs the API once on enable, then passes it into managers/listeners.

---

## 4. Define Your Items (HeadBound-Style Pattern)

Don’t scatter random `ItemStack` builders everywhere.
HeadBound uses an **enum** to define all its items.

Minimal version:

```java
public enum MyCuriosItems {

    RING_OF_SPEED(
            "ring-of-speed",
            "§bRing of Speed",
            Material.GOLD_NUGGET,
            "Grants a small speed boost while equipped."
    );

    private final String key;
    private final String displayName;
    private final Material material;
    private final String description;

    MyCuriosItems(String key, String displayName, Material material, String description) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.description = description;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public String getDescription() { return description; }
}
```

Then create a tiny **ItemFactory** like HeadBound’s `ItemManager` / `ItemUtil`:

```java
public final class ItemFactory {

    private ItemFactory() {}

    public static ItemStack buildItem(MyCuriosItems def) {
        ItemStack stack = new ItemStack(def.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(def.getDisplayName());
        meta.setLore(List.of("§7" + def.getDescription()));
        stack.setItemMeta(meta);
        return stack;
    }
}
```

---

## 5. Tag Items as Accessories

Tagging is the **core** operation.
Without it, CuriosPaper will treat your item as a normal vanilla item.

```java
public class StarterGiver {

    private final CuriosPaperAPI curiosApi;

    public StarterGiver(CuriosPaperAPI curiosApi) {
        this.curiosApi = curiosApi;
    }

    public void giveSpeedRing(Player player) {
        // 1) Build base item
        ItemStack base = ItemFactory.buildItem(MyCuriosItems.RING_OF_SPEED);

        // 2) Tag as accessory for the "ring" slot
        ItemStack tagged = curiosApi.tagAccessoryItem(base, "ring", true);
        // 3rd param: whether to add slot lore (if enabled in config)

        // 3) Give to player
        player.getInventory().addItem(tagged);
    }
}
```

You can inspect the slot type later:

```java
String slotType = curiosApi.getAccessorySlotType(itemStack); // null if not tagged
```

HeadBound uses this exact pattern:
**build item → tag with `tagAccessoryItem` → give/drop/trade it.**

---

## 6. Check Slot Types & Limits

Use the API to respect server config:

```java
boolean exists = curiosApi.isValidSlotType("ring");
int ringSlots = curiosApi.getSlotAmount("ring"); // e.g. 2
```

Validate that an item is a proper accessory for a given slot:

```java
boolean valid = curiosApi.isValidAccessory(itemStack, "ring");
```

HeadBound does similar checks internally when deciding what items are allowed and how they behave.

---

## 7. Read Equipped Accessories

Never guess from the player’s vanilla inventory.
Use CuriosPaper’s accessors like HeadBound does.

```java
public class AccessoryDebug {

    private final CuriosPaperAPI curiosApi;

    public AccessoryDebug(CuriosPaperAPI curiosApi) {
        this.curiosApi = curiosApi;
    }

    public void logAccessories(Player player) {
        List<ItemStack> rings = curiosApi.getEquippedItems(player, "ring");
        ItemStack backItem = curiosApi.getEquippedItem(player, "back", 0);

        boolean hasBack = curiosApi.hasEquippedItems(player, "back");
        int ringCount = curiosApi.countEquippedItems(player, "ring");

        player.sendMessage("§eYou have " + ringCount + " ring(s) equipped.");
        if (hasBack && backItem != null) {
            player.sendMessage("§eBack slot: " + backItem.getItemMeta().getDisplayName());
        }
    }
}
```

UUID-based variants exist if you only have the player’s UUID:

```java
boolean hasCharms = curiosApi.hasEquippedItems(uuid, "charm");
int charmCount = curiosApi.countEquippedItems(uuid, "charm");
```

---

## 8. Modify Equipped Accessories Programmatically

You can equip/unequip **without** touching GUI internals.

```java
// Equip into ring slot index 0
curiosApi.setEquippedItem(player, "ring", 0, taggedRing);

// Bulk equip bracelets
curiosApi.setEquippedItems(player, "bracelet", braceletList);

// Remove specific index
curiosApi.removeEquippedItemAt(player, "necklace", 0);

// Remove first matching item
curiosApi.removeEquippedItem(player, "necklace", targetItem);

// Clear all charms
curiosApi.clearEquippedItems(player, "charm");
```

This is where you hook in:

* Leveling systems
* Class systems
* Unlockable bonuses
* Unlockable slot expansions

---

## 9. Listen to `AccessoryEquipEvent` (HeadBound-Style)

This is where you attach **actual gameplay**.

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.event.AccessoryEquipEvent.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CuriosListener implements Listener {

    private final CuriosPaperAPI curiosApi;

    public CuriosListener(CuriosPaperAPI curiosApi) {
        this.curiosApi = curiosApi;
    }

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        String slotType = event.getSlotType();
        Action action = event.getAction();

        ItemStack oldItem = event.getPreviousItem();
        ItemStack newItem = event.getNewItem();

        // Example: react only to ring equips
        if (!"ring".equalsIgnoreCase(slotType)) return;

        // Simple restriction example
        if (action == Action.EQUIP && isForbidden(newItem)) {
            player.sendMessage("§cYou cannot equip that accessory.");
            event.setCancelled(true);
            return;
        }

        // Effect example: give speed when our example ring is equipped
        if (action == Action.EQUIP && isSpeedRing(newItem)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    20 * 15, // 15 seconds
                    0
            ));
            player.sendMessage("§aYou feel lighter on your feet.");
        }

        // Optional: remove effect when unequipped
        if (action == Action.UNEQUIP && isSpeedRing(oldItem)) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    private boolean isSpeedRing(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name != null && name.contains("Ring of Speed");
    }

    private boolean isForbidden(ItemStack item) {
        // Your own rules
        return false;
    }
}
```

HeadBound uses this exact model but split per-item:

* `ScoutsLensHandler`
* `MinersCharmbandHandler`
* `WandererHoodHandler`
* etc.

Each handler listens for equip/unequip and applies its own logic.

---

## 10. Register Your Resource Pack Assets

If you ship models/textures, follow the same pattern as HeadBound:

```
src/main/resources/resources/assets/<your-namespace>/models/item/...
src/main/resources/resources/assets/<your-namespace>/textures/item/...
```

Then, during plugin init:

```java
@Override
public void onEnable() {
    // ... get curiosApi first
    curiosApi.registerResourcePackSource(this, getFile());
}
```

In your config (or CuriosPaper’s if you’re targeting its namespace):

```yaml
item-model: "myaddon:ring_of_speed"
```

CuriosPaper will:

* Merge your assets into its pack
* Handle namespace rules
* Host the final ZIP

---

## 11. Minimal “Hello Curios” Flow

1. Add CuriosPaper via JitPack.
2. Declare `depend: [CuriosPaper]` in `plugin.yml`.
3. Grab `CuriosPaperAPI` in `onEnable`.
4. Create an enum for your items.
5. Build + tag items using `tagAccessoryItem`.
6. Listen to `AccessoryEquipEvent` and apply effects.
7. (Optional but recommended) Ship models + register them with the RP API.

You now have the same basic architecture that HeadBound uses, just with your own content.

---
