package org.bg52.curiospaper.data;

import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents an ability/effect that can be applied when an item is equipped,
 * de-equipped, or while equipped.
 */
public class AbilityData {
  private TriggerType trigger;
  private EffectType effectType;
  private String effectName; // Potion effect type or modifier type
  private int amplifier; // Effect amplifier (0-9)
  private int duration; // Duration in ticks (for potion effects)
  private AttributeModifier.Operation operation; // Operation for player modifiers (ADD_NUMBER, ADD_SCALAR, MULTIPLY_SCALAR_1)

  /**
   * When the ability should trigger
   */
  public enum TriggerType {
    EQUIP, // When item is equipped
    DE_EQUIP, // When item is de-equipped
    WHILE_EQUIPPED // Continuously while equipped
  }

  /**
   * Type of effect to apply
   */
  public enum EffectType {
    POTION_EFFECT, // Apply a potion effect
    PLAYER_MODIFIER // Modify player attribute
  }

  public AbilityData(TriggerType trigger, EffectType effectType, String effectName, int amplifier, int duration) {
    this(trigger, effectType, effectName, amplifier, duration, AttributeModifier.Operation.ADD_NUMBER);
  }

  public AbilityData(TriggerType trigger, EffectType effectType, String effectName, int amplifier, int duration, AttributeModifier.Operation operation) {
    this.trigger = trigger;
    this.effectType = effectType;
    this.effectName = effectName;
    this.amplifier = amplifier;
    this.duration = Math.max(0, duration);
    this.operation = operation != null ? operation : AttributeModifier.Operation.ADD_NUMBER;
  }

  // Getters
  public TriggerType getTrigger() {
    return trigger;
  }

  public EffectType getEffectType() {
    return effectType;
  }

  public String getEffectName() {
    return effectName;
  }

  public int getAmplifier() {
    return amplifier;
  }

  public int getDuration() {
    return duration;
  }

  public AttributeModifier.Operation getOperation() {
    return operation != null ? operation : AttributeModifier.Operation.ADD_NUMBER;
  }

  // Setters
  public void setTrigger(TriggerType trigger) {
    this.trigger = trigger;
  }

  public void setEffectType(EffectType effectType) {
    this.effectType = effectType;
  }

  public void setEffectName(String effectName) {
    this.effectName = effectName;
  }

  public void setAmplifier(int amplifier) {
    this.amplifier = amplifier;
  }

  public void setDuration(int duration) {
    this.duration = Math.max(0, duration);
  }

  public void setOperation(AttributeModifier.Operation operation) {
    this.operation = operation != null ? operation : AttributeModifier.Operation.ADD_NUMBER;
  }

  /**
   * Saves this ability to a configuration section
   */
  public void saveToConfig(ConfigurationSection config) {
    config.set("trigger", trigger.name());
    config.set("effect-type", effectType.name());
    config.set("effect-name", effectName);
    config.set("amplifier", amplifier);
    config.set("duration", duration);
    if (effectType == EffectType.PLAYER_MODIFIER && operation != null) {
      config.set("operation", operation.name());
    }
  }

  /**
   * Loads an ability from a configuration section
   */
  public static AbilityData loadFromConfig(ConfigurationSection config) {
    try {
      TriggerType trigger = TriggerType.valueOf(config.getString("trigger", "EQUIP"));
      EffectType effectType = EffectType.valueOf(config.getString("effect-type", "POTION_EFFECT"));
      String effectName = config.getString("effect-name", "SPEED");
      int amplifier = config.getInt("amplifier", 0);
      int duration = config.getInt("duration", 200);

      AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
      String opStr = config.getString("operation", null);
      if (opStr != null) {
        try {
          operation = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
      }

      return new AbilityData(trigger, effectType, effectName, amplifier, duration, operation);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Validates the ability configuration
   */
  public boolean isValid() {
    return trigger != null && effectType != null && effectName != null && !effectName.isEmpty();
  }

  @Override
  public String toString() {
    return "AbilityData{" +
        "trigger=" + trigger +
        ", effectType=" + effectType +
        ", effectName='" + effectName + '\'' +
        ", amplifier=" + amplifier +
        ", duration=" + duration +
        ", operation=" + operation +
        '}';
  }
}
