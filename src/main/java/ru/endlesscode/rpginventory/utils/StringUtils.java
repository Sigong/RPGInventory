/*
 * This file is part of RPGInventory.
 * Copyright (C) 2015-2017 Osip Fatkullin
 *
 * RPGInventory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RPGInventory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RPGInventory.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.endlesscode.rpginventory.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
//import org.bukkit.ChatColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.endlesscode.rpginventory.RPGInventory;
import ru.endlesscode.rpginventory.inventory.InventoryManager;
import ru.endlesscode.rpginventory.item.ItemManager;
import ru.endlesscode.rpginventory.item.ItemStat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by OsipXD on 29.08.2015
 * It is part of the RpgInventory.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
public class StringUtils {

    // Regex Pattern for a RGB hex color code prepended with an &
    private static final String pattern = "&#[a-fA-F0-9]{6}";

    // Returns the string with each group of characters that matches the pattern replaced with
    // the corresponding hex ColorCode
    private static String interpretHex(String str){
        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(str);

        ArrayList<String> pieces = new ArrayList<>();

        // For every match, add all non-matching characters between this match
        // and the last match (or beginning of the string), then add the match
        // with the ampersand (&) removed.
        int idx = 0;
        while(matcher.find()){
            int start = matcher.start();
            if(idx != start){
                pieces.add(str.substring(idx, start));
            }
            pieces.add(str.substring(start+1, start+8));
            idx = start+8;
        }

        // Add the final non-matching section of the string if it exists.
        if(idx != str.length()){
            pieces.add(str.substring(idx));
        }

        // Replace each hex color code String in the ArrayList with a ChatColor
        for(int i = 0; i < pieces.size() ; i++){
            if(pieces.get(i).matches(pattern.substring(1))){
                pieces.set(i, "" + ChatColor.of(pieces.get(i)));
            }
        }

        return String.join("", pieces);
    }

    @NotNull
    public static String coloredLine(@NotNull String line) {
        //Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "CALLED COLORED LINE METHOD FOR STRING" + line);
        if(Pattern.compile(pattern).matcher(line).find()){
            //Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "LINE MATCHES HEX PATTERN");
            line = interpretHex(line);
        }
        return ChatColor.translateAlternateColorCodes('&', line);
    }

    @NotNull
    public static List<String> coloredLines(@Nullable List<String> lines) {
        if (lines == null) {
            return Collections.emptyList();
        }

        List<String> coloredLines = new ArrayList<>(lines.size());
        for (String line : lines) {
            coloredLines.add(StringUtils.coloredLine(line));
        }

        return coloredLines;
    }

    public static String doubleToString(double value) {
        return value == (long) value ? String.format("%d", (long) value) : String.format("%s", value);
    }

    public static void coloredConsole(String message) {
        Bukkit.getServer().getConsoleSender().sendMessage(message);
    }

    @NotNull
    public static String setPlaceholders(@NotNull Player player, @NotNull String line) {
        if (RPGInventory.isPlaceholderApiHooked()) {
            try {
                return PlaceholderAPI.setPlaceholders(player, line);
            } catch (Throwable e) {
                Log.w("Can''t set placeholders for line \"{0}\"", line);
                Log.w("Make sure that you use the latest version of PlaceholderAPI.");
                Log.w(e, "If it is, please report about the error to PlaceholderAPI author:");
                return line;
            }
        }

        return line;
    }

    public static class Placeholders extends PlaceholderExpansion {

        private static Placeholders instance = null;

        public static void registerPlaceholders() {
            instance = new Placeholders();
            instance.register();
        }

        public static void unregisterPlaceholders() {
            if (instance != null) {
                instance.unregister();
            }
        }

        private Placeholders() {
            // Make constructor private
        }

        @Override
        public @NotNull String getIdentifier() {
            return "rpginv";
        }

        @Override
        public @NotNull String getAuthor() {
            return "osipxd";
        }

        @Override
        public @NotNull String getVersion() {
            return "1.0";
        }

        @Override
        public boolean canRegister() {
            return true;
        }

        @Override
        public String onPlaceholderRequest(@Nullable Player player, @NotNull String identifier) {
            if (!InventoryManager.playerIsLoaded(player)) {
                return null;
            }

            switch (identifier) {
                case "damage_bonus":
                    return ItemManager.getModifier(player, ItemStat.StatType.DAMAGE).toString();
                case "bow_damage_bonus":
                    return ItemManager.getModifier(player, ItemStat.StatType.BOW_DAMAGE).toString();
                case "hand_damage_bonus":
                    return ItemManager.getModifier(player, ItemStat.StatType.HAND_DAMAGE).toString();
                case "crit_damage_bonus":
                    return ItemManager.getModifier(player, ItemStat.StatType.CRIT_DAMAGE).toString();
                case "crit_chance":
                    return ItemManager.getModifier(player, ItemStat.StatType.CRIT_CHANCE).toString();
                case "armor_bonus":
                    return ItemManager.getModifier(player, ItemStat.StatType.ARMOR).toString();
                case "speed_bonus":
                    return ItemManager.getModifier(player, ItemStat.StatType.SPEED).toString();
                case "jump_bonus":
                    return ItemManager.getModifier(player, ItemStat.StatType.JUMP).toString();
            }

            return null;
        }
    }
}
