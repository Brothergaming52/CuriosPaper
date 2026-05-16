package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ItemListGUI implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final Map<UUID, Integer> playerPages;

    public ItemListGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
        this.playerPages = new HashMap<>();
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        playerPages.put(player.getUniqueId(), page);

        List<ItemData> allItems = itemDataManager.getAllItemIds().stream()
                .map(itemDataManager::getItemData)
                .filter(Objects::nonNull)
                .filter(item -> !item.isHidden())
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil(allItems.size() / 45.0);
        if (page < 0) page = 0;
        if (page >= totalPages && totalPages > 0) page = totalPages - 1;

        Inventory gui = Bukkit.createInventory(null, 54, "§8Curios Items (Page " + (page + 1) + ")");

        int start = page * 45;
        int end = Math.min(start + 45, allItems.size());

        for (int i = start; i < end; i++) {
            ItemData data = allItems.get(i);
            ItemStack item = plugin.getCuriosPaperAPI().createItemStack(data.getItemId());
            if (item == null) {
                item = createGuiItem(Material.BARRIER, "§cError: " + data.getItemId(), "§7Failed to build item stack");
            } else {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("");
                    lore.add("§eLeft-Click §7to view recipes");
                    lore.add("§eRight-Click §7to edit item");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
            }
            gui.setItem(i - start, item);
        }

        // Navigation row
        if (page > 0) {
            gui.setItem(45, createGuiItem(Material.ARROW, "§ePrevious Page"));
        }
        
        gui.setItem(49, createGuiItem(Material.BARRIER, "§cClose"));

        if (page < totalPages - 1) {
            gui.setItem(53, createGuiItem(Material.ARROW, "§eNext Page"));
        }

        // Fill empty slots in bottom row
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String title = event.getView().getTitle();
        if (!title.startsWith("§8Curios Items")) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot >= 54) return;

        int page = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (slot == 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
            open(player, page - 1);
            return;
        }

        if (slot == 53 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
            open(player, page + 1);
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot < 45) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            String itemId = getCuriosItemId(clicked);
            if (itemId == null) return;

            if (event.getClick() == ClickType.RIGHT) {
                plugin.getEditGUI().open(player, itemId);
            } else if (event.getClick() == ClickType.LEFT) {
                plugin.getItemRecipeListGUI().open(player, itemId);
            }
        }
    }

    private String getCuriosItemId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(plugin.getCuriosPaperAPI().getItemIdKey(), org.bukkit.persistence.PersistentDataType.STRING);
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
