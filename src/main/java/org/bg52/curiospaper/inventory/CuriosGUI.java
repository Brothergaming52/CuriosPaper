package org.bg52.curiospaper.inventory;

import net.kyori.adventure.text.Component;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.event.AccessoryEquipEvent;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bg52.curiospaper.util.BedrockUtil;
import org.bg52.curiospaper.util.TextUtil;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Controller and event listener managing the two-tier Curios accessory inventory GUI:
 * - Tier 1: Main Overview GUI with designed slot category buttons (only appearing when an item is registered)
 * - Tier 2: Dedicated Slot Items GUI with centered sub-slots, locked slots, and Back button
 */
public class CuriosGUI implements Listener {

    private final CuriosPaper plugin;
    private final NamespacedKey slotTypeKey;
    private final NamespacedKey backButtonKey;

    public CuriosGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.slotTypeKey = new NamespacedKey(plugin, "gui_slot_type");
        this.backButtonKey = new NamespacedKey(plugin, "gui_back_button");
    }

    // =========================================================================
    // Tier-1 Main Overview GUI
    // =========================================================================

    /**
     * Opens the Tier-1 Main Overview GUI showing only slot types that have at least one item
     * registered (or that the player currently has equipped).
     */
    public void open(Player player) {
        if (plugin.getElytraBackSlotHandler() != null) {
            plugin.getElytraBackSlotHandler().retagAllPlayerElytras(player);
        }

        plugin.getSlotRegistry().refreshActiveSlots();
        List<String> activeKeys = plugin.getSlotRegistry().getActiveSlotKeysFor(player);
        int activeCount = activeKeys.size();
        int size = computeOverviewSize(activeCount);

        CuriosOverviewHolder holder = new CuriosOverviewHolder(player);
        Component title = plugin.getLanguageManager().getRawComponent("gui.main-title");
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        // Fill background with decorative border & filler
        createBorderAndFill(inv, size);

        // Compute aesthetic layout positions
        int[] positions = getPresetPositions(activeCount, size);
        PlayerCuriosInventory curiosInv = plugin.getStorageManager().getInventory(player.getUniqueId());

        for (int i = 0; i < activeCount && i < positions.length; i++) {
            String slotId = activeKeys.get(i);
            SlotDefinition def = plugin.getSlotRegistry().getSlot(slotId);
            if (def == null) continue;

            int slotPos = positions[i];
            holder.mapSlotButton(slotPos, def.getId());

            int equipped = curiosInv.countEquipped(def.getId());
            int unlocked = curiosInv.getUnlockedSlots(def, player);

            ItemStack button = createSlotButton(player, def, equipped, unlocked);
            inv.setItem(slotPos, button);
        }

        player.openInventory(inv);
        playOpenSound(player);
    }

    // =========================================================================
    // Tier-2 Slot Items GUI
    // =========================================================================

    /**
     * Opens the Tier-2 Slot Items GUI displaying the player's slots for a specific slot type.
     */
    public void openSlotGUI(Player player, SlotDefinition def) {
        if (def == null) return;
        if (plugin.getElytraBackSlotHandler() != null && "back".equalsIgnoreCase(def.getId())) {
            plugin.getElytraBackSlotHandler().retagAllPlayerElytras(player);
        }

        int maxSlots = def.getMaxSlots();
        int size = calculateSlotGUISize(maxSlots);
        int[] slotPositions = calculateSlotPositions(maxSlots, size);

        CuriosSlotHolder holder = new CuriosSlotHolder(player, def);
        Component title = plugin.getLanguageManager().getRawComponent("gui.slot-title-prefix")
                .append(Component.text(" " + def.getName()));
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        createBorderAndFill(inv, size);

        // Back button placed at bottom-left (slot size - 9)
        inv.setItem(size - 9, createBackButton(player));

        // Close button placed at bottom-right (slot size - 1)
        inv.setItem(size - 1, createCloseButton());

        PlayerCuriosInventory curiosInv = plugin.getStorageManager().getInventory(player.getUniqueId());
        int unlocked = curiosInv.getUnlockedSlots(def, player);

        for (int subIndex = 0; subIndex < maxSlots && subIndex < slotPositions.length; subIndex++) {
            int targetSlot = slotPositions[subIndex];
            holder.mapSlot(targetSlot, subIndex);

            if (subIndex < unlocked) {
                ItemStack equipped = curiosInv.getItem(def.getId(), subIndex);
                if (equipped != null && equipped.getType() != Material.AIR) {
                    inv.setItem(targetSlot, equipped.clone());
                } else {
                    inv.setItem(targetSlot, def.createPlaceholderItem(player));
                }
            } else {
                inv.setItem(targetSlot, createLockedItem(player, def, subIndex + 1));
            }
        }

        player.openInventory(inv);
        playOpenSound(player);
    }

    // =========================================================================
    // Event Handlers
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Inventory top = event.getView().getTopInventory();
        if (top == null) return;

        // ---------------------------------------------------------------------
        // Case A: Clicking inside Tier-1 Main Overview GUI
        // ---------------------------------------------------------------------
        if (top.getHolder() instanceof CuriosOverviewHolder) {
            CuriosOverviewHolder holder = (CuriosOverviewHolder) top.getHolder();
            int rawSlot = event.getRawSlot();

            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                event.setCancelled(true);
                String slotType = holder.getSlotType(rawSlot);
                if (slotType != null) {
                    SlotDefinition def = plugin.getSlotRegistry().getSlot(slotType);
                    if (def != null) {
                        playClickSound(player);
                        openSlotGUI(player, def);
                    }
                }
            } else if (event.isShiftClick()) {
                // Prevent shift-clicking into decorative overview
                event.setCancelled(true);
            }
            return;
        }

        // ---------------------------------------------------------------------
        // Case B: Clicking inside Tier-2 Slot Items GUI
        // ---------------------------------------------------------------------
        if (top.getHolder() instanceof CuriosSlotHolder) {
            CuriosSlotHolder holder = (CuriosSlotHolder) top.getHolder();
            int rawSlot = event.getRawSlot();
            SlotDefinition def = holder.getSlotDef();
            PlayerCuriosInventory curiosInv = plugin.getStorageManager().getInventory(player.getUniqueId());

            // 1. Clicked in the Curios top inventory
            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                event.setCancelled(true);

                // Check Back Button
                if (rawSlot == top.getSize() - 9) {
                    playClickSound(player);
                    open(player);
                    return;
                }

                // Check Close Button
                if (rawSlot == top.getSize() - 1) {
                    player.closeInventory();
                    return;
                }

                Integer subIndex = holder.getSubIndex(rawSlot);
                if (subIndex == null) {
                    return; // decorative or border slot
                }

                int unlocked = curiosInv.getUnlockedSlots(def, player);
                if (subIndex >= unlocked) {
                    plugin.getLanguageManager().sendMessage(player, "inventory.slot-locked", "slot", def.getName());
                    return;
                }

                ItemStack cursor = event.getCursor();
                boolean cursorEmpty = (cursor == null || cursor.getType().isAir());
                ItemStack currentlyEquipped = curiosInv.getItem(def.getId(), subIndex);

                if (cursorEmpty) {
                    // UNEQUIP item to cursor
                    if (currentlyEquipped != null && !currentlyEquipped.getType().isAir()) {
                        curiosInv.setItem(def.getId(), subIndex, null);

                        AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                                player, def.getId(), subIndex, currentlyEquipped, null, AccessoryEquipEvent.Action.UNEQUIP
                        );
                        Bukkit.getPluginManager().callEvent(equipEvent);
                        if (equipEvent.isCancelled()) {
                            curiosInv.setItem(def.getId(), subIndex, currentlyEquipped);
                            return;
                        }

                        top.setItem(rawSlot, def.createPlaceholderItem(player));

                        if (event.isShiftClick()) {
                            player.setItemOnCursor(null);
                            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(currentlyEquipped);
                            if (!leftover.isEmpty()) {
                                for (ItemStack drop : leftover.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                                }
                            }
                            player.updateInventory();
                        } else {
                            player.setItemOnCursor(currentlyEquipped);
                        }

                        playUnequipSound(player);
                        plugin.getItemEngineManager().notifyUnequip(player, currentlyEquipped);
                        plugin.getPacketModelManager().updatePlayerAccessories(player);
                        plugin.getStorageManager().savePlayer(player.getUniqueId());
                        plugin.getLanguageManager().sendMessage(player, "inventory.item-unequipped",
                                "item", TextUtil.getItemDisplayName(currentlyEquipped), "slot", def.getName());
                    }
                } else {
                    // EQUIP or SWAP item from cursor
                    String targetSlot = plugin.getItemEngineManager().resolveCuriosSlot(cursor);
                    if (targetSlot == null || !targetSlot.equalsIgnoreCase(def.getId())) {
                        plugin.getLanguageManager().sendMessage(player, "inventory.invalid-item");
                        return;
                    }

                    ItemStack toEquip = cursor.clone();
                    toEquip.setAmount(1);

                    curiosInv.setItem(def.getId(), subIndex, toEquip);

                    AccessoryEquipEvent.Action action = (currentlyEquipped != null && !currentlyEquipped.getType().isAir())
                            ? AccessoryEquipEvent.Action.SWAP : AccessoryEquipEvent.Action.EQUIP;

                    AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                            player, def.getId(), subIndex, currentlyEquipped, toEquip, action
                    );
                    Bukkit.getPluginManager().callEvent(equipEvent);
                    if (equipEvent.isCancelled()) {
                        curiosInv.setItem(def.getId(), subIndex, currentlyEquipped);
                        return;
                    }

                    // Deduct from cursor
                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                        player.setItemOnCursor(cursor);
                    } else {
                        player.setItemOnCursor(currentlyEquipped);
                    }

                    top.setItem(rawSlot, toEquip.clone());
                    playEquipSound(player);
                    plugin.getItemEngineManager().notifyEquip(player, toEquip);
                    plugin.getPacketModelManager().updatePlayerAccessories(player);
                    plugin.getStorageManager().savePlayer(player.getUniqueId());
                    plugin.getLanguageManager().sendMessage(player, "inventory.item-equipped",
                            "item", TextUtil.getItemDisplayName(toEquip), "slot", def.getName());
                }
            } else {
                // 2. Clicked in player's inventory
                if (event.isShiftClick()) {
                    ItemStack clicked = event.getCurrentItem();
                    if (clicked == null || clicked.getType().isAir()) return;

                    String targetSlot = plugin.getItemEngineManager().resolveCuriosSlot(clicked);
                    if (targetSlot != null && targetSlot.equalsIgnoreCase(def.getId())) {
                        int freeSub = curiosInv.getFirstEmptySlot(def, player);
                        if (freeSub != -1) {
                            event.setCancelled(true);
                            ItemStack toEquip = clicked.clone();
                            toEquip.setAmount(1);

                            curiosInv.setItem(def.getId(), freeSub, toEquip);

                            AccessoryEquipEvent equipEvent = new AccessoryEquipEvent(
                                    player, def.getId(), freeSub, null, toEquip, AccessoryEquipEvent.Action.EQUIP
                            );
                            Bukkit.getPluginManager().callEvent(equipEvent);
                            if (equipEvent.isCancelled()) {
                                curiosInv.setItem(def.getId(), freeSub, null);
                                return;
                            }

                            if (clicked.getAmount() > 1) {
                                clicked.setAmount(clicked.getAmount() - 1);
                            } else {
                                event.setCurrentItem(null);
                            }
                            player.updateInventory();

                            // Find and update GUI slot
                            for (int s = 0; s < top.getSize(); s++) {
                                Integer sIdx = holder.getSubIndex(s);
                                if (sIdx != null && sIdx == freeSub) {
                                    top.setItem(s, toEquip.clone());
                                    break;
                                }
                            }

                            playEquipSound(player);
                            plugin.getItemEngineManager().notifyEquip(player, toEquip);
                            plugin.getPacketModelManager().updatePlayerAccessories(player);
                            plugin.getStorageManager().savePlayer(player.getUniqueId());
                            plugin.getLanguageManager().sendMessage(player, "inventory.item-equipped",
                                    "item", TextUtil.getItemDisplayName(toEquip), "slot", def.getName());
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && (top.getHolder() instanceof CuriosOverviewHolder || top.getHolder() instanceof CuriosSlotHolder)) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < top.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && (top.getHolder() instanceof CuriosOverviewHolder || top.getHolder() instanceof CuriosSlotHolder)) {
            Player player = (Player) event.getPlayer();
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                player.setItemOnCursor(null);
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(cursor);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            plugin.getStorageManager().savePlayer(player.getUniqueId());
        }
    }

    // =========================================================================
    // GUI Construction Helpers
    // =========================================================================

    public int computeOverviewSize(int activeCount) {
        if (activeCount <= 5) return 27;
        if (activeCount <= 8) return 36;
        if (activeCount <= 10) return 45;
        return 54;
    }

    public int[] getPresetPositions(int count, int size) {
        switch (count) {
            case 0:
                return new int[0];
            case 1:
                return new int[]{13};
            case 2:
                return new int[]{12, 14};
            case 3:
                return new int[]{11, 13, 15};
            case 4:
                return new int[]{10, 12, 14, 16};
            case 5:
                return new int[]{11, 12, 13, 14, 15};
            case 6:
                return new int[]{11, 13, 15, 20, 22, 24};
            case 7:
                return new int[]{10, 12, 14, 16, 20, 22, 24};
            case 8:
                return new int[]{10, 12, 14, 16, 19, 21, 23, 25};
            case 9:
                return new int[]{10, 12, 14, 16, 20, 22, 24, 28, 30, 32};
            default:
                return generateGridPositions(count, size);
        }
    }

    public int calculateSlotGUISize(int slotAmount) {
        if (slotAmount <= 5) return 27;
        if (slotAmount <= 16) return 45;
        return 54;
    }

    public int[] calculateSlotPositions(int slotAmount, int inventorySize) {
        if (slotAmount == 1) return new int[]{13};
        if (slotAmount == 2) return new int[]{12, 14};
        if (slotAmount == 3) return new int[]{11, 13, 15};
        if (slotAmount == 4) return new int[]{10, 12, 14, 16};
        if (slotAmount == 5) return new int[]{11, 12, 13, 14, 15};
        if (slotAmount == 6) return new int[]{11, 13, 15, 29, 31, 33};
        if (slotAmount == 7) return new int[]{12, 14, 20, 22, 24, 30, 32};
        if (slotAmount == 8) return new int[]{11, 13, 15, 21, 23, 29, 31, 33};
        return generateGridPositions(slotAmount, inventorySize);
    }

    private int[] generateGridPositions(int count, int size) {
        int rows = size / 9;
        int itemsPerRow = Math.min(7, count);
        int neededRows = (count + itemsPerRow - 1) / itemsPerRow;
        int startRow = Math.max(1, (rows - neededRows) / 2);

        List<Integer> positions = new ArrayList<>();
        for (int r = 0; r < neededRows && positions.size() < count; r++) {
            int inRow = Math.min(itemsPerRow, count - positions.size());
            int startCol = (9 - inRow) / 2;
            for (int c = 0; c < inRow; c++) {
                positions.add((startRow + r) * 9 + startCol + c);
            }
        }
        return positions.stream().mapToInt(Integer::intValue).toArray();
    }

    private void createBorderAndFill(Inventory inv, int size) {
        ItemStack border = createBorderItem(Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = createBorderItem(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // Perimeter
        for (int c = 0; c < 9; c++) {
            inv.setItem(c, border);
            inv.setItem(size - 9 + c, border);
        }
        int rows = size / 9;
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }
    }

    private ItemStack createSlotButton(SlotDefinition def, int equipped, int total) {
        return createSlotButton(null, def, equipped, total);
    }

    private ItemStack createSlotButton(Player player, SlotDefinition def, int equipped, int total) {
        boolean isBedrock = player != null && BedrockUtil.isBedrockPlayer(player);
        boolean useVanilla = plugin.getConfig().getBoolean("features.use-vanilla-slot-icons", false);
        Material mat = def.getIcon();
        if (isBedrock || (!useVanilla && (def.getItemModel() != null || def.getCustomModelData() != null))) {
            mat = Material.PAPER;
        }

        ItemStack button = new ItemStack(mat);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse("<gold><bold>" + def.getName() + "</bold></gold>"));
            List<Component> lore = new ArrayList<>();
            for (String line : def.getDescription()) {
                lore.add(TextUtil.parse(line));
            }
            lore.add(Component.empty());
            lore.add(plugin.getLanguageManager().getRawComponent("gui.slot-button-slots", "amount", String.valueOf(def.getMaxSlots())));
            lore.add(TextUtil.parse("<gray>Equipped:</gray> <white>" + equipped + "/" + total + "</white>"));
            lore.add(Component.empty());
            lore.add(plugin.getLanguageManager().getRawComponent("gui.slot-button-click"));
            meta.lore(lore);

            if (def.getItemModel() != null || def.getCustomModelData() != null) {
                VersionUtil.setItemModelSafe(meta, def.getItemModel(), def.getCustomModelData());
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(slotTypeKey, PersistentDataType.STRING, def.getId());
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createBackButton() {
        return createBackButton(null);
    }

    private ItemStack createBackButton(Player player) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLanguageManager().getRawComponent("gui.back-button-name"));
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getLanguageManager().getRawComponent("gui.back-button-lore-1"));
            lore.add(plugin.getLanguageManager().getRawComponent("gui.back-button-lore-2"));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(backButtonKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLanguageManager().getRawComponent("gui.close-button-name"));
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getLanguageManager().getRawComponent("gui.close-button-lore"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedItem(SlotDefinition def, int slotNumber) {
        return createLockedItem(null, def, slotNumber);
    }

    private ItemStack createLockedItem(Player player, SlotDefinition def, int slotNumber) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLanguageManager().getRawComponent("gui.locked-slot-name",
                    "slot", def.getName(), "number", String.valueOf(slotNumber)));
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getLanguageManager().getRawComponent("gui.locked-slot-lore-1"));
            lore.add(plugin.getLanguageManager().getRawComponent("gui.locked-slot-lore-2",
                    "permission", def.getPermission(), "number", String.valueOf(slotNumber)));
            meta.lore(lore);

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBorderItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    public void playEquipSound(Player player) {
        if (!plugin.getConfig().getBoolean("sounds.equip.enabled", true)) return;
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.equip.sound", "ITEM_ARMOR_EQUIP_GENERIC"));
            float vol = (float) plugin.getConfig().getDouble("sounds.equip.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.equip.pitch", 1.1);
            player.playSound(player.getLocation(), sound, vol, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    public void playUnequipSound(Player player) {
        if (!plugin.getConfig().getBoolean("sounds.unequip.enabled", true)) return;
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.unequip.sound", "ITEM_ARMOR_EQUIP_LEATHER"));
            float vol = (float) plugin.getConfig().getDouble("sounds.unequip.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.unequip.pitch", 0.9);
            player.playSound(player.getLocation(), sound, vol, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    public void playOpenSound(Player player) {
        if (!plugin.getConfig().getBoolean("features.play-gui-sound", true)) return;
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("features.gui-sound", "BLOCK_CHEST_OPEN"));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }

    public void playClickSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        } catch (Throwable ignored) {}
    }
}
