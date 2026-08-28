package com.sxilverr.quickcraft.integration.avaritia;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.crafting.Ingredients;
import com.sxilverr.quickcraft.crafting.ModdedRecipeOption;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.util.Reflect;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AvaritiaSupport {
    public static final String MODID = "avaritia";

    private static final String[] RECIPE_INTERFACES = {
            "morph.avaritia.recipe.extreme.IExtremeRecipe",
            "morph.avaritia.recipe.IExtremeRecipe",
    };

    private static final String[] RECIPE_MANAGERS = {
            "morph.avaritia.recipe.AvaritiaRecipeManager",
            "morph.avaritia.recipe.RecipeManager",
    };

    private static final String[] RECIPE_FIELDS = {"EXTREME_RECIPES", "extremeRecipes", "EXTREME"};

    private static final String[] TABLE_IDS = {
            "avaritia:extreme_crafting_table",
            "avaritia:extreme_crafting",
    };

    private static boolean resolved;
    private static Class<?> extremeRecipeClass;
    private static Block table;

    private AvaritiaSupport() {
    }

    public static boolean available() {
        resolve();
        return extremeRecipeClass != null || table != null;
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        if (!Reg.loaded(MODID)) return;

        for (String name : RECIPE_INTERFACES) {
            extremeRecipeClass = Reflect.cls(name);
            if (extremeRecipeClass != null) break;
        }
        table = resolveTable();
    }

    private static Block resolveTable() {
        for (String id : TABLE_IDS) {
            Block block = Reg.block(id);
            if (block != null) return block;
        }
        for (Block block : ForgeRegistries.BLOCKS) {
            ResourceLocation id = block.getRegistryName();
            if (id == null || !MODID.equals(id.getNamespace())) continue;
            if (id.getPath().contains("crafting_table") || id.getPath().contains("crafting")) return block;
        }
        return null;
    }

    public static Block table() {
        resolve();
        return table;
    }

    public static boolean isExtremeCraftingBlock(IBlockState state) {
        resolve();
        if (state == null || table == null || state.getBlock() != table) return false;
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
            if (!"type".equals(entry.getKey().getName())) continue;
            return String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT).contains("extreme");
        }
        return true;
    }

    public static boolean isExtremeRecipe(IRecipe recipe) {
        resolve();
        return extremeRecipeClass != null && recipe != null && extremeRecipeClass.isInstance(recipe);
    }

    public static ModdedRecipeOption toOption(ResourceLocation id, Object recipe) {
        ItemStack result = resultOf(recipe);
        if (result == null || result.isEmpty()) return null;
        List<Ingredient> inputs = inputsOf(recipe);
        if (inputs.isEmpty()) return null;
        ResourceLocation recipeId = id != null ? id : idOf(recipe);
        if (recipeId == null) return null;
        return new ModdedRecipeOption(recipeId, result.copy(), inputs, Station.EXTREME_CRAFTING);
    }

    public static List<ModdedRecipeOption> collectPrivateRecipes() {
        resolve();
        if (!Reg.loaded(MODID)) return Collections.emptyList();

        List<ModdedRecipeOption> out = new ArrayList<ModdedRecipeOption>();
        Object recipes = privateRecipeHolder();
        if (recipes == null) return out;

        try {
            if (recipes instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) recipes).entrySet()) {
                    Object key = entry.getKey();
                    ModdedRecipeOption option = toOption(key instanceof ResourceLocation ? (ResourceLocation) key : null,
                            entry.getValue());
                    if (option != null) out.add(option);
                }
            } else if (recipes instanceof Iterable) {
                for (Object recipe : (Iterable<?>) recipes) {
                    ModdedRecipeOption option = toOption(null, recipe);
                    if (option != null) out.add(option);
                }
            }
        } catch (Throwable t) {
            QuickCraft.LOGGER.warn("Quick Craft: Avaritia extreme recipe indexing failed", t);
        }
        return out;
    }

    private static Object privateRecipeHolder() {
        for (String managerName : RECIPE_MANAGERS) {
            Class<?> manager = Reflect.cls(managerName);
            if (manager == null) continue;
            for (String fieldName : RECIPE_FIELDS) {
                Field field = Reflect.field(manager, fieldName);
                Object value = Reflect.get(field, null);
                if (value instanceof Map && !((Map<?, ?>) value).isEmpty()) return value;
                if (value instanceof Collection && !((Collection<?>) value).isEmpty()) return value;
            }
        }
        return null;
    }

    private static ItemStack resultOf(Object recipe) {
        if (recipe instanceof IRecipe) {
            try {
                return ((IRecipe) recipe).getRecipeOutput();
            } catch (Throwable t) {
                return null;
            }
        }
        Object result = Reflect.invoke(Reflect.methodByName(recipe.getClass(), "getRecipeOutput", 0), recipe);
        if (result == null) result = Reflect.invoke(Reflect.methodByName(recipe.getClass(), "getOutput", 0), recipe);
        return result instanceof ItemStack ? (ItemStack) result : null;
    }

    private static List<Ingredient> inputsOf(Object recipe) {
        Object raw;
        if (recipe instanceof IRecipe) {
            try {
                raw = ((IRecipe) recipe).getIngredients();
            } catch (Throwable t) {
                return Collections.emptyList();
            }
        } else {
            raw = Reflect.invoke(Reflect.methodByName(recipe.getClass(), "getIngredients", 0), recipe);
            if (raw == null) raw = Reflect.invoke(Reflect.methodByName(recipe.getClass(), "getInputs", 0), recipe);
        }
        if (!(raw instanceof Iterable)) return Collections.emptyList();

        List<Ingredient> inputs = new ArrayList<Ingredient>();
        for (Object entry : (Iterable<?>) raw) {
            if (!(entry instanceof Ingredient)) continue;
            Ingredient ingredient = (Ingredient) entry;
            if (!Ingredients.isEmpty(ingredient)) inputs.add(ingredient);
        }
        return inputs;
    }

    private static ResourceLocation idOf(Object recipe) {
        if (recipe instanceof IRecipe) return ((IRecipe) recipe).getRegistryName();
        Object id = Reflect.invoke(Reflect.methodByName(recipe.getClass(), "getRegistryName", 0), recipe);
        return id instanceof ResourceLocation ? (ResourceLocation) id : null;
    }
}
