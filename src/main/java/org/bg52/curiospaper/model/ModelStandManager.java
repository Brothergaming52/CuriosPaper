package org.bg52.curiospaper.model;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.event.AccessoryEquipEvent;
import org.bg52.curiospaper.event.CuriosModelEquipEvent;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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
  private final Map<UUID, BukkitTask> pendingRemounts = new ConcurrentHashMap<>();
  private final Map<UUID, RecordedSession> activeRecordings = new ConcurrentHashMap<>();
  
  private final Set<String> rtpCommands = ConcurrentHashMap.newKeySet();
  private final Set<String> rtpBlocks = ConcurrentHashMap.newKeySet();
  private final Set<String> rtpEntities = ConcurrentHashMap.newKeySet();
  private final Set<String> rtpGuis = ConcurrentHashMap.newKeySet();

  private final Set<UUID> dismountedPlayers = ConcurrentHashMap.newKeySet();
  private boolean rtpEnabled = true;

  private final NamespacedKey modelHiddenKey;
  private final NamespacedKey modelStandTag;

  private static final float ROTATION_THRESHOLD = 1.5f;

  public ModelStandManager(CuriosPaper plugin) {
    this.plugin = plugin;
    this.modelHiddenKey = new NamespacedKey(plugin, "curios_model_hidden");
    this.modelStandTag = new NamespacedKey(plugin, "curios_model_stand");
  }

  public boolean isRtpEnabled() {
    return rtpEnabled;
  }

  public void initialize() {
    loadRtpTriggers();
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
    for (BukkitTask task : pendingRemounts.values()) {
      if (task != null) {
        task.cancel();
      }
    }
    pendingRemounts.clear();

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
    UUID playerId = event.getPlayer().getUniqueId();
    activeRecordings.remove(playerId);
    dismountedPlayers.remove(playerId);
    removeAllStands(playerId);
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

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    if (rtpEnabled) {
      // 1. Handle Recording completion on teleport
      if (activeRecordings.containsKey(playerId)) {
        RecordedSession session = activeRecordings.remove(playerId);
        saveRecordedSession(session);
        player.sendMessage("§a✓ Teleport detected! RTP sequence recording auto-saved successfully.");
      }

      // 2. Handle matching cleanup
      dismountedPlayers.remove(playerId);
    }
    
    // Always remove all stands before teleport to prevent plugins (e.g. Multiverse,
    // RTP) from trying to async-teleport the armor stand passengers, which fails.
    // We recreate them at the destination instead.
    removeAllStands(playerId);
    
    // Use a longer delay for cross-world teleports to let the world fully load
    long delay = (event.getFrom().getWorld() != event.getTo().getWorld()) ? 5L : 2L;
    
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if (player.isOnline()) {
        rescanPlayer(player);
      }
    }, delay);
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
  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (modelEntityIds.contains(event.getRightClicked().getUniqueId())) {
      event.setCancelled(true);
      return;
    }
    if (!rtpEnabled) return;

    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();
    Entity clicked = event.getRightClicked();
    
    // 1. Handle Recording
    if (activeRecordings.containsKey(playerId)) {
      RecordedSession session = activeRecordings.get(playerId);
      String name = clicked.getCustomName() != null ? clicked.getCustomName() : clicked.getName();
      if (!session.entities.contains(name.toLowerCase())) {
        session.entities.add(name.toLowerCase());
        player.sendMessage("§e[Recorded] Entity: " + name);
      }
    }

    // 2. Handle matching
    String name = clicked.getCustomName() != null ? clicked.getCustomName() : clicked.getName();
    String typeName = clicked.getType().name();
    String uuidStr = clicked.getUniqueId().toString();
    if (rtpEntities.contains(name.toLowerCase()) || 
        rtpEntities.contains(typeName.toLowerCase()) || 
        rtpEntities.contains(uuidStr)) {
      tempDismount(player);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    if (modelEntityIds.contains(event.getRightClicked().getUniqueId())) {
      event.setCancelled(true);
      return;
    }
    if (!rtpEnabled) return;

    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();
    Entity clicked = event.getRightClicked();
    
    // 1. Handle Recording
    if (activeRecordings.containsKey(playerId)) {
      RecordedSession session = activeRecordings.get(playerId);
      String name = clicked.getCustomName() != null ? clicked.getCustomName() : clicked.getName();
      if (!session.entities.contains(name.toLowerCase())) {
        session.entities.add(name.toLowerCase());
        player.sendMessage("§e[Recorded] Entity: " + name);
      }
    }

    // 2. Handle matching
    String name = clicked.getCustomName() != null ? clicked.getCustomName() : clicked.getName();
    String typeName = clicked.getType().name();
    String uuidStr = clicked.getUniqueId().toString();
    if (rtpEntities.contains(name.toLowerCase()) || 
        rtpEntities.contains(typeName.toLowerCase()) || 
        rtpEntities.contains(uuidStr)) {
      tempDismount(player);
    }
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    Location from = event.getFrom();
    Location to = event.getTo();

    if (to == null) {
      return;
    }

    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();
    if (dismountedPlayers.contains(playerId)) {
      if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
        double distSq = (from.getX() - to.getX()) * (from.getX() - to.getX()) +
                        (from.getY() - to.getY()) * (from.getY() - to.getY()) +
                        (from.getZ() - to.getZ()) * (from.getZ() - to.getZ());
        if (distSq > 0.0025) { // 0.05 blocks distance
          dismountedPlayers.remove(playerId);
          remountStandsIfDismounted(player);
        }
      }
    }

    boolean rotationChanged = Math.abs(from.getYaw() - to.getYaw()) >= ROTATION_THRESHOLD ||
        Math.abs(from.getPitch() - to.getPitch()) >= ROTATION_THRESHOLD;

    // Force update if player is in a state that usually requires model hiding
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

  public static class RecordedSession {
    public final List<String> commands = new ArrayList<>();
    public final List<String> blocks = new ArrayList<>();
    public final List<String> entities = new ArrayList<>();
    public final List<String> guis = new ArrayList<>();
  }

  public void loadRtpTriggers() {
    rtpCommands.clear();
    rtpBlocks.clear();
    rtpEntities.clear();
    rtpGuis.clear();
    dismountedPlayers.clear();

    org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
    rtpEnabled = config.getBoolean("features.rtp.enabled", true);
    if (!rtpEnabled) {
      return;
    }
    
    List<String> cmds = config.getStringList("features.rtp.commands");
    if (cmds != null) {
      for (String cmd : cmds) {
        rtpCommands.add(cmd.toLowerCase());
      }
    }
    if (rtpCommands.isEmpty()) {
      rtpCommands.add("rtp");
      rtpCommands.add("wild");
      config.set("features.rtp.commands", new ArrayList<>(rtpCommands));
      plugin.saveConfig();
    }

    List<String> blks = config.getStringList("features.rtp.blocks");
    if (blks != null) {
      for (String blk : blks) {
        rtpBlocks.add(blk.toUpperCase());
      }
    }

    List<String> ents = config.getStringList("features.rtp.entities");
    if (ents != null) {
      for (String ent : ents) {
        rtpEntities.add(ent.toLowerCase());
      }
    }

    List<String> gs = config.getStringList("features.rtp.guis");
    if (gs != null) {
      for (String g : gs) {
        rtpGuis.add(g.toLowerCase());
      }
    }
  }

  public void toggleRecording(Player player) {
    UUID playerId = player.getUniqueId();
    if (activeRecordings.containsKey(playerId)) {
      RecordedSession session = activeRecordings.remove(playerId);
      saveRecordedSession(session);
      player.sendMessage("§a✓ Recording stopped! Triggers have been saved to config.yml.");
    } else {
      activeRecordings.put(playerId, new RecordedSession());
      player.sendMessage("§e[CuriosPaper] RTP trigger recording started.");
      player.sendMessage("§ePerform the interaction sequence (commands, NPC/block clicks, GUI buttons) and finally teleport.");
      player.sendMessage("§eTeleporting will automatically save the recording. You can also run /curios recordrtp again to stop and save manually.");
    }
  }

  private void saveRecordedSession(RecordedSession session) {
    org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
    
    if (!session.commands.isEmpty()) {
      List<String> cmds = config.getStringList("features.rtp.commands");
      if (cmds == null) cmds = new ArrayList<>();
      for (String cmd : session.commands) {
        if (!cmds.contains(cmd)) {
          cmds.add(cmd);
          rtpCommands.add(cmd.toLowerCase());
        }
      }
      config.set("features.rtp.commands", cmds);
    }

    if (!session.blocks.isEmpty()) {
      List<String> blks = config.getStringList("features.rtp.blocks");
      if (blks == null) blks = new ArrayList<>();
      for (String blk : session.blocks) {
        if (!blks.contains(blk)) {
          blks.add(blk);
          rtpBlocks.add(blk.toUpperCase());
        }
      }
      config.set("features.rtp.blocks", blks);
    }

    if (!session.entities.isEmpty()) {
      List<String> ents = config.getStringList("features.rtp.entities");
      if (ents == null) ents = new ArrayList<>();
      for (String ent : session.entities) {
        if (!ents.contains(ent)) {
          ents.add(ent);
          rtpEntities.add(ent.toLowerCase());
        }
      }
      config.set("features.rtp.entities", ents);
    }

    if (!session.guis.isEmpty()) {
      List<String> gs = config.getStringList("features.rtp.guis");
      if (gs == null) gs = new ArrayList<>();
      for (String g : session.guis) {
        if (!gs.contains(g)) {
          gs.add(g);
          rtpGuis.add(g.toLowerCase());
        }
      }
      config.set("features.rtp.guis", gs);
    }

    plugin.saveConfig();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    if (!rtpEnabled) return;
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();
    String message = event.getMessage();
    if (message.isEmpty() || !message.startsWith("/")) return;
    
    String cmdName = message.split(" ")[0].substring(1).toLowerCase();

    // 1. Handle Recording
    if (activeRecordings.containsKey(playerId)) {
      if (!cmdName.equalsIgnoreCase("curios")) {
        RecordedSession session = activeRecordings.get(playerId);
        if (!session.commands.contains(cmdName)) {
          session.commands.add(cmdName);
          player.sendMessage("§e[Recorded] Command: /" + cmdName);
        }
      }
    }
    
    // 2. Handle matching
    if (rtpCommands.contains(cmdName)) {
      tempDismount(player);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!rtpEnabled) return;
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();
    UUID playerId = player.getUniqueId();
    
    String title = event.getView().getTitle();
    int slot = event.getRawSlot();
    if (slot < 0) return;
    
    // 1. Handle Recording
    if (activeRecordings.containsKey(playerId)) {
      RecordedSession session = activeRecordings.get(playerId);
      String key = title.toLowerCase() + ":" + slot;
      if (!session.guis.contains(key)) {
        session.guis.add(key);
        player.sendMessage("§e[Recorded] GUI Click: '" + title + "' slot " + slot);
      }
    }

    // 2. Handle matching
    String guiKey = title.toLowerCase();
    String guiSlotKey = title.toLowerCase() + ":" + slot;
    if (rtpGuis.contains(guiKey) || rtpGuis.contains(guiSlotKey)) {
      tempDismount(player);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!rtpEnabled) return;
    if (!(event.getWhoClicked() instanceof Player)) return;
    Player player = (Player) event.getWhoClicked();
    UUID playerId = player.getUniqueId();
    
    String title = event.getView().getTitle();
    
    // 1. Handle Recording
    if (activeRecordings.containsKey(playerId)) {
      RecordedSession session = activeRecordings.get(playerId);
      String key = title.toLowerCase();
      if (!session.guis.contains(key)) {
        session.guis.add(key);
        player.sendMessage("§e[Recorded] GUI Drag in: '" + title + "'");
      }
    }

    // 2. Handle matching
    String guiKey = title.toLowerCase();
    if (rtpGuis.contains(guiKey)) {
      tempDismount(player);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (!rtpEnabled) return;
    Player player = event.getPlayer();
    UUID playerId = player.getUniqueId();

    // 1. Handle Recording
    if (activeRecordings.containsKey(playerId)) {
      if (event.getClickedBlock() != null) {
        Material type = event.getClickedBlock().getType();
        String locStr = event.getClickedBlock().getWorld().getName() + "," +
                       event.getClickedBlock().getX() + "," +
                       event.getClickedBlock().getY() + "," +
                       event.getClickedBlock().getZ();
        RecordedSession session = activeRecordings.get(playerId);
        
        if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
          String key = type.name();
          if (!session.blocks.contains(key)) {
            session.blocks.add(key);
            player.sendMessage("§e[Recorded] Stepped on block type: " + key);
          }
        } else if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
          String keyName = type.name();
          if (keyName.contains("BUTTON") || keyName.contains("SIGN") || type == Material.LEVER) {
            if (!session.blocks.contains(locStr)) {
              session.blocks.add(locStr);
              player.sendMessage("§e[Recorded] Interacted block location: " + locStr);
            }
          }
        }
      }
    }

    // 2. Handle matching
    if (event.getClickedBlock() != null) {
      Material type = event.getClickedBlock().getType();
      String locStr = event.getClickedBlock().getWorld().getName() + "," +
                     event.getClickedBlock().getX() + "," +
                     event.getClickedBlock().getY() + "," +
                     event.getClickedBlock().getZ();
      if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL) {
        if (rtpBlocks.contains(type.name())) {
          tempDismount(player);
        }
      } else if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
        if (rtpBlocks.contains(type.name()) || rtpBlocks.contains(locStr)) {
          tempDismount(player);
        } else {
          String name = type.name();
          if (name.contains("BUTTON") || name.contains("SIGN") || type == Material.LEVER) {
            tempDismount(player);
          }
        }
      }
    }
  }

  public void tempDismount(Player player) {
    Map<String, Entity> playerStands = activeStands.get(player.getUniqueId());
    if (playerStands == null || playerStands.isEmpty()) {
      return;
    }

    for (Entity entity : playerStands.values()) {
      if (entity != null && entity.isValid()) {
        player.removePassenger(entity);
      }
    }

    dismountedPlayers.add(player.getUniqueId());
  }

  private void remountStandsIfDismounted(Player player) {
    Map<String, Entity> playerStands = activeStands.get(player.getUniqueId());
    if (playerStands == null || playerStands.isEmpty()) {
      return;
    }

    for (Entity entity : playerStands.values()) {
      if (entity != null && entity.isValid() && entity.getWorld().equals(player.getWorld())) {
        if (!player.getPassengers().contains(entity)) {
          entity.teleport(player.getLocation());
          player.addPassenger(entity);
        }
      }
    }
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

    ItemStack modelHelmet = createModelHelmet(player, item, slotKey, itemData);
    if (modelHelmet == null)
      return;

    spawnModelStand(player, slotKey, modelHelmet, itemData);

    // Initial sync
    if (modelHelmet != null) {
      updateStandsForPlayer(player, true);
    }
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
      if (equip != null && modelHelmet != null) {
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

  private ItemStack createModelHelmet(Player player, ItemStack curiosityStack, String slotKey, ItemData itemData) {
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

    String[] parts = slotKey.split(":");
    String slotType = parts.length > 0 ? parts[0] : "unknown";
    int slotIndex = 0;
    if (parts.length > 1) {
      try {
        slotIndex = Integer.parseInt(parts[1]);
      } catch (NumberFormatException ignored) {
      }
    }

    CuriosModelEquipEvent event = new CuriosModelEquipEvent(player, curiosityStack, slotType, slotIndex,
        modelMat, itemData.getModelCustomModelData(), itemData.getModelItemModel());
    Bukkit.getPluginManager().callEvent(event);

    if (event.isCancelled()) {
      return null;
    }

    ItemStack helmet = new ItemStack(event.getModelMaterial());
    ItemMeta meta = helmet.getItemMeta();
    if (meta != null) {
      VersionUtil.setItemModelSafe(meta, event.getItemModel(), event.getCustomModelData());
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
    long currentTime = System.currentTimeMillis();
    if (stateHide) {
      lastHideTickState.put(player.getUniqueId(), currentTime);
      shouldHide = true;
    } else {
      Long lastTime = lastHideTickState.get(player.getUniqueId());
      if (lastTime != null) {
        long diff = currentTime - lastTime;
        if (diff >= 0 && diff < 250) { // 250ms = 5 ticks
          shouldHide = true;
        }
      }
    }

    // Pitch limits
    boolean hideOnlyForWearer = false;
    if (!shouldHide) {
      float pitch = player.getLocation().getPitch();
      Float pitchUp = itemData.getPitchUpLimit();
      Float pitchDown = itemData.getPitchDownLimit();
      if (pitchUp != null && pitch < -pitchUp) {
        shouldHide = true;
        hideOnlyForWearer = true;
      }
      if (pitchDown != null && pitch > pitchDown) {
        shouldHide = true;
        hideOnlyForWearer = true;
      }
    }

    EntityEquipment equip = null;
    if (entity instanceof ArmorStand) {
      equip = ((ArmorStand) entity).getEquipment();
    }

    if (equip == null)
      return;

    if (shouldHide) {
      if (hideOnlyForWearer) {
        // Hide only for wearer: keep helmet on stand but hide stand from wearer
        ItemStack current = equip.getHelmet();
        ItemStack modelHelmet = createModelHelmet(player, equippedItem, slotKey, itemData);
        if (modelHelmet != null && (current == null || !modelHelmet.isSimilar(current))) {
          equip.setHelmet(modelHelmet);
        }
        if (canSeeReflection(player, entity)) {
          hideEntityReflection(player, entity);
        }
      } else {
        // Hide for everyone: remove helmet from stand and hide stand from wearer (just in case)
        if (equip.getHelmet() != null && equip.getHelmet().getType() != Material.AIR) {
          equip.setHelmet(null);
        }
        if (canSeeReflection(player, entity)) {
          hideEntityReflection(player, entity);
        }
      }
    } else {
      // Show for everyone (including wearer)
      ItemStack current = equip.getHelmet();
      ItemStack modelHelmet = createModelHelmet(player, equippedItem, slotKey, itemData);
      
      // Refresh helmet if it changed (e.g. dyed) or is missing
      if (modelHelmet != null && (current == null || !modelHelmet.isSimilar(current))) {
        equip.setHelmet(modelHelmet);
      }
      if (!canSeeReflection(player, entity)) {
        showEntityReflection(player, entity);
      }
    }
  }

  private void hideEntityReflection(Player player, Entity entity) {
    try {
      java.lang.reflect.Method hideMethod = player.getClass().getMethod("hideEntity", org.bukkit.plugin.Plugin.class, org.bukkit.entity.Entity.class);
      hideMethod.invoke(player, plugin, entity);
    } catch (Throwable ignored) {
    }
  }

  private void showEntityReflection(Player player, Entity entity) {
    try {
      java.lang.reflect.Method showMethod = player.getClass().getMethod("showEntity", org.bukkit.plugin.Plugin.class, org.bukkit.entity.Entity.class);
      showMethod.invoke(player, plugin, entity);
    } catch (Throwable ignored) {
    }
  }

  private boolean canSeeReflection(Player player, Entity entity) {
    try {
      java.lang.reflect.Method canSeeMethod = player.getClass().getMethod("canSee", org.bukkit.entity.Entity.class);
      return (Boolean) canSeeMethod.invoke(player, entity);
    } catch (Throwable ignored) {
      return true;
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
    BukkitTask pending = pendingRemounts.remove(playerId);
    if (pending != null) {
      pending.cancel();
    }

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
