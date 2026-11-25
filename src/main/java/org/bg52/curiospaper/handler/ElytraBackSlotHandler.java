package org.bg52.curiospaper.handler;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.api.event.AccessoryEquipEvent;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bg52.curiospaper.scheduler.PlatformScheduler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bulletproof elytra back-slot flight management system.
 * <p>
 * Key guarantees:
 * 1. Synchronously intercepts chestplate right-click and inventory interactions to prevent fake elytra leaks.
 * 2. Only adds/removes glider components on chestplates tagged with {@code curiospaper:glider_granted}.
 * 3. Never touches chestplates that already possess flight naturally or via other plugins.
 * 4. Transparently routes flight durability damage to the back-slot elytra while gliding.
 */
public class ElytraBackSlotHandler implements Listener {

    public record BackElytraSlot(int index, ItemStack item) {}

    private final CuriosPaper plugin;
    private final NamespacedKey gliderGrantedKey;
    private final NamespacedKey secretElytraKey;
    private final Set<UUID> playersWithSecretElytra = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastFlightDamageTime = new ConcurrentHashMap<>();

    public ElytraBackSlotHandler(CuriosPaper plugin) {
        this.plugin = plugin;
        this.gliderGrantedKey = new NamespacedKey(plugin, "glider_granted");
        this.secretElytraKey = new NamespacedKey(plugin, "secret_elytra");
        startFlightDurabilityTask();
    }

    public boolean isFeatureEnabled() {
        return plugin.getConfig().getBoolean("features.allow-elytra-on-back-slot", true);
    }

    public boolean isChestplate(Material mat) {
        return mat != null && mat.name().endsWith("_CHESTPLATE");
    }

    public String resolveChestplateWingsAsset(Material type) {
        if (!isChestplate(type)) return null;
        String base = type.name().toLowerCase(Locale.ROOT).replace("_chestplate", "");
        return "elytra_" + base + "_chestplate";
    }

    public boolean isEligibleElytra(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (item.getType() == Material.ELYTRA) return true;
        return VersionUtil.hasGlider(item);
    }

    public BackElytraSlot findBackSlotElytra(Player player) {
        if (player == null) return null;
        List<ItemStack> items = plugin.getCuriosPaperAPI().getEquippedItems(player, "back");
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (isEligibleElytra(item)) {
                return new BackElytraSlot(i, item);
            }
        }
        return null;
    }

    public ItemStack getBackSlotElytra(Player player) {
        BackElytraSlot slot = findBackSlotElytra(player);
        return slot != null ? slot.item() : null;
    }

    public void setBackSlotElytra(Player player, int index, ItemStack item) {
        plugin.getCuriosPaperAPI().setEquippedItem(player, "back", index, item);
    }

    public void setBackSlotElytra(Player player, ItemStack item) {
        BackElytraSlot slot = findBackSlotElytra(player);
        int index = slot != null ? slot.index() : 0;
        setBackSlotElytra(player, index, item);
    }

    public boolean isSecretElytra(ItemStack item) {
        if (item == null || item.getType() != Material.ELYTRA || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(secretElytraKey, PersistentDataType.BYTE);
    }

    public boolean hasGliderGranted(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(gliderGrantedKey, PersistentDataType.BYTE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAccessoryEquip(AccessoryEquipEvent event) {
        if (!isFeatureEnabled() || !"back".equalsIgnoreCase(event.getSlotType())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItem();
        ItemStack prevItem = event.getPreviousItem();

        if (newItem != null && isEligibleElytra(newItem)) {
            handleElytraEquipped(player, newItem);
        } else if (prevItem != null && isEligibleElytra(prevItem)) {
            handleElytraUnequipped(player);
        }
    }

    public void handleElytraEquipped(Player player) {
        handleElytraEquipped(player, null);
    }

    public void handleElytraEquipped(Player player, ItemStack backStack) {
        if (!isFeatureEnabled() || !player.isOnline()) return;

        ItemStack back = backStack != null ? backStack : getBackSlotElytra(player);
        if (back == null) return;

        // Check if back-slot elytra is broken (durability >= max - 1)
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta instanceof Damageable dmg) {
            int max = VersionUtil.getMaxDurability(back);
            if (dmg.getDamage() >= (max - 1)) {
                return;
            }
        }

        ItemStack chestplate = player.getInventory().getChestplate();

        if (chestplate != null && chestplate.getType() != Material.AIR && isChestplate(chestplate.getType())) {
            // Player already has a chestplate:
            // Check if chestplate ALREADY has flight naturally or from another plugin and is not managed by us
            if (VersionUtil.hasGlider(chestplate) && !hasGliderGranted(chestplate)) {
                return;
            }

            applyGliderToChestplate(player, chestplate, back);
        } else if (chestplate == null || chestplate.getType() == Material.AIR) {
            equipSecretElytra(player, back);
        }
    }

    public void applyGliderToChestplate(Player player, ItemStack chestplate, ItemStack backElytra) {
        if (chestplate == null || chestplate.getType().isAir() || !isChestplate(chestplate.getType())) return;

        // Tag that CuriosPaper granted glider
        ItemMeta meta = chestplate.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(gliderGrantedKey, PersistentDataType.BYTE, (byte) 1);
            chestplate.setItemMeta(meta);
        }

        // Check if back slot Elytra has a custom equippable asset ID
        NamespacedKey customAsset = VersionUtil.getEquippableAsset(backElytra);
        if (customAsset != null && !(customAsset.getNamespace().equalsIgnoreCase("minecraft") && customAsset.getKey().equalsIgnoreCase("elytra"))) {
            String base = chestplate.getType().name().toLowerCase(Locale.ROOT).replace("_chestplate", "");
            String sanitized = VersionUtil.sanitizeAssetKey(customAsset.getNamespace(), customAsset.getKey());
            String combinedAsset = "elytra_" + base + "_" + sanitized;
            VersionUtil.applyElytraFlight(chestplate, "curiospaper", combinedAsset);
        } else {
            // Resolve custom wings asset ID by chestplate material (e.g. elytra_diamond_chestplate)
            String assetId = resolveChestplateWingsAsset(chestplate.getType());
            if (assetId != null) {
                VersionUtil.applyElytraFlight(chestplate, "curiospaper", assetId);
            } else {
                VersionUtil.applyElytraFlight(chestplate, "minecraft", "elytra");
            }
        }

        player.getInventory().setChestplate(chestplate);
        player.updateInventory();
    }

    public void handleElytraUnequipped(Player player) {
        if (!player.isOnline()) return;

        // 1. Remove secret elytra if worn
        removeSecretElytra(player);

        // 2. Check chestplate: ONLY strip glider if CuriosPaper granted it
        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && isChestplate(chest.getType())) {
            ItemMeta meta = chest.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc.has(gliderGrantedKey, PersistentDataType.BYTE)) {
                    pdc.remove(gliderGrantedKey);
                    chest.setItemMeta(meta);
                    VersionUtil.removeGlider(chest);
                    player.getInventory().setChestplate(chest);
                }
            }
        }

        wipeSecretElytra(player);
    }

    public void equipSecretElytra(Player player) {
        equipSecretElytra(player, null);
    }

    public void equipSecretElytra(Player player, ItemStack backElytra) {
        removeSecretElytra(player);

        ItemStack fakeElytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta = fakeElytra.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(secretElytraKey, PersistentDataType.BYTE, (byte) 1);
            VersionUtil.setItemModelSafe(meta, "curiospaper:invisible", null);

            // Copy enchantments from the back-slot elytra so vanilla simulation respects Unbreaking/Mending
            if (backElytra != null && backElytra.hasItemMeta()) {
                ItemMeta bMeta = backElytra.getItemMeta();
                if (bMeta.hasEnchants()) {
                    for (Map.Entry<Enchantment, Integer> entry : bMeta.getEnchants().entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                }
            }
            fakeElytra.setItemMeta(meta);
        }

        if (backElytra != null) {
            NamespacedKey customAsset = VersionUtil.getEquippableAsset(backElytra);
            if (customAsset != null && !(customAsset.getNamespace().equalsIgnoreCase("minecraft") && customAsset.getKey().equalsIgnoreCase("elytra"))) {
                VersionUtil.applyElytraFlight(fakeElytra, customAsset.getNamespace(), customAsset.getKey());
            }
        }

        player.getInventory().setChestplate(fakeElytra);
        playersWithSecretElytra.add(player.getUniqueId());
        player.updateInventory();
    }

    public void removeSecretElytra(Player player) {
        playersWithSecretElytra.remove(player.getUniqueId());
        ItemStack chest = player.getInventory().getChestplate();
        if (chest != null && isSecretElytra(chest)) {
            player.getInventory().setChestplate(null);
        }
    }

    /**
     * Synchronously intercepts right-clicking with a chestplate in hand when the player is wearing secret elytra.
     * Cancelling the interact event stops vanilla from swapping the secret elytra into the player's hand.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isFeatureEnabled()) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !isChestplate(item.getType())) return;

        Player player = event.getPlayer();
        ItemStack chest = player.getInventory().getChestplate();
        if (!isSecretElytra(chest) && !playersWithSecretElytra.contains(player.getUniqueId())) {
            return;
        }

        // Cancel vanilla right-click armor swap
        event.setCancelled(true);

        // Equip chestplate synchronously
        ItemStack toEquip = item.clone();
        toEquip.setAmount(1);

        // Deduct from hand
        if (event.getHand() == EquipmentSlot.HAND) {
            ItemStack main = player.getInventory().getItemInMainHand();
            if (main.getAmount() > 1) {
                main.setAmount(main.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off.getAmount() > 1) {
                off.setAmount(off.getAmount() - 1);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }

        // Remove secret elytra tracking
        playersWithSecretElytra.remove(player.getUniqueId());
        wipeSecretElytra(player);

        // Apply glider capability if back slot elytra is active
        ItemStack back = getBackSlotElytra(player);
        if (back != null) {
            applyGliderToChestplate(player, toEquip, back);
        } else {
            player.getInventory().setChestplate(toEquip);
        }

        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
        player.updateInventory();
    }

    /**
     * Intercepts inventory clicks on chest armor slot (slot 38) and shift-clicks to prevent fake elytra leaks.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isFeatureEnabled() || !(event.getWhoClicked() instanceof Player player)) return;

        // Check secret elytra first
        ItemStack currentChest = player.getInventory().getChestplate();
        boolean hasSecret = isSecretElytra(currentChest) || playersWithSecretElytra.contains(player.getUniqueId());

        // Ensure clicked or cursor elytras have back-slot tag and lore
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() == Material.ELYTRA && !isSecretElytra(clickedItem)) {
            ItemStack updated = ensureBackTaggedElytra(clickedItem);
            if (updated != clickedItem) {
                event.setCurrentItem(updated);
            }
        }
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && cursorItem.getType() == Material.ELYTRA && !isSecretElytra(cursorItem)) {
            ItemStack updated = ensureBackTaggedElytra(cursorItem);
            if (updated != cursorItem) {
                event.getView().setCursor(updated);
            }
        }

        // 1. Player clicks on chestplate slot (38)
        if (event.getSlot() == 38 && event.getClickedInventory() == player.getInventory()) {
            if (hasSecret) {
                ClickType click = event.getClick();
                ItemStack cursor = event.getCursor();

                // Block empty hand pickup, shift-click, offhand swap, and dropping
                if (cursor == null || cursor.getType().isAir()) {
                    event.setCancelled(true);
                    return;
                }

                if (click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT ||
                    click == ClickType.DROP || click == ClickType.CONTROL_DROP ||
                    click == ClickType.SWAP_OFFHAND) {
                    event.setCancelled(true);
                    return;
                }

                if (click == ClickType.NUMBER_KEY) {
                    int hotbarSlot = event.getHotbarButton();
                    if (hotbarSlot >= 0 && hotbarSlot < 9) {
                        ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                        if (hotbarItem != null && isChestplate(hotbarItem.getType())) {
                            event.setCancelled(true);
                            ItemStack toEquip = hotbarItem.clone();
                            player.getInventory().setItem(hotbarSlot, null);
                            playersWithSecretElytra.remove(player.getUniqueId());
                            wipeSecretElytra(player);

                            ItemStack backElytra = getBackSlotElytra(player);
                            if (backElytra != null) {
                                applyGliderToChestplate(player, toEquip, backElytra);
                            } else {
                                player.getInventory().setChestplate(toEquip);
                            }
                            player.updateInventory();
                            return;
                        }
                    }
                    event.setCancelled(true);
                    return;
                }

                // If cursor is a chestplate, swap cleanly
                if (isChestplate(cursor.getType())) {
                    event.setCancelled(true);
                    ItemStack toEquip = cursor.clone();
                    toEquip.setAmount(1);

                    if (cursor.getAmount() > 1) {
                        cursor.setAmount(cursor.getAmount() - 1);
                    } else {
                        player.setItemOnCursor(null);
                    }

                    playersWithSecretElytra.remove(player.getUniqueId());
                    wipeSecretElytra(player);

                    ItemStack backElytra = getBackSlotElytra(player);
                    if (backElytra != null) {
                        applyGliderToChestplate(player, toEquip, backElytra);
                    } else {
                        player.getInventory().setChestplate(toEquip);
                    }
                    player.updateInventory();
                    return;
                }

                event.setCancelled(true);
            } else {
                // Player clicked slot 38 with a normal chestplate:
                // Check if they are removing a chestplate managed by CuriosPaper
                if (currentChest != null && hasGliderGranted(currentChest)) {
                    ItemMeta meta = currentChest.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().remove(gliderGrantedKey);
                        currentChest.setItemMeta(meta);
                    }
                    VersionUtil.removeGlider(currentChest);
                }

                // If slot becomes empty after interaction, re-equip secret elytra; if chestplate, apply glider
                PlatformScheduler.get().runEntity(player, () -> {
                    if (player.isOnline()) {
                        ItemStack backElytra = getBackSlotElytra(player);
                        if (backElytra != null) {
                            ItemStack nowChest = player.getInventory().getChestplate();
                            if (nowChest == null || nowChest.getType().isAir()) {
                                equipSecretElytra(player, backElytra);
                            } else if (isChestplate(nowChest.getType())) {
                                applyGliderToChestplate(player, nowChest, backElytra);
                            }
                        }
                    }
                });
            }
        }
        // 2. Shift-clicking a chestplate into slot 38 while wearing secret elytra
        else if (event.isShiftClick() && hasSecret) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && isChestplate(clicked.getType())) {
                event.setCancelled(true);
                ItemStack toEquip = clicked.clone();
                toEquip.setAmount(1);

                if (clicked.getAmount() > 1) {
                    clicked.setAmount(clicked.getAmount() - 1);
                } else {
                    event.setCurrentItem(null);
                }

                playersWithSecretElytra.remove(player.getUniqueId());
                wipeSecretElytra(player);

                ItemStack backElytra = getBackSlotElytra(player);
                if (backElytra != null) {
                    applyGliderToChestplate(player, toEquip, backElytra);
                } else {
                    player.getInventory().setChestplate(toEquip);
                }
                player.updateInventory();
            }
        }
        // General shift-click to ensure chestplate gets flight immediately when placed into slot 38
        else if (event.isShiftClick()) {
            PlatformScheduler.get().runEntity(player, () -> {
                if (player.isOnline()) {
                    ItemStack backElytra = getBackSlotElytra(player);
                    if (backElytra != null) {
                        ItemStack chest = player.getInventory().getChestplate();
                        if (chest != null && isChestplate(chest.getType()) && !hasGliderGranted(chest)) {
                            applyGliderToChestplate(player, chest, backElytra);
                        } else if ((chest == null || chest.getType().isAir()) && !playersWithSecretElytra.contains(player.getUniqueId())) {
                            equipSecretElytra(player, backElytra);
                        }
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isFeatureEnabled() || !(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlots().contains(38)) {
            ItemStack chest = player.getInventory().getChestplate();
            if (isSecretElytra(chest) || playersWithSecretElytra.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (!isFeatureEnabled()) return;
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isSecretElytra(dropped)) {
            event.setCancelled(true);
            event.getItemDrop().remove();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (!isFeatureEnabled()) return;

        Player player = event.getPlayer();
        if (!player.isGliding()) {
            return;
        }

        ItemStack damaged = event.getItem();
        if (damaged == null) return;

        boolean isGliderChest = isChestplate(damaged.getType()) && (hasGliderGranted(damaged) || VersionUtil.hasGlider(damaged));
        boolean isSecret = isSecretElytra(damaged) || (damaged.getType() == Material.ELYTRA && playersWithSecretElytra.contains(player.getUniqueId()));

        if (!isGliderChest && !isSecret) return;

        BackElytraSlot backSlot = findBackSlotElytra(player);
        if (backSlot == null) return;

        event.setCancelled(true); // Don't damage the chestplate or secret elytra

        applyFlightDamage(player, backSlot, event.getDamage());
    }

    public void applyFlightDamage(Player player, BackElytraSlot backSlot, int amount) {
        if (player == null || backSlot == null || amount <= 0) return;
        lastFlightDamageTime.put(player.getUniqueId(), System.currentTimeMillis());

        ItemStack backElytra = backSlot.item();
        ItemMeta meta = backElytra.getItemMeta();
        if (meta instanceof Damageable dmg) {
            int current = dmg.getDamage();
            int max = VersionUtil.getMaxDurability(backElytra);
            int maxUsable = max - 1;

            int next = Math.min(maxUsable, current + amount);
            dmg.setDamage(next);
            backElytra.setItemMeta(meta);
            setBackSlotElytra(player, backSlot.index(), backElytra);

            if (next >= maxUsable) {
                handleElytraUnequipped(player);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                plugin.getLogger().info("Back-slot elytra for " + player.getName()
                        + " reached 1 durability; gliding disabled but item kept.");
            }
        }
    }

    private void startFlightDurabilityTask() {
        PlatformScheduler.get().runGlobalTimer(() -> {
            if (!isFeatureEnabled()) return;

            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlatformScheduler.get().runEntity(player, () -> {
                    if (!player.isOnline() || !player.isGliding()) return;

                    Long lastTime = lastFlightDamageTime.get(player.getUniqueId());
                    if (lastTime != null && (now - lastTime) < 950) {
                        return; // Already processed via PlayerItemDamageEvent this second
                    }

                    BackElytraSlot backSlot = findBackSlotElytra(player);
                    if (backSlot == null) return;

                    ItemStack chest = player.getInventory().getChestplate();
                    boolean isManaged = (chest != null && isChestplate(chest.getType()) && (hasGliderGranted(chest) || VersionUtil.hasGlider(chest)))
                            || (chest != null && isSecretElytra(chest))
                            || playersWithSecretElytra.contains(player.getUniqueId());

                    if (!isManaged) return;

                    // Handle unbreaking enchantment calculation if present
                    int unbreakingLevel = backSlot.item().getEnchantmentLevel(Enchantment.UNBREAKING);
                    if (unbreakingLevel > 0) {
                        double chance = 0.6 + (0.4 / (unbreakingLevel + 1.0));
                        if (Math.random() > chance) {
                            lastFlightDamageTime.put(player.getUniqueId(), now);
                            return;
                        }
                    }

                    applyFlightDamage(player, backSlot, 1);
                });
            }
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        playersWithSecretElytra.remove(player.getUniqueId());

        Iterator<ItemStack> it = event.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack drop = it.next();
            if (drop == null || drop.getType() == Material.AIR) continue;

            if (isSecretElytra(drop)) {
                it.remove(); // Never drop fake secret elytra
            } else if (hasGliderGranted(drop)) {
                ItemMeta meta = drop.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().remove(gliderGrantedKey);
                    drop.setItemMeta(meta);
                }
                VersionUtil.removeGlider(drop);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastFlightDamageTime.remove(player.getUniqueId());
        if (playersWithSecretElytra.remove(player.getUniqueId())) {
            ItemStack chest = player.getInventory().getChestplate();
            if (isSecretElytra(chest)) {
                player.getInventory().setChestplate(null);
            }
        }
        wipeSecretElytra(player);
    }

    public boolean hasBackSlotLore(ItemMeta meta) {
        if (meta == null || !meta.hasLore() || meta.lore() == null) return false;
        for (net.kyori.adventure.text.Component line : meta.lore()) {
            String plain = org.bg52.curiospaper.util.TextUtil.stripAll(org.bg52.curiospaper.util.TextUtil.toLegacy(line)).toLowerCase(Locale.ROOT);
            if (plain.startsWith("slot:") || plain.startsWith("required slot:")) {
                return true;
            }
        }
        return false;
    }

    public ItemStack ensureBackTaggedElytra(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ELYTRA || !isFeatureEnabled()) {
            return stack;
        }
        if (isSecretElytra(stack)) {
            return stack;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String existingSlot = pdc.get(plugin.getCuriosPaperAPI().getSlotTypeKey(), PersistentDataType.STRING);
            if ("back".equalsIgnoreCase(existingSlot) && hasBackSlotLore(meta)) {
                return stack;
            }
        }

        return plugin.getCuriosPaperAPI().tagAccessoryItem(stack, "back");
    }

    public void retagAllPlayerElytras(Player player) {
        if (!isFeatureEnabled() || player == null || !player.isOnline()) return;

        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack original = contents[i];
            if (original != null && original.getType() == Material.ELYTRA && !isSecretElytra(original)) {
                ItemStack updated = ensureBackTaggedElytra(original);
                if (updated != original) {
                    contents[i] = updated;
                    changed = true;
                }
            }
        }

        if (changed) {
            player.getInventory().setContents(contents);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onElytraPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!isFeatureEnabled() || !(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();
        if (stack == null || stack.getType() != Material.ELYTRA) return;

        ItemStack tagged = ensureBackTaggedElytra(stack);
        if (tagged != stack) {
            event.getItem().setItemStack(tagged);
        }

        PlatformScheduler.get().runEntity(player, () -> {
            if (player.isOnline()) {
                retagAllPlayerElytras(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!isFeatureEnabled() || !(event.getPlayer() instanceof Player player)) return;
        retagAllPlayerElytras(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareCraft(org.bukkit.event.inventory.PrepareItemCraftEvent event) {
        if (!isFeatureEnabled()) return;
        ItemStack result = event.getInventory().getResult();
        if (result != null && result.getType() == Material.ELYTRA && !isSecretElytra(result)) {
            event.getInventory().setResult(ensureBackTaggedElytra(result));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isFeatureEnabled()) {
            retagAllPlayerElytras(player);
            if (getBackSlotElytra(player) != null) {
                handleElytraEquipped(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlatformScheduler.get().runEntityLater(player, () -> {
            if (player.isOnline() && isFeatureEnabled()) {
                retagAllPlayerElytras(player);
                if (getBackSlotElytra(player) != null) {
                    handleElytraEquipped(player);
                }
            }
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (isFeatureEnabled() && getBackSlotElytra(player) != null) {
            handleElytraEquipped(player);
        }
    }

    public void wipeSecretElytra(Player player) {
        // Only wipe secret elytra; never remove gliders from untagged chestplates
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            if (isSecretElytra(contents[i])) {
                contents[i] = null;
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setContents(contents);
        }

        ItemStack cursor = player.getItemOnCursor();
        if (isSecretElytra(cursor)) {
            player.setItemOnCursor(null);
        }
    }
}
