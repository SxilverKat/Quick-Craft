package com.sxilverr.quickcraft.integration.futuremc;

import com.sxilverr.quickcraft.crafting.ModdedRecipeOption;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.util.Reflect;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class FutureMcSupport {
    public static final String MODID = "futuremc";
    public static final String STONECUTTER_BLOCK = "futuremc:stonecutter";
    public static final String SMITHING_TABLE_BLOCK = "futuremc:smithing_table";

    private static final String STONECUTTER_REGISTRY = "thedarkcolour.futuremc.recipe.stonecutter.StonecutterRecipes";
    private static final String SMITHING_REGISTRY = "thedarkcolour.futuremc.recipe.smithing.SmithingRecipes";

    private FutureMcSupport() {
    }

    public static boolean available() {
        return Reg.loaded(MODID);
    }

    public static List<ModdedRecipeOption> stonecutterRecipes() {
        List<ModdedRecipeOption> out = new ArrayList<ModdedRecipeOption>();
        int index = 0;
        for (Object recipe : registryRecipes(STONECUTTER_REGISTRY)) {
            Ingredient input = ingredient(recipe, "getInput");
            ItemStack output = output(recipe);
            if (input == null || output.isEmpty()) continue;
            List<Ingredient> inputs = new ArrayList<Ingredient>();
            inputs.add(input);
            out.add(new ModdedRecipeOption(new ResourceLocation(MODID, "stonecutter/" + index++), output, inputs,
                    Station.STONECUTTER));
        }
        return out;
    }

    public static List<ModdedRecipeOption> smithingRecipes() {
        List<ModdedRecipeOption> out = new ArrayList<ModdedRecipeOption>();
        int index = 0;
        for (Object recipe : registryRecipes(SMITHING_REGISTRY)) {
            Ingredient base = ingredient(recipe, "getInput");
            Ingredient material = ingredient(recipe, "getMaterial");
            ItemStack output = output(recipe);
            if (base == null || material == null || output.isEmpty()) continue;
            List<Ingredient> inputs = new ArrayList<Ingredient>();
            inputs.add(base);
            inputs.add(material);
            out.add(new ModdedRecipeOption(new ResourceLocation(MODID, "smithing/" + index++), output, inputs,
                    Station.SMITHING));
        }
        return out;
    }

    private static Collection<?> registryRecipes(String className) {
        if (!available()) return Collections.emptyList();
        try {
            Class<?> registry = Reflect.cls(className);
            if (registry == null) return Collections.emptyList();
            Field instanceField = Reflect.field(registry, "INSTANCE");
            Object instance = instanceField == null ? null : Reflect.get(instanceField, null);
            if (instance == null) return Collections.emptyList();
            Method getRecipes = Reflect.methodByName(registry, "getRecipes", 0);
            Object recipes = getRecipes == null ? null : Reflect.invoke(getRecipes, instance);
            return recipes instanceof Collection ? (Collection<?>) recipes : Collections.emptyList();
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    private static Ingredient ingredient(Object recipe, String getter) {
        try {
            Method method = Reflect.methodByName(recipe.getClass(), getter, 0);
            Object value = method == null ? null : Reflect.invoke(method, recipe);
            return value instanceof Ingredient ? (Ingredient) value : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static ItemStack output(Object recipe) {
        try {
            Method method = Reflect.methodByName(recipe.getClass(), "getOutput", 0);
            Object value = method == null ? null : Reflect.invoke(method, recipe);
            return value instanceof ItemStack ? ((ItemStack) value).copy() : ItemStack.EMPTY;
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }
}
