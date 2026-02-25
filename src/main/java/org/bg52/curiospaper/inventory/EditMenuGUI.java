package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.SlotConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EditMenuGUI {
    private final CuriosPaper plugin;
    public static final String EDIT_MENU_TITLE = "§c⚙ Edit Accessory Menu ⚙";
    private static final Material FILLER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material SELECTED_MATERIAL = Material.LIME_STAINED_GLASS_PANE;
    private static final String FILLER_NAME = "§r";
    private static final String SELECTED_PREFIX = "§a§l✔ ";

    public EditMenuGUI(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        int size = plugin.getConfigManager().getMainGuiSize();
        Inventory inv = Bukkit.createInventory(null, size, EDIT_MENU_TITLE);

        // Fill with gray glass
        ItemStack filler = createFillerItem(FILLER_MATERIAL);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler);
        }

        // Place current slots
        Map<String, Integer> layout = plugin.getConfigManager().getMainLayout();
        Map<String, SlotConfiguration> configs = plugin.getConfigManager().getSlotConfigurations();

        for (SlotConfiguration config : configs.values()) {
            String key = config.getKey();
            int pos = layout.getOrDefault(key.toLowerCase(), -1);

            ItemStack button = createEditButton(config);

            if (pos >= 0 && pos < size) {
                inv.setItem(pos, button);
            } else {
                // Find first filler slot to place the missing button
                for (int i = 0; i < size; i++) {
                    ItemStack current = inv.getItem(i);
                    if (isFiller(current)) {
                        inv.setItem(i, button);
                        break;
                    }
                }
            }
        }

        player.openInventory(inv);
    }

    private ItemStack createEditButton(SlotConfiguration config) {
        ItemStack item = new ItemStack(config.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(config.getName());
            List<String> lore = new ArrayList<>();
            lore.add("§7Key: §f" + config.getKey());
            lore.add("");
            lore.add("§e▶ Click to select, then click another slot to swap");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(plugin.getSlotTypeKey(), PersistentDataType.STRING, config.getKey());
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a green glass pane marker indicating a button is selected.
     *
     * @param displayName The original button's display name
     * @return The green glass pane selection marker
     */
    public static ItemStack createSelectedMarker(String displayName) {
        ItemStack marker = new ItemStack(SELECTED_MATERIAL);
        ItemMeta meta = marker.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SELECTED_PREFIX + (displayName != null ? displayName : "Selected"));
            List<String> lore = new ArrayList<>();
            lore.add("§7Click another slot to swap");
            lore.add("§7Click here again to deselect");
            meta.setLore(lore);
            marker.setItemMeta(meta);
        }
        return marker;
    }

    /**
     * Checks if an item is the green glass selection marker.
     */
    public static boolean isSelectedMarker(ItemStack item) {
        if (item == null || item.getType() != SELECTED_MATERIAL)
            return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getDisplayName() != null && meta.getDisplayName().startsWith(SELECTED_PREFIX);
    }

    private ItemStack createFillerItem(Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(FILLER_NAME);
            filler.setItemMeta(meta);
        }
        return filler;
    }

    public static ItemStack createFiller() {
        ItemStack filler = new ItemStack(FILLER_MATERIAL);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(FILLER_NAME);
            filler.setItemMeta(meta);
        }
        return filler;
    }

    public static boolean isFiller(ItemStack item) {
        if (item == null || item.getType() != FILLER_MATERIAL)
            return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && FILLER_NAME.equals(meta.getDisplayName());
    }

    public static boolean isEditMenu(String title) {
        return EDIT_MENU_TITLE.equals(title);
    }
}
