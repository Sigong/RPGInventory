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

// import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// import org.bukkit.Location;
// import org.bukkit.entity.LivingEntity;
// import org.bukkit.entity.Player;
// import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.utility.MinecraftReflection;

// import ru.endlesscode.rpginventory.inventory.InventoryManager;


/**
 * Created by OsipXD on 02.12.2015
 * It is part of the RpgInventory.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
public class EntityUtils {
    @SuppressWarnings("unused")
    private static Method craftEntity_getHandle;
    @SuppressWarnings("unused")
    private static Method navigationAbstract_a;
    @SuppressWarnings("unused")
    private static Method entityInsentient_getNavigation;
    private static final Class<?> entityInsentientClass = MinecraftReflection.getMinecraftClass("EntityInsentient");

    //private static final Reporter reporter = RPGInventory.getInstance().getReporter();

    static {
        try {
            craftEntity_getHandle = MinecraftReflection.getCraftEntityClass().getDeclaredMethod("getHandle");
            entityInsentient_getNavigation = entityInsentientClass.getDeclaredMethod("getNavigation");
            navigationAbstract_a = MinecraftReflection.getMinecraftClass("NavigationAbstract")
                    .getDeclaredMethod("a", double.class, double.class, double.class, double.class);
        } catch (NoSuchMethodException e) {
            //reporter.report("Error on EntityUtils initialization", e);
        }
    }
}
