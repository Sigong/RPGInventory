package ru.endlesscode.rpginventory.utils;

import java.util.UUID;

import org.bukkit.OfflinePlayer;

import io.lumine.mythic.lib.api.player.MMOPlayerData;

public class ProfileUtils {
    private ProfileUtils(){}

    /*
    Attempts to return the UUID of the player's currently selected profile. If it can't, returns the player's UUID.
    */
    public static UUID tryToGetProfileUUID(OfflinePlayer player){
        try{
            //UUID uuid = MMOPlayerData.get(player.getUniqueId()).getProfileId();
            //Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Got profile ID: " + uuid);
            return MMOPlayerData.get(player.getUniqueId()).getProfileId();
        }catch(NullPointerException e){
            // https://stackoverflow.com/questions/4065518/java-how-to-get-the-caller-function-name
//            StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
//            String methodName2 = stacktrace[2].getMethodName();
//            String className2 = stacktrace[2].getClassName();
//            String methodName3 = stacktrace[3].getMethodName();
//            String className3 = stacktrace[3].getClassName();
//            String methodName4 = stacktrace[4].getMethodName();
//            String className4 = stacktrace[4].getClassName();
            //Bukkit.getConsoleSender().sendMessage("Player " + player.getName() + " does not have a profile selected.");
//            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + e.getMessage());
//            Bukkit.getConsoleSender().sendMessage(className2 + "." + methodName2 + " <- " + className3 + "." + methodName3); //+ " <- " + className4 + "." + methodName4);

            return player.getUniqueId();
        }
    }
}
