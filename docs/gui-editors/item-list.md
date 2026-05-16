# Item List GUI

The Item List GUI is a powerful administrative tool that allows you to browse, manage, and inspect all custom items created with CuriosPaper.

## Accessing the List

Run the following command:

```
/curios list
```

**Permission required:** `curiospaper.admin`

## Features

### Paginated Browser

View all custom items in a paginated 54-slot inventory. The list automatically filters out hidden items and displays each item with its configured name, material, and slot type.

| Interaction | Result |
|---|---|
| **Left-Click** | Open the [Recipe List](#recipe-list) for the item |
| **Right-Click** | Open the [Main Edit GUI](index.md#main-edit-gui) for the item |
| **Arrows** | Navigate between pages |

### Recipe List

Clicking an item in the browser opens its Recipe List, showing all configured ways to obtain the item (crafting, smelting, smithing, etc.).

- **Icon Indicators:** Different recipe types show unique icons (e.g., Furnace for smelting, Anvil for repairs).
- **Admin Give:** A special button allows admins to instantly give themselves the item for testing.
- **Back Button:** Easily return to the main item list.

### Recipe Detail View

Clicking a recipe icon opens the **Recipe Detail View**, which visually displays the recipe's shape and ingredients. This is useful for verifying recipe configurations without checking YAML files.

- **3x3 Grid:** Shows the exact layout for shaped recipes.
- **Ingredient Tooltips:** Hover over ingredients to see their material names.

## Use Cases

1. **Quick Editing:** Instead of typing `/curios edit <id>`, browse the list and right-click the item you want to modify.
2. **Recipe Verification:** Visually confirm that your complex shaped recipes are correctly configured.
3. **Item Testing:** Use the "Admin Give" feature in the recipe list to quickly grab items for testing in-game.
4. **Inventory Management:** See an overview of all accessories available on your server in one place.
