package org.bg52.curiospaper.engine.adapter;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.engine.EngineAdapter;
import org.bg52.synthetix.api.SynthetixAPI;
import org.bg52.synthetix.item.ItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Adapter integrating CuriosPaper directly with the Synthetix Core Plugin.
 */
public class SynthetixAdapter implements EngineAdapter {

    private final CuriosPaper plugin;
    private boolean available = false;

    public SynthetixAdapter(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getEngineId() {
        return "synthetix";
    }

    @Override
    public boolean isAvailable() {
        if (!available) {
            try {
                available = Bukkit.getPluginManager().isPluginEnabled("Synthetix") && SynthetixAPI.getOptional().isPresent();
                if (available) {
                    hookSetManager();
                }
            } catch (Throwable ignored) {
                available = false;
            }
        }
        return available;
    }

    @Override
    public void initialize() {
        try {
            available = Bukkit.getPluginManager().isPluginEnabled("Synthetix") && SynthetixAPI.getOptional().isPresent();
            if (available) {
                plugin.getLogger().info("[ItemEngine] Successfully linked with Synthetix Core Engine!");
                hookSetManager();
            }
        } catch (Throwable t) {
            available = false;
        }
    }

    private void hookSetManager() {
        try {
            org.bg52.synthetix.SynthetixPlugin synth = org.bg52.synthetix.SynthetixPlugin.get();
            if (synth != null && synth.getSetManager() != null) {
                synth.getSetManager().registerEquipmentProvider(player -> {
                    org.bg52.curiospaper.inventory.PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
                    if (inv == null) return java.util.Collections.emptyList();
                    java.util.List<ItemStack> list = new java.util.ArrayList<>();
                    for (java.util.List<ItemStack> items : inv.getAllSlots().values()) {
                        if (items != null) {
                            for (ItemStack it : items) {
                                if (it != null && !it.getType().isAir()) {
                                    list.add(it);
                                }
                            }
                        }
                    }
                    return list;
                });
                plugin.getLogger().info("[SynthetixAdapter] Hooked Curios slots into Synthetix SetManager!");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[SynthetixAdapter] Could not hook into SetManager: " + t.getMessage());
        }
    }

    @Override
    public void shutdown() {
        available = false;
    }

    @Override
    public String getItemId(ItemStack item) {
        if (!available || item == null) return null;
        try {
            return SynthetixAPI.get().getItemId(item);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public ItemStack createItemStack(String itemId) {
        if (!available || itemId == null) return null;
        try {
            return SynthetixAPI.get().createItemStack(itemId);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public boolean isCustomItem(ItemStack item) {
        if (!available || item == null) return false;
        try {
            return SynthetixAPI.get().isCustomItem(item);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public java.util.Set<String> getRegisteredSlotTypes() {
        java.util.Set<String> result = new java.util.HashSet<>();
        if (!available) return result;
        try {
            if (SynthetixAPI.getOptional().isPresent()) {
                org.bg52.synthetix.item.ItemRegistry registry = SynthetixAPI.get().getItemRegistry();
                if (registry != null) {
                    for (ItemDefinition def : registry.getAll()) {
                        if (def.getTags() != null) {
                            String slot = def.getTag("curiospaper:slot_type");
                            if (slot == null) slot = def.getTag("curios_slot");
                            if (slot == null) slot = def.getTag("curios.slot");
                            if (slot == null) slot = def.getTag("slot");
                            if (slot == null) slot = def.getTag("curiospaper:slot");
                            if (slot != null && !slot.trim().isEmpty()) {
                                for (String s : slot.split("[,;\\s]+")) {
                                    if (!s.trim().isEmpty()) {
                                        result.add(s.trim().toLowerCase(java.util.Locale.ROOT));
                                    }
                                }
                            }
                        }
                        if (def.getLore() != null) {
                            for (String line : def.getLore()) {
                                String plain = org.bukkit.ChatColor.stripColor(line).trim().toLowerCase(java.util.Locale.ROOT);
                                if (plain.startsWith("slot:")) {
                                    String s = plain.substring(5).trim();
                                    if (!s.isEmpty()) result.add(s);
                                } else if (plain.startsWith("required slot:")) {
                                    String s = plain.substring(14).trim();
                                    if (!s.isEmpty()) result.add(s);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    @Override
    public String resolveCuriosSlot(ItemStack item) {
        if (!available || item == null) return null;
        try {
            String itemId = getItemId(item);
            if (itemId == null) return null;

            ItemDefinition def = SynthetixAPI.get().getItemRegistry().get(itemId);
            if (def == null || def.getTags() == null) return null;

            // Check custom tags or properties
            if (def.getTags().containsKey("curios_slot")) {
                return def.getTags().get("curios_slot");
            }
            if (def.getTags().containsKey("curios.slot")) {
                return def.getTags().get("curios.slot");
            }
            if (def.getTags().containsKey("slot")) {
                return def.getTags().get("slot");
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Override
    public boolean shouldRenderModel(ItemStack item) {
        if (!available || item == null) return false;
        try {
            String itemId = getItemId(item);
            if (itemId == null) return false;

            ItemDefinition def = SynthetixAPI.get().getItemRegistry().get(itemId);
            if (def == null || def.getTags() == null) return false;

            String renderTag = def.getTag("curiospaper:render_model");
            if ("true".equalsIgnoreCase(renderTag)) {
                return true;
            }
            if ("false".equalsIgnoreCase(renderTag)) {
                return false;
            }

            // Check if separate 3D model properties are set in definition
            String sMat = def.getTag("curiospaper:model_item");
            if (sMat != null && !sMat.trim().isEmpty() && !sMat.equalsIgnoreCase("DEFAULT")) {
                return true;
            }
            String sCmd = def.getTag("curiospaper:model_cmd");
            if (sCmd != null && !sCmd.trim().isEmpty() && !sCmd.equalsIgnoreCase("DEFAULT")) {
                return true;
            }
            String sMod = def.getTag("curiospaper:model_item_model");
            if (sMod != null && !sMod.trim().isEmpty() && !sMod.equalsIgnoreCase("DEFAULT")) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    @Override
    public void onEquip(Player player, ItemStack item) {
        if (!available || item == null) return;
        try {
            SynthetixAPI.get().getTriggerManager().fireEquip(player, item);
            org.bg52.synthetix.SynthetixPlugin synth = org.bg52.synthetix.SynthetixPlugin.get();
            if (synth != null && synth.getSetManager() != null) {
                synth.getSetManager().evaluatePlayerSets(player);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onUnequip(Player player, ItemStack item) {
        if (!available || item == null) return;
        try {
            SynthetixAPI.get().getTriggerManager().fireUnequip(player, item);
            org.bg52.synthetix.SynthetixPlugin synth = org.bg52.synthetix.SynthetixPlugin.get();
            if (synth != null && synth.getSetManager() != null) {
                synth.getSetManager().evaluatePlayerSets(player);
            }
        } catch (Throwable ignored) {}
    }
}
