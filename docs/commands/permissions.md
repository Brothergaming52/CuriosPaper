# Permissions

## Permission Nodes

| Permission | Default | Description |
|---|---|---|
| `curiospaper.admin` | OP | Access to `/curios` admin and management commands |
| `curiospaper.debug` | OP | Access to `/curios debug` commands (player and item inspection) |

## Command-Permission Mapping

| Command | Required Permission |
|---|---|
| `/baubles` | None |
| `/curios rp info` | `curiospaper.admin` |
| `/curios rp rebuild` | `curiospaper.admin` |
| `/curios rp conflicts` | `curiospaper.admin` |
| `/curios reload` | `curiospaper.admin` |
| `/curios create <id>` | `curiospaper.admin` |
| `/curios edit <id>` | `curiospaper.admin` |
| `/curios delete <id>` | `curiospaper.admin` |
| `/curios list` | `curiospaper.admin` |
| `/curios give <id> [player] [amount]` | `curiospaper.admin` |
| `/curios inspect <player> [slot]` | `curiospaper.admin` |
| `/curios recordrtp` | `curiospaper.admin` |
| `/curios debug player <name>` | `curiospaper.debug` |
| `/curios debug item` | `curiospaper.debug` |

## Setting Permissions

You can configure permissions using any permissions plugin (e.g., LuckPerms, PermissionsEx).

### LuckPerms Example

```
/lp group admin permission set curiospaper.admin true
/lp group admin permission set curiospaper.debug true
```

### permissions.yml (Bukkit)

```yaml
users:
  player-uuid:
    permissions:
      curiospaper.admin: true
      curiospaper.edit: true
```

!!! info "Default Behavior"
    All permission nodes default to **OP** — server operators have all CuriosPaper permissions by default. Regular players can use `/baubles` without any additional permissions.
