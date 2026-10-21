package org.Cry_Legende.sHItemsBoosts;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LocationManager {

    private final SHItemsBoosts plugin;
    private final File locationsFile;
    private FileConfiguration locationsConfig;

    private final Map<Integer, Location> spawnLocations = new ConcurrentHashMap<>();
    private int nextId = 1;

    public LocationManager(SHItemsBoosts plugin) {
        this.plugin = plugin;
        this.locationsFile = new File(plugin.getDataFolder(), "locations.yml");
    }

    public void loadLocations() {
        // CORRECTION : On initialise toujours locationsConfig, même si le fichier n'existe pas.
        // Cela empêche la NullPointerException.
        this.locationsConfig = YamlConfiguration.loadConfiguration(this.locationsFile);

        spawnLocations.clear();

        List<?> rawList = locationsConfig.getList("locations");
        if (rawList == null) {
            nextId = 1;
            plugin.getLogger().info("Aucun point d'apparition trouvé. Le fichier est peut-être vide ou nouveau.");
            return;
        }

        int currentId = 1;
        for (Object obj : rawList) {
            if (obj instanceof Location) {
                spawnLocations.put(currentId++, (Location) obj);
            }
        }
        nextId = currentId;
        plugin.getLogger().info((nextId - 1) + " points d'apparition chargés.");
    }

    private void saveLocations() {
        // On ne recharge pas le fichier ici, on utilise l'objet en mémoire.
        List<Location> locationList = new ArrayList<>(spawnLocations.values());
        locationsConfig.set("locations", locationList);
        try {
            locationsConfig.save(locationsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de sauvegarder le fichier locations.yml", e);
        }
    }

    public int addLocation(Location location) {
        int newId = nextId++;
        spawnLocations.put(newId, location);
        saveLocations();
        return newId;
    }

    public boolean removeLocation(int id) {
        if (spawnLocations.containsKey(id)) {
            spawnLocations.remove(id);
            reindexLocations(); // Réindexer pour ne pas avoir de "trous" dans les IDs
            saveLocations();
            return true;
        }
        return false;
    }

    private void reindexLocations() {
        Map<Integer, Location> reindexedMap = new ConcurrentHashMap<>();
        int currentId = 1;

        // Trier les locations restantes assure une réindexation cohérente et prédictible
        List<Location> sortedLocations = spawnLocations.values().stream()
            .sorted(Comparator.comparing((Location loc) -> loc.getWorld().getName())
                .thenComparing(Location::getX)
                .thenComparing(Location::getY)
                .thenComparing(Location::getZ))
            .collect(Collectors.toList());

        for (Location loc : sortedLocations) {
            reindexedMap.put(currentId++, loc);
        }

        spawnLocations.clear();
        spawnLocations.putAll(reindexedMap);
        nextId = currentId;
    }

    public Location getLocation(int id) {
        return spawnLocations.get(id);
    }

    public Map<Integer, Location> getSpawnLocations() {
        return new HashMap<>(spawnLocations);
    }
}
