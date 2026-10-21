package org.Cry_Legende.sHItemsBoosts;

import java.util.UUID;

/**
 * Représente un boost actuellement actif dans le monde.
 * Contient le type de boost et l'UUID de l'entité Item correspondante.
 */
public class ActiveBoost {
    private final BoostType boostType;
    private final UUID itemEntityId;

    public ActiveBoost(BoostType boostType, UUID itemEntityId) {
        this.boostType = boostType;
        this.itemEntityId = itemEntityId;
    }

    public BoostType getBoostType() {
        return boostType;
    }

    public UUID getItemEntityId() {
        return itemEntityId;
    }
}