package org.bg52.curiospaper.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Universal text formatting utility supporting Adventure MiniMessage and legacy color codes.
 */
public final class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    private TextUtil() {}

    /**
     * Parses a string into an Adventure Component.
     * Automatically supports MiniMessage tags (&lt;red&gt;, &lt;gradient:#ff0000:#0000ff&gt;) and legacy color codes (&amp;c, &amp;l).
     */
    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        // Convert legacy & codes to MiniMessage friendly or parse directly
        if (input.contains("&") || input.contains("§")) {
            input = ChatColor.translateAlternateColorCodes('&', input);
            return SECTION_SERIALIZER.deserialize(input);
        }

        return MINI_MESSAGE.deserialize(input);
    }

    /**
     * Parses a list of strings into a list of Adventure Components.
     */
    public static List<Component> parseList(List<String> list) {
        if (list == null) return new ArrayList<>();
        List<Component> result = new ArrayList<>(list.size());
        for (String s : list) {
            result.add(parse(s));
        }
        return result;
    }

    /**
     * Converts a Component to a legacy formatted string.
     */
    public static String toLegacy(Component component) {
        if (component == null) return "";
        return SECTION_SERIALIZER.serialize(component);
    }

    /**
     * Strips both MiniMessage XML-like tags (&lt;gold&gt;, &lt;/yellow&gt;, etc.) and legacy color codes (§c, &amp;c).
     */
    public static String stripAll(String input) {
        if (input == null || input.isEmpty()) return "";
        String s = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input));
        return s.replaceAll("<[^>]*>", "").trim();
    }

    /**
     * Resolves the human-readable display name of an ItemStack.
     * Prioritizes custom display name or item name before falling back to formatted Material title.
     */
    public static String getItemDisplayName(ItemStack item) {
        if (item == null || item.getType().isAir()) return "";
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName()) {
                    Component c = meta.displayName();
                    if (c != null) {
                        return MINI_MESSAGE.serialize(c);
                    }
                    String legacy = meta.getDisplayName();
                    if (legacy != null && !legacy.isEmpty()) {
                        return legacy;
                    }
                }
                if (meta.hasItemName()) {
                    Component c = meta.itemName();
                    if (c != null) {
                        return MINI_MESSAGE.serialize(c);
                    }
                    String in = meta.getItemName();
                    if (in != null && !in.isEmpty()) {
                        return in;
                    }
                }
            }
        }

        // Check custom item engines (Synthetix, Nexo, etc.)
        try {
            org.bg52.curiospaper.CuriosPaper curPlugin = org.bg52.curiospaper.CuriosPaper.getInstance();
            if (curPlugin != null && curPlugin.getItemEngineManager() != null) {
                String id = curPlugin.getItemEngineManager().getItemId(item);
                if (id != null) {
                    if (Bukkit.getPluginManager().isPluginEnabled("Synthetix") && org.bg52.synthetix.api.SynthetixAPI.getOptional().isPresent()) {
                        org.bg52.synthetix.item.ItemDefinition def = org.bg52.synthetix.api.SynthetixAPI.get().getItemRegistry().get(id);
                        if (def != null && def.getDisplayName() != null && !def.getDisplayName().isEmpty()) {
                            return def.getDisplayName();
                        }
                    }
                    return prettify(id);
                }
            }
        } catch (Throwable ignored) {}

        String name = item.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static String prettify(String id) {
        if (id == null || id.isEmpty()) return "";
        String clean = id.replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
        String[] parts = clean.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
