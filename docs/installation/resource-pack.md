# Resource Pack Setup

CuriosPaper includes a built-in resource pack system that provides custom textures for slot icons and custom items. The plugin can automatically generate, host, and serve the resource pack to players.

## Enabling the Resource Pack Server

In `config.yml`, configure the resource pack section:

```yaml
resource-pack:
  enabled: true
  port: 8080
  host-ip: "your-server-ip"
  base-material: "PAPER"
  allow-minecraft-namespace: false
  allow-namespace-conflicts: false
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Enable the built-in HTTP server |
| `port` | `8080` | Port for the HTTP server (must be different from the game server port) |
| `host-ip` | `localhost` | Public IP or hostname players use to connect |
| `base-material` | `PAPER` | Base material used for custom slot icons |
| `allow-minecraft-namespace` | `false` | Allow modifying vanilla `minecraft` namespace assets |
| `allow-namespace-conflicts` | `false` | Allow file conflicts between registered sources |

!!! warning "Port Configuration"
    The resource pack port **must** be different from your Minecraft server port. Using the same port will cause conflicts.

## How It Works

1. On startup, CuriosPaper extracts its embedded resource pack assets
2. The plugin generates a `.zip` resource pack file
3. An HTTP server starts on the configured port
4. When players join, they receive the resource pack URL

## Firewall Configuration

If players cannot download the resource pack, ensure:

- The configured `port` (default: 8080) is open in your firewall
- Port forwarding is set up if behind a router
- The `host-ip` is set to your server's public IP (not `localhost` in production)

## Managing the Resource Pack

Use the `/curios rp` commands to manage the resource pack:

```
/curios rp info       — View resource pack status and details
/curios rp rebuild    — Regenerate and re-send the resource pack
/curios rp conflicts  — Show file/namespace conflicts
```

!!! tip "After Configuration Changes"
    Run `/curios rp rebuild` after changing resource pack settings or custom item textures to regenerate the pack and push it to online players.

## Custom Model Data vs Item Models

CuriosPaper supports two methods for custom item textures:

| Method | Minecraft Version | Config Key |
|---|---|---|
| CustomModelData | 1.14 – 1.21.2 | `custom-model-data` |
| Item Model | 1.21.3+ | `item-model` |

The plugin automatically uses the correct method based on your server version.

See [Custom Model Data](../resource-pack/custom-model-data.md) for detailed texture setup instructions.

---

## 🌐 Geyser & Bedrock Edition Support

If you have Bedrock Edition players joining your server via GeyserMC, they will need the Geyser Bedrock Resource Pack and custom mappings to view CuriosPaper slot icons correctly.

### Setup Instructions

- **Automatic Setup:** If Geyser is running on the same server, CuriosPaper will automatically detect Geyser's folder on startup, extract the mappings and resource pack, and copy them directly to Geyser's `packs/` and `custom_mappings/` folders.
- **Manual Setup (Proxy/Separate Server):** If Geyser is hosted on a separate proxy (e.g. Velocity/BungeeCord) or automatic setup is skipped:
  1. **Locate Mappings:** Retrieve the Geyser files from your server's `plugins/CuriosPaper/geyser/` directory (automatically extracted on startup).
  2. **Install Resource Pack:** Copy `CuriosPaper_Geyser.zip` and place it in your Geyser's `packs/` folder.
  3. **Install Mappings:** Copy `CuriosPaper_mappings.json` and place it in your Geyser's `custom_mappings/` directory.
  4. **Enable Custom Content:** In Geyser's `config.yml`, ensure that `enable-custom-content` is set to `true`.
  5. **Restart:** Restart your Geyser proxy.
