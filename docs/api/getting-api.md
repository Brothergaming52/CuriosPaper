# Getting the API

## Setup

### Maven Dependency

Add CuriosPaper as a compile-time dependency:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Brothergaming52</groupId>
    <artifactId>CuriosPaper</artifactId>
    <version>1.3.0</version>
    <scope>provided</scope>
</dependency>
```

### plugin.yml

```yaml
name: MyPlugin
version: 1.0.0
main: com.example.MyPlugin
depend: [CuriosPaper]
```

## Accessing the API Instance

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;

public class MyPlugin extends JavaPlugin {

    private CuriosPaperAPI curiosAPI;

    @Override
    public void onEnable() {
        // Check if CuriosPaper is installed
        if (getServer().getPluginManager().getPlugin("CuriosPaper") == null) {
            getLogger().severe("CuriosPaper is not installed! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Get the API instance
        CuriosPaper curiosPaper = CuriosPaper.getInstance();
        curiosAPI = curiosPaper.getCuriosPaperAPI();

        getLogger().info("CuriosPaper API loaded successfully!");
    }

    // Expose the API for your other classes
    public CuriosPaperAPI getCuriosAPI() {
        return curiosAPI;
    }
}
```

## Soft Dependency Pattern

If CuriosPaper is optional for your plugin:

```java
public class MyPlugin extends JavaPlugin {

    private CuriosPaperAPI curiosAPI = null;
    private boolean curiosEnabled = false;

    @Override
    public void onEnable() {
        // Try to hook into CuriosPaper
        if (getServer().getPluginManager().getPlugin("CuriosPaper") != null) {
            try {
                curiosAPI = CuriosPaper.getInstance().getCuriosPaperAPI();
                curiosEnabled = true;
                getLogger().info("CuriosPaper integration enabled!");
            } catch (Exception e) {
                getLogger().warning("Failed to hook into CuriosPaper: " + e.getMessage());
            }
        } else {
            getLogger().info("CuriosPaper not found, accessory features disabled.");
        }
    }

    public boolean isCuriosEnabled() {
        return curiosEnabled;
    }

    public CuriosPaperAPI getCuriosAPI() {
        return curiosAPI;
    }

    // Example: safely check accessories
    public boolean playerHasRing(Player player) {
        if (!curiosEnabled) return false;
        return curiosAPI.hasEquippedItems(player, "ring");
    }
}
```

## Full API Method Reference

### Item Tagging & Validation

| Method | Return | Description |
|---|---|---|
| `getSlotTypeKey()` | `NamespacedKey` | Get the PDC key used for slot tagging |
| `getItemIdKey()` | `NamespacedKey` | Get the PDC key used for item IDs |
| `isValidAccessory(ItemStack, String)` | `boolean` | Check if tagged for a specific slot |
| `tagAccessoryItem(ItemStack, String)` | `ItemStack` | Tag an item for a slot type (with lore) |
| `tagAccessoryItem(ItemStack, String, boolean)` | `ItemStack` | Tag an item with optional lore |
| `getAccessorySlotType(ItemStack)` | `String` | Get the slot type an item is tagged for |
| `createItemStack(String)` | `ItemStack` | Create an ItemStack from a custom item ID |

### Slot Methods

| Method | Return | Description |
|---|---|---|
| `getAllSlotTypes()` | `List<String>` | Get all registered slot type keys |
| `isValidSlotType(String)` | `boolean` | Check if a slot type exists |
| `getSlotAmount(String)` | `int` | Get the number of slots for a type |
| `registerSlot(...)` | `boolean` | Register a new slot type at runtime |
| `registerSlot(... defaultSlotPosition)` | `boolean` | Register with a default GUI position |
| `unregisterSlot(String)` | `boolean` | Remove a slot type |

### Player Data Methods

| Method | Return | Description |
|---|---|---|
| `getEquippedItems(Player, String)` | `List<ItemStack>` | Get equipped items for a slot |
| `getEquippedItems(UUID, String)` | `List<ItemStack>` | Get equipped items by UUID |
| `setEquippedItems(Player, String, List)` | `void` | Set items in a slot |
| `setEquippedItems(UUID, String, List)` | `void` | Set items by UUID |
| `getEquippedItem(Player, String, int)` | `ItemStack` | Get item at specific index |
| `getEquippedItem(UUID, String, int)` | `ItemStack` | Get item at specific index by UUID |
| `setEquippedItem(Player, String, int, ItemStack)` | `void` | Set item at specific index |
| `setEquippedItem(UUID, String, int, ItemStack)` | `void` | Set item at specific index by UUID |
| `removeEquippedItem(Player, String, ItemStack)` | `boolean` | Remove first matching item |
| `removeEquippedItem(UUID, String, ItemStack)` | `boolean` | Remove first matching item by UUID |
| `removeEquippedItemAt(Player, String, int)` | `ItemStack` | Remove item at index |
| `removeEquippedItemAt(UUID, String, int)` | `ItemStack` | Remove item at index by UUID |
| `clearEquippedItems(Player, String)` | `void` | Clear all items from a slot |
| `clearEquippedItems(UUID, String)` | `void` | Clear all items by UUID |
| `hasEquippedItems(Player, String)` | `boolean` | Check if any items equipped |
| `hasEquippedItems(UUID, String)` | `boolean` | Check if any items equipped by UUID |
| `countEquippedItems(Player, String)` | `int` | Count non-empty slots |
| `countEquippedItems(UUID, String)` | `int` | Count non-empty slots by UUID |

### Item Data Management

| Method | Return | Description |
|---|---|---|
| `getItemData(String)` | `ItemData` | Get custom item data by ID |
| `createItem(String)` | `ItemData` | Create a new custom item |
| `createItem(Plugin, String)` | `ItemData` | Create item with plugin ownership |
| `saveItemData(String)` | `boolean` | Save item data to disk |
| `deleteItem(String)` | `boolean` | Delete a custom item |

### Registration Methods

| Method | Return | Description |
|---|---|---|
| `registerItemRecipe(String, RecipeData)` | `boolean` | Register a crafting recipe |
| `registerItemLootTable(String, LootTableData)` | `boolean` | Register a loot table entry |
| `registerItemMobDrop(String, MobDropData)` | `boolean` | Register a mob drop |
| `registerItemVillagerTrade(String, VillagerTradeData)` | `boolean` | Register a villager trade |

### 3D Model Configuration

| Method | Return | Description |
|---|---|---|
| `setItemModelConfig(String, boolean, String, Integer, String, Float, Float)` | `boolean` | Configure item 3D model |
| `setMobDropModelConfig(String, String, boolean, String, Integer, String)` | `boolean` | Configure mob drop 3D model |

### Resource Pack

| Method | Return | Description |
|---|---|---|
| `registerResourcePackAssets(Plugin, File)` | `void` | Register RP assets from a folder |
| `registerResourcePackAssetsFromJar(Plugin)` | `File` | Extract and register RP assets from JAR |
