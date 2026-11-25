package org.bg52.curiospaper.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.CuriosPaperAPI;
import org.bg52.curiospaper.inventory.PlayerCuriosInventory;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Universal PlaceholderAPI expansion exposing CuriosPaper accessory state to
 * scripting plugins (Skript, Denizen), scoreboards, action bars, and GUI menus.
 *
 * Supported Placeholders:
 * - %curios_total_count% : Total count of equipped accessories
 * - %curios_has_<item_id>% : Returns "yes" or "no" if item is equipped in any slot
 * - %curios_has_slot_<slot>% : Returns "yes" or "no" if any accessory is equipped in that slot
 * - %curios_has_<slot>_<item_id>% : Returns "yes" or "no" if item is in specific slot
 * - %curios_is_equipped_<item_id>% : Returns "true" or "false"
 * - %curios_count_<slot>% : Number of accessories equipped in slot
 * - %curios_slots_unlocked_<slot>% : Total unlocked slots for player in slot type
 * - %curios_equipped_<slot>_<index>% : Display name of item (or "Empty")
 * - %curios_equipped_id_<slot>_<index>% : Item ID or material (or "none")
 */
public class CuriosExpansion extends PlaceholderExpansion {

    private final CuriosPaper plugin;

    public CuriosExpansion(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "curios";
    }

    @Override
    public @NotNull String getAuthor() {
        return "bg52";
    }

    @Override
    public @NotNull String getVersion() {
        return "3.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }
        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        CuriosPaperAPI api = plugin.getCuriosPaperAPI();
        if (api == null) return "";

        String p = params.toLowerCase(Locale.ROOT);

        // 1. %curios_total_count%
        if (p.equals("total_count") || p.equals("equipped_count")) {
            return String.valueOf(api.getAllEquippedList(player).size());
        }

        // 2. %curios_has_<item_id>%
        if (p.startsWith("has_")) {
            String target = params.substring(4);
            if (target.toLowerCase(Locale.ROOT).startsWith("slot_")) {
                String slotType = target.substring(5);
                return !api.getEquippedItems(player, slotType).isEmpty() ? "yes" : "no";
            }
            int underscore = target.indexOf('_');
            if (underscore > 0) {
                String slotCandidate = target.substring(0, underscore);
                if (plugin.getSlotRegistry().getSlot(slotCandidate) != null) {
                    String itemId = target.substring(underscore + 1);
                    return api.hasEquipped(player, slotCandidate, itemId) ? "yes" : "no";
                }
            }
            return api.hasEquipped(player, target) ? "yes" : "no";
        }

        // 3. %curios_is_equipped_<item_id>%
        if (p.startsWith("is_equipped_")) {
            String target = params.substring(12);
            return api.hasEquipped(player, target) ? "true" : "false";
        }

        // 4. %curios_count_<slot>%
        if (p.startsWith("count_")) {
            String slot = params.substring(6);
            return String.valueOf(api.getEquippedItems(player, slot).size());
        }

        // 5. %curios_slots_unlocked_<slot>%
        if (p.startsWith("slots_unlocked_")) {
            String slot = params.substring(15);
            SlotDefinition def = plugin.getSlotRegistry().getSlot(slot);
            if (def != null) {
                PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(player.getUniqueId());
                return String.valueOf(inv != null ? inv.getUnlockedSlots(def, player) : def.getDefaultUnlocked());
            }
            return "0";
        }

        // 6. %curios_equipped_id_<slot>_<index>%
        if (p.startsWith("equipped_id_")) {
            String rest = params.substring(12);
            int idx = rest.lastIndexOf('_');
            if (idx > 0) {
                String slot = rest.substring(0, idx);
                try {
                    int index = Integer.parseInt(rest.substring(idx + 1));
                    ItemStack item = api.getEquippedItem(player, slot, index);
                    if (item != null && !item.getType().isAir()) {
                        String id = api.getItemId(item);
                        return id != null ? id : item.getType().name();
                    }
                    return "none";
                } catch (NumberFormatException ignored) {}
            }
        }

        // 7. %curios_equipped_<slot>_<index>%
        if (p.startsWith("equipped_")) {
            String rest = params.substring(9);
            int idx = rest.lastIndexOf('_');
            if (idx > 0) {
                String slot = rest.substring(0, idx);
                try {
                    int index = Integer.parseInt(rest.substring(idx + 1));
                    ItemStack item = api.getEquippedItem(player, slot, index);
                    if (item != null && !item.getType().isAir()) {
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                            return item.getItemMeta().getDisplayName();
                        }
                        return item.getType().name();
                    }
                    return "Empty";
                } catch (NumberFormatException ignored) {}
            }
        }

        return null;
    }
}

