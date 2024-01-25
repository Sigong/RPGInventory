package ru.endlesscode.rpginventory.event.listener;

import fr.phoenixdevt.profiles.event.ProfileSelectEvent;
import fr.phoenixdevt.profiles.event.ProfileUnloadEvent;
import io.lumine.mythic.lib.api.player.MMOPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.endlesscode.rpginventory.inventory.InventoryManager;

public class ProfileListener implements Listener{
    @EventHandler
    public void onProfileSelect(ProfileSelectEvent event) {
        //Bukkit.getConsoleSender().sendMessage("[RPGInventory] " + event.getPlayer().getName() + " selected profile " + event.getProfile().getUniqueId());
        InventoryManager.unloadPlayerInventory(event.getPlayer()); //First unload any inventories the player has loaded.
        InventoryManager.loadPlayerInventory(event.getPlayer()); //Then load the inventory associated with the profile.
    }

    @EventHandler
    public void onProfileUnload(ProfileUnloadEvent event){
        //Bukkit.getConsoleSender().sendMessage("[RPGInventory] " + event.getPlayer().getName() + " unloaded profile.");
        InventoryManager.unloadPlayerInventory(event.getPlayer());
    }
}
