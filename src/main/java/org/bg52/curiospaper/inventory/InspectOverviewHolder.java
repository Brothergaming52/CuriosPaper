package org.bg52.curiospaper.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * InventoryHolder for Admin Inspect Overview GUI.
 */
public class InspectOverviewHolder implements InventoryHolder {

    private final Player admin;
    private final UUID targetUUID;
    private final String targetName;
    private Inventory inventory;
    private final Map<Integer, String> slotButtonMappings = new HashMap<>();

    public InspectOverviewHolder(Player admin, UUID targetUUID, String targetName) {
        this.admin = admin;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
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

    public void mapSlotButton(int rawSlot, String slotType) {
        slotButtonMappings.put(rawSlot, slotType);
    }

    public String getSlotType(int rawSlot) {
        return slotButtonMappings.get(rawSlot);
    }
}

