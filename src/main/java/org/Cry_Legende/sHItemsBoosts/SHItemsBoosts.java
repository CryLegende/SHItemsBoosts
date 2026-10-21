package org.Cry_Legende.sHItemsBoosts;

import org.Cry_Legende.sHItemsBoosts.commands.SHIBCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SHItemsBoosts extends JavaPlugin {

    private static SHItemsBoosts instance;
    private LocationManager locationManager;
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        instance = this;

        // Sauvegarde et chargement de la configuration
        saveDefaultConfig();

        // Initialisation des gestionnaires
        this.locationManager = new LocationManager(this);
        this.itemManager = new ItemManager(this);

        itemManager.cleanupOrphanedItems();

        // Chargement des données
        locationManager.loadLocations();
        itemManager.loadBoostTypes();

        // Démarrage des tâches du gestionnaire d'items
        itemManager.startTasks();

        itemManager.burstSpawnInitialItems();

        // Enregistrement des commandes
        getCommand("shitemsboosts").setExecutor(new SHIBCommand(this));

        getServer().getPluginManager().registerEvents(new ChunkListener(this, this.itemManager), this);

        getLogger().info("SHItemsBoosts a été activé avec succès !");
    }

    @Override
    public void onDisable() {
        // Arrêt des tâches et nettoyage
        if (itemManager != null) {
            itemManager.cleanup();
        }

        getLogger().info("SHItemsBoosts a été désactivé.");
    }

    public void reloadPlugin() {
        // Nettoyage avant de recharger
        if (itemManager != null) {
            itemManager.cleanup();
        }

        // Rechargement
        reloadConfig();
        locationManager.loadLocations();
        itemManager.loadBoostTypes();
        itemManager.startTasks();
        getLogger().info("SHItemsBoosts a été rechargé.");
    }


    public static SHItemsBoosts getInstance() {
        return instance;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }
}