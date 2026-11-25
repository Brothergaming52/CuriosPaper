package org.bg52.curiospaper.citizens;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.api.util.ItemStorage;
import org.bg52.curiospaper.CuriosPaper;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Citizens Trait allowing Citizens NPCs (Player, Zombie, Skeleton, Piglin, etc.)
 * to equip Curios accessories and display their 3D models via PacketModelManager.
 */
@TraitName("curios")
public class CuriosTrait extends Trait {

    private final Map<String, ItemStack> equippedItems = new ConcurrentHashMap<>();

    public CuriosTrait() {
        super("curios");
    }

    @Override
    public void onAttach() {
        if (npc != null && npc.isSpawned()) {
            rescanModels();
        }
    }

    @Override
    public void onSpawn() {
        rescanModels();
    }

    @Override
    public void onDespawn() {
        if (npc != null && npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            CuriosPaper.getInstance().getPacketModelManager().removeAllMobModels((org.bukkit.entity.LivingEntity) npc.getEntity());
        }
    }

    @Override
    public void onRemove() {
        if (npc != null && npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            CuriosPaper.getInstance().getPacketModelManager().removeAllMobModels((org.bukkit.entity.LivingEntity) npc.getEntity());
        }
        equippedItems.clear();
    }

    public void rescanModels() {
        if (npc == null || !npc.isSpawned() || !(npc.getEntity() instanceof org.bukkit.entity.LivingEntity)) return;

        CuriosPaper plugin = CuriosPaper.getInstance();
        org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) npc.getEntity();
        for (Map.Entry<String, ItemStack> entry : equippedItems.entrySet()) {
            String key = entry.getKey();
            ItemStack item = entry.getValue();
            if (item != null && item.getType() != Material.AIR) {
                String slotType = key.contains("_") ? key.substring(0, key.indexOf('_')) : key;
                plugin.getPacketModelManager().equipMobModel(living, slotType, item);
            }
        }
    }

    @Override
    public void load(DataKey key) {
        equippedItems.clear();
        if (key.keyExists("items")) {
            DataKey itemsKey = key.getRelative("items");
            for (DataKey subKey : itemsKey.getSubKeys()) {
                try {
                    ItemStack item = ItemStorage.loadItemStack(subKey);
                    if (item != null && item.getType() != Material.AIR) {
                        equippedItems.put(subKey.name(), item);
                    }
                } catch (Throwable t) {
                    CuriosPaper.getInstance().getLogger().warning(
                            "[CuriosTrait] Failed to load NPC item '" + subKey.name() + "': " + t.getMessage());
                }
            }
        }
    }

    @Override
    public void save(DataKey key) {
        key.removeKey("items");
        if (!equippedItems.isEmpty()) {
            DataKey itemsKey = key.getRelative("items");
            for (Map.Entry<String, ItemStack> entry : equippedItems.entrySet()) {
                ItemStack item = entry.getValue();
                if (item != null && item.getType() != Material.AIR) {
                    DataKey itemKey = itemsKey.getRelative(entry.getKey());
                    ItemStorage.saveItem(itemKey, item);
                }
            }
        }
    }

    public ItemStack getEquippedItem(String slotType, int index) {
        return equippedItems.get(slotType.toLowerCase(Locale.ROOT) + "_" + index);
    }

    public void setEquippedItem(String slotType, int index, ItemStack item) {
        String key = slotType.toLowerCase(Locale.ROOT) + "_" + index;
        if (item == null || item.getType() == Material.AIR) {
            equippedItems.remove(key);
            if (npc != null && npc.isSpawned() && npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                CuriosPaper.getInstance().getPacketModelManager().removeMobModel((org.bukkit.entity.LivingEntity) npc.getEntity(), slotType);
            }
        } else {
            equippedItems.put(key, item.clone());
            if (npc != null && npc.isSpawned() && npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
                CuriosPaper.getInstance().getPacketModelManager().equipMobModel((org.bukkit.entity.LivingEntity) npc.getEntity(), slotType, item);
            }
        }
    }

    public void clearAllItems() {
        equippedItems.clear();
        if (npc != null && npc.isSpawned() && npc.getEntity() instanceof org.bukkit.entity.LivingEntity) {
            CuriosPaper.getInstance().getPacketModelManager().removeAllMobModels((org.bukkit.entity.LivingEntity) npc.getEntity());
        }
    }

    public Map<String, ItemStack> getAllEquipped() {
        return Collections.unmodifiableMap(equippedItems);
    }

    public boolean isHumanoid() {
        if (npc == null || npc.getEntity() == null) return false;
        EntityType type = npc.getEntity().getType();
        return type == EntityType.PLAYER || type == EntityType.ZOMBIE || type == EntityType.SKELETON
                || type == EntityType.WITHER_SKELETON || type == EntityType.HUSK || type == EntityType.DROWNED
                || type == EntityType.STRAY || type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.ZOMBIFIED_PIGLIN || type == EntityType.VILLAGER || type == EntityType.WANDERING_TRADER;
    }
}
