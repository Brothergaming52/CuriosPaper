package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class PlayerDeathListener implements Listener {
    private final CuriosPaper plugin;

    public PlayerDeathListener(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        ConfigManager.KeepInventoryType keepType = plugin.getConfigManager().getKeepInventoryType();
        
        boolean keepCurios = false;
        
        if (keepType == ConfigManager.KeepInventoryType.ALWAYS) {
            keepCurios = true;
        } else if (keepType == ConfigManager.KeepInventoryType.NEVER) {
            keepCurios = false;
        } else {
            // AUTO - Follow the vanilla keepInventory flag
            keepCurios = event.getKeepInventory();
        }

        if (!keepCurios) {
            // Drop curios
            Map<String, List<ItemStack>> accessories = plugin.getSlotManager().getPlayerAccessoriesMap(player.getUniqueId());
            
            for (List<ItemStack> items : accessories.values()) {
                if (items == null) continue;
                for (ItemStack item : items) {
                    if (item != null && item.getType() != Material.AIR) {
                        event.getDrops().add(item);
                    }
                }
            }
            
            // Clear items from slots
            plugin.getSlotManager().clearAllAccessories(player.getUniqueId());
        }
    }
}
