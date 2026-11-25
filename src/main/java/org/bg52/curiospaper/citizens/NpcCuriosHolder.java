package org.bg52.curiospaper.citizens;

import net.citizensnpcs.api.npc.NPC;
import org.bg52.curiospaper.inventory.CuriosGUIHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * InventoryHolder for the NPC Curios GUI.
 */
public class NpcCuriosHolder implements InventoryHolder {

    private final NPC npc;
    private final CuriosTrait trait;
    private Inventory inventory;
    private final Map<Integer, CuriosGUIHolder.SlotMapping> slotMappings = new HashMap<>();

    public NpcCuriosHolder(NPC npc, CuriosTrait trait) {
        this.npc = npc;
        this.trait = trait;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public NPC getNpc() {
        return npc;
    }

    public CuriosTrait getTrait() {
        return trait;
    }

    public void mapSlot(int guiSlot, String slotType, int subIndex) {
        slotMappings.put(guiSlot, new CuriosGUIHolder.SlotMapping(slotType, subIndex));
    }

    public CuriosGUIHolder.SlotMapping getMapping(int guiSlot) {
        return slotMappings.get(guiSlot);
    }
}

