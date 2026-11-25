package org.bg52.curiospaper.engine;

import net.kyori.adventure.text.Component;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.engine.adapter.*;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bg52.curiospaper.util.TextUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Universal Item Engine coordinator bridging CuriosPaper with Synthetix, Nexo, ItemsAdder, Oraxen, and CraftEngine.
 */
public class ItemEngineManager {

    private final CuriosPaper plugin;
    private final Map<String, EngineAdapter> adapters = new LinkedHashMap<>();
    private final NamespacedKey slotTypeKey;
    private final NamespacedKey itemIdKey;

    public ItemEngineManager(CuriosPaper plugin) {
        this.plugin = plugin;
        this.slotTypeKey = new NamespacedKey(plugin, "slot_type");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    public void initialize() {
        adapters.clear();

        // 1. Synthetix (Primary Core Engine)
        registerAdapter(new SynthetixAdapter(plugin));

        // 2. External Item Engines (Nexo, ItemsAdder, Oraxen, CraftEngine)
        registerAdapter(new NexoAdapter(plugin));
        registerAdapter(new ItemsAdderAdapter(plugin));
        registerAdapter(new OraxenAdapter(plugin));
        registerAdapter(new CraftEngineAdapter(plugin));

        // Initialize all registered adapters
        for (EngineAdapter adapter : adapters.values()) {
            adapter.initialize();
        }

        // Warn if no item creation engine is present, reminding the user that CuriosPaper no longer creates its own items
        boolean hasAnyEngine = adapters.values().stream().anyMatch(EngineAdapter::isAvailable);
        if (!hasAnyEngine) {
            plugin.getLogger().warning("==================================================================");
            plugin.getLogger().warning("Notice: Synthetix or other item creation plugin not found.");
            plugin.getLogger().warning("CuriosPaper 3.0 does not create its own items or host resource packs.");
            plugin.getLogger().warning("Accessories can still be configured via /curios tag <slot>,");
            plugin.getLogger().warning("via slots.yml, or by installing Synthetix/Nexo/ItemsAdder/Oraxen/CraftEngine.");
            plugin.getLogger().warning("==================================================================");
        }
    }

    private void registerAdapter(EngineAdapter adapter) {
        adapters.put(adapter.getEngineId(), adapter);
    }

    public void shutdown() {
        for (EngineAdapter adapter : adapters.values()) {
            adapter.shutdown();
        }
        adapters.clear();
    }

    public NamespacedKey getSlotTypeKey() {
        return slotTypeKey;
    }

    public NamespacedKey getItemIdKey() {
        return itemIdKey;
    }

    /**
     * Resolves the target Curios slot type for an ItemStack.
     * Evaluates:
     * 1. Direct PDC tag ("curiospaper:slot_type")
     * 2. Adapter-specific slot resolution (Synthetix tag, Nexo/Oraxen/IA/CraftEngine curios section)
     * 3. Lore inspection for "Slot: <Name>"
     * 4. Vanilla Elytra -> "back" (if feature enabled)
     */
    public String resolveCuriosSlot(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 1. Check direct CuriosPaper PDC tag
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(slotTypeKey, PersistentDataType.STRING)) {
                String slot = pdc.get(slotTypeKey, PersistentDataType.STRING);
                if (slot != null && plugin.getSlotRegistry().hasSlot(slot)) {
                    return slot.toLowerCase(Locale.ROOT);
                }
            }
        }

        // 2. Check each active item engine adapter
        for (EngineAdapter adapter : adapters.values()) {
            if (adapter.isAvailable()) {
                String slot = adapter.resolveCuriosSlot(item);
                if (slot != null && plugin.getSlotRegistry().hasSlot(slot)) {
                    return slot.toLowerCase(Locale.ROOT);
                }
            }
        }

        // 3. Check Lore for "Slot: <SlotName>"
        if (meta != null && meta.hasLore()) {
            List<Component> lore = meta.lore();
            if (lore != null) {
                for (Component lineComp : lore) {
                    String plain = TextUtil.toLegacy(lineComp);
                    String stripped = ChatColor.stripColor(plain).trim().toLowerCase(Locale.ROOT);
                    if (stripped.startsWith("slot:")) {
                        String slotCandidate = stripped.substring(5).trim();
                        for (SlotDefinition slotDef : plugin.getSlotRegistry().getAllSlots()) {
                            String defNameStripped = ChatColor.stripColor(TextUtil.toLegacy(slotDef.getNameComponent())).toLowerCase(Locale.ROOT);
                            if (slotCandidate.equalsIgnoreCase(slotDef.getId()) || slotCandidate.equalsIgnoreCase(defNameStripped)) {
                                return slotDef.getId();
                            }
                        }
                    }
                }
            }
        }

        // 4. Vanilla Elytra check
        if (item.getType() == Material.ELYTRA && plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", true)) {
            if (plugin.getSlotRegistry().hasSlot("back")) {
                return "back";
            }
        }

        return null;
    }

    /**
     * Tags an item for a specific Curios slot.
     */
    public ItemStack tagAccessoryItem(ItemStack item, String slotType) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        SlotDefinition slotDef = plugin.getSlotRegistry().getSlot(slotType);
        if (slotDef == null) {
            return item;
        }

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return clone;
        }

        // 1. Set PDC
        meta.getPersistentDataContainer().set(slotTypeKey, PersistentDataType.STRING, slotDef.getId());

        // 2. Add or update Slot Lore
        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        boolean hasSlotLine = false;
        String rawSlotFormat = plugin.getLanguageManager().get("items.slot-lore", "slot", slotDef.getName());
        Component slotLine = TextUtil.parse(rawSlotFormat);

        for (int i = 0; i < lore.size(); i++) {
            String plain = TextUtil.stripAll(TextUtil.toLegacy(lore.get(i))).toLowerCase(Locale.ROOT);
            if (plain.startsWith("slot:") || plain.startsWith("required slot:")) {
                lore.set(i, slotLine);
                hasSlotLine = true;
                break;
            }
        }

        if (!hasSlotLine) {
            lore.add(slotLine);
        }

        // 3. Quick equip lore line if enabled
        if (plugin.getConfig().getBoolean("features.quick-equip-right-click", true)) {
            String qeFormat = plugin.getLanguageManager().get("items.quick-equip-lore");
            if (qeFormat != null && !qeFormat.trim().isEmpty()) {
                Component qeLine = TextUtil.parse(qeFormat);
                boolean hasQeLine = false;
                for (Component line : lore) {
                    String plain = TextUtil.stripAll(TextUtil.toLegacy(line)).toLowerCase(Locale.ROOT);
                    if (plain.contains("shift + right click") || plain.contains("quick to equip") || plain.contains("click to equip")) {
                        hasQeLine = true;
                        break;
                    }
                }
                if (!hasQeLine) {
                    lore.add(qeLine);
                }
            }
        }

        meta.lore(lore);
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * Removes the Curios accessory tag from an item.
     */
    public ItemStack untagAccessoryItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return item;
        }

        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) {
            return clone;
        }

        meta.getPersistentDataContainer().remove(slotTypeKey);

        if (meta.hasLore() && meta.lore() != null) {
            List<Component> lore = new ArrayList<>(meta.lore());
            lore.removeIf(line -> {
                String plain = TextUtil.stripAll(TextUtil.toLegacy(line)).toLowerCase(Locale.ROOT);
                return plain.startsWith("slot:") || plain.startsWith("required slot:") ||
                       plain.contains("shift + right click") || plain.contains("quick to equip") || plain.contains("click to equip");
            });
            meta.lore(lore);
        }

        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * Resolves the custom item ID from an item stack.
     */
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        // 1. Check native PDC
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(itemIdKey, PersistentDataType.STRING)) {
            return pdc.get(itemIdKey, PersistentDataType.STRING);
        }

        // 2. Check adapters
        for (EngineAdapter adapter : adapters.values()) {
            if (adapter.isAvailable()) {
                String id = adapter.getItemId(item);
                if (id != null) return id;
            }
        }

        return null;
    }

    /**
     * Creates an ItemStack from a custom item ID across engines.
     */
    public ItemStack createItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;

        // Check namespace prefix if specified (e.g. "synthetix:ruby_ring", "nexo:dagger")
        if (itemId.contains(":")) {
            String[] parts = itemId.split(":", 2);
            EngineAdapter adapter = adapters.get(parts[0].toLowerCase(Locale.ROOT));
            if (adapter != null && adapter.isAvailable()) {
                ItemStack stack = adapter.createItemStack(parts[1]);
                if (stack != null) return stack;
            }
        }

        // Try adapters in priority order
        for (EngineAdapter adapter : adapters.values()) {
            if (adapter.isAvailable()) {
                ItemStack stack = adapter.createItemStack(itemId);
                if (stack != null) return stack;
            }
        }

        return null;
    }

    /**
     * Checks if the item is recognized as a custom item by any active engine.
     */
    public boolean isCustomItem(ItemStack item) {
        return getItemId(item) != null;
    }

    /**
     * Queries all active engine adapters to retrieve all accessory slot IDs
     * that currently have registered custom items.
     */
    public Set<String> getRegisteredSlotTypes() {
        Set<String> active = new HashSet<>();
        for (EngineAdapter adapter : adapters.values()) {
            if (adapter.isAvailable()) {
                active.addAll(adapter.getRegisteredSlotTypes());
            }
        }
        return active;
    }

    /**
     * Notifies engines when an item is equipped.
     */
    public void notifyEquip(Player player, ItemStack item) {
        for (EngineAdapter adapter : adapters.values()) {
            if (adapter.isAvailable()) {
                adapter.onEquip(player, item);
            }
        }
    }

    /**
     * Notifies engines when an item is unequipped.
     */
    public void notifyUnequip(Player player, ItemStack item) {
        for (EngineAdapter adapter : adapters.values()) {
            if (adapter.isAvailable()) {
                adapter.onUnequip(player, item);
            }
        }
    }

    public EngineAdapter getAdapter(String engineId) {
        return adapters.get(engineId.toLowerCase(Locale.ROOT));
    }

    /**
     * Checks if any active engine indicates that this item should render a 3D model.
     */
    public boolean shouldRenderModel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        for (EngineAdapter adapter : adapters.values()) {
            if (adapter.isAvailable() && adapter.shouldRenderModel(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets separate 3D model properties directly into an ItemStack's persistent data container.
     */
    public ItemStack set3DModel(ItemStack item, Material modelMat, Integer cmd, String itemModel, Float pitchUp, Float pitchDown) {
        if (item == null || item.getType() == Material.AIR) return item;
        ItemStack clone = item.clone();
        ItemMeta meta = clone.getItemMeta();
        if (meta == null) return clone;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (modelMat != null) {
            pdc.set(new NamespacedKey(plugin, "model_item"), PersistentDataType.STRING, modelMat.name());
        } else {
            pdc.remove(new NamespacedKey(plugin, "model_item"));
        }

        if (cmd != null) {
            pdc.set(new NamespacedKey(plugin, "model_cmd"), PersistentDataType.STRING, String.valueOf(cmd));
        } else {
            pdc.remove(new NamespacedKey(plugin, "model_cmd"));
        }

        if (itemModel != null && !itemModel.isEmpty()) {
            pdc.set(new NamespacedKey(plugin, "model_item_model"), PersistentDataType.STRING, itemModel);
        } else {
            pdc.remove(new NamespacedKey(plugin, "model_item_model"));
        }

        if (pitchUp != null) {
            pdc.set(new NamespacedKey(plugin, "pitch_up_limit"), PersistentDataType.STRING, String.valueOf(pitchUp));
        } else {
            pdc.remove(new NamespacedKey(plugin, "pitch_up_limit"));
        }

        if (pitchDown != null) {
            pdc.set(new NamespacedKey(plugin, "pitch_down_limit"), PersistentDataType.STRING, String.valueOf(pitchDown));
        } else {
            pdc.remove(new NamespacedKey(plugin, "pitch_down_limit"));
        }

        clone.setItemMeta(meta);
        return clone;
    }
}

