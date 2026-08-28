package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;

import java.math.BigInteger;

public final class CraftExecutor {
    private CraftExecutor() {
    }

    private static final int MAX_PASSES = 256;

    public static void simulate(CraftNode root, VirtualPool pool) {
        ItemKey rootKey = ItemKey.of(root.output);
        int goal = pool.count(rootKey) + root.requiredCount;
        for (int pass = 0; pass < MAX_PASSES && pool.count(rootKey) < goal; pass++) {
            if (ensure(root, goal, pool) == 0) break;
        }
    }

    private static int ensure(CraftNode node, int need, VirtualPool pool) {
        if (!node.fitsStation || node.children.isEmpty()) return 0;

        ItemKey outputKey = ItemKey.of(node.output);
        int crafts = Math.max(1, node.craftsNeeded);
        int done = 0;
        while (pool.count(outputKey) < need) {
            int before = pool.count(outputKey);
            for (CraftNode child : node.children) {
                done += ensure(child, child.requiredCount / crafts, pool);
            }
            if (!canCraftOnce(node, pool, crafts)) break;
            doCraftOnce(node, pool, crafts, outputKey);
            done++;
            if (pool.count(outputKey) <= before) break;
        }
        return done;
    }

    private static boolean canCraftOnce(CraftNode node, VirtualPool pool, int crafts) {
        if (!pool.hasEmc()) {
            for (CraftNode child : node.children) {
                if (pool.count(ItemKey.of(child.output)) < child.requiredCount / crafts) return false;
            }
            return true;
        }
        BigInteger need = BigInteger.ZERO;
        for (CraftNode child : node.children) {
            ItemKey key = ItemKey.of(child.output);
            int required = child.requiredCount / crafts;
            int have = pool.count(key);
            if (have >= required) continue;
            long value = pool.emcValue(key);
            if (value <= 0L) return false;
            need = need.add(BigInteger.valueOf(value).multiply(BigInteger.valueOf(required - have)));
        }
        return pool.emcAfford(need);
    }

    private static void doCraftOnce(CraftNode node, VirtualPool pool, int crafts, ItemKey outputKey) {
        for (CraftNode child : node.children) {
            int occ = child.requiredCount / crafts;
            pool.take(ItemKey.of(child.output), occ);
            ItemStack remainder = ForgeHooks.getContainerItem(child.output);
            if (remainder != null && !remainder.isEmpty()) {
                pool.add(ItemKey.of(remainder), occ * remainder.getCount());
            }
        }
        pool.produce(outputKey, node.resultPerCraft);
    }
}
