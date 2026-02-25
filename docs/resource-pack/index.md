# Resource Pack System

CuriosPaper includes a complete resource pack management system that generates, hosts, and distributes custom textures for slot icons and custom items.

## Sections

| Page | Description |
|---|---|
| [Hosting](hosting.md) | Built-in HTTP server configuration |
| [Custom Model Data](custom-model-data.md) | Creating custom textures for items and slots |
| [Troubleshooting](troubleshooting.md) | Common resource pack issues |

## Architecture

```
┌──────────────────┐     ┌──────────────────┐
│  ResourcePack  │◀───▶│ External Plugin │
│  Manager       │     │  Assets         │
└──────────────────┘     └──────────────────┘
         │
         ▼
┌──────────────────┐     ┌──────────────────┐
│ Pack Generator │────▶│  HTTP Server    │
│ (.zip creation)│     │  (Netty-based)  │
└──────────────────┘     └──────────────────┘
         │                        │
         ▼                        ▼
┌──────────────────┐     ┌──────────────────┐
│  SHA-1 Hash    │     │ Player Download │
│  Calculation   │     │ (on join)       │
└──────────────────┘     └──────────────────┘
```

## Key Features

- **Automatic generation** — Pack is built from embedded assets and registered sources
- **Built-in HTTP server** — Serves the pack without external hosting
- **Multi-plugin support** — Other plugins can register asset folders
- **Conflict detection** — Warns about namespace and file conflicts
- **Hot rebuild** — Regenerate and re-push with `/curios rp rebuild`
