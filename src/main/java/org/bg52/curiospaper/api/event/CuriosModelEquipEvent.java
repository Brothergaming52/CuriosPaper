package org.bg52.curiospaper.api.event;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a curios item's 3D model is about to be equipped or rendered on a player.
 * Allows other plugins to modify the model item, custom model data, and item model component on the fly,
 * or cancel the model from being equipped.
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

    public CuriosModelEquipEvent(@NotNull Player player,
                                 @NotNull ItemStack curiosityStack,
                                 @NotNull String slotType,
                                 int slotIndex,
                                 @NotNull Material modelMaterial,
                                 @Nullable Integer customModelData,
                                 @Nullable String itemModel) {
        this.player = player;
        this.curiosityStack = curiosityStack;
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.modelMaterial = modelMaterial;
        this.customModelData = customModelData;
        this.itemModel = itemModel;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public ItemStack getCuriosityStack() {
        return curiosityStack;
    }

    @NotNull
    public String getSlotType() {
        return slotType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    @NotNull
    public Material getModelMaterial() {
        return modelMaterial;
    }

    public void setModelMaterial(@NotNull Material modelMaterial) {
        this.modelMaterial = modelMaterial;
    }

    @Nullable
    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(@Nullable Integer customModelData) {
        this.customModelData = customModelData;
    }

    @Nullable
    public String getItemModel() {
        return itemModel;
    }

    public void setItemModel(@Nullable String itemModel) {
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

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

