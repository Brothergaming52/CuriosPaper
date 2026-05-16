package org.bg52.curiospaper.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a curios item is crafted, smelted, smith-transformed, or repaired.
 * This event is fired after any data transfer has occurred and allows for final modification
 * of the result item.
 */
public class CuriosCraftEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final Inventory inventory;
    private final String itemId;
    private ItemStack result;

    public CuriosCraftEvent(Inventory inventory, String itemId, ItemStack result) {
        this.inventory = inventory;
        this.itemId = itemId;
        this.result = result;
    }

    /**
     * Gets the inventory where the craft is occurring.
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Gets the internal ID of the custom item being created.
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Gets the result item.
     */
    public ItemStack getResult() {
        return result;
    }

    /**
     * Sets the result item.
     */
    public void setResult(ItemStack result) {
        this.result = result;
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
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
