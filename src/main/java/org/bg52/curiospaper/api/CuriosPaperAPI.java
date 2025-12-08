package org.bg52.curiospaper.api;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public interface CuriosPaperAPI {

    // ========== ITEM TAGGING & VALIDATION ==========

    /**
     * Gets the NamespacedKey used for tagging accessory items
     */
    NamespacedKey getSlotTypeKey();

    /**
     * Checks if an item is a valid accessory for the given slot type
     */
    boolean isValidAccessory(ItemStack itemStack, String slotType);

    /**
     * Tags an item as an accessory for a specific slot type
     * 
     * @param addLore If true, adds a lore line indicating the required slot
     */
    ItemStack tagAccessoryItem(ItemStack itemStack, String slotType, boolean addLore);

    /**
     * Tags an item as an accessory for a specific slot type (no lore added)
     */
    default ItemStack tagAccessoryItem(ItemStack itemStack, String slotType) {
        return tagAccessoryItem(itemStack, slotType, false);
    }

    /**
     * Gets the slot type an item is tagged for, or null if not tagged
     */
    String getAccessorySlotType(ItemStack itemStack);

    // ========== EQUIPPED ITEMS ACCESS ==========

    /**
     * Gets all items equipped in a specific slot type for a player
     */
    List<ItemStack> getEquippedItems(Player player, String slotType);

    /**
     * Gets all items equipped in a specific slot type for a player UUID
     */
    List<ItemStack> getEquippedItems(UUID playerId, String slotType);

    /**
     * Sets all items in a specific slot type for a player
     */
    void setEquippedItems(Player player, String slotType, List<ItemStack> items);

    /**
     * Sets all items in a specific slot type for a player UUID
     */
    void setEquippedItems(UUID playerId, String slotType, List<ItemStack> items);

    /**
     * Gets a specific item at an index within a slot type
     */
    ItemStack getEquippedItem(Player player, String slotType, int index);

    /**
     * Gets a specific item at an index within a slot type
     */
    ItemStack getEquippedItem(UUID playerId, String slotType, int index);

    /**
     * Sets a specific item at an index within a slot type
     */
    void setEquippedItem(Player player, String slotType, int index, ItemStack item);

    /**
     * Sets a specific item at an index within a slot type
     */
    void setEquippedItem(UUID playerId, String slotType, int index, ItemStack item);

    // ========== ITEM REMOVAL ==========

    /**
     * Removes the first matching item from a player's equipped accessories
     * 
     * @return true if an item was removed, false otherwise
     */
    boolean removeEquippedItem(Player player, String slotType, ItemStack itemToRemove);

    /**
     * Removes the first matching item from a player's equipped accessories
     * 
     * @return true if an item was removed, false otherwise
     */
    boolean removeEquippedItem(UUID playerId, String slotType, ItemStack itemToRemove);

    /**
     * Removes an item at a specific index
     * 
     * @return The removed item, or null if the slot was empty
     */
    ItemStack removeEquippedItemAt(Player player, String slotType, int index);

    /**
     * Removes an item at a specific index
     * 
     * @return The removed item, or null if the slot was empty
     */
    ItemStack removeEquippedItemAt(UUID playerId, String slotType, int index);

    /**
     * Clears all items from a specific slot type
     */
    void clearEquippedItems(Player player, String slotType);

    /**
     * Clears all items from a specific slot type
     */
    void clearEquippedItems(UUID playerId, String slotType);

    // ========== CONFIGURATION QUERIES ==========

    /**
     * Checks if a slot type exists in the configuration
     */
    boolean isValidSlotType(String slotType);

    /**
     * Gets the number of slots available for a slot type
     */
    int getSlotAmount(String slotType);

    /**
     * Gets all registered slot type keys
     */
    List<String> getAllSlotTypes();

    // ========== UTILITY ==========

    /**
     * Checks if a player has any items equipped in a specific slot type
     */
    boolean hasEquippedItems(Player player, String slotType);

    /**
     * Checks if a player has any items equipped in a specific slot type
     */
    boolean hasEquippedItems(UUID playerId, String slotType);

    /**
     * Counts the number of non-empty slots for a player in a slot type
     */
    int countEquippedItems(Player player, String slotType);

    /**
     * Counts the number of non-empty slots for a player in a slot type
     */
    /**
     * Counts the number of non-empty slots for a player in a slot type
     */
    int countEquippedItems(UUID playerId, String slotType);

    // ========== RESOURCE PACK ==========

    /**
     * Registers a folder containing resource pack assets to be included in the
     * generated pack.
     * The folder should contain the 'assets' directory structure (e.g.
     * assets/minecraft/textures/...)
     * 
     * @param plugin The plugin registering the assets
     * @param folder The folder containing the assets
     */
    void registerResourcePackAssets(org.bukkit.plugin.Plugin plugin, java.io.File folder);

    java.io.File registerResourcePackAssetsFromJar(org.bukkit.plugin.Plugin plugin);
}