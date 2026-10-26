package org.Cry_Legende.sHItemsBoosts;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LocationManager {

    private final SHItemsBoosts plugin;
    private final File locationsFile;

    private final Map<Integer, Location> spawnLocations = new ConcurrentHashMap<>();
    // Points dont le monde n'est pas encore chargé (PhantomWorlds) : gardés tels quels et réécrits à chaque sauvegarde.
    private final List<Map<?, ?>> pendingLocations = new ArrayList<>();
    private int nextId = 1;

    public LocationManager(SHItemsBoosts plugin) {
        this.plugin = plugin;
        this.locationsFile = new File(plugin.getDataFolder(), "locations.yml");
    }

    public void loadLocations() {
        spawnLocations.clear();
        pendingLocations.clear();

        // Lecture brute : la désérialisation Bukkit rendrait null (et la sauvegarde suivante effacerait) les points d'un monde pas encore chargé.
        List<?> rawList = null;
        if (locationsFile.exists()) {
            try (Reader reader = Files.newBufferedReader(locationsFile.toPath(), StandardCharsets.UTF_8)) {
                Object root = new Yaml().load(reader);
                if (root instanceof Map<?, ?> map && map.get("locations") instanceof List<?> list) {
                    rawList = list;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Impossible de lire le fichier locations.yml", e);
                // la prochaine sauvegarde réécrirait un fichier vide : on en garde une copie
                try {
                    Files.copy(locationsFile.toPath(), new File(plugin.getDataFolder(), "locations.yml.illisible").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException copyError) {
                    plugin.getLogger().log(Level.SEVERE, "Copie de locations.yml impossible", copyError);
                }
            }
        }
        if (rawList == null) {
            nextId = 1;
            plugin.getLogger().info("Aucun point d'apparition trouvé. Le fichier est peut-être vide ou nouveau.");
            return;
        }

        int currentId = 1;
        for (Object obj : rawList) {
            if (!(obj instanceof Map<?, ?> raw)) continue;
            Location location = deserialize(raw);
            if (location != null) {
                spawnLocations.put(currentId++, location);
            } else {
                pendingLocations.add(raw);
            }
        }
        nextId = currentId;
        plugin.getLogger().info((nextId - 1) + " points d'apparition chargés.");
        if (!pendingLocations.isEmpty()) {
            plugin.getLogger().info(pendingLocations.size() + " points d'apparition en attente du chargement de leur monde.");
        }
    }

    /** Location lue depuis la map YAML, ou null si son monde n'est pas (encore) chargé. */
    @SuppressWarnings("unchecked")
    private Location deserialize(Map<?, ?> raw) {
        try {
            return Location.deserialize((Map<String, Object>) raw);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Un monde vient de se charger : résout les points en attente, sans relire le fichier ni toucher aux autres. */
    public void resolvePending() {
        if (pendingLocations.isEmpty()) return;
        int resolved = 0;
        for (Iterator<Map<?, ?>> it = pendingLocations.iterator(); it.hasNext(); ) {
            Location location = deserialize(it.next());
            if (location != null) {
                spawnLocations.put(nextId++, location);
                it.remove();
                resolved++;
            }
        }
        if (resolved > 0) {
            plugin.getLogger().info(resolved + " points d'apparition retrouvés après le chargement de leur monde.");
        }
    }

    private void saveLocations() {
        // Points résolus + points en attente, pour ne rien perdre tant qu'un monde n'est pas chargé.
        List<Object> locationList = new ArrayList<>(spawnLocations.values());
        locationList.addAll(pendingLocations);
        YamlConfiguration locationsConfig = new YamlConfiguration();
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
            .sorted(Comparator.comparing(LocationManager::worldName)
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

    private static String worldName(Location loc) {
        return loc.isWorldLoaded() ? loc.getWorld().getName() : "";
    }

    public Location getLocation(int id) {
        return spawnLocations.get(id);
    }

    public Map<Integer, Location> getSpawnLocations() {
        return new HashMap<>(spawnLocations);
    }
}
