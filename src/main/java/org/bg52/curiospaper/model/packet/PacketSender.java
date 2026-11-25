package org.bg52.curiospaper.model.packet;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Universal reflection packet dispatcher supporting Mojang mappings, Spigot/CraftBukkit mappings,
 * and Paper 1.20.6 through 26.x+.
 */
public final class PacketSender {

    private static boolean initialized = false;
    private static boolean available = false;
    private static String failureReason = "Not initialized";

    // ByteBuf & Serialization Cache
    private static Class<?> friendlyByteBufClass;
    private static Constructor<?> friendlyByteBufConstructor;
    private static Method unpooledBufferMethod;
    private static Method writeVarIntMethod;
    private static Method writeByteMethod;
    private static Method writeVarIntArrayMethod;

    // Buffer Constructors (Modern 1.20.2 through 26.x+)
    private static Constructor<?> rotateHeadBufConstructor;
    private static Constructor<?> setPassengersBufConstructor;
    private static Constructor<?> removeEntitiesBufConstructor;

    // NMS Classes
    private static Class<?> craftPlayerClass;
    private static Class<?> craftItemStackClass;
    private static Class<?> packetClass;
    private static Class<?> entityTypeClass;
    private static Class<?> vec3Class;
    private static Class<?> addEntityPacketClass;
    private static Class<?> setEquipmentPacketClass;
    private static Class<?> setPassengersPacketClass;
    private static Class<?> removeEntitiesPacketClass;
    private static Class<?> setEntityDataPacketClass;
    private static Class<?> dataValueClass;
    private static Class<?> rotateHeadPacketClass;
    private static Class<?> moveEntityRotPacketClass;

    // Methods & Fields
    private static Method getHandleMethod;
    private static Field connectionField;
    private static Method sendPacketMethod;
    private static Method asNMSCopyMethod;

    // EntityType.ARMOR_STAND & Entity Data
    private static Object armorStandType;
    private static Object vec3Zero;
    private static Object byteSerializer;
    private static Constructor<?> dataValueConstructor;
    private static Constructor<?> setEntityDataPacketConstructor;

    // Rotation Constructors & Fields
    private static Constructor<?> rotateHeadPacketConstructor;
    private static Field rotateHeadEntityIdField;
    private static Field rotateHeadYawField;
    private static Constructor<?> moveEntityRotConstructor;
    private static Field moveEntityIdField;
    private static Field moveEntityYawField;
    private static Field moveEntityPitchField;
    private static Field moveEntityHasRotField;

    // Attributes & Scale
    private static Class<?> updateAttributesPacketClass;
    private static Class<?> attributeSnapshotClass;
    private static Constructor<?> updateAttributesConstructor;
    private static Constructor<?> snapshotConstructor;
    private static Field updateAttributesEntityIdField;
    private static Field updateAttributesValuesField;
    private static Object scaleHolder;

    static {
        init();
    }

    private PacketSender() {}

    /**
     * Safely sets a field value via standard reflection.
     */
    public static void setField(Object obj, Field field, Object value) {
        if (obj == null || field == null) return;
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Throwable ignored) {}
    }

    /**
     * Creates a new FriendlyByteBuf backed by a Netty ByteBuf.
     */
    private static Object createBuffer() {
        if (unpooledBufferMethod == null || friendlyByteBufConstructor == null) return null;
        try {
            Object byteBuf = unpooledBufferMethod.invoke(null);
            return friendlyByteBufConstructor.newInstance(byteBuf);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Writes a VarInt into the packet buffer.
     */
    private static void writeVarInt(Object buf, int value) {
        if (buf == null) return;
        if (writeVarIntMethod != null) {
            try {
                writeVarIntMethod.invoke(buf, value);
                return;
            } catch (Throwable ignored) {}
        }
        try {
            if (buf instanceof io.netty.buffer.ByteBuf) {
                io.netty.buffer.ByteBuf byteBuf = (io.netty.buffer.ByteBuf) buf;
                while ((value & -128) != 0) {
                    byteBuf.writeByte(value & 127 | 128);
                    value >>>= 7;
                }
                byteBuf.writeByte(value);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Writes a byte into the packet buffer.
     */
    private static void writeByte(Object buf, byte value) {
        if (buf == null) return;
        if (writeByteMethod != null) {
            try {
                writeByteMethod.invoke(buf, (int) value);
                return;
            } catch (Throwable ignored) {}
        }
        try {
            if (buf instanceof io.netty.buffer.ByteBuf) {
                ((io.netty.buffer.ByteBuf) buf).writeByte(value);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Writes an int array as VarInt-prefixed array into the packet buffer.
     */
    private static void writeVarIntArray(Object buf, int[] array) {
        if (buf == null) return;
        if (writeVarIntArrayMethod != null) {
            try {
                writeVarIntArrayMethod.invoke(buf, (Object) array);
                return;
            } catch (Throwable ignored) {}
        }
        int[] safe = array != null ? array : new int[0];
        writeVarInt(buf, safe.length);
        for (int id : safe) {
            writeVarInt(buf, id);
        }
    }

    public static Class<?> findClass(String... names) {
        for (String name : names) {
            if (name == null || name.isEmpty()) continue;
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        try {
            String cbPackage = Bukkit.getServer().getClass().getPackage().getName();

            // 1. CraftPlayer & CraftItemStack
            craftPlayerClass = findClass(
                    cbPackage + ".entity.CraftPlayer",
                    "org.bukkit.craftbukkit.entity.CraftPlayer"
            );
            if (craftPlayerClass != null) {
                getHandleMethod = craftPlayerClass.getMethod("getHandle");
            }

            craftItemStackClass = findClass(
                    cbPackage + ".inventory.CraftItemStack",
                    "org.bukkit.craftbukkit.inventory.CraftItemStack"
            );
            if (craftItemStackClass != null) {
                asNMSCopyMethod = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);
            }

            // 2. NMS Packet & Entity classes (Mojang and Spigot mappings)
            packetClass = findClass(
                    "net.minecraft.network.protocol.Packet",
                    "net.minecraft.server.network.Packet"
            );

            entityTypeClass = findClass(
                    "net.minecraft.world.entity.EntityType",
                    "net.minecraft.world.entity.EntityTypes"
            );

            if (entityTypeClass != null) {
                try {
                    armorStandType = entityTypeClass.getField("ARMOR_STAND").get(null);
                } catch (Throwable t) {
                    for (Field f : entityTypeClass.getFields()) {
                        if (f.getName().equalsIgnoreCase("ARMOR_STAND")) {
                            armorStandType = f.get(null);
                            break;
                        }
                    }
                }
            }

            // CraftEntityType fallback for ARMOR_STAND
            if (armorStandType == null) {
                Class<?> craftEntityTypeClass = findClass(
                        cbPackage + ".entity.CraftEntityType",
                        "org.bukkit.craftbukkit.entity.CraftEntityType"
                );
                if (craftEntityTypeClass != null) {
                    try {
                        Method b2m = craftEntityTypeClass.getMethod("bukkitToMinecraft", org.bukkit.entity.EntityType.class);
                        armorStandType = b2m.invoke(null, org.bukkit.entity.EntityType.ARMOR_STAND);
                    } catch (Throwable ignored) {}
                }
            }

            vec3Class = findClass(
                    "net.minecraft.world.phys.Vec3",
                    "net.minecraft.world.phys.Vec3D"
            );

            if (vec3Class != null) {
                try {
                    vec3Zero = vec3Class.getField("ZERO").get(null);
                } catch (Throwable t) {
                    for (Field f : vec3Class.getFields()) {
                        if (f.getName().equalsIgnoreCase("ZERO")) {
                            vec3Zero = f.get(null);
                            break;
                        }
                    }
                    if (vec3Zero == null) {
                        try {
                            Constructor<?> vc = vec3Class.getConstructor(double.class, double.class, double.class);
                            vec3Zero = vc.newInstance(0.0, 0.0, 0.0);
                        } catch (Throwable ignored) {}
                    }
                }
            }

            addEntityPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundAddEntityPacket",
                    "net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity"
            );

            setEquipmentPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket",
                    "net.minecraft.network.protocol.game.PacketPlayOutEntityEquipment"
            );

            setPassengersPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundSetPassengersPacket",
                    "net.minecraft.network.protocol.game.PacketPlayOutMount"
            );

            removeEntitiesPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket",
                    "net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy"
            );

            // 3. Entity Metadata Packet & DataValue classes (Mojang and Spigot mappings)
            setEntityDataPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket",
                    "net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata"
            );

            dataValueClass = findClass(
                    "net.minecraft.network.syncher.SynchedEntityData$DataValue",
                    "net.minecraft.network.syncher.DataWatcher$b"
            );

            Class<?> serializersClass = findClass(
                    "net.minecraft.network.syncher.EntityDataSerializers",
                    "net.minecraft.network.syncher.DataWatcherRegistry"
            );

            if (serializersClass != null) {
                try {
                    byteSerializer = serializersClass.getField("BYTE").get(null);
                } catch (Throwable t) {
                    for (Field f : serializersClass.getFields()) {
                        if (f.getName().equalsIgnoreCase("BYTE") || f.getName().equals("a")) {
                            try {
                                byteSerializer = f.get(null);
                                break;
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }

            if (dataValueClass != null) {
                for (Constructor<?> c : dataValueClass.getDeclaredConstructors()) {
                    c.setAccessible(true);
                    if (c.getParameterCount() == 3 && c.getParameterTypes()[0] == int.class) {
                        dataValueConstructor = c;
                        break;
                    }
                }
            }

            if (setEntityDataPacketClass != null) {
                for (Constructor<?> c : setEntityDataPacketClass.getDeclaredConstructors()) {
                    c.setAccessible(true);
                    if (c.getParameterCount() == 2 && c.getParameterTypes()[0] == int.class && List.class.isAssignableFrom(c.getParameterTypes()[1])) {
                        setEntityDataPacketConstructor = c;
                        break;
                    }
                }
            }

            // 4. Rotation packets: Head rotation & Entity look
            rotateHeadPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundRotateHeadPacket",
                    "net.minecraft.network.protocol.game.PacketPlayOutEntityHeadRotation"
            );

            if (rotateHeadPacketClass != null) {
                for (Constructor<?> c : rotateHeadPacketClass.getDeclaredConstructors()) {
                    c.setAccessible(true);
                    if (c.getParameterCount() == 2 && c.getParameterTypes()[0] == int.class && c.getParameterTypes()[1] == byte.class) {
                        rotateHeadPacketConstructor = c;
                        break;
                    }
                }
                for (Field f : rotateHeadPacketClass.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    f.setAccessible(true);
                    if (f.getType() == int.class && rotateHeadEntityIdField == null) {
                        rotateHeadEntityIdField = f;
                    } else if (f.getType() == byte.class && rotateHeadYawField == null) {
                        rotateHeadYawField = f;
                    }
                }
            }

            moveEntityRotPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundMoveEntityPacket$Rot",
                    "net.minecraft.network.protocol.game.PacketPlayOutEntity$PacketPlayOutEntityLook"
            );

            // Dynamic inner class scan fallback for entity look packet
            if (moveEntityRotPacketClass == null) {
                Class<?> baseMoveClass = findClass(
                        "net.minecraft.network.protocol.game.ClientboundMoveEntityPacket",
                        "net.minecraft.network.protocol.game.PacketPlayOutEntity"
                );
                if (baseMoveClass != null) {
                    for (Class<?> sub : baseMoveClass.getDeclaredClasses()) {
                        if (sub.getSimpleName().equalsIgnoreCase("Rot") || sub.getSimpleName().contains("Look")) {
                            moveEntityRotPacketClass = sub;
                            break;
                        }
                    }
                    if (moveEntityRotPacketClass == null) {
                        for (Class<?> sub : baseMoveClass.getDeclaredClasses()) {
                            for (Constructor<?> c : sub.getDeclaredConstructors()) {
                                if (c.getParameterCount() == 4
                                        && c.getParameterTypes()[0] == int.class
                                        && c.getParameterTypes()[1] == byte.class
                                        && c.getParameterTypes()[2] == byte.class
                                        && c.getParameterTypes()[3] == boolean.class) {
                                    moveEntityRotPacketClass = sub;
                                    moveEntityRotConstructor = c;
                                    c.setAccessible(true);
                                    break;
                                }
                            }
                            if (moveEntityRotPacketClass != null) break;
                        }
                    }
                }
            }

            if (moveEntityRotPacketClass != null && moveEntityRotConstructor == null) {
                for (Constructor<?> c : moveEntityRotPacketClass.getDeclaredConstructors()) {
                    c.setAccessible(true);
                    if (c.getParameterCount() == 4
                            && c.getParameterTypes()[0] == int.class
                            && c.getParameterTypes()[1] == byte.class
                            && c.getParameterTypes()[2] == byte.class
                            && c.getParameterTypes()[3] == boolean.class) {
                        moveEntityRotConstructor = c;
                        break;
                    }
                }
            }

            if (moveEntityRotPacketClass != null) {
                Class<?> cl = moveEntityRotPacketClass;
                while (cl != null && cl != Object.class) {
                    for (Field f : cl.getDeclaredFields()) {
                        if (Modifier.isStatic(f.getModifiers())) continue;
                        f.setAccessible(true);
                        if (f.getType() == int.class && moveEntityIdField == null) {
                            moveEntityIdField = f;
                        } else if (f.getType() == byte.class) {
                            if (moveEntityYawField == null) {
                                moveEntityYawField = f;
                            } else if (moveEntityPitchField == null) {
                                moveEntityPitchField = f;
                            }
                        } else if (f.getType() == boolean.class) {
                            if (f.getName().equalsIgnoreCase("hasRot") || f.getName().equals("g") || f.getName().equals("h")) {
                                moveEntityHasRotField = f;
                            }
                        }
                    }
                    cl = cl.getSuperclass();
                }
            }

            // 4. Update Attributes Packet for generic.scale
            updateAttributesPacketClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket",
                    "net.minecraft.network.protocol.game.PacketPlayOutUpdateAttributes"
            );
            attributeSnapshotClass = findClass(
                    "net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket$AttributeSnapshot",
                    "net.minecraft.network.protocol.game.PacketPlayOutUpdateAttributes$AttributeSnapshot"
            );

            Class<?> attributesClass = findClass(
                    "net.minecraft.world.entity.ai.attributes.Attributes",
                    "net.minecraft.world.entity.ai.attributes.GenericAttributes"
            );
            if (attributesClass != null) {
                try {
                    Field scaleField = attributesClass.getField("SCALE");
                    scaleHolder = scaleField.get(null);
                } catch (Throwable ignored) {}
            }

            if (updateAttributesPacketClass != null && attributeSnapshotClass != null) {
                for (Constructor<?> ctor : attributeSnapshotClass.getDeclaredConstructors()) {
                    if (ctor.getParameterCount() == 3) {
                        ctor.setAccessible(true);
                        snapshotConstructor = ctor;
                        break;
                    }
                }
                for (Constructor<?> ctor : updateAttributesPacketClass.getDeclaredConstructors()) {
                    Class<?>[] pts = ctor.getParameterTypes();
                    if (pts.length == 2 && pts[0] == int.class && List.class.isAssignableFrom(pts[1])) {
                        ctor.setAccessible(true);
                        updateAttributesConstructor = ctor;
                        break;
                    }
                }
                for (Field f : updateAttributesPacketClass.getDeclaredFields()) {
                    if (f.getType() == int.class) {
                        updateAttributesEntityIdField = f;
                    } else if (List.class.isAssignableFrom(f.getType())) {
                        updateAttributesValuesField = f;
                    }
                }
            }

            // 5. FriendlyByteBuf and Netty ByteBuf serialization support
            Class<?> unpooledClass = findClass("io.netty.buffer.Unpooled");
            if (unpooledClass != null) {
                try {
                    unpooledBufferMethod = unpooledClass.getMethod("buffer");
                } catch (Throwable ignored) {}
            }

            friendlyByteBufClass = findClass(
                    "net.minecraft.network.FriendlyByteBuf",
                    "net.minecraft.network.PacketDataSerializer"
            );
            if (friendlyByteBufClass != null) {
                Class<?> byteBufClass = findClass("io.netty.buffer.ByteBuf");
                if (byteBufClass != null) {
                    try {
                        friendlyByteBufConstructor = friendlyByteBufClass.getConstructor(byteBufClass);
                        friendlyByteBufConstructor.setAccessible(true);
                    } catch (Throwable ignored) {}
                }
                for (Method m : friendlyByteBufClass.getMethods()) {
                    if (m.getParameterCount() == 1) {
                        if (m.getParameterTypes()[0] == int.class && (m.getName().equals("writeVarInt") || m.getName().equals("d"))) {
                            writeVarIntMethod = m;
                        } else if (m.getParameterTypes()[0] == int.class && m.getName().equals("writeByte")) {
                            writeByteMethod = m;
                        } else if (m.getParameterTypes()[0] == int[].class && (m.getName().equals("writeVarIntArray") || m.getName().equals("a"))) {
                            writeVarIntArrayMethod = m;
                        }
                    }
                }
            }

            // Buffer constructor lookups for rotation, passengers, and entity destroy
            if (rotateHeadPacketClass != null && friendlyByteBufClass != null) {
                for (Constructor<?> c : rotateHeadPacketClass.getDeclaredConstructors()) {
                    if (c.getParameterCount() == 1 && c.getParameterTypes()[0].isAssignableFrom(friendlyByteBufClass)) {
                        c.setAccessible(true);
                        rotateHeadBufConstructor = c;
                        break;
                    }
                }
            }

            if (setPassengersPacketClass != null && friendlyByteBufClass != null) {
                for (Constructor<?> c : setPassengersPacketClass.getDeclaredConstructors()) {
                    if (c.getParameterCount() == 1 && c.getParameterTypes()[0].isAssignableFrom(friendlyByteBufClass)) {
                        c.setAccessible(true);
                        setPassengersBufConstructor = c;
                        break;
                    }
                }
            }

            if (removeEntitiesPacketClass != null && friendlyByteBufClass != null) {
                for (Constructor<?> c : removeEntitiesPacketClass.getDeclaredConstructors()) {
                    if (c.getParameterCount() == 1 && c.getParameterTypes()[0].isAssignableFrom(friendlyByteBufClass)) {
                        c.setAccessible(true);
                        removeEntitiesBufConstructor = c;
                        break;
                    }
                }
            }

            // Validation
            if (getHandleMethod == null) {
                failureReason = "CraftPlayer.getHandle not available";
                available = false;
                return;
            }
            if (asNMSCopyMethod == null) {
                failureReason = "CraftItemStack.asNMSCopy not available";
                available = false;
                return;
            }
            if (packetClass == null) {
                failureReason = "NMS Packet interface not found";
                available = false;
                return;
            }
            if (armorStandType == null) {
                failureReason = "EntityType.ARMOR_STAND not found";
                available = false;
                return;
            }
            if (addEntityPacketClass == null) {
                failureReason = "Spawn entity packet class not found";
                available = false;
                return;
            }

            available = true;
            failureReason = "OK";
        } catch (Throwable t) {
            available = false;
            failureReason = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static String getFailureReason() {
        return failureReason;
    }

    public static void sendPacket(Player player, Object packet) {
        if (!available || player == null || packet == null) return;
        try {
            Object serverPlayer = getHandleMethod.invoke(player);
            if (connectionField == null) {
                try {
                    connectionField = serverPlayer.getClass().getField("connection");
                } catch (NoSuchFieldException e) {
                    try {
                        connectionField = serverPlayer.getClass().getField("b");
                    } catch (NoSuchFieldException e2) {
                        for (Field f : serverPlayer.getClass().getFields()) {
                            if (f.getType().getName().contains("PacketListener") || f.getType().getName().contains("Connection")) {
                                connectionField = f;
                                break;
                            }
                        }
                    }
                }
            }
            if (connectionField == null) return;
            Object connection = connectionField.get(serverPlayer);
            if (connection == null) return;

            if (sendPacketMethod == null) {
                for (Method m : connection.getClass().getMethods()) {
                    if ((m.getName().equals("send") || m.getName().equals("sendPacket") || m.getName().equals("b"))
                            && m.getParameterCount() == 1 && (packetClass == null || m.getParameterTypes()[0].isAssignableFrom(packet.getClass()))) {
                        sendPacketMethod = m;
                        break;
                    }
                }
            }
            if (sendPacketMethod != null) {
                sendPacketMethod.invoke(connection, packet);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Constructs and sends ClientboundAddEntityPacket / PacketPlayOutSpawnEntity for a packet armor stand.
     */
    public static void sendSpawnArmorStand(Player viewer, int entityId, UUID uuid, Location loc) {
        if (!available) return;
        try {
            Constructor<?> ctor = null;
            for (Constructor<?> c : addEntityPacketClass.getDeclaredConstructors()) {
                c.setAccessible(true);
                if (c.getParameterCount() >= 7 && c.getParameterTypes()[0] == int.class && c.getParameterTypes()[1] == UUID.class) {
                    ctor = c;
                    break;
                }
            }
            if (ctor != null) {
                Object packet;
                if (ctor.getParameterCount() == 11) {
                    packet = ctor.newInstance(
                            entityId, uuid, loc.getX(), loc.getY(), loc.getZ(),
                            loc.getPitch(), loc.getYaw(), armorStandType, 0, vec3Zero, (double) loc.getYaw()
                    );
                } else if (ctor.getParameterCount() == 7) {
                    packet = ctor.newInstance(
                            entityId, uuid, loc.getX(), loc.getY(), loc.getZ(),
                            loc.getPitch(), loc.getYaw()
                    );
                } else {
                    Object[] args = new Object[ctor.getParameterCount()];
                    args[0] = entityId;
                    args[1] = uuid;
                    args[2] = loc.getX();
                    args[3] = loc.getY();
                    args[4] = loc.getZ();
                    args[5] = loc.getPitch();
                    args[6] = loc.getYaw();
                    if (args.length > 7) args[7] = armorStandType;
                    if (args.length > 8) args[8] = 0;
                    if (args.length > 9) args[9] = vec3Zero;
                    if (args.length > 10) args[10] = (double) loc.getYaw();
                    packet = ctor.newInstance(args);
                }
                sendPacket(viewer, packet);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Constructs and sends ClientboundSetEquipmentPacket / PacketPlayOutEntityEquipment for the helmet slot.
     */
    public static void sendHelmetEquipment(Player viewer, int entityId, ItemStack item) {
        if (!available || item == null) return;
        try {
            Object nmsItem = asNMSCopyMethod.invoke(null, item);
            Class<?> nmsSlotClass = findClass(
                    "net.minecraft.world.entity.EquipmentSlot",
                    "net.minecraft.world.entity.EnumItemSlot"
            );
            Object headSlot = null;
            if (nmsSlotClass != null) {
                try {
                    headSlot = nmsSlotClass.getField("HEAD").get(null);
                } catch (Throwable t) {
                    for (Field f : nmsSlotClass.getFields()) {
                        if (f.getName().equalsIgnoreCase("HEAD")) {
                            headSlot = f.get(null);
                            break;
                        }
                    }
                }
            }

            Class<?> pairClass = findClass("com.mojang.datafixers.util.Pair");
            if (pairClass != null && headSlot != null && setEquipmentPacketClass != null) {
                Method ofMethod = pairClass.getMethod("of", Object.class, Object.class);
                Object pair = ofMethod.invoke(null, headSlot, nmsItem);

                List<Object> list = Collections.singletonList(pair);
                for (Constructor<?> c : setEquipmentPacketClass.getDeclaredConstructors()) {
                    c.setAccessible(true);
                    if (c.getParameterCount() == 2 && c.getParameterTypes()[0] == int.class && List.class.isAssignableFrom(c.getParameterTypes()[1])) {
                        Object packet = c.newInstance(entityId, list);
                        sendPacket(viewer, packet);
                        return;
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Constructs and sends ClientboundSetPassengersPacket / PacketPlayOutMount.
     */
    public static void sendMountPassenger(Player viewer, int vehicleId, int passengerId) {
        sendSetPassengers(viewer, vehicleId, new int[]{passengerId});
    }

    /**
     * Constructs and sends ClientboundSetPassengersPacket for multiple passengers simultaneously.
     * Attaches all specified passenger entity IDs to the vehicle entity.
     */
    public static void sendSetPassengers(Player viewer, int vehicleId, int[] passengerIds) {
        if (!available || setPassengersPacketClass == null) return;
        try {
            int[] safePassengers = passengerIds != null ? passengerIds : new int[0];

            // 1. Modern Buffer Constructor (1.20.2 through 26.x+)
            if (setPassengersBufConstructor != null) {
                Object buf = createBuffer();
                if (buf != null) {
                    writeVarInt(buf, vehicleId);
                    writeVarIntArray(buf, safePassengers);
                    Object packet = setPassengersBufConstructor.newInstance(buf);
                    sendPacket(viewer, packet);
                    return;
                }
            }

            // 2. Direct Parameter Constructor (1.17 - 1.20.1)
            for (Constructor<?> c : setPassengersPacketClass.getDeclaredConstructors()) {
                c.setAccessible(true);
                if (c.getParameterCount() == 2 && c.getParameterTypes()[0] == int.class && c.getParameterTypes()[1] == int[].class) {
                    Object packet = c.newInstance(vehicleId, safePassengers);
                    sendPacket(viewer, packet);
                    return;
                }
            }

            // 3. Reflection fallback
            try {
                Constructor<?> defCtor = setPassengersPacketClass.getDeclaredConstructor();
                defCtor.setAccessible(true);
                Object packet = defCtor.newInstance();
                for (Field f : setPassengersPacketClass.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType() == int.class) {
                        setField(packet, f, vehicleId);
                    } else if (f.getType() == int[].class) {
                        setField(packet, f, safePassengers);
                    }
                }
                sendPacket(viewer, packet);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /**
     * Constructs and sends ClientboundRemoveEntitiesPacket / PacketPlayOutEntityDestroy.
     */
    public static void sendDestroyEntities(Player viewer, int... entityIds) {
        if (!available || entityIds.length == 0 || removeEntitiesPacketClass == null) return;
        try {
            for (Constructor<?> c : removeEntitiesPacketClass.getDeclaredConstructors()) {
                c.setAccessible(true);
                if (c.getParameterCount() == 1) {
                    if (c.getParameterTypes()[0] == int[].class) {
                        Object packet = c.newInstance((Object) entityIds);
                        sendPacket(viewer, packet);
                        return;
                    } else if (c.getParameterTypes()[0].getName().contains("IntList")) {
                        Class<?> intArrayListClass = findClass("it.unimi.dsi.fastutil.ints.IntArrayList");
                        if (intArrayListClass != null) {
                            Constructor<?> ilCtor = intArrayListClass.getConstructor(int[].class);
                            Object packet = c.newInstance(ilCtor.newInstance((Object) entityIds));
                            sendPacket(viewer, packet);
                            return;
                        }
                    }
                }
            }

            // Modern Buffer Constructor
            if (removeEntitiesBufConstructor != null) {
                Object buf = createBuffer();
                if (buf != null) {
                    writeVarIntArray(buf, entityIds);
                    Object packet = removeEntitiesBufConstructor.newInstance(buf);
                    sendPacket(viewer, packet);
                    return;
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Constructs and sends ClientboundSetEntityDataPacket / PacketPlayOutEntityMetadata
     * ensuring the virtual armor stand is completely invisible (flag 0x20) and a marker without baseplate (flag 0x18).
     */
    public static void sendArmorStandMetadata(Player viewer, int entityId) {
        if (!available || viewer == null || setEntityDataPacketClass == null) return;
        try {
            List<Object> items = new ArrayList<>();

            if (dataValueConstructor != null && byteSerializer != null) {
                // Index 0: Entity Flags -> 0x20 (Invisible)
                Object flagItem = dataValueConstructor.newInstance(0, byteSerializer, (byte) 0x20);
                // Index 15: ArmorStand Flags -> 0x10 (Marker) | 0x08 (No Baseplate) = 0x18
                Object standItem = dataValueConstructor.newInstance(15, byteSerializer, (byte) 0x18);
                items.add(flagItem);
                items.add(standItem);
            }

            if (!items.isEmpty() && setEntityDataPacketConstructor != null) {
                Object packet = setEntityDataPacketConstructor.newInstance(entityId, items);
                sendPacket(viewer, packet);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Constructs and sends ClientboundMoveEntityPacket$Rot / PacketPlayOutEntityLook to update
     * the entity's body yaw and pitch.
     */
    public static void sendEntityRotation(Player viewer, int entityId, float yaw, float pitch) {
        if (!available || viewer == null || moveEntityRotPacketClass == null) return;
        try {
            byte yRot = (byte) ((int) (yaw * 256.0F / 360.0F));
            byte xRot = (byte) ((int) (pitch * 256.0F / 360.0F));
            Object packet = null;

            if (moveEntityRotConstructor != null) {
                packet = moveEntityRotConstructor.newInstance(entityId, yRot, xRot, false);
            }

            if (packet != null) {
                sendPacket(viewer, packet);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Constructs and sends ClientboundRotateHeadPacket / PacketPlayOutEntityHeadRotation to update
     * the entity's head yaw.
     */
    public static void sendHeadRotation(Player viewer, int entityId, float yaw) {
        if (!available || viewer == null || rotateHeadPacketClass == null) return;
        try {
            byte yHeadRot = (byte) ((int) (yaw * 256.0F / 360.0F));

            // 1. Direct Parameter Constructor (Legacy versions)
            if (rotateHeadPacketConstructor != null) {
                Object packet = rotateHeadPacketConstructor.newInstance(entityId, yHeadRot);
                sendPacket(viewer, packet);
                return;
            }

            // 2. Modern Buffer Constructor (1.20.2 through 26.x+)
            if (rotateHeadBufConstructor != null) {
                Object buf = createBuffer();
                if (buf != null) {
                    writeVarInt(buf, entityId);
                    writeByte(buf, yHeadRot);
                    Object packet = rotateHeadBufConstructor.newInstance(buf);
                    sendPacket(viewer, packet);
                    return;
                }
            }

            // 3. Fallback: reflection instantiation
            try {
                Constructor<?> defCtor = rotateHeadPacketClass.getDeclaredConstructor();
                defCtor.setAccessible(true);
                Object packet = defCtor.newInstance();
                if (rotateHeadEntityIdField != null) {
                    setField(packet, rotateHeadEntityIdField, entityId);
                }
                if (rotateHeadYawField != null) {
                    setField(packet, rotateHeadYawField, yHeadRot);
                }
                sendPacket(viewer, packet);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /**
     * Synchronizes both entity yaw (body) and head yaw in one unified call.
     */
    public static void sendEntityLook(Player viewer, int entityId, float yaw, float pitch) {
        sendEntityRotation(viewer, entityId, yaw, pitch);
        sendHeadRotation(viewer, entityId, yaw);
    }

    /**
     * Constructs and sends ClientboundUpdateAttributesPacket to synchronize entity scale (Attributes.SCALE).
     */
    public static void sendEntityScale(Player viewer, int entityId, double scale) {
        if (!available || viewer == null || !viewer.isOnline() || updateAttributesPacketClass == null || scaleHolder == null || snapshotConstructor == null) return;
        try {
            Object snapshot = snapshotConstructor.newInstance(scaleHolder, scale, Collections.emptyList());
            List<Object> list = Collections.singletonList(snapshot);
            if (updateAttributesConstructor != null) {
                Object packet = updateAttributesConstructor.newInstance(entityId, list);
                if (packet != null) {
                    sendPacket(viewer, packet);
                }
            }
        } catch (Throwable ignored) {}
    }
}
