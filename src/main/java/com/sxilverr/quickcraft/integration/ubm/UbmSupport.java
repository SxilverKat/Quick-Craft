package com.sxilverr.quickcraft.integration.ubm;

import com.sxilverr.quickcraft.crafting.ModdedRecipeOption;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.util.Reflect;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UbmSupport {
    public static final String MODID = "ubm";
    public static final String SMITHING_TABLE_BLOCK = "ubm:smithing_table";

    private static final String ITEMS_CLASS = "de.julianweinelt.ubm.items.ModItems";
    private static final String INGOT_FIELD = "NETHERITE_INGOT";
    private static final Item[] DIAMOND_GEAR = {
            Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
            Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_AXE, Items.DIAMOND_HOE,
    };
    private static final String[] NETHERITE_FIELDS = {
            "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS",
            "NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_SHOVEL", "NETHERITE_AXE", "NETHERITE_HOE",
    };

    private UbmSupport() {
    }

    public static boolean available() {
        return Reg.loaded(MODID);
    }

    public static List<ModdedRecipeOption> smithingRecipes() {
        List<ModdedRecipeOption> out = new ArrayList<ModdedRecipeOption>();
        if (!available()) return out;
        Class<?> items = Reflect.cls(ITEMS_CLASS);
        if (items == null) return out;
        Item ingot = item(items, INGOT_FIELD);
        if (ingot == null) return out;
        for (int i = 0; i < DIAMOND_GEAR.length; i++) {
            Item result = item(items, NETHERITE_FIELDS[i]);
            if (result == null) continue;
            List<Ingredient> inputs = new ArrayList<Ingredient>();
            inputs.add(Ingredient.fromItem(DIAMOND_GEAR[i]));
            inputs.add(Ingredient.fromItem(ingot));
            String path = "smithing/" + NETHERITE_FIELDS[i].toLowerCase(Locale.ROOT);
            out.add(new ModdedRecipeOption(new ResourceLocation(MODID, path), new ItemStack(result), inputs,
                    Station.SMITHING));
        }
        return out;
    }

    private static Item item(Class<?> owner, String fieldName) {
        try {
            Field field = Reflect.field(owner, fieldName);
            Object value = field == null ? null : Reflect.get(field, null);
            return value instanceof Item && value != Items.AIR ? (Item) value : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
