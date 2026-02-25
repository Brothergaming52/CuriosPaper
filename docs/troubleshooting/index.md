# Troubleshooting

Solutions to common issues with CuriosPaper.

## Sections

| Page | Description |
|---|---|
| [Common Issues](common-issues.md) | Frequently encountered problems and their solutions |

## Quick Diagnostic

If something isn't working:

1. **Check the console** for error messages on startup
2. **Enable debug logging**:
   ```yaml
   debug:
     enabled: true
   ```
3. **Check permissions** with `/curios debug player <name>`
4. **Inspect items** with `/curios debug item` while holding the item
5. **Check the resource pack** with `/curios rp info`
