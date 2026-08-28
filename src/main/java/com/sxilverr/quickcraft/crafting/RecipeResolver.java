package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.integration.avaritia.AvaritiaSupport;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipeResolver {
    private final Map<Item, List<RecipeOption>> byResult = new HashMap<Item, List<RecipeOption>>();
    private final Set<ResourceLocation> forgeRegistryRecipeIds = new HashSet<ResourceLocation>();

    public RecipeResolver() {
        indexCrafting();
        indexAvaritia();
    }

    private void indexCrafting() {
        int crafting = 0;
        int extreme = 0;
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            if (recipe == null) continue;
            try {
                ItemStack result = recipe.getRecipeOutput();
                if (result == null || result.isEmpty()) continue;
                ResourceLocation id = recipe.getRegistryName();
                if (id == null) continue;

                if (AvaritiaSupport.isExtremeRecipe(recipe)) {
                    ModdedRecipeOption option = AvaritiaSupport.toOption(id, recipe);
                    if (option == null) continue;
                    add(result.getItem(), option);
                    forgeRegistryRecipeIds.add(id);
                    extreme++;
                    continue;
                }

                CraftingRecipeOption option = new CraftingRecipeOption(id, recipe);
                if (option.inputs().isEmpty()) continue;
                add(result.getItem(), option);
                crafting++;
            } catch (Throwable t) {
            }
        }
        QuickCraft.LOGGER.info("Quick Craft indexed {} crafting recipe(s)", crafting);
        if (extreme > 0) {
            QuickCraft.LOGGER.info("Quick Craft indexed {} Avaritia extreme recipe(s) from the Forge registry", extreme);
        }
    }

    private void indexAvaritia() {
        if (!AvaritiaSupport.available()) return;
        int count = 0;
        for (ModdedRecipeOption option : AvaritiaSupport.collectPrivateRecipes()) {
            if (!forgeRegistryRecipeIds.add(option.id())) continue;
            add(option.result().getItem(), option);
            count++;
        }
        if (count > 0) {
            QuickCraft.LOGGER.info("Quick Craft indexed {} Avaritia extreme recipe(s)", count);
        }
    }

    private void add(Item result, RecipeOption option) {
        List<RecipeOption> list = byResult.get(result);
        if (list == null) {
            list = new ArrayList<RecipeOption>();
            byResult.put(result, list);
        }
        list.add(option);
    }

    public boolean canCraft(ItemStack output) {
        return !recipesFor(output).isEmpty();
    }

    public List<RecipeOption> recipesFor(ItemStack output) {
        if (output == null || output.isEmpty()) return Collections.emptyList();
        List<RecipeOption> all = byResult.get(output.getItem());
        if (all == null || all.isEmpty()) return Collections.emptyList();

        List<RecipeOption> matched = new ArrayList<RecipeOption>();
        for (RecipeOption option : all) {
            if (resultMatches(option.result(), output)) matched.add(option);
        }
        return matched;
    }

    private static boolean resultMatches(ItemStack result, ItemStack requested) {
        if (metaOf(result) != metaOf(requested)) return false;
        NBTTagCompound resultTag = result.getTagCompound();
        if (resultTag == null || resultTag.isEmpty()) return true;
        NBTTagCompound requestedTag = requested.getTagCompound();
        if (requestedTag == null) return false;
        return isSubset(resultTag, requestedTag);
    }

    private static int metaOf(ItemStack stack) {
        int meta = stack.getItemDamage();
        if (meta == OreDictionary.WILDCARD_VALUE) return 0;
        Item item = stack.getItem();
        return item != null && item.isDamageable() ? 0 : meta;
    }

    private static boolean isSubset(NBTTagCompound inner, NBTTagCompound outer) {
        for (String key : inner.getKeySet()) {
            NBTBase innerValue = inner.getTag(key);
            NBTBase outerValue = outer.getTag(key);
            if (outerValue == null) return false;
            if (innerValue instanceof NBTTagCompound && outerValue instanceof NBTTagCompound) {
                if (!isSubset((NBTTagCompound) innerValue, (NBTTagCompound) outerValue)) return false;
            } else if (!innerValue.equals(outerValue)) {
                return false;
            }
        }
        return true;
    }
}
