package org.bg52.curiospaper.event;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event fired when a mob is about to equip a 3D model.
 * Allows other plugins to modify the model item, custom model data, and item model component on the fly.
 */
public class CuriosMobModelEquipEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final LivingEntity mob;
    private final String itemId;
    private Material modelMaterial;
    private Integer customModelData;
    private String itemModel;

    public CuriosMobModelEquipEvent(LivingEntity mob, String itemId, Material modelMaterial, Integer customModelData, String itemModel) {
        this.mob = mob;
        this.itemId = itemId;
        this.modelMaterial = modelMaterial;
        this.customModelData = customModelData;
        this.itemModel = itemModel;
    }

    /**
     * Gets the mob equipping the model.
     */
    public LivingEntity getEntity() {
        return mob;
    }

    /**
     * Gets the internal ID of the curios item.
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Gets the material that will be used for the model armor stand helmet.
     */
    public Material getModelMaterial() {
        return modelMaterial;
    }

    /**
     * Sets the material that will be used for the model armor stand helmet.
     */
    public void setModelMaterial(Material modelMaterial) {
        this.modelMaterial = modelMaterial;
    }

    /**
     * Gets the custom model data that will be applied to the model helmet.
     */
    public Integer getCustomModelData() {
        return customModelData;
    }

    /**
     * Sets the custom model data that will be applied to the model helmet.
     */
    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    /**
     * Gets the item model string (for 1.21.4+) that will be applied to the model helmet.
     */
    public String getItemModel() {
        return itemModel;
    }

    /**
     * Sets the item model string (for 1.21.4+) that will be applied to the model helmet.
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
