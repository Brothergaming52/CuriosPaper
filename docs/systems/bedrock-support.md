# Bedrock Edition & Geyser Support

CuriosPaper provides full support for Bedrock Edition players connecting to Java servers via **GeyserMC** and **Floodgate**.

## Overview

Bedrock Edition players face two main challenges on Java Edition custom item servers:
1. **Custom Model Data & Slot Icons:** Custom resource pack textures and slot icons require Bedrock mapping definitions.
2. **Container Packet Differences:** Java Anvil and Smithing Table containers rely on client-side prediction and server packets that behave differently or glitch on Bedrock clients.

CuriosPaper solves both issues natively without requiring external plugins.

```
┌─────────────────────────┐
│ Bedrock Player Joins    │
│ (via Geyser / Floodgate)│
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│ BedrockUtil Detection   │
│ (Floodgate / Geyser API)│
└────────────┬────────────┘
             │
    ┌────────┴────────────────────────┐
    ▼                                 ▼
┌───────────────────────┐   ┌───────────────────────┐
│ Custom Bedrock Anvil  │   │ Custom Bedrock        │
│ 3-Row Chest GUI       │   │ Smithing 3-Row GUI    │
└───────────────────────┘   └───────────────────────┘
```

---

## Bedrock Player Detection (`BedrockUtil`)

CuriosPaper automatically identifies Bedrock players using a multi-tiered reflection-based check:

1. **Floodgate API:** Checks `FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())`.
2. **Geyser API:** Checks `GeyserApi.api().isBedrockPlayer(player.getUniqueId())`.
3. **Offline UUID Fallback:** Checks if the player's UUID matches the Floodgate offline prefix pattern (`00000000-0000-0000-...`).

No compile-time dependencies on Geyser or Floodgate are required; detection is handled dynamically via reflection.

---

## Custom Bedrock Containers

To guarantee smooth container gameplay for Bedrock players, CuriosPaper intercepts Anvil and Smithing Table interactions and presents dedicated 3-row Chest interfaces.

### 🔨 Bedrock Anvil GUI (`BedrockAnvilGUI`)

When a Bedrock player right-clicks an Anvil (or opens an Anvil inventory window), CuriosPaper opens a custom 3-row Chest GUI:

- **Combining & Repairing:** Place base gear in slot 10 and sacrificial material/gear in slot 12.
- **Renaming:** Click the Name Tag icon in slot 14 to enter a custom display name via chat input.
- **Level Cost Calculation:** Automatically computes standard Minecraft anvil repair/combine XP level costs.
- **Output:** The result appears in slot 16. Click to consume XP levels and claim the item.

### 🛠️ Bedrock Smithing GUI (`BedrockSmithingGUI`)

When a Bedrock player right-clicks a Smithing Table, CuriosPaper opens a custom 3-row Chest GUI:

- **Minecraft 1.20+ Template Smithing:** Slot 10 (Smithing Template), Slot 12 (Base Gear), Slot 14 (Addition Material). Output appears in slot 16.
- **Legacy Smithing (Pre-1.20):** Slot 11 (Base Gear) and Slot 13 (Addition Material). Output appears in slot 15.
- Supports vanilla Netherite upgrades, trim applications, and custom CuriosPaper smithing recipes.

### Configuration Toggles

Both custom container interfaces are enabled by default and can be toggled in `config.yml`:

```yaml
features:
  # Enable custom Bedrock Smithing Table GUI
  use-custom-smithing-gui: true

  # Enable custom Bedrock Anvil GUI
  use-custom-anvil-gui: true
```

---

## Resource Pack & Geyser Mappings

CuriosPaper extracts two Bedrock asset files into `plugins/CuriosPaper/geyser/` on startup:

1. `CuriosPaper_Geyser.zip` — Bedrock resource pack containing custom slot icons and accessory textures.
2. `CuriosPaper_mappings.json` — Custom Geyser item mappings linking Java CustomModelData IDs to Bedrock pack textures.

### Automatic Setup (Geyser on Same Server)

If GeyserSpigot is running on the same server, CuriosPaper automatically locates Geyser's folder on boot and copies:
- `CuriosPaper_Geyser.zip` $\rightarrow$ `plugins/Geyser-Spigot/packs/`
- `CuriosPaper_mappings.json` $\rightarrow$ `plugins/Geyser-Spigot/custom_mappings/`

### Manual Setup (Velocity / BungeeCord Proxy Setup)

If Geyser is installed on a proxy (e.g., Velocity, BungeeCord, or Waterfall):
1. Copy `CuriosPaper_Geyser.zip` from your paper server's `plugins/CuriosPaper/geyser/` into your proxy's `plugins/Geyser-Velocity/packs/` folder.
2. Copy `CuriosPaper_mappings.json` into your proxy's `plugins/Geyser-Velocity/custom_mappings/` folder.
3. Ensure `enable-custom-content: true` is set in Geyser's `config.yml`.
4. Restart Geyser.
