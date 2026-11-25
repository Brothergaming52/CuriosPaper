package org.bg52.curiospaper.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * InventoryHolder for admin inspection sessions.
 */
public class InspectGUIHolder implements InventoryHolder {

    private final Player admin;
    private final UUID targetUUID;
    private final String targetName;
    private final PlayerCuriosInventory targetInventory;
    private Inventory inventory;

    private final Map<Integer, CuriosGUIHolder.SlotMapping> slotMappings = new HashMap<>();

    public InspectGUIHolder(Player admin, UUID targetUUID, String targetName, PlayerCuriosInventory targetInventory) {
        this.admin = admin;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.targetInventory = targetInventory;
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

    public PlayerCuriosInventory getTargetInventory() {
        return targetInventory;
    }

    public void mapSlot(int guiSlot, String slotType, int subIndex) {
        slotMappings.put(guiSlot, new CuriosGUIHolder.SlotMapping(slotType, subIndex));
    }

    public CuriosGUIHolder.SlotMapping getMapping(int guiSlot) {
        return slotMappings.get(guiSlot);
    }
}

