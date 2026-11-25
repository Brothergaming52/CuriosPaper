package org.bg52.curiospaper.citizens;

import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.inventory.CuriosGUIHolder;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bg52.curiospaper.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Interactive GUI for managing a Citizens NPC's equipped Curios accessories.
 */
public class NpcCuriosGUI implements Listener {

    private final CuriosPaper plugin;

    public NpcCuriosGUI(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, NPC npc) {
        if (!player.hasPermission("curiospaper.admin")) {
            plugin.getLanguageManager().sendMessage(player, "common.no-permission");
            return;
        }

        if (npc == null) {
            plugin.getLanguageManager().sendMessage(player, "citizens.no-npc-selected");
            return;
        }

        CuriosTrait trait = npc.getOrAddTrait(CuriosTrait.class);
        if (plugin.getConfig().getBoolean("citizens.humanoid-only", true) && !trait.isHumanoid()) {
            plugin.getLanguageManager().sendMessage(player, "citizens.not-humanoid", "npc", npc.getName());
            return;
        }

        NpcCuriosHolder holder = new NpcCuriosHolder(npc, trait);
        Component title = plugin.getLanguageManager().getRawComponent("gui.npc-title", "npc", npc.getName());
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);

        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Perimeter
        for (int c = 0; c < 9; c++) {
            inv.setItem(c, border);
            inv.setItem(45 + c, border);
        }
        for (int r = 1; r < 5; r++) {
            inv.setItem(r * 9, border);
            inv.setItem(r * 9 + 8, border);
        }

        // Map and render slots
        for (SlotDefinition def : plugin.getSlotRegistry().getAllSlots()) {
            int maxSlots = def.getMaxSlots();
            int baseGuiSlot = def.getGuiSlot();

            for (int subIndex = 0; subIndex < maxSlots; subIndex++) {
                int targetSlot = baseGuiSlot + subIndex;
                if (targetSlot < 54 && targetSlot % 9 != 0 && targetSlot % 9 != 8) {
                    holder.mapSlot(targetSlot, def.getId(), subIndex);
                    ItemStack equipped = trait.getEquippedItem(def.getId(), subIndex);
                    if (equipped != null && equipped.getType() != Material.AIR) {
                        inv.setItem(targetSlot, equipped.clone());
                    } else {
                        inv.setItem(targetSlot, def.createPlaceholderItem(player));
                    }
                }
            }
        }

        player.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof NpcCuriosHolder)) {
            return;
        }

        NpcCuriosHolder holder = (NpcCuriosHolder) top.getHolder();
        int rawSlot = event.getRawSlot();

        if (rawSlot >= 0 && rawSlot < top.getSize()) {
            CuriosGUIHolder.SlotMapping mapping = holder.getMapping(rawSlot);
            if (mapping == null) {
                event.setCancelled(true);
                return;
            }

            Player player = (Player) event.getWhoClicked();
            ItemStack cursor = event.getCursor();
            ItemStack currentlyEquipped = holder.getTrait().getEquippedItem(mapping.getSlotType(), mapping.getSubIndex());

            if (cursor == null || cursor.getType().isAir()) {
                // Take item from NPC
                if (currentlyEquipped != null && !currentlyEquipped.getType().isAir()) {
                    holder.getTrait().setEquippedItem(mapping.getSlotType(), mapping.getSubIndex(), null);
                    SlotDefinition def = plugin.getSlotRegistry().getSlot(mapping.getSlotType());
                    top.setItem(rawSlot, def != null ? def.createPlaceholderItem(player) : null);
                    player.setItemOnCursor(currentlyEquipped);
                    plugin.getLanguageManager().sendMessage(player, "citizens.unequipped", "slot", mapping.getSlotType(), "npc", holder.getNpc().getName());
                }
            } else {
                // Put item on NPC
                String targetSlot = plugin.getItemEngineManager().resolveCuriosSlot(cursor);
                if (targetSlot == null || !targetSlot.equalsIgnoreCase(mapping.getSlotType())) {
                    plugin.getLanguageManager().sendMessage(player, "inventory.invalid-item");
                    event.setCancelled(true);
                    return;
                }

                ItemStack toEquip = cursor.clone();
                toEquip.setAmount(1);

                if (cursor.getAmount() > 1) {
                    cursor.setAmount(cursor.getAmount() - 1);
                    player.setItemOnCursor(cursor);
                } else {
                    player.setItemOnCursor(currentlyEquipped);
                }

                holder.getTrait().setEquippedItem(mapping.getSlotType(), mapping.getSubIndex(), toEquip);
                top.setItem(rawSlot, toEquip.clone());
                plugin.getLanguageManager().sendMessage(player, "citizens.equipped",
                        "item", toEquip.getType().name(),
                        "slot", mapping.getSlotType(),
                        "npc", holder.getNpc().getName()
                );
            }
            event.setCancelled(true);
        }
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.parse(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}

