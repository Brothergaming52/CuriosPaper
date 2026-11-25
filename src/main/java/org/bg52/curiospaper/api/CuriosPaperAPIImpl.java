package org.bg52.curiospaper.api;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.inventory.PlayerCuriosInventory;
import org.bg52.curiospaper.slot.SlotRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Concrete implementation of the CuriosPaperAPI service.
 */
public class CuriosPaperAPIImpl implements CuriosPaperAPI {

    private final CuriosPaper plugin;

    public CuriosPaperAPIImpl(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack getEquippedItem(Player player, String slotType, int index) {
        if (player == null || slotType == null) return null;
        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
        return inv.getItem(slotType, index);
    }

    @Override
    public List<ItemStack> getEquippedItems(Player player, String slotType) {
        if (player == null || slotType == null) return List.of();
        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
        return inv.getItems(slotType);
    }

    @Override
    public Map<String, List<ItemStack>> getAllEquippedItems(Player player) {
        if (player == null) return Map.of();
        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
        return inv.getAllSlots();
    }

    @Override
    public boolean hasEquipped(Player player, String itemId) {
        if (player == null || itemId == null || itemId.isEmpty()) return false;
        return findEquipped(player, itemId).isPresent();
    }

    @Override
    public boolean hasEquipped(Player player, String slotType, String itemId) {
        if (player == null || slotType == null || itemId == null || itemId.isEmpty()) return false;
        List<ItemStack> items = getEquippedItems(player, slotType);
        for (ItemStack item : items) {
            if (matchesItemId(item, itemId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ItemStack> getAllEquippedList(Player player) {
        if (player == null) return List.of();
        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
        if (inv == null) return List.of();
        List<ItemStack> list = new ArrayList<>();
        for (List<ItemStack> slotItems : inv.getAllSlots().values()) {
            if (slotItems != null) {
                for (ItemStack it : slotItems) {
                    if (it != null && !it.getType().isAir()) {
                        list.add(it);
                    }
                }
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public Optional<ItemStack> findEquipped(Player player, String itemId) {
        if (player == null || itemId == null) return Optional.empty();
        for (ItemStack item : getAllEquippedList(player)) {
            if (matchesItemId(item, itemId)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ItemStack> findFirst(Player player, java.util.function.Predicate<ItemStack> filter) {
        if (player == null || filter == null) return Optional.empty();
        for (ItemStack item : getAllEquippedList(player)) {
            if (filter.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean hasEquipped(Player player, java.util.function.Predicate<ItemStack> filter) {
        return findFirst(player, filter).isPresent();
    }

    @Override
    public int countEquipped(Player player, java.util.function.Predicate<ItemStack> filter) {
        if (player == null || filter == null) return 0;
        int count = 0;
        for (ItemStack item : getAllEquippedList(player)) {
            if (filter.test(item)) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesItemId(ItemStack item, String targetId) {
        if (item == null || item.getType().isAir()) return false;
        String resolvedId = getItemId(item);
        if (resolvedId != null) {
            if (resolvedId.equalsIgnoreCase(targetId)) return true;
            if (targetId.startsWith("synth:") && resolvedId.equalsIgnoreCase(targetId.substring(6))) return true;
            if (resolvedId.startsWith("synth:") && targetId.equalsIgnoreCase(resolvedId.substring(6))) return true;
        }
        return item.getType().name().equalsIgnoreCase(targetId);
    }

    @Override
    public void setEquippedItem(Player player, String slotType, int index, ItemStack item) {
        if (player == null || slotType == null) return;
        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
        ItemStack previous = inv.getItem(slotType, index);

        ItemStack newItem = (item != null && !item.getType().isAir()) ? item.clone() : null;
        if (newItem != null) {
            newItem.setAmount(1);
        }

        org.bg52.curiospaper.api.event.AccessoryEquipEvent.Action action;
        if (previous != null && newItem != null) {
            action = org.bg52.curiospaper.api.event.AccessoryEquipEvent.Action.SWAP;
        } else if (newItem != null) {
            action = org.bg52.curiospaper.api.event.AccessoryEquipEvent.Action.EQUIP;
        } else {
            action = org.bg52.curiospaper.api.event.AccessoryEquipEvent.Action.UNEQUIP;
        }

        org.bg52.curiospaper.api.event.AccessoryEquipEvent event = new org.bg52.curiospaper.api.event.AccessoryEquipEvent(
                player, slotType, index, previous, newItem, action
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        inv.setItem(slotType, index, newItem);

        if (previous != null) {
            plugin.getItemEngineManager().notifyUnequip(player, previous);
        }
        if (newItem != null) {
            plugin.getItemEngineManager().notifyEquip(player, newItem);
        }

        plugin.getPacketModelManager().updatePlayerAccessories(player);
        plugin.getStorageManager().savePlayer(player.getUniqueId());
    }

    @Override
    public boolean isAccessory(ItemStack item) {
        return getAccessorySlot(item) != null;
    }

    @Override
    public String getAccessorySlot(ItemStack item) {
        return plugin.getItemEngineManager().resolveCuriosSlot(item);
    }

    @Override
    public ItemStack tagAccessoryItem(ItemStack item, String slotType) {
        return plugin.getItemEngineManager().tagAccessoryItem(item, slotType);
    }

    @Override
    public ItemStack untagAccessoryItem(ItemStack item) {
        return plugin.getItemEngineManager().untagAccessoryItem(item);
    }

    @Override
    public NamespacedKey getSlotTypeKey() {
        return plugin.getItemEngineManager().getSlotTypeKey();
    }

    @Override
    public NamespacedKey getItemIdKey() {
        return plugin.getItemEngineManager().getItemIdKey();
    }

    @Override
    public void openGUI(Player player) {
        if (player != null && player.isOnline()) {
            plugin.getCuriosGUI().open(player);
        }
    }

    @Override
    public void openInspectGUI(Player viewer, Player target) {
        if (viewer != null && target != null && viewer.isOnline() && target.isOnline()) {
            plugin.getInspectGUI().open(viewer, target);
        }
    }

    @Override
    public SlotRegistry getSlotRegistry() {
        return plugin.getSlotRegistry();
    }

    @Override
    public String getItemId(ItemStack item) {
        return plugin.getItemEngineManager().getItemId(item);
    }

    @Override
    public ItemStack createItemStack(String itemId) {
        return plugin.getItemEngineManager().createItemStack(itemId);
    }

    @Override
    public boolean equipMobModel(LivingEntity entity, String slotId, ItemStack accessory) {
        return plugin.getPacketModelManager().equipMobModel(entity, slotId, accessory);
    }

    @Override
    public void removeMobModel(LivingEntity entity, String slotId) {
        plugin.getPacketModelManager().removeMobModel(entity, slotId);
    }

    @Override
    public void removeAllMobModels(LivingEntity entity) {
        plugin.getPacketModelManager().removeAllMobModels(entity);
    }

    @Override
    public ItemStack set3DModel(ItemStack item, org.bukkit.Material modelMat, Integer cmd, String itemModel, Float pitchUp, Float pitchDown) {
        return plugin.getItemEngineManager().set3DModel(item, modelMat, cmd, itemModel, pitchUp, pitchDown);
    }
}
