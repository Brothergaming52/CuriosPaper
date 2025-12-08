package org.bg52.curiospaper.inventory;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.AbilityData;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.manager.ChatInputManager;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * GUI for managing item abilities (effects triggered on equip/de-equip/while
 * equipped)
 */
public class AbilityEditorGUI implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final ChatInputManager chatInputManager;
    private final Map<UUID, String> currentItemId;
    private final Map<UUID, Integer> editingAbilityIndex; // -1 = new ability
    private final Map<UUID, AbilityData> pendingAbility;

    private static final String ABILITY_LIST_TITLE = "§8Abilities: ";
    private static final String CONFIGURE_TITLE = "§8Configure Ability";
    private static final String EFFECT_SELECT_TITLE = "§8Select Effect";

    public AbilityEditorGUI(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
        this.chatInputManager = plugin.getChatInputManager();
        this.currentItemId = new HashMap<>();
        this.editingAbilityIndex = new HashMap<>();
        this.pendingAbility = new HashMap<>();
    }

    /**
     * Opens the main ability list GUI
     */
    public void open(Player player, String itemId) {
        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null) {
            player.sendMessage("§cItem not found!");
            return;
        }

        currentItemId.put(player.getUniqueId(), itemId);
        openAbilityList(player, itemData);
    }

    /**
     * Screen 1: Ability List
     */
    private void openAbilityList(Player player, ItemData itemData) {
        Inventory gui = Bukkit.createInventory(null, 54, ABILITY_LIST_TITLE + itemData.getItemId());

        List<AbilityData> abilities = itemData.getAbilities();

        // Header info
        gui.setItem(4, createGuiItem(Material.ENCHANTED_BOOK, "§6§lItem Abilities",
                "§7Total Abilities: §e" + abilities.size(),
                "",
                "§7Abilities modify item behavior",
                "§7when equipped or de-equipped"));

        // Display abilities
        int startSlot = 9;
        for (int i = 0; i < Math.min(abilities.size(), 36); i++) {
            AbilityData ability = abilities.get(i);
            gui.setItem(startSlot + i, createAbilityIcon(ability, i));
        }

        // Controls
        gui.setItem(45, createGuiItem(Material.LIME_DYE, "§a§l+ Add New Ability",
                "§7Click to create a new ability",
                "",
                "§eSupports:",
                "§7• Potion Effects (27 types)",
                "§7• Attribute Modifiers (all attributes)"));
        gui.setItem(49, createGuiItem(Material.OAK_DOOR, "§e« Back to Editor", "§7Return to item editor"));
        gui.setItem(53, createGuiItem(Material.LAVA_BUCKET, "§c§l⚠ Delete Mode",
                "§cShift-click an ability to delete it",
                "",
                "§7Cannot be undone!"));

        // Fill empty slots
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    /**
     * Screen 2: Configure Ability
     */
    private void openConfigureGUI(Player player, AbilityData ability, int index) {
        Inventory gui = Bukkit.createInventory(null, 54, CONFIGURE_TITLE);

        UUID playerId = player.getUniqueId();
        editingAbilityIndex.put(playerId, index);
        pendingAbility.put(playerId, ability);

        // Header
        gui.setItem(4, createGuiItem(Material.KNOWLEDGE_BOOK, "§6§lAbility Configuration",
                "§7Configure when and what this ability does",
                "",
                "§71. Choose a trigger",
                "§72. Choose an effect type",
                "§73. Select specific effect"));

        // Section 1: Trigger Selection
        gui.setItem(18, createGuiItem(Material.CLOCK, "§e§lWhen", "§7Choose when to activate"));
        gui.setItem(19, createTriggerIcon(AbilityData.TriggerType.EQUIP,
                ability.getTrigger() == AbilityData.TriggerType.EQUIP));
        gui.setItem(20, createTriggerIcon(AbilityData.TriggerType.DE_EQUIP,
                ability.getTrigger() == AbilityData.TriggerType.DE_EQUIP));
        gui.setItem(21, createTriggerIcon(AbilityData.TriggerType.WHILE_EQUIPPED,
                ability.getTrigger() == AbilityData.TriggerType.WHILE_EQUIPPED));

        // Section 2: Effect Type Selection
        gui.setItem(27, createGuiItem(Material.COMPARATOR, "§e§lWhat", "§7Choose effect category"));
        gui.setItem(28, createEffectTypeIcon(AbilityData.EffectType.POTION_EFFECT,
                ability.getEffectType() == AbilityData.EffectType.POTION_EFFECT));
        gui.setItem(29, createEffectTypeIcon(AbilityData.EffectType.PLAYER_MODIFIER,
                ability.getEffectType() == AbilityData.EffectType.PLAYER_MODIFIER));

        // Section 3: Effect Selection
        gui.setItem(36, createGuiItem(Material.NETHER_STAR, "§e§lWhich", "§7Pick specific effect"));
        if (ability.getEffectName() != null) {
            Material icon = ability.getEffectType() == AbilityData.EffectType.POTION_EFFECT
                    ? Material.POTION
                    : Material.ENCHANTED_BOOK;

            String valueDisplay = ability.getEffectType() == AbilityData.EffectType.POTION_EFFECT
                    ? "§7Level: §f" + (ability.getAmplifier() + 1) + " §8| §7Duration: §f"
                            + (ability.getDuration() / 20) + "s"
                    : "§7Value: §f" + (ability.getAmplifier() / 100.0);

            gui.setItem(37, createGuiItem(icon, "§a§l✔ " + ability.getEffectName(),
                    "§7Trigger: §f" + ability.getTrigger().name(),
                    "§7Type: §f" + ability.getEffectType().name(),
                    valueDisplay,
                    "",
                    "§eClick to change"));
        } else {
            gui.setItem(37, createGuiItem(Material.BARRIER, "§c§lNot Selected",
                    "§7Choose trigger & type first",
                    "",
                    "§eClick to select"));
        }

        // Controls
        gui.setItem(48, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§c✖ Cancel", "§7Discard changes"));
        gui.setItem(50, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "§a✔ Save",
                ability.isValid() ? "§7Click to save ability" : "§cConfigure all settings first"));

        // Fill
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    /**
     * Screen 3: Effect Selection (shows potion effects or player modifiers)
     */
    private void openEffectSelector(Player player, AbilityData.EffectType effectType) {
        Inventory gui = Bukkit.createInventory(null, 54, EFFECT_SELECT_TITLE);

        if (effectType == AbilityData.EffectType.POTION_EFFECT) {
            // Display potion effects
            String[] potions = { "SPEED", "SLOWNESS", "HASTE", "MINING_FATIGUE", "STRENGTH",
                    "INSTANT_HEALTH", "INSTANT_DAMAGE", "JUMP_BOOST", "NAUSEA", "REGENERATION",
                    "RESISTANCE", "FIRE_RESISTANCE", "WATER_BREATHING", "INVISIBILITY", "BLINDNESS",
                    "NIGHT_VISION", "HUNGER", "WEAKNESS", "POISON", "WITHER", "HEALTH_BOOST",
                    "ABSORPTION", "SATURATION", "GLOWING", "LEVITATION", "LUCK", "UNLUCK" };

            for (int i = 0; i < Math.min(potions.length, 45); i++) {
                gui.setItem(i, createGuiItem(Material.POTION, "§d" + potions[i],
                        "§7Type: §fPotion Effect",
                        "",
                        "§eClick to select"));
            }
        } else {
            // Display ALL available attributes dynamically
            org.bukkit.attribute.Attribute[] attributes = org.bukkit.attribute.Attribute.values();
            int slot = 0;
            for (org.bukkit.attribute.Attribute attr : attributes) {
                if (slot >= 45)
                    break;

                Material icon = getAttributeIcon(attr);
                String name = formatAttributeName(attr.name());

                gui.setItem(slot, createGuiItem(icon, "§b" + name,
                        "§7Type: §fAttribute Modifier",
                        "§7Key: §f" + attr.getKey().getKey(),
                        "",
                        "§eClick to select"));
                slot++;
            }
        }

        gui.setItem(49, createGuiItem(Material.BARRIER, "§cBack", "§7Return to configuration"));

        // Fill
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Only handle our GUIs
        if (!title.startsWith(ABILITY_LIST_TITLE) &&
                !title.equals(CONFIGURE_TITLE) &&
                !title.equals(EFFECT_SELECT_TITLE)) {
            return;
        }

        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (raw >= topSize)
            return;

        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        String itemId = currentItemId.get(playerId);
        if (itemId == null)
            return;

        ItemData itemData = itemDataManager.getItemData(itemId);
        if (itemData == null)
            return;

        // Handle different screens
        if (title.startsWith(ABILITY_LIST_TITLE)) {
            handleAbilityListClick(player, itemData, raw, event.isShiftClick());
        } else if (title.equals(CONFIGURE_TITLE)) {
            handleConfigureClick(player, itemData, raw);
        } else if (title.equals(EFFECT_SELECT_TITLE)) {
            handleEffectSelectClick(player, itemData, raw, event.getCurrentItem());
        }
    }

    private void handleAbilityListClick(Player player, ItemData itemData, int slot, boolean isShiftClick) {
        UUID playerId = player.getUniqueId();
        List<AbilityData> abilities = itemData.getAbilities();

        if (slot == 45) { // Add new ability
            AbilityData newAbility = new AbilityData(
                    AbilityData.TriggerType.EQUIP,
                    AbilityData.EffectType.POTION_EFFECT,
                    null, 0, 200);
            openConfigureGUI(player, newAbility, -1);

        } else if (slot == 49) { // Back
            player.closeInventory();
            currentItemId.remove(playerId);
            plugin.getEditGUI().open(player, itemData.getItemId());

        } else if (slot >= 9 && slot < 45) { // Click on ability (abilities start at slot 9)
            int abilityIndex = slot - 9; // Convert slot to ability index
            if (abilityIndex < abilities.size()) {
                if (isShiftClick) {
                    // Delete ability
                    List<AbilityData> newList = new ArrayList<>(abilities);
                    newList.remove(abilityIndex);
                    itemData.setAbilities(newList);
                    itemDataManager.saveItemData(itemData.getItemId());
                    player.sendMessage("§c✘ Ability deleted");
                    openAbilityList(player, itemData);
                } else {
                    // Edit ability
                    openConfigureGUI(player, abilities.get(abilityIndex), abilityIndex);
                }
            }
        }
    }

    private void handleConfigureClick(Player player, ItemData itemData, int slot) {
        UUID playerId = player.getUniqueId();
        AbilityData ability = pendingAbility.get(playerId);
        if (ability == null)
            return;

        // Trigger selection (slots 19-21)
        if (slot == 19)
            ability.setTrigger(AbilityData.TriggerType.EQUIP);
        else if (slot == 20)
            ability.setTrigger(AbilityData.TriggerType.DE_EQUIP);
        else if (slot == 21)
            ability.setTrigger(AbilityData.TriggerType.WHILE_EQUIPPED);

        // Effect type selection (slots 28-29)
        else if (slot == 28)
            ability.setEffectType(AbilityData.EffectType.POTION_EFFECT);
        else if (slot == 29)
            ability.setEffectType(AbilityData.EffectType.PLAYER_MODIFIER);

        // Select effect (slot 37)
        else if (slot == 37 && ability.getEffectType() != null) {
            openEffectSelector(player, ability.getEffectType());
            return;
        }

        // Cancel (slot 48)
        else if (slot == 48) {
            pendingAbility.remove(playerId);
            editingAbilityIndex.remove(playerId);
            openAbilityList(player, itemData);
            return;
        }

        // Confirm (slot 50)
        else if (slot == 50) {
            if (ability.isValid()) {
                int index = editingAbilityIndex.getOrDefault(playerId, -1);
                List<AbilityData> abilities = new ArrayList<>(itemData.getAbilities());

                if (index == -1) {
                    abilities.add(ability);
                } else {
                    abilities.set(index, ability);
                }

                itemData.setAbilities(abilities);
                itemDataManager.saveItemData(itemData.getItemId());
                player.sendMessage("§a✔ Ability saved!");

                pendingAbility.remove(playerId);
                editingAbilityIndex.remove(playerId);
                openAbilityList(player, itemData);
                return;
            } else {
                player.sendMessage("§cPlease configure all ability settings");
                return;
            }
        }

        // Refresh GUI with updated selection
        openConfigureGUI(player, ability, editingAbilityIndex.getOrDefault(playerId, -1));
    }

    private void handleEffectSelectClick(Player player, ItemData itemData, int slot, ItemStack clicked) {
        UUID playerId = player.getUniqueId();
        AbilityData ability = pendingAbility.get(playerId);
        if (ability == null)
            return;

        if (slot == 49) { // Back
            openConfigureGUI(player, ability, editingAbilityIndex.getOrDefault(playerId, -1));
            return;
        }

        // Get effect name from clicked item
        if (clicked != null && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String displayName = clicked.getItemMeta().getDisplayName();
            // Remove color codes
            String effectName = displayName.replaceAll("§[0-9a-fk-or]", "").trim();
            ability.setEffectName(effectName);

            player.closeInventory();

            // Different prompts for potion effects vs attributes
            if (ability.getEffectType() == AbilityData.EffectType.POTION_EFFECT) {
                // Potion Effect prompts
                chatInputManager.startSingleLineSession(player,
                        "§d§lPotion Effect Configuration\n§eEnter effect level (1-10, higher = stronger):",
                        ampStr -> {
                            try {
                                int amp = Math.max(0, Math.min(9, Integer.parseInt(ampStr) - 1)); // Convert 1-10 to 0-9
                                ability.setAmplifier(amp);

                                chatInputManager.startSingleLineSession(player,
                                        "§eEnter duration in seconds (e.g., 10 for 10 seconds):\n§7Tip: Use 1 for WHILE_EQUIPPED abilities",
                                        durStr -> {
                                            try {
                                                int seconds = Integer.parseInt(durStr);
                                                int ticks = seconds * 20; // Convert to ticks
                                                ability.setDuration(ticks);
                                                Bukkit.getScheduler().runTaskLater(plugin,
                                                        () -> openConfigureGUI(player, ability,
                                                                editingAbilityIndex.getOrDefault(playerId, -1)),
                                                        2L);
                                            } catch (NumberFormatException e) {
                                                player.sendMessage("§cInvalid duration, using 10 seconds");
                                                ability.setDuration(200);
                                                Bukkit.getScheduler().runTaskLater(plugin,
                                                        () -> openConfigureGUI(player, ability,
                                                                editingAbilityIndex.getOrDefault(playerId, -1)),
                                                        2L);
                                            }
                                        },
                                        () -> Bukkit.getScheduler().runTaskLater(plugin,
                                                () -> openConfigureGUI(player, ability,
                                                        editingAbilityIndex.getOrDefault(playerId, -1)),
                                                2L));
                            } catch (NumberFormatException e) {
                                player.sendMessage("§cInvalid level, using 1");
                                ability.setAmplifier(0);
                                ability.setDuration(200);
                                Bukkit.getScheduler().runTaskLater(plugin,
                                        () -> openConfigureGUI(player, ability,
                                                editingAbilityIndex.getOrDefault(playerId, -1)),
                                        2L);
                            }
                        },
                        () -> Bukkit.getScheduler().runTaskLater(plugin,
                                () -> openConfigureGUI(player, ability, editingAbilityIndex.getOrDefault(playerId, -1)),
                                2L));
            } else {
                // Attribute Modifier prompts
                chatInputManager.startSingleLineSession(player,
                        "§b§lAttribute Modifier Configuration\n§eEnter modifier value (can be decimal, e.g., 0.5 or 10):\n§7Positive = increase, Negative = decrease",
                        valueStr -> {
                            try {
                                // Store the value as amplifier * 100 to preserve decimals
                                double value = Double.parseDouble(valueStr);
                                int storedValue = (int) (value * 100); // Store as int
                                ability.setAmplifier(storedValue);
                                ability.setDuration(0); // Not used for attributes
                                player.sendMessage("§a✔ Modifier value set to: " + value);
                                Bukkit.getScheduler().runTaskLater(plugin,
                                        () -> openConfigureGUI(player, ability,
                                                editingAbilityIndex.getOrDefault(playerId, -1)),
                                        2L);
                            } catch (NumberFormatException e) {
                                player.sendMessage("§cInvalid value, using 1.0");
                                ability.setAmplifier(100);
                                ability.setDuration(0);
                                Bukkit.getScheduler().runTaskLater(plugin,
                                        () -> openConfigureGUI(player, ability,
                                                editingAbilityIndex.getOrDefault(playerId, -1)),
                                        2L);
                            }
                        },
                        () -> Bukkit.getScheduler().runTaskLater(plugin,
                                () -> openConfigureGUI(player, ability, editingAbilityIndex.getOrDefault(playerId, -1)),
                                2L));
            }
        }
    }

    private ItemStack createAbilityIcon(AbilityData ability, int index) {
        Material icon = ability.getEffectType() == AbilityData.EffectType.POTION_EFFECT
                ? Material.SPLASH_POTION
                : Material.ENCHANTED_BOOK;

        return createGuiItem(icon, "§e§l#" + (index + 1) + " " + ability.getEffectName(),
                "§7Trigger: §f" + ability.getTrigger().name(),
                "§7Type: §f" + ability.getEffectType().name(),
                "§7Amplifier: §f" + ability.getAmplifier(),
                "§7Duration: §f" + ability.getDuration() + " ticks",
                "",
                "§eClick to edit",
                "§cShift-click to delete");
    }

    private ItemStack createTriggerIcon(AbilityData.TriggerType trigger, boolean selected) {
        Material mat = selected ? Material.LIME_STAINED_GLASS_PANE : Material.PAPER;
        String prefix = selected ? "§a✔ " : "§7";

        return createGuiItem(mat, prefix + trigger.name(),
                trigger == AbilityData.TriggerType.EQUIP ? "§7When item is equipped"
                        : trigger == AbilityData.TriggerType.DE_EQUIP ? "§7When item is removed"
                                : "§7While item is equipped",
                "",
                selected ? "§aSelected" : "§eClick to select");
    }

    private ItemStack createEffectTypeIcon(AbilityData.EffectType effectType, boolean selected) {
        Material mat = selected ? Material.LIME_STAINED_GLASS_PANE
                : (effectType == AbilityData.EffectType.POTION_EFFECT ? Material.POTION : Material.ENCHANTED_BOOK);
        String prefix = selected ? "§§a✔ " : "§7";

        return createGuiItem(mat, prefix + effectType.name(),
                effectType == AbilityData.EffectType.POTION_EFFECT ? "§7Apply potion effect"
                        : "§7Modify player attribute",
                "",
                selected ? "§aSelected" : "§eClick to select");
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Gets an appropriate icon for an attribute
     */
    private Material getAttributeIcon(org.bukkit.attribute.Attribute attr) {
        String name = attr.name().toLowerCase();
        if (name.contains("speed"))
            return Material.FEATHER;
        if (name.contains("health"))
            return Material.GOLDEN_APPLE;
        if (name.contains("damage") || name.contains("attack"))
            return Material.DIAMOND_SWORD;
        if (name.contains("armor"))
            return Material.IRON_CHESTPLATE;
        if (name.contains("knockback"))
            return Material.SHIELD;
        if (name.contains("luck"))
            return Material.RABBIT_FOOT;
        if (name.contains("scale"))
            return Material.SLIME_BALL;
        if (name.contains("reach"))
            return Material.STICK;
        if (name.contains("step"))
            return Material.SCAFFOLDING;
        return Material.ENCHANTED_BOOK;
    }

    /**
     * Formats attribute name to be more readable
     */
    private String formatAttributeName(String attrName) {
        // Remove GENERIC_ or PLAYER_ prefix and convert to title case
        String formatted = attrName.replace("GENERIC_", "")
                .replace("PLAYER_", "")
                .replace("_", " ")
                .toLowerCase();

        // Capitalize first letter of each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}
