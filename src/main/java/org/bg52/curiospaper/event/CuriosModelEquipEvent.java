package org.bg52.curiospaper.event;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a curios item's 3D model is about to be equipped or displayed.
 * This event allows other plugins to modify the model item, custom model data, 
 * and item model component on the fly.
 */
public class CuriosModelEquipEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final ItemStack curiosityStack;
    private final String slotType;
    private final int slotIndex;

    private Material modelMaterial;
    private Integer customModelData;
    private String itemModel;

    public CuriosModelEquipEvent(Player player, ItemStack curiosityStack, String slotType, int slotIndex, Material modelMaterial, Integer customModelData, String itemModel) {
        this.player = player;
        this.curiosityStack = curiosityStack;
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.modelMaterial = modelMaterial;
        this.customModelData = customModelData;
        this.itemModel = itemModel;
    }

    /**
     * Gets the player for whom the model is being equipped.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the curios item stack that triggered the 3D model.
     *
     * @return The curiosity item stack.
     */
    public ItemStack getCuriosityStack() {
        return curiosityStack;
    }

    /**
     * Gets the type of the curios slot (e.g., "ring", "back").
     *
     * @return The slot type.
     */
    public String getSlotType() {
        return slotType;
    }

    /**
     * Gets the index of the curios slot.
     *
     * @return The slot index.
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * Gets the material that will be used for the model armor stand helmet.
     *
     * @return The model material.
     */
    public Material getModelMaterial() {
        return modelMaterial;
    }

    /**
     * Sets the material that will be used for the model armor stand helmet.
     *
     * @param modelMaterial The new model material.
     */
    public void setModelMaterial(Material modelMaterial) {
        this.modelMaterial = modelMaterial;
    }

    /**
     * Gets the custom model data that will be applied to the model helmet.
     *
     * @return The custom model data, or null if none.
     */
    public Integer getCustomModelData() {
        return customModelData;
    }

    /**
     * Sets the custom model data that will be applied to the model helmet.
     *
     * @param customModelData The new custom model data.
     */
    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    /**
     * Gets the item model string (for 1.21.4+) that will be applied to the model helmet.
     *
     * @return The item model string, or null if none.
     */
    public String getItemModel() {
        return itemModel;
    }

    /**
     * Sets the item model string (for 1.21.4+) that will be applied to the model helmet.
     *
     * @param itemModel The new item model string.
     */
    public void setItemModel(String itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
