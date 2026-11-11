package org.Cry_Legende.sHItemsBoosts.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.Cry_Legende.sHItemsBoosts.SHItemsBoosts;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class SHIBCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "shitemsboosts.admin.";
    // Sous-commandes dans l'ordre d'affichage de la ligne d'usage
    private static final List<String> SUBCOMMANDS = List.of("add", "remove", "list", "tp", "forcecleanup", "inspect", "reload");
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final SHItemsBoosts plugin;

    public SHIBCommand(SHItemsBoosts plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> allowed = allowedSubcommands(sender);
        if (allowed.isEmpty()) {
            sender.sendMessage(plugin.message("no-permission"));
            return true;
        }
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (!allowed.contains(sub)) {
            sender.sendMessage(plugin.message("usage", "%subs%", String.join("&7|&e", allowed)));
            return true;
        }
        switch (sub) {
            case "add" -> handleAdd(sender);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "tp" -> handleTp(sender, args);
            case "forcecleanup" -> handleForceCleanup(sender, args);
            case "inspect" -> handleInspect(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(plugin.message("usage", "%subs%", String.join("&7|&e", allowed)));
        }
        return true;
    }

    /** Sous-commandes que l'expéditeur a le droit d'utiliser. */
    private List<String> allowedSubcommands(CommandSender sender) {
        List<String> allowed = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sender.hasPermission(PERM + sub)) {
                allowed.add(sub);
            }
        }
        return allowed;
    }

    private void handleInspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("player-only"));
            return;
        }

        // Trouve l'item le plus proche du joueur dans un rayon de 5 blocs
        Optional<Item> closestItemOpt = player.getNearbyEntities(5, 5, 5).stream()
            .filter(e -> e instanceof Item)
            .map(e -> (Item) e)
            .min(Comparator.comparing(e -> e.getLocation().distanceSquared(player.getLocation())));

        if (closestItemOpt.isEmpty()) {
            player.sendMessage(plugin.message("inspect.none"));
            return;
        }

        Item item = closestItemOpt.get();
        NamespacedKey boostItemKey = new NamespacedKey(plugin, "shib_boost_item");
        boolean hasTag = item.getItemStack().getItemMeta() != null && item.getItemStack().getItemMeta().getPersistentDataContainer().has(boostItemKey, PersistentDataType.BYTE);

        player.sendMessage(plugin.message("inspect.header"));
        player.sendMessage(plugin.message("inspect.item", "%item%", String.valueOf(item.getItemStack().getType())));
        player.sendMessage(plugin.message("inspect.pickup-delay", "%ticks%", String.valueOf(item.getPickupDelay())));
        player.sendMessage(plugin.message("inspect.unlimited-lifetime", "%value%", plugin.message(item.isUnlimitedLifetime() ? "inspect.yes" : "inspect.no")));
        player.sendMessage(plugin.message("inspect.tag", "%value%", plugin.message(hasTag ? "inspect.yes" : "inspect.no")));
        player.sendMessage(plugin.message("inspect.footer"));
    }

    /** Supprime les anciens items orphelins (délai de ramassage énorme, sans le marqueur actuel) d'un monde. */
    private void handleForceCleanup(CommandSender sender, String[] args) {
        World worldToClean;
        if (args.length > 1) {
            worldToClean = Bukkit.getWorld(args[1]);
            if (worldToClean == null) {
                sender.sendMessage(plugin.message("forcecleanup.world-unknown", "%world%", args[1]));
                return;
            }
        } else if (sender instanceof Player player) {
            worldToClean = player.getWorld();
        } else {
            sender.sendMessage(plugin.message("forcecleanup.world-required"));
            return;
        }

        sender.sendMessage(plugin.message("forcecleanup.started", "%world%", worldToClean.getName()));

        NamespacedKey boostItemKey = new NamespacedKey(plugin, "shib_boost_item");
        int cleanedCount = 0;

        for (Item item : worldToClean.getEntitiesByClass(Item.class)) {
            // On vérifie si le délai est très grand, au lieu d'une valeur exacte.
            if (item.getPickupDelay() > 30000) {
                ItemMeta meta = item.getItemStack().getItemMeta();
                if (meta == null || !meta.getPersistentDataContainer().has(boostItemKey, PersistentDataType.BYTE)) {
                    item.remove();
                    cleanedCount++;
                }
            }
        }

        sender.sendMessage(plugin.message("forcecleanup.done", "%count%", String.valueOf(cleanedCount), "%world%", worldToClean.getName()));
    }

    private void handleAdd(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("player-only"));
            return;
        }
        Location location = player.getLocation().toBlockLocation();
        int newId = plugin.getLocationManager().addLocation(location);
        sender.sendMessage(plugin.message("add.done", "%id%", String.valueOf(newId)));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.message("remove.usage"));
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            if (plugin.getLocationManager().removeLocation(id)) {
                sender.sendMessage(plugin.message("remove.done", "%id%", String.valueOf(id)));
            } else {
                sender.sendMessage(plugin.message("remove.unknown", "%id%", String.valueOf(id)));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.message("remove.invalid-id"));
        }
    }

    private void handleList(CommandSender sender) {
        Map<Integer, Location> locations = new TreeMap<>(plugin.getLocationManager().getSpawnLocations());
        if (locations.isEmpty()) {
            sender.sendMessage(plugin.message("list.none"));
            return;
        }
        sender.sendMessage(plugin.message("list.header", "%count%", String.valueOf(locations.size())));
        // Seul un joueur peut cliquer : la console reçoit les lignes sans boutons
        boolean clickable = sender instanceof Player;
        boolean canTp = clickable && sender.hasPermission(PERM + "tp");
        boolean canRemove = clickable && sender.hasPermission(PERM + "remove");
        for (Map.Entry<Integer, Location> entry : locations.entrySet()) {
            int id = entry.getKey();
            Location loc = entry.getValue();
            Component line = LEGACY.deserialize(plugin.message("list.line",
                "%id%", String.valueOf(id),
                "%x%", String.valueOf(loc.getBlockX()),
                "%y%", String.valueOf(loc.getBlockY()),
                "%z%", String.valueOf(loc.getBlockZ()),
                "%world%", loc.isWorldLoaded() ? loc.getWorld().getName() : "?"));
            if (canTp) {
                line = line.clickEvent(ClickEvent.runCommand("/shitemsboosts tp " + id))
                    .hoverEvent(HoverEvent.showText(LEGACY.deserialize(plugin.message("list.hover-tp"))));
            }
            if (canRemove) {
                line = line.append(LEGACY.deserialize(plugin.message("list.remove-button"))
                    .clickEvent(ClickEvent.runCommand("/shitemsboosts remove " + id))
                    .hoverEvent(HoverEvent.showText(LEGACY.deserialize(plugin.message("list.hover-remove", "%id%", String.valueOf(id))))));
            }
            sender.sendMessage(line);
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("player-only"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.message("tp.usage"));
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            Location loc = plugin.getLocationManager().getLocation(id);
            if (loc != null && loc.isWorldLoaded()) {
                player.teleport(loc.clone().add(0.5, 0, 0.5));
                player.sendMessage(plugin.message("tp.done", "%id%", String.valueOf(id)));
            } else {
                player.sendMessage(plugin.message("tp.unknown"));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.message("remove.invalid-id"));
        }
    }

    private void handleReload(CommandSender sender) {
        String[] error = plugin.reloadPlugin();
        if (error != null) {
            sender.sendMessage(plugin.message("reload-failed", "%file%", error[0], "%error%", error[1]));
            return;
        }
        sender.sendMessage(plugin.message("reloaded",
            "%types%", String.valueOf(plugin.getItemManager().boostTypeCount()),
            "%points%", String.valueOf(plugin.getLocationManager().getSpawnLocations().size())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> allowed = allowedSubcommands(sender);
        List<String> matches = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : allowed) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    matches.add(sub);
                }
            }
            return matches;
        }
        if (args.length == 2 && allowed.contains(args[0].toLowerCase(Locale.ROOT))) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "remove", "tp" -> plugin.getLocationManager().getSpawnLocations().keySet().stream()
                    .sorted().map(String::valueOf).filter(s -> s.startsWith(args[1])).forEach(matches::add);
                case "forcecleanup" -> Bukkit.getWorlds().stream().map(World::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).forEach(matches::add);
                default -> {
                }
            }
        }
        return matches;
    }
}
