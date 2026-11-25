package org.bg52.curiospaper.inventory;

import net.kyori.adventure.text.Component;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bg52.curiospaper.util.TextUtil;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Administrative Inspection GUI allowing staff to inspect or modify another player's curios accessories.
 * Uses the same two-tier design: Overview of active slots -> Click to manage individual slot.
 */
public class InspectGUI implements Listener {

    private final CuriosPaper plugin;

    public InspectGUI(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    // =========================================================================
    // Tier-1: Inspect Overview GUI
    // =========================================================================

    public void open(Player admin, Player target) {
        if (target == null) return;
        PlayerCuriosInventory targetInv = plugin.getStorageManager().getInventory(target.getUniqueId());
        List<String> activeKeys = plugin.getSlotRegistry().getActiveSlotKeysFor(target);
        int activeCount = activeKeys.size();
        int size = plugin.getCuriosGUI().computeOverviewSize(activeCount);

        InspectOverviewHolder holder = new InspectOverviewHolder(admin, target.getUniqueId(), target.getName());
        Component title = plugin.getLanguageManager().getRawComponent("gui.inspect-title", "player", target.getName());
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        createBorderAndFill(inv, size);

        int[] positions = plugin.getCuriosGUI().getPresetPositions(activeCount, size);
        for (int i = 0; i < activeCount && i < positions.length; i++) {
            String slotId = activeKeys.get(i);
            SlotDefinition def = plugin.getSlotRegistry().getSlot(slotId);
            if (def == null) continue;

            int slotPos = positions[i];
            holder.mapSlotButton(slotPos, def.getId());

            int equipped = targetInv.countEquipped(def.getId());
            int unlocked = targetInv.getUnlockedSlots(def, target);

            ItemStack button = createInspectSlotButton(def, equipped, unlocked);
            inv.setItem(slotPos, button);
        }

        admin.openInventory(inv);
    }

    // =========================================================================
    // Tier-2: Inspect Slot Items GUI
    // =========================================================================

    public void openSlotGUI(Player admin, UUID targetUUID, String targetName, SlotDefinition def) {
        if (def == null) return;
        Player target = Bukkit.getPlayer(targetUUID);
        PlayerCuriosInventory targetInv = plugin.getStorageManager().getInventory(targetUUID);

        int maxSlots = def.getMaxSlots();
        int size = plugin.getCuriosGUI().calculateSlotGUISize(maxSlots);
        int[] slotPositions = plugin.getCuriosGUI().calculateSlotPositions(maxSlots, size);

        InspectSlotHolder holder = new InspectSlotHolder(admin, targetUUID, targetName, def);
        Component title = TextUtil.parse("<dark_gray>Inspect:</dark_gray> <gold>" + targetName + "</gold> <dark_gray>»</dark_gray> <yellow>" + def.getName() + "</yellow>");
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        createBorderAndFill(inv, size);

        // Back button placed at bottom-left (slot size - 9)
        inv.setItem(size - 9, createBackButton());

        int unlocked = target != null ? targetInv.getUnlockedSlots(def, target) : def.getMaxSlots();

        for (int subIndex = 0; subIndex < maxSlots && subIndex < slotPositions.length; subIndex++) {
            int targetSlot = slotPositions[subIndex];
            holder.mapSlot(targetSlot, subIndex);

            if (subIndex < unlocked) {
                ItemStack equipped = targetInv.getItem(def.getId(), subIndex);
                if (equipped != null && equipped.getType() != Material.AIR) {
                    inv.setItem(targetSlot, equipped.clone());
                } else {
                    inv.setItem(targetSlot, def.createPlaceholderItem(admin));
                }
            } else {
                inv.setItem(targetSlot, createLockedItem(def, subIndex + 1));
            }
        }

        admin.openInventory(inv);
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
        // Case A: Clicking inside Inspect Overview GUI
        // ---------------------------------------------------------------------
        if (top.getHolder() instanceof InspectOverviewHolder) {
            InspectOverviewHolder holder = (InspectOverviewHolder) top.getHolder();
            int rawSlot = event.getRawSlot();

            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                event.setCancelled(true);
                String slotType = holder.getSlotType(rawSlot);
                if (slotType != null) {
                    SlotDefinition def = plugin.getSlotRegistry().getSlot(slotType);
                    if (def != null) {
                        try {
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                        } catch (Throwable ignored) {}
                        openSlotGUI(player, holder.getTargetUUID(), holder.getTargetName(), def);
                    }
                }
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        // ---------------------------------------------------------------------
        // Case B: Clicking inside Inspect Slot Items GUI
        // ---------------------------------------------------------------------
        if (top.getHolder() instanceof InspectSlotHolder) {
            InspectSlotHolder holder = (InspectSlotHolder) top.getHolder();
            int rawSlot = event.getRawSlot();
            SlotDefinition def = holder.getSlotDef();
            PlayerCuriosInventory targetInv = plugin.getStorageManager().getInventory(holder.getTargetUUID());

            if (rawSlot >= 0 && rawSlot < top.getSize()) {
                event.setCancelled(true);

                // Check Back Button
                if (rawSlot == top.getSize() - 9) {
                    try {
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                    } catch (Throwable ignored) {}
                    Player target = Bukkit.getPlayer(holder.getTargetUUID());
                    if (target != null) {
                        open(player, target);
                    } else {
                        player.closeInventory();
                    }
                    return;
                }

                Integer subIndex = holder.getSubIndex(rawSlot);
                if (subIndex == null) {
                    return; // border/filler
                }

                ItemStack cursor = event.getCursor();
                ItemStack currentlyEquipped = targetInv.getItem(def.getId(), subIndex);

                if (cursor == null || cursor.getType().isAir()) {
                    // Take item
                    if (currentlyEquipped != null && !currentlyEquipped.getType().isAir()) {
                        targetInv.setItem(def.getId(), subIndex, null);
                        top.setItem(rawSlot, def.createPlaceholderItem(player));
                        player.setItemOnCursor(currentlyEquipped);
                    }
                } else {
                    // Place/Swap item
                    String targetSlot = plugin.getItemEngineManager().resolveCuriosSlot(cursor);
                    if (targetSlot == null || !targetSlot.equalsIgnoreCase(def.getId())) {
                        plugin.getLanguageManager().sendMessage(player, "inventory.invalid-item");
                        return;
                    }

                    ItemStack toEquip = cursor.clone();
                    toEquip.setAmount(1);

                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                        player.setItemOnCursor(cursor);
                    } else {
                        player.setItemOnCursor(currentlyEquipped);
                    }

                    targetInv.setItem(def.getId(), subIndex, toEquip);
                    top.setItem(rawSlot, toEquip.clone());
                }

                // Update target player models and attributes if online
                Player target = Bukkit.getPlayer(holder.getTargetUUID());
                if (target != null && target.isOnline()) {
                    plugin.getPacketModelManager().updatePlayerAccessories(target);
                    if (plugin.getAbilityListener() != null) {
                        plugin.getAbilityListener().reapplyAllCuriosAttributes(target);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null) {
            if (top.getHolder() instanceof InspectOverviewHolder) {
                InspectOverviewHolder holder = (InspectOverviewHolder) top.getHolder();
                plugin.getStorageManager().savePlayer(holder.getTargetUUID());
            } else if (top.getHolder() instanceof InspectSlotHolder) {
                InspectSlotHolder holder = (InspectSlotHolder) top.getHolder();
                plugin.getStorageManager().savePlayer(holder.getTargetUUID());
            }
        }
    }

    private void createBorderAndFill(Inventory inv, int size) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

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

    private ItemStack createInspectSlotButton(SlotDefinition def, int equipped, int total) {
        ItemStack button = new ItemStack(def.getIcon());
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
            button.setItemMeta(meta);
        }
        return button;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getLanguageManager().getRawComponent("gui.back-button-name"));
            List<Component> lore = new ArrayList<>();
            lore.add(plugin.getLanguageManager().getRawComponent("gui.back-button-lore-1"));
            lore.add(plugin.getLanguageManager().getRawComponent("gui.back-button-lore-2"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedItem(SlotDefinition def, int slotNumber) {
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
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
