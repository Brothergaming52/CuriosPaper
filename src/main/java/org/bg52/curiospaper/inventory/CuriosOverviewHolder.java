package org.bg52.curiospaper.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * InventoryHolder for Tier-1 Main Accessory Overview GUI.
 */
public class CuriosOverviewHolder implements InventoryHolder {

    private final Player player;
    private Inventory inventory;
    private final Map<Integer, String> slotButtonMappings = new HashMap<>();

    public CuriosOverviewHolder(Player player) {
        this.player = player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public void mapSlotButton(int rawSlot, String slotType) {
        slotButtonMappings.put(rawSlot, slotType);
    }

    public String getSlotType(int rawSlot) {
        return slotButtonMappings.get(rawSlot);
    }
}

