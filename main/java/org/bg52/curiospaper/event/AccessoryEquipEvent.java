package org.bg52.curiospaper.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a player equips, unequips, or swaps an accessory item.
 * This event is called after the inventory change has been processed.
 */
public class AccessoryEquipEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final String slotType;
    private final int slotIndex;
    private final ItemStack previousItem;
    private final ItemStack newItem;
    private final Action action;

    public enum Action {
        EQUIP,    // Item was placed in empty slot
        UNEQUIP,  // Item was removed from slot
        SWAP      // Item was replaced with different item
    }

    public AccessoryEquipEvent(Player player, String slotType, int slotIndex,
                               ItemStack previousItem, ItemStack newItem, Action action) {
        this.player = player;
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.previousItem = previousItem;
        this.newItem = newItem;
        this.action = action;
    }

    /**
     * Gets the player who equipped/unequipped the accessory
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the slot type (e.g., "ring", "necklace")
     */
    public String getSlotType() {
        return slotType;
    }

    /**
     * Gets the index of the slot within the slot type
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * Gets the item that was previously in the slot (null if empty)
     */
    public ItemStack getPreviousItem() {
        return previousItem;
    }

    /**
     * Gets the item that is now in the slot (null if empty)
     */
    public ItemStack getNewItem() {
        return newItem;
    }

    /**
     * Gets the action type (EQUIP, UNEQUIP, or SWAP)
     */
    public Action getAction() {
        return action;
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
