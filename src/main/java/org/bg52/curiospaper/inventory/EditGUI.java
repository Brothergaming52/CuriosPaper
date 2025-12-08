package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GUI for editing custom items
 */
public class EditGUI implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final ChatInputManager chatInputManager;
    private final Map<UUID, String> currentlyEditing;

    public EditGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
        this.chatInputManager = plugin.getChatInputManager();
        this.currentlyEditing = new HashMap<>();
    }

    public void open(Player player, String itemId) {
        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            player.sendMessage("§cItem not found!");
            return;
        }

        currentlyEditing.put(player.getUniqueId(), itemId);

        Inventory gui = Bukkit.createInventory(null, 54, "§8Edit: " + itemId);

        // Item Preview (slot 4)
        try {
            Material mat = Material.valueOf(itemData.getMaterial().toUpperCase());
            ItemStack preview = new ItemStack(mat);
            preview.editMeta(meta -> {
                if (itemData.getDisplayName() != null) {
                    meta.setDisplayName(itemData.getDisplayName());
                }
                if (!itemData.getLore().isEmpty()) {
                    meta.setLore(itemData.getLore());
                }
                if (itemData.getItemModel() != null && !itemData.getItemModel().isBlank()) {
                    String modelStr = itemData.getItemModel().trim();

                    // 1. Try to parse as CustomModelData (Integer)
                    try {
                        int customModelData = Integer.parseInt(modelStr);
                        meta.setCustomModelData(customModelData);
                    } catch (NumberFormatException e) {
                        // 2. Not an integer, treat as Item Model (NamespacedKey)
                        try {
                            org.bukkit.NamespacedKey key;
                            if (modelStr.contains(":")) {
                                key = org.bukkit.NamespacedKey.fromString(modelStr);
                            } else {
                                key = new org.bukkit.NamespacedKey(plugin, modelStr);
                            }

                            // Check if method exists (Paper 1.21+)
                            // Using suppression in case you compile on older API, but runtime checks are
                            // better
                            meta.setItemModel(key);
                        } catch (Exception ex) {
                            // Fallback or log if needed
                        }
                    }
                }
            });
            if (!(itemData.getSlotType() == null)) {
                preview = plugin.getCuriosPaperAPI().tagAccessoryItem(preview, itemData.getSlotType());
                gui.setItem(4, preview);
            } else {
                gui.setItem(4, preview);
            }
        } catch (Exception e) {
            gui.setItem(4,
                    createGuiItem(Material.BARRIER, "§cInvalid Material", "§7Current: " + itemData.getMaterial()));
        }

        // Basic Properties
        gui.setItem(19, createGuiItem(Material.NAME_TAG, "§e✎ Edit Display Name",
                "§7Current: " + (itemData.getDisplayName() != null ? itemData.getDisplayName() : "§cnone"),
                "", "§eClick to edit via chat"));

        gui.setItem(21, createGuiItem(Material.IRON_BLOCK, "§e⚒ Set Material",
                "§7Current: " + (itemData.getMaterial() != null ? itemData.getMaterial() : "§cnone"),
                "", "§eClick to edit via chat"));

        gui.setItem(23, createGuiItem(Material.PAINTING, "§e⚙ Set Item Model",
                "§7Current: " + (itemData.getItemModel() != null ? itemData.getItemModel() : "§cnone"),
                "", "§eClick to set custom model",
                "§7Format: namespace:model_name"));

        gui.setItem(25, createGuiItem(Material.BOOK, "§e✎ Edit Lore",
                "§7Lines: " + itemData.getLore().size(),
                "", "§eClick to edit via chat"));

        // Slot Selection
        gui.setItem(27, createGuiItem(Material.CHEST, "§e⚡ Set Required Slot  ",
                "§7Current: " + (itemData.getSlotType() != null ? itemData.getSlotType() : "§cnone"),
                "", "§eClick to select slot"));

        // Recipe
        gui.setItem(29, createGuiItem(Material.CRAFTING_TABLE, "§e⚒ Recipe",
                itemData.getRecipe() != null ? "§a✔ Configured" : "§7Not configured",
                "", "§eClick to configure recipe"));

        // Loot Tables
        gui.setItem(31, createGuiItem(Material.CHEST_MINECART, "§e⚒ Loot Tables",
                "§7Entries: " + itemData.getLootTables().size(),
                "", "§eClick to manage loot tables"));

        // Mob Drops
        gui.setItem(33, createGuiItem(Material.ZOMBIE_HEAD, "§e⚒ Mob Drops",
                "§7Entries: " + itemData.getMobDrops().size(),
                "", "§eClick to manage mob drops"));

        // Villager Trades
        gui.setItem(35, createGuiItem(Material.EMERALD, "§e⚒ Villager Trades",
                "§7Entries: " + itemData.getVillagerTrades().size(),
                "", "§eClick to manage trades"));

        // Abilities
        gui.setItem(37, createGuiItem(Material.POTION, "§e⚡ Abilities",
                "§7Configured: " + itemData.getAbilities().size(),
                "", "§eClick to manage abilities"));

        // Save & Close
        gui.setItem(49, createGuiItem(Material.EMERALD, "§a✔ Save & Close",
                "§7Saves all changes"));

        // Fill empty slots with glass pane
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith("§8Edit: "))
            return;

        // Only consider clicks that are inside the top inventory (the GUI).
        // If the raw slot is >= top inventory size, it's the player's inventory —
        // ignore it.
        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (raw >= topSize)
            return; // allow player to interact with their own inventory

        // Now cancel the click (prevent item pickup/drag) because it's in the GUI area
        event.setCancelled(true);

        String itemId = currentlyEditing.get(player.getUniqueId());
        if (itemId == null)
            return;

        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null)
            return;

        switch (raw) {
            case 19: // Edit Display Name (single-line)
                player.closeInventory();
                chatInputManager.startSingleLineSession(player,
                        "Enter the display name for this item (supports color codes with &):",
                        single -> {
                            if (single != null && !single.isBlank()) {
                                itemData.setDisplayName(single.replace('&', '§'));
                                itemDataManager.saveItemData(itemId);
                                player.sendMessage("§a✔ Display name updated!");
                            }
                            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                        },
                        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
                break;

            case 21: // Set Material (single-line)
                player.closeInventory();
                chatInputManager.startSingleLineSession(player,
                        "Enter the material type (e.g., DIAMOND, GOLD_INGOT, PAPER):",
                        single -> {
                            if (single != null && !single.isBlank()) {
                                String material = single.trim().toUpperCase();
                                try {
                                    Material.valueOf(material);
                                    itemData.setMaterial(material);
                                    itemDataManager.saveItemData(itemId);
                                    player.sendMessage("§a✔ Material set to: " + material);
                                } catch (IllegalArgumentException e) {
                                    player.sendMessage("§c✘ Invalid material: " + material);
                                }
                            }
                            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                        },
                        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
                break;

            case 23: // Set Item Model (single-line)
                player.closeInventory();
                chatInputManager.startSingleLineSession(player,
                        "Enter the item model (format: namespace:model_name or just model_name for curiospaper:):",
                        single -> {
                            if (single != null && !single.isBlank()) {
                                String model = single.trim();
                                itemData.setItemModel(model);
                                itemDataManager.saveItemData(itemId);
                                player.sendMessage("§a✔ Item model set to: " + model);
                            }
                            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                        },
                        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
                break;

            case 25: // Edit Lore (multi-line, stays the same)
                player.closeInventory();
                chatInputManager.startSession(player,
                        "Enter lore lines (one per message, supports & color codes):",
                        lines -> {
                            List<String> coloredLore = lines.stream()
                                    .map(line -> line.replace('&', '§'))
                                    .toList();
                            itemData.setLore(coloredLore);
                            itemDataManager.saveItemData(itemId);
                            player.sendMessage("§a✔ Lore updated with " + lines.size() + " lines!");
                            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                        },
                        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
                break;

            case 27: // Set Required Slot (single-line)
                player.closeInventory();
                player.sendMessage("§e▬▬▬ Available Slots ▬▬▬");
                for (String slotType : plugin.getCuriosPaperAPI().getAllSlotTypes()) {
                    player.sendMessage("§6• §e" + slotType);
                }
                player.sendMessage("§e▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");

                chatInputManager.startSingleLineSession(player,
                        "Enter the slot type this item requires:",
                        single -> {
                            if (single != null && !single.isBlank()) {
                                String slotType = single.trim().toLowerCase();
                                if (plugin.getCuriosPaperAPI().isValidSlotType(slotType)) {
                                    itemData.setSlotType(slotType);
                                    itemDataManager.saveItemData(itemId);
                                    player.sendMessage("§a✔ Required slot set to: " + slotType);
                                } else {
                                    player.sendMessage("§c✘ Invalid slot type: " + slotType);
                                }
                            }
                            Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L);
                        },
                        () -> Bukkit.getScheduler().runTaskLater(plugin, () -> open(player, itemId), 2L));
                break;

            case 29: // Recipe
                plugin.getRecipeEditor().open(player, itemId);
                break;
            case 31: // Loot Tables
                plugin.getLootTableBrowser().open(player, itemId);
                break;
            case 33: // Mob Drops
                plugin.getMobDropEditor().open(player, itemId);
                break;
            case 35: // Villager Trades
                plugin.getTradeEditor().open(player, itemId);
                break;
            case 37: // Abilities
                plugin.getAbilityEditor().open(player, itemId);
                break;

            case 49: // Save & Close
                player.closeInventory();
                currentlyEditing.remove(player.getUniqueId());
                itemDataManager.saveItemData(itemId);
                player.sendMessage("§a✔ Saved item: §e" + itemId);
                break;
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
