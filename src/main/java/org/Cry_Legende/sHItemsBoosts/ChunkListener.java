package org.Cry_Legende.sHItemsBoosts;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Ce listener agit comme un filet de sécurité permanent.
 * Il vérifie chaque chunk lors de son chargement pour y trouver et supprimer
 * d'éventuels items orphelins du plugin.
 */
public class ChunkListener implements Listener {

    private final SHItemsBoosts plugin;
    private final ItemManager itemManager;
    private final NamespacedKey boostItemKey;

    public ChunkListener(SHItemsBoosts plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.boostItemKey = new NamespacedKey(plugin, "shib_boost_item");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // On parcourt toutes les entités du chunk qui vient d'être chargé
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                ItemMeta meta = item.getItemStack().getItemMeta();

                // On vérifie si l'item a notre marqueur
                if (meta != null && meta.getPersistentDataContainer().has(boostItemKey, PersistentDataType.BYTE)) {

                    // S'il a le marqueur, on demande au manager s'il est activement suivi
                    if (!itemManager.isBoostTracked(item.getUniqueId())) {
                        // S'il n'est pas suivi, c'est un orphelin. On le supprime.
                        item.remove();
                    }
                }
            }
        }
    }
}