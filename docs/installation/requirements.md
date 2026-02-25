# Requirements

## Server Software

CuriosPaper is compatible with the following server platforms:

| Platform | Supported | Notes |
|---|---|---|
| **Spigot** | ✅ | 1.14.4 and above |
| **Paper** | ✅ | Recommended for best performance |
| **Purpur** | ✅ | Fully compatible |
| **Folia** | ❌ | Not currently supported |
| **Vanilla** | ❌ | Requires Bukkit-based server |

## Minecraft Version

| Version Range | Support Level |
|---|---|
| **1.14.4 – 1.21.2** | Full support (uses CustomModelData) |
| **1.21.3+** | Full support (uses Item Model component) |

!!! tip "Version Auto-Detection"
    CuriosPaper automatically detects your server version and uses the appropriate API. No manual version configuration is needed.

## Java Version

| Java Version | Status |
|---|---|
| **Java 8+** | ✅ Required minimum |
| **Java 17+** | ✅ Recommended |
| **Java 21** | ✅ Supported |

## Dependencies

CuriosPaper has **no required plugin dependencies**. It is a standalone plugin.

### Bundled Libraries

The following libraries are bundled inside the plugin JAR:

- **bStats** (v3.1.0) — Anonymous usage statistics
- **Netty** — Used by the built-in resource pack HTTP server (provided by the server)

## Hardware Requirements

CuriosPaper is lightweight and has minimal resource requirements:

- **RAM**: ~5–15 MB additional memory depending on player count
- **Disk**: < 1 MB for plugin files + player data files (one YAML file per player)
- **CPU**: Negligible impact during normal operation
