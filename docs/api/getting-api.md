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
    <version>1.2.0</version>
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

### Slot Methods

| Method | Return | Description |
|---|---|---|
| `getAllSlotTypes()` | `List<String>` | Get all registered slot type keys |
| `isValidSlotType(String)` | `boolean` | Check if a slot type exists |
| `getSlotAmount(String)` | `int` | Get the number of slots for a type |
| `registerSlot(...)` | `boolean` | Register a new slot type at runtime |
| `unregisterSlot(String)` | `boolean` | Remove a slot type |

### Item Methods

| Method | Return | Description |
|---|---|---|
| `isValidAccessory(ItemStack)` | `boolean` | Check if an item is tagged as an accessory |
| `isValidAccessory(ItemStack, String)` | `boolean` | Check if tagged for a specific slot |
| `getAccessorySlotType(ItemStack)` | `String` | Get the slot type an item is tagged for |
| `getSlotTypeKey(ItemStack)` | `String` | Get the slot type key from PDC |
| `tagAccessoryItem(ItemStack, String)` | `ItemStack` | Tag an item for a slot type |
| `getItemData(String)` | `ItemData` | Get custom item data by ID |
| `createItem(String)` | `ItemData` | Create a new custom item |
| `createItem(Plugin, String)` | `ItemData` | Create item with plugin ownership |
| `saveItemData(String)` | `boolean` | Save item data to disk |
| `deleteItem(String)` | `boolean` | Delete a custom item |

### Player Data Methods

| Method | Return | Description |
|---|---|---|
| `getEquippedItems(Player, String)` | `List<ItemStack>` | Get equipped items for a slot |
| `getEquippedItems(UUID, String)` | `List<ItemStack>` | Get equipped items by UUID |
| `setEquippedItems(Player, String, List)` | `void` | Set items in a slot |
| `setEquippedItems(UUID, String, List)` | `void` | Set items by UUID |
| `clearEquippedItems(UUID, String)` | `void` | Clear all items from a slot |
| `hasEquippedItems(Player, String)` | `boolean` | Check if any items equipped |
| `countEquippedItems(Player, String)` | `int` | Count non-empty slots |

### Registration Methods

| Method | Return | Description |
|---|---|---|
| `registerItemRecipe(String, RecipeData)` | `boolean` | Register a recipe |
| `registerItemLootTable(String, LootTableData)` | `boolean` | Register a loot table |
| `registerItemMobDrop(String, MobDropData)` | `boolean` | Register a mob drop |
| `registerItemVillagerTrade(String, VillagerTradeData)` | `boolean` | Register a trade |
| `registerResourcePackAssets(Plugin, File)` | `void` | Register RP assets |
