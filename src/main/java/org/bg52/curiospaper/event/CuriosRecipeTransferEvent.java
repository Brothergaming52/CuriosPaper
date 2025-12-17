package org.bg52.curiospaper.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CuriosRecipeTransferEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    private final Inventory inventory;
    private final ItemStack result;
    private final ItemStack source;

    public CuriosRecipeTransferEvent(Inventory inventory, ItemStack result, ItemStack source) {
        this.inventory = inventory;
        this.result = result;
        this.source = source;
    }

    public Inventory getInventory() {
        return inventory;
    }

    /**
     * The result item that will receive the transferred data.
     */
    public ItemStack getResult() {
        return result;
    }

    /**
     * The source item (ingredient) from which data is being transferred.
     */
    public ItemStack getSource() {
        return source;
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
