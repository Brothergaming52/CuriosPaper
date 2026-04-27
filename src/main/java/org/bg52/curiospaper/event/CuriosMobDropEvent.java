package org.bg52.curiospaper.event;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a custom item is dropped by a mob.
 */
public class CuriosMobDropEvent extends Event implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();
  private boolean cancelled = false;

  private final LivingEntity mob;
  private final String customItemId;
  private ItemStack item;

  public CuriosMobDropEvent(LivingEntity mob, String customItemId, ItemStack item) {
    this.mob = mob;
    this.customItemId = customItemId;
    this.item = item;
  }

  /**
   * Gets the mob that is dropping the custom item.
   */
  public LivingEntity getEntity() {
    return mob;
  }

  /**
   * Gets the internal ID of the custom item being dropped.
   */
  public String getCustomItemId() {
    return customItemId;
  }

  /**
   * Gets the dropped item.
   */
  public ItemStack getItem() {
    return item;
  }

  /**
   * Sets the item to be dropped.
   */
  public void setItem(ItemStack item) {
    this.item = item;
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
