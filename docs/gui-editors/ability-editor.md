# Ability Editor

The Ability Editor allows you to visually add, modify, and remove abilities from custom items.

## Accessing

1. Open the item editor: `/edit gui <itemId>`
2. Click the **Abilities** button (potion bottle icon)
3. The Ability Editor GUI opens

## Interface

The Ability Editor displays each configured ability as an item in the GUI. Each ability shows:

- **Trigger type** (EQUIP, DE_EQUIP, WHILE_EQUIPPED)
- **Effect type** (POTION_EFFECT or PLAYER_MODIFIER)
- **Effect name** (e.g., SPEED, GENERIC_MAX_HEALTH)
- **Amplifier** and **duration**

<!-- TODO: Add image - In-game screenshot of the Ability Editor GUI showing a list of configured abilities as items, with their trigger/effect info visible in the lore -->
![Ability Editor showing configured abilities](../images/ability-editor-speed.png)

## Adding an Ability

1. Click the **Add Ability** button
2. Set each property via chat input when prompted:
    - **Trigger**: Type `equip`, `de_equip`, or `while_equipped`
    - **Effect Type**: Type `potion` or `modifier`
    - **Effect Name**: Type the effect/attribute name (e.g., `SPEED`)
    - **Amplifier**: Type a number (0–9)
    - **Duration**: Type duration in ticks (20 ticks = 1 second)

## Editing an Ability

1. Click on an existing ability in the GUI
2. Select which property to modify
3. Enter the new value in chat

## Removing an Ability

1. Click on an existing ability
2. Select the **Remove** option

## Validation

The editor validates abilities before saving:

- Effect name must be a valid PotionEffectType or Attribute
- Amplifier must be between 0 and 9
- Trigger must not be null

Invalid configurations are rejected with an error message in chat.
