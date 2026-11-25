package org.bg52.curiospaper.api;

import org.bg52.curiospaper.slot.SlotRegistry;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public developer API for interacting with the CuriosPaper accessory system.
 */
public interface CuriosPaperAPI {

    String HIDE_MODELS_METADATA = "curios_hide_models";

    /**
     * Retrieves the active CuriosPaperAPI instance from Bukkit's ServicesManager.
     */
    static CuriosPaperAPI get() {
        RegisteredServiceProvider<CuriosPaperAPI> rsp = Bukkit.getServicesManager().getRegistration(CuriosPaperAPI.class);
        if (rsp == null || rsp.getProvider() == null) {
            throw new IllegalStateException("CuriosPaperAPI is not available! Is CuriosPaper enabled?");
        }
        return rsp.getProvider();
    }

    /**
     * Safely retrieves the active CuriosPaperAPI instance if present.
     */
    static Optional<CuriosPaperAPI> getOptional() {
        RegisteredServiceProvider<CuriosPaperAPI> rsp = Bukkit.getServicesManager().getRegistration(CuriosPaperAPI.class);
        return rsp != null ? Optional.ofNullable(rsp.getProvider()) : Optional.empty();
    }

    /**
     * Static helper for external plugins & scripts to check if an accessory is equipped.
     */
    static boolean hasAccessoryEquipped(Player player, String itemId) {
        return getOptional().map(api -> api.hasEquipped(player, itemId)).orElse(false);
    }

    /**
     * Static helper to retrieve all equipped accessories for a player.
     */
    static List<ItemStack> getEquippedAccessories(Player player) {
        return getOptional().map(api -> api.getAllEquippedList(player)).orElse(List.of());
    }

    /**
     * Static helper to find an equipped accessory for a player.
     */
    static Optional<ItemStack> getFirstEquipped(Player player, String itemId) {
        return getOptional().flatMap(api -> api.findEquipped(player, itemId));
    }

    /**
     * Checks if a player has an accessory equipped matching the given item ID
     * across any curios slot. Matches custom item IDs from supported engines or material names.
     */
    boolean hasEquipped(Player player, String itemId);

    /**
     * Checks if a player has an accessory equipped matching the given item ID
     * within a specific slot type.
     */
    boolean hasEquipped(Player player, String slotType, String itemId);

    /**
     * Retrieves a flat list of all active non-empty accessories currently equipped by the player.
     */
    List<ItemStack> getAllEquippedList(Player player);

    /**
     * Finds and returns the first equipped accessory matching the given item ID, or empty.
     */
    Optional<ItemStack> findEquipped(Player player, String itemId);

    /**
     * Finds the first equipped accessory matching a custom predicate.
     */
    Optional<ItemStack> findFirst(Player player, java.util.function.Predicate<ItemStack> filter);

    /**
     * Checks if any equipped accessory matches a custom predicate.
     */
    boolean hasEquipped(Player player, java.util.function.Predicate<ItemStack> filter);

    /**
     * Counts the number of equipped accessories matching a custom predicate.
     */
    int countEquipped(Player player, java.util.function.Predicate<ItemStack> filter);

    /**
     * Gets an equipped accessory in a specific slot type and index.
     */
    ItemStack getEquippedItem(Player player, String slotType, int index);

    /**
     * Gets all equipped accessories for a given slot type.
     */
    List<ItemStack> getEquippedItems(Player player, String slotType);

    /**
     * Gets all equipped accessories organized by slot type for a player.
     */
    Map<String, List<ItemStack>> getAllEquippedItems(Player player);

    /**
     * Programmatically sets an equipped accessory.
     */
    void setEquippedItem(Player player, String slotType, int index, ItemStack item);

    /**
     * Checks if the given ItemStack is a valid accessory for any slot.
     */
    boolean isAccessory(ItemStack item);

    /**
     * Resolves the target Curios slot type for an ItemStack, or null if not an accessory.
     */
    String getAccessorySlot(ItemStack item);

    /**
     * Tags an ItemStack as a valid accessory for a slot type, adding PDC metadata and slot lore.
     */
    ItemStack tagAccessoryItem(ItemStack item, String slotType);

    /**
     * Removes the Curios accessory tag from an ItemStack.
     */
    ItemStack untagAccessoryItem(ItemStack item);

    /**
     * Returns the NamespacedKey used to store the slot type in persistent data containers.
     */
    NamespacedKey getSlotTypeKey();

    /**
     * Returns the NamespacedKey used to identify custom item IDs.
     */
    NamespacedKey getItemIdKey();

    /**
     * Opens the Curios accessory inventory for a player.
     */
    void openGUI(Player player);

    /**
     * Opens the administrative inspection GUI to view another player's accessories.
     */
    void openInspectGUI(Player viewer, Player target);

    /**
     * Retrieves the SlotRegistry containing all loaded slot definitions.
     */
    SlotRegistry getSlotRegistry();

    /**
     * Programmatically registers or updates an accessory slot definition.
     */
    default void registerSlot(org.bg52.curiospaper.slot.SlotDefinition def) {
        if (getSlotRegistry() != null && def != null) {
            getSlotRegistry().registerSlot(def);
        }
    }

    /**
     * Resolves the custom item ID from an ItemStack (Synthetix, Nexo, ItemsAdder, Oraxen, CraftEngine, or native).
     */
    String getItemId(ItemStack item);

    /**
     * Creates an ItemStack from a registered item ID across available engines.
     */
    ItemStack createItemStack(String itemId);

    /**
     * Equips a 3D curio accessory model onto a living mob or entity via packet-level armor stand.
     * Fires {@link org.bg52.curiospaper.api.event.CuriosMobModelEquipEvent}.
     *
     * @param entity The living mob/NPC
     * @param slotId Identifier for the model slot (e.g. "head", "back")
     * @param accessory The accessory item to render
     * @return true if equipped successfully, false if cancelled or unsupported
     */
    boolean equipMobModel(LivingEntity entity, String slotId, ItemStack accessory);

    /**
     * Removes a specific 3D curio model from a living mob or entity.
     */
    void removeMobModel(LivingEntity entity, String slotId);

    /**
     * Removes all 3D curio models from a living mob or entity.
     */
    void removeAllMobModels(LivingEntity entity);

    /**
     * Sets separate 3D model properties directly into an ItemStack's persistent data container.
     */
    ItemStack set3DModel(ItemStack item, org.bukkit.Material modelMat, Integer cmd, String itemModel, Float pitchUp, Float pitchDown);
}
