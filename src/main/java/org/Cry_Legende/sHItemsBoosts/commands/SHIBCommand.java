package org.Cry_Legende.sHItemsBoosts.commands;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.Cry_Legende.sHItemsBoosts.SHItemsBoosts;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SHIBCommand implements CommandExecutor, TabCompleter {

    private final SHItemsBoosts plugin;
    private final String prefix = ChatColor.AQUA + "[SHIB] " + ChatColor.GRAY;

    public SHIBCommand(SHItemsBoosts plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                handleAdd(sender);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "tp":
                handleTp(sender, args);
                break;
            case "forcecleanup":
                handleForceCleanup(sender);
                break;
            // NOUVELLE COMMANDE DE DIAGNOSTIC
            case "inspect":
                handleInspect(sender);
                break;
            default:
                sendHelp(sender, label);
                break;
        }
        return true;
    }

    // NOUVELLE MÉTHODE DE DIAGNOSTIC
    private void handleInspect(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return;
        }
        Player player = (Player) sender;

        // Trouve l'item le plus proche du joueur dans un rayon de 5 blocs
        Optional<Item> closestItemOpt = player.getNearbyEntities(5, 5, 5).stream()
            .filter(e -> e instanceof Item)
            .map(e -> (Item) e)
            .min(Comparator.comparing(e -> e.getLocation().distanceSquared(player.getLocation())));

        if (closestItemOpt.isEmpty()) {
            player.sendMessage(prefix + "Aucun item trouvé à proximité.");
            return;
        }

        Item item = closestItemOpt.get();
        NamespacedKey boostItemKey = new NamespacedKey(plugin, "shib_boost_item");
        boolean hasTag = item.getItemStack().getItemMeta() != null && item.getItemStack().getItemMeta().getPersistentDataContainer().has(boostItemKey, PersistentDataType.BYTE);

        player.sendMessage(ChatColor.AQUA + "--- SHIB Inspector ---");
        player.sendMessage(ChatColor.GRAY + "Item: " + ChatColor.YELLOW + item.getItemStack().getType());
        player.sendMessage(ChatColor.GRAY + "Pickup Delay: " + ChatColor.GOLD + item.getPickupDelay() + " ticks");
        player.sendMessage(ChatColor.GRAY + "Unlimited Lifetime: " + (item.isUnlimitedLifetime() ? ChatColor.GREEN + "true" : ChatColor.RED + "false"));
        player.sendMessage(ChatColor.GRAY + "Has new SHIB Tag: " + (hasTag ? ChatColor.GREEN + "true" : ChatColor.RED + "false"));
        player.sendMessage(ChatColor.AQUA + "----------------------");
    }

    // MÉTHODE CORRIGÉE
    private void handleForceCleanup(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Cette commande doit être exécutée par un joueur pour cibler un monde spécifique.");
            return;
        }
        Player player = (Player) sender;
        World worldToClean = player.getWorld();

        sender.sendMessage(prefix + "Lancement du nettoyage forcé dans le monde '" + ChatColor.YELLOW + worldToClean.getName() + ChatColor.GRAY + "'...");

        NamespacedKey boostItemKey = new NamespacedKey(plugin, "shib_boost_item");
        int cleanedCount = 0;

        for (Item item : worldToClean.getEntitiesByClass(Item.class)) {
            // CORRECTION : On vérifie si le délai est très grand, au lieu d'une valeur exacte.
            if (item.getPickupDelay() > 30000) {
                ItemMeta meta = item.getItemStack().getItemMeta();
                if (meta == null || !meta.getPersistentDataContainer().has(boostItemKey, PersistentDataType.BYTE)) {
                    item.remove();
                    cleanedCount++;
                }
            }
        }

        sender.sendMessage(prefix + "Nettoyage terminé. " + ChatColor.YELLOW + cleanedCount + ChatColor.GRAY + " anciens items orphelins ont été supprimés de ce monde.");
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(prefix + "Commandes disponibles :");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " add, remove, list, reload");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " forcecleanup" + ChatColor.GRAY + " - Supprime les anciens items orphelins.");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " inspect" + ChatColor.GRAY + " - Affiche les infos de l'item le plus proche.");
    }

    // --- Le reste des méthodes est inchangé ---
    private void handleAdd(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Cette commande est réservée aux joueurs.");
            return;
        }
        Player player = (Player) sender;
        Location location = player.getLocation().toBlockLocation();
        int newId = plugin.getLocationManager().addLocation(location);
        sender.sendMessage(prefix + "Point d'apparition " + ChatColor.YELLOW + "#" + newId + ChatColor.GRAY + " ajouté à votre position.");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(prefix + "Usage: /shib remove <id>");
            return;
        }
        try {
            int id = Integer.parseInt(args[1]);
            if (plugin.getLocationManager().removeLocation(id)) {
                sender.sendMessage(prefix + "Point d'apparition " + ChatColor.YELLOW + "#" + id + ChatColor.GRAY + " supprimé.");
            } else {
                sender.sendMessage(prefix + ChatColor.RED + "Aucun point d'apparition trouvé avec l'ID " + id + ".");
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + ChatColor.RED + "L'ID doit être un nombre.");
        }
    }

    private void handleList(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + ChatColor.RED + "Cette commande est réservée aux joueurs pour l'affichage interactif.");
            return;
        }
        Player player = (Player) sender;
        Map<Integer, Location> locations = plugin.getLocationManager().getSpawnLocations();
        if (locations.isEmpty()) {
            player.sendMessage(prefix + "Aucun point d'apparition n'a été défini. Utilisez " + ChatColor.YELLOW + "/shib add" + ChatColor.GRAY + ".");
            return;
        }
        player.sendMessage(prefix + "Liste des points d'apparition (cliquables) :");
        for (Map.Entry<Integer, Location> entry : locations.entrySet()) {
            int id = entry.getKey();
            Location loc = entry.getValue();
            String posString = String.format("%d, %d, %d dans le monde '%s'", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
            TextComponent message = new TextComponent(ChatColor.YELLOW.toString() + id + ". " + ChatColor.GRAY + posString + " ");
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shib tp " + id));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Cliquez pour vous téléporter").color(net.md_5.bungee.api.ChatColor.GREEN).create()));
            TextComponent removeButton = new TextComponent("§7(§c§l-§7)");
            removeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shib remove " + id));
            removeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Cliquez pour supprimer le point #" + id).color(net.md_5.bungee.api.ChatColor.RED).create()));
            message.addExtra(removeButton);
            player.spigot().sendMessage(message);
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return;
        if (args.length < 2) return;
        Player player = (Player) sender;
        try {
            int id = Integer.parseInt(args[1]);
            Location loc = plugin.getLocationManager().getLocation(id);
            if (loc != null) {
                player.teleport(loc.clone().add(0.5, 0, 0.5));
                player.sendMessage(prefix + "Téléportation au point " + ChatColor.YELLOW + "#" + id + ChatColor.GRAY + ".");
            } else {
                player.sendMessage(prefix + ChatColor.RED + "Ce point n'existe plus.");
            }
        } catch (NumberFormatException e) { /* Ignorer */ }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPlugin();
        sender.sendMessage(prefix + "Le plugin a été rechargé.");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove", "list", "reload", "forcecleanup", "inspect").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return plugin.getLocationManager().getSpawnLocations().keySet().stream().map(String::valueOf).filter(s -> s.startsWith(args[1])).collect(Collectors.toList());
        }
        return List.of();
    }
}
