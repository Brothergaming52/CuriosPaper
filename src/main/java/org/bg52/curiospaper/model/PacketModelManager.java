package org.bg52.curiospaper.model;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.scheduler.PlatformScheduler;
import org.bg52.curiospaper.api.event.CuriosMobModelEquipEvent;
import org.bg52.curiospaper.api.event.CuriosModelEquipEvent;
import org.bg52.curiospaper.inventory.PlayerCuriosInventory;
import org.bg52.curiospaper.model.packet.PacketArmorStand;
import org.bg52.curiospaper.model.packet.PacketSender;
import org.bg52.curiospaper.slot.SlotDefinition;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Universal 3D Model System supporting both high-performance Packet-Level Armor Stands
 * and Universal Non-Persistent Marker Entity Armor Stands.
 * <p>
 * Guarantees that 3D models NEVER get disabled on any server platform (Spigot, Paper 1.20.6 - 26.x+, Folia, Purpur).
 * If packet reflection is unavailable or fails, it automatically and seamlessly operates with the
 * Universal Non-Persistent Entity Engine.
 */
public class PacketModelManager implements Listener {

    private final CuriosPaper plugin;

    // Mode flags
    private boolean enabled = true;
    private boolean usePacketMode = false;
    private double trackingDistance = 48.0;

    // =========================================================================
    // Packet Mode State
    // =========================================================================
    // Player UUID -> (SlotKey -> PacketArmorStand)
    private final Map<UUID, Map<String, PacketArmorStand>> activeStands = new ConcurrentHashMap<>();
    // Player UUID -> Set of viewer UUIDs
    private final Map<UUID, Set<UUID>> currentViewers = new ConcurrentHashMap<>();

    // Mob UUID -> Set of viewer UUIDs
    private final Map<UUID, Set<UUID>> mobViewers = new ConcurrentHashMap<>();

    // Mob UUID -> (SlotKey -> PacketArmorStand)
    private final Map<UUID, Map<String, PacketArmorStand>> activeMobStands = new ConcurrentHashMap<>();
    private final Map<UUID, LivingEntity> activeMobs = new ConcurrentHashMap<>();

    // =========================================================================
    // Universal Entity Mode State (Fallback & Platform Universal)
    // =========================================================================
    // Player UUID -> (SlotKey -> ArmorStand)
    private final Map<UUID, Map<String, ArmorStand>> activeEntityStands = new ConcurrentHashMap<>();
    // Player UUID -> (SlotKey -> StandMeta)
    private final Map<UUID, Map<String, StandMeta>> activeEntityMeta = new ConcurrentHashMap<>();

    // Mob UUID -> (SlotKey -> ArmorStand)
    private final Map<UUID, Map<String, ArmorStand>> activeMobEntityStands = new ConcurrentHashMap<>();
    private final Map<UUID, LivingEntity> activeEntityMobs = new ConcurrentHashMap<>();

    // Player UUID -> Timestamp of last hide tick (for 250ms smoothing)
    private final Map<UUID, Long> lastHideTickState = new ConcurrentHashMap<>();

    // Player UUID -> Pending scheduled rescan task (for debouncing teleports, world changes, respawns)
    private final Map<UUID, PlatformScheduler.TaskHandle> pendingRescans = new ConcurrentHashMap<>();
    // Player UUIDs currently in transit during a teleport sequence
    private final Set<UUID> teleportingPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Scale compensation multiplier applied to armor stand models when a Bedrock player sneaks.
     * Bedrock Edition applies a client-side sneak animation that lowers the player model,
     * while the server-side passenger mount also lowers by ~0.3 blocks. Scaling the passenger
     * armor stand by ~1.176 offsets the head and torso upwards by ~0.3 blocks, keeping the
     * 3D model properly aligned with the Bedrock player's body and preventing it from sinking.
     */
    public static final double BEDROCK_SNEAK_SCALE_MULTIPLIER = 1.176;

    public static double resolveModelScale(Player player) {
        return resolveModelScale(player, player != null && player.isSneaking());
    }

    public static double resolveModelScale(Player player, boolean isSneaking) {
        double scale = PacketArmorStand.resolveEntityScale(player);
        if (player != null && isSneaking && org.bg52.curiospaper.util.BedrockUtil.isBedrockPlayer(player)) {
            scale *= BEDROCK_SNEAK_SCALE_MULTIPLIER;
        }
        return scale;
    }

    public static double resolveModelScale(LivingEntity entity) {
        if (entity instanceof Player player) {
            return resolveModelScale(player);
        }
        return PacketArmorStand.resolveEntityScale(entity);
    }

    private static class StandMeta {
        ItemStack item;
        Float pitchUpLimit;
        Float pitchDownLimit;
        boolean hiddenFromWearer = false;
        boolean hiddenFromAll = false;
    }

    public PacketModelManager(CuriosPaper plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.enabled = plugin.getConfig().getBoolean("models.enabled", true);
        this.trackingDistance = plugin.getConfig().getDouble("models.tracking-distance", 48.0);
        String configMode = plugin.getConfig().getString("models.mode", "auto").trim().toLowerCase(Locale.ROOT);

        if (!enabled) {
            plugin.getLogger().info("[ModelEngine] 3D Model Engine is disabled in config.");
            return;
        }

        PacketSender.init();

        if ("packet".equalsIgnoreCase(configMode)) {
            if (PacketSender.isAvailable()) {
                this.usePacketMode = true;
                plugin.getLogger().info("[ModelEngine] Enhanced Packet-Level 3D Model Engine initialized!");
            } else {
                this.usePacketMode = false;
                plugin.getLogger().warning("[ModelEngine] Packet reflection unavailable (" + PacketSender.getFailureReason() + "). Seamlessly using Universal Non-Persistent Entity Engine. 3D models are fully active!");
            }
        } else if ("entity".equalsIgnoreCase(configMode)) {
            this.usePacketMode = false;
            plugin.getLogger().info("[ModelEngine] Universal Non-Persistent Entity Engine initialized! 3D models are fully active.");
        } else {
            // "auto" mode:
            // When Geyser or Floodgate is detected on the server, universal entity mode is used
            // so Geyser can translate Bukkit passenger ArmorStands for Bedrock players seamlessly.
            boolean bedrock = org.bg52.curiospaper.util.BedrockUtil.isBedrockPresent();
            if (PacketSender.isAvailable() && !bedrock) {
                this.usePacketMode = true;
                plugin.getLogger().info("[ModelEngine] Enhanced Packet-Level 3D Model Engine initialized!");
            } else {
                this.usePacketMode = false;
                plugin.getLogger().info("[ModelEngine] Universal Non-Persistent Entity Engine active (Geyser/Bedrock compatibility). 3D models are fully functional!");
            }
        }

        if (this.usePacketMode) {
            // Periodic tracking & rotation sync for players and mobs in packet mode
            PlatformScheduler.get().runAsyncTimer(() -> {
                syncPlayerViewers();
                syncMobViewers();
            }, 20L, 20L);
        } else {
            // Periodic passenger safety check & rotation sync in entity mode
            PlatformScheduler.get().runGlobalTimer(() -> {
                validateEntityPassengers();
                syncEntityMobRotations();
            }, 40L, 40L);
        }

        // Continuous scale synchronization & anti-ejection monitor (every 5 ticks)
        PlatformScheduler.get().runGlobalTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlatformScheduler.get().runEntity(player, () -> {
                    if (!player.isOnline()) return;
                    updatePlayerVisibility(player);
                    double currentScale = resolveModelScale(player);
                    if (usePacketMode) {
                        Map<String, PacketArmorStand> stands = activeStands.get(player.getUniqueId());
                        if (stands != null && !stands.isEmpty()) {
                            boolean scaleChanged = false;
                            for (PacketArmorStand stand : stands.values()) {
                                if (Math.abs(currentScale - stand.getLastScale()) >= 0.001) {
                                    stand.setLastScale(currentScale);
                                    scaleChanged = true;
                                    Set<UUID> viewers = currentViewers.get(player.getUniqueId());
                                    if (viewers != null) {
                                        for (UUID vId : viewers) {
                                            Player v = Bukkit.getPlayer(vId);
                                            if (v != null && v.isOnline()) {
                                                PacketSender.sendEntityScale(v, stand.getEntityId(), currentScale);
                                            }
                                        }
                                    }
                                }
                            }
                            if (scaleChanged) {
                                updatePassengersForPlayer(player);
                            }
                        }
                    } else {
                        Map<String, ArmorStand> stands = activeEntityStands.get(player.getUniqueId());
                        if (stands != null && !stands.isEmpty()) {
                            boolean scaleChanged = false;
                            for (ArmorStand stand : stands.values()) {
                                if (stand != null && stand.isValid()) {
                                    applyEntityScale(stand, currentScale);
                                    if (!player.getPassengers().contains(stand)) {
                                        scaleChanged = true;
                                    }
                                }
                            }
                            if (scaleChanged) {
                                updateEntityPassengersForPlayer(player);
                            }
                        }
                    }
                });
            }
        }, 5L, 5L);
    }

    public void shutdown() {
        // Despawn all player stands
        for (UUID ownerUUID : activeStands.keySet()) {
            despawnAllForPlayer(ownerUUID);
        }
        for (UUID ownerUUID : activeEntityStands.keySet()) {
            despawnAllForPlayer(ownerUUID);
        }

        // Despawn all mob stands
        for (LivingEntity mob : activeMobs.values()) {
            removeAllMobModels(mob);
        }
        for (LivingEntity mob : activeEntityMobs.values()) {
            removeAllMobModels(mob);
        }

        activeStands.clear();
        currentViewers.clear();
        activeMobStands.clear();
        mobViewers.clear();
        activeMobs.clear();

        activeEntityStands.clear();
        activeEntityMeta.clear();
        activeMobEntityStands.clear();
        activeEntityMobs.clear();
        lastHideTickState.clear();

        for (PlatformScheduler.TaskHandle task : pendingRescans.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        pendingRescans.clear();
        teleportingPlayers.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUsingPacketMode() {
        return usePacketMode;
    }

    /**
     * Determines whether an equipped accessory should render a separate 3D model.
     * By default, vanilla items (like Elytras) and unconfigured accessories DO NOT render
     * 3D armor stands. Only items with 3D models explicitly enabled or separate model data render.
     */
    public boolean shouldRender3DModel(ItemStack equipped, SlotDefinition def) {
        if (equipped == null || equipped.getType().isAir()) {
            return false;
        }

        // 1. Explicit PDC tag: curiospaper:render_model
        String renderTag = resolveItemTag(equipped, "curiospaper:render_model");
        if ("false".equalsIgnoreCase(renderTag)) {
            return false;
        }
        if ("true".equalsIgnoreCase(renderTag)) {
            return true;
        }

        // 2. Check if separate 3D model properties are explicitly attached to the item
        String modelItemStr = resolveItemTag(equipped, "curiospaper:model_item");
        if (modelItemStr != null && !modelItemStr.trim().isEmpty() && !modelItemStr.equalsIgnoreCase("DEFAULT")) {
            return true;
        }
        String cmdStr = resolveItemTag(equipped, "curiospaper:model_cmd");
        if (cmdStr != null && !cmdStr.trim().isEmpty() && !cmdStr.equalsIgnoreCase("DEFAULT")) {
            return true;
        }
        String itemModelStr = resolveItemTag(equipped, "curiospaper:model_item_model");
        if (itemModelStr != null && !itemModelStr.trim().isEmpty() && !itemModelStr.equalsIgnoreCase("DEFAULT")) {
            return true;
        }

        // 3. Check item engine integration (e.g. Synthetix custom curios item with 3d model enabled)
        if (plugin.getItemEngineManager() != null && plugin.getItemEngineManager().shouldRenderModel(equipped)) {
            return true;
        }

        // Standard items (including vanilla Elytra) do NOT render 3D armor stands
        return false;
    }

    /**
     * Re-evaluates equipped curios accessories for a player and synchronizes 3D models.
     */
    public void updatePlayerAccessories(Player player) {
        if (!enabled || player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        PlayerCuriosInventory inv = plugin.getStorageManager().getInventory(uuid);
        if (inv == null) return;

        Set<String> activeSlotKeys = new HashSet<>();
        Set<String> activeCurioTags = new HashSet<>();

        // Maps for current mode
        Map<String, PacketArmorStand> playerPacketStands = usePacketMode ? activeStands.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()) : null;
        Map<String, ArmorStand> playerEntityStands = !usePacketMode ? activeEntityStands.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()) : null;
        Map<String, StandMeta> playerEntityMetas = !usePacketMode ? activeEntityMeta.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()) : null;

        for (SlotDefinition def : plugin.getSlotRegistry().getAllSlots()) {
            int unlocked = inv.getUnlockedSlots(def, player);
            for (int i = 0; i < unlocked; i++) {
                String slotKey = def.getId() + ":" + i;
                ItemStack equipped = inv.getItem(def.getId(), i);

                if (equipped != null && !equipped.getType().isAir()) {
                    // Check if this item is configured to render a 3D model
                    if (!shouldRender3DModel(equipped, def)) {
                        continue;
                    }

                    // 2. Resolve separate 3D model material
                    Material modelMat = null;
                    String modelItemStr = resolveItemTag(equipped, "curiospaper:model_item");
                    if (modelItemStr != null && !modelItemStr.trim().isEmpty() && !modelItemStr.equalsIgnoreCase("DEFAULT")) {
                        modelMat = Material.matchMaterial(modelItemStr.trim().toUpperCase(Locale.ROOT));
                    }
                    if (modelMat == null) {
                        modelMat = equipped.getType();
                    }

                    // 3. Resolve separate 3D CustomModelData (prioritize 3D model tag if set, otherwise fallback to item)
                    Integer cmd = null;
                    String cmdStr = resolveItemTag(equipped, "curiospaper:model_cmd");
                    if (cmdStr != null && !cmdStr.trim().isEmpty() && !cmdStr.equalsIgnoreCase("DEFAULT")) {
                        try {
                            cmd = Integer.parseInt(cmdStr.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    if (cmd == null) {
                        cmd = VersionUtil.getCustomModelDataSafe(equipped.getItemMeta());
                    }
                    if (cmd == null) {
                        cmd = def.getCustomModelData();
                    }

                    // 4. Resolve separate 3D item model (prioritize 3D model tag if set, otherwise fallback to item)
                    String itemModel = null;
                    String modelTag = resolveItemTag(equipped, "curiospaper:model_item_model");
                    if (modelTag != null && !modelTag.trim().isEmpty() && !modelTag.equalsIgnoreCase("DEFAULT")) {
                        itemModel = modelTag;
                    }
                    if (itemModel == null || itemModel.trim().isEmpty()) {
                        itemModel = VersionUtil.getItemModelSafe(equipped.getItemMeta());
                    }
                    if (itemModel == null) {
                        itemModel = def.getItemModel();
                    }

                    // 5. Resolve pitch limits
                    Float pitchUp = resolvePitchLimit(equipped, "pitch_up_limit", def);
                    Float pitchDown = resolvePitchLimit(equipped, "pitch_down_limit", def);

                    // Fire CuriosModelEquipEvent allowing other plugins to customize or cancel
                    CuriosModelEquipEvent equipEvent = new CuriosModelEquipEvent(
                            player, equipped, def.getId(), i, modelMat, cmd, itemModel
                    );
                    Bukkit.getPluginManager().callEvent(equipEvent);

                    if (equipEvent.isCancelled()) {
                        continue;
                    }

                    activeSlotKeys.add(slotKey);
                    collectCurioTags(activeCurioTags, equipped, equipEvent, def);

                    // Build rendered helmet item
                    ItemStack renderedItem = new ItemStack(equipEvent.getModelMaterial());
                    ItemMeta meta = renderedItem.getItemMeta();
                    if (meta != null) {
                        VersionUtil.setItemModelSafe(meta, equipEvent.getItemModel(), equipEvent.getCustomModelData());
                        renderedItem.setItemMeta(meta);
                    }

                    // =========================================================
                    // Dispatch to Active Engine
                    // =========================================================
                    if (usePacketMode && playerPacketStands != null) {
                        PacketArmorStand existing = playerPacketStands.get(slotKey);
                        if (existing == null) {
                            PacketArmorStand stand = new PacketArmorStand(uuid, slotKey, renderedItem);
                            stand.setPitchUpLimit(pitchUp);
                            stand.setPitchDownLimit(pitchDown);
                            playerPacketStands.put(slotKey, stand);
                            spawnStandForNearby(stand, player);
                        } else {
                            existing.setItem(renderedItem);
                            existing.setPitchUpLimit(pitchUp);
                            existing.setPitchDownLimit(pitchDown);
                            updateStandForViewers(existing, uuid);
                        }
                    } else if (playerEntityStands != null && playerEntityMetas != null) {
                        float targetYaw = PacketArmorStand.isHeadSlot(slotKey) ? player.getLocation().getYaw() : getEntityBodyYaw(player);
                        ArmorStand existing = playerEntityStands.get(slotKey);
                        if (existing != null && existing.isValid() && existing.getWorld().equals(player.getWorld())) {
                            EntityEquipment eq = existing.getEquipment();
                            if (eq != null) {
                                eq.setHelmet(renderedItem);
                                if (isTorsoBackSlot(slotKey)) {
                                    eq.setChestplate(renderedItem);
                                }
                            }
                            applyEntityScale(existing, resolveModelScale(player));
                            existing.setRotation(targetYaw, 0.0f);
                            setEntityBodyYaw(existing, targetYaw);
                            existing.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
                            if (!player.getPassengers().contains(existing)) {
                                existing.teleport(player.getLocation());
                                player.addPassenger(existing);
                            }
                            StandMeta sm = playerEntityMetas.computeIfAbsent(slotKey, k -> new StandMeta());
                            sm.item = renderedItem;
                            sm.pitchUpLimit = pitchUp;
                            sm.pitchDownLimit = pitchDown;
                        } else {
                            if (existing != null && existing.isValid()) {
                                existing.remove();
                            }
                            World world = player.getWorld();
                            Location loc = player.getLocation();
                            ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
                                as.setVisible(false);
                                as.setBasePlate(false);
                                as.setGravity(false);
                                as.setInvulnerable(true);
                                as.setArms(false);
                                as.setSmall(false);
                                as.setMarker(true);
                                as.setSilent(true);
                                as.setPersistent(false);
                                as.setCanPickupItems(false);
                                try { as.setCanTick(false); } catch (Throwable ignored) {}
                                try { as.setCollidable(false); } catch (Throwable ignored) {}
                            });

                            applyEntityScale(stand, resolveModelScale(player));

                            EntityEquipment eq = stand.getEquipment();
                            if (eq != null) {
                                eq.setHelmet(renderedItem);
                                if (isTorsoBackSlot(slotKey)) {
                                    eq.setChestplate(renderedItem);
                                }
                            }

                            stand.setRotation(targetYaw, 0.0f);
                            setEntityBodyYaw(stand, targetYaw);
                            stand.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));

                            player.addPassenger(stand);
                            playerEntityStands.put(slotKey, stand);

                            StandMeta sm = playerEntityMetas.computeIfAbsent(slotKey, k -> new StandMeta());
                            sm.item = renderedItem;
                            sm.pitchUpLimit = pitchUp;
                            sm.pitchDownLimit = pitchDown;
                            sm.hiddenFromWearer = false;
                        }
                    }
                }
            }
        }

        // Remove unequipped stands
        if (usePacketMode && playerPacketStands != null) {
            Iterator<Map.Entry<String, PacketArmorStand>> it = playerPacketStands.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, PacketArmorStand> entry = it.next();
                if (!activeSlotKeys.contains(entry.getKey())) {
                    destroyStandForViewers(entry.getValue(), uuid);
                    it.remove();
                }
            }
        } else if (playerEntityStands != null && playerEntityMetas != null) {
            Iterator<Map.Entry<String, ArmorStand>> it = playerEntityStands.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ArmorStand> entry = it.next();
                if (!activeSlotKeys.contains(entry.getKey())) {
                    if (entry.getValue() != null && entry.getValue().isValid()) {
                        player.removePassenger(entry.getValue());
                        entry.getValue().remove();
                    }
                    it.remove();
                    playerEntityMetas.remove(entry.getKey());
                }
            }
        }

        if (usePacketMode) {
            updatePassengersForPlayer(player);
        } else {
            updateEntityPassengersForPlayer(player);
        }

        // Immediately evaluate visibility and pitch limits so models don't glitch on equip
        updatePlayerVisibility(player);

        // Synchronize curio scoreboard tags on the Bukkit player for Bedrock client bone attachment
        syncPlayerScoreboardTags(player, activeCurioTags);
    }

    public void updatePassengersForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        Map<String, PacketArmorStand> stands = activeStands.get(uuid);
        int vehicleId = player.getEntityId();
        if (stands == null || stands.isEmpty()) {
            PacketSender.sendSetPassengers(player, vehicleId, new int[0]);
            Set<UUID> viewers = currentViewers.get(uuid);
            if (viewers != null) {
                for (UUID vId : viewers) {
                    Player v = Bukkit.getPlayer(vId);
                    if (v != null && v.isOnline()) {
                        PacketSender.sendSetPassengers(v, vehicleId, new int[0]);
                    }
                }
            }
            return;
        }

        int[] passengerIds = stands.values().stream().mapToInt(PacketArmorStand::getEntityId).toArray();

        Set<UUID> viewers = currentViewers.get(uuid);
        if (viewers != null) {
            for (UUID vId : viewers) {
                Player v = Bukkit.getPlayer(vId);
                if (v != null && v.isOnline()) {
                    PacketSender.sendSetPassengers(v, vehicleId, passengerIds);
                }
            }
        }
        PacketSender.sendSetPassengers(player, vehicleId, passengerIds);
    }

    public void updateEntityPassengersForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        Map<String, ArmorStand> stands = activeEntityStands.get(uuid);
        if (stands == null || stands.isEmpty()) {
            for (Entity pass : new ArrayList<>(player.getPassengers())) {
                if (pass instanceof ArmorStand as && !as.isPersistent()) {
                    player.removePassenger(as);
                    as.remove();
                }
            }
            if (PacketSender.isAvailable()) {
                double distSq = trackingDistance * trackingDistance;
                Location loc = player.getLocation();
                for (Player viewer : player.getWorld().getPlayers()) {
                    if (viewer.getLocation().distanceSquared(loc) <= distSq) {
                        PacketSender.sendSetPassengers(viewer, player.getEntityId(), new int[0]);
                    }
                }
            }
            return;
        }

        List<Integer> idList = new ArrayList<>();
        for (ArmorStand stand : stands.values()) {
            if (stand != null && stand.isValid()) {
                idList.add(stand.getEntityId());
                try {
                    player.addPassenger(stand);
                } catch (Throwable ignored) {}
            }
        }

        // Clean up any stray non-persistent armor stands on the player that are not part of active stands
        for (Entity pass : new ArrayList<>(player.getPassengers())) {
            if (pass instanceof ArmorStand as && !as.isPersistent() && !stands.containsValue(as)) {
                player.removePassenger(as);
                as.remove();
            }
        }

        if (idList.isEmpty()) return;

        int[] passengerIds = idList.stream().mapToInt(Integer::intValue).toArray();
        int vehicleId = player.getEntityId();

        if (PacketSender.isAvailable()) {
            double distSq = trackingDistance * trackingDistance;
            Location loc = player.getLocation();
            for (Player viewer : player.getWorld().getPlayers()) {
                if (viewer.getLocation().distanceSquared(loc) <= distSq) {
                    PacketSender.sendSetPassengers(viewer, vehicleId, passengerIds);
                }
            }
        }
    }

    public void updatePassengersForMob(LivingEntity mob) {
        if (mob == null || !mob.isValid()) return;
        UUID mobId = mob.getUniqueId();
        Map<String, PacketArmorStand> stands = activeMobStands.get(mobId);
        if (stands == null || stands.isEmpty()) return;

        int[] passengerIds = stands.values().stream().mapToInt(PacketArmorStand::getEntityId).toArray();
        int vehicleId = mob.getEntityId();

        Set<UUID> viewers = mobViewers.get(mobId);
        if (viewers != null) {
            for (UUID vId : viewers) {
                Player v = Bukkit.getPlayer(vId);
                if (v != null && v.isOnline()) {
                    PacketSender.sendSetPassengers(v, vehicleId, passengerIds);
                }
            }
        }
    }

    private void spawnStandForNearby(PacketArmorStand stand, Player owner) {
        UUID ownerId = owner.getUniqueId();
        Set<UUID> viewers = currentViewers.computeIfAbsent(ownerId, k -> ConcurrentHashMap.newKeySet());
        double distSq = trackingDistance * trackingDistance;
        Location loc = owner.getLocation();

        for (Player viewer : owner.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(loc) <= distSq) {
                stand.spawn(viewer, owner);
                viewers.add(viewer.getUniqueId());
            }
        }
        updatePassengersForPlayer(owner);
    }

    private void updateStandForViewers(PacketArmorStand stand, UUID ownerId) {
        Set<UUID> viewers = currentViewers.get(ownerId);
        if (viewers == null) return;

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                stand.updateEquipment(viewer);
            }
        }
    }

    private void destroyStandForViewers(PacketArmorStand stand, UUID ownerId) {
        Set<UUID> viewers = currentViewers.get(ownerId);
        if (viewers == null) return;

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                stand.destroy(viewer);
            }
        }
    }

    public void despawnAllForPlayer(UUID ownerId) {
        // Despawn packet stands
        Map<String, PacketArmorStand> stands = activeStands.remove(ownerId);
        Set<UUID> viewers = currentViewers.remove(ownerId);

        if (stands != null && viewers != null) {
            for (PacketArmorStand stand : stands.values()) {
                for (UUID viewerId : viewers) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer != null && viewer.isOnline()) {
                        stand.destroy(viewer);
                    }
                }
            }
        }

        // Despawn entity stands
        Map<String, ArmorStand> entityStands = activeEntityStands.remove(ownerId);
        activeEntityMeta.remove(ownerId);
        if (entityStands != null) {
            for (ArmorStand stand : entityStands.values()) {
                if (stand != null && stand.isValid()) {
                    stand.remove();
                }
            }
        }

        Player player = Bukkit.getPlayer(ownerId);
        if (player != null && player.isOnline()) {
            for (Entity pass : new ArrayList<>(player.getPassengers())) {
                if (pass instanceof ArmorStand as && !as.isPersistent()) {
                    player.removePassenger(as);
                    as.remove();
                }
            }
            if (PacketSender.isAvailable()) {
                PacketSender.sendSetPassengers(player, player.getEntityId(), new int[0]);
                if (viewers != null) {
                    for (UUID vId : viewers) {
                        Player v = Bukkit.getPlayer(vId);
                        if (v != null && v.isOnline()) {
                            PacketSender.sendSetPassengers(v, player.getEntityId(), new int[0]);
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Mob Model Engine
    // =========================================================================

    public boolean equipMobModel(Entity entity, String slotId, ItemStack accessory) {
        if (entity instanceof LivingEntity) {
            return equipMobModel((LivingEntity) entity, slotId, accessory);
        }
        return false;
    }

    public boolean equipMobModel(LivingEntity entity, String slotId, ItemStack accessory) {
        if (!enabled || entity == null || !entity.isValid()) return false;

        UUID mobId = entity.getUniqueId();
        Material modelMat = null;
        Integer cmd = null;
        String itemModel = null;

        if (accessory != null) {
            String modelItemStr = resolveItemTag(accessory, "curiospaper:model_item");
            if (modelItemStr != null && !modelItemStr.trim().isEmpty() && !modelItemStr.equalsIgnoreCase("DEFAULT")) {
                modelMat = Material.matchMaterial(modelItemStr.trim().toUpperCase(Locale.ROOT));
            }
            if (modelMat == null) {
                modelMat = accessory.getType();
            }

            String cmdStr = resolveItemTag(accessory, "curiospaper:model_cmd");
            if (cmdStr != null && !cmdStr.trim().isEmpty() && !cmdStr.equalsIgnoreCase("DEFAULT")) {
                try {
                    cmd = Integer.parseInt(cmdStr.trim());
                } catch (NumberFormatException ignored) {}
            }
            if (cmd == null) {
                cmd = VersionUtil.getCustomModelDataSafe(accessory.getItemMeta());
            }

            String modelTag = resolveItemTag(accessory, "curiospaper:model_item_model");
            if (modelTag != null && !modelTag.trim().isEmpty() && !modelTag.equalsIgnoreCase("DEFAULT")) {
                itemModel = modelTag;
            }
            if (itemModel == null || itemModel.trim().isEmpty()) {
                itemModel = VersionUtil.getItemModelSafe(accessory.getItemMeta());
            }
        }
        if (modelMat == null) {
            modelMat = Material.PAPER;
        }

        CuriosMobModelEquipEvent event = new CuriosMobModelEquipEvent(entity, slotId, accessory, modelMat, cmd, itemModel);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        ItemStack renderedItem = new ItemStack(event.getModelMaterial());
        ItemMeta meta = renderedItem.getItemMeta();
        if (meta != null) {
            VersionUtil.setItemModelSafe(meta, event.getItemModel(), event.getCustomModelData());
            renderedItem.setItemMeta(meta);
        }

        if (usePacketMode) {
            Map<String, PacketArmorStand> mobStands = activeMobStands.computeIfAbsent(mobId, k -> new ConcurrentHashMap<>());
            activeMobs.put(mobId, entity);

            PacketArmorStand existing = mobStands.get(slotId);
            if (existing == null) {
                PacketArmorStand stand = new PacketArmorStand(mobId, slotId, renderedItem);
                mobStands.put(slotId, stand);
                spawnStandForMobTrackers(stand, entity);
            } else {
                existing.setItem(renderedItem);
                for (Player tracker : getTrackingPlayers(entity)) {
                    existing.updateEquipment(tracker);
                }
            }
        } else {
            Map<String, ArmorStand> mobStands = activeMobEntityStands.computeIfAbsent(mobId, k -> new ConcurrentHashMap<>());
            activeEntityMobs.put(mobId, entity);

            float targetYaw = PacketArmorStand.isHeadSlot(slotId) ? entity.getLocation().getYaw() : getEntityBodyYaw(entity);
            ArmorStand existing = mobStands.get(slotId);
            if (existing != null && existing.isValid() && existing.getWorld().equals(entity.getWorld())) {
                EntityEquipment eq = existing.getEquipment();
                if (eq != null) {
                    eq.setHelmet(renderedItem);
                }
                existing.setRotation(targetYaw, 0.0f);
                setEntityBodyYaw(existing, targetYaw);
                existing.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
                if (!entity.getPassengers().contains(existing)) {
                    existing.teleport(entity.getLocation());
                    entity.addPassenger(existing);
                }
            } else {
                if (existing != null && existing.isValid()) {
                    existing.remove();
                }
                World world = entity.getWorld();
                ArmorStand stand = world.spawn(entity.getLocation(), ArmorStand.class, as -> {
                    as.setVisible(false);
                    as.setBasePlate(false);
                    as.setGravity(false);
                    as.setInvulnerable(true);
                    as.setArms(false);
                    as.setSmall(false);
                    as.setMarker(true);
                    as.setSilent(true);
                    as.setPersistent(false);
                    as.setCanPickupItems(false);
                    try { as.setCanTick(false); } catch (Throwable ignored) {}
                    try { as.setCollidable(false); } catch (Throwable ignored) {}
                });

                EntityEquipment eq = stand.getEquipment();
                if (eq != null) {
                    eq.setHelmet(renderedItem);
                }
                stand.setRotation(targetYaw, 0.0f);
                setEntityBodyYaw(stand, targetYaw);
                stand.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));

                entity.addPassenger(stand);
                mobStands.put(slotId, stand);
            }
        }

        return true;
    }

    public void removeMobModel(Entity entity, String slotId) {
        if (entity instanceof LivingEntity) {
            removeMobModel((LivingEntity) entity, slotId);
        }
    }

    public void removeMobModel(LivingEntity entity, String slotId) {
        if (entity == null) return;
        UUID mobId = entity.getUniqueId();

        if (usePacketMode) {
            Map<String, PacketArmorStand> stands = activeMobStands.get(mobId);
            if (stands != null) {
                PacketArmorStand stand = stands.remove(slotId);
                if (stand != null) {
                    for (Player tracker : getTrackingPlayers(entity)) {
                        stand.destroy(tracker);
                    }
                }
                if (stands.isEmpty()) {
                    activeMobStands.remove(mobId);
                    mobViewers.remove(mobId);
                    activeMobs.remove(mobId);
                }
            }
        } else {
            Map<String, ArmorStand> stands = activeMobEntityStands.get(mobId);
            if (stands != null) {
                ArmorStand stand = stands.remove(slotId);
                if (stand != null && stand.isValid()) {
                    stand.remove();
                }
                if (stands.isEmpty()) {
                    activeMobEntityStands.remove(mobId);
                    activeEntityMobs.remove(mobId);
                }
            }
        }
    }

    public void removeAllMobModels(Entity entity) {
        if (entity instanceof LivingEntity) {
            removeAllMobModels((LivingEntity) entity);
        }
    }

    public void removeAllMobModels(LivingEntity entity) {
        if (entity == null) return;
        UUID mobId = entity.getUniqueId();

        if (usePacketMode) {
            Map<String, PacketArmorStand> stands = activeMobStands.remove(mobId);
            mobViewers.remove(mobId);
            activeMobs.remove(mobId);
            if (stands != null) {
                Collection<Player> trackers = getTrackingPlayers(entity);
                for (PacketArmorStand stand : stands.values()) {
                    for (Player tracker : trackers) {
                        stand.destroy(tracker);
                    }
                }
            }
        } else {
            Map<String, ArmorStand> stands = activeMobEntityStands.remove(mobId);
            activeEntityMobs.remove(mobId);
            if (stands != null) {
                for (ArmorStand stand : stands.values()) {
                    if (stand != null && stand.isValid()) {
                        stand.remove();
                    }
                }
            }
        }
    }

    private void spawnStandForMobTrackers(PacketArmorStand stand, LivingEntity entity) {
        for (Player tracker : getTrackingPlayers(entity)) {
            stand.spawn(tracker, entity);
        }
        updatePassengersForMob(entity);
    }

    private Collection<Player> getTrackingPlayers(Entity entity) {
        if (entity == null || !entity.isValid()) return Collections.emptyList();
        try {
            return entity.getTrackedPlayers();
        } catch (Throwable t) {
            double distSq = trackingDistance * trackingDistance;
            Location loc = entity.getLocation();
            List<Player> nearby = new ArrayList<>();
            for (Player p : entity.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(loc) <= distSq) {
                    nearby.add(p);
                }
            }
            return nearby;
        }
    }

    private void syncMobViewers() {
        if (activeMobs.isEmpty()) return;

        Iterator<Map.Entry<UUID, LivingEntity>> it = activeMobs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = it.next();
            UUID mobId = entry.getKey();
            LivingEntity mob = entry.getValue();
            if (mob == null || !mob.isValid()) {
                activeMobStands.remove(mobId);
                mobViewers.remove(mobId);
                it.remove();
                continue;
            }

            Map<String, PacketArmorStand> stands = activeMobStands.get(mobId);
            if (stands == null || stands.isEmpty()) {
                mobViewers.remove(mobId);
                it.remove();
                continue;
            }

            Collection<Player> trackers = getTrackingPlayers(mob);
            Set<UUID> currentTrackers = mobViewers.computeIfAbsent(mobId, k -> ConcurrentHashMap.newKeySet());
            Set<UUID> trackerIds = new HashSet<>();
            for (Player p : trackers) {
                trackerIds.add(p.getUniqueId());
            }

            // Remove viewers who stopped tracking
            Iterator<UUID> itV = currentTrackers.iterator();
            while (itV.hasNext()) {
                UUID vId = itV.next();
                if (!trackerIds.contains(vId)) {
                    Player v = Bukkit.getPlayer(vId);
                    if (v != null && v.isOnline()) {
                        for (PacketArmorStand stand : stands.values()) {
                            stand.destroy(v);
                        }
                    }
                    itV.remove();
                }
            }

            // Add new trackers & synchronize rotation
            for (Player tracker : trackers) {
                boolean newlyAdded = currentTrackers.add(tracker.getUniqueId());
                if (newlyAdded) {
                    for (PacketArmorStand stand : stands.values()) {
                        stand.spawn(tracker, mob);
                    }
                    int[] passengerIds = stands.values().stream().mapToInt(PacketArmorStand::getEntityId).toArray();
                    PacketSender.sendSetPassengers(tracker, mob.getEntityId(), passengerIds);
                }
                for (PacketArmorStand stand : stands.values()) {
                    float targetYaw = stand.calculateTargetYaw(mob);
                    if (Float.isNaN(stand.getLastYaw()) || Math.abs(targetYaw - stand.getLastYaw()) >= 1.0f) {
                        stand.setLastYaw(targetYaw);
                        stand.setLastPitch(0.0f);
                        stand.updateRotation(tracker, targetYaw, 0.0f);
                    }
                }
            }
        }
    }

    private void syncEntityMobRotations() {
        if (activeEntityMobs.isEmpty() || activeMobEntityStands.isEmpty()) return;

        Iterator<Map.Entry<UUID, LivingEntity>> it = activeEntityMobs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> entry = it.next();
            UUID mobId = entry.getKey();
            LivingEntity mob = entry.getValue();
            if (mob == null || !mob.isValid()) {
                activeMobEntityStands.remove(mobId);
                it.remove();
                continue;
            }

            Map<String, ArmorStand> stands = activeMobEntityStands.get(mobId);
            if (stands == null || stands.isEmpty()) {
                it.remove();
                continue;
            }

            for (Map.Entry<String, ArmorStand> standEntry : stands.entrySet()) {
                ArmorStand stand = standEntry.getValue();
                if (stand != null && stand.isValid()) {
                    float targetYaw = PacketArmorStand.isHeadSlot(standEntry.getKey()) ? mob.getLocation().getYaw() : getEntityBodyYaw(mob);
                    Location sLoc = stand.getLocation();
                    if (Math.abs(targetYaw - sLoc.getYaw()) >= 1.0f) {
                        stand.setRotation(targetYaw, 0.0f);
                        setEntityBodyYaw(stand, targetYaw);
                        stand.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
                    }
                }
            }
        }
    }

    private void syncPlayerViewers() {
        if (!enabled || !usePacketMode || activeStands.isEmpty()) return;

        double distSq = trackingDistance * trackingDistance;

        for (Map.Entry<UUID, Map<String, PacketArmorStand>> entry : activeStands.entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = Bukkit.getPlayer(ownerId);
            if (owner == null || !owner.isOnline() || owner.isDead()) continue;

            Map<String, PacketArmorStand> stands = entry.getValue();
            if (stands == null || stands.isEmpty()) continue;

            Set<UUID> viewers = currentViewers.computeIfAbsent(ownerId, k -> ConcurrentHashMap.newKeySet());
            Location ownerLoc = owner.getLocation();
            World ownerWorld = owner.getWorld();

            Set<UUID> inRange = new HashSet<>();
            for (Player candidate : ownerWorld.getPlayers()) {
                if (candidate.getLocation().distanceSquared(ownerLoc) <= distSq) {
                    inRange.add(candidate.getUniqueId());
                }
            }
            inRange.add(ownerId);

            // 1. Despawn for viewers who left range or changed worlds
            Iterator<UUID> it = viewers.iterator();
            while (it.hasNext()) {
                UUID viewerId = it.next();
                if (!inRange.contains(viewerId)) {
                    Player viewer = Bukkit.getPlayer(viewerId);
                    if (viewer != null && viewer.isOnline()) {
                        for (PacketArmorStand stand : stands.values()) {
                            stand.destroy(viewer);
                        }
                    }
                    it.remove();
                }
            }

            // 2. Spawn for newly arrived players
            for (UUID newViewerId : inRange) {
                if (viewers.add(newViewerId)) {
                    Player newViewer = Bukkit.getPlayer(newViewerId);
                    if (newViewer != null && newViewer.isOnline()) {
                        for (PacketArmorStand stand : stands.values()) {
                            stand.spawn(newViewer, owner);
                        }
                        int[] passengerIds = stands.values().stream().mapToInt(PacketArmorStand::getEntityId).toArray();
                        PacketSender.sendSetPassengers(newViewer, owner.getEntityId(), passengerIds);
                    }
                }
            }
        }
    }

    public void updateStandsRotation(Player player, boolean force) {
        updateStandsRotation(player, force, player != null && player.isSneaking());
    }

    public void updateStandsRotation(Player player, boolean force, boolean isSneaking) {
        if (!enabled || player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        Location pLoc = player.getLocation();
        float bodyYaw = getEntityBodyYaw(player);
        float headYaw = pLoc.getYaw();

        if (usePacketMode) {
            Map<String, PacketArmorStand> stands = activeStands.get(uuid);
            if (stands == null || stands.isEmpty()) return;

            Set<UUID> viewers = currentViewers.get(uuid);
            if (viewers == null || viewers.isEmpty()) return;

            double currentScale = resolveModelScale(player, isSneaking);
            for (Map.Entry<String, PacketArmorStand> entry : stands.entrySet()) {
                String slotKey = entry.getKey();
                PacketArmorStand stand = entry.getValue();
                if (stand == null) continue;

                boolean isHead = PacketArmorStand.isHeadSlot(slotKey);
                float targetYaw = isHead ? headYaw : bodyYaw;
                float targetPitch = 0.0f;

                boolean rotChanged = force || Float.isNaN(stand.getLastYaw()) || Math.abs(targetYaw - stand.getLastYaw()) >= 1.0f;
                boolean scaleChanged = Math.abs(currentScale - stand.getLastScale()) >= 0.001;

                if (rotChanged || scaleChanged) {
                    if (rotChanged) {
                        stand.setLastYaw(targetYaw);
                        stand.setLastPitch(targetPitch);
                    }
                    if (scaleChanged) {
                        stand.setLastScale(currentScale);
                    }

                    for (UUID viewerId : viewers) {
                        Player viewer = Bukkit.getPlayer(viewerId);
                        if (viewer != null && viewer.isOnline()) {
                            if (rotChanged) {
                                stand.updateRotation(viewer, targetYaw, targetPitch);
                            }
                            if (scaleChanged) {
                                PacketSender.sendEntityScale(viewer, stand.getEntityId(), currentScale);
                            }
                        }
                    }
                }
            }
        } else {
            Map<String, ArmorStand> stands = activeEntityStands.get(uuid);
            if (stands == null || stands.isEmpty()) return;

            double currentScale = resolveModelScale(player, isSneaking);
            for (Map.Entry<String, ArmorStand> entry : stands.entrySet()) {
                String slotKey = entry.getKey();
                ArmorStand stand = entry.getValue();
                if (stand == null || !stand.isValid()) continue;

                applyEntityScale(stand, currentScale);

                boolean isHead = PacketArmorStand.isHeadSlot(slotKey);
                float targetYaw = isHead ? headYaw : bodyYaw;
                float targetPitch = 0.0f;

                Location sLoc = stand.getLocation();
                if (force || Math.abs(targetYaw - sLoc.getYaw()) >= 1.0f) {
                    stand.setRotation(targetYaw, targetPitch);
                    setEntityBodyYaw(stand, targetYaw);
                    stand.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
                }
            }
        }
    }

    public static org.bukkit.attribute.Attribute getScaleAttribute() {
        try {
            org.bukkit.attribute.Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("scale"));
            if (attr != null) return attr;
        } catch (Throwable ignored) {}
        try {
            org.bukkit.attribute.Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(org.bukkit.NamespacedKey.minecraft("generic.scale"));
            if (attr != null) return attr;
        } catch (Throwable ignored) {}
        try {
            return org.bukkit.attribute.Attribute.valueOf("GENERIC_SCALE");
        } catch (Throwable ignored) {}
        try {
            return org.bukkit.attribute.Attribute.valueOf("SCALE");
        } catch (Throwable ignored) {}
        return null;
    }

    public static void applyEntityScale(ArmorStand stand, double scale) {
        if (stand == null || !stand.isValid()) return;
        try {
            org.bukkit.attribute.Attribute scaleAttr = getScaleAttribute();
            if (scaleAttr != null) {
                org.bukkit.attribute.AttributeInstance inst = stand.getAttribute(scaleAttr);
                if (inst != null && Math.abs(inst.getBaseValue() - scale) > 0.0001) {
                    inst.setBaseValue(scale);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static float getEntityBodyYaw(LivingEntity entity) {
        if (entity == null) return 0.0f;
        try {
            return entity.getBodyYaw();
        } catch (Throwable t) {
            return entity.getLocation().getYaw();
        }
    }

    public static void setEntityBodyYaw(ArmorStand stand, float yaw) {
        if (stand == null) return;
        try {
            stand.setBodyYaw(yaw);
        } catch (Throwable ignored) {}
    }

    private void validateEntityPassengers() {
        if (usePacketMode || !enabled) return;

        for (Map.Entry<UUID, Map<String, ArmorStand>> entry : activeEntityStands.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || player.isDead()) continue;

            Map<String, ArmorStand> stands = entry.getValue();
            if (stands == null || stands.isEmpty()) continue;

            boolean missing = false;
            List<Entity> passengers = player.getPassengers();
            for (ArmorStand stand : stands.values()) {
                if (stand != null && stand.isValid() && !passengers.contains(stand)) {
                    missing = true;
                    break;
                }
            }

            if (missing) {
                updateEntityPassengersForPlayer(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDismount(org.bukkit.event.entity.EntityDismountEvent event) {
        if (!enabled) return;
        Entity dismounted = event.getEntity();
        if (!(dismounted instanceof ArmorStand stand)) return;
        Entity vehicle = event.getDismounted();
        if (!(vehicle instanceof Player player)) return;

        // Never cancel the dismount if the player is currently teleporting or changing dimensions
        if (teleportingPlayers.contains(player.getUniqueId())) {
            return;
        }

        Map<String, ArmorStand> stands = activeEntityStands.get(player.getUniqueId());
        if (stands != null && stands.containsValue(stand)) {
            event.setCancelled(true);
            PlatformScheduler.get().runEntity(player, () -> {
                if (player.isOnline() && stand.isValid() && !teleportingPlayers.contains(player.getUniqueId())) {
                    if (!player.getPassengers().contains(stand)) {
                        updateEntityPassengersForPlayer(player);
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (activeMobStands.containsKey(entity.getUniqueId()) || activeMobEntityStands.containsKey(entity.getUniqueId())) {
            removeAllMobModels(entity);
        }
    }

    // =========================================================================
    // Player Model Lifecycle Listeners
    // =========================================================================

    /**
     * Schedules a debounced model reconstruction for a player after teleport, respawn, or dimension change.
     */
    public void scheduleRescan(Player player, long delayTicks) {
        if (!enabled || player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        PlatformScheduler.TaskHandle existing = pendingRescans.remove(uuid);
        if (existing != null) {
            existing.cancel();
        }
        PlatformScheduler.TaskHandle task = PlatformScheduler.get().runEntityLater(player, () -> {
            pendingRescans.remove(uuid);
            teleportingPlayers.remove(uuid);
            if (player.isOnline()) {
                updatePlayerAccessories(player);
                if (usePacketMode) {
                    syncPlayerViewers();
                }
            }
        }, delayTicks);
        pendingRescans.put(uuid, task);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleportLowest(PlayerTeleportEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        teleportingPlayers.add(uuid);

        // In entity mode, dismount passengers immediately before any teleport check occurs
        if (!usePacketMode) {
            Map<String, ArmorStand> stands = activeEntityStands.get(uuid);
            if (stands != null && !stands.isEmpty()) {
                for (ArmorStand stand : stands.values()) {
                    if (stand != null && stand.isValid()) {
                        player.removePassenger(stand);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (event.isCancelled()) {
            teleportingPlayers.remove(uuid);
            scheduleRescan(player, 1L);
            return;
        }

        // Clean up stands from the origin location/world
        despawnAllForPlayer(uuid);

        boolean isCrossWorld = event.getFrom().getWorld() != null &&
                               event.getTo() != null &&
                               event.getTo().getWorld() != null &&
                               !event.getFrom().getWorld().equals(event.getTo().getWorld());
        long delay = isCrossWorld ? 10L : 2L;

        scheduleRescan(player, delay);

        // Silent failed teleport recovery fallback for cross-world or long-distance teleports
        Location fromLoc = event.getFrom();
        Location toLoc = event.getTo();
        if (toLoc != null && (!fromLoc.getWorld().equals(toLoc.getWorld()) || fromLoc.distanceSquared(toLoc) > 4.0)) {
            Location expectedDest = toLoc.clone();
            Location originLoc = fromLoc.clone();
            PlatformScheduler.get().runEntityLater(player, () -> {
                if (player.isOnline()) {
                    Location currentLoc = player.getLocation();
                    if (currentLoc.getWorld().equals(originLoc.getWorld()) && currentLoc.distanceSquared(originLoc) < 1.0) {
                        plugin.getLogger().warning("[CuriosPaper] Detected silently failed teleport for " + player.getName() + " to " + expectedDest.getWorld().getName() + ". Auto-recovering player teleport cleanly without passengers!");
                        despawnAllForPlayer(uuid);
                        player.teleport(expectedDest);
                        scheduleRescan(player, 10L);
                    }
                }
            }, 4L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        despawnAllForPlayer(uuid);
        // Cross-dimension transition requires slight delay for client chunk loading
        scheduleRescan(player, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!enabled) return;
        UUID uuid = event.getEntity().getUniqueId();
        lastHideTickState.remove(uuid);
        teleportingPlayers.remove(uuid);
        PlatformScheduler.TaskHandle task = pendingRescans.remove(uuid);
        if (task != null) task.cancel();
        despawnAllForPlayer(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // Despawn any orphaned stands (crucial for End Portal return to spawn)
        despawnAllForPlayer(uuid);
        scheduleRescan(player, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        lastHideTickState.remove(uuid);
        teleportingPlayers.remove(uuid);
        PlatformScheduler.TaskHandle task = pendingRescans.remove(uuid);
        if (task != null) task.cancel();
        removeCurioTags(player);
        despawnAllForPlayer(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        // Bedrock and Java players both receive standard passenger visuals
    }

    private void collectCurioTags(Set<String> tags, ItemStack equipped, CuriosModelEquipEvent equipEvent, SlotDefinition def) {
        tags.add("curio_has_3d");
        if (def != null && def.getId() != null) {
            tags.add("curio_slot_" + def.getId().toLowerCase(Locale.ROOT));
        }

        Integer cmd = equipEvent.getCustomModelData();
        if (cmd != null && cmd > 0) {
            tags.add("curio_cmd_" + cmd);
        }

        String itemModel = equipEvent.getItemModel();
        if (itemModel != null && !itemModel.trim().isEmpty()) {
            String sanitized = itemModel.toLowerCase(Locale.ROOT)
                    .replace("minecraft:", "")
                    .replace(':', '_')
                    .replace('/', '_')
                    .replace('-', '_');
            tags.add("curio_" + sanitized);

            // Handle probablybackpacks specific tags
            if (itemModel.contains("backpack")) {
                tags.add("curio_backpack");
                String[] parts = itemModel.split("/");
                if (parts.length >= 2) {
                    String color = parts[parts.length - 1].toLowerCase(Locale.ROOT);
                    String tier = parts[parts.length - 2].toLowerCase(Locale.ROOT);
                    tags.add("curio_backpack_" + tier + "_" + color);
                    tags.add("curio_backpack_" + tier);
                    tags.add("curio_backpack_" + color);
                }
            }

            // Handle artifacts specific tags
            if (itemModel.contains("artifacts:")) {
                String cleanArtifact = itemModel.replace("artifacts:", "").replace("_3d", "");
                tags.add("curio_artifacts_" + cleanArtifact);
                tags.add("curio_" + cleanArtifact);
            }
        }
    }

    private void syncPlayerScoreboardTags(Player player, Set<String> targetCurioTags) {
        if (player == null || !player.isOnline()) return;
        Set<String> currentTags = player.getScoreboardTags();
        boolean changed = false;
        for (String tag : new HashSet<>(currentTags)) {
            if (tag.startsWith("curio_") && !targetCurioTags.contains(tag)) {
                player.removeScoreboardTag(tag);
                changed = true;
            }
        }
        for (String tag : targetCurioTags) {
            if (!currentTags.contains(tag)) {
                player.addScoreboardTag(tag);
                changed = true;
            }
        }
        if (changed) {
            try {
                // Refresh entity metadata tracking so Bedrock clients immediately re-evaluate render controllers
                player.setCustomNameVisible(player.isCustomNameVisible());
            } catch (Throwable ignored) {}
        }
    }

    public void removeCurioTags(Player player) {
        if (player == null) return;
        for (String tag : new HashSet<>(player.getScoreboardTags())) {
            if (tag.startsWith("curio_")) {
                player.removeScoreboardTag(tag);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!enabled) return;
        if (event.getEntity() instanceof Player player) {
            updatePlayerVisibility(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityToggleSwim(EntityToggleSwimEvent event) {
        if (!enabled) return;
        if (event.getEntity() instanceof Player player) {
            updatePlayerVisibility(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (!enabled) return;
        updatePlayerVisibility(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean hasPacketStands = usePacketMode && activeStands.containsKey(uuid);
        boolean hasEntityStands = !usePacketMode && activeEntityStands.containsKey(uuid);
        if (!hasPacketStands && !hasEntityStands) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        float toPitch = to.getPitch();
        float fromPitch = from.getPitch();
        boolean pitchChanged = toPitch != fromPitch;
        boolean hidingState = isHidingState(player) || lastHideTickState.containsKey(uuid);

        if (pitchChanged || hidingState) {
            updatePlayerVisibility(player);
        }

        float yawDiff = Math.abs(to.getYaw() - from.getYaw());
        boolean posChanged = from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ();

        if (yawDiff >= 1.0f || posChanged) {
            updateStandsRotation(player, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!enabled) return;
        updateStandsRotation(event.getPlayer(), true, event.isSneaking());
    }

    /**
     * Checks if the player is in a physical state or pose that requires hiding accessory models.
     * This includes gliding (elytra/helium flamingo), swimming, flying, spectator mode, death,
     * spinning attack (riptide), or custom hide metadata.
     */
    public static boolean isHidingState(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return true;
        }
        if (player.isGliding() || player.isSwimming() || player.isFlying()) {
            return true;
        }
        try {
            if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                return true;
            }
        } catch (Throwable ignored) {}
        try {
            org.bukkit.entity.Pose pose = player.getPose();
            if (pose == org.bukkit.entity.Pose.SWIMMING ||
                pose == org.bukkit.entity.Pose.FALL_FLYING ||
                pose == org.bukkit.entity.Pose.DYING ||
                pose == org.bukkit.entity.Pose.SPIN_ATTACK) {
                return true;
            }
        } catch (Throwable ignored) {}
        if (player.hasMetadata(org.bg52.curiospaper.api.CuriosPaperAPI.HIDE_MODELS_METADATA)) {
            return true;
        }
        return false;
    }

    /**
     * Determines whether models should be hidden for all viewers, applying 250ms smoothing
     * to prevent flickering during rapid transitions (e.g. Helium Flamingo, water hopping).
     */
    public boolean shouldHideFromAll(Player player) {
        boolean stateHide = isHidingState(player);
        long currentTime = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        if (stateHide) {
            lastHideTickState.put(uuid, currentTime);
            return true;
        }

        Long lastTime = lastHideTickState.get(uuid);
        if (lastTime != null) {
            long diff = currentTime - lastTime;
            if (diff >= 0 && diff < 250) { // 250ms smoothing window
                return true;
            } else {
                lastHideTickState.remove(uuid);
            }
        }

        return false;
    }

    /**
     * Authoritatively updates visibility for all 3D accessory models worn by a player.
     * - Gliding, swimming, flying, spectator, or hide metadata hides the models from ALL players.
     * - Pitch limits hide head/hat models only from the first-person perspective of the wearer.
     */
    public void updatePlayerVisibility(Player player) {
        if (!enabled || player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        boolean hideAll = shouldHideFromAll(player);
        float pitch = player.getLocation().getPitch();

        if (usePacketMode) {
            Map<String, PacketArmorStand> stands = activeStands.get(uuid);
            if (stands == null || stands.isEmpty()) return;
            Set<UUID> viewers = currentViewers.get(uuid);

            for (PacketArmorStand stand : stands.values()) {
                Float pitchUp = stand.getPitchUpLimit();
                Float pitchDown = stand.getPitchDownLimit();
                boolean pitchHide = (pitchUp != null && pitch < -pitchUp) || (pitchDown != null && pitch > pitchDown);

                boolean prevHideAll = stand.isHiddenFromAll();
                boolean prevHideWearer = stand.isHiddenFromWearer();

                stand.setHiddenFromAll(hideAll);
                stand.setHiddenFromWearer(pitchHide);

                if (prevHideAll != hideAll || prevHideWearer != pitchHide) {
                    if (viewers != null) {
                        for (UUID vId : viewers) {
                            Player viewer = Bukkit.getPlayer(vId);
                            if (viewer != null && viewer.isOnline()) {
                                stand.updateEquipment(viewer);
                            }
                        }
                    }
                }
            }
        } else {
            Map<String, ArmorStand> stands = activeEntityStands.get(uuid);
            Map<String, StandMeta> metas = activeEntityMeta.get(uuid);
            if (stands == null || metas == null) return;

            for (Map.Entry<String, StandMeta> entry : metas.entrySet()) {
                StandMeta sm = entry.getValue();
                ArmorStand stand = stands.get(entry.getKey());
                if (stand == null || !stand.isValid()) continue;

                Float pitchUp = sm.pitchUpLimit;
                Float pitchDown = sm.pitchDownLimit;
                boolean pitchHide = (pitchUp != null && pitch < -pitchUp) || (pitchDown != null && pitch > pitchDown);

                boolean prevHideAll = sm.hiddenFromAll;
                boolean prevHideWearer = sm.hiddenFromWearer;

                sm.hiddenFromAll = hideAll;
                sm.hiddenFromWearer = pitchHide;

                if (prevHideAll != hideAll || prevHideWearer != pitchHide) {
                    EntityEquipment eq = stand.getEquipment();
                    if (hideAll) {
                        if (eq != null && eq.getHelmet() != null && eq.getHelmet().getType() != Material.AIR) {
                            eq.setHelmet(null);
                        }
                        if (player.canSee(stand)) {
                            player.hideEntity(plugin, stand);
                        }
                    } else {
                        if (eq != null && (eq.getHelmet() == null || !sm.item.isSimilar(eq.getHelmet()))) {
                            eq.setHelmet(sm.item);
                        }
                        if (pitchHide) {
                            if (PacketSender.isAvailable()) {
                                PacketSender.sendHelmetEquipment(player, stand.getEntityId(), new ItemStack(Material.AIR));
                            } else if (player.canSee(stand)) {
                                player.hideEntity(plugin, stand);
                            }
                        } else {
                            if (PacketSender.isAvailable()) {
                                PacketSender.sendHelmetEquipment(player, stand.getEntityId(), sm.item);
                            }
                            if (!player.canSee(stand)) {
                                player.showEntity(plugin, stand);
                            }
                        }
                    }
                }
            }
        }
    }

    public void handlePitchLimits(Player player, float pitch) {
        updatePlayerVisibility(player);
    }

    public Float resolvePitchLimit(ItemStack item, String limitType, SlotDefinition slotDef) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            String subKey = limitType.contains(":") ? limitType.substring(limitType.indexOf(':') + 1) : limitType;
            String dashKey = subKey.replace('_', '-');
            String underKey = subKey.replace('-', '_');

            List<String> namespaces = Arrays.asList("curiospaper", plugin.getName().toLowerCase(Locale.ROOT), "synthetix", "probablybackpacks");
            for (String ns : namespaces) {
                for (String k : Arrays.asList(subKey, dashKey, underKey)) {
                    NamespacedKey nk = new NamespacedKey(ns, k);
                    if (pdc.has(nk, PersistentDataType.FLOAT)) {
                        return pdc.get(nk, PersistentDataType.FLOAT);
                    }
                    if (pdc.has(nk, PersistentDataType.DOUBLE)) {
                        Double d = pdc.get(nk, PersistentDataType.DOUBLE);
                        return d != null ? d.floatValue() : null;
                    }
                    if (pdc.has(nk, PersistentDataType.INTEGER)) {
                        Integer val = pdc.get(nk, PersistentDataType.INTEGER);
                        return val != null ? val.floatValue() : null;
                    }
                    if (pdc.has(nk, PersistentDataType.STRING)) {
                        try {
                            String s = pdc.get(nk, PersistentDataType.STRING);
                            if (s != null && !s.trim().isEmpty()) {
                                return Float.parseFloat(s.trim());
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Synthetix tag check
            if (Bukkit.getPluginManager().isPluginEnabled("Synthetix")) {
                try {
                    if (org.bg52.synthetix.api.SynthetixAPI.getOptional().isPresent()) {
                        org.bg52.synthetix.item.ItemDefinition def = org.bg52.synthetix.api.SynthetixAPI.get().getItemRegistry().getByItemStack(item);
                        if (def != null) {
                            for (String k : Arrays.asList(limitType, subKey, dashKey, underKey)) {
                                String val = def.getTag(k);
                                if (val != null) {
                                    try {
                                        return Float.parseFloat(val.trim());
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        // Fallback to slot definition defaults if configured
        if (slotDef != null) {
            if (limitType.contains("up")) {
                return slotDef.getDefaultPitchUpLimit();
            } else if (limitType.contains("down")) {
                return slotDef.getDefaultPitchDownLimit();
            }
        }
        return null;
    }

    public String resolveItemTag(ItemStack item, String tagKey) {
        if (item == null || !item.hasItemMeta()) return null;

        // 1. Check item PDC
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String subKey = tagKey.contains(":") ? tagKey.substring(tagKey.indexOf(':') + 1) : tagKey;
        NamespacedKey nk = new NamespacedKey(plugin, subKey);
        if (pdc.has(nk, PersistentDataType.STRING)) {
            return pdc.get(nk, PersistentDataType.STRING);
        }

        // 2. Check Synthetix ItemDefinition if present
        if (Bukkit.getPluginManager().isPluginEnabled("Synthetix")) {
            try {
                if (org.bg52.synthetix.api.SynthetixAPI.getOptional().isPresent()) {
                    org.bg52.synthetix.item.ItemDefinition def = org.bg52.synthetix.api.SynthetixAPI.get().getItemRegistry().getByItemStack(item);
                    if (def != null) {
                        String val = def.getTag(tagKey);
                        if (val != null) return val;
                        val = def.getTag(subKey);
                        if (val != null) return val;
                    }
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    private static boolean isTorsoBackSlot(String slotKey) {
        if (slotKey == null) return false;
        String clean = slotKey.toLowerCase(Locale.ROOT);
        return clean.equals("back") || clean.equals("body") || clean.startsWith("back_") || clean.startsWith("body_") || clean.contains("backpack");
    }
}
