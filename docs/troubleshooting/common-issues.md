# Common Issues

## Items Can't Be Placed in Slots

**Problem:** An item won't go into an accessory slot.

**Causes & Solutions:**

1. **Item not tagged** — The item needs a `curiospaper:slot_type` PDC tag
    - Use `/curios edit <itemId>` to set the slot type
    - Or use the API: `api.tagAccessoryItem(item, "ring")`

2. **Wrong slot type** — The item's tag doesn't match the target slot
    - Use `/curios debug item` to check the item's slot type
    - Ensure it matches the slot you're trying to use

3. **Slot type doesn't exist** — The tagged slot type isn't in `config.yml`
    - Check `config.yml` for the slot key

## Abilities Not Working

**Problem:** Equipped accessories don't apply their effects.

**Solutions:**

1. **Check ability configuration:**
    - View the item file in `plugins/CuriosPaper/items/<id>.yml`
    - Verify trigger, effect type, and effect name are set

2. **Validate effect name:**
    - For `POTION_EFFECT`: Must be a valid `PotionEffectType` (e.g., `SPEED`, not `Speed`)
    - For `PLAYER_MODIFIER`: Must be a valid `Attribute` (e.g., `GENERIC_MAX_HEALTH`)

3. **Check amplifier range:**
    - Must be between 0 and 9

4. **WHILE_EQUIPPED duration too short:**
    - Set duration to at least 100 ticks to avoid flickering

## GUI Not Opening

**Problem:** `/baubles` doesn't open the GUI.

**Solutions:**

1. **Check if player data is loaded** — Rejoin the server
2. **Check console for errors** — Look for exceptions during GUI creation
3. **Verify plugin is enabled** — Run `/plugins` and check for CuriosPaper

## Custom Textures Not Showing

See [Resource Pack Troubleshooting](../resource-pack/troubleshooting.md).

## Plugin Won't Start

**Problem:** CuriosPaper fails to enable on startup.

**Solutions:**

1. **Check Java version** — Requires Java 8+
2. **Check server version** — Requires Spigot/Paper 1.14.4+
3. **Check for conflicting plugins** — Disable other plugins and test
4. **Delete `config.yml` and restart** — Regenerates default configuration
5. **Check console for stack traces** — Post the full error in the support channel

## Player Data Lost

**Problem:** A player's accessories disappeared.

**Solutions:**

1. **Check the data file** — Look in `plugins/CuriosPaper/playerdata/<UUID>.yml`
2. **Check for backup** — If backups are enabled, restore from a backup file
3. **Server crash** — If the server crashed, data since the last auto-save may be lost
    - Lower `save-interval` for more frequent saves
    - Enable `save-on-close` for immediate saves

## Elytra Back Slot Not Working

**Problem:** Elytra can't be placed in the back slot or gliding doesn't work.

**Solutions:**

1. **Check server version** — Requires Minecraft 1.21.3+ with Paper
2. **Check config:**
   ```yaml
   features:
     allow-elytra-on-back-slot: true
   ```
3. **Check for the back slot** — Ensure a slot with key `back` exists in `config.yml`

## High Memory Usage

**Problem:** CuriosPaper is using too much memory.

**Solutions:**

1. **Enable `unload-on-quit`:**
   ```yaml
   performance:
     unload-on-quit: true
   ```
2. **Reduce `max-items-per-slot`** if set unnecessarily high
3. **Clean up orphaned player data files** for deleted/banned players

## Recipe Not Registering

**Problem:** Custom recipes don't appear in the crafting table.

**Solutions:**

1. **Check recipe type** — Verify it's a valid type (`SHAPED`, `SHAPELESS`, etc.)
2. **Check ingredients** — All materials must be valid Bukkit `Material` names
3. **For shaped recipes** — Ensure the shape has exactly 3 rows of 3 characters
4. **Restart the server** — Recipes are registered on startup
5. **Check console** — Look for recipe registration errors
