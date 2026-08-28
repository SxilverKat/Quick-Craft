package com.sxilverr.quickcraft.storage;

import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DamageMatch {
    private DamageMatch() {
    }

    public static boolean tolerant(ItemStack representative) {
        return representative != null && !representative.isEmpty() && representative.isItemStackDamageable();
    }

    public static List<ItemStack> variants(List<ItemStack> stacks, ItemStack representative) {
        ItemKey key = ItemKey.of(representative);
        int exact = representative.getItemDamage();
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty() || stack.getItem() != representative.getItem()) continue;
            int damage = stack.getItemDamage();
            if (damage == exact || holds(out, damage)) continue;
            if (!key.equals(ItemKey.of(stack))) continue;
            out.add(stack);
        }
        out.sort(new Comparator<ItemStack>() {
            @Override
            public int compare(ItemStack a, ItemStack b) {
                return Integer.compare(b.getItemDamage(), a.getItemDamage());
            }
        });
        return out;
    }

    public static ItemStack worst(List<ItemStack> stacks, ItemStack representative) {
        if (!tolerant(representative)) return ItemStack.EMPTY;
        List<ItemStack> found = variants(stacks, representative);
        if (found.isEmpty()) return ItemStack.EMPTY;
        ItemStack sample = found.get(0).copy();
        sample.setCount(1);
        return sample;
    }

    private static boolean holds(List<ItemStack> stacks, int damage) {
        for (ItemStack stack : stacks) {
            if (stack.getItemDamage() == damage) return true;
        }
        return false;
    }
}
