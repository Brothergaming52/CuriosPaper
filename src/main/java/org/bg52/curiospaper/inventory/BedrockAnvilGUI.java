package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.event.CuriosCraftEvent;
import org.bg52.curiospaper.event.CuriosRecipeTransferEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom 3-row Chest GUI for Bedrock players when using Anvils.
 * Symmetrical Layout:
 * - Slot 10: Input 1 (Base item)
 * - Slot 11: Plus Icon [10015]
 * - Slot 12: Input 2 (Sacrifice item)
 * - Slot 14: Recipe Status Icon (Error 10014 / Arrow 10013)
 * - Slot 16: Result item
 * - Slot 25: XP Cost Indicator (Green = Affordable, Red = Expensive)
 * - Slot 8:  Rename Button (Opens vanilla Anvil GUI for renaming)
 */
public class BedrockAnvilGUI implements Listener, InventoryHolder {

    private final CuriosPaper plugin;
    private final String title = ChatColor.DARK_GRAY + "Anvil";
    private final Map<UUID, AnvilState> activeSessions = new HashMap<>();
    private static final ThreadLocal<Boolean> firingMockEvent = ThreadLocal.withInitial(() -> false);

    public BedrockAnvilGUI(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    private static class AnvilState {
        int cost = 0;
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

        // Clear interactive input & output slots (Slots 10, 12, 16)
        inv.setItem(10, null);
        inv.setItem(12, null);
        inv.setItem(16, null);

        // Plus icon between slot 10 and slot 12 (Slot 11)
        inv.setItem(11, createIconItem(10035, ChatColor.YELLOW + "+"));

        // Status arrow/error icon in slot 14
        inv.setItem(14, createErrorIcon());

        // Rename button in top right corner (Slot 8)
        ItemStack renameBtn = createItem(Material.ANVIL, ChatColor.YELLOW + "Rename Item",
                ChatColor.GRAY + "Click to open vanilla Anvil",
                ChatColor.GRAY + "interface for item renaming.");
        inv.setItem(8, renameBtn);

        // Update cost indicator in slot 25
        updateCostIndicator(inv, player, 0, false, null);

        activeSessions.put(player.getUniqueId(), new AnvilState());
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

        // Player Inventory Interactivity (slot >= 27)
        if (slot >= 27) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                    return;
                }

                int[] targetSlots = new int[]{10, 12};
                for (int tSlot : targetSlots) {
                    ItemStack slotItem = inv.getItem(tSlot);
                    if (slotItem == null || slotItem.getType() == Material.AIR) {
                        inv.setItem(tSlot, clickedItem);
                        event.setCurrentItem(null);
                        player.updateInventory();
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            updateRecipe(inv, player);
                            player.updateInventory();
                        });
                        return;
                    }
                }
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    updateRecipe(inv, player);
                    player.updateInventory();
                });
            }
            return;
        }

        // Top right slot: Open real Anvil GUI for renaming
        if (slot == 8) {
            event.setCancelled(true);
            returnItemsAndCloseForRename(player, inv);
            return;
        }

        boolean isInputSlot = (slot == 10 || slot == 12);
        boolean isResultSlot = (slot == 16);

        if (!isInputSlot && !isResultSlot) {
            event.setCancelled(true);
            return;
        }

        if (isInputSlot) {
            event.setCancelled(false); // Uncancel for Geyser compatibility
            ItemStack currentSlotItem = inv.getItem(slot);
            if (event.isShiftClick()) {
                if (currentSlotItem != null && currentSlotItem.getType() != Material.AIR) {
                    // Let vanilla handle shift-clicking back to player inventory, but update recipe on next tick
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        updateRecipe(inv, player);
                        player.updateInventory();
                    });
                } else {
                    event.setCancelled(true);
                }
                return;
            }
            // Let vanilla handle click (placing, taking, swapping) and update recipe on next tick
            Bukkit.getScheduler().runTask(plugin, () -> {
                updateRecipe(inv, player);
                player.updateInventory();
            });
            return;
        }

        if (isResultSlot) {
            event.setCancelled(true); // Always cancel original event to prevent double-processing/ghosts
            AnvilState state = activeSessions.get(player.getUniqueId());
            ItemStack resultItem = inv.getItem(16);

            if (resultItem == null || resultItem.getType() == Material.AIR || state == null) {
                return;
            }

            int cost = state.cost;
            if (player.getGameMode() != GameMode.CREATIVE && player.getLevel() < cost) {
                player.sendMessage(ChatColor.RED + "You do not have enough experience levels! Required: " + cost);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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

            // Deduct cost and consume inputs
            if (player.getGameMode() != GameMode.CREATIVE && cost > 0) {
                player.setLevel(player.getLevel() - cost);
            }

            consumeInput(inv, 10);
            consumeInput(inv, 12);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
            
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

        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < 27 && rawSlot != 10 && rawSlot != 12) {
                event.setCancelled(true);
                return;
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

        // Return leftover items in input slots (10 & 12)
        ItemStack item1 = event.getInventory().getItem(10);
        ItemStack item2 = event.getInventory().getItem(12);

        event.getInventory().setItem(10, null);
        event.getInventory().setItem(12, null);
        event.getInventory().setItem(14, null);
        event.getInventory().setItem(16, null);

        activeSessions.remove(player.getUniqueId());

        returnItemToPlayer(player, item1);
        returnItemToPlayer(player, item2);
    }

    private void updateRecipe(Inventory inv, Player player) {
        ItemStack left = inv.getItem(10);
        ItemStack right = inv.getItem(12);

        AnvilState state = activeSessions.computeIfAbsent(player.getUniqueId(), k -> new AnvilState());

        if (left == null || left.getType() == Material.AIR) {
            inv.setItem(14, createErrorIcon());
            inv.setItem(16, null);
            state.cost = 0;
            state.result = null;
            state.isCustomRecipe = false;
            state.customItemId = null;
            updateCostIndicator(inv, player, 0, false, null);
            return;
        }

        // 1. Check Curios Custom Anvil Recipes
        if (plugin.getItemDataManager() != null) {
            for (ItemData itemData : plugin.getItemDataManager().getAllItems().values()) {
                for (RecipeData recipe : itemData.getRecipes()) {
                    if (recipe.getType() == RecipeData.RecipeType.ANVIL) {
                        if (matchesAnvilRecipe(recipe, left, right)) {
                            ItemStack result = plugin.getCuriosPaperAPI().createItemStack(itemData.getItemId());
                            if (result != null) {
                                CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, left);
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
                                        int cost = (int) recipe.getExperience();
                                        state.cost = cost;
                                        state.result = result;
                                        state.isCustomRecipe = true;
                                        state.customItemId = itemData.getItemId();

                                        inv.setItem(14, createArrowIcon());
                                        inv.setItem(16, result);
                                        updateCostIndicator(inv, player, cost, true, result);
                                    } else {
                                        inv.setItem(14, createErrorIcon());
                                        inv.setItem(16, null);
                                        state.cost = 0;
                                        state.result = null;
                                        state.isCustomRecipe = false;
                                        state.customItemId = null;
                                        updateCostIndicator(inv, player, 0, false, null);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Fallback to Vanilla Anvil combination logic
        AnvilResult vanillaResult = calculateVanillaAnvil(left, right);
        if (vanillaResult != null && vanillaResult.result != null) {
            ItemStack result = vanillaResult.result;

            // Fire CuriosCraftEvent for vanilla combination too!
            String craftItemId = result.getType().name();
            CuriosCraftEvent craftEvent = new CuriosCraftEvent(inv, craftItemId, result);
            Bukkit.getPluginManager().callEvent(craftEvent);
            if (craftEvent.isCancelled()) {
                result = null;
            } else {
                result = craftEvent.getResult();
            }

            if (result != null) {
                state.cost = vanillaResult.cost;
                state.result = result;
                state.isCustomRecipe = false;
                state.customItemId = null;

                inv.setItem(14, createArrowIcon());
                inv.setItem(16, result);
                updateCostIndicator(inv, player, vanillaResult.cost, true, result);
            } else {
                inv.setItem(14, createErrorIcon());
                inv.setItem(16, null);
                state.cost = 0;
                state.result = null;
                state.isCustomRecipe = false;
                state.customItemId = null;
                updateCostIndicator(inv, player, 0, false, null);
            }
        } else {
            inv.setItem(14, createErrorIcon());
            inv.setItem(16, null);
            state.cost = 0;
            state.result = null;
            state.isCustomRecipe = false;
            state.customItemId = null;
            updateCostIndicator(inv, player, 0, false, null);
        }
    }

    private void updateCostIndicator(Inventory inv, Player player, int cost, boolean validRecipe, ItemStack result) {
        if (!validRecipe || result == null) {
            ItemStack indicator = createItem(Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Invalid Combination",
                    ChatColor.GRAY + "Place compatible items in the slots above.");
            inv.setItem(25, indicator);
            return;
        }

        if (cost <= 0) {
            cost = 1; // Minimum 1 level
        }

        boolean canAfford = player.getGameMode() == GameMode.CREATIVE || player.getLevel() >= cost;

        if (canAfford) {
            ItemStack indicator = createItem(Material.LIME_STAINED_GLASS_PANE,
                    ChatColor.GREEN + "Enchantment Cost: " + cost + " Levels",
                    ChatColor.GRAY + "Your Level: " + ChatColor.YELLOW + player.getLevel(),
                    ChatColor.GREEN + "Click the result item to combine!");
            inv.setItem(25, indicator);
        } else {
            ItemStack indicator = createItem(Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Too Expensive! Cost: " + cost + " Levels",
                    ChatColor.GRAY + "Your Level: " + ChatColor.RED + player.getLevel(),
                    ChatColor.RED + "You need " + (cost - player.getLevel()) + " more levels.");
            inv.setItem(25, indicator);
        }
    }

    private void returnItemsAndCloseForRename(Player player, Inventory inv) {
        ItemStack item1 = inv.getItem(10);
        ItemStack item2 = inv.getItem(12);

        inv.setItem(10, null);
        inv.setItem(12, null);
        inv.setItem(14, null);
        inv.setItem(16, null);

        activeSessions.remove(player.getUniqueId());

        returnItemToPlayer(player, item1);
        returnItemToPlayer(player, item2);

        player.closeInventory();
        player.setMetadata("curiospaper_bypass_anvil", new FixedMetadataValue(plugin, true));

        // Open vanilla anvil GUI on next tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                openAnvilSafe(player);
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not open vanilla anvil GUI for " + player.getName() + ": " + t.getMessage());
            }
        });
    }

    private boolean matchesAnvilRecipe(RecipeData recipe, ItemStack left, ItemStack right) {
        if (left == null) return false;
        return matchesReq(recipe.getLeftInput(), left) && matchesReq(recipe.getRightInput(), right);
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

    private static class AnvilResult {
        ItemStack result;
        int cost;

        AnvilResult(ItemStack result, int cost) {
            this.result = result;
            this.cost = cost;
        }
    }

    private AnvilResult calculateVanillaAnvil(ItemStack left, ItemStack right) {
        if (left == null || left.getType() == Material.AIR) return null;

        ItemStack result = left.clone();
        int baseCost = 0;
        int enchantmentCost = 0;

        ItemMeta meta = result.getItemMeta();

        // Case 1: Repairing with identical item or material
        if (right != null && right.getType() != Material.AIR) {
            if (meta instanceof Damageable) {
                Damageable damageable = (Damageable) meta;
                if (damageable.hasDamage() && right.getType() == left.getType()) {
                    int maxDurability = left.getType().getMaxDurability();
                    Damageable rightDamageable = (Damageable) (right.hasItemMeta() ? right.getItemMeta() : null);
                    int rightDamage = rightDamageable != null ? rightDamageable.getDamage() : 0;
                    int repairDurability = (maxDurability - rightDamage) + (int) (maxDurability * 0.12);
                    int newDamage = Math.max(0, damageable.getDamage() - repairDurability);
                    damageable.setDamage(newDamage);
                    baseCost += 2;
                }
            }

            // Case 2: Combining enchantments from right item or enchanted book
            Map<Enchantment, Integer> targetEnchants = new HashMap<>(result.getEnchantments());
            Map<Enchantment, Integer> sourceEnchants = new HashMap<>();

            if (right.getType() == Material.ENCHANTED_BOOK && right.hasItemMeta()) {
                EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) right.getItemMeta();
                if (bookMeta != null) {
                    sourceEnchants = bookMeta.getStoredEnchants();
                }
            } else {
                sourceEnchants = right.getEnchantments();
            }

            boolean enchantmentAdded = false;
            for (Map.Entry<Enchantment, Integer> entry : sourceEnchants.entrySet()) {
                Enchantment ench = entry.getKey();
                int sourceLevel = entry.getValue();

                // Check if enchantment applies to result item
                if (right.getType() == Material.ENCHANTED_BOOK || ench.canEnchantItem(result)) {
                    int currentLevel = targetEnchants.getOrDefault(ench, 0);
                    int newLevel;

                    if (currentLevel == sourceLevel) {
                        newLevel = Math.min(sourceLevel + 1, ench.getMaxLevel());
                    } else {
                        newLevel = Math.max(currentLevel, sourceLevel);
                    }

                    if (newLevel > currentLevel) {
                        meta.addEnchant(ench, newLevel, true);
                        enchantmentCost += newLevel * 2;
                        enchantmentAdded = true;
                    }
                }
            }

            if (!enchantmentAdded && baseCost == 0) {
                return null;
            }
        } else {
            return null;
        }

        result.setItemMeta(meta);
        return new AnvilResult(result, Math.max(1, baseCost + enchantmentCost));
    }

    private void consumeInput(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
                inv.setItem(slot, item);
            } else {
                inv.setItem(slot, null);
            }
        }
    }

    private void returnItemToPlayer(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private ItemStack createErrorIcon() {
        return createIconItem(10034, ChatColor.RED + "Invalid Combination", ChatColor.GRAY + "Place compatible items in slots 10 & 12.");
    }

    private ItemStack createArrowIcon() {
        return createIconItem(10033, ChatColor.GREEN + "Combination Ready", ChatColor.GRAY + "Click the result item to combine!");
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

    private void openAnvilSafe(Player player) {
        try {
            java.lang.reflect.Method method = player.getClass().getMethod("openAnvil", org.bukkit.Location.class, boolean.class);
            method.invoke(player, player.getLocation(), true);
        } catch (Exception e) {
            player.openInventory(Bukkit.createInventory(player, InventoryType.ANVIL, ChatColor.DARK_GRAY + "Repair & Name"));
        }
    }
}
