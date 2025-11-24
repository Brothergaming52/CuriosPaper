package org.bg52.curiospaper.config;

import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class SlotConfiguration {
    private final String key;
    private final String name;
    private final Material icon;
    private final NamespacedKey ItemModel;
    private final int amount;
    private final List<String> lore;

    public SlotConfiguration(String key, String name, Material icon, NamespacedKey ItemModel, int amount,
            List<String> lore) {
        this.key = key;
        this.name = name;
        this.icon = icon;
        this.ItemModel = ItemModel;
        this.amount = amount;
        this.lore = lore;
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

    public @Nullable NamespacedKey getItemModel() {return ItemModel;}

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
