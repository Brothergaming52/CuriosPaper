# Events

CuriosPaper provides custom Bukkit events that your plugins can listen to. These let you react to accessory changes, build custom game mechanics, and integrate with other systems.

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
        // Count how many "Inferno" set pieces are equipped
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

        // Grant bonus based on number of equipped set pieces
        if (setPieces >= 3) {
            // Full set bonus: Fire Resistance
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, 200, 0, true, false));
            player.sendMessage(ChatColor.GOLD + "★ Inferno Set Bonus: Fire Resistance!");
        }
        if (setPieces >= 2) {
            // Partial set bonus: Strength
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

## Registering Listeners

```java
import org.bukkit.plugin.java.JavaPlugin;

public class MyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register all your CuriosPaper event listeners
        getServer().getPluginManager().registerEvents(new AccessoryLogger(), this);
        getServer().getPluginManager().registerEvents(new AccessoryRestriction(), this);
        getServer().getPluginManager().registerEvents(new SetBonusListener(), this);
    }
}
```
