package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.slot.SlotDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * InventoryHolder for Tier-2 Slot Items GUI (shows player's sub-slots for a specific slot type).
 */
public class CuriosSlotHolder implements InventoryHolder {

    private final Player player;
    private final SlotDefinition slotDef;
    private Inventory inventory;
    private final Map<Integer, Integer> positionToSubIndex = new HashMap<>();

    public CuriosSlotHolder(Player player, SlotDefinition slotDef) {
        this.player = player;
        this.slotDef = slotDef;
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

    public SlotDefinition getSlotDef() {
        return slotDef;
    }

    public void mapSlot(int rawSlot, int subIndex) {
        positionToSubIndex.put(rawSlot, subIndex);
    }

    public Integer getSubIndex(int rawSlot) {
        return positionToSubIndex.get(rawSlot);
    }
}

