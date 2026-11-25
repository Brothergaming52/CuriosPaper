package org.bg52.curiospaper.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player dies and a curio accessory item is about to be dropped into the world drops.
 * <p>
 * If cancelled, this curio item will NOT be added to world drops. Custom grave plugins, death chest plugins,
 * or soulbound plugins can intercept this event, store the curio item in their grave/chest container,
 * and cancel the drop so no items are lost or duplicated on the ground.
 */
public class PlayerDeathCurioDropEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final String slotType;
    private final int slotIndex;
    private final ItemStack curioItem;
    private boolean keepInCuriosSlot = false;

    public PlayerDeathCurioDropEvent(@NotNull Player player,
                                     @NotNull String slotType,
                                     int slotIndex,
                                     @NotNull ItemStack curioItem) {
        this.player = player;
        this.slotType = slotType;
        this.slotIndex = slotIndex;
        this.curioItem = curioItem;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getSlotType() {
        return slotType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    @NotNull
    public ItemStack getCurioItem() {
        return curioItem;
    }

    /**
     * Whether the item should remain in the player's curio slot instead of being removed from curio inventory.
     * Default is false (grave plugins usually take the item and remove it from slot).
     */
    public boolean isKeepInCuriosSlot() {
        return keepInCuriosSlot;
    }

    public void setKeepInCuriosSlot(boolean keepInCuriosSlot) {
        this.keepInCuriosSlot = keepInCuriosSlot;
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

