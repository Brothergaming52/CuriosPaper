package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.event.CuriosCraftEvent;
import org.bg52.curiospaper.event.CuriosRecipeTransferEvent;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom 3-row Chest GUI for Bedrock players when using Smithing Tables.
 * Layout:
 * - Slot 10: Template Item (1.20+) [Icon 10010] or Base Item (Pre-1.20) [Icon 10011]
 * - Slot 11: Base Item (1.20+) [Icon 10011] or Addition Item (Pre-1.20) [Icon 10012]
 * - Slot 12: Addition Item (1.20+) [Icon 10012]
 * - Slot 14: Recipe Status Icon (Error 10014 / Arrow 10013)
 * - Slot 16: Result item (spaced away on the right side)
 * - Slot 8:  Vanilla Smithing Table button
 */
public class BedrockSmithingGUI implements Listener, InventoryHolder {

    private final CuriosPaper plugin;
    private final NamespacedKey placeholderKey;
    private final String title = ChatColor.DARK_GRAY + "Smithing Table";
    private final Map<UUID, SmithingState> activeSessions = new HashMap<>();
    private static final ThreadLocal<Boolean> firingMockEvent = ThreadLocal.withInitial(() -> false);

    public BedrockSmithingGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.placeholderKey = new NamespacedKey(plugin, "gui_placeholder");
    }

    private static class SmithingState {
        ItemStack result = null;
        boolean isCustomRecipe = false;
        String customItemId = null;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, 27, title);

        // Fill background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Set placeholder items in interactive input slots
        inv.setItem(10, getPlaceholderForSlot(10));
        inv.setItem(11, getPlaceholderForSlot(11));
        inv.setItem(12, getPlaceholderForSlot(12));

        // Recipe indicator arrow/error in slot 14
        inv.setItem(14, createErrorIcon());

        // Result slot empty initially
        inv.setItem(16, null);

        // Vanilla Smithing Table button in top right corner (Slot 8)
        ItemStack vanillaBtn = createItem(Material.SMITHING_TABLE, ChatColor.YELLOW + "Open Vanilla Smithing Table",
                ChatColor.GRAY + "Click to open vanilla Smithing Table",
                ChatColor.GRAY + "interface for armor trimming & upgrades.");
        inv.setItem(8, vanillaBtn);

        activeSessions.put(player.getUniqueId(), new SmithingState());
        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (firingMockEvent.get()) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getHolder() != this) return;

        Inventory inv = event.getInventory();
        int slot = event.getRawSlot();

        boolean hasTemplate = VersionUtil.supportsSmithingTemplate();

        // Player Inventory Interactivity (slot >= 27)
        if (slot >= 27) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR || isPlaceholder(clickedItem)) {
                    return;
                }

                int targetSlot;
                if (hasTemplate) {
                    if (isTemplateItem(clickedItem)) {
                        targetSlot = 10;
                    } else if (isOreItem(clickedItem)) {
                        targetSlot = 12;
                    } else {
                        targetSlot = 11;
                    }
                } else {
                    if (isOreItem(clickedItem)) {
                        targetSlot = 11;
                    } else {
                        targetSlot = 10;
                    }
                }

                ItemStack slotItem = inv.getItem(targetSlot);
                if (slotItem == null || slotItem.getType() == Material.AIR || isPlaceholder(slotItem)) {
                    inv.setItem(targetSlot, clickedItem);
                    event.setCurrentItem(null);
                    player.updateInventory();
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        updateRecipe(inv, player);
                        player.updateInventory();
                    });
                }
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    updateRecipe(inv, player);
                    player.updateInventory();
                });
            }
            return;
        }

        // Top right slot: Open real Smithing GUI
        if (slot == 8) {
            event.setCancelled(true);
            returnItemsAndCloseForVanillaSmithing(player, inv);
            return;
        }

        boolean isInputSlot = (slot == 10 || slot == 11 || (hasTemplate && slot == 12));
        boolean isResultSlot = (slot == 16);

        if (!isInputSlot && !isResultSlot) {
            event.setCancelled(true);
            return;
        }

        if (isInputSlot) {
            event.setCancelled(false); // Uncancel for Geyser compatibility
            ItemStack currentSlotItem = inv.getItem(slot);
            ItemStack cursorItem = event.getCursor();

            boolean slotHasPlaceholder = isPlaceholder(currentSlotItem);
            boolean cursorHasItem = (cursorItem != null && cursorItem.getType() != Material.AIR);

            if (event.isShiftClick()) {
                if (currentSlotItem != null && !slotHasPlaceholder) {
                    // Let vanilla handle the shift-click back to player inventory
                    // But we must restore the placeholder in the next tick if it becomes empty
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        ItemStack item = inv.getItem(slot);
                        if (item == null || item.getType() == Material.AIR) {
                            inv.setItem(slot, getPlaceholderForSlot(slot));
                        }
                        updateRecipe(inv, player);
                        player.updateInventory();
                    });
                } else {
                    event.setCancelled(true);
                }
                return;
            }

            if (slotHasPlaceholder) {
                boolean isPlacing = cursorHasItem || (event.getClick().isKeyboardClick() && player.getInventory().getItem(event.getHotbarButton()) != null && player.getInventory().getItem(event.getHotbarButton()).getType() != Material.AIR);
                if (isPlacing) {
                    // Remove placeholder so it doesn't swap to cursor or go to player inventory
                    event.setCurrentItem(null);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        updateRecipe(inv, player);
                        player.updateInventory();
                    });
                } else {
                    // Player is clicking an empty placeholder slot with nothing in hand.
                    // We cancel the click so they don't pick up the placeholder.
                    event.setCancelled(true);
                }
            } else {
                // Slot has a real item.
                // We let vanilla handle the click (picking it up, swapping it, etc.)
                // In the next tick, if the slot is now empty, we restore the placeholder.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    ItemStack item = inv.getItem(slot);
                    if (item == null || item.getType() == Material.AIR) {
                        inv.setItem(slot, getPlaceholderForSlot(slot));
                    }
                    updateRecipe(inv, player);
                    player.updateInventory();
                });
            }
            return;
        }

        if (isResultSlot) {
            event.setCancelled(true); // Always cancel original event to prevent double-processing/ghosts
            SmithingState state = activeSessions.get(player.getUniqueId());
            ItemStack resultItem = inv.getItem(16);

            if (resultItem == null || resultItem.getType() == Material.AIR || isPlaceholder(resultItem) || state == null) {
                return;
            }

            // Handle manual item transfer to player
            if (event.isShiftClick()) {
                java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(resultItem.clone());
                if (!leftover.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Your inventory is full!");
                    return;
                }
            } else {
                ItemStack cursor = player.getItemOnCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    return;
                }
                player.setItemOnCursor(resultItem.clone());
            }

            // Play sound and consume inputs
            try {
                Sound sound = Sound.valueOf("BLOCK_SMITHING_TABLE_USE");
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            }

            consumeInputAndRestorePlaceholder(inv, 10);
            consumeInputAndRestorePlaceholder(inv, 11);
            if (hasTemplate) {
                consumeInputAndRestorePlaceholder(inv, 12);
            }

            // Update inventory and recalculate recipe
            updateRecipe(inv, player);
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        boolean hasTemplate = VersionUtil.supportsSmithingTemplate();

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 27) {
                if (rawSlot != 10 && rawSlot != 11 && !(hasTemplate && rawSlot == 12)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            updateRecipe(event.getInventory(), player);
            player.updateInventory();
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        boolean hasTemplate = VersionUtil.supportsSmithingTemplate();

        ItemStack item1 = event.getInventory().getItem(10);
        ItemStack item2 = event.getInventory().getItem(11);
        ItemStack item3 = hasTemplate ? event.getInventory().getItem(12) : null;

        event.getInventory().setItem(10, null);
        event.getInventory().setItem(11, null);
        if (hasTemplate) {
            event.getInventory().setItem(12, null);
        }
        event.getInventory().setItem(14, null);
        event.getInventory().setItem(16, null);

        activeSessions.remove(player.getUniqueId());

        returnItemToPlayer(player, item1);
        returnItemToPlayer(player, item2);
        returnItemToPlayer(player, item3);
    }

    private void returnItemsAndCloseForVanillaSmithing(Player player, Inventory inv) {
        boolean hasTemplate = VersionUtil.supportsSmithingTemplate();

        ItemStack item1 = inv.getItem(10);
        ItemStack item2 = inv.getItem(11);
        ItemStack item3 = hasTemplate ? inv.getItem(12) : null;

        inv.setItem(10, null);
        inv.setItem(11, null);
        if (hasTemplate) {
            inv.setItem(12, null);
        }
        inv.setItem(14, null);
        inv.setItem(16, null);

        activeSessions.remove(player.getUniqueId());

        returnItemToPlayer(player, item1);
        returnItemToPlayer(player, item2);
        returnItemToPlayer(player, item3);

        player.closeInventory();
        player.setMetadata("curiospaper_bypass_smithing", new FixedMetadataValue(plugin, true));

        // Open vanilla smithing table GUI on next tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                openSmithingTableSafe(player);
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not open vanilla smithing GUI for " + player.getName() + ": " + t.getMessage());
            }
        });
    }

    private void updateRecipe(Inventory inv, Player player) {
        boolean hasTemplate = VersionUtil.supportsSmithingTemplate();

        ItemStack templateSlotItem = inv.getItem(10);
        ItemStack baseSlotItem = hasTemplate ? inv.getItem(11) : inv.getItem(10);
        ItemStack additionSlotItem = hasTemplate ? inv.getItem(12) : inv.getItem(11);

        ItemStack template = (hasTemplate && !isPlaceholder(templateSlotItem)) ? templateSlotItem : null;
        ItemStack base = !isPlaceholder(baseSlotItem) ? baseSlotItem : null;
        ItemStack addition = !isPlaceholder(additionSlotItem) ? additionSlotItem : null;

        SmithingState state = activeSessions.computeIfAbsent(player.getUniqueId(), k -> new SmithingState());

        if (base == null || base.getType() == Material.AIR || addition == null || addition.getType() == Material.AIR) {
            inv.setItem(14, createErrorIcon());
            inv.setItem(16, null);
            state.result = null;
            state.isCustomRecipe = false;
            state.customItemId = null;
            return;
        }

        // Search custom Curios smithing recipes
        if (plugin.getItemDataManager() != null) {
            for (ItemData itemData : plugin.getItemDataManager().getAllItems().values()) {
                for (RecipeData recipe : itemData.getRecipes()) {
                    if (recipe.getType() == RecipeData.RecipeType.SMITHING) {
                        if (matchesSmithingRecipe(recipe, template, base, addition)) {
                            ItemStack result = plugin.getCuriosPaperAPI().createItemStack(itemData.getItemId());
                            if (result != null) {
                                CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, base);
                                Bukkit.getPluginManager().callEvent(transferEvent);
                                if (!transferEvent.isCancelled()) {
                                    result = transferEvent.getResult();
                                    result = ensureItemTags(result);

                                    // Fire CuriosCraftEvent before setting the result item
                                    CuriosCraftEvent craftEvent = new CuriosCraftEvent(inv, itemData.getItemId(), result);
                                    Bukkit.getPluginManager().callEvent(craftEvent);
                                    if (craftEvent.isCancelled()) {
                                        result = null;
                                    } else {
                                        result = craftEvent.getResult();
                                    }

                                    if (result != null) {
                                        // Fire SmithingTransformEvent (Paper 1.20+)
                                        result = fireSmithingTransformEventSafe(player, inv, result);
                                    }

                                    state.result = result;
                                    state.isCustomRecipe = true;
                                    state.customItemId = itemData.getItemId();

                                    if (result != null) {
                                        inv.setItem(14, createArrowIcon());
                                        inv.setItem(16, result);
                                    } else {
                                        inv.setItem(14, createErrorIcon());
                                        inv.setItem(16, null);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        inv.setItem(14, createErrorIcon());
        inv.setItem(16, null);
        state.result = null;
        state.isCustomRecipe = false;
        state.customItemId = null;
    }

    private boolean matchesSmithingRecipe(RecipeData recipe, ItemStack template, ItemStack base, ItemStack addition) {
        if (recipe.getTemplateItem() != null && !recipe.getTemplateItem().isEmpty()) {
            if (!matchesReq(recipe.getTemplateItem(), template)) return false;
        }
        if (!matchesReq(recipe.getBaseItem(), base)) return false;
        if (!matchesReq(recipe.getAdditionItem(), addition)) return false;
        return true;
    }

    private boolean matchesReq(String req, ItemStack stack) {
        if (req == null || req.isEmpty()) return stack == null || stack.getType() == Material.AIR;
        if (stack == null || stack.getType() == Material.AIR) return false;

        ItemData customData = plugin.getItemDataManager().getItemData(req);
        if (customData != null) {
            return isCustomItem(stack, customData.getItemId());
        }
        try {
            Material mat = Material.valueOf(req.toUpperCase());
            return stack.getType() == mat;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isCustomItem(ItemStack item, String targetId) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey idKey = plugin.getCuriosPaperAPI().getItemIdKey();
        if (!pdc.has(idKey, PersistentDataType.STRING)) return false;
        String id = pdc.get(idKey, PersistentDataType.STRING);
        return id != null && id.equals(targetId);
    }

    private void consumeInputAndRestorePlaceholder(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && item.getType() != Material.AIR && !isPlaceholder(item)) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                inv.setItem(slot, item);
            } else {
                inv.setItem(slot, getPlaceholderForSlot(slot));
            }
        }
    }

    private void returnItemToPlayer(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR && !isPlaceholder(item)) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private ItemStack getPlaceholderForSlot(int slot) {
        boolean hasTemplate = VersionUtil.supportsSmithingTemplate();
        if (slot == 10) {
            if (hasTemplate) {
                return createModelPlaceholder(10030, ChatColor.YELLOW + "Smithing Template Slot", ChatColor.GRAY + "Place Smithing Template here");
            } else {
                return createModelPlaceholder(10031, ChatColor.YELLOW + "Base Item Slot", ChatColor.GRAY + "Place Equipment / Tool here");
            }
        } else if (slot == 11) {
            if (hasTemplate) {
                return createModelPlaceholder(10031, ChatColor.YELLOW + "Base Item Slot", ChatColor.GRAY + "Place Equipment / Tool here");
            } else {
                return createModelPlaceholder(10032, ChatColor.YELLOW + "Addition Slot", ChatColor.GRAY + "Place Ingredient here");
            }
        } else if (slot == 12) {
            if (hasTemplate) {
                return createModelPlaceholder(10032, ChatColor.YELLOW + "Addition Slot", ChatColor.GRAY + "Place Ingredient here");
            } else {
                return createItem(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "Not used in this version");
            }
        }
        return null;
    }

    private ItemStack createModelPlaceholder(int customModelData, String name, String... lore) {
        ItemStack stack = createItem(Material.PAPER, name, lore);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            meta.getPersistentDataContainer().set(placeholderKey, PersistentDataType.BYTE, (byte) 1);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack createErrorIcon() {
        return createIconItem(10034, ChatColor.RED + "No Recipe", ChatColor.GRAY + "Place valid items to craft");
    }

    private ItemStack createArrowIcon() {
        return createIconItem(10033, ChatColor.GREEN + "Crafting Ready", ChatColor.GRAY + "Click the result item to craft");
    }

    private ItemStack createIconItem(int customModelData, String name, String... lore) {
        ItemStack stack = createItem(Material.PAPER, name, lore);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean isPlaceholder(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(placeholderKey, PersistentDataType.BYTE);
    }

    private boolean isTemplateItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String name = item.getType().name();
        return name.contains("TEMPLATE") || name.contains("TRIM");
    }

    private boolean isOreItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String name = item.getType().name();
        return name.endsWith("_INGOT") || name.endsWith("_ORE") || name.endsWith("_RAW") 
                || name.equals("DIAMOND") || name.equals("EMERALD") || name.equals("COAL") 
                || name.equals("REDSTONE") || name.equals("LAPIS_LAZULI") || name.equals("QUARTZ") 
                || name.equals("AMETHYST_SHARD") || name.equals("NETHERITE_SCRAP");
    }

    private ItemStack ensureItemTags(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        NamespacedKey itemIdKey = plugin.getCuriosPaperAPI().getItemIdKey();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(itemIdKey, PersistentDataType.STRING)) {
            String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);
            ItemData itemData = plugin.getItemDataManager().getItemData(itemId);
            if (itemData != null && itemData.getSlotType() != null) {
                return plugin.getCuriosPaperAPI().tagAccessoryItem(item, itemData.getSlotType());
            }
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                java.util.List<String> loreList = new java.util.ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void openSmithingTableSafe(Player player) {
        try {
            java.lang.reflect.Method method = player.getClass().getMethod("openSmithingTable", org.bukkit.Location.class, boolean.class);
            method.invoke(player, player.getLocation(), true);
        } catch (Exception e) {
            try {
                org.bukkit.event.inventory.InventoryType type = org.bukkit.event.inventory.InventoryType.valueOf("SMITHING");
                player.openInventory(Bukkit.createInventory(player, type, ChatColor.DARK_GRAY + "Upgrade Gear"));
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not open vanilla smithing GUI: " + t.getMessage());
            }
        }
    }


    private ItemStack fireSmithingTransformEventSafe(Player player, Inventory inv, ItemStack resultItem) {
        if (!VersionUtil.supportsSmithingTemplate()) {
            return resultItem;
        }
        try {
            Class<?> eventClass = Class.forName("org.bukkit.event.inventory.SmithingTransformEvent");
            java.lang.reflect.Constructor<?> ctor = eventClass.getConstructor(
                org.bukkit.inventory.InventoryView.class,
                ItemStack.class,
                ItemStack.class,
                ItemStack.class,
                ItemStack.class
            );
            
            ItemStack template = inv.getItem(10);
            ItemStack base = inv.getItem(11);
            ItemStack addition = inv.getItem(12);
            
            if (isPlaceholder(template)) template = null;
            if (isPlaceholder(base)) base = null;
            if (isPlaceholder(addition)) addition = null;

            Object eventInstance = ctor.newInstance(
                player.getOpenInventory(),
                template,
                base,
                addition,
                resultItem
            );
            
            Bukkit.getPluginManager().callEvent((org.bukkit.event.Event) eventInstance);
            
            java.lang.reflect.Method isCancelledMethod = eventClass.getMethod("isCancelled");
            boolean cancelled = (boolean) isCancelledMethod.invoke(eventInstance);
            if (cancelled) {
                return null; // Signals cancellation
            }
            
            java.lang.reflect.Method getResultMethod = eventClass.getMethod("getResult");
            return (ItemStack) getResultMethod.invoke(eventInstance);
        } catch (Exception e) {
            // ClassNotFound or method exception, ignore and return resultItem
            return resultItem;
        }
    }
}
