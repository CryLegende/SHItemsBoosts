package org.Cry_Legende.sHItemsBoosts;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Ce listener agit comme un filet de sécurité permanent.
 * Il vérifie les entités de chaque chunk lors de leur chargement pour y trouver et supprimer
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

    // Sur Paper les entités se chargent après le chunk : au ChunkLoadEvent, chunk.getEntities() est encore vide.
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        // On parcourt toutes les entités qui viennent d'être chargées
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Item item) {
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

    // Un monde chargé après le démarrage (PhantomWorlds) : les points qui l'attendaient deviennent utilisables.
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getLocationManager().resolvePending();
    }
}
