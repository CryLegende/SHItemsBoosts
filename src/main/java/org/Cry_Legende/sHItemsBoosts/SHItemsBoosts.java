package org.Cry_Legende.sHItemsBoosts;

import org.Cry_Legende.sHItemsBoosts.commands.SHIBCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class SHItemsBoosts extends JavaPlugin {

    private static SHItemsBoosts instance;
    private LocationManager locationManager;
    private ItemManager itemManager;
    private volatile YamlConfiguration messages;

    @Override
    public void onEnable() {
        instance = this;

        // Sauvegarde et chargement de la configuration
        saveDefaultConfig();
        loadMessagesAtStartup();

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
        PluginCommand command = getCommand("shitemsboosts");
        SHIBCommand executor = new SHIBCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

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

    /**
     * Relit config.yml et messages.yml. Si l'un des deux a une erreur de syntaxe, rien n'est appliqué (ni la config,
     * ni les boosts posés) : renvoie le fichier et la première ligne de l'erreur. Renvoie null quand tout est rechargé.
     * locations.yml (les points d'apparition) n'est pas relu : la mémoire fait foi, chaque ajout ou retrait l'écrit.
     */
    public String[] reloadPlugin() {
        File configFile = new File(getDataFolder(), "config.yml");
        try {
            new YamlConfiguration().load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            return new String[]{configFile.getName(), firstLine(e)};
        }
        YamlConfiguration newMessages;
        try {
            newMessages = readMessages();
        } catch (IOException | InvalidConfigurationException e) {
            return new String[]{"messages.yml", firstLine(e)};
        }

        // Retire les boosts posés, applique la nouvelle configuration puis repose les boosts
        itemManager.cleanup();
        reloadConfig();
        messages = newMessages;
        itemManager.loadBoostTypes();
        itemManager.startTasks();
        itemManager.burstSpawnInitialItems();
        getLogger().info("SHItemsBoosts a été rechargé.");
        return null;
    }

    // ------------------------------------------------------------------ messages

    private void loadMessagesAtStartup() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) {
            saveResource("messages.yml", false);
        }
        try {
            messages = readMessages();
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().warning("messages.yml illisible, textes par défaut utilisés : " + firstLine(e));
            messages = defaultMessages();
        }
    }

    private YamlConfiguration defaultMessages() {
        try (InputStream in = getResource("messages.yml")) {
            if (in != null) {
                return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            getLogger().warning("messages.yml du jar illisible : " + e.getMessage());
        }
        return new YamlConfiguration();
    }

    /** messages.yml du serveur ; les clés absentes retombent sur le texte du jar. */
    private YamlConfiguration readMessages() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(new File(getDataFolder(), "messages.yml"));
        yaml.setDefaults(defaultMessages());
        return yaml;
    }

    private static String firstLine(Exception e) {
        String text = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        int end = text.indexOf('\n');
        return (end < 0 ? text : text.substring(0, end)).trim();
    }

    /** Texte de messages.yml avec ses variables remplacées (paires %clé% / valeur) et ses codes couleur &. */
    public String message(String key, String... replacements) {
        String text = messages.getString(key);
        if (text == null) {
            text = "";
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
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
