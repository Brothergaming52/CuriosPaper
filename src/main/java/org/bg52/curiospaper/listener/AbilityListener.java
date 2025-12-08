package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.AbilityData;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Listens for AccessoryEquipEvent and applies/removes abilities based on
 * equipped items
 */
public class AbilityListener implements Listener {
    private final CuriosPaper plugin;
    private final ItemDataManager itemDataManager;
    private final Map<UUID, Set<String>> activeModifiers; // player UUID -> set of modifier IDs
    private BukkitRunnable whileEquippedTask;

    private static final String MODIFIER_PREFIX = "curiospaper_ability_";

    public AbilityListener(CuriosPaper plugin) {
        this.plugin = plugin;
        this.itemDataManager = plugin.getItemDataManager();
        this.activeModifiers = new HashMap<>();
        startWhileEquippedTask();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        Player player = event.getPlayer();
        ItemStack previousItem = event.getPreviousItem();
        ItemStack newItem = event.getNewItem();

        // Remove abilities from previous item (if any)
        if (previousItem != null && !previousItem.getType().isAir()) {
            String prevItemId = getItemId(previousItem);
            if (prevItemId != null) {
                ItemData prevData = itemDataManager.getItemData(prevItemId);
                if (prevData != null) {
                    removeAbilities(player, prevData, AbilityData.TriggerType.EQUIP);
                    removeAbilities(player, prevData, AbilityData.TriggerType.WHILE_EQUIPPED);
                    // Apply DE_EQUIP trigger
                    applyAbilities(player, prevData, AbilityData.TriggerType.DE_EQUIP);
                }
            }
        }

        // Apply abilities from new item (if any)
        if (newItem != null && !newItem.getType().isAir()) {
            String newItemId = getItemId(newItem);
            if (newItemId != null) {
                ItemData newData = itemDataManager.getItemData(newItemId);
                if (newData != null) {
                    // Apply EQUIP trigger
                    applyAbilities(player, newData, AbilityData.TriggerType.EQUIP);
                }
            }
        }
    }

    /**
     * Applies abilities with the specified trigger type
     */
    private void applyAbilities(Player player, ItemData itemData, AbilityData.TriggerType trigger) {
        for (AbilityData ability : itemData.getAbilities()) {
            if (ability.getTrigger() == trigger) {
                applyAbility(player, ability, itemData.getItemId());
            }
        }
    }

    /**
     * Removes abilities with the specified trigger type
     */
    private void removeAbilities(Player player, ItemData itemData, AbilityData.TriggerType trigger) {
        for (AbilityData ability : itemData.getAbilities()) {
            if (ability.getTrigger() == trigger) {
                removeAbility(player, ability, itemData.getItemId());
            }
        }
    }

    /**
     * Applies a single ability to a player
     */
    private void applyAbility(Player player, AbilityData ability, String itemId) {
        if (ability.getEffectType() == AbilityData.EffectType.POTION_EFFECT) {
            applyPotionEffect(player, ability);
        } else if (ability.getEffectType() == AbilityData.EffectType.PLAYER_MODIFIER) {
            applyPlayerModifier(player, ability, itemId);
        }
    }

    /**
     * Removes a single ability from a player
     */
    private void removeAbility(Player player, AbilityData ability, String itemId) {
        if (ability.getEffectType() == AbilityData.EffectType.PLAYER_MODIFIER) {
            removePlayerModifier(player, ability, itemId);
        }
        // Potion effects expire naturally
    }

    private void applyPotionEffect(Player player, AbilityData ability) {
        try {
            PotionEffectType type = PotionEffectType.getByName(ability.getEffectName());
            if (type != null) {
                PotionEffect effect = new PotionEffect(type, ability.getDuration(),
                        ability.getAmplifier(), false, true, true);
                player.addPotionEffect(effect);

                if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
                    plugin.getLogger().info("Applied potion effect " + ability.getEffectName() +
                            " to " + player.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply potion effect: " + ability.getEffectName());
        }
    }

    private void applyPlayerModifier(Player player, AbilityData ability, String itemId) {
        Attribute attribute = getAttributeFromName(ability.getEffectName());
        if (attribute == null)
            return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null)
            return;

        // Create unique modifier ID
        String modifierId = MODIFIER_PREFIX + itemId + "_" + ability.getEffectName();
        UUID modifierUUID = UUID.nameUUIDFromBytes(modifierId.getBytes());

        // Remove existing modifier if present
        AttributeModifier existing = instance.getModifier(modifierUUID);
        if (existing != null) {
            instance.removeModifier(existing);
        }

        // Calculate modifier value based on amplifier
        double value = calculateModifierValue(attribute, ability.getAmplifier());

        // Add new modifier
        AttributeModifier modifier = new AttributeModifier(
                modifierUUID,
                modifierId,
                value,
                AttributeModifier.Operation.ADD_NUMBER);

        instance.addModifier(modifier);

        // Track active modifier
        activeModifiers.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(modifierId);

        if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
            plugin.getLogger().info("Applied modifier " + ability.getEffectName() +
                    " (" + value + ") to " + player.getName());
        }
    }

    private void removePlayerModifier(Player player, AbilityData ability, String itemId) {
        Attribute attribute = getAttributeFromName(ability.getEffectName());
        if (attribute == null)
            return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null)
            return;

        String modifierId = MODIFIER_PREFIX + itemId + "_" + ability.getEffectName();
        UUID modifierUUID = UUID.nameUUIDFromBytes(modifierId.getBytes());

        AttributeModifier existing = instance.getModifier(modifierUUID);
        if (existing != null) {
            instance.removeModifier(existing);

            Set<String> playerModifiers = activeModifiers.get(player.getUniqueId());
            if (playerModifiers != null) {
                playerModifiers.remove(modifierId);
            }

            if (plugin.getConfig().getBoolean("debug.log-inventory-events", false)) {
                plugin.getLogger().info("Removed modifier " + ability.getEffectName() +
                        " from " + player.getName());
            }
        }
    }

    /**
     * Periodic task to apply WHILE_EQUIPPED abilities
     */
    private void startWhileEquippedTask() {
        whileEquippedTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    processWhileEquippedAbilities(player);
                }
            }
        };
        whileEquippedTask.runTaskTimer(plugin, 20L, 20L); // Run every second
    }

    private void processWhileEquippedAbilities(Player player) {
        // Get all equipped items
        for (String slotType : plugin.getCuriosPaperAPI().getAllSlotTypes()) {
            List<ItemStack> items = plugin.getCuriosPaperAPI().getEquippedItems(player, slotType);
            for (ItemStack item : items) {
                if (item == null || item.getType().isAir())
                    continue;

                String itemId = getItemId(item);
                if (itemId == null)
                    continue;

                ItemData itemData = itemDataManager.getItemData(itemId);
                if (itemData == null)
                    continue;

                // Apply WHILE_EQUIPPED abilities
                for (AbilityData ability : itemData.getAbilities()) {
                    if (ability.getTrigger() == AbilityData.TriggerType.WHILE_EQUIPPED) {
                        applyAbility(player, ability, itemId);
                    }
                }
            }
        }
    }

    public void shutdown() {
        if (whileEquippedTask != null) {
            whileEquippedTask.cancel();
        }

        // Remove all modifiers
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Set<String> modifiers = activeModifiers.get(player.getUniqueId());
            if (modifiers != null) {
                for (String modifierId : new HashSet<>(modifiers)) {
                    // Parse modifier to get attribute name
                    String attrName = modifierId.replace(MODIFIER_PREFIX, "").split("_")[1];
                    Attribute attribute = getAttributeFromName(attrName);
                    if (attribute != null) {
                        AttributeInstance instance = player.getAttribute(attribute);
                        if (instance != null) {
                            UUID uuid = UUID.nameUUIDFromBytes(modifierId.getBytes());
                            AttributeModifier mod = instance.getModifier(uuid);
                            if (mod != null) {
                                instance.removeModifier(mod);
                            }
                        }
                    }
                }
            }
        }
        activeModifiers.clear();
    }

    private Attribute getAttributeFromName(String name) {
        // Try direct match first
        for (Attribute attr : Attribute.values()) {
            if (attr.name().equalsIgnoreCase(name) ||
                    attr.getKey().getKey().equalsIgnoreCase(name)) {
                return attr;
            }
        }

        // Try formatted name match
        for (Attribute attr : Attribute.values()) {
            String formattedName = attr.name()
                    .replace("GENERIC_", "")
                    .replace("PLAYER_", "")
                    .replace("_", " ");
            if (formattedName.equalsIgnoreCase(name.replace("_", " "))) {
                return attr;
            }
        }

        return null;
    }

    private double calculateModifierValue(Attribute attribute, int amplifier) {
        // For attributes: amplifier is stored as value * 100 to preserve decimals
        // So we divide by 100 to get the actual value
        return amplifier / 100.0;
    }

    /**
     * Gets the item ID from an ItemStack by matching it against registered items
     */
    private String getItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta())
            return null;

        // Try to find matching item by display name
        String displayName = itemStack.getItemMeta().getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            for (ItemData data : itemDataManager.getAllItems().values()) {
                if (displayName.equals(data.getDisplayName())) {
                    return data.getItemId();
                }
            }
        }

        return null;
    }
}
