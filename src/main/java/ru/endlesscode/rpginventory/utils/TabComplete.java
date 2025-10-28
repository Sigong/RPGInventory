package ru.endlesscode.rpginventory.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class TabComplete implements TabCompleter {

    private final List<String> subcommands = Arrays.asList("open", "reload", "food", "pet", "item", "bp");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest subcommands for first argument
            String partial = args[0].toLowerCase();
            for (String sub : subcommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // For subcommands that require a player name as second argument
            String firstArg = args[0].toLowerCase();
            if (firstArg.equals("open") || firstArg.equals("food") || firstArg.equals("pet") || firstArg.equals("item") || firstArg.equals("bp")) {
                // Suggest online player names
                String partial = args[1].toLowerCase();
                for (String playerName : sender.getServer().getOnlinePlayers().stream().map(p -> p.getName()).toList()) {
                    if (playerName.toLowerCase().startsWith(partial)) {
                        completions.add(playerName);
                    }
                }
            }
            // You can add other cases here for other subcommands if needed
        } else if (args.length == 3) {
            // For subcommands that need an id or similar
            String firstArg = args[0].toLowerCase();
            if (firstArg.equals("food")) {
                // Suggest food IDs
                List<String> foodIds = Arrays.asList("apple", "bread", "carrot");  // <-- Replace with your actual food IDs
                String partial = args[2].toLowerCase();
                for (String foodId : foodIds) {
                    if (foodId.startsWith(partial)) {
                        completions.add(foodId);
                    }
                }
            } else if (firstArg.equals("pet")) {
                // Suggest pet IDs
                List<String> petIds = Arrays.asList("kitty", "puppy", "horse", "pig", "rare-wolf");  // <-- Replace with your actual pet IDs
                String partial = args[2].toLowerCase();
                for (String petId : petIds) {
                    if (petId.startsWith(partial)) {
                        completions.add(petId);
                    }
                }
            } else if (firstArg.equals("item")) {
                // Suggest item IDs
                List<String> itemIds = Arrays.asList("ring-of-gods", "mysterious-amulet");  // <-- Replace with your actual item IDs
                String partial = args[2].toLowerCase();
                for (String itemId : itemIds) {
                    if (itemId.startsWith(partial)) {
                        completions.add(itemId);
                    }
                }
            } else if (firstArg.equals("bp")) {
                // Suggest backpack IDs
                List<String> backpackIds = Arrays.asList("small", "medium", "large");  // <-- Replace with your actual backpack IDs
                String partial = args[2].toLowerCase();
                for (String bpId : backpackIds) {
                    if (bpId.startsWith(partial)) {
                        completions.add(bpId);
                    }
                }
            }
        }
        // For args.length == 4, only "food" command has amount argument, you can add optional completions or leave empty

        return completions;
    }
}
