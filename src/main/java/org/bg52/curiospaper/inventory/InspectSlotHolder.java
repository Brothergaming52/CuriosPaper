package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.slot.SlotDefinition;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * InventoryHolder for Admin Inspect Slot Items GUI.
 */
public class InspectSlotHolder implements InventoryHolder {

    private final Player admin;
    private final UUID targetUUID;
    private final String targetName;
    private final SlotDefinition slotDef;
    private Inventory inventory;
    private final Map<Integer, Integer> positionToSubIndex = new HashMap<>();

    public InspectSlotHolder(Player admin, UUID targetUUID, String targetName, SlotDefinition slotDef) {
        this.admin = admin;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.slotDef = slotDef;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Player getAdmin() {
        return admin;
    }

    public UUID getTargetUUID() {
        return targetUUID;
    }

    public String getTargetName() {
        return targetName;
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

