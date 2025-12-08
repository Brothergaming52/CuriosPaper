package org.bg52.curiospaper.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.loot.LootTables;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * LootTableFetcher tuned to return ONLY chest-type loot tables.
 * Keeps the same robust multi-strategy fetching, but filters results
 * down to chest loot tables (vanilla + datapack + addon).
 */
public final class LootTableFetcher {

    private LootTableFetcher() {}

    @SuppressWarnings("unchecked")
    public static List<NamespacedKey> fetchAllLootTableKeys(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        try {
            // Collected set (unique)
            Set<NamespacedKey> discovered = new HashSet<>();

            // 1) Official API - Bukkit.getLootTableRegistry() (if present)
            try {
                Method m = Bukkit.class.getMethod("getLootTableRegistry");
                Object registry = m.invoke(null);
                if (registry instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) registry;
                    for (Object rawKey : map.keySet()) {
                        NamespacedKey nk = toNamespacedKey(rawKey == null ? null : rawKey.toString());
                        if (nk != null && isChestLoot(nk)) discovered.add(nk);
                    }
                    plugin.getLogger().info("[LootTableFetcher] Found " + discovered.size()
                            + " chest keys via Bukkit.getLootTableRegistry()");
                    if (!discovered.isEmpty()) {
                        return sortedList(discovered);
                    }
                }
            } catch (NoSuchMethodException ignored) {
                // not available on this API; continue to next strategy
            } catch (Throwable t) {
                plugin.getLogger().warning("[LootTableFetcher] Error calling Bukkit.getLootTableRegistry(): " + t.getMessage());
            }

            // 2) Baseline: LootTables enum (vanilla). Filter to chest-like paths.
            List<NamespacedKey> baseline = Arrays.stream(LootTables.values())
                    .map(LootTables::getKey)
                    .filter(Objects::nonNull)
                    .map(k -> new NamespacedKey(k.getNamespace(), k.getKey()))
                    .filter(LootTableFetcher::isChestLoot)
                    .collect(Collectors.toList());
            discovered.addAll(baseline);

            // 3) Try reflective NMS extraction (heuristic) and add chest keys only
            Object craftServer = Bukkit.getServer();
            Object nmsServer = null;
            try {
                Method getServerMethod = craftServer.getClass().getMethod("getServer");
                nmsServer = getServerMethod.invoke(craftServer);
            } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException ex) {
                plugin.getLogger().fine("[LootTableFetcher] Could not call getServer() on CraftServer: " + ex.getClass().getSimpleName());
            } catch (Throwable t) {
                plugin.getLogger().fine("[LootTableFetcher] Unexpected in getServer(): " + t.getMessage());
            }

            if (nmsServer != null) {
                Class<?> nmsServerClass = nmsServer.getClass();

                String[] likelyFieldNames = new String[]{"lootData", "lootDataManager", "lootTableRegistry", "LootDataManager", "f"};

                // 3a) Known field names
                for (String fieldName : likelyFieldNames) {
                    try {
                        Field f = findFieldIgnoreCase(nmsServerClass, fieldName);
                        if (f == null) continue;
                        f.setAccessible(true);
                        Object lootDataObj = f.get(nmsServer);
                        if (lootDataObj == null) continue;

                        // methods on lootDataObj returning Map
                        List<Method> zeroMapMethods = Arrays.stream(lootDataObj.getClass().getMethods())
                                .filter(mth -> mth.getParameterCount() == 0 && Map.class.isAssignableFrom(mth.getReturnType()))
                                .collect(Collectors.toList());

                        for (Method candidate : zeroMapMethods) {
                            try {
                                Object ret = candidate.invoke(lootDataObj);
                                if (ret instanceof Map) {
                                    Map<?, ?> map = (Map<?, ?>) ret;
                                    int added = addChestKeysFromMap(plugin, map, discovered);
                                    if (added > 0) {
                                        plugin.getLogger().info("[LootTableFetcher] Added " + added + " chest keys via field '"
                                                + f.getName() + "' method '" + candidate.getName() + "'");
                                    }
                                }
                            } catch (Throwable tt) {
                                // ignore individual method failures
                            }
                        }
                    } catch (Throwable ex) {
                        // ignore per-field
                    }
                }

                // 3b) Heuristic scan: inspect every field on nmsServer and probe methods that return Map
                try {
                    Field[] serverFields = nmsServerClass.getDeclaredFields();
                    for (Field f : serverFields) {
                        try {
                            f.setAccessible(true);
                            Object val = f.get(nmsServer);
                            if (val == null) continue;
                            for (Method mth : val.getClass().getMethods()) {
                                if (mth.getParameterCount() != 0) continue;
                                if (!Map.class.isAssignableFrom(mth.getReturnType())) continue;
                                try {
                                    Object ret = mth.invoke(val);
                                    if (!(ret instanceof Map)) continue;
                                    Map<?, ?> map = (Map<?, ?>) ret;
                                    if (map.isEmpty()) continue;
                                    int added = addChestKeysFromMap(plugin, map, discovered);
                                    if (added > 0) {
                                        plugin.getLogger().info("[LootTableFetcher] Heuristic: added " + added + " chest keys from field '"
                                                + f.getName() + "' method '" + mth.getName() + "'");
                                    }
                                } catch (Throwable tt) {
                                    // ignore invocation errors
                                }
                            }
                        } catch (Throwable ignore) {
                            // ignore per-field failures
                        }
                    }
                } catch (Throwable ex) {
                    plugin.getLogger().fine("[LootTableFetcher] Heuristic scan failed: " + ex.getMessage());
                }
            } // end if nmsServer != null

            // final result: if nothing found except maybe baseline, return what we have.
            List<NamespacedKey> finalList = sortedList(discovered);
            plugin.getLogger().info("[LootTableFetcher] Total chest loot table keys discovered: " + finalList.size());
            return finalList;
        } catch (Throwable t) {
            plugin.getLogger().warning("[LootTableFetcher] Unexpected error while fetching loot tables: " + t.getMessage());
            return Collections.emptyList();
        }
    }

    // Add chest-like keys from map into discovered set. Returns number added.
    private static int addChestKeysFromMap(JavaPlugin plugin, Map<?, ?> map, Set<NamespacedKey> discovered) {
        int added = 0;
        for (Object key : map.keySet()) {
            try {
                if (key == null) continue;
                String raw = key.toString();
                NamespacedKey nk = toNamespacedKey(raw);
                if (nk == null) continue;
                if (isChestLoot(nk)) {
                    if (discovered.add(nk)) added++;
                }
            } catch (Throwable ignored) {
            }
        }
        return added;
    }

    // Helper: returns true if this NamespacedKey appears to represent a chest loot table.
    private static boolean isChestLoot(NamespacedKey nk) {
        if (nk == null) return false;
        String path = nk.getKey(); // may contain slashes like "chests/simple_dungeon"
        if (path == null || path.isEmpty()) return false;
        path = path.toLowerCase(Locale.ROOT);

        // Strict check: path starts with "chests" (covers "chests/..." and "chests")
        if (path.startsWith("chests")) return true;

        // Common alternative: contains "/chests/" somewhere (rare)
        if (path.contains("/chests/")) return true;

        // Fallback: contains "chest" substring (covers odd nonstandard names). Use only as fallback.
        if (path.contains("chest")) return true;

        return false;
    }

    // Convert objects/string into NamespacedKey. Accepts "namespace:path..." strings.
    private static NamespacedKey toNamespacedKey(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        int colon = raw.indexOf(':');
        if (colon <= 0 || colon == raw.length() - 1) return null;
        String ns = raw.substring(0, colon);
        String path = raw.substring(colon + 1);
        if (ns.isEmpty() || path.isEmpty()) return null;
        try {
            return new NamespacedKey(ns, path);
        } catch (Exception ex) {
            return null;
        }
    }

    // Find field ignoring case
    private static Field findFieldIgnoreCase(Class<?> clazz, String name) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.getName().equalsIgnoreCase(name)) return f;
        }
        return null;
    }

    private static List<NamespacedKey> sortedList(Collection<NamespacedKey> set) {
        return set.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(NamespacedKey::toString))
                .collect(Collectors.toList());
    }
}
