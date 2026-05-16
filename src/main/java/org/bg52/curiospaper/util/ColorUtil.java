package org.bg52.curiospaper.util;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive color utility supporting ALL Minecraft color formats:
 * 
 * 1. Legacy §-codes: §a, §l, etc.
 * 2. Ampersand codes: &a, &l, etc.
 * 3. Bungee HEX: §x§R§R§G§G§B§B (e.g. §x§9§7§0§0§C§1)
 * 4. Ampersand HEX: &#RRGGBB (e.g. &#9700C1)
 * 5. Bracket HEX: {#RRGGBB} or &{#RRGGBB}
 * 6. MiniMessage gradients: <gradient:#RRGGBB:#RRGGBB>text</gradient>
 * 7. MiniMessage colors: <color:#RRGGBB>text</color> or <#RRGGBB>text
 * 8. Named MiniMessage colors: <red>, <gold>, etc.
 */
public class ColorUtil {

  // &#RRGGBB pattern (6 hex digits after &#)
  private static final Pattern AMP_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

  // {#RRGGBB} pattern
  private static final Pattern BRACKET_HEX_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})}");

  // <#RRGGBB> pattern (simple hex tag)
  private static final Pattern MINI_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

  // <color:#RRGGBB> pattern
  private static final Pattern MINI_COLOR_PATTERN = Pattern.compile("<color:#([A-Fa-f0-9]{6})>");

  // <gradient:#RRGGBB:#RRGGBB[:...]>text</gradient>
  private static final Pattern GRADIENT_PATTERN = Pattern.compile(
      "<gradient((?::#[A-Fa-f0-9]{6})+)>(.*?)</gradient>",
      Pattern.DOTALL);

  // Extract individual hex colors from gradient spec
  private static final Pattern GRADIENT_COLOR_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

  // Named MiniMessage color tags
  private static final Pattern NAMED_COLOR_PATTERN = Pattern.compile(
      "<(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>");

  // <bold>, <italic>, etc. formatting tags
  private static final Pattern FORMAT_PATTERN = Pattern.compile(
      "<(bold|italic|underlined|strikethrough|obfuscated|reset)>");

  // Closing tags </bold>, </color>, </gradient>, etc.
  private static final Pattern CLOSE_TAG_PATTERN = Pattern.compile("</[a-z_]+>");

  /**
   * Translates ALL supported color formats into Minecraft-compatible color codes.
   * This is the main entry point - call this on any message string.
   *
   * @param text the raw text with color codes
   * @return the translated text with §-based color codes
   */
  public static String translate(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }

    // 1. Process MiniMessage gradients first (most complex)
    text = processGradients(text);

    // 2. Process MiniMessage <color:#RRGGBB> tags
    text = processMiniColorTags(text);

    // 3. Process <#RRGGBB> simple hex tags
    text = processMiniHexTags(text);

    // 4. Process named MiniMessage color tags
    text = processNamedColors(text);

    // 5. Process formatting tags
    text = processFormatTags(text);

    // 6. Strip remaining closing tags
    text = CLOSE_TAG_PATTERN.matcher(text).replaceAll("");

    // 7. Process {#RRGGBB} bracket hex
    text = processBracketHex(text);

    // 8. Process &#RRGGBB ampersand hex
    text = processAmpHex(text);

    // 9. Process legacy & color codes (last, so we don't interfere with &#)
    text = translateLegacy(text);

    return text;
  }

  /**
   * Processes <gradient:#RRGGBB:#RRGGBB[:...]>text</gradient> patterns.
   * Applies a smooth color gradient across the text characters.
   */
  private static String processGradients(String text) {
    Matcher matcher = GRADIENT_PATTERN.matcher(text);
    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String colorSpec = matcher.group(1); // e.g. :#9700C1:#FF00EE:#9700C1
      String content = matcher.group(2);

      // Extract colors
      Matcher colorMatcher = GRADIENT_COLOR_PATTERN.matcher(colorSpec);
      java.util.List<Color> colors = new java.util.ArrayList<>();
      while (colorMatcher.find()) {
        colors.add(Color.decode("#" + colorMatcher.group(1)));
      }

      if (colors.size() < 2) {
        continue; // Need at least 2 colors for a gradient
      }

      // Strip any existing color codes from content for length calculation
      String stripped = org.bukkit.ChatColor.stripColor(content);
      if (stripped.isEmpty()) {
        // If content is just spaces, use the space count
        stripped = content;
      }

      StringBuilder gradientResult = new StringBuilder();
      int len = stripped.length();

      for (int i = 0; i < len; i++) {
        float ratio = (len > 1) ? (float) i / (len - 1) : 0;
        Color c = interpolateMultiGradient(colors, ratio);
        gradientResult.append(toBungeeHex(c)).append(stripped.charAt(i));
      }

      matcher.appendReplacement(sb, Matcher.quoteReplacement(gradientResult.toString()));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Interpolates between multiple gradient stops.
   */
  private static Color interpolateMultiGradient(java.util.List<Color> colors, float ratio) {
    if (ratio <= 0)
      return colors.get(0);
    if (ratio >= 1)
      return colors.get(colors.size() - 1);

    int segments = colors.size() - 1;
    float segmentRatio = ratio * segments;
    int segmentIndex = (int) segmentRatio;
    if (segmentIndex >= segments)
      segmentIndex = segments - 1;

    float localRatio = segmentRatio - segmentIndex;
    Color c1 = colors.get(segmentIndex);
    Color c2 = colors.get(segmentIndex + 1);

    int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * localRatio);
    int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * localRatio);
    int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * localRatio);

    return new Color(
        Math.max(0, Math.min(255, r)),
        Math.max(0, Math.min(255, g)),
        Math.max(0, Math.min(255, b)));
  }

  /**
   * Converts a Color to Bungee §x§R§R§G§G§B§B format.
   */
  private static String toBungeeHex(Color color) {
    String hex = String.format("%06X", color.getRGB() & 0xFFFFFF);
    StringBuilder result = new StringBuilder("§x");
    for (char c : hex.toCharArray()) {
      result.append("§").append(c);
    }
    return result.toString();
  }

  /**
   * Converts a 6-digit hex string to Bungee §x§R§R§G§G§B§B format.
   */
  private static String hexToBungee(String hex) {
    StringBuilder result = new StringBuilder("§x");
    for (char c : hex.toCharArray()) {
      result.append("§").append(c);
    }
    return result.toString();
  }

  /**
   * Processes <color:#RRGGBB> tags.
   */
  private static String processMiniColorTags(String text) {
    Matcher matcher = MINI_COLOR_PATTERN.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String hex = matcher.group(1);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(hexToBungee(hex)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Processes <#RRGGBB> simple hex tags.
   */
  private static String processMiniHexTags(String text) {
    Matcher matcher = MINI_HEX_PATTERN.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String hex = matcher.group(1);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(hexToBungee(hex)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Processes named MiniMessage color tags like <red>, <gold>, etc.
   */
  private static String processNamedColors(String text) {
    Matcher matcher = NAMED_COLOR_PATTERN.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String name = matcher.group(1);
      String code = namedColorToCode(name);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(code));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Processes formatting tags like <bold>, <italic>, etc.
   */
  private static String processFormatTags(String text) {
    Matcher matcher = FORMAT_PATTERN.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String name = matcher.group(1);
      String code = formatToCode(name);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(code));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Processes {#RRGGBB} bracket hex patterns.
   */
  private static String processBracketHex(String text) {
    Matcher matcher = BRACKET_HEX_PATTERN.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String hex = matcher.group(1);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(hexToBungee(hex)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Processes &#RRGGBB ampersand hex patterns.
   */
  private static String processAmpHex(String text) {
    Matcher matcher = AMP_HEX_PATTERN.matcher(text);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String hex = matcher.group(1);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(hexToBungee(hex)));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  /**
   * Translates legacy & color codes to § codes.
   * Does NOT touch &# (hex) since those are handled separately.
   */
  private static String translateLegacy(String text) {
    char[] chars = text.toCharArray();
    StringBuilder result = new StringBuilder(chars.length);

    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == '&' && i + 1 < chars.length) {
        char next = chars[i + 1];
        // Don't translate &# (handled by hex processor) or &{ (bracket hex)
        if (next == '#' || next == '{') {
          result.append(chars[i]);
          continue;
        }
        // Check if next char is a valid color/format code
        if ("0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(next) >= 0) {
          result.append('§').append(next);
          i++; // Skip next char
          continue;
        }
      }
      result.append(chars[i]);
    }

    return result.toString();
  }

  /**
   * Maps named MiniMessage colors to legacy § codes.
   */
  private static String namedColorToCode(String name) {
    switch (name.toLowerCase()) {
      case "black":
        return "§0";
      case "dark_blue":
        return "§1";
      case "dark_green":
        return "§2";
      case "dark_aqua":
        return "§3";
      case "dark_red":
        return "§4";
      case "dark_purple":
        return "§5";
      case "gold":
        return "§6";
      case "gray":
        return "§7";
      case "dark_gray":
        return "§8";
      case "blue":
        return "§9";
      case "green":
        return "§a";
      case "aqua":
        return "§b";
      case "red":
        return "§c";
      case "light_purple":
        return "§d";
      case "yellow":
        return "§e";
      case "white":
        return "§f";
      default:
        return "";
    }
  }

  /**
   * Maps formatting tag names to § codes.
   */
  private static String formatToCode(String name) {
    switch (name.toLowerCase()) {
      case "bold":
        return "§l";
      case "italic":
        return "§o";
      case "underlined":
        return "§n";
      case "strikethrough":
        return "§m";
      case "obfuscated":
        return "§k";
      case "reset":
        return "§r";
      default:
        return "";
    }
  }

  /**
   * Strips ALL color codes from text (legacy, hex, MiniMessage).
   */
  public static String stripAll(String text) {
    if (text == null)
      return null;
    // First translate everything, then strip using Bukkit
    String translated = translate(text);
    return org.bukkit.ChatColor.stripColor(translated);
  }
}
