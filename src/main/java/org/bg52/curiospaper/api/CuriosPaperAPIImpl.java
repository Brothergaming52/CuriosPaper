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

    public CuriosPaperAPIImpl(CuriosPaper plugin) {
        this.plugin = plugin;
        this.slotTypeKey = new NamespacedKey(plugin, "curious_slot_type");
    }

    @Override
    public NamespacedKey getSlotTypeKey() {
        return slotTypeKey;
    }

    @Override
    public boolean isValidAccessory(ItemStack itemStack, String slotType) {
        if (itemStack == null || itemStack.getType().isAir()) {
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
        if (itemStack == null || itemStack.getType().isAir()) {
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
                    lore.add("");
                    lore.add(ChatColor.GRAY + "Required Slot: " + ChatColor.RESET + config.getName());
                    meta.setLore(lore);
                }
            }

            tagged.setItemMeta(meta);
        }

        return tagged;
    }

    @Override
    public String getAccessorySlotType(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
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
        if (removed != null && !removed.getType().isAir()) {
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
        return items.stream().anyMatch(item -> item != null && !item.getType().isAir());
    }

    @Override
    public int countEquippedItems(Player player, String slotType) {
        return countEquippedItems(player.getUniqueId(), slotType);
    }

    @Override
    public int countEquippedItems(UUID playerId, String slotType) {
        List<ItemStack> items = getEquippedItems(playerId, slotType);
        return (int) items.stream()
                .filter(item -> item != null && !item.getType().isAir())
                .count();
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