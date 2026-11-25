package org.bg52.curiospaper.engine.synthetix;

import net.kyori.adventure.text.Component;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bg52.curiospaper.util.TextUtil;
import org.bg52.curiospaper.util.VersionUtil;
import org.bg52.synthetix.SynthetixPlugin;
import org.bg52.synthetix.api.editor.ItemEditorCategory;
import org.bg52.synthetix.gui.Canvas;
import org.bg52.synthetix.gui.CanvasButton;
import org.bg52.synthetix.gui.editor.BaseEditorCanvas;
import org.bg52.synthetix.item.ItemDefinition;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Synthetix Item Editor Extension providing dedicated Curios accessory settings
 * directly inside Synthetix's in-game item editor (/synth edit <item>).
 */
public class CuriosItemEditorCategory implements ItemEditorCategory {

    private final CuriosPaper plugin;

    public CuriosItemEditorCategory(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "curios_accessory";
    }

    @Override
    public Component getDisplayName() {
        return TextUtil.parse("<gold><bold>Curios Accessory</bold></gold>");
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean isPluginCategory() {
        return true;
    }

    @Override
    public ItemStack getIcon(ItemDefinition def) {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(getDisplayName());
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Configure accessory slot and 3D model rendering</gray>"));
            lore.add(TextUtil.parse("<gray>when worn in Curios slots.</gray>"));
            lore.add(Component.empty());

            String currentSlot = def.getTag("curiospaper:slot_type");
            boolean isCurios = currentSlot != null && !currentSlot.trim().isEmpty();

            if (isCurios) {
                SlotDefinition slotDef = plugin.getSlotRegistry().getSlot(currentSlot);
                String slotName = slotDef != null ? slotDef.getName() : currentSlot;
                lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Curios Status:</gray> <green><bold>Enabled</bold></green>"));
                lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Assigned Slot:</gray> <gold><bold>" + slotName + "</bold></gold>"));

                String renderModel = def.getTag("curiospaper:render_model");
                boolean renderOn = !"false".equalsIgnoreCase(renderModel);
                lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>3D Model:</gray> " + (renderOn ? "<green>Enabled</green>" : "<red>Disabled</red>")));

                String modelItem = def.getTag("curiospaper:model_item");
                if (modelItem != null && !modelItem.isEmpty()) {
                    lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>3D Model Mat:</gray> <white>" + modelItem + "</white>"));
                }
                String modelCmd = def.getTag("curiospaper:model_cmd");
                if (modelCmd != null && !modelCmd.isEmpty()) {
                    lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>3D Model CMD:</gray> <white>" + modelCmd + "</white>"));
                }
                String modelItemModel = def.getTag("curiospaper:model_item_model");
                if (modelItemModel != null && !modelItemModel.isEmpty()) {
                    lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>3D Item Model:</gray> <white>" + modelItemModel + "</white>"));
                }

                boolean hasSlotLore = hasSlotLoreLine(def);
                lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Slot Lore:</gray> " + (hasSlotLore ? "<green>Enabled</green>" : "<red>Disabled</red>")));
            } else {
                lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Curios Status:</gray> <red><bold>Disabled</bold></red>"));
                lore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Assigned Slot:</gray> <dark_gray>None (Click to configure)</dark_gray>"));
            }

            lore.add(Component.empty());
            lore.add(TextUtil.parse("<yellow>Click to configure Curios settings »</yellow>"));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void open(Player player, ItemDefinition def, Runnable onBack) {
        SynthetixPlugin synth = (SynthetixPlugin) Bukkit.getPluginManager().getPlugin("Synthetix");
        if (synth == null) {
            player.sendMessage(TextUtil.parse("<red>Synthetix is not available.</red>"));
            return;
        }

        Component title = TextUtil.parse("<gold><bold>Curios Accessory</bold></gold> <dark_gray>»</dark_gray> <yellow>" + def.getId() + "</yellow>");
        BaseEditorCanvas base = new BaseEditorCanvas(synth, def, title, 6);
        base.setupSideBorders();

        String currentSlot = def.getTag("curiospaper:slot_type");
        if (currentSlot == null) currentSlot = "";

        // Ensure lore line is enabled by default when curios slot is configured
        if (!currentSlot.isEmpty() && !hasSlotLoreLine(def)) {
            SlotDefinition sd = plugin.getSlotRegistry().getSlot(currentSlot);
            updateSlotLore(def, sd != null ? sd.getName() : currentSlot);
        }

        // Row 2 & 3: Slot Buttons (Slots 10..16, 19..25)
        int[] displaySlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int idx = 0;
        for (SlotDefinition slotDef : plugin.getSlotRegistry().getAllSlots()) {
            if (idx >= displaySlots.length) break;
            int guiSlot = displaySlots[idx++];

            boolean isCurrent = currentSlot.equalsIgnoreCase(slotDef.getId());
            ItemStack btn = new ItemStack(slotDef.getIcon());
            ItemMeta meta = btn.getItemMeta();
            if (meta != null) {
                meta.displayName(TextUtil.parse("<gold><bold>" + slotDef.getName() + " Slot</bold></gold>"));
                List<Component> lore = new ArrayList<>();
                for (String desc : slotDef.getDescription()) {
                    lore.add(TextUtil.parse(desc));
                }
                lore.add(Component.empty());
                lore.add(TextUtil.parse("<gray>Capacity:</gray> <white>" + slotDef.getMaxSlots() + " slots</white>"));
                lore.add(Component.empty());

                if (isCurrent) {
                    lore.add(TextUtil.parse("<green><bold>✔ CURRENTLY ASSIGNED</bold></green>"));
                    lore.add(TextUtil.parse("<yellow>Click to remove slot assignment</yellow>"));
                } else {
                    lore.add(TextUtil.parse("<dark_gray>○ Click to assign item to " + slotDef.getName() + "</dark_gray>"));
                }
                meta.lore(lore);

                if (slotDef.getItemModel() != null || slotDef.getCustomModelData() != null) {
                    VersionUtil.setItemModelSafe(meta, slotDef.getItemModel(), slotDef.getCustomModelData());
                }

                btn.setItemMeta(meta);
            }

            final String sId = slotDef.getId();
            final String sName = slotDef.getName();
            base.getCanvas().setButton(guiSlot, CanvasButton.of(btn, e -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                if (sId.equalsIgnoreCase(def.getTag("curiospaper:slot_type"))) {
                    // Unassign
                    def.setTag("curiospaper:slot_type", null);
                    def.setTag("curiospaper:render_model", null);
                    removeSlotLore(def);
                } else {
                    // Assign
                    def.setTag("curiospaper:slot_type", sId);
                    updateSlotLore(def, sName);
                    plugin.getSlotRegistry().markSlotActive(sId);
                }
                open(player, def, onBack);
            }));
        }

        // =========================================================================
        // ROW 4: Special Curios Controls (Slots 29, 31, 33)
        // =========================================================================

        // Slot 29: Dedicated 3D Model Settings
        String modelTag = def.getTag("curiospaper:render_model");
        boolean isCurios = !currentSlot.isEmpty();
        boolean modelEnabled = isCurios && "true".equalsIgnoreCase(modelTag);
        ItemStack modelBtn = new ItemStack(Material.ARMOR_STAND);
        ItemMeta mMeta = modelBtn.getItemMeta();
        if (mMeta != null) {
            mMeta.displayName(TextUtil.parse("<yellow><bold>3D Model Settings »</bold></yellow>"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Configure separate 3D models, custom</gray>"));
            lore.add(TextUtil.parse("<gray>materials, model data, and pitch limits.</gray>"));
            lore.add(Component.empty());
            if (!isCurios) {
                lore.add(TextUtil.parse("<gray>Status:</gray> <dark_gray>Disabled (Assign a slot first)</dark_gray>"));
            } else {
                lore.add(TextUtil.parse("<gray>Status:</gray> " + (modelEnabled ? "<green>Enabled</green>" : "<red>Disabled</red>")));
            }

            String sMat = def.getTag("curiospaper:model_item");
            lore.add(TextUtil.parse("<gray>Model Material:</gray> <white>" + (sMat != null && !sMat.isEmpty() ? sMat : "Default (Base Item)") + "</white>"));

            String sCmd = def.getTag("curiospaper:model_cmd");
            lore.add(TextUtil.parse("<gray>Custom Model Data:</gray> <white>" + (sCmd != null && !sCmd.isEmpty() ? sCmd : "None") + "</white>"));

            String sMod = def.getTag("curiospaper:model_item_model");
            lore.add(TextUtil.parse("<gray>Item Model (1.21.4+):</gray> <white>" + (sMod != null && !sMod.isEmpty() ? sMod : "None") + "</white>"));

            String up = def.getTag("curiospaper:pitch_up_limit");
            String down = def.getTag("curiospaper:pitch_down_limit");
            lore.add(TextUtil.parse("<gray>Pitch Limits:</gray> <white>Up: " + (up != null ? up + "°" : "None") + ", Down: " + (down != null ? down + "°" : "None") + "</white>"));

            lore.add(Component.empty());
            lore.add(TextUtil.parse("<yellow>Left-Click: Open 3D Model Editor »</yellow>"));
            lore.add(TextUtil.parse("<gray>Right-Click: Quick toggle enabled/disabled</gray>"));
            mMeta.lore(lore);
            modelBtn.setItemMeta(mMeta);
        }
        base.getCanvas().setButton(29, CanvasButton.of(modelBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            if (e.isRightClick()) {
                def.setTag("curiospaper:render_model", modelEnabled ? "false" : "true");
                open(player, def, onBack);
            } else {
                open3DModelSettings(player, synth, def, () -> open(player, def, onBack));
            }
        }));

        // Slot 31: Auto Lore Line Toggle
        boolean hasLore = hasSlotLoreLine(def);
        ItemStack loreBtn = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta lMeta = loreBtn.getItemMeta();
        if (lMeta != null) {
            lMeta.displayName(TextUtil.parse("<yellow><bold>Slot Lore Line</bold></yellow>"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Adds or removes visual 'Slot: <Type>'</gray>"));
            lore.add(TextUtil.parse("<gray>tooltip on the item lore.</gray>"));
            lore.add(Component.empty());
            lore.add(TextUtil.parse("<gray>Status:</gray> " + (hasLore ? "<green>Enabled</green>" : "<red>Disabled</red>")));
            lore.add(Component.empty());
            lore.add(TextUtil.parse("<yellow>Click to toggle lore line »</yellow>"));
            lMeta.lore(lore);
            loreBtn.setItemMeta(lMeta);
        }
        base.getCanvas().setButton(31, CanvasButton.of(loreBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            String assigned = def.getTag("curiospaper:slot_type");
            if (hasSlotLoreLine(def)) {
                removeSlotLore(def);
            } else if (assigned != null && !assigned.isEmpty()) {
                SlotDefinition sd = plugin.getSlotRegistry().getSlot(assigned);
                updateSlotLore(def, sd != null ? sd.getName() : assigned);
            } else {
                player.sendMessage(TextUtil.parse("<yellow>[CuriosPaper] Assign an accessory slot first to add the slot lore line.</yellow>"));
            }
            open(player, def, onBack);
        }));

        // Slot 33: Clear All Curios Tags
        ItemStack clearBtn = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = clearBtn.getItemMeta();
        if (cMeta != null) {
            cMeta.displayName(TextUtil.parse("<red><bold>Clear Curios Status</bold></red>"));
            List<Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Removes Curios accessory tagging</gray>"));
            lore.add(TextUtil.parse("<gray>and reverts item to standard item.</gray>"));
            lore.add(Component.empty());
            lore.add(TextUtil.parse("<red>Click to clear »</red>"));
            cMeta.lore(lore);
            clearBtn.setItemMeta(cMeta);
        }
        base.getCanvas().setButton(33, CanvasButton.of(clearBtn, e -> {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
            def.setTag("curiospaper:slot_type", null);
            removeSlotLore(def);
            open(player, def, onBack);
        }));

        base.setBackButton(onBack);
        base.setSaveButton();
        base.open(player);
    }

    public void open3DModelSettings(Player player, SynthetixPlugin synth, ItemDefinition def, Runnable onBack) {
        Component title = TextUtil.parse("<gold><bold>3D Model Settings</bold></gold> <dark_gray>»</dark_gray> <yellow>" + def.getId() + "</yellow>");
        BaseEditorCanvas canvas = new BaseEditorCanvas(synth, def, title, 6);
        canvas.setupSideBorders();

        // 1. Header Preview Item (Slot 4)
        String modelMatStr = def.getTag("curiospaper:model_item");
        Material previewMat = null;
        if (modelMatStr != null && !modelMatStr.isEmpty() && !modelMatStr.equalsIgnoreCase("DEFAULT")) {
            previewMat = Material.matchMaterial(modelMatStr.toUpperCase(Locale.ROOT));
        }
        if (previewMat == null) previewMat = def.getMaterial();

        ItemStack preview = new ItemStack(previewMat != null ? previewMat : Material.PAPER);
        ItemMeta pMeta = preview.getItemMeta();
        if (pMeta != null) {
            pMeta.displayName(TextUtil.parse("<gold><bold>3D Model Preview</bold></gold>"));
            List<Component> pLore = new ArrayList<>();
            pLore.add(TextUtil.parse("<gray>Visual appearance when rendered on</gray>"));
            pLore.add(TextUtil.parse("<gray>player or NPC armor stands.</gray>"));
            pLore.add(Component.empty());
            pLore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Item ID:</gray> <yellow>" + def.getId() + "</yellow>"));
            pLore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Model Material:</gray> <white>" + preview.getType().name() + "</white>"));
            String cmdTag = def.getTag("curiospaper:model_cmd");
            pLore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>CustomModelData:</gray> <white>" + (cmdTag != null ? cmdTag : "None / Default") + "</white>"));
            String modelTag = def.getTag("curiospaper:model_item_model");
            pLore.add(TextUtil.parse("<dark_gray>•</dark_gray> <gray>Item Model (1.21.4+):</gray> <white>" + (modelTag != null ? modelTag : "None / Default") + "</white>"));
            pMeta.lore(pLore);

            Integer previewCmd = null;
            if (cmdTag != null) {
                try { previewCmd = Integer.parseInt(cmdTag.trim()); } catch (Exception ignored) {}
            }
            VersionUtil.setItemModelSafe(pMeta, modelTag, previewCmd);
            preview.setItemMeta(pMeta);
        }
        canvas.getCanvas().setButton(4, CanvasButton.of(preview));

        // 2. Toggle Model Enabled (Slot 19)
        String renderModel = def.getTag("curiospaper:render_model");
        boolean enabled = "true".equalsIgnoreCase(renderModel);
        ItemStack toggleBtn = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta tMeta = toggleBtn.getItemMeta();
        if (tMeta != null) {
            tMeta.displayName(TextUtil.parse(enabled ? "<green><bold>✔ 3D Model Enabled</bold></green>" : "<red><bold>✖ 3D Model Disabled</bold></red>"));
            List<Component> tLore = new ArrayList<>();
            tLore.add(TextUtil.parse("<gray>Controls whether a 3D armor stand</gray>"));
            tLore.add(TextUtil.parse("<gray>is rendered on players wearing this item.</gray>"));
            tLore.add(Component.empty());
            tLore.add(TextUtil.parse("<gray>Status:</gray> " + (enabled ? "<green>Enabled</green>" : "<red>Disabled</red>")));
            tLore.add(Component.empty());
            tLore.add(TextUtil.parse("<yellow>Click to toggle »</yellow>"));
            tMeta.lore(tLore);
            toggleBtn.setItemMeta(tMeta);
        }
        canvas.getCanvas().setButton(19, CanvasButton.of(toggleBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
            def.setTag("curiospaper:render_model", enabled ? "false" : "true");
            open3DModelSettings(player, synth, def, onBack);
        }));

        // 3. Model Item Material (Slot 21)
        ItemStack matBtn = new ItemStack(Material.IRON_BLOCK);
        ItemMeta mMeta = matBtn.getItemMeta();
        if (mMeta != null) {
            mMeta.displayName(TextUtil.parse("<gold><bold>Model Item Material</bold></gold>"));
            List<Component> mLore = new ArrayList<>();
            mLore.add(TextUtil.parse("<gray>The specific material placed on the armor stand</gray>"));
            mLore.add(TextUtil.parse("<gray>head to display custom item 3D models.</gray>"));
            mLore.add(TextUtil.parse("<gray>(e.g. LEATHER_HORSE_ARMOR, PAPER, CARROT_ON_A_STICK)</gray>"));
            mLore.add(Component.empty());
            mLore.add(TextUtil.parse("<gray>Current:</gray> <white>" + (modelMatStr != null && !modelMatStr.isEmpty() ? modelMatStr : "Default (Base Item)") + "</white>"));
            mLore.add(Component.empty());
            mLore.add(TextUtil.parse("<yellow>Left-Click: Enter custom material name</yellow>"));
            mLore.add(TextUtil.parse("<red>Right-Click: Reset to Default</red>"));
            mMeta.lore(mLore);
            matBtn.setItemMeta(mMeta);
        }
        canvas.getCanvas().setButton(21, CanvasButton.of(matBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            if (e.isRightClick()) {
                def.setTag("curiospaper:model_item", null);
                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Reset 3D model material to default base item.</green>"));
                open3DModelSettings(player, synth, def, onBack);
            } else {
                synth.getChatPromptManager().prompt(player,
                        TextUtil.parse("<gold>[CuriosPaper]</gold> <yellow>Enter material for 3D model (e.g. LEATHER_HORSE_ARMOR, PAPER):</yellow>"),
                        input -> {
                            if (input != null && !input.trim().isEmpty() && !input.equalsIgnoreCase("remove") && !input.equalsIgnoreCase("clear")) {
                                Material mat = Material.matchMaterial(input.trim().toUpperCase(Locale.ROOT));
                                if (mat != null) {
                                    def.setTag("curiospaper:model_item", mat.name());
                                    player.sendMessage(TextUtil.parse("<green>[CuriosPaper] 3D model material set to: <white>" + mat.name() + "</white></green>"));
                                } else {
                                    player.sendMessage(TextUtil.parse("<red>[CuriosPaper] Unknown material: <white>" + input.trim() + "</white></red>"));
                                }
                            } else {
                                def.setTag("curiospaper:model_item", null);
                                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Reset 3D model material.</green>"));
                            }
                            open3DModelSettings(player, synth, def, onBack);
                        });
            }
        }));

        // 4. Model CustomModelData (Slot 23)
        String cmdVal = def.getTag("curiospaper:model_cmd");
        ItemStack cmdBtn = new ItemStack(Material.PAINTING);
        ItemMeta cmdMeta = cmdBtn.getItemMeta();
        if (cmdMeta != null) {
            cmdMeta.displayName(TextUtil.parse("<gold><bold>Model Custom Model Data</bold></gold>"));
            List<Component> cmdLore = new ArrayList<>();
            cmdLore.add(TextUtil.parse("<gray>Integer CustomModelData value for the</gray>"));
            cmdLore.add(TextUtil.parse("<gray>rendered 3D model armor stand.</gray>"));
            cmdLore.add(Component.empty());
            cmdLore.add(TextUtil.parse("<gray>Current:</gray> <white>" + (cmdVal != null ? cmdVal : "None / Default") + "</white>"));
            cmdLore.add(Component.empty());
            cmdLore.add(TextUtil.parse("<yellow>Left-Click: Enter CustomModelData integer</yellow>"));
            cmdLore.add(TextUtil.parse("<red>Right-Click: Clear CustomModelData</red>"));
            cmdMeta.lore(cmdLore);
            cmdBtn.setItemMeta(cmdMeta);
        }
        canvas.getCanvas().setButton(23, CanvasButton.of(cmdBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            if (e.isRightClick()) {
                def.setTag("curiospaper:model_cmd", null);
                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared 3D model CustomModelData.</green>"));
                open3DModelSettings(player, synth, def, onBack);
            } else {
                synth.getChatPromptManager().prompt(player,
                        TextUtil.parse("<gold>[CuriosPaper]</gold> <yellow>Enter 3D model CustomModelData integer (or type 'clear'):</yellow>"),
                        input -> {
                            if (input != null && !input.trim().isEmpty() && !input.equalsIgnoreCase("clear") && !input.equalsIgnoreCase("remove")) {
                                try {
                                    int cmd = Integer.parseInt(input.trim());
                                    def.setTag("curiospaper:model_cmd", String.valueOf(cmd));
                                    player.sendMessage(TextUtil.parse("<green>[CuriosPaper] 3D model CustomModelData set to: <white>" + cmd + "</white></green>"));
                                } catch (NumberFormatException ex) {
                                    player.sendMessage(TextUtil.parse("<red>[CuriosPaper] Invalid integer: <white>" + input.trim() + "</white></red>"));
                                }
                            } else {
                                def.setTag("curiospaper:model_cmd", null);
                                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared 3D model CustomModelData.</green>"));
                            }
                            open3DModelSettings(player, synth, def, onBack);
                        });
            }
        }));

        // 5. Model Item Model (1.21.4+) (Slot 25)
        String modelComp = def.getTag("curiospaper:model_item_model");
        ItemStack modelBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta modelMeta = modelBtn.getItemMeta();
        if (modelMeta != null) {
            modelMeta.displayName(TextUtil.parse("<gold><bold>Item Model Component (1.21.4+)</bold></gold>"));
            List<Component> imLore = new ArrayList<>();
            imLore.add(TextUtil.parse("<gray>Namespaced item model for modern Minecraft</gray>"));
            imLore.add(TextUtil.parse("<gray>resource packs (format: namespace:key).</gray>"));
            imLore.add(Component.empty());
            imLore.add(TextUtil.parse("<gray>Current:</gray> <white>" + (modelComp != null ? modelComp : "None / Default") + "</white>"));
            imLore.add(Component.empty());
            imLore.add(TextUtil.parse("<yellow>Left-Click: Enter item model identifier</yellow>"));
            imLore.add(TextUtil.parse("<red>Right-Click: Clear item model</red>"));
            modelMeta.lore(imLore);
            modelBtn.setItemMeta(modelMeta);
        }
        canvas.getCanvas().setButton(25, CanvasButton.of(modelBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            if (e.isRightClick()) {
                def.setTag("curiospaper:model_item_model", null);
                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared 3D item model identifier.</green>"));
                open3DModelSettings(player, synth, def, onBack);
            } else {
                synth.getChatPromptManager().prompt(player,
                        TextUtil.parse("<gold>[CuriosPaper]</gold> <yellow>Enter 1.21.4+ item model (format namespace:key):</yellow>"),
                        input -> {
                            if (input != null && !input.trim().isEmpty() && !input.equalsIgnoreCase("clear") && !input.equalsIgnoreCase("remove")) {
                                String clean = input.trim().toLowerCase(Locale.ROOT);
                                if (!clean.contains(":")) {
                                    clean = "minecraft:" + clean;
                                }
                                def.setTag("curiospaper:model_item_model", clean);
                                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] 3D item model set to: <white>" + clean + "</white></green>"));
                            } else {
                                def.setTag("curiospaper:model_item_model", null);
                                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared 3D item model identifier.</green>"));
                            }
                            open3DModelSettings(player, synth, def, onBack);
                        });
            }
        }));

        // 6. Pitch Up Limit (Slot 28)
        String pitchUp = def.getTag("curiospaper:pitch_up_limit");
        ItemStack upBtn = new ItemStack(Material.ARROW);
        ItemMeta upMeta = upBtn.getItemMeta();
        if (upMeta != null) {
            upMeta.displayName(TextUtil.parse("<gold><bold>↑ Pitch Up Limit</bold></gold>"));
            List<Component> upLore = new ArrayList<>();
            upLore.add(TextUtil.parse("<gray>Hides the 3D model from the wearer's</gray>"));
            upLore.add(TextUtil.parse("<gray>first-person view when looking up past this angle.</gray>"));
            upLore.add(Component.empty());
            upLore.add(TextUtil.parse("<gray>Current:</gray> <white>" + (pitchUp != null ? pitchUp + "°" : "None") + "</white>"));
            upLore.add(Component.empty());
            upLore.add(TextUtil.parse("<yellow>Left-Click: Enter pitch angle (e.g. 45.0)</yellow>"));
            upLore.add(TextUtil.parse("<red>Right-Click: Clear limit</red>"));
            upMeta.lore(upLore);
            upBtn.setItemMeta(upMeta);
        }
        canvas.getCanvas().setButton(28, CanvasButton.of(upBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            if (e.isRightClick()) {
                def.setTag("curiospaper:pitch_up_limit", null);
                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared Pitch Up Limit.</green>"));
                open3DModelSettings(player, synth, def, onBack);
            } else {
                synth.getChatPromptManager().prompt(player,
                        TextUtil.parse("<gold>[CuriosPaper]</gold> <yellow>Enter pitch up limit in degrees (e.g. 45.0):</yellow>"),
                        input -> {
                            if (input != null && !input.trim().isEmpty() && !input.equalsIgnoreCase("clear") && !input.equalsIgnoreCase("remove")) {
                                try {
                                    float val = Float.parseFloat(input.trim());
                                    def.setTag("curiospaper:pitch_up_limit", String.valueOf(val));
                                    player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Pitch Up Limit set to: <white>" + val + "°</white></green>"));
                                } catch (NumberFormatException ex) {
                                    player.sendMessage(TextUtil.parse("<red>[CuriosPaper] Invalid float value: <white>" + input.trim() + "</white></red>"));
                                }
                            } else {
                                def.setTag("curiospaper:pitch_up_limit", null);
                                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared Pitch Up Limit.</green>"));
                            }
                            open3DModelSettings(player, synth, def, onBack);
                        });
            }
        }));

        // 7. Pitch Down Limit (Slot 30)
        String pitchDown = def.getTag("curiospaper:pitch_down_limit");
        ItemStack downBtn = new ItemStack(Material.ARROW);
        ItemMeta downMeta = downBtn.getItemMeta();
        if (downMeta != null) {
            downMeta.displayName(TextUtil.parse("<gold><bold>↓ Pitch Down Limit</bold></gold>"));
            List<Component> downLore = new ArrayList<>();
            downLore.add(TextUtil.parse("<gray>Hides the 3D model from the wearer's</gray>"));
            downLore.add(TextUtil.parse("<gray>first-person view when looking down past this angle.</gray>"));
            downLore.add(Component.empty());
            downLore.add(TextUtil.parse("<gray>Current:</gray> <white>" + (pitchDown != null ? pitchDown + "°" : "None") + "</white>"));
            downLore.add(Component.empty());
            downLore.add(TextUtil.parse("<yellow>Left-Click: Enter pitch angle (e.g. 60.0)</yellow>"));
            downLore.add(TextUtil.parse("<red>Right-Click: Clear limit</red>"));
            downMeta.lore(downLore);
            downBtn.setItemMeta(downMeta);
        }
        canvas.getCanvas().setButton(30, CanvasButton.of(downBtn, e -> {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
            if (e.isRightClick()) {
                def.setTag("curiospaper:pitch_down_limit", null);
                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared Pitch Down Limit.</green>"));
                open3DModelSettings(player, synth, def, onBack);
            } else {
                synth.getChatPromptManager().prompt(player,
                        TextUtil.parse("<gold>[CuriosPaper]</gold> <yellow>Enter pitch down limit in degrees (e.g. 60.0):</yellow>"),
                        input -> {
                            if (input != null && !input.trim().isEmpty() && !input.equalsIgnoreCase("clear") && !input.equalsIgnoreCase("remove")) {
                                try {
                                    float val = Float.parseFloat(input.trim());
                                    def.setTag("curiospaper:pitch_down_limit", String.valueOf(val));
                                    player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Pitch Down Limit set to: <white>" + val + "°</white></green>"));
                                } catch (NumberFormatException ex) {
                                    player.sendMessage(TextUtil.parse("<red>[CuriosPaper] Invalid float value: <white>" + input.trim() + "</white></red>"));
                                }
                            } else {
                                def.setTag("curiospaper:pitch_down_limit", null);
                                player.sendMessage(TextUtil.parse("<green>[CuriosPaper] Cleared Pitch Down Limit.</green>"));
                            }
                            open3DModelSettings(player, synth, def, onBack);
                        });
            }
        }));

        canvas.setBackButton(onBack);
        canvas.setSaveButton();
        canvas.open(player);
    }

    private boolean hasSlotLoreLine(ItemDefinition def) {
        if (def.getLore() == null) return false;
        for (String line : def.getLore()) {
            String stripped = TextUtil.stripAll(line).toLowerCase(Locale.ROOT);
            if (stripped.startsWith("slot:") || stripped.startsWith("required slot:")) {
                return true;
            }
        }
        return false;
    }

    private void updateSlotLore(ItemDefinition def, String slotName) {
        List<String> lore = def.getLore() != null ? new ArrayList<>(def.getLore()) : new ArrayList<>();
        String newSlotLine = plugin.getLanguageManager().get("items.slot-lore", "slot", slotName);
        boolean updated = false;

        for (int i = 0; i < lore.size(); i++) {
            String stripped = TextUtil.stripAll(lore.get(i)).toLowerCase(Locale.ROOT);
            if (stripped.startsWith("slot:") || stripped.startsWith("required slot:")) {
                lore.set(i, newSlotLine);
                updated = true;
                break;
            }
        }

        if (!updated) {
            lore.add(newSlotLine);
        }

        if (plugin.getConfig().getBoolean("features.quick-equip-right-click", true)) {
            String qeFormat = plugin.getLanguageManager().get("items.quick-equip-lore");
            if (qeFormat != null && !qeFormat.trim().isEmpty()) {
                boolean hasQe = false;
                for (String l : lore) {
                    String stripped = TextUtil.stripAll(l).toLowerCase(Locale.ROOT);
                    if (stripped.contains("shift + right click") || stripped.contains("quick to equip") || stripped.contains("click to equip")) {
                        hasQe = true;
                        break;
                    }
                }
                if (!hasQe) {
                    lore.add(qeFormat);
                }
            }
        }

        def.setLore(lore);
    }

    private void removeSlotLore(ItemDefinition def) {
        if (def.getLore() == null) return;
        List<String> lore = new ArrayList<>(def.getLore());
        lore.removeIf(line -> {
            String stripped = TextUtil.stripAll(line).toLowerCase(Locale.ROOT);
            return stripped.startsWith("slot:") || stripped.startsWith("required slot:") ||
                   stripped.contains("shift + right click") || stripped.contains("quick to equip") || stripped.contains("click to equip");
        });
        def.setLore(lore);
    }
}

