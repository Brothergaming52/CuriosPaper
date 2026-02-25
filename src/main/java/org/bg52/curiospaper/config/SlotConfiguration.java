package org.bg52.curiospaper.config;

import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.stream.Collectors;

public class SlotConfiguration {
    private final String key;
    private final String name;
    private final Material icon;
    private final NamespacedKey itemModel;
    private final Integer customModelData;
    private final int amount;
    private final List<String> lore;

    public SlotConfiguration(String key, String name, Material icon, NamespacedKey itemModel, Integer customModelData,
            int amount,
            List<String> lore) {
        this.key = key;
        this.name = name;
        this.icon = icon;
        this.itemModel = itemModel;
        this.customModelData = customModelData;
        this.amount = amount;
        this.lore = lore;
    }

    /**
     * Legacy constructor for backwards compatibility
     */
    public SlotConfiguration(String key, String name, Material icon, NamespacedKey itemModel, int amount,
            List<String> lore) {
        this(key, name, icon, itemModel, null, amount, lore);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return ChatColor.translateAlternateColorCodes('&', name);
    }

    public String getRawName() {
        return name;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * Get the NamespacedKey item model (for MC 1.21.3+)
     * 
     * @return NamespacedKey or null if not set
     */
    public NamespacedKey getItemModel() {
        return itemModel;
    }

    /**
     * Get the CustomModelData value (for MC 1.14 - 1.21.2)
     * 
     * @return Integer or null if not set
     */
    public Integer getCustomModelData() {
        return customModelData;
    }

    public int getAmount() {
        return amount;
    }

    public List<String> getLore() {
        return lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
    }

    public List<String> getRawLore() {
        return lore;
    }
}
