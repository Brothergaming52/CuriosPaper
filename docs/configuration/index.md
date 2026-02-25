# Configuration

CuriosPaper is fully configurable through `config.yml`. This section covers all available options.

## Sections

| Page | Description |
|---|---|
| [Slots](slots.md) | Accessory slot type definitions |
| [Abilities](abilities.md) | Ability configuration for custom items |
| [Performance & Storage](performance.md) | Performance tuning, storage, and caching |

## Config File Location

```
plugins/CuriosPaper/config.yml
```

## Reloading Configuration

CuriosPaper reads `config.yml` on startup. To apply configuration changes:

1. Edit `config.yml`
2. Restart the server (or reload the plugin if supported)

!!! note "Hot Reload"
    Some settings (like GUI titles and sound effects) take effect immediately. Slot type changes require a server restart.

## Complete Config Reference

See the [default config.yml](https://github.com/Brothergaming52/CuriosPaper/blob/main/src/main/resources/config.yml) for all options with inline comments.
