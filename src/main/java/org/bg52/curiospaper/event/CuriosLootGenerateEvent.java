package org.bg52.curiospaper.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Event fired when a custom item is generated in a loot table (chest, barrel, brushable block).
 */
public class CuriosLootGenerateEvent extends Event implements Cancellable {
  private static final HandlerList HANDLERS = new HandlerList();
  private boolean cancelled = false;

  private final String lootTableKey;
  private final String customItemId;
  private ItemStack item;

  public CuriosLootGenerateEvent(String lootTableKey, String customItemId, ItemStack item) {
    this.lootTableKey = lootTableKey;
    this.customItemId = customItemId;
    this.item = item;
  }

  /**
   * Gets the namespaced key string of the loot table (e.g. "minecraft:chests/simple_dungeon").
   */
  public String getLootTableKey() {
    return lootTableKey;
  }

  /**
   * Gets the internal ID of the custom item being generated.
   */
  public String getCustomItemId() {
    return customItemId;
  }

  /**
   * Gets the generated item.
   */
  public ItemStack getItem() {
    return item;
  }

  /**
   * Sets the item to be generated.
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
