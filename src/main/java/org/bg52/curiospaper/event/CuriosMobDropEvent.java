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

  /**
   * Gets the material of the dropped item.
   */
  public org.bukkit.Material getMaterial() {
    return item != null ? item.getType() : null;
  }

  /**
   * Sets the material of the dropped item.
   */
  public void setMaterial(org.bukkit.Material material) {
    if (item != null && material != null) {
      item.setType(material);
    }
  }

  /**
   * Gets the custom model data of the dropped item.
   */
  public Integer getCustomModelData() {
    if (item == null || !item.hasItemMeta())
      return null;
    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
    return meta.hasCustomModelData() ? meta.getCustomModelData() : null;
  }

  /**
   * Sets the custom model data of the dropped item.
   */
  public void setCustomModelData(Integer customModelData) {
    if (item == null)
      return;
    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setCustomModelData(customModelData);
      item.setItemMeta(meta);
    }
  }

  /**
   * Sets the item model string (for 1.21.4+) of the dropped item.
   */
  public void setItemModel(String itemModel) {
    if (item == null)
      return;
    org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, itemModel, getCustomModelData());
      item.setItemMeta(meta);
    }
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
