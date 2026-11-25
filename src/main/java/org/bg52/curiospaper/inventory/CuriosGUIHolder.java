package org.bg52.curiospaper.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe InventoryHolder identifying an active Curios accessory inventory session.
 */
public class CuriosGUIHolder implements InventoryHolder {

    private final Player player;
    private final PlayerCuriosInventory curiosInventory;
    private Inventory inventory;

    // GUI slot index -> SlotMapping (slotType, subIndex)
    private final Map<Integer, SlotMapping> slotMappings = new HashMap<>();

    public CuriosGUIHolder(Player player, PlayerCuriosInventory curiosInventory) {
        this.player = player;
        this.curiosInventory = curiosInventory;
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

    public PlayerCuriosInventory getCuriosInventory() {
        return curiosInventory;
    }

    public void mapSlot(int guiSlot, String slotType, int subIndex) {
        slotMappings.put(guiSlot, new SlotMapping(slotType, subIndex));
    }

    public SlotMapping getMapping(int guiSlot) {
        return slotMappings.get(guiSlot);
    }

    public boolean isCuriosSlot(int guiSlot) {
        return slotMappings.containsKey(guiSlot);
    }

    public static class SlotMapping {
        private final String slotType;
        private final int subIndex;

        public SlotMapping(String slotType, int subIndex) {
            this.slotType = slotType;
            this.subIndex = subIndex;
        }

        public String getSlotType() {
            return slotType;
        }

        public int getSubIndex() {
            return subIndex;
        }
    }
}

