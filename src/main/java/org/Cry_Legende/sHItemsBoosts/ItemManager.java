package org.Cry_Legende.sHItemsBoosts;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ItemManager {

    private final SHItemsBoosts plugin;
    private final List<BoostType> boostTypes = new ArrayList<>();

    // CHANGEMENT MAJEUR : La structure de données principale.
    // On suit maintenant les boosts par leur point de spawn de base.
    // La valeur est notre nouvel objet qui contient le type de boost et l'UUID de l'item.
    private final Map<Location, ActiveBoost> activeBoosts = new ConcurrentHashMap<>();

    private final NamespacedKey boostItemKey;
    private BukkitTask spawnerTask;
    private BukkitTask pickupTask;

    public ItemManager(SHItemsBoosts plugin) {
        this.plugin = plugin;
        this.boostItemKey = new NamespacedKey(plugin, "shib_boost_item");
    }

    /**
     * Vérifie si un boost avec un UUID d'item spécifique est actuellement suivi par le manager.
     *
     * @param uuid L'UUID de l'entité Item à vérifier.
     * @return true si l'item est activement suivi, false sinon.
     */
    public boolean isBoostTracked(UUID uuid) {
        // On vérifie si l'un des objets ActiveBoost dans notre map contient cet UUID.
        return activeBoosts.values().stream().anyMatch(boost -> boost.getItemEntityId().equals(uuid));
    }

    // --- spawnSingleItem() a été modifié pour utiliser la nouvelle structure ---
    private boolean spawnSingleItem() {
        int maxItems = plugin.getConfig().getInt("settings.max-concurrent-items", 20);
        // On vérifie la taille de notre nouvelle map
        if (activeBoosts.size() >= maxItems || boostTypes.isEmpty()) {
            return false;
        }

        // On utilise les clés de la nouvelle map pour savoir quels points sont occupés
        List<Location> availableLocations = new ArrayList<>(plugin.getLocationManager().getSpawnLocations().values());
        availableLocations.removeAll(activeBoosts.keySet());

        if (availableLocations.isEmpty()) {
            return false;
        }

        Random random = new Random();
        Location spawnLocation = availableLocations.get(random.nextInt(availableLocations.size())); // C'est notre clé
        BoostType boostToSpawn = getRandomWeightedBoost();
        if (boostToSpawn == null) return false;

        double yOffset = plugin.getConfig().getDouble("settings.spawn-height-offset", 1.0);
        Location itemSpawnLocation = spawnLocation.clone().add(0.5, yOffset, 0.5);

        if (!itemSpawnLocation.getChunk().isLoaded()) return false;

        // La création de l'item reste la même...
        ItemStack itemStack = new ItemStack(boostToSpawn.getItemType());
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(boostToSpawn.getDisplayName());
        if (boostToSpawn.isEnchanted()) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.getPersistentDataContainer().set(boostItemKey, PersistentDataType.BYTE, (byte) 1);
        itemStack.setItemMeta(meta);

        Item droppedItem = itemSpawnLocation.getWorld().dropItem(itemSpawnLocation, itemStack);
        droppedItem.setGravity(false);
        droppedItem.setUnlimitedLifetime(true);
        droppedItem.setPickupDelay(Integer.MAX_VALUE);
        droppedItem.setVelocity(new Vector(0, 0, 0));

        // On stocke le nouvel objet ActiveBoost dans notre map
        activeBoosts.put(spawnLocation, new ActiveBoost(boostToSpawn, droppedItem.getUniqueId()));

        if (plugin.getConfig().getBoolean("sound.play-sound-on-spawn", true)) {
            playSoundOnSpawn(itemSpawnLocation);
        }
        return true;
    }

    // --- checkPlayerProximity() est la partie la plus lourdement modifiée ---
    private void checkPlayerProximity() {
        if (activeBoosts.isEmpty()) return;
        double pickupRadiusSquared = Math.pow(plugin.getConfig().getDouble("settings.pickup-radius", 1.5), 2);

        Iterator<Map.Entry<Location, ActiveBoost>> iterator = activeBoosts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, ActiveBoost> entry = iterator.next();
            Location spawnPointKey = entry.getKey();
            ActiveBoost activeBoost = entry.getValue();

            // On essaie de récupérer l'entité via son UUID.
            Entity entity = Bukkit.getEntity(activeBoost.getItemEntityId());

            // CAS 1 : L'entité n'existe plus (supprimée par une commande, etc.)
            if (entity == null || !entity.isValid()) {
                // L'item a disparu. On nettoie notre suivi.
                iterator.remove();
                continue;
            }

            // On s'assure que c'est bien un Item
            if (!(entity instanceof Item)) {
                iterator.remove();
                continue;
            }
            Item item = (Item) entity;

            // CAS 2 : Le chunk est déchargé. L'entité est valide mais inaccessible.
            if (!item.getLocation().getChunk().isLoaded()) {
                // C'est la logique clé : on ne fait RIEN. On l'ignore pour ce tick.
                // On ne le supprime PAS de la map, on le garde en mémoire pour plus tard.
                continue;
            }

            // CAS 3 : Tout est normal, le chunk est chargé, l'item est là.
            for (Player player : item.getWorld().getPlayers()) {
                if ((player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) && player.getLocation().distanceSquared(item.getLocation()) <= pickupRadiusSquared) {
                    applyBoost(player, activeBoost.getBoostType());

                    item.remove(); // On supprime l'entité physique
                    iterator.remove(); // On supprime le boost de notre map de suivi

                    break;
                }
            }
        }
    }

    // --- cleanup() doit aussi être mise à jour ---
    public void cleanup() {
        if (spawnerTask != null) spawnerTask.cancel();
        if (pickupTask != null) pickupTask.cancel();

        for (ActiveBoost boost : activeBoosts.values()) {
            Entity entity = Bukkit.getEntity(boost.getItemEntityId());
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        activeBoosts.clear();
        plugin.getLogger().info("Tous les items de boost actifs ont été supprimés.");
    }

    // --- Les autres méthodes n'ont pas besoin de changements majeurs ---
    // (loadBoostTypes, burstSpawnInitialItems, cleanupOrphanedItems, getRandomWeightedBoost, playSoundOnSpawn, applyBoost, startTasks)
    // Je les inclus pour que tu aies le fichier complet et fonctionnel.

    public void loadBoostTypes() {
        boostTypes.clear();
        ConfigurationSection boostSection = plugin.getConfig().getConfigurationSection("boosts");
        if (boostSection == null) {
            plugin.getLogger().warning("La section 'boosts' est manquante dans config.yml !");
            return;
        }
        for (String key : boostSection.getKeys(false)) {
            try {
                String path = "boosts." + key + ".";
                Material itemType = Material.valueOf(plugin.getConfig().getString(path + "item-type", "STONE").toUpperCase());
                String displayName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString(path + "display-name", "Boost"));
                boolean enchanted = plugin.getConfig().getBoolean(path + "enchanted");
                PotionEffectType effectType = PotionEffectType.getByName(plugin.getConfig().getString(path + "effect-type", "SPEED").toUpperCase());
                int duration = plugin.getConfig().getInt(path + "duration-seconds", 10);
                int amplifier = plugin.getConfig().getInt(path + "amplifier", 0);
                int weight = plugin.getConfig().getInt(path + "weight", 10);
                String pickupMessage = plugin.getConfig().getString(path + "pickup-message", "");
                Sound pickupSound = Sound.valueOf(plugin.getConfig().getString(path + "pickup-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());
                float pickupVolume = (float) plugin.getConfig().getDouble(path + "pickup-sound-volume", 1.0);
                float pickupPitch = (float) plugin.getConfig().getDouble(path + "pickup-sound-pitch", 1.0);
                if (effectType == null) {
                    plugin.getLogger().warning("Type d'effet invalide pour le boost '" + key + "'. Ce boost sera ignoré.");
                    continue;
                }
                boostTypes.add(new BoostType(key, itemType, displayName, enchanted, effectType, duration, amplifier, pickupMessage, pickupSound, pickupVolume, pickupPitch, weight));
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Erreur lors du chargement du boost '" + key + "' depuis config.yml.", e);
            }
        }
        plugin.getLogger().info(boostTypes.size() + " types de boost chargés.");
    }

    public void startTasks() {
        if (spawnerTask != null) spawnerTask.cancel();
        if (pickupTask != null) pickupTask.cancel();
        long spawnInterval = plugin.getConfig().getLong("settings.spawn-check-interval-seconds", 5) * 20;
        spawnerTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnSingleItem();
            }
        }.runTaskTimer(plugin, 100L, spawnInterval);
        long pickupInterval = plugin.getConfig().getLong("settings.pickup-check-interval-ticks", 10);
        pickupTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkPlayerProximity();
            }
        }.runTaskTimer(plugin, 100L, pickupInterval);
    }

    public void burstSpawnInitialItems() {
        boolean burstEnabled = plugin.getConfig().getBoolean("settings.startup-burst", true);
        if (!burstEnabled) return;
        plugin.getLogger().info("Lancement du 'burst spawn' initial...");
        int maxItems = plugin.getConfig().getInt("settings.max-concurrent-items", 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                int spawnedCount = 0;
                for (int i = 0; i < maxItems; i++) {
                    if (spawnSingleItem()) {
                        spawnedCount++;
                    } else {
                        break;
                    }
                }
                plugin.getLogger().info(spawnedCount + " items ont été générés lors du 'burst spawn'.");
            }
        }.runTaskLater(plugin, 20L);
    }

    public void cleanupOrphanedItems() {
        int cleanedCount = 0;
        plugin.getLogger().info("Recherche d'items orphelins (anti-crash)...");
        for (World world : Bukkit.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {
                ItemMeta meta = item.getItemStack().getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(boostItemKey, PersistentDataType.BYTE)) {
                    item.remove();
                    cleanedCount++;
                }
            }
        }
        if (cleanedCount > 0) {
            plugin.getLogger().info(cleanedCount + " items orphelins ont été supprimés.");
        }
    }

    private BoostType getRandomWeightedBoost() {
        if (boostTypes.isEmpty()) return null;
        int totalWeight = boostTypes.stream().mapToInt(BoostType::getWeight).sum();
        if (totalWeight <= 0) return null;
        int randomValue = new Random().nextInt(totalWeight);
        int currentWeight = 0;
        for (BoostType type : boostTypes) {
            currentWeight += type.getWeight();
            if (randomValue < currentWeight) {
                return type;
            }
        }
        return null;
    }

    private void playSoundOnSpawn(Location loc) {
        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sound.spawn-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());
            float volume = (float) plugin.getConfig().getDouble("sound.spawn-sound-volume", 0.5);
            float pitch = (float) plugin.getConfig().getDouble("sound.spawn-sound-pitch", 1.5);
            double radiusSquared = Math.pow(plugin.getConfig().getDouble("sound.spawn-sound-radius", 25), 2);
            for (Player player : loc.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(loc) <= radiusSquared) {
                    player.playSound(loc, sound, volume, pitch);
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Son d'apparition invalide dans config.yml: " + plugin.getConfig().getString("sound.spawn-sound"));
        }
    }

    private void applyBoost(Player player, BoostType boost) {
        player.addPotionEffect(new PotionEffect(boost.getEffectType(), boost.getDurationSeconds() * 20, boost.getAmplifier(), false, true));
        try {
            player.playSound(player.getLocation(), boost.getPickupSound(), boost.getPickupSoundVolume(), boost.getPickupSoundPitch());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Son de ramassage invalide pour le boost '" + boost.getId() + "'.");
        }
        if (boost.getPickupMessage() != null && !boost.getPickupMessage().isEmpty()) {
            String displayName = ChatColor.stripColor(boost.getDisplayName());
            String message = boost.getPickupMessage()
                .replace("%boost_name%", displayName)
                .replace("%duration%", String.valueOf(boost.getDurationSeconds()));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }
}
