package org.bg52.curiospaper.model;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages 3D model entities for items equipped in curios slots.
 * ArmorStands.
 */
public class ModelStandManager implements Listener {

  private final CuriosPaper plugin;

  private final Map<UUID, Map<String, Entity>> activeStands = new ConcurrentHashMap<>();

  private final Set<UUID> modelEntityIds = ConcurrentHashMap.newKeySet();
  private final Set<UUID> activeTridentUsers = ConcurrentHashMap.newKeySet();
  private final Map<UUID, Long> lastHideTickState = new ConcurrentHashMap<>();

  private final NamespacedKey modelHiddenKey;
  private final NamespacedKey modelStandTag;

  private static final float ROTATION_THRESHOLD = 1.5f;

  public ModelStandManager(CuriosPaper plugin) {
    this.plugin = plugin;
    this.modelHiddenKey = new NamespacedKey(plugin, "curios_model_hidden");
    this.modelStandTag = new NamespacedKey(plugin, "curios_model_stand");
  }

  public void initialize() {
    Bukkit.getPluginManager().registerEvents(this, plugin);

    if (VersionUtil.supportsEntityPoseChangeEvent()) {
      try {
        Bukkit.getPluginManager().registerEvents(new PoseChangeHandler(this), plugin);
      } catch (Exception e) {
        plugin.getLogger().warning("EntityPoseChangeEvent not available, skipping.");
      }
    }

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      for (Player player : Bukkit.getOnlinePlayers()) {
        rescanPlayer(player);
      }
    }, 5L);

    // Periodic sync task for scale and visibility
    Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      for (Player player : Bukkit.getOnlinePlayers()) {
        updateStandsForPlayer(player, false);
      }
    }, 20L, 20L);
  }

  public void shutdown() {
    for (UUID playerId : new HashSet<>(activeStands.keySet())) {
      removeAllStands(playerId);
    }
    activeTridentUsers.clear();
  }

  // ========== EVENT HANDLERS ==========

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onAccessoryEquip(AccessoryEquipEvent event) {
    Player player = event.getPlayer();
    String slotKey = event.getSlotType() + ":" + event.getSlotIndex();

    handleUnequip(player, slotKey);

    if (event.getAction() != AccessoryEquipEvent.Action.UNEQUIP && event.getNewItem() != null) {
      handleEquip(player, slotKey, event.getNewItem());
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    removeAllStands(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    removeAllStands(event.getEntity().getUniqueId());
  }

  @EventHandler
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (player.isOnline()) {
        rescanPlayer(player);
      }
    }, 1L);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (event.getPlayer().isOnline()) {
        rescanPlayer(event.getPlayer());
      }
    }, 10L);
  }

  @EventHandler
  public void onWorldChange(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    removeAllStands(player.getUniqueId());
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (player.isOnline()) {
        rescanPlayer(player);
      }
    }, 5L);
  }

  @EventHandler
  public void onTeleport(PlayerTeleportEvent event) {
    Player player = event.getPlayer();
    updateStandsForPlayer(player, true);
    if (event.getFrom().getWorld() != event.getTo().getWorld()) {
      removeAllStands(player.getUniqueId());
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (player.isOnline()) {
          rescanPlayer(player);
        }
      }, 5L);
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) {
    if (modelEntityIds.contains(event.getEntity().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onEntityTarget(EntityTargetEvent event) {
    if (modelEntityIds.contains(event.getEntity().getUniqueId())) {
      event.setCancelled(true);
    }
    // Also cancel if a mob tries to target a model entity
    if (event.getTarget() != null && modelEntityIds.contains(event.getTarget().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityToggleGlide(EntityToggleGlideEvent event) {
    if (event.getEntity() instanceof Player) {
      updateStandsForPlayer((Player) event.getEntity(), true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityToggleSwim(EntityToggleSwimEvent event) {
    if (event.getEntity() instanceof Player) {
      updateStandsForPlayer((Player) event.getEntity(), true);
    }
  }

  // Prevent right-click interactions (e.g. villager trade GUI opening)
  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (modelEntityIds.contains(event.getRightClicked().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    if (modelEntityIds.contains(event.getRightClicked().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Location from = event.getFrom();
    Location to = event.getTo();

    if (to == null) {
      return;
    }

    boolean rotationChanged = Math.abs(from.getYaw() - to.getYaw()) >= ROTATION_THRESHOLD ||
        Math.abs(from.getPitch() - to.getPitch()) >= ROTATION_THRESHOLD;

    // Force update if player is in a state that usually requires model hiding
    Player player = event.getPlayer();
    boolean forceUpdate = player.isGliding() || player.isSwimming();

    if (!rotationChanged && !forceUpdate) {
      return;
    }

    updateStandsForPlayer(player, false);
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    updateStandsForPlayer(event.getPlayer(), false);
  }

  @EventHandler
  public void onGameModeChange(PlayerGameModeChangeEvent event) {
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      updateStandsForPlayer(event.getPlayer(), true);
    }, 1L);
  }

  static class PoseChangeHandler implements Listener {
    private final ModelStandManager parent;

    PoseChangeHandler(ModelStandManager parent) {
      this.parent = parent;
    }

    @EventHandler
    public void onEntityPoseChange(org.bukkit.event.entity.EntityPoseChangeEvent event) {
      if (event.getEntity() instanceof Player) {
        parent.updateStandsForPlayer((Player) event.getEntity(), true);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteractTrident(PlayerInteractEvent event) {
    if (!event.getAction().name().contains("RIGHT")) {
      return;
    }

    ItemStack item = event.getItem();
    if (item == null || item.getType() != Material.TRIDENT) {
      return;
    }

    Player player = event.getPlayer();

    // Check if this player has active stands
    if (!activeStands.containsKey(player.getUniqueId()) || activeStands.get(player.getUniqueId()).isEmpty()) {
      return;
    }

    // Already handling this player's trident use
    if (activeTridentUsers.contains(player.getUniqueId())) {
      return;
    }

    activeTridentUsers.add(player.getUniqueId());

    // Fully remove the armor stands
    removeAllStands(player.getUniqueId());

    boolean hasRiptide = item.containsEnchantment(Enchantment.RIPTIDE);

    if (hasRiptide) {
      handleRiptideTrident(player);
    } else {
      handleNormalTrident(player);
    }
  }

  private void handleRiptideTrident(Player player) {
    new BukkitRunnable() {
      private int ticksWaited = 0;
      private boolean wasInAir = false;

      @Override
      public void run() {
        ticksWaited++;

        if (!player.isOnline()) {
          cleanupTrident(player);
          cancel();
          return;
        }

        // Track if the player actually launched into the air
        if (!player.isOnGround()) {
          wasInAir = true;
        }

        // Restore once player lands after being in the air,
        // or after a short delay if riptide didn't activate (not in water/rain)
        boolean shouldRestore = false;

        if (wasInAir && player.isOnGround()) {
          shouldRestore = true;
        } else if (!wasInAir && ticksWaited > 20) {
          shouldRestore = true;
        } else if (ticksWaited > 200) {
          shouldRestore = true;
        }

        if (shouldRestore) {
          restoreTridentAndCleanup(player);
          cancel();
        }
      }
    }.runTaskTimer(plugin, 5L, 1L);
  }

  private void handleNormalTrident(Player player) {
    new BukkitRunnable() {
      private int ticksWaited = 0;
      private boolean wasCharging = false;

      @Override
      public void run() {
        ticksWaited++;

        if (!player.isOnline()) {
          cleanupTrident(player);
          cancel();
          return;
        }

        if (player.isHandRaised()) {
          wasCharging = true;
        }

        boolean released = wasCharging && !player.isHandRaised();
        boolean timeout = ticksWaited > 200;

        if (released || timeout) {
          Bukkit.getScheduler().runTaskLater(plugin, () -> {
            restoreTridentAndCleanup(player);
          }, 40L);
          cancel();
        }
      }
    }.runTaskTimer(plugin, 2L, 1L);
  }

  private void restoreTridentAndCleanup(Player player) {
    if (player.isOnline()) {
      rescanPlayer(player);
    }
    cleanupTrident(player);
  }

  private void cleanupTrident(Player player) {
    activeTridentUsers.remove(player.getUniqueId());
  }

  // ========== CORE LOGIC ==========

  private void handleEquip(Player player, String slotKey, ItemStack item) {
    if (item == null || item.getType() == Material.AIR)
      return;

    if (isModelHiddenOnItem(item))
      return;

    ItemData itemData = getItemDataFromStack(item);
    if (itemData == null || !itemData.isModelEnabled())
      return;

    ItemStack modelHelmet = createModelHelmet(itemData);
    if (modelHelmet == null)
      return;

    spawnModelStand(player, slotKey, modelHelmet, itemData);

    // Initial sync
    updateStandsForPlayer(player, true);
  }

  private void handleUnequip(Player player, String slotKey) {
    Map<String, Entity> playerStands = activeStands.get(player.getUniqueId());
    if (playerStands == null)
      return;

    Entity entity = playerStands.remove(slotKey);
    if (entity != null) {
      modelEntityIds.remove(entity.getUniqueId());
      player.removePassenger(entity);
      entity.remove();
    }

    if (playerStands.isEmpty()) {
      activeStands.remove(player.getUniqueId());
    }
  }

  private void spawnModelStand(Player player, String slotKey, ItemStack modelHelmet, ItemData itemData) {
    World world = player.getWorld();
    Location spawnLoc = player.getLocation();

    Entity spawnedEntity;

    {
      ArmorStand stand = world.spawn(spawnLoc, ArmorStand.class, as -> {
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
        as.setCollidable(false);
      });

      EntityEquipment equip = stand.getEquipment();
      if (equip != null) {
        equip.setHelmet(modelHelmet);
      }

      spawnedEntity = stand;
    }

    player.addPassenger(spawnedEntity);
    modelEntityIds.add(spawnedEntity.getUniqueId());

    Map<String, Entity> playerStands = activeStands.computeIfAbsent(
        player.getUniqueId(), k -> new ConcurrentHashMap<>());
    playerStands.put(slotKey, spawnedEntity);
  }

  private ItemStack createModelHelmet(ItemData itemData) {
    String modelItemStr = itemData.getModelItem();
    if (modelItemStr == null || modelItemStr.isEmpty())
      return null;

    Material modelMat;
    try {
      modelMat = Material.valueOf(modelItemStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      plugin.getLogger().warning("[ModelStandManager] Invalid model material: " + modelItemStr);
      return null;
    }

    ItemStack helmet = new ItemStack(modelMat);
    ItemMeta meta = helmet.getItemMeta();
    if (meta != null) {
      VersionUtil.setItemModelSafe(meta, itemData.getModelItemModel(), itemData.getModelCustomModelData());
      meta.getPersistentDataContainer().set(modelStandTag, PersistentDataType.BYTE, (byte) 1);
      helmet.setItemMeta(meta);
    }
    return helmet;
  }

  // ========== SYNC UPDATE LOGIC ==========

  public void updateStandsForPlayer(Player player, boolean force) {
    if (player == null || !player.isOnline())
      return;

    Map<String, Entity> playerStands = activeStands.get(player.getUniqueId());
    if (playerStands == null || playerStands.isEmpty())
      return;

    Location pLoc = player.getLocation();

    for (Map.Entry<String, Entity> standEntry : playerStands.entrySet()) {
      Entity entity = standEntry.getValue();
      if (entity == null || !entity.isValid()) {
        playerStands.remove(standEntry.getKey());
        continue;
      }

      if (entity instanceof ArmorStand) {
        ArmorStand stand = (ArmorStand) entity;
        float targetYaw = pLoc.getYaw();
        float targetPitch = 0f;

        Location sLoc = stand.getLocation();
        if (force || Math.abs(targetYaw - sLoc.getYaw()) > ROTATION_THRESHOLD
            || Math.abs(targetPitch - sLoc.getPitch()) > ROTATION_THRESHOLD) {
          stand.setRotation(targetYaw, targetPitch);
        }

        stand.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
      }

      syncScale(player, entity);
      updateSelfVisibility(player, entity, standEntry.getKey());
    }
  }

  private void updateSelfVisibility(Player player, Entity entity, String slotKey) {
    String[] parts = slotKey.split(":");
    if (parts.length != 2)
      return;

    String slotType = parts[0];
    int slotIndex;
    try {
      slotIndex = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      return;
    }

    ItemStack equippedItem = plugin.getCuriosPaperAPI().getEquippedItem(player, slotType, slotIndex);
    ItemData itemData = getItemDataFromStack(equippedItem);
    if (itemData == null)
      return;

    boolean shouldHide = false;

    // Global hide checks
    boolean stateHide = player.isDead() || player.isGliding() || player.isSwimming();

    if (!stateHide) {
      try {
        org.bukkit.entity.Pose pose = player.getPose();
        if (pose == org.bukkit.entity.Pose.SWIMMING ||
            pose == org.bukkit.entity.Pose.FALL_FLYING ||
            pose == org.bukkit.entity.Pose.DYING ||
            pose == org.bukkit.entity.Pose.SPIN_ATTACK) {
          stateHide = true;
        }
      } catch (NoSuchMethodError | NoSuchFieldError ignored) {
      }
    }

    // Metadata check (custom hide signal from other plugins)
    if (!stateHide && player.hasMetadata(org.bg52.curiospaper.api.CuriosPaperAPI.HIDE_MODELS_METADATA)) {
      stateHide = true;
    }

    // Visibility smoothing to prevent flickering (e.g. Helium Flamingo without Elytra)
    long currentTick = player.getWorld().getFullTime();
    if (stateHide) {
      lastHideTickState.put(player.getUniqueId(), currentTick);
      shouldHide = true;
    } else {
      Long lastTick = lastHideTickState.get(player.getUniqueId());
      if (lastTick != null && (currentTick - lastTick) < 5) {
        shouldHide = true;
      }
    }

    // Pitch limits
    if (!shouldHide) {
      float pitch = player.getLocation().getPitch();
      Float pitchUp = itemData.getPitchUpLimit();
      Float pitchDown = itemData.getPitchDownLimit();
      if (pitchUp != null && pitch < -pitchUp) {
        shouldHide = true;
      }
      if (pitchDown != null && pitch > pitchDown) {
        shouldHide = true;
      }
    }

    EntityEquipment equip = null;
    if (entity instanceof ArmorStand) {
      equip = ((ArmorStand) entity).getEquipment();
    }

    if (equip == null)
      return;

    if (shouldHide) {
      if (equip.getHelmet() != null && equip.getHelmet().getType() != Material.AIR) {
        equip.setHelmet(null);
      }
    } else {
      ItemStack current = equip.getHelmet();
      if (current == null || current.getType() == Material.AIR) {
        ItemStack modelHelmet = createModelHelmet(itemData);
        if (modelHelmet != null) {
          equip.setHelmet(modelHelmet);
        }
      }
    }
  }

  // ========== UTILITIES ==========

  public void rescanPlayer(Player player) {
    removeAllStands(player.getUniqueId());

    for (String slotType : plugin.getCuriosPaperAPI().getAllSlotTypes()) {
      int slotCount = plugin.getCuriosPaperAPI().getSlotAmount(slotType);
      for (int i = 0; i < slotCount; i++) {
        ItemStack item = plugin.getCuriosPaperAPI().getEquippedItem(player, slotType, i);
        if (item != null && item.getType() != Material.AIR) {
          String key = slotType + ":" + i;
          handleEquip(player, key, item);
        }
      }
    }
  }

  private void removeAllStands(UUID playerId) {
    Map<String, Entity> playerStands = activeStands.remove(playerId);
    if (playerStands == null)
      return;

    Player player = Bukkit.getPlayer(playerId);
    for (Entity entity : playerStands.values()) {
      if (entity != null) {
        modelEntityIds.remove(entity.getUniqueId());
        if (player != null) {
          player.removePassenger(entity);
        }
        entity.remove();
      }
    }
  }

  private ItemData getItemDataFromStack(ItemStack item) {
    if (item == null || !item.hasItemMeta())
      return null;

    PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
    String itemId = pdc.get(plugin.getCuriosPaperAPI().getItemIdKey(), PersistentDataType.STRING);
    if (itemId == null)
      return null;

    return plugin.getCuriosPaperAPI().getItemData(itemId);
  }

  private void syncScale(Player player, Entity entity) {
    if (!VersionUtil.supportsScaleAttribute()) {
      return;
    }

    try {
      // Attribute.GENERIC_SCALE was added in 1.20.5
      Attribute scaleAttr = Attribute.valueOf("GENERIC_SCALE");
      AttributeInstance playerScale = player.getAttribute(scaleAttr);
      if (playerScale == null)
        return;

      double scaleValue = playerScale.getValue();

      // Armor stands and other living entities are Attributable in 1.20.5+
      if (entity instanceof org.bukkit.attribute.Attributable) {
        AttributeInstance entityScale = ((org.bukkit.attribute.Attributable) entity).getAttribute(scaleAttr);
        if (entityScale != null && Math.abs(entityScale.getBaseValue() - scaleValue) > 0.001) {
          entityScale.setBaseValue(scaleValue);
        }
      }
    } catch (Throwable ignored) {
      // Fallback or ignore if attributes are not supported/available
    }
  }

  private boolean isModelHiddenOnItem(ItemStack item) {
    if (item == null || !item.hasItemMeta())
      return false;
    PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
    return pdc.getOrDefault(modelHiddenKey, PersistentDataType.BYTE, (byte) 0) == 1;
  }

  public boolean isModelEntity(org.bukkit.entity.Entity entity) {
    return modelEntityIds.contains(entity.getUniqueId());
  }

  /** @deprecated Use {@link #isModelEntity(org.bukkit.entity.Entity)} instead. */
  @Deprecated
  public boolean isModelArmorStand(org.bukkit.entity.Entity entity) {
    return isModelEntity(entity);
  }

  public Map<UUID, Map<String, Entity>> getActiveStands() {
    return Collections.unmodifiableMap(activeStands);
  }
}
