package org.bg52.curiospaper.slot;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.inventory.PlayerCuriosInventory;
import org.bg52.synthetix.api.SynthetixAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry storing all configured accessory slot definitions loaded from slots.yml,
 * and tracking which slots are currently active (have registered items or equipped items).
 */
public class SlotRegistry {

    private final CuriosPaper plugin;
    private final Map<String, SlotDefinition> slots = new LinkedHashMap<>();
    private final Set<String> activeSlotTypes = ConcurrentHashMap.newKeySet();

    public SlotRegistry(CuriosPaper plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        slots.clear();
        activeSlotTypes.clear();

        File slotsFile = new File(plugin.getDataFolder(), "slots.yml");
        if (!slotsFile.exists()) {
            plugin.saveResource("slots.yml", false);
        }
        FileConfiguration slotsConfig = YamlConfiguration.loadConfiguration(slotsFile);
        ConfigurationSection root = slotsConfig.getConfigurationSection("slots");

        if (root == null) {
            plugin.getLogger().warning("No 'slots' section found in slots.yml!");
            return;
        }

        int defaultGuiSlot = 10;
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) continue;

            String name = sec.getString("name", key);
            String iconName = sec.getString("icon", "ITEM_FRAME");
            Material icon = Material.matchMaterial(iconName.toUpperCase());
            if (icon == null) {
                if (iconName.equalsIgnoreCase("IRON_CHAIN")) {
                    icon = Material.matchMaterial("CHAIN");
                }
                if (icon == null) {
                    icon = Material.ITEM_FRAME;
                }
            }

            String itemModel = sec.getString("item-model", null);
            Integer customModelData = sec.contains("custom-model-data") ? sec.getInt("custom-model-data") : null;

            int amount = sec.getInt("amount", sec.getInt("default-unlocked", 1));
            int maxSlots = sec.getInt("max-slots", amount);
            int guiSlot = sec.getInt("gui-slot", defaultGuiSlot++);
            String permission = sec.getString("permission", "curiospaper.slot." + key.toLowerCase(Locale.ROOT));

            List<String> description = sec.getStringList("lore");
            if (description.isEmpty()) {
                description = sec.getStringList("description");
            }

            Float pitchUpLimit = sec.contains("pitch-up-limit") ? (float) sec.getDouble("pitch-up-limit") : null;
            Float pitchDownLimit = sec.contains("pitch-down-limit") ? (float) sec.getDouble("pitch-down-limit") : null;

            SlotDefinition def = new SlotDefinition(
                    key, name, icon, itemModel, customModelData, guiSlot, amount, maxSlots, permission, description,
                    pitchUpLimit, pitchDownLimit
            );
            slots.put(def.getId(), def);
        }

        plugin.getLogger().info("Loaded " + slots.size() + " accessory slot definitions.");
        refreshActiveSlots();
    }

    /**
     * Refreshes active slot types by inspecting Synthetix and active item engines.
     */
    public void refreshActiveSlots() {
        // 1. Back slot active if elytra flight feature is enabled
        if (plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", true)) {
            activeSlotTypes.add("back");
        }

        // 2. Query ItemEngineManager for all registered slots from adapters (Synthetix, Nexo, etc.)
        if (plugin.getItemEngineManager() != null) {
            Set<String> engineSlots = plugin.getItemEngineManager().getRegisteredSlotTypes();
            for (String s : engineSlots) {
                if (s == null) continue;
                String normalized = s.trim().toLowerCase(Locale.ROOT);
                if (hasSlot(normalized)) {
                    activeSlotTypes.add(normalized);
                } else {
                    for (SlotDefinition slotDef : slots.values()) {
                        if (slotDef.getName().equalsIgnoreCase(s.trim())) {
                            activeSlotTypes.add(slotDef.getId());
                        }
                    }
                }
            }
        }

        // 3. Synthetix Item Definitions (direct verification)
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Synthetix") && SynthetixAPI.getOptional().isPresent()) {
                org.bg52.synthetix.item.ItemRegistry registry = SynthetixAPI.get().getItemRegistry();
                if (registry != null) {
                    for (org.bg52.synthetix.item.ItemDefinition def : registry.getAll()) {
                        if (def.getTags() != null) {
                            String slot = def.getTag("curiospaper:slot_type");
                            if (slot == null) slot = def.getTag("curios_slot");
                            if (slot == null) slot = def.getTag("curios.slot");
                            if (slot == null) slot = def.getTag("slot");
                            if (slot == null) slot = def.getTag("curiospaper:slot");
                            if (slot != null && !slot.trim().isEmpty()) {
                                for (String s : slot.split("[,;\\s]+")) {
                                    String norm = s.trim().toLowerCase(Locale.ROOT);
                                    if (hasSlot(norm)) {
                                        activeSlotTypes.add(norm);
                                    } else {
                                        for (SlotDefinition slotDef : slots.values()) {
                                            if (slotDef.getName().equalsIgnoreCase(norm)) {
                                                activeSlotTypes.add(slotDef.getId());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Check lore
                        if (def.getLore() != null) {
                            for (String line : def.getLore()) {
                                String plain = ChatColor.stripColor(line).trim().toLowerCase(Locale.ROOT);
                                if (plain.startsWith("slot:")) {
                                    String s = plain.substring(5).trim();
                                    for (SlotDefinition slotDef : slots.values()) {
                                        if (slotDef.getId().equalsIgnoreCase(s) || slotDef.getName().equalsIgnoreCase(s)) {
                                            activeSlotTypes.add(slotDef.getId());
                                        }
                                    }
                                } else if (plain.startsWith("required slot:")) {
                                    String s = plain.substring(14).trim();
                                    for (SlotDefinition slotDef : slots.values()) {
                                        if (slotDef.getId().equalsIgnoreCase(s) || slotDef.getName().equalsIgnoreCase(s)) {
                                            activeSlotTypes.add(slotDef.getId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 4. Mappings in config.yml
        ConfigurationSection mappingsSec = plugin.getConfig().getConfigurationSection("item-engines.mappings");
        if (mappingsSec != null) {
            for (String key : mappingsSec.getKeys(false)) {
                String slot = mappingsSec.getString(key);
                if (slot != null && hasSlot(slot)) {
                    activeSlotTypes.add(slot.toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    public Set<String> getActiveSlotIds() {
        return Collections.unmodifiableSet(activeSlotTypes);
    }

    public void markSlotActive(String slotId) {
        if (slotId != null && hasSlot(slotId)) {
            activeSlotTypes.add(slotId.toLowerCase(Locale.ROOT));
        }
    }

    public boolean isSlotActive(String slotId, Player player) {
        if (slotId == null) return false;
        String normalized = slotId.toLowerCase(Locale.ROOT);
        if (activeSlotTypes.contains(normalized)) {
            return true;
        }
        if (player != null && plugin.getStorageManager() != null) {
            PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
            if (inv != null && inv.hasAnyItem(normalized)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the active slot keys for a given player in configuration insertion order.
     */
    public List<String> getActiveSlotKeysFor(Player player) {
        boolean onlyActive = plugin.getConfig().getBoolean("gui.only-show-active-slots", true);
        List<String> list = new ArrayList<>();
        PlayerCuriosInventory inv = (player != null && plugin.getStorageManager() != null)
                ? plugin.getStorageManager().getInventory(player.getUniqueId()) : null;

        for (SlotDefinition def : slots.values()) {
            if (!onlyActive) {
                list.add(def.getId());
                continue;
            }
            if (activeSlotTypes.contains(def.getId())) {
                list.add(def.getId());
                continue;
            }
            if (inv != null && inv.hasAnyItem(def.getId())) {
                list.add(def.getId());
            }
        }

        // Safety fallback if no items are registered yet
        if (list.isEmpty()) {
            list.addAll(slots.keySet());
        }

        return list;
    }

    public SlotDefinition getSlot(String id) {
        if (id == null) return null;
        return slots.get(id.toLowerCase(Locale.ROOT));
    }

    public void registerSlot(SlotDefinition def) {
        if (def == null || def.getId() == null) return;
        slots.put(def.getId().toLowerCase(Locale.ROOT), def);
        activeSlotTypes.add(def.getId().toLowerCase(Locale.ROOT));
    }

    public boolean hasSlot(String id) {
        if (id == null) return false;
        return slots.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<SlotDefinition> getAllSlots() {
        return Collections.unmodifiableCollection(slots.values());
    }

    public Set<String> getSlotIds() {
        return Collections.unmodifiableSet(slots.keySet());
    }
}
