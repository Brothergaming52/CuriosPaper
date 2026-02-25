# Getting Started

Welcome to CuriosPaper! This section will help you understand the plugin's core concepts and get your first accessory created.

## Sections

| Page | Description |
|---|---|
| [Core Concepts](concepts.md) | Slots, accessories, tagging, and the data model |
| [Your First Accessory](first-accessory.md) | Step-by-step guide to creating a custom accessory |
| [GUI Overview](gui-overview.md) | How the accessory inventory GUI works |

## The Big Picture

CuriosPaper works in three layers:

1. **Slots** — Configurable categories (ring, necklace, etc.) defined in `config.yml`
2. **Items** — Custom items created via the `/edit` command or the API, tagged with a slot type
3. **Player Data** — Per-player YAML files tracking which items are equipped in each slot
