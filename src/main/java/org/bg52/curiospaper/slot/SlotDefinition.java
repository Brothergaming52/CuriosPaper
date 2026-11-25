package org.bg52.curiospaper.slot;

import net.kyori.adventure.text.Component;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.util.BedrockUtil;
import org.bg52.curiospaper.util.TextUtil;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definition of a custom Curios accessory slot type.
 */
public class SlotDefinition {

    private final String id;
    private final String name;
    private final Component nameComponent;
    private final Material icon;
    private final String itemModel;
    private final Integer customModelData;
    private final int guiSlot;
    private final int defaultUnlocked;
    private final int maxSlots;
    private final String permission;
    private final List<String> description;
    private final Float defaultPitchUpLimit;
    private final Float defaultPitchDownLimit;

    public SlotDefinition(String id, String name, Material icon, String itemModel, Integer customModelData,
                          int guiSlot, int defaultUnlocked, int maxSlots, String permission, List<String> description) {
        this(id, name, icon, itemModel, customModelData, guiSlot, defaultUnlocked, maxSlots, permission, description, null, null);
    }

    public SlotDefinition(String id, String name, Material icon, String itemModel, Integer customModelData,
                          int guiSlot, int defaultUnlocked, int maxSlots, String permission, List<String> description,
                          Float defaultPitchUpLimit, Float defaultPitchDownLimit) {
        this.id = id.toLowerCase();
        this.name = name;
        this.nameComponent = TextUtil.parse(name);
        this.icon = icon != null ? icon : Material.ITEM_FRAME;
        this.itemModel = itemModel;
        this.customModelData = customModelData;
        this.guiSlot = guiSlot;
        this.defaultUnlocked = Math.max(1, defaultUnlocked);
        this.maxSlots = Math.max(this.defaultUnlocked, maxSlots);
        this.permission = permission != null ? permission : "curiospaper.slot." + this.id;
        this.description = description != null ? description : Collections.emptyList();
        this.defaultPitchUpLimit = defaultPitchUpLimit;
        this.defaultPitchDownLimit = defaultPitchDownLimit;
    }

    public Float getDefaultPitchUpLimit() {
        return defaultPitchUpLimit;
    }

    public Float getDefaultPitchDownLimit() {
        return defaultPitchDownLimit;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Component getNameComponent() {
        return nameComponent;
    }

    public Material getIcon() {
        return icon;
    }

    public String getItemModel() {
        return itemModel;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public int getGuiSlot() {
        return guiSlot;
    }

    public int getDefaultUnlocked() {
        return defaultUnlocked;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getDescription() {
        return description;
    }

    /**
     * Creates an empty slot indicator / placeholder ItemStack for the GUI.
     */
    public ItemStack createPlaceholderItem() {
        return createPlaceholderItem(null);
    }

    /**
     * Creates an empty slot indicator / placeholder ItemStack for the GUI tailored to the viewing player.
     * Bedrock players connected via Geyser receive Material.PAPER with custom model data to match Geyser mappings.
     */
    public ItemStack createPlaceholderItem(Player player) {
        boolean isBedrock = player != null && BedrockUtil.isBedrockPlayer(player);
        boolean useVanilla = CuriosPaper.getInstance().getConfig().getBoolean("features.use-vanilla-slot-icons", false);
        Material mat = icon;
        if (isBedrock || (!useVanilla && (itemModel != null || customModelData != null))) {
            mat = Material.PAPER;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Component displayName = CuriosPaper.getInstance().getLanguageManager().getRawComponent("gui.empty-slot-name", "slot", name);
            if (displayName == null || displayName.equals(Component.empty())) {
                displayName = nameComponent;
            }
            meta.displayName(displayName);
            List<Component> lore = new ArrayList<>();
            for (String line : description) {
                lore.add(TextUtil.parse(line));
            }
            lore.add(Component.empty());
            Component hint = CuriosPaper.getInstance().getLanguageManager().getRawComponent("gui.empty-slot-lore", "slot", name);
            lore.add(hint);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS);

            if (itemModel != null || customModelData != null) {
                VersionUtil.setItemModelSafe(meta, itemModel, customModelData);
            }

            item.setItemMeta(meta);
        }
        return item;
    }
}
