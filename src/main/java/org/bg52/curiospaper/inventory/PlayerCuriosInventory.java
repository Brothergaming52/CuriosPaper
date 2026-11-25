package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.slot.SlotDefinition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * In-memory container holding a player's active curios accessories.
 */
public class PlayerCuriosInventory {

    private final UUID playerUUID;
    // slotType -> List of equipped items
    private final Map<String, List<ItemStack>> slots = new HashMap<>();

    public PlayerCuriosInventory(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public synchronized ItemStack getItem(String slotType, int index) {
        List<ItemStack> list = slots.get(slotType.toLowerCase(Locale.ROOT));
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        ItemStack item = list.get(index);
        return (item != null && item.getType() != Material.AIR) ? item : null;
    }

    public synchronized void setItem(String slotType, int index, ItemStack item) {
        String key = slotType.toLowerCase(Locale.ROOT);
        List<ItemStack> list = slots.computeIfAbsent(key, k -> new ArrayList<>());
        while (list.size() <= index) {
            list.add(null);
        }
        list.set(index, (item != null && item.getType() != Material.AIR) ? item.clone() : null);
    }

    public synchronized List<ItemStack> getItems(String slotType) {
        List<ItemStack> list = slots.get(slotType.toLowerCase(Locale.ROOT));
        if (list == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    public synchronized Map<String, List<ItemStack>> getAllSlots() {
        Map<String, List<ItemStack>> copy = new HashMap<>();
        for (Map.Entry<String, List<ItemStack>> entry : slots.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public synchronized void loadFromMap(Map<String, List<ItemStack>> map) {
        slots.clear();
        if (map != null) {
            for (Map.Entry<String, List<ItemStack>> entry : map.entrySet()) {
                slots.put(entry.getKey().toLowerCase(Locale.ROOT), new ArrayList<>(entry.getValue()));
            }
        }
    }

    public int getUnlockedSlots(SlotDefinition def, Player player) {
        if (def == null) return 0;
        int unlocked = def.getDefaultUnlocked();
        if (player != null) {
            for (int i = def.getMaxSlots(); i > unlocked; i--) {
                if (player.hasPermission("curiospaper.slot." + def.getId() + "." + i)
                        || player.hasPermission("curiospaper.slot." + def.getId() + ".*")) {
                    return i;
                }
            }
        }
        return unlocked;
    }

    public synchronized int getFirstEmptySlot(SlotDefinition def, Player player) {
        int unlocked = getUnlockedSlots(def, player);
        List<ItemStack> list = slots.computeIfAbsent(def.getId(), k -> new ArrayList<>());
        for (int i = 0; i < unlocked; i++) {
            if (i >= list.size() || list.get(i) == null || list.get(i).getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }

    public synchronized boolean hasAnyItem(String slotType) {
        List<ItemStack> list = slots.get(slotType.toLowerCase(Locale.ROOT));
        if (list == null) return false;
        for (ItemStack item : list) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasAnyEquippedItems() {
        for (List<ItemStack> list : slots.values()) {
            if (list == null) continue;
            for (ItemStack item : list) {
                if (item != null && item.getType() != Material.AIR) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized int countEquipped(String slotType) {
        List<ItemStack> list = slots.get(slotType.toLowerCase(Locale.ROOT));
        if (list == null) return 0;
        int count = 0;
        for (ItemStack item : list) {
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }

    public synchronized void clear() {
        slots.clear();
    }

    public synchronized void clear(String slotType) {
        slots.remove(slotType.toLowerCase(Locale.ROOT));
    }
}

