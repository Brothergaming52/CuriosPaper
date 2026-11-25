package org.bg52.curiospaper.model.packet;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a virtual, packet-level Armor Stand equipped on a player or living mob.
 */
public class PacketArmorStand {

    private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger(1_800_000);

    private final int entityId;
    private final UUID uuid;
    private final UUID ownerUUID;
    private final String slotKey;
    private ItemStack item;
    private Float pitchUpLimit;
    private Float pitchDownLimit;
    private boolean hiddenFromWearer = false;
    private boolean hiddenFromAll = false;

    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private double lastScale = 1.0;

    public PacketArmorStand(UUID ownerUUID, String slotKey, ItemStack item) {
        this.entityId = ENTITY_COUNTER.incrementAndGet();
        this.uuid = UUID.randomUUID();
        this.ownerUUID = ownerUUID;
        this.slotKey = slotKey;
        this.item = item != null ? item.clone() : null;
    }

    public Float getPitchUpLimit() {
        return pitchUpLimit;
    }

    public void setPitchUpLimit(Float pitchUpLimit) {
        this.pitchUpLimit = pitchUpLimit;
    }

    public Float getPitchDownLimit() {
        return pitchDownLimit;
    }

    public void setPitchDownLimit(Float pitchDownLimit) {
        this.pitchDownLimit = pitchDownLimit;
    }

    public boolean isHiddenFromWearer() {
        return hiddenFromWearer;
    }

    public void setHiddenFromWearer(boolean hiddenFromWearer) {
        this.hiddenFromWearer = hiddenFromWearer;
    }

    public boolean isHiddenFromAll() {
        return hiddenFromAll;
    }

    public void setHiddenFromAll(boolean hiddenFromAll) {
        this.hiddenFromAll = hiddenFromAll;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    public double getLastScale() {
        return lastScale;
    }

    public void setLastScale(double lastScale) {
        this.lastScale = lastScale;
    }

    public int getEntityId() {
        return entityId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getSlotKey() {
        return slotKey;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item != null ? item.clone() : null;
    }

    public static boolean isHeadSlot(String slotKey) {
        if (slotKey == null) return false;
        String id = slotKey.contains(":") ? slotKey.substring(0, slotKey.indexOf(':')).toLowerCase(Locale.ROOT) : slotKey.toLowerCase(Locale.ROOT);
        return id.contains("head") || id.contains("hat") || id.contains("helmet") || id.contains("mask") || id.contains("face") || id.contains("glasses") || id.contains("goggles");
    }

    public float calculateTargetYaw(Entity owner) {
        if (owner == null) return 0.0f;
        if (isHeadSlot(slotKey)) {
            return owner.getLocation().getYaw();
        }
        if (owner instanceof LivingEntity living) {
            try {
                return living.getBodyYaw();
            } catch (Throwable t) {
                return living.getLocation().getYaw();
            }
        }
        return owner.getLocation().getYaw();
    }

    public void spawn(Player viewer, Entity owner) {
        if (viewer == null || owner == null || !viewer.isOnline() || !owner.isValid()) return;

        float yaw = calculateTargetYaw(owner);
        Location loc = owner.getLocation().clone();
        loc.setYaw(yaw);
        loc.setPitch(0.0f);

        PacketSender.sendSpawnArmorStand(viewer, entityId, uuid, loc);
        PacketSender.sendArmorStandMetadata(viewer, entityId);

        double currentScale = resolveEntityScale(owner);
        this.lastScale = currentScale;
        if (Math.abs(currentScale - 1.0) > 0.001) {
            PacketSender.sendEntityScale(viewer, entityId, currentScale);
        }

        if (hiddenFromAll || (hiddenFromWearer && viewer.getUniqueId().equals(ownerUUID))) {
            // Stand remains invisible without helmet
        } else if (item != null) {
            PacketSender.sendHelmetEquipment(viewer, entityId, item);
        }
        PacketSender.sendEntityLook(viewer, entityId, yaw, 0.0f);

        this.lastYaw = yaw;
        this.lastPitch = 0.0f;
    }

    public static double resolveEntityScale(Entity entity) {
        if (entity instanceof LivingEntity living) {
            try {
                org.bukkit.attribute.Attribute scaleAttr = org.bg52.curiospaper.model.PacketModelManager.getScaleAttribute();
                if (scaleAttr != null) {
                    org.bukkit.attribute.AttributeInstance inst = living.getAttribute(scaleAttr);
                    if (inst != null) {
                        return inst.getValue();
                    }
                }
            } catch (Throwable ignored) {}
        }
        return 1.0;
    }

    public void updateScale(Player viewer, double scale) {
        if (viewer == null || !viewer.isOnline()) return;
        this.lastScale = scale;
        PacketSender.sendEntityScale(viewer, entityId, scale);
    }

    public void updateRotation(Player viewer, float targetYaw, float targetPitch) {
        if (viewer == null || !viewer.isOnline()) return;
        PacketSender.sendEntityLook(viewer, entityId, targetYaw, targetPitch);
    }

    public void updateEquipment(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        if (hiddenFromAll || (hiddenFromWearer && viewer.getUniqueId().equals(ownerUUID))) {
            PacketSender.sendHelmetEquipment(viewer, entityId, new ItemStack(org.bukkit.Material.AIR));
        } else if (item != null) {
            PacketSender.sendHelmetEquipment(viewer, entityId, item);
        }
    }

    public void destroy(Player viewer) {
        if (viewer == null || !viewer.isOnline()) return;
        PacketSender.sendDestroyEntities(viewer, entityId);
    }
}
