# Permissions

## Permission Nodes

| Permission | Default | Description |
|---|---|---|
| `curiospaper.admin` | OP | Access to `/curios` admin commands (resource pack management) |
| `curiospaper.debug` | OP | Access to `/curios debug` commands (player and item inspection) |
| `curiospaper.edit` | OP | Access to `/edit` command (create and manage custom items) |

## Command-Permission Mapping

| Command | Required Permission |
|---|---|
| `/baubles` | None |
| `/curios rp info` | `curiospaper.admin` |
| `/curios rp rebuild` | `curiospaper.admin` |
| `/curios rp conflicts` | `curiospaper.admin` |
| `/curios debug player <name>` | `curiospaper.debug` |
| `/curios debug item` | `curiospaper.debug` |
| `/edit create <id>` | `curiospaper.edit` |
| `/edit gui <id>` | `curiospaper.edit` |
| `/edit delete <id>` | `curiospaper.edit` |
| `/edit list` | `curiospaper.edit` |
| `/edit give <id> [player] [amount]` | `curiospaper.edit` |

## Setting Permissions

You can configure permissions using any permissions plugin (e.g., LuckPerms, PermissionsEx).

### LuckPerms Example

```
/lp group admin permission set curiospaper.admin true
/lp group admin permission set curiospaper.debug true
/lp group admin permission set curiospaper.edit true
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
