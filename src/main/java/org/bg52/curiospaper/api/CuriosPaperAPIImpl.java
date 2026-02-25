package org.bg52.curiospaper.api;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.config.SlotConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CuriosPaperAPIImpl implements CuriosPaperAPI {
    private final CuriosPaper plugin;
    private final NamespacedKey slotTypeKey;
    private final NamespacedKey itemIdKey;

    public CuriosPaperAPIImpl(CuriosPaper plugin) {
        this.plugin = plugin;
        this.slotTypeKey = new NamespacedKey(plugin, "curious_slot_type");
        this.itemIdKey = new NamespacedKey(plugin, "curios_custom_id");
    }

    @Override
    public NamespacedKey getSlotTypeKey() {
        return slotTypeKey;
    }

    @Override
    public NamespacedKey getItemIdKey() {
        return itemIdKey;
    }

    @Override
    public ItemStack createItemStack(String itemId) {
        org.bg52.curiospaper.data.ItemData itemData = getItemData(itemId);
        if (itemData == null) {
            return null;
        }

        try {
            org.bukkit.Material material = org.bukkit.Material.valueOf(itemData.getMaterial().toUpperCase());
            ItemStack item = new ItemStack(material);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Set display name
                if (itemData.getDisplayName() != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemData.getDisplayName()));
                }

                // Set lore
                if (!itemData.getLore().isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : itemData.getLore()) {
                        coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(coloredLore);
                }

                // Set item model
                if (itemData.getItemModel() != null && !itemData.getItemModel().isEmpty()) {
                    org.bg52.curiospaper.util.VersionUtil.setItemModelSafe(meta, itemData.getItemModel(),
                            itemData.getCustomModelData());
                }

                // Set custom ID in PDC
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(itemIdKey, PersistentDataType.STRING, itemId);

                item.setItemMeta(meta);
            }

            // Tag for slot if applicable
            if (itemData.getSlotType() != null && !itemData.getSlotType().isEmpty()) {
                item = tagAccessoryItem(item, itemData.getSlotType());
            }

            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create ItemStack for " + itemId + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isValidAccessory(ItemStack itemStack, String slotType) {
        if (itemStack == null || itemStack.getType() == org.bukkit.Material.AIR) {
            return false;
        }

        // Special handling for elytra in back slot when feature is enabled
        if (itemStack.getType() == org.bukkit.Material.ELYTRA &&
                "back".equalsIgnoreCase(slotType) &&
                plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", false)) {
            return true;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        String requiredType = container.get(slotTypeKey, PersistentDataType.STRING);

        if (requiredType == null) {
            return false;
        }

        return slotType.equalsIgnoreCase(requiredType);
    }

    @Override
    public ItemStack tagAccessoryItem(ItemStack itemStack, String slotType, boolean addLore) {
        if (itemStack == null || itemStack.getType() == org.bukkit.Material.AIR) {
            throw new IllegalArgumentException("Cannot tag air or null items");
        }

        if (!isValidSlotType(slotType)) {
            throw new IllegalArgumentException("Invalid slot type: " + slotType);
        }

        ItemStack tagged = itemStack.clone();
        ItemMeta meta = tagged.getItemMeta();

        if (meta == null) {
            meta = plugin.getServer().getItemFactory().getItemMeta(tagged.getType());
        }

        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(slotTypeKey, PersistentDataType.STRING, slotType.toLowerCase());

            if (addLore) {
                SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
                if (config != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    String loreLine = ChatColor.GRAY + "Required Slot: " + ChatColor.RESET + config.getName();

                    boolean hasLine = false;
                    for (String line : lore) {
                        if (line.equals(loreLine)) {
                            hasLine = true;
                            break;
                        }
                    }

                    if (!hasLine) {
                        lore.add("");
                        lore.add(loreLine);
                        meta.setLore(lore);
                    }
                }
            }

            tagged.setItemMeta(meta);
        }

        return tagged;
    }

    @Override
    public String getAccessorySlotType(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == org.bukkit.Material.AIR) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(slotTypeKey, PersistentDataType.STRING);
    }

    @Override
    public List<ItemStack> getEquippedItems(Player player, String slotType) {
        return getEquippedItems(player.getUniqueId(), slotType);
    }

    @Override
    public List<ItemStack> getEquippedItems(UUID playerId, String slotType) {
        return plugin.getSlotManager().getAccessories(playerId, slotType);
    }

    @Override
    public void setEquippedItems(Player player, String slotType, List<ItemStack> items) {
        setEquippedItems(player.getUniqueId(), slotType, items);
    }

    @Override
    public void setEquippedItems(UUID playerId, String slotType, List<ItemStack> items) {
        plugin.getSlotManager().setAccessories(playerId, slotType, items);
    }

    @Override
    public ItemStack getEquippedItem(Player player, String slotType, int index) {
        return getEquippedItem(player.getUniqueId(), slotType, index);
    }

    @Override
    public ItemStack getEquippedItem(UUID playerId, String slotType, int index) {
        return plugin.getSlotManager().getAccessoryItem(playerId, slotType, index);
    }

    @Override
    public void setEquippedItem(Player player, String slotType, int index, ItemStack item) {
        setEquippedItem(player.getUniqueId(), slotType, index, item);
    }

    @Override
    public void setEquippedItem(UUID playerId, String slotType, int index, ItemStack item) {
        plugin.getSlotManager().setAccessoryItem(playerId, slotType, index, item);
    }

    @Override
    public boolean removeEquippedItem(Player player, String slotType, ItemStack itemToRemove) {
        return removeEquippedItem(player.getUniqueId(), slotType, itemToRemove);
    }

    @Override
    public boolean removeEquippedItem(UUID playerId, String slotType, ItemStack itemToRemove) {
        List<ItemStack> items = getEquippedItems(playerId, slotType);

        for (int i = 0; i < items.size(); i++) {
            ItemStack current = items.get(i);
            if (current != null && current.isSimilar(itemToRemove)) {
                items.set(i, null);
                setEquippedItems(playerId, slotType, items);
                return true;
            }
        }

        return false;
    }

    @Override
    public ItemStack removeEquippedItemAt(Player player, String slotType, int index) {
        return removeEquippedItemAt(player.getUniqueId(), slotType, index);
    }

    @Override
    public ItemStack removeEquippedItemAt(UUID playerId, String slotType, int index) {
        ItemStack removed = getEquippedItem(playerId, slotType, index);
        if (removed != null && removed.getType() != org.bukkit.Material.AIR) {
            setEquippedItem(playerId, slotType, index, null);
            return removed;
        }
        return null;
    }

    @Override
    public void clearEquippedItems(Player player, String slotType) {
        clearEquippedItems(player.getUniqueId(), slotType);
    }

    @Override
    public void clearEquippedItems(UUID playerId, String slotType) {
        int slotAmount = getSlotAmount(slotType);
        List<ItemStack> emptyList = new ArrayList<>();
        for (int i = 0; i < slotAmount; i++) {
            emptyList.add(null);
        }
        setEquippedItems(playerId, slotType, emptyList);
    }

    @Override
    public boolean isValidSlotType(String slotType) {
        return plugin.getConfigManager().hasSlotType(slotType);
    }

    @Override
    public int getSlotAmount(String slotType) {
        SlotConfiguration config = plugin.getConfigManager().getSlotConfiguration(slotType);
        return config != null ? config.getAmount() : 0;
    }

    @Override
    public List<String> getAllSlotTypes() {
        return new ArrayList<>(plugin.getConfigManager().getSlotConfigurations().keySet());
    }

    @Override
    public boolean hasEquippedItems(Player player, String slotType) {
        return hasEquippedItems(player.getUniqueId(), slotType);
    }

    @Override
    public boolean hasEquippedItems(UUID playerId, String slotType) {
        List<ItemStack> items = getEquippedItems(playerId, slotType);
        return items.stream().anyMatch(item -> item != null && item.getType() != org.bukkit.Material.AIR);
    }

    @Override
    public int countEquippedItems(Player player, String slotType) {
        return countEquippedItems(player.getUniqueId(), slotType);
    }

    @Override
    public int countEquippedItems(UUID playerId, String slotType) {
        List<ItemStack> items = getEquippedItems(playerId, slotType);
        return (int) items.stream()
                .filter(item -> item != null && item.getType() != org.bukkit.Material.AIR)
                .count();
    }

    // ========== SLOT REGISTRATION ==========

    @Override
    public boolean registerSlot(String slotType, String displayName, org.bukkit.Material icon,
            String itemModel, Integer customModelData, int amount, java.util.List<String> lore) {
        if (slotType == null || slotType.trim().isEmpty()) {
            plugin.getLogger().warning("Cannot register slot with null or empty type");
            return false;
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = slotType;
        }

        if (icon == null || !icon.isItem()) {
            plugin.getLogger().warning("Invalid icon material for slot: " + slotType);
            return false;
        }

        if (amount < 1) {
            plugin.getLogger().warning("Slot amount must be at least 1");
            return false;
        }

        if (amount > 54) {
            plugin.getLogger().warning("Slot amount cannot exceed 54. Capping at 54.");
            amount = 54;
        }

        // Parse item model key if present using version-safe util
        org.bukkit.NamespacedKey modelKey = null;
        if (itemModel != null && !itemModel.isEmpty()) {
            modelKey = org.bg52.curiospaper.util.VersionUtil.parseNamespacedKey(itemModel);
        }

        SlotConfiguration config = new SlotConfiguration(
                slotType, displayName, icon, modelKey, customModelData, amount,
                lore != null ? lore : new java.util.ArrayList<>());

        return plugin.getConfigManager().addSlotConfiguration(slotType, config);
    }

    @Override
    public boolean unregisterSlot(String slotType) {
        if (slotType == null) {
            return false;
        }
        return plugin.getConfigManager().removeSlotConfiguration(slotType);
    }

    // ========== ITEM DATA MANAGEMENT ==========

    @Override
    public boolean registerItemRecipe(String itemId, org.bg52.curiospaper.data.RecipeData recipe) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = plugin.getItemDataManager();
        if (itemDataManager == null) {
            plugin.getLogger().warning(
                    "ItemDataManager not initialized. Plugin might not work correctly because 'features.item-editor.enabled' is disabled.");
            return false;
        }

        org.bg52.curiospaper.data.ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found");
            return false;
        }

        if (recipe == null || !recipe.isValid()) {
            plugin.getLogger().warning("Invalid recipe data for item: " + itemId);
            return false;
        }

        itemData.addRecipe(recipe);
        if (itemDataManager.saveItemData(itemId)) {
            // Register the new recipe in the server immediately
            plugin.getRecipeListener().registerRecipe(itemData, recipe);
            return true;
        }
        return false;
    }

    @Override
    public boolean registerItemLootTable(String itemId, org.bg52.curiospaper.data.LootTableData lootTable) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = plugin.getItemDataManager();
        if (itemDataManager == null) {
            plugin.getLogger().warning(
                    "ItemDataManager not initialized. Plugin might not work correctly because 'features.item-editor.enabled' is disabled.");
            return false;
        }

        org.bg52.curiospaper.data.ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found");
            return false;
        }

        if (lootTable == null || !lootTable.isValid()) {
            plugin.getLogger().warning("Invalid loot table data for item: " + itemId);
            return false;
        }

        // Check if this loot table is already registered to avoid duplicates
        for (org.bg52.curiospaper.data.LootTableData existing : itemData.getLootTables()) {
            if (isSameLootTable(existing, lootTable)) {
                plugin.getLogger().info("Loot table for item '" + itemId + "' already exists (skipping duplicate)");
                return true;
            }
        }

        itemData.addLootTable(lootTable);
        return itemDataManager.saveItemData(itemId);
    }

    /**
     * Helper method to check if two loot tables are effectively the same
     */
    private boolean isSameLootTable(org.bg52.curiospaper.data.LootTableData lt1,
            org.bg52.curiospaper.data.LootTableData lt2) {
        if (lt1 == lt2)
            return true;
        if (lt1 == null || lt2 == null)
            return false;

        return lt1.getLootTableType().equalsIgnoreCase(lt2.getLootTableType()) &&
                Double.compare(lt1.getChance(), lt2.getChance()) == 0 &&
                lt1.getMinAmount() == lt2.getMinAmount() &&
                lt1.getMaxAmount() == lt2.getMaxAmount();
    }

    @Override
    public boolean registerItemMobDrop(String itemId, org.bg52.curiospaper.data.MobDropData mobDrop) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = plugin.getItemDataManager();
        if (itemDataManager == null) {
            plugin.getLogger().warning(
                    "ItemDataManager not initialized. Plugin might not work correctly because 'features.item-editor.enabled' is disabled.");
            return false;
        }

        org.bg52.curiospaper.data.ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found");
            return false;
        }

        if (mobDrop == null || !mobDrop.isValid()) {
            plugin.getLogger().warning("Invalid mob drop data for item: " + itemId);
            return false;
        }

        // Check if this mob drop is already registered to avoid duplicates
        for (org.bg52.curiospaper.data.MobDropData existing : itemData.getMobDrops()) {
            if (isSameMobDrop(existing, mobDrop)) {
                plugin.getLogger().info("Mob drop for item '" + itemId + "' already exists (skipping duplicate)");
                return true;
            }
        }

        itemData.addMobDrop(mobDrop);
        return itemDataManager.saveItemData(itemId);
    }

    /**
     * Helper method to check if two mob drops are effectively the same
     */
    private boolean isSameMobDrop(org.bg52.curiospaper.data.MobDropData md1,
            org.bg52.curiospaper.data.MobDropData md2) {
        if (md1 == md2)
            return true;
        if (md1 == null || md2 == null)
            return false;

        return md1.getEntityType().equalsIgnoreCase(md2.getEntityType()) &&
                Double.compare(md1.getChance(), md2.getChance()) == 0 &&
                md1.getMinAmount() == md2.getMinAmount() &&
                md1.getMaxAmount() == md2.getMaxAmount();
    }

    @Override
    public org.bg52.curiospaper.data.ItemData getItemData(String itemId) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = plugin.getItemDataManager();
        if (itemDataManager == null) {
            return null;
        }
        return itemDataManager.getItemData(itemId);
    }

    @Override
    public org.bg52.curiospaper.data.ItemData createItem(String itemId) {
        return createItem(null, itemId);
    }

    @Override
    public org.bg52.curiospaper.data.ItemData createItem(org.bukkit.plugin.Plugin plugin, String itemId) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = this.plugin.getItemDataManager();
        if (itemDataManager == null) {
            this.plugin.getLogger().warning(
                    "ItemDataManager not initialized. Plugin might not work correctly because 'features.item-editor.enabled' is disabled.");
            return null;
        }
        return itemDataManager.createItem(plugin, itemId);
    }

    @Override
    public boolean saveItemData(String itemId) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = plugin.getItemDataManager();
        if (itemDataManager == null) {
            return false;
        }
        return itemDataManager.saveItemData(itemId);
    }

    @Override
    public boolean registerItemVillagerTrade(String itemId, org.bg52.curiospaper.data.VillagerTradeData trade) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = plugin.getItemDataManager();
        if (itemDataManager == null) {
            plugin.getLogger().warning(
                    "ItemDataManager not initialized. Plugin might not work correctly because 'features.item-editor.enabled' is disabled.");
            return false;
        }

        org.bg52.curiospaper.data.ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found");
            return false;
        }

        if (trade == null || !trade.isValid()) {
            plugin.getLogger().warning("Invalid villager trade data for item: " + itemId);
            return false;
        }

        // Check if this villager trade is already registered to avoid duplicates
        for (org.bg52.curiospaper.data.VillagerTradeData existing : itemData.getVillagerTrades()) {
            if (isSameVillagerTrade(existing, trade)) {
                plugin.getLogger().info("Villager trade for item '" + itemId + "' already exists (skipping duplicate)");
                return true;
            }
        }

        itemData.addVillagerTrade(trade);
        return itemDataManager.saveItemData(itemId);
    }

    /**
     * Helper method to check if two villager trades are effectively the same
     */
    private boolean isSameVillagerTrade(org.bg52.curiospaper.data.VillagerTradeData vt1,
            org.bg52.curiospaper.data.VillagerTradeData vt2) {
        if (vt1 == vt2)
            return true;
        if (vt1 == null || vt2 == null)
            return false;

        // Compare professions, chance, and levels
        if (!vt1.getProfessions().equals(vt2.getProfessions()))
            return false;
        if (Double.compare(vt1.getChance(), vt2.getChance()) != 0)
            return false;
        if (!vt1.getTradeLevels().equals(vt2.getTradeLevels()))
            return false;

        // Compare cost items
        if (vt1.getCostItems().size() != vt2.getCostItems().size())
            return false;
        for (int i = 0; i < vt1.getCostItems().size(); i++) {
            org.bg52.curiospaper.data.VillagerTradeData.TradeCost c1 = vt1.getCostItems().get(i);
            org.bg52.curiospaper.data.VillagerTradeData.TradeCost c2 = vt2.getCostItems().get(i);
            if (!c1.getMaterial().equals(c2.getMaterial()) ||
                    c1.getMinAmount() != c2.getMinAmount() ||
                    c1.getMaxAmount() != c2.getMaxAmount()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean deleteItem(String itemId) {
        org.bg52.curiospaper.manager.ItemDataManager itemDataManager = plugin.getItemDataManager();
        if (itemDataManager == null) {
            return false;
        }
        return itemDataManager.deleteItem(itemId);
    }

    @Override
    public void registerResourcePackAssets(org.bukkit.plugin.Plugin plugin, java.io.File folder) {
        this.plugin.getResourcePackManager().registerResource(plugin, folder);
    }

    @Override
    public java.io.File registerResourcePackAssetsFromJar(org.bukkit.plugin.Plugin sourcePlugin) {
        // Target: <that plugin's data folder>/resources
        java.io.File targetFolder = new java.io.File(sourcePlugin.getDataFolder(), "resources");
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            this.plugin.getLogger().severe(
                    "[CuriosPaper] Failed to create resources directory for " +
                            sourcePlugin.getName() + ": " + targetFolder.getAbsolutePath());
        }

        try {
            extractEmbeddedResourcesFolder(sourcePlugin, "resources/", targetFolder);
            this.plugin.getLogger().info(
                    "[CuriosPaper] Extracted embedded resources for " +
                            sourcePlugin.getName() + " to " + targetFolder.getAbsolutePath());
        } catch (Exception e) {
            this.plugin.getLogger().severe(
                    "[CuriosPaper] Failed to extract embedded resources for " +
                            sourcePlugin.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Register with the pack builder
        this.plugin.getResourcePackManager().registerResource(sourcePlugin, targetFolder);
        return targetFolder;
    }

    /**
     * Extracts all entries under `jarPrefix` (e.g. "resources/") from the given
     * plugin's JAR into the specified targetRoot.
     *
     * Existing files are NOT overwritten (so server owners can edit them).
     */
    private void extractEmbeddedResourcesFolder(org.bukkit.plugin.Plugin sourcePlugin,
            String jarPrefix,
            java.io.File targetRoot) throws Exception {
        if (!jarPrefix.endsWith("/")) {
            jarPrefix = jarPrefix + "/";
        }

        java.net.URL jarUrl = sourcePlugin.getClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();

        if (jarUrl == null) {
            this.plugin.getLogger().warning(
                    "[CuriosPaper] Could not locate plugin JAR for " +
                            sourcePlugin.getName() + "; skipping embedded resources extraction.");
            return;
        }

        java.io.File jarFile = new java.io.File(jarUrl.toURI());
        if (!jarFile.isFile()) {
            this.plugin.getLogger().warning(
                    "[CuriosPaper] Code source is not a file for " +
                            sourcePlugin.getName() + ": " + jarFile.getAbsolutePath());
            return;
        }

        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.startsWith(jarPrefix))
                    continue;

                String relativePath = name.substring(jarPrefix.length());
                if (relativePath.isEmpty())
                    continue;

                java.io.File outFile = new java.io.File(targetRoot, relativePath);

                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        this.plugin.getLogger().warning(
                                "[CuriosPaper] Failed to create directory for resource: " +
                                        outFile.getAbsolutePath());
                    }
                    continue;
                }

                // Do not overwrite existing server-edited files
                if (outFile.exists())
                    continue;

                java.io.File parent = outFile.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    this.plugin.getLogger().warning(
                            "[CuriosPaper] Failed to create parent directories for: " +
                                    outFile.getAbsolutePath());
                    continue;
                }

                try (java.io.InputStream in = jar.getInputStream(entry);
                        java.io.OutputStream out = new java.io.FileOutputStream(outFile)) {

                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }
    }
}