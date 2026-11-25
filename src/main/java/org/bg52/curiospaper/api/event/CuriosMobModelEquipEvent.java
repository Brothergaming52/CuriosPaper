package org.bg52.curiospaper.api.event;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event fired when a LivingEntity (mob, NPC, or custom entity) is about to equip a 3D curio model.
 * Allows other plugins to modify the model item, custom model data, and item model component,
 * or cancel the model rendering completely.
 */
public class CuriosMobModelEquipEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final LivingEntity mob;
    private final String slotId;
    private final ItemStack curioStack;

    private Material modelMaterial;
    private Integer customModelData;
    private String itemModel;

    public CuriosMobModelEquipEvent(@NotNull LivingEntity mob,
                                    @NotNull String slotId,
                                    @Nullable ItemStack curioStack,
                                    @NotNull Material modelMaterial,
                                    @Nullable Integer customModelData,
                                    @Nullable String itemModel) {
        this.mob = mob;
        this.slotId = slotId;
        this.curioStack = curioStack;
        this.modelMaterial = modelMaterial;
        this.customModelData = customModelData;
        this.itemModel = itemModel;
    }

    @NotNull
    public LivingEntity getEntity() {
        return mob;
    }

    @NotNull
    public String getSlotId() {
        return slotId;
    }

    @Nullable
    public ItemStack getCurioStack() {
        return curioStack;
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

