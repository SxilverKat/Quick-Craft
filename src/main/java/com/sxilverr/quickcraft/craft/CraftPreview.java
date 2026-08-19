package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CraftPreview {
    private CraftPreview() {
    }

    public record Gain(ItemKey key, int count) {
        public ItemStack toStack() {
            return key.toStack(1);
        }
    }

    public record Result(int craftable, int requested, List<Gain> gained) {
        public boolean full() {
            return requested > 0 && craftable >= requested;
        }
    }

    public static Result simulate(CraftNode root, Map<ItemKey, Integer> have, ItemStack target, int quantity) {
        return simulate(root, have, target, quantity, null);
    }

    public static Result simulate(CraftNode root, Map<ItemKey, Integer> have, ItemStack target, int quantity, EmcBank bank) {
        VirtualPool initial = new VirtualPool();
        for (Map.Entry<ItemKey, Integer> entry : have.entrySet()) {
            initial.add(entry.getKey(), entry.getValue());
        }
        VirtualPool working = initial.copy();
        working.setEmc(bank);
        CraftExecutor.simulate(root, working);

        ItemKey targetKey = ItemKey.of(target);
        int crafted = Math.max(0, working.count(targetKey) - initial.count(targetKey));
        if (bank != null && crafted < quantity && bank.supplies(targetKey)) {
            int buy = Math.min(quantity - crafted, bank.affordable(targetKey));
            if (buy > 0 && bank.buy(targetKey, buy)) working.produce(targetKey, buy);
        }
        int craftable = Math.max(0, working.count(targetKey) - initial.count(targetKey));

        List<Gain> gained = new ArrayList<>();
        for (ItemKey key : working.counts().keySet()) {
            int delta = working.count(key) - initial.count(key);
            if (delta > 0) gained.add(new Gain(key, delta));
        }
        gained.sort(Comparator
                .comparingInt((Gain g) -> g.key().equals(targetKey) ? 0 : 1)
                .thenComparing(g -> -g.count()));

        return new Result(Math.min(craftable, Math.max(0, quantity)), quantity, gained);
    }
}
