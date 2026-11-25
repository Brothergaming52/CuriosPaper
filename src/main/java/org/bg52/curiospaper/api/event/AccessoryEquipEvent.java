package org.bg52.curiospaper.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Event called when a player equips, unequips, or swaps an accessory in a Curios slot.
 */
public class AccessoryEquipEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    public enum Action {
        EQUIP,
        UNEQUIP,
        SWAP
    }

    private final String slotType;
    private final int slotIndex;
    private final ItemStack previousItem;
    private final ItemStack newItem;
    private final Action action;
    private boolean cancelled = false;

    public AccessoryEquipEvent(Player player, String slotType, int slotIndex,
                               ItemStack previousItem, ItemStack newItem, Action action) {
        super(player);
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.previousItem = previousItem != null ? previousItem.clone() : null;
        this.newItem = newItem != null ? newItem.clone() : null;
        this.action = action;
    }

    public String getSlotType() {
        return slotType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public int getSubIndex() {
        return slotIndex;
    }

    public ItemStack getPreviousItem() {
        return previousItem;
    }

    public ItemStack getNewItem() {
        return newItem;
    }

    public Action getAction() {
        return action;
    }

    public boolean hasPreviousItem() {
        return previousItem != null && !previousItem.getType().isAir();
    }

    public boolean hasNewItem() {
        return newItem != null && !newItem.getType().isAir();
    }

    public boolean isEquipping() {
        return action == Action.EQUIP;
    }

    public boolean isUnequipping() {
        return action == Action.UNEQUIP;
    }

    public boolean isSwapping() {
        return action == Action.SWAP;
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

