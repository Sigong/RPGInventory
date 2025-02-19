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

package ru.endlesscode.rpginventory.inventory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.endlesscode.inspector.report.Reporter;
import ru.endlesscode.rpginventory.RPGInventory;
import ru.endlesscode.rpginventory.api.InventoryAPI;
import ru.endlesscode.rpginventory.event.PetEquipEvent;
import ru.endlesscode.rpginventory.event.PetUnequipEvent;
import ru.endlesscode.rpginventory.event.PlayerInventoryLoadEvent;
import ru.endlesscode.rpginventory.event.PlayerInventoryUnloadEvent;
import ru.endlesscode.rpginventory.event.listener.InventoryListener;
import ru.endlesscode.rpginventory.inventory.slot.Slot;
import ru.endlesscode.rpginventory.inventory.slot.SlotManager;
import ru.endlesscode.rpginventory.item.ItemManager;
import ru.endlesscode.rpginventory.item.Texture;
import ru.endlesscode.rpginventory.misc.config.Config;
import ru.endlesscode.rpginventory.misc.serialization.Serialization;
import ru.endlesscode.rpginventory.pet.PetManager;
import ru.endlesscode.rpginventory.pet.PetType;
import ru.endlesscode.rpginventory.resourcepack.ResourcePackModule;
import ru.endlesscode.rpginventory.utils.EffectUtils;
import ru.endlesscode.rpginventory.utils.InventoryUtils;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import ru.endlesscode.rpginventory.utils.Log;
import ru.endlesscode.rpginventory.utils.PlayerUtils;
import ru.endlesscode.rpginventory.utils.ProfileUtils;
import ru.endlesscode.rpginventory.utils.SafeEnums;
import ru.endlesscode.rpginventory.utils.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryManager {
    static final String TITLE = RPGInventory.getLanguage().getMessage("title");
    private static final Map<UUID, PlayerWrapper> INVENTORIES = new HashMap<>();

    private static ItemStack FILL_SLOT = null;
    //private static Reporter reporter;

    private InventoryManager() {
    }

    public static boolean init(@NotNull RPGInventory instance) {
        //reporter = instance.getReporter();

        try {
            Texture texture = Texture.parseTexture(Config.getConfig().getString("fill"));
            InventoryManager.FILL_SLOT = texture.getItemStack();
            ItemMeta meta = InventoryManager.FILL_SLOT.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                InventoryManager.FILL_SLOT.setItemMeta(meta);
            }
        } catch (Exception e) {
            //reporter.report("Error on InventoryManager initialization", e);
            return false;
        }

        // Register events
        instance.getServer().getPluginManager().registerEvents(new InventoryListener(), instance);
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean validateUpdate(Player player, ActionType actionType, @NotNull Slot slot, ItemStack item) {
        return actionType == ActionType.GET || actionType == ActionType.DROP
                || actionType == ActionType.SET && slot.isValidItem(item)
                && ItemManager.allowedForPlayer(player, item, true);
    }

    @NotNull
    public static ItemStack getFillSlot() {
        return InventoryManager.FILL_SLOT;
    }

    public static boolean validatePet(Player player, InventoryAction action, @Nullable ItemStack currentItem, @NotNull ItemStack cursor) {
        ActionType actionType = ActionType.getTypeOfAction(action);

        if (ItemUtils.isNotEmpty(currentItem)
                && (actionType == ActionType.GET || action == InventoryAction.SWAP_WITH_CURSOR || actionType == ActionType.DROP)
                && PetManager.getCooldown(currentItem) > 0) {
            return false;
        }

        if (actionType == ActionType.SET) {
            if (PetType.isPetItem(cursor) && ItemManager.allowedForPlayer(player, cursor, true)) {
                PetEquipEvent event = new PetEquipEvent(player, cursor);
                RPGInventory.getInstance().getServer().getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return false;
                }

                PetManager.respawnPet(event.getPlayer(), event.getPetItem());
                return true;
            }
        } else if (actionType == ActionType.GET || actionType == ActionType.DROP) {
            PetUnequipEvent event = new PetUnequipEvent(player);
            RPGInventory.getInstance().getServer().getPluginManager().callEvent(event);
            PetManager.despawnPet(event.getPlayer());
            return true;
        }

        return false;
    }

    public static boolean validateArmor(Player player, InventoryAction action, @NotNull Slot slot, ItemStack item) {
        ActionType actionType = ActionType.getTypeOfAction(action);
        return actionType != ActionType.OTHER && (actionType != ActionType.SET || slot.isValidItem(item))
                && ItemManager.allowedForPlayer(player, item, true);
    }

    public static void updateShieldSlot(@NotNull Player player, @NotNull Inventory inventory, @NotNull Slot slot, int slotId,
                                        InventoryType.SlotType slotType, InventoryAction action,
                                        ItemStack currentItem, @NotNull ItemStack cursor) {
        ActionType actionType = ActionType.getTypeOfAction(action);
        if (actionType == ActionType.GET) {
            if (slot.isCup(currentItem)) {
                return;
            }

            if (slotType == InventoryType.SlotType.QUICKBAR && InventoryAPI.isRPGInventory(inventory)) {
                inventory.setItem(slot.getSlotId(), slot.getCup());
            } else {
                player.getEquipment().setItemInOffHand(new ItemStack(Material.AIR));
            }
        } else if (actionType == ActionType.SET) {
            if (slot.isCup(currentItem)) {
                currentItem = null;
                action = InventoryAction.PLACE_ALL;
            }

            if (slotType == InventoryType.SlotType.QUICKBAR && InventoryAPI.isRPGInventory(inventory)) {
                inventory.setItem(slot.getSlotId(), cursor);
            } else {
                player.getEquipment().setItemInOffHand(cursor);
            }
        }

        InventoryManager.updateInventory(player, inventory, slotId, slotType, action, currentItem, cursor);
    }

    public static void updateQuickSlot(@NotNull Player player, @NotNull Inventory inventory, @NotNull Slot slot, int slotId,
                                       InventoryType.SlotType slotType, InventoryAction action,
                                       ItemStack currentItem, ItemStack cursor) {
        ActionType actionType = ActionType.getTypeOfAction(action);
        if (actionType == ActionType.GET) {
            if (slot.isCup(currentItem)) {
                return;
            }

            if (player.getInventory().getHeldItemSlot() == slot.getQuickSlot()) {
                InventoryUtils.heldFreeSlot(player, slot.getQuickSlot(), InventoryUtils.SearchType.NEXT);
            }

            if (slotType == InventoryType.SlotType.QUICKBAR && InventoryAPI.isRPGInventory(inventory)) {
                inventory.setItem(slot.getSlotId(), slot.getCup());
            } else {
                player.getInventory().setItem(slot.getQuickSlot(), slot.getCup());
            }

            action = InventoryAction.SWAP_WITH_CURSOR;
            cursor = slot.getCup();
        } else if (actionType == ActionType.SET) {
            if (slot.isCup(currentItem)) {
                currentItem = null;
                action = InventoryAction.PLACE_ALL;
            }

            if (slotType == InventoryType.SlotType.QUICKBAR && InventoryAPI.isRPGInventory(inventory)) {
                inventory.setItem(slot.getSlotId(), cursor);
            } else {
                player.getInventory().setItem(slot.getQuickSlot(), cursor);
            }
        }

        InventoryManager.updateInventory(player, inventory, slotId, slotType, action, currentItem, cursor);
    }

    public static void updateArmor(@NotNull Player player, @NotNull Inventory inventory, @NotNull Slot slot, int slotId, InventoryAction action, ItemStack currentItem, @NotNull ItemStack cursor) {
        ActionType actionType = ActionType.getTypeOfAction(action);

        EntityEquipment equipment = player.getEquipment();
        assert equipment != null;

        // Equip armor
        if (actionType == ActionType.SET || action == InventoryAction.UNKNOWN) {
            switch (slot.getName()) {
                case "helmet":
                    InventoryManager.updateInventory(player, inventory, slotId, action, currentItem, cursor);
                    equipment.setHelmet(inventory.getItem(slotId));
                    break;
                case "chestplate":
                    InventoryManager.updateInventory(player, inventory, slotId, action, currentItem, cursor);
                    equipment.setChestplate(inventory.getItem(slotId));
                    break;
                case "leggings":
                    InventoryManager.updateInventory(player, inventory, slotId, action, currentItem, cursor);
                    equipment.setLeggings(inventory.getItem(slotId));
                    break;
                case "boots":
                    InventoryManager.updateInventory(player, inventory, slotId, action, currentItem, cursor);
                    equipment.setBoots(inventory.getItem(slotId));
                    break;
            }
        } else if (actionType == ActionType.GET || actionType == ActionType.DROP) { // Unequip armor
            InventoryManager.updateInventory(player, inventory, slotId, action, currentItem, cursor);

            switch (slot.getName()) {
                case "helmet":
                    equipment.setHelmet(null);
                    break;
                case "chestplate":
                    equipment.setChestplate(null);
                    break;
                case "leggings":
                    equipment.setLeggings(null);
                    break;
                case "boots":
                    equipment.setBoots(null);
                    break;
            }
        }
    }

    public static void syncArmor(PlayerWrapper playerWrapper) {
        Player player = (Player) playerWrapper.getPlayer();
        Inventory inventory = playerWrapper.getInventory();
        SlotManager sm = SlotManager.instance();
        EntityEquipment equipment = player.getEquipment();
        assert equipment != null;

        if (ArmorType.HELMET.getSlot() != -1) {
            ItemStack helmet = equipment.getHelmet();
            Slot helmetSlot = sm.getSlot(ArmorType.HELMET.getSlot(), InventoryType.SlotType.CONTAINER);
            inventory.setItem(ArmorType.HELMET.getSlot(), (ItemUtils.isEmpty(helmet))
                    && helmetSlot != null ? helmetSlot.getCup() : helmet);
        }

        if (ArmorType.CHESTPLATE.getSlot() != -1) {
            ItemStack savedChestplate = playerWrapper.getSavedChestplate();
            ItemStack chestplate = savedChestplate == null ? equipment.getChestplate() : savedChestplate;
            Slot chestplateSlot = sm.getSlot(ArmorType.CHESTPLATE.getSlot(), InventoryType.SlotType.CONTAINER);
            inventory.setItem(ArmorType.CHESTPLATE.getSlot(), (ItemUtils.isEmpty(chestplate))
                    && chestplateSlot != null ? chestplateSlot.getCup() : chestplate);
        }

        if (ArmorType.LEGGINGS.getSlot() != -1) {
            ItemStack leggings = equipment.getLeggings();
            Slot leggingsSlot = sm.getSlot(ArmorType.LEGGINGS.getSlot(), InventoryType.SlotType.CONTAINER);
            inventory.setItem(ArmorType.LEGGINGS.getSlot(), (ItemUtils.isEmpty(leggings))
                    && leggingsSlot != null ? leggingsSlot.getCup() : leggings);
        }

        if (ArmorType.BOOTS.getSlot() != -1) {
            ItemStack boots = equipment.getBoots();
            Slot bootsSlot = sm.getSlot(ArmorType.BOOTS.getSlot(), InventoryType.SlotType.CONTAINER);
            inventory.setItem(ArmorType.BOOTS.getSlot(), (ItemUtils.isEmpty(boots))
                    && bootsSlot != null ? bootsSlot.getCup() : boots);
        }
    }

    public static void syncQuickSlots(PlayerWrapper playerWrapper) {
        Player player = (Player) playerWrapper.getPlayer();
        for (Slot quickSlot : SlotManager.instance().getQuickSlots()) {
            playerWrapper.getInventory().setItem(quickSlot.getSlotId(), player.getInventory().getItem(quickSlot.getQuickSlot()));
        }
    }

    public static void syncInfoSlots(PlayerWrapper playerWrapper) {
        final Player player = (Player) playerWrapper.getPlayer();
        for (Slot infoSlot : SlotManager.instance().getInfoSlots()) {
            ItemStack cup = infoSlot.getCup();
            if (ItemUtils.isEmpty(cup)) {
                continue;
            }

            ItemMeta meta = cup.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore();
                if (lore == null) {
                    lore = new ArrayList<>();
                }

                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i);
                    lore.set(i, StringUtils.setPlaceholders(player, line));
                }

                meta.setLore(lore);
                cup.setItemMeta(meta);
            }
            playerWrapper.getInventory().setItem(infoSlot.getSlotId(), cup);
        }

        player.updateInventory();
    }

    public static void syncShieldSlot(@NotNull PlayerWrapper playerWrapper) {
        Slot slot = SlotManager.instance().getShieldSlot();
        if (slot == null) {
            return;
        }

        Player player = (Player) playerWrapper.getPlayer();
        ItemStack itemInHand = player.getEquipment().getItemInOffHand();
        playerWrapper.getInventory().setItem(slot.getSlotId(), ItemUtils.isEmpty(itemInHand) ? slot.getCup() : itemInHand);
    }

    private static void updateInventory(
            @NotNull Player player,
            @NotNull Inventory inventory,
            int slot,
            InventoryAction action,
            ItemStack currentItem,
            @NotNull ItemStack cursor
    ) {
        InventoryManager.updateInventory(player, inventory, slot, InventoryType.SlotType.CONTAINER, action, currentItem, cursor);
    }

    private static void updateInventory(
            @NotNull Player player,
            @NotNull Inventory inventory,
            int slot,
            InventoryType.SlotType slotType,
            InventoryAction action,
            ItemStack currentItem,
            @NotNull ItemStack cursorItem
    ) {
        if (ActionType.getTypeOfAction(action) == ActionType.DROP) {
            return;
        }

        if (action == InventoryAction.PLACE_ALL) {
            if (ItemUtils.isEmpty(currentItem)) {
                currentItem = cursorItem.clone();
            } else {
                currentItem.setAmount(currentItem.getAmount() + cursorItem.getAmount());
            }

            cursorItem = null;
        } else if (action == InventoryAction.PLACE_ONE) {
            if (ItemUtils.isEmpty(currentItem)) {
                currentItem = cursorItem.clone();
                currentItem.setAmount(1);
                cursorItem.setAmount(cursorItem.getAmount() - 1);
            } else if (currentItem.getMaxStackSize() < currentItem.getAmount() + 1) {
                currentItem.setAmount(currentItem.getAmount() + 1);
                cursorItem.setAmount(cursorItem.getAmount() - 1);
            }
        } else if (action == InventoryAction.PLACE_SOME) {
            cursorItem.setAmount(currentItem.getMaxStackSize() - currentItem.getAmount());
            currentItem.setAmount(currentItem.getMaxStackSize());
        } else if (action == InventoryAction.SWAP_WITH_CURSOR) {
            ItemStack tempItem = cursorItem.clone();
            cursorItem = currentItem.clone();
            currentItem = tempItem;
        } else if (action == InventoryAction.PICKUP_ALL) {
            cursorItem = currentItem.clone();
            currentItem = null;
        } else if (action == InventoryAction.PICKUP_HALF) {
            ItemStack item = currentItem.clone();
            if (currentItem.getAmount() % 2 == 0) {
                item.setAmount(item.getAmount() / 2);
                currentItem = item.clone();
                cursorItem = item.clone();
            } else {
                currentItem = item.clone();
                currentItem.setAmount(item.getAmount() / 2);
                cursorItem = item.clone();
                cursorItem.setAmount(item.getAmount() / 2 + 1);
            }
        } else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            player.getInventory().addItem(currentItem);
            currentItem = null;
        }

        if (slotType == InventoryType.SlotType.QUICKBAR) {
            if (slot < 9) { // Exclude shield
                player.getInventory().setItem(slot, currentItem);
            }
        } else {
            inventory.setItem(slot, currentItem);
        }

        player.setItemOnCursor(cursorItem);
        player.updateInventory();
    }

    static void lockEmptySlots(Player player) {
        lockEmptySlots(INVENTORIES.get(ProfileUtils.tryToGetProfileUUID(player)).getInventory());
    }

    public static void lockEmptySlots(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            Slot slot = SlotManager.instance().getSlot(i, InventoryType.SlotType.CONTAINER);
            if (slot == null) {
                inventory.setItem(i, FILL_SLOT);
            } else if (ItemUtils.isEmpty(inventory.getItem(i))) {
                inventory.setItem(i, slot.getCup());
            }
        }
    }

    static void unlockEmptySlots(UUID uuid) {
        Inventory inventory = INVENTORIES.get(uuid).getInventory();

        for (int i = 0; i < inventory.getSize(); i++) {
            Slot slot = SlotManager.instance().getSlot(i, InventoryType.SlotType.CONTAINER);
            if (slot == null || slot.isCup(inventory.getItem(i))) {
                inventory.setItem(i, null);
            }
        }
    }

    public static boolean isQuickSlot(int slot) {
        return getQuickSlot(slot) != null;
    }

    @Nullable
    public static Slot getQuickSlot(int slot) {
        for (Slot quickSlot : SlotManager.instance().getQuickSlots()) {
            if (slot == quickSlot.getQuickSlot()) {
                return quickSlot;
            }
        }

        return null;
    }

    static void lockQuickSlots(@NotNull Player player) {
        for (Slot quickSlot : SlotManager.instance().getQuickSlots()) {
            int slotId = quickSlot.getQuickSlot();

            if (ItemUtils.isEmpty(player.getInventory().getItem(slotId))) {
                player.getInventory().setItem(slotId, quickSlot.getCup());
            }

            if (player.getInventory().getHeldItemSlot() == slotId) {
                if (quickSlot.isCup(player.getInventory().getItem(slotId))) {
                    InventoryUtils.heldFreeSlot(player, slotId, InventoryUtils.SearchType.NEXT);
                }
            }
        }
    }

    static void unlockQuickSlots(@NotNull Player player) {
        for (Slot quickSlot : SlotManager.instance().getQuickSlots()) {
            int slotId = quickSlot.getQuickSlot();
            if (quickSlot.isCup(player.getInventory().getItem(slotId))) {
                player.getInventory().setItem(slotId, null);
            }
        }
    }

    public static boolean isNewPlayer(@NotNull Player player) {
        Path dataFolder = RPGInventory.getInstance().getDataFolder().toPath();
        return Files.notExists(dataFolder.resolve("inventories/" + ProfileUtils.tryToGetProfileUUID(player) + ".inv"));
    }

    public static void loadPlayerInventory(Player player) {
        if (!InventoryManager.isAllowedWorld(player.getWorld())) {
            InventoryManager.INVENTORIES.remove(ProfileUtils.tryToGetProfileUUID(player));
            return;
        }

        try {
            Path dataFolder = RPGInventory.getInstance().getDataPath();
            Path folder = dataFolder.resolve("inventories");
            Files.createDirectories(folder);

            // Load inventory from file
            Path file = folder.resolve(ProfileUtils.tryToGetProfileUUID(player) + ".inv");

            PlayerWrapper playerWrapper = null;
            if (Files.exists(file)) {
                playerWrapper = Serialization.loadPlayerOrNull(player, file);
                if (playerWrapper == null) {
                    Log.s("Error on loading {0}''s inventory.", player.getName());
                    Log.s("Will be created new inventory. Old file was renamed.");
                }
            }

            if (playerWrapper == null) {
                playerWrapper = new PlayerWrapper(player);
                playerWrapper.setBuyedSlots(0);
            }

            PlayerInventoryLoadEvent.Pre event = new PlayerInventoryLoadEvent.Pre(player);
            RPGInventory.getInstance().getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            InventoryManager.INVENTORIES.put(ProfileUtils.tryToGetProfileUUID(player), playerWrapper);

            InventoryLocker.lockSlots(player);
            PetManager.initPlayer(player);

            RPGInventory.getInstance().getServer().getPluginManager().callEvent(new PlayerInventoryLoadEvent.Post(player));
        } catch (IOException e) {
            //reporter.report("Error on inventory load", e);
        }
    }

    // This version of the method first unloads the player's base inventory, then unloads the profile inventory if it exists.
    public static void unloadPlayerInventory(@NotNull Player player) {
        boolean unloadedBaseInventory = false;
        if(InventoryManager.uuidIsLoaded(player.getUniqueId())){
            unloadUUIDInventory(player, player.getUniqueId());
            unloadedBaseInventory = true;
        }
        boolean unloadedProfileInventory = false;
        // The below code won't run if there is no profile selected because the utility method will return the base UUID
        if(InventoryManager.uuidIsLoaded(ProfileUtils.tryToGetProfileUUID(player))){
            unloadUUIDInventory(player, ProfileUtils.tryToGetProfileUUID(player));
            unloadedProfileInventory = true;
        }
        if(unloadedBaseInventory && unloadedProfileInventory){
            Bukkit.getLogger().warning("[RPG Inventory] Base and profile inventory were unloaded for a single player, this should not occur.");
        }
    }

    private static void unloadUUIDInventory(@NotNull Player player, @NotNull UUID uuid){


        InventoryManager.INVENTORIES.get(uuid).onUnload();
        saveUUIDInventory(uuid);
        InventoryLocker.unlockSlots(player, uuid);

        InventoryManager.INVENTORIES.remove(uuid);

        RPGInventory.getInstance().getServer().getPluginManager().callEvent(new PlayerInventoryUnloadEvent.Post(player));
    }

//    public static void unloadPlayerInventory(@NotNull Player player, boolean unloadBaseInventory) {
//        if (!InventoryManager.playerIsLoaded(player)) {
//            return;
//        }
//
//        InventoryManager.INVENTORIES.get(ProfileUtils.tryToGetProfileUUID(player)).onUnload();
//        savePlayerInventory(player);
//        InventoryLocker.unlockSlots(player);
//
//        InventoryManager.INVENTORIES.remove(ProfileUtils.tryToGetProfileUUID(player));
//
//        RPGInventory.getInstance().getServer().getPluginManager().callEvent(new PlayerInventoryUnloadEvent.Post(player));
//    }

    public static void saveUUIDInventory(@NotNull UUID uuid){
        if (!InventoryManager.uuidIsLoaded(uuid)) {
            return;
        }

        PlayerWrapper playerWrapper = InventoryManager.INVENTORIES.get(uuid);
        try {
            Path dataFolder = RPGInventory.getInstance().getDataPath();
            Path folder = dataFolder.resolve("inventories");
            Files.createDirectories(folder);

            Path file = folder.resolve(uuid + ".inv");
            Files.deleteIfExists(file);

            Serialization.save(playerWrapper.createSnapshot(), file);
        } catch (IOException | NullPointerException e) {
            Log.w(e, "Error on inventory save");
        }
    }

//    public static void savePlayerInventory(@NotNull Player player) {
//        if (!InventoryManager.playerIsLoaded(player)) {
//            return;
//        }
//
//        PlayerWrapper playerWrapper = InventoryManager.INVENTORIES.get(ProfileUtils.tryToGetProfileUUID(player));
//        try {
//            Path dataFolder = RPGInventory.getInstance().getDataPath();
//            Path folder = dataFolder.resolve("inventories");
//            Files.createDirectories(folder);
//
//            Path file = folder.resolve(ProfileUtils.tryToGetProfileUUID(player) + ".inv");
//            Files.deleteIfExists(file);
//
//            Serialization.save(playerWrapper.createSnapshot(), file);
//        } catch (IOException | NullPointerException e) {
//            Log.w(e, "Error on inventory save");
//        }
//    }

    @NotNull
    public static PlayerWrapper get(@NotNull OfflinePlayer player) {
        PlayerWrapper playerWrapper = InventoryManager.INVENTORIES.get(ProfileUtils.tryToGetProfileUUID(player));
        if (playerWrapper == null) {
            throw new IllegalStateException(player.getName() + "'s inventory should be initialized!");
        }

        return playerWrapper;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isQuickEmptySlot(@Nullable ItemStack item) {
        for (Slot quickSlot : SlotManager.instance().getQuickSlots()) {
            if (quickSlot.isCup(item)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isFilledSlot(@Nullable ItemStack item) {
        return InventoryManager.FILL_SLOT.equals(item);
    }

    public static boolean isEmptySlot(@Nullable ItemStack item) {
        for (Slot slot : SlotManager.instance().getSlots()) {
            if (slot.isCup(item)) {
                return true;
            }
        }

        return false;
    }

    // Same functionality as below method, but allows caller to pass in UUID to check.
    // Added to allow for direct profile uuid checking without modifying too much other code.
    public static boolean uuidIsLoaded(@Nullable UUID uuid){
        return uuid != null && InventoryManager.INVENTORIES.containsKey(uuid);
    }

    @Contract("null -> false")
    public static boolean playerIsLoaded(@Nullable AnimalTamer player) {
        return player != null && InventoryManager.INVENTORIES.containsKey(ProfileUtils.tryToGetProfileUUID((Player) player));
    }

    public static boolean isAllowedWorld(@NotNull World world) {
        List<String> list = Config.getConfig().getStringList("worlds.list");

        ListType listType = SafeEnums.valueOf(ListType.class, Config.getConfig().getString("worlds.mode"), "list type");
        if (listType != null) {
            switch (listType) {
                case BLACKLIST:
                    return !list.contains(world.getName());
                case WHITELIST:
                    return list.contains(world.getName());
            }
        }

        return true;
    }

    public static boolean buySlot(@NotNull Player player, PlayerWrapper playerWrapper, Slot slot) {
        double cost = slot.getCost();

        if (!playerWrapper.isPreparedToBuy(slot)) {
            PlayerUtils.sendMessage(player, RPGInventory.getLanguage().getMessage("error.buyable", slot.getCost()));
            playerWrapper.prepareToBuy(slot);
            return false;
        }

        if (!PlayerUtils.checkMoney(player, cost) || !RPGInventory.getEconomy().withdrawPlayer(player, cost).transactionSuccess()) {
            return false;
        }

        playerWrapper.setBuyedSlots(slot.getName());
        PlayerUtils.sendMessage(player, RPGInventory.getLanguage().getMessage("message.buyed"));

        return true;
    }

    public static void initPlayer(@NotNull final Player player, boolean skipJoinMessage) {
        ResourcePackModule rpModule = RPGInventory.getResourcePackModule();
        if (rpModule != null) {
            rpModule.loadResourcePack(player, skipJoinMessage);
        } else {
            if (!skipJoinMessage) {
                EffectUtils.showDefaultJoinMessage(player);
            }
            InventoryManager.loadPlayerInventory(player);
        }

        if (RPGInventory.getPermissions().has(player, "rpginventory.admin")) {
            RPGInventory.getInstance().checkUpdates(player);
        }
    }

    private enum ListType {
        BLACKLIST, WHITELIST
    }
}
