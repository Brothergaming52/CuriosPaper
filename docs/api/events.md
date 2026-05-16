# Events

CuriosPaper provides custom Bukkit events that your plugins can listen to. These let you react to accessory changes, loot generation, mob drops, and recipe crafting.

## AccessoryEquipEvent

Fired when a player equips, unequips, or swaps an accessory.

### Example 1: Logging Accessory Changes

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AccessoryLogger implements Listener {

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        String slotType = event.getSlotType();
        int slotIndex = event.getSlotIndex();

        switch (event.getAction()) {
            case EQUIP:
                String itemName = event.getNewItem().getItemMeta().getDisplayName();
                player.sendMessage(ChatColor.GREEN + "✓ Equipped " + itemName
                    + " in " + slotType + " slot " + slotIndex);
                break;

            case UNEQUIP:
                String oldName = event.getPreviousItem().getItemMeta().getDisplayName();
                player.sendMessage(ChatColor.RED + "✗ Unequipped " + oldName
                    + " from " + slotType + " slot " + slotIndex);
                break;

            case SWAP:
                player.sendMessage(ChatColor.YELLOW + "↔ Swapped items in "
                    + slotType + " slot " + slotIndex);
                break;
        }
    }
}
```

### Example 2: Restricting Accessories by Permission

Prevent players from equipping certain slot types without permission:

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AccessoryRestriction implements Listener {

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        if (event.getAction() == AccessoryEquipEvent.Action.UNEQUIP) return;

        String slotType = event.getSlotType();
        String permission = "myplugin.slot." + slotType;

        // Check if the player has permission for this slot type
        if (!event.getPlayer().hasPermission(permission)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED
                + "You don't have permission to use " + slotType + " slots!");
        }
    }
}
```

### Example 3: Accessory Set Bonus System

Grant bonus effects when a player has a full set of matching accessories:

```java
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class SetBonusListener implements Listener {

    private final CuriosPaperAPI api;
    private static final String SET_PREFIX = "&c&lInferno ";

    public SetBonusListener() {
        this.api = CuriosPaper.getInstance().getCuriosPaperAPI();
    }

    @EventHandler
    public void onAccessoryChange(AccessoryEquipEvent event) {
        Player player = event.getPlayer();

        // Check after a 1-tick delay so the item is actually in the slot
        player.getServer().getScheduler().runTaskLater(
            CuriosPaper.getInstance(), () -> checkSetBonus(player), 1L);
    }

    private void checkSetBonus(Player player) {
        int setPieces = 0;

        for (String slotType : api.getAllSlotTypes()) {
            List<ItemStack> items = api.getEquippedItems(player, slotType);
            for (ItemStack item : items) {
                if (item != null && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Inferno")) {
                    setPieces++;
                }
            }
        }

        if (setPieces >= 3) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, 200, 0, true, false));
            player.sendMessage(ChatColor.GOLD + "★ Inferno Set Bonus: Fire Resistance!");
        }
        if (setPieces >= 2) {
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.INCREASE_DAMAGE, 200, 0, true, false));
        }
    }
}
```

### Example 4: Economy Integration

Charge players for equipping accessories:

```java
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
// Assumes you have a Vault-compatible economy plugin
// import net.milkbowl.vault.economy.Economy;

public class AccessoryCostListener implements Listener {

    // private Economy economy; // Inject via Vault

    @EventHandler
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        if (event.getAction() != AccessoryEquipEvent.Action.EQUIP) return;

        Player player = event.getPlayer();
        double cost = getSlotCost(event.getSlotType());

        if (cost <= 0) return;

        // Check if player can afford the cost
        // if (!economy.has(player, cost)) {
        //     event.setCancelled(true);
        //     player.sendMessage(ChatColor.RED + "You need $" + cost + " to equip here!");
        //     return;
        // }

        // Charge the player
        // economy.withdrawPlayer(player, cost);
        // player.sendMessage(ChatColor.GREEN + "Charged $" + cost + " for equipping.");
    }

    private double getSlotCost(String slotType) {
        switch (slotType) {
            case "ring": return 100.0;
            case "necklace": return 250.0;
            case "charm": return 50.0;
            default: return 0.0;
        }
    }
}
```

### AccessoryEquipEvent Properties

| Method | Return | Description |
|---|---|---|
| `getPlayer()` | `Player` | The player equipping/unequipping |
| `getSlotType()` | `String` | The slot type key (e.g., "ring") |
| `getSlotIndex()` | `int` | The index within the slot type |
| `getPreviousItem()` | `ItemStack` | The item previously in the slot (may be null) |
| `getNewItem()` | `ItemStack` | The new item being placed (may be null) |
| `getAction()` | `Action` | EQUIP, UNEQUIP, or SWAP |
| `isCancelled()` | `boolean` | Whether the event is cancelled |
| `setCancelled(boolean)` | `void` | Cancel the event |

### Actions

| Action | Previous Item | New Item |
|---|---|---|
| `EQUIP` | `null` | Item being equipped |
| `UNEQUIP` | Item being removed | `null` |
| `SWAP` | Item being replaced | New item |

---

## CuriosLootGenerateEvent

Fired when a custom CuriosPaper item is about to be generated inside a loot container (chest, barrel, brushable block). This event is cancellable and allows modifying the generated item.

### Example: Restricting Loot by World

```java
import org.bg52.curiospaper.event.CuriosLootGenerateEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LootWorldRestriction implements Listener {

    @EventHandler
    public void onLootGenerate(CuriosLootGenerateEvent event) {
        String tableKey = event.getLootTableKey();

        // Only allow custom items in nether loot tables
        if (!tableKey.contains("nether") && !tableKey.contains("bastion")) {
            event.setCancelled(true);
        }
    }
}
```

### Example: Modifying Generated Loot

```java
import org.bg52.curiospaper.event.CuriosLootGenerateEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LootEnhancer implements Listener {

    @EventHandler
    public void onLootGenerate(CuriosLootGenerateEvent event) {
        // Add a special lore tag to all loot-generated accessories
        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();
            lore.add("");
            lore.add("§8§o✦ Found in a dungeon chest ✦");
            meta.setLore(lore);
            item.setItemMeta(meta);
            event.setItem(item);
        }
    }
}
```

### CuriosLootGenerateEvent Properties

| Method | Return | Description |
|---|---|---|
| `getLootTableKey()` | `String` | The namespaced key of the loot table (e.g., `minecraft:chests/simple_dungeon`) |
| `getCustomItemId()` | `String` | The internal ID of the custom item being generated |
| `getItem()` | `ItemStack` | The item being generated |
| `setItem(ItemStack)` | `void` | Replace the item to be generated |
| `isCancelled()` | `boolean` | Whether the generation is cancelled |
| `setCancelled(boolean)` | `void` | Cancel item generation (item will not appear in chest) |

---

## CuriosMobDropEvent

Fired when a custom CuriosPaper item is about to be dropped by a mob on death. This event is cancellable and allows modifying the dropped item.

### Example: Logging Mob Drops

```java
import org.bg52.curiospaper.event.CuriosMobDropEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MobDropLogger implements Listener {

    @EventHandler
    public void onMobDrop(CuriosMobDropEvent event) {
        LivingEntity mob = event.getEntity();
        String itemId = event.getCustomItemId();

        mob.getServer().getLogger().info(
            "[CuriosDrop] " + mob.getType().name()
            + " dropped custom item: " + itemId
            + " at " + mob.getLocation().toVector()
        );
    }
}
```

### Example: Boosting Drops with Looting Enchantment

```java
import org.bg52.curiospaper.event.CuriosMobDropEvent;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class LootingBoost implements Listener {

    @EventHandler
    public void onMobDrop(CuriosMobDropEvent event) {
        // Check if the killer has the looting enchantment
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            int lootingLevel = killer.getInventory()
                .getItemInMainHand().getEnchantmentLevel(Enchantment.LOOTING);

            if (lootingLevel > 0) {
                ItemStack item = event.getItem();
                // Each looting level adds 1 extra item (up to max 5)
                item.setAmount(Math.min(5, item.getAmount() + lootingLevel));
                event.setItem(item);
            }
        }
    }
}
```

### Example: Cancelling Drops in Certain Regions

```java
import org.bg52.curiospaper.event.CuriosMobDropEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RegionDropRestriction implements Listener {

    @EventHandler
    public void onMobDrop(CuriosMobDropEvent event) {
        // Prevent custom drops in the spawn world
        if (event.getEntity().getWorld().getName().equals("world_spawn")) {
            event.setCancelled(true);
        }
    }
}
```

### CuriosMobDropEvent Properties

| Method | Return | Description |
|---|---|---|
| `getEntity()` | `LivingEntity` | The mob that is dropping the item |
| `getCustomItemId()` | `String` | The internal ID of the custom item being dropped |
| `getItem()` | `ItemStack` | The item being dropped |
| `setItem(ItemStack)` | `void` | Replace the item to be dropped |
| `isCancelled()` | `boolean` | Whether the drop is cancelled |
| `setCancelled(boolean)` | `void` | Cancel the drop (item will not appear) |

---

## CuriosRecipeTransferEvent

Fired when a crafting recipe produces a custom CuriosPaper item, allowing you to modify the result or transfer custom data.

### Example: Adding Crafter Attribution

```java
import org.bg52.curiospaper.event.CuriosRecipeTransferEvent;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CraftingAttributionListener implements Listener {

    @EventHandler
    public void onRecipeTransfer(CuriosRecipeTransferEvent event) {
        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta()) return;

        // Add a "Crafted by" lore line to all custom-crafted accessories
        ItemMeta meta = result.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "✦ Hand-crafted accessory");
        meta.setLore(lore);
        result.setItemMeta(meta);
    }
}
```

### CuriosRecipeTransferEvent Properties

| Method | Return | Description |
|---|---|---|
| `getInventory()` | `Inventory` | The crafting inventory |
| `getResult()` | `ItemStack` | The crafted result item |
| `getSource()` | `ItemStack` | The source ingredient item |
| `isCancelled()` | `boolean` | Whether the transfer is cancelled |
| `setCancelled(boolean)` | `void` | Cancel the transfer |

---

---

## CuriosCraftEvent

Fired when a custom CuriosPaper item is crafted, smelted, smith-transformed, or repaired. This event is fired after any data transfer has occurred and allows for final modification of the result item.

### Example: Tracking Crafted Items

```java
import org.bg52.curiospaper.event.CuriosCraftEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CraftTracker implements Listener {

    @EventHandler
    public void onCuriosCraft(CuriosCraftEvent event) {
        String itemId = event.getItemId();
        event.getInventory().getViewers().forEach(player -> 
            player.sendMessage("§aYou just created a: " + itemId));
    }
}
```

### CuriosCraftEvent Properties

| Method | Return | Description |
|---|---|---|
| `getInventory()` | `Inventory` | The inventory where the craft is occurring |
| `getItemId()` | `String` | The internal ID of the custom item being created |
| `getResult()` | `ItemStack` | The result item |
| `setResult(ItemStack)` | `void` | Replace the result item |
| `isCancelled()` | `boolean` | Whether the event is cancelled |
| `setCancelled(boolean)` | `void` | Cancel the craft |

---

## CuriosModelEquipEvent

Fired when a curios item's 3D model is about to be equipped or displayed on a player. This event allows other plugins to modify the model item, material, custom model data, and item model component on the fly.

### Example: Dynamically Changing Model Materials

```java
import org.bg52.curiospaper.event.CuriosModelEquipEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ModelMaterialChanger implements Listener {

    @EventHandler
    public void onModelEquip(CuriosModelEquipEvent event) {
        // Change all models to gold material for a specific player
        if (event.getPlayer().getName().equals("Midas")) {
            event.setModelMaterial(Material.GOLD_BLOCK);
        }
    }
}
```

### CuriosModelEquipEvent Properties

| Method | Return | Description |
|---|---|---|
| `getPlayer()` | `Player` | The player for whom the model is being equipped |
| `getCuriosityStack()` | `ItemStack` | The curios item stack that triggered the 3D model |
| `getSlotType()` | `String` | The type of the curios slot (e.g., "ring", "back") |
| `getSlotIndex()` | `int` | The index of the curios slot |
| `getModelMaterial()` | `Material` | The material that will be used for the model helmet |
| `setModelMaterial(Material)` | `void` | Set the material for the model helmet |
| `getCustomModelData()` | `Integer` | The custom model data that will be applied |
| `setCustomModelData(Integer)` | `void` | Set the custom model data |
| `getItemModel()` | `String` | The item model string (for 1.21.4+) |
| `setItemModel(String)` | `void` | Set the item model string |

---

## CuriosMobModelEquipEvent

Fired when a mob is about to equip a 3D model. Similar to `CuriosModelEquipEvent`, but for entities other than players.

### Example: Randomizing Mob Accessory Models

```java
import org.bg52.curiospaper.event.CuriosMobModelEquipEvent;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.Random;

public class MobModelRandomizer implements Listener {
    private final Random random = new Random();

    @EventHandler
    public void onMobModelEquip(CuriosMobModelEquipEvent event) {
        if (random.nextBoolean()) {
            event.setCustomModelData(1001); // Use a variant model
        }
    }
}
```

### CuriosMobModelEquipEvent Properties

| Method | Return | Description |
|---|---|---|
| `getEntity()` | `LivingEntity` | The mob equipping the model |
| `getItemId()` | `String` | The internal ID of the curios item |
| `getModelMaterial()` | `Material` | The material that will be used for the model helmet |
| `setModelMaterial(Material)` | `void` | Set the material for the model helmet |
| `getCustomModelData()` | `Integer` | The custom model data that will be applied |
| `setCustomModelData(Integer)` | `void` | Set the custom model data |
| `getItemModel()` | `String` | The item model string (for 1.21.4+) |
| `setItemModel(String)` | `void` | Set the item model string |

---

## Event Summary

| Event | When It Fires | Cancellable | Can Modify Item |
|---|---|---|---|
| `AccessoryEquipEvent` | Player equips/unequips/swaps an accessory | ✅ | ❌ |
| `CuriosLootGenerateEvent` | Custom item generated in a loot container | ✅ | ✅ |
| `CuriosMobDropEvent` | Custom item dropped by a killed mob | ✅ | ✅ |
| `CuriosRecipeTransferEvent` | Custom item crafted via a recipe (data transfer) | ✅ | ✅ |
| `CuriosCraftEvent` | Custom item crafted/smelted/repaired (final) | ✅ | ✅ |
| `CuriosModelEquipEvent` | 3D model about to be displayed on player | ✅ | ✅ |
| `CuriosMobModelEquipEvent` | 3D model about to be displayed on mob | ✅ | ✅ |

---

## Registering Listeners

```java
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register all your CuriosPaper event listeners
        getServer().getPluginManager().registerEvents(new AccessoryLogger(), this);
        getServer().getPluginManager().registerEvents(new LootWorldRestriction(), this);
        getServer().getPluginManager().registerEvents(new MobDropLogger(), this);
        getServer().getPluginManager().registerEvents(new CraftingAttributionListener(), this);
        getServer().getPluginManager().registerEvents(new CraftTracker(), this);
        getServer().getPluginManager().registerEvents(new ModelMaterialChanger(), this);
    }
}
```
