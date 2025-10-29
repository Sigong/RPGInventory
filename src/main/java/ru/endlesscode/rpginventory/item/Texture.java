package ru.endlesscode.rpginventory.item;

import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;
import ru.endlesscode.rpginventory.compat.MaterialCompat;
import ru.endlesscode.rpginventory.misc.config.Config;
import ru.endlesscode.rpginventory.misc.config.TexturesType;
import ru.endlesscode.rpginventory.utils.ItemUtils;
import ru.endlesscode.rpginventory.utils.Log;
import ru.endlesscode.rpginventory.utils.NbtFactoryMirror;

import java.util.Objects;

public class Texture {

    private static final Texture EMPTY_TEXTURE = new Texture(new ItemStack(Material.AIR));

    @NotNull
    private final ItemStack prototype;
    private final int data;

    private Texture(@NotNull ItemStack prototype) {
        this(prototype, (short) -1);
    }

    private Texture(@NotNull ItemStack prototype, int data) {
        this.prototype = prototype;
        this.data = data;
    }

    public boolean isEmpty() {
        return this.equals(EMPTY_TEXTURE);
    }

    @NotNull
    public ItemStack getItemStack() {
        return prototype.clone();
    }

    public int getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Texture)) {
            return false;
        }

        Texture texture = (Texture) o;
        return prototype.equals(texture.prototype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prototype);
    }

    public static Texture parseTexture(String texture) {
        if (texture == null) {
            return EMPTY_TEXTURE;
        }

        String[] textureParts = texture.split(":");

        Material material = MaterialCompat.getMaterialOrNull(textureParts[0]);
        if (material == null) {
            Log.w("Unknown material: {0}", textureParts[0]);
            return EMPTY_TEXTURE;
        }

        ItemStack item = ItemUtils.toBukkitItemStack(new ItemStack(material));
        if (ItemUtils.isEmpty(item)) {
            return EMPTY_TEXTURE;
        }

        if (textureParts.length > 1) {
            // MONSTER_EGG before 1.13
            if (material.name().equals("MONSTER_EGG")) {
                return parseLegacyMonsterEgg(item, textureParts[1]);
            } else if (material.name().startsWith("LEATHER_")) {
                return parseLeatherArmor(item, textureParts[1]);
            } else {
                return parseItemWithData(item, textureParts[1]);
            }
        }

        return new Texture(item);
    }

    private static Texture parseLegacyMonsterEgg(ItemStack item, String entityType) {
        NbtCompound nbt = NbtFactoryMirror.fromItemCompound(item);
        if (nbt == null) {
            nbt = NbtFactory.ofCompound("tag");
            NbtFactoryMirror.setItemTag(item, nbt);
        }
        nbt.put(ItemUtils.ENTITY_TAG, NbtFactory.ofCompound("temp").put("id", entityType));

        return new Texture(item);
    }

    private static Texture parseLeatherArmor(ItemStack item, String hexColor) {
        try {
            LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
            assert meta != null;
            meta.setColor(Color.fromRGB(Integer.parseInt(hexColor, 16)));
            item.setItemMeta(meta);
        } catch (ClassCastException | IllegalArgumentException | NullPointerException e) {
            Log.w("Can''t parse leather color: {0}", e.toString());
        }

        return new Texture(item);
    }

    private static Texture parseItemWithData(ItemStack item, String textureDataValue) {
        if (item.getItemMeta() == null) {
            return new Texture(item);
        }

        // Try to parse as a number first for legacy support
        try {
            int textureData = Integer.parseInt(textureDataValue);
            if (Config.texturesType == TexturesType.DAMAGE) {
                return parseItemWithDurability(item, textureData);
            }
            return parseItemWithCustomModelData(item, textureData);
        } catch (NumberFormatException e) {
            // Not a number, handle as an identifier
            if (Config.texturesType != TexturesType.NAMED_IDENTIFIER) {
                Log.w("Can''t parse texture modifier. For numeric values, specify a number. For named identifiers, set textures-type: named_identifier in config.yml. Value: \"{0}\"", textureDataValue);
                return new Texture(item);
            }

            // Handle different formats for named identifiers
            if (!textureDataValue.contains(":") && !textureDataValue.contains("/")) {
                // Simple format: just the variant name
                // Use the item type as the base identifier
                String baseItem = item.getType().getKey().toString();
                return parseItemWithNamedIdentifier(item, baseItem + "/" + textureDataValue);
            }

            // Full format: either namespace:id, namespace:id/variant, or id/variant
            return parseItemWithNamedIdentifier(item, textureDataValue);
        }
    }

    private static Texture parseItemWithDurability(ItemStack item, int damage) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        meta.addItemFlags(ItemFlag.values());
        if (damage != -1) {
            ((Damageable) meta).setDamage(damage);
            if (ItemUtils.isItemHasDurability(item)) {
                meta.setUnbreakable(true);
            }
        }
        item.setItemMeta(meta);

        return new Texture(item, damage);
    }

    private static Texture parseItemWithCustomModelData(ItemStack item, int customModelData) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        meta.addItemFlags(ItemFlag.values());
        if (customModelData != -1) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);

        return new Texture(item, customModelData);
    }

    private static Texture parseItemWithNamedIdentifier(ItemStack item, String identifier) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        meta.addItemFlags(ItemFlag.values());
        
        // Set the custom identifier using NBT
        NbtCompound nbt = NbtFactoryMirror.fromItemCompound(item);
        if (nbt == null) {
            nbt = NbtFactory.ofCompound("tag");
        }

        // Parse the identifier (format can be: namespace:id, namespace:id/variant, or just id)
        String namespace = "minecraft";
        String id = identifier;
        String variant = null;

        if (identifier.contains(":")) {
            String[] parts = identifier.split(":");
            namespace = parts[0];
            id = parts[1];
        }
        
        if (id.contains("/")) {
            String[] parts = id.split("/");
            id = parts[0];
            variant = parts[1];
        }

        // Support for CraftEngine/ModelEngine format
        if (namespace.equals("craftengine") || namespace.equals("modelengine")) {
            nbt.put("MODELENGINE", variant != null ? variant : id);
            if (variant != null) {
                // ModelEngine uses CustomModelData for variants
                meta.setCustomModelData(calculateModelData(variant));
            }
        } else {
            // Standard format
            String fullId = namespace + ":" + id + (variant != null ? "/" + variant : "");
            nbt.put("custom_item", fullId);
            
            // Support for generic resource pack custom models
            if (variant != null) {
                meta.setCustomModelData(calculateModelData(variant));
            }
        }

        item.setItemMeta(meta);
        NbtFactoryMirror.setItemTag(item, nbt);

        return new Texture(item, -1); // Use -1 as we're not using numeric identifiers
    }

    private static int calculateModelData(String variant) {
        // This is a simple hash function to generate consistent CustomModelData values
        // You might want to adjust this based on your needs
        return Math.abs(variant.hashCode()) % 999999;
    }
}

