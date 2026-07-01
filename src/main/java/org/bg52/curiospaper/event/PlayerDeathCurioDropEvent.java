package org.bg52.curiospaper.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a player dies and their curios items are being dropped.
 * If cancelled, this curio item will not drop on death and will remain equipped.
 */
public class PlayerDeathCurioDropEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final String slotType;
    private final int slotIndex;
    private final ItemStack curioItem;

    public PlayerDeathCurioDropEvent(Player player, String slotType, int slotIndex, ItemStack curioItem) {
        this.player = player;
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.curioItem = curioItem;
    }

    /**
     * Gets the player who died.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the slot type (e.g., "back", "ring").
     */
    public String getSlotType() {
        return slotType;
    }

    /**
     * Gets the index of the slot within the slot type.
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * Gets the curio item being dropped.
     */
    public ItemStack getCurioItem() {
        return curioItem;
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
