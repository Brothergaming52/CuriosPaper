package org.bg52.curiospaper.listener;

import org.bg52.curiospaper.CuriosPaper;
import org.bg52.curiospaper.data.ItemData;
import org.bg52.curiospaper.data.RecipeData;
import org.bg52.curiospaper.event.CuriosRecipeTransferEvent;
import org.bg52.curiospaper.manager.ItemDataManager;
import org.bg52.curiospaper.util.VersionUtil;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * Handles registration of custom recipes and ensures custom ingredients are
 * respected.
 */
public class RecipeListener implements Listener {
  private final CuriosPaper plugin;
  private final ItemDataManager itemDataManager;
  private static final Pattern VALID_KEY_PATTERN = Pattern.compile("[a-z0-9/._-]+");

  public RecipeListener(CuriosPaper plugin, ItemDataManager itemDataManager) {
    this.plugin = plugin;
    this.itemDataManager = itemDataManager;
    registerPrepareSmithingHandler();
  }

  /**
   * Registers all recipes from loaded items
   */
  public void registerAllRecipes() {
    int registered = 0;
    int failed = 0;

    for (ItemData itemData : itemDataManager.getAllItems().values()) {
      List<RecipeData> recipes = itemData.getRecipes();
      for (int i = 0; i < recipes.size(); i++) {
        if (registerRecipe(itemData, recipes.get(i), i, recipes.size() > 1)) {
          registered++;
        } else {
          failed++;
        }
      }
    }

    plugin.getLogger().info("Recipe registration complete.");
    plugin.getLogger().info(" Successfully registered: " + registered);
  }

  /**
   * Reloads recipes for a specific item (Removes old, registers new)
   * This is much more efficient than registerAllRecipes() for editors.
   */
  public void reloadItemRecipes(String itemId) {
    ItemData itemData = itemDataManager.getItemData(itemId);
    if (itemData == null)
      return;

    // 1. Calculate all potential keys for this item and remove them
    String owningPlugin = itemData.getOwningPlugin();
    if (owningPlugin == null || owningPlugin.isEmpty()) {
      owningPlugin = "curiospaper";
    }
    owningPlugin = sanitizeKey(owningPlugin);
    String safeItemId = sanitizeKey(itemId);

    // We clean up both "single" style key and "variant" style keys
    // Logic: just iterate server recipes once and remove any that match our prefix
    // pattern for this item
    String baseKey = "custom_" + owningPlugin + "_" + safeItemId;

    Iterator<Recipe> it = plugin.getServer().recipeIterator();
    while (it.hasNext()) {
      Recipe r = it.next();
      if (r instanceof Keyed) {
        String key = ((Keyed) r).getKey().getKey();
        if (key.equals(baseKey) || key.startsWith(baseKey + "_variant")) {
          it.remove();
        }
      }
    }

    // 2. Register new recipes without trying to remove (since we just did)
    List<RecipeData> recipes = itemData.getRecipes();
    for (int i = 0; i < recipes.size(); i++) {
      // pass 'true' for skipRemoval
      registerRecipe(itemData, recipes.get(i), i, recipes.size() > 1, true);
    }

    plugin.getLogger().info("Reloaded recipes for " + itemId);

  }

  /**
   * Registers a recipe for a specific item (convenience method)
   */
  public boolean registerRecipe(ItemData itemData, RecipeData recipeData) {
    return registerRecipe(itemData, recipeData, 0, false, false);
  }

  public boolean registerRecipe(ItemData itemData, RecipeData recipeData, int variantIndex, boolean hasMultiple) {
    return registerRecipe(itemData, recipeData, variantIndex, hasMultiple, false);
  }

  public boolean registerRecipe(ItemData itemData, RecipeData recipeData, int variantIndex, boolean hasMultiple,
      boolean skipRemoval) {
    if (recipeData == null || !recipeData.isValid()) {
      return false;
    }

    try {
      ItemStack result = createResultItem(itemData);
      if (result == null)
        return false;

      // Generate a stable key: custom_<plugin>_<itemid>[_variant<N>]
      String owningPlugin = itemData.getOwningPlugin();
      if (owningPlugin == null || owningPlugin.isEmpty()) {
        owningPlugin = "curiospaper";
      }
      owningPlugin = sanitizeKey(owningPlugin.toLowerCase());
      String safeItemId = sanitizeKey(itemData.getItemId().toLowerCase());

      String keyString = "custom_" + owningPlugin + "_" + safeItemId;
      if (hasMultiple) {
        keyString += "_variant" + variantIndex;
      }

      NamespacedKey key = new NamespacedKey(plugin, keyString);

      if (!skipRemoval) {
        // Remove existing if any
        // plugin.getServer().removeRecipe(key); // Missing in 1.14
        Iterator<Recipe> it = plugin.getServer().recipeIterator();
        while (it.hasNext()) {
          Recipe r = it.next();
          if (r instanceof Keyed) {
            if (((Keyed) r).getKey().equals(key)) {
              it.remove();
              break;
            }
          }
        }
      }

      switch (recipeData.getType()) {
        case SHAPED:
          return registerShapedRecipe(key, result, recipeData);
        case SHAPELESS:
          return registerShapelessRecipe(key, result, recipeData);
        case FURNACE:
          return registerFurnaceRecipe(key, result, recipeData, false, false);
        case BLAST_FURNACE:
          return registerFurnaceRecipe(key, result, recipeData, true, false);
        case SMOKER:
          return registerFurnaceRecipe(key, result, recipeData, false, true);
        case CAMPFIRE:
          return registerCampfireRecipe(key, result, recipeData);
        case SMITHING:
          return registerSmithingRecipe(key, result, recipeData);
        case ANVIL:
          return true;
        default:
          return false;
      }

    } catch (Exception e) {
      plugin.getLogger().severe("Error registering recipe for " + itemData.getItemId() + ": " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private String sanitizeKey(String key) {
    String sanitized = key.toLowerCase().replace(' ', '_');
    if (!VALID_KEY_PATTERN.matcher(sanitized).matches()) {
      // Fallback cleanup if it contains other invalid chars
      sanitized = sanitized.replaceAll("[^a-z0-9/._-]", "");
    }
    return sanitized;
  }

  private boolean registerShapedRecipe(NamespacedKey key, ItemStack result, RecipeData recipeData) {
    ShapedRecipe recipe = new ShapedRecipe(key, result);

    String[] shape = recipeData.getShape();
    if (shape == null || shape.length != 3) {
      return false;
    }

    recipe.shape(shape[0], shape[1], shape[2]);

    for (Map.Entry<Character, String> entry : recipeData.getIngredients().entrySet()) {
      RecipeChoice choice = resolveIngredient(entry.getValue());
      if (choice == null)
        return false;
      recipe.setIngredient(entry.getKey(), choice);
    }

    plugin.getServer().addRecipe(recipe);
    return true;
  }

  private boolean registerShapelessRecipe(NamespacedKey key, ItemStack result, RecipeData recipeData) {
    ShapelessRecipe recipe = new ShapelessRecipe(key, result);

    for (String ingredient : recipeData.getIngredients().values()) {
      RecipeChoice choice = resolveIngredient(ingredient);
      if (choice == null)
        return false;
      recipe.addIngredient(choice);
    }

    plugin.getServer().addRecipe(recipe);
    return true;
  }

  private boolean registerFurnaceRecipe(NamespacedKey key, ItemStack result, RecipeData recipe, boolean blast,
      boolean smoker) {
    RecipeChoice input = resolveIngredient(recipe.getInputItem());
    if (input == null)
      return false;

    org.bukkit.inventory.Recipe bukkitRecipe;
    if (blast) {
      bukkitRecipe = new org.bukkit.inventory.BlastingRecipe(key, result, input, recipe.getExperience(),
          recipe.getCookingTime());
    } else if (smoker) {
      bukkitRecipe = new org.bukkit.inventory.SmokingRecipe(key, result, input, recipe.getExperience(),
          recipe.getCookingTime());
    } else {
      bukkitRecipe = new org.bukkit.inventory.FurnaceRecipe(key, result, input, recipe.getExperience(),
          recipe.getCookingTime());
    }

    plugin.getServer().addRecipe(bukkitRecipe);
    return true;
  }

  private boolean registerCampfireRecipe(NamespacedKey key, ItemStack result, RecipeData recipe) {
    RecipeChoice input = resolveIngredient(recipe.getInputItem());
    if (input == null)
      return false;

    try {
      org.bukkit.inventory.CampfireRecipe campfireRecipe = new org.bukkit.inventory.CampfireRecipe(
          key, result, input, recipe.getExperience(), recipe.getCookingTime());
      plugin.getServer().addRecipe(campfireRecipe);
      return true;
    } catch (NoClassDefFoundError e) {
      plugin.getLogger().warning("CampfireRecipe not available on this server version");
      return false;
    }
  }

  private boolean registerSmithingRecipe(NamespacedKey key, ItemStack result, RecipeData recipe) {
    RecipeChoice base = resolveIngredient(recipe.getBaseItem());
    RecipeChoice addition = resolveIngredient(recipe.getAdditionItem());

    if (base == null || addition == null)
      return false;

    if (VersionUtil.supportsSmithingTemplate()) {
      // 1.20+: Use SmithingTransformRecipe with template slot via reflection
      RecipeChoice template = resolveIngredient(recipe.getTemplateItem());
      if (template == null) {
        template = new RecipeChoice.MaterialChoice(Material.AIR);
      }
      try {
        Class<?> strClass = Class.forName("org.bukkit.inventory.SmithingTransformRecipe");
        java.lang.reflect.Constructor<?> ctor = strClass.getConstructor(
            NamespacedKey.class, ItemStack.class, RecipeChoice.class,
            RecipeChoice.class, RecipeChoice.class);
        Recipe smithingRecipe = (Recipe) ctor.newInstance(key, result, template, base, addition);
        plugin.getServer().addRecipe(smithingRecipe);
        return true;
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to register SmithingTransformRecipe: " + e.getMessage());
        return false;
      }
    } else {
      // Pre-1.20: Use legacy SmithingRecipe via reflection (since it's not in 1.14
      // API)
      try {
        Class<?> srClass = Class.forName("org.bukkit.inventory.SmithingRecipe");
        // Constructor: SmithingRecipe(NamespacedKey key, ItemStack result, RecipeChoice
        // base, RecipeChoice addition)
        java.lang.reflect.Constructor<?> ctor = srClass.getConstructor(
            NamespacedKey.class, ItemStack.class, RecipeChoice.class, RecipeChoice.class);
        Recipe smithingRecipe = (Recipe) ctor.newInstance(key, result, base, addition);
        plugin.getServer().addRecipe(smithingRecipe);
        return true;
      } catch (ClassNotFoundException e) {
        // SmithingRecipe not available (1.14/1.15)
        return false;
      } catch (Exception e) {
        plugin.getLogger().warning("Failed to register SmithingRecipe: " + e.getMessage());
        return false;
      }
    }
  }

  /**
   * Registers a PrepareSmithingEvent handler via reflection.
   * PrepareSmithingEvent doesn't exist on 1.14/1.15, so we use reflection
   * to avoid compile-time issues. This handler overrides the result item
   * in the smithing table preview with the correct custom item, because
   * the legacy SmithingRecipe copies NBT from the base item which
   * overwrites our custom item metadata.
   */
  @SuppressWarnings("unchecked")
  private void registerPrepareSmithingHandler() {
    try {
      Class<? extends org.bukkit.event.Event> prepareClass = (Class<? extends org.bukkit.event.Event>) Class
          .forName(
              "org.bukkit.event.inventory.PrepareSmithingEvent");

      org.bukkit.event.Listener dummyListener = new org.bukkit.event.Listener() {
      };
      RecipeListener self = this;

      plugin.getServer().getPluginManager().registerEvent(
          prepareClass,
          dummyListener,
          org.bukkit.event.EventPriority.HIGH,
          (listener, event) -> {
            if (!prepareClass.isInstance(event))
              return;
            try {
              self.handlePrepareSmithing(event);
            } catch (Exception ex) {
              plugin.getLogger().warning("Error in PrepareSmithingEvent handler: " + ex.getMessage());
            }
          },
          plugin);
    } catch (ClassNotFoundException e) {
      // PrepareSmithingEvent not available (1.14/1.15) — smithing doesn't exist
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to register PrepareSmithingEvent handler: " + e.getMessage());
    }
  }

  /**
   * Handles the PrepareSmithingEvent by matching the smithing table contents
   * against registered custom smithing recipes and setting the correct result.
   */
  private void handlePrepareSmithing(org.bukkit.event.Event event) throws Exception {
    // Get the inventory via reflection to avoid compile-time dependency
    java.lang.reflect.Method getInventory = event.getClass().getMethod("getInventory");
    org.bukkit.inventory.Inventory inv = (org.bukkit.inventory.Inventory) getInventory.invoke(event);

    // Determine slot layout based on version
    // 1.20+ (with template): slot 0=template, 1=base, 2=addition
    // Pre-1.20: slot 0=base, 1=addition
    boolean hasTemplate = VersionUtil.supportsSmithingTemplate();
    int baseSlot = hasTemplate ? 1 : 0;
    int additionSlot = hasTemplate ? 2 : 1;

    ItemStack base = inv.getItem(baseSlot);
    ItemStack addition = inv.getItem(additionSlot);
    ItemStack template = hasTemplate ? inv.getItem(0) : null;

    if (base == null || addition == null)
      return;

    // Search all registered custom items for a matching smithing recipe
    for (ItemData itemData : itemDataManager.getAllItems().values()) {
      for (RecipeData recipe : itemData.getRecipes()) {
        if (recipe.getType() != RecipeData.RecipeType.SMITHING)
          continue;

        if (matchesSmithing(recipe, template, base, addition)) {
          // Create the correct result item
          ItemStack result = createResultItem(itemData);
          if (result == null)
            continue;

          // Fire transfer event so plugins can copy data (e.g. backpack UUID)
          CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, base);
          plugin.getServer().getPluginManager().callEvent(transferEvent);

          if (!transferEvent.isCancelled()) {
            result = transferEvent.getResult();
            result = ensureItemTags(result);
          }

          // Set result via reflection
          java.lang.reflect.Method setResult = event.getClass().getMethod("setResult", ItemStack.class);
          setResult.invoke(event, result);
          return;
        }
      }
    }
  }

  private RecipeChoice resolveIngredient(String ingredient) {
    if (ingredient == null)
      return null;
    try {
      Material material = Material.valueOf(ingredient.toUpperCase());
      return new RecipeChoice.MaterialChoice(material);
    } catch (IllegalArgumentException e) {
      if (itemDataManager.hasItem(ingredient)) {
        ItemData data = itemDataManager.getItemData(ingredient);
        if (data != null) {
          try {
            Material mat = Material.valueOf(data.getMaterial().toUpperCase());
            return new RecipeChoice.MaterialChoice(mat);
          } catch (IllegalArgumentException ignored) {
          }
        }
      }
    }
    return null;
  }

  private ItemStack createResultItem(ItemData itemData) {
    return plugin.getCuriosPaperAPI().createItemStack(itemData.getItemId());
  }

  @EventHandler
  public void onPrepareCraft(PrepareItemCraftEvent event) {
    Recipe recipe = event.getRecipe();
    if (recipe == null || !(recipe instanceof Keyed))
      return;

    NamespacedKey key = ((Keyed) recipe).getKey();
    if (!key.getNamespace().equalsIgnoreCase(plugin.getName()) || !key.getKey().startsWith("custom_")) {
      // Not a CuriosPaper custom recipe
      return;
    }

    ItemStack[] matrix = event.getInventory().getMatrix();
    // Strict Validation Logic
    if (!validateCustomIngredients(key, matrix)) {
      event.getInventory().setResult(null); // Invalid craft
      return;
    }

    // Use the first valid custom item as the transfer source
    ItemStack sourceForTransfer = null;
    for (ItemStack item : matrix) {
      if (item != null && item.hasItemMeta()) {
        PersistentDataContainer itemPdc = item.getItemMeta().getPersistentDataContainer();
        if (itemPdc.has(plugin.getCuriosPaperAPI().getItemIdKey(), PersistentDataType.STRING)) {
          if (sourceForTransfer == null) {
            sourceForTransfer = item;
          }
        }
      }
    }

    ItemStack result = event.getInventory().getResult();
    if (sourceForTransfer != null && result != null) {
      CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(event.getInventory(), result,
          sourceForTransfer);
      plugin.getServer().getPluginManager().callEvent(transferEvent);

      if (!transferEvent.isCancelled()) {
        ItemStack resultItem = transferEvent.getResult();
        // Ensure tags are applied even if transfer overwrote them
        resultItem = ensureItemTags(resultItem);
        event.getInventory().setResult(resultItem);
      }
    }
  }

  /**
   * Strictly validates that the framing matrix matches the custom requirements
   */
  private boolean validateCustomIngredients(NamespacedKey recipeKey, ItemStack[] matrix) {
    // 1. Identify the recipe definition from the key
    RecipeDefinition definition = parseRecipeDefinition(recipeKey);
    if (definition == null || definition.recipeData == null) {
      // Should not happen if key is ours, unless data reload removed it
      return false;
    }

    RecipeData data = definition.recipeData;

    if (data.getType() == RecipeData.RecipeType.SHAPED) {
      return validateShaped(data, matrix);
    } else if (data.getType() == RecipeData.RecipeType.SHAPELESS) {
      return validateShapeless(data, matrix);
    }

    return true;
  }

  private boolean validateShapeless(RecipeData data, ItemStack[] matrix) {
    // Create a list of required ingredients
    List<String> requirements = new ArrayList<>(data.getIngredients().values());

    // Create a list of provided items
    List<ItemStack> provided = new ArrayList<>();
    for (ItemStack is : matrix) {
      if (is != null && is.getType() != Material.AIR) {
        provided.add(is);
      }
    }

    if (provided.size() != requirements.size()) {
      return false; // Mismatch in count
    }

    // Match provided items to requirements
    // We use a simple greedy match (since ingredients are interchangeable in
    // shapeless)
    // But we must respect custom IDs.

    // Create a temp copy of requirements to tick off
    List<String> remainingReqs = new ArrayList<>(requirements);

    for (ItemStack item : provided) {
      boolean matched = false;
      for (int i = 0; i < remainingReqs.size(); i++) {
        String req = remainingReqs.get(i);
        if (matchesRequirement(req, item)) {
          remainingReqs.remove(i);
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false; // Item in matrix that matches no requirement
      }
    }

    return remainingReqs.isEmpty();
  }

  private boolean validateShaped(RecipeData data, ItemStack[] matrix) {
    // Need to find the bounds of the items in the matrix to align with recipe shape
    int minRow = 2, maxRow = 0, minCol = 2, maxCol = 0;
    boolean empty = true;

    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        if (matrix[r * 3 + c] != null && matrix[r * 3 + c].getType() != Material.AIR) {
          empty = false;
          if (r < minRow)
            minRow = r;
          if (r > maxRow)
            maxRow = r;
          if (c < minCol)
            minCol = c;
          if (c > maxCol)
            maxCol = c;
        }
      }
    }

    if (empty)
      return false;

    return checkShapedMatch(data, matrix);
  }

  // Checks if the matrix matches the shape definition strictly
  private boolean checkShapedMatch(RecipeData data, ItemStack[] matrix) {
    String[] shape = data.getShape();
    Map<Character, String> ingredients = data.getIngredients();

    // Converting shape to a character grid for easier handling
    char[][] shapeGrid = new char[3][3];
    for (int r = 0; r < 3; r++) {
      String row = (shape[r] != null) ? shape[r] : "  ";
      // Pad to 3
      while (row.length() < 3)
        row += " ";
      shapeGrid[r] = row.toCharArray();
    }

    // Locate top-left non-air item in matrix
    // We iterate offsets to align the shape on top of the matrix items.

    for (int rowOffset = -2; rowOffset <= 2; rowOffset++) {
      for (int colOffset = -2; colOffset <= 2; colOffset++) {
        if (matchesAtOffset(matrix, shapeGrid, ingredients, rowOffset, colOffset)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean matchesAtOffset(ItemStack[] matrix, char[][] shapeGrid, Map<Character, String> ingredients,
      int rowOffset, int colOffset) {
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        int shapeR = r - rowOffset;
        int shapeC = c - colOffset;

        ItemStack item = matrix[r * 3 + c];
        boolean itemIsAir = (item == null || item.getType() == Material.AIR);

        char requiredChar = ' ';
        if (shapeR >= 0 && shapeR < 3 && shapeC >= 0 && shapeC < 3) {
          requiredChar = shapeGrid[shapeR][shapeC];
        }

        // If ingredient is defined
        if (ingredients.containsKey(requiredChar)) {
          String reqString = ingredients.get(requiredChar);
          if (!matchesRequirement(reqString, item)) {
            return false;
          }
        } else {
          // Expecting Air
          if (!itemIsAir) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean matchesRequirement(String reqString, ItemStack item) {
    if (item == null || item.getType() == Material.AIR)
      return false;

    // Check if requirement is a Custom Item ID
    if (itemDataManager.hasItem(reqString)) {
      return isCustomItem(item, reqString);
    } else {
      // Requirement is a vanilla material, verify item material matches
      try {
        return item.getType().name().equalsIgnoreCase(reqString);
      } catch (Exception e) {
        return false;
      }
    }
  }

  private boolean isCustomItem(ItemStack item, String targetId) {
    if (item == null || !item.hasItemMeta())
      return false;
    PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
    NamespacedKey idKey = plugin.getCuriosPaperAPI().getItemIdKey();
    if (!pdc.has(idKey, PersistentDataType.STRING))
      return false;
    String id = pdc.get(idKey, PersistentDataType.STRING);
    return id != null && id.equals(targetId);
  }

  private static class RecipeDefinition {
    RecipeData recipeData;
  }

  private RecipeDefinition parseRecipeDefinition(NamespacedKey key) {
    // Format: custom_<plugin>_<itemid>[_variantN]
    String keyStr = key.getKey();
    if (!keyStr.startsWith("custom_"))
      return null;

    String remaining = keyStr.substring(7); // <plugin>_<itemid>[_variantN]

    int firstUnder = remaining.indexOf('_');
    if (firstUnder == -1)
      return null;

    String rest = remaining.substring(firstUnder + 1); // <itemid> or <itemid>_variantN

    // Check for _variant suffix
    int variantIndex = 0;
    String itemId = rest;

    int variantMarker = rest.lastIndexOf("_variant");
    if (variantMarker != -1 && variantMarker + 8 < rest.length()) {
      try {
        String numStr = rest.substring(variantMarker + 8);
        variantIndex = Integer.parseInt(numStr);
        itemId = rest.substring(0, variantMarker);
      } catch (NumberFormatException ignored) {
      }
    }

    ItemData data = itemDataManager.getItemData(itemId);
    if (data == null) {
      // Fallback for sanitized ID matching if exact lookup fails
      for (ItemData idata : itemDataManager.getAllItems().values()) {
        if (sanitizeKey(idata.getItemId()).equals(itemId)) {
          data = idata;
          break;
        }
      }
    }

    if (data != null) {
      List<RecipeData> recipes = data.getRecipes();
      if (variantIndex >= 0 && variantIndex < recipes.size()) {
        RecipeDefinition def = new RecipeDefinition();
        def.recipeData = recipes.get(variantIndex);
        return def;
      }
    }

    return null;
  }

  @EventHandler
  public void onSmithingClick(org.bukkit.event.inventory.InventoryClickEvent event) {
    if (event.getInventory() == null)
      return;

    // Check for Smithing Table inventory type safely (since SMITHING enum missing
    // in 1.14)
    String typeName = event.getInventory().getType().name();
    if (!"SMITHING".equals(typeName)) {
      return;
    }

    // Smithing result slot is usually index 3 (0=template, 1=base, 2=addition) in
    // 1.20+
    // But in 1.16-1.19 (legacy smithing), result is slot 2 (0=base, 1=addition).
    // Safest check: verify slot type is RESULT
    if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.RESULT) {
      ItemStack result = event.getCurrentItem();
      if (result == null || !result.hasItemMeta())
        return;

      // This logic is simplified because we can't easily access the matrix without
      // reflection on the specific inventory view class which varies by version.
      // However, we can handle the TRANSFER event here if the result is a custom
      // item.

      NamespacedKey itemIdKey = plugin.getCuriosPaperAPI().getItemIdKey();
      PersistentDataContainer pdc = result.getItemMeta().getPersistentDataContainer();
      if (pdc.has(itemIdKey, PersistentDataType.STRING)) {
        // It's a custom item result.
        // We should try to transfer data from the base item.
        // Base item in 1.20+ is slot 1. In 1.16+ is slot 0.

        int baseSlot = VersionUtil.supportsSmithingTemplate() ? 1 : 0;
        ItemStack base = event.getInventory().getItem(baseSlot);

        if (base != null && base.hasItemMeta()) {
          CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(event.getInventory(),
              result, base);
          plugin.getServer().getPluginManager().callEvent(transferEvent);

          if (!transferEvent.isCancelled()) {
            // Update the item on cursor or in slot?
            // Click event: result is what they are clicking.
            // If we modify current item, it might work if cancellable?
            // Usually in click event result slot, we modify the result being picked up.
            // But event.setCurrentItem works on the slot.
            ItemStack resultItem = transferEvent.getResult();
            resultItem = ensureItemTags(resultItem);
            event.setCurrentItem(resultItem);
          }
        }
      }
    }
  }

  private boolean matchesSmithing(RecipeData recipe, ItemStack template, ItemStack base, ItemStack addition) {
    if (recipe.getTemplateItem() != null && !recipe.getTemplateItem().isEmpty()) {
      if (!matchesRequirement(recipe.getTemplateItem(), template))
        return false;
    }
    if (!matchesRequirement(recipe.getBaseItem(), base))
      return false;
    if (!matchesRequirement(recipe.getAdditionItem(), addition))
      return false;
    return true;
  }

  @EventHandler
  public void onFurnaceSmelt(org.bukkit.event.inventory.FurnaceSmeltEvent event) {
    ItemStack source = event.getSource();
    ItemStack result = event.getResult();

    if (result != null && result.hasItemMeta()) {
      NamespacedKey keyIdx = plugin.getCuriosPaperAPI().getItemIdKey();
      PersistentDataContainer resultPdc = result.getItemMeta().getPersistentDataContainer();
      if (resultPdc.has(keyIdx, PersistentDataType.STRING)) {
        String resultId = resultPdc.get(keyIdx, PersistentDataType.STRING);
        ItemData itemData = itemDataManager.getItemData(resultId);

        if (itemData != null) {
          boolean isValid = false;
          for (RecipeData recipe : itemData.getRecipes()) {
            if (isFurnaceType(recipe.getType())) {
              if (matchesRequirement(recipe.getInputItem(), source)) {
                isValid = true;
                break;
              }
            }
          }

          if (!isValid) {
            event.setCancelled(true);
            return;
          }
        }

        if (event.getBlock().getState() instanceof org.bukkit.inventory.InventoryHolder) {
          org.bukkit.inventory.Inventory inv = ((org.bukkit.inventory.InventoryHolder) event.getBlock()
              .getState()).getInventory();
          CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, source);
          plugin.getServer().getPluginManager().callEvent(transferEvent);

          if (!transferEvent.isCancelled()) {
            ItemStack resultItem = transferEvent.getResult();
            resultItem = ensureItemTags(resultItem);
            event.setResult(resultItem);
          }
        }
      }
    }
  }

  private boolean isFurnaceType(RecipeData.RecipeType type) {
    return type == RecipeData.RecipeType.FURNACE ||
        type == RecipeData.RecipeType.BLAST_FURNACE ||
        type == RecipeData.RecipeType.SMOKER ||
        type == RecipeData.RecipeType.CAMPFIRE;
  }

  @EventHandler
  public void onPrepareAnvil(org.bukkit.event.inventory.PrepareAnvilEvent event) {
    org.bukkit.inventory.AnvilInventory inv = event.getInventory();
    ItemStack left = inv.getItem(0);
    ItemStack right = inv.getItem(1);

    if (left == null || right == null)
      return;

    for (ItemData itemData : itemDataManager.getAllItems().values()) {
      for (RecipeData recipe : itemData.getRecipes()) {
        if (recipe.getType() == RecipeData.RecipeType.ANVIL) {
          if (matchesAnvil(recipe, left, right)) {
            ItemStack result = createResultItem(itemData);

            CuriosRecipeTransferEvent transferEvent = new CuriosRecipeTransferEvent(inv, result, left);
            plugin.getServer().getPluginManager().callEvent(transferEvent);

            if (!transferEvent.isCancelled()) {
              ItemStack resultItem = transferEvent.getResult();
              resultItem = ensureItemTags(resultItem);
              event.setResult(resultItem);
              plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (event.getView() != null) {
                  event.getInventory().setRepairCost((int) recipe.getExperience());
                }
              });
            }
            return;
          }
        }
      }
    }
  }

  private boolean matchesAnvil(RecipeData recipe, ItemStack left, ItemStack right) {
    return matchesRequirement(recipe.getLeftInput(), left) && matchesRequirement(recipe.getRightInput(), right);
  }

  public boolean unregisterRecipe(String itemId) {
    ItemData data = itemDataManager.getItemData(itemId);
    if (data != null) {
      String owningPlugin = data.getOwningPlugin();
      if (owningPlugin == null || owningPlugin.isEmpty()) {
        owningPlugin = "curiospaper";
      }
      owningPlugin = sanitizeKey(owningPlugin);
      String safeItemId = sanitizeKey(itemId);

      List<RecipeData> recipes = data.getRecipes();
      for (int i = 0; i < recipes.size(); i++) {
        String keyString = "custom_" + owningPlugin + "_" + safeItemId;
        if (recipes.size() > 1) {
          keyString += "_variant" + i;
        }
        NamespacedKey key = new NamespacedKey(plugin, keyString);
        // plugin.getServer().removeRecipe(key); // Missing in 1.14
        Iterator<Recipe> it = plugin.getServer().recipeIterator();
        while (it.hasNext()) {
          Recipe r = it.next();
          if (r instanceof Keyed && ((Keyed) r).getKey().equals(key)) {
            it.remove();
          }
        }
      }
      return true;
    }
    return false;
  }

  public void unregisterAllRecipes() {
    for (String itemId : itemDataManager.getAllItemIds()) {
      unregisterRecipe(itemId);
    }
    plugin.getLogger().info("Unregistered all custom recipes");
  }

  private ItemStack ensureItemTags(ItemStack item) {
    if (item == null || !item.hasItemMeta()) {
      return item;
    }

    NamespacedKey itemIdKey = plugin.getCuriosPaperAPI().getItemIdKey();
    PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

    if (pdc.has(itemIdKey, PersistentDataType.STRING)) {
      String itemId = pdc.get(itemIdKey, PersistentDataType.STRING);
      ItemData itemData = itemDataManager.getItemData(itemId);

      if (itemData != null && itemData.getSlotType() != null) {
        return plugin.getCuriosPaperAPI().tagAccessoryItem(item, itemData.getSlotType());
      }
    }
    return item;
  }
}
