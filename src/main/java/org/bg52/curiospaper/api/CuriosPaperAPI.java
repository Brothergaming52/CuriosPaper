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
        return tagAccessoryItem(itemStack, slotType, true);
    }

    /**
     * Gets the slot type an item is tagged for, or null if not tagged
     */
    String getAccessorySlotType(ItemStack itemStack);

    /**
     * Gets the NamespacedKey used for identifying custom items
     */
    NamespacedKey getItemIdKey();

    /**
     * Creates an ItemStack for a custom item by its ID.
     * Use this instead of manually creating items to ensure they
     * have the correct NBT data for recipes.
     * 
     * @param itemId The unique item identifier
     * @return The ItemStack, or null if the item doesn't exist
     */
    ItemStack createItemStack(String itemId);

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

    // ========== SLOT REGISTRATION ==========

    /**
     * Registers a new custom slot type at runtime.
     * This allows plugins to dynamically add slot types without modifying
     * config.yml.
     * 
     * @param slotType        The unique identifier for the slot type
     * @param displayName     The display name shown in the GUI
     * @param icon            The material to use as the icon
     * @param itemModel       The item model key (namespace:path format)
     * @param customModelData The CustomModelData value for older versions (can be
     *                        null)
     * @param amount          The number of slots available for this type
     * @param lore            The lore to display on the slot icon
     * @return true if registration was successful, false otherwise
     */
    boolean registerSlot(String slotType, String displayName, org.bukkit.Material icon,
            String itemModel, Integer customModelData, int amount, java.util.List<String> lore);

    /**
     * Unregisters a custom slot type.
     * Warning: This will not remove items already equipped in this slot.
     * 
     * @param slotType The slot type to unregister
     * @return true if unregistration was successful, false otherwise
     */
    boolean unregisterSlot(String slotType);

    // ========== ITEM DATA MANAGEMENT ==========

    /**
     * Registers a recipe for a custom item
     * 
     * @param itemId The unique item identifier
     * @param recipe The recipe data
     * @return true if registration was successful
     */
    boolean registerItemRecipe(String itemId, org.bg52.curiospaper.data.RecipeData recipe);

    /**
     * Registers a loot table entry for a custom item
     * 
     * @param itemId    The unique item identifier
     * @param lootTable The loot table data
     * @return true if registration was successful
     */
    boolean registerItemLootTable(String itemId, org.bg52.curiospaper.data.LootTableData lootTable);

    /**
     * Registers a mob drop for a custom item
     * 
     * @param itemId  The unique item identifier
     * @param mobDrop The mob drop data
     * @return true if registration was successful
     */
    boolean registerItemMobDrop(String itemId, org.bg52.curiospaper.data.MobDropData mobDrop);

    /**
     * Gets the item data for a custom item
     * 
     * @param itemId The unique item identifier
     * @return The item data, or null if not found
     */
    org.bg52.curiospaper.data.ItemData getItemData(String itemId);

    /**
     * Creates a new custom item with the given ID
     * 
     * @param itemId The unique item identifier
     * @return The created ItemData, or null if creation failed
     */
    org.bg52.curiospaper.data.ItemData createItem(String itemId);

    /**
     * Creates a new custom item with the given ID and owning plugin.
     * Use this when creating items from an external plugin to ensure they are
     * cleaned up if the plugin is removed.
     * 
     * @param plugin The plugin creating the item
     * @param itemId The unique item identifier
     * @return The created ItemData, or null if creation failed
     */
    org.bg52.curiospaper.data.ItemData createItem(org.bukkit.plugin.Plugin plugin, String itemId);

    /**
     * Saves item data to disk
     * 
     * @param itemId The unique item identifier
     * @return true if save was successful
     */
    boolean saveItemData(String itemId);

    /**
     * Registers a villager trade for a custom item
     * 
     * @param itemId The unique item identifier
     * @param trade  The villager trade data
     * @return true if registration was successful
     */
    boolean registerItemVillagerTrade(String itemId, org.bg52.curiospaper.data.VillagerTradeData trade);

    /**
     * Deletes a custom item
     * 
     * @param itemId The unique item identifier
     * @return true if deletion was successful
     */
    boolean deleteItem(String itemId);

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