package org.Cry_Legende.sHItemsBoosts;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

public class BoostType {

    private final String id;
    private final Material itemType;
    private final String displayName;
    private final boolean enchanted;
    private final PotionEffectType effectType;
    private final int durationSeconds;
    private final int amplifier;
    private final String pickupMessage;
    private final Sound pickupSound;
    private final float pickupSoundVolume;
    private final float pickupSoundPitch;
    private final int weight; // Nouvelle propriété

    public BoostType(String id, Material itemType, String displayName, boolean enchanted, PotionEffectType effectType, int durationSeconds, int amplifier, String pickupMessage, Sound pickupSound, float pickupSoundVolume, float pickupSoundPitch, int weight) {
        this.id = id;
        this.itemType = itemType;
        this.displayName = displayName;
        this.enchanted = enchanted;
        this.effectType = effectType;
        this.durationSeconds = durationSeconds;
        this.amplifier = amplifier;
        this.pickupMessage = pickupMessage;
        this.pickupSound = pickupSound;
        this.pickupSoundVolume = pickupSoundVolume;
        this.pickupSoundPitch = pickupSoundPitch;
        this.weight = weight; // Initialisation
    }

    // --- Getters ---
    public String getId() {
        return id;
    }

    public Material getItemType() {
        return itemType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnchanted() {
        return enchanted;
    }

    public PotionEffectType getEffectType() {
        return effectType;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public String getPickupMessage() {
        return pickupMessage;
    }

    public Sound getPickupSound() {
        return pickupSound;
    }

    public float getPickupSoundVolume() {
        return pickupSoundVolume;
    }

    public float getPickupSoundPitch() {
        return pickupSoundPitch;
    }

    public int getWeight() {
        return weight;
    } // Getter pour le poids
}
