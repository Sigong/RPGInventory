package ru.endlesscode.rpginventory.utils;

import java.util.UUID;

/**
 * Small holder for attributes/constants previously stored in pet.Attributes.
 */
public final class Attributes {
    private Attributes() {}

    // Unique id used to mark the plugin's movement speed modifier
    public static final UUID SPEED_MODIFIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final String SPEED_MODIFIER = "RPGInventory Speed";
}
