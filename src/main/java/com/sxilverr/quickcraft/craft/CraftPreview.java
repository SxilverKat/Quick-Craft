package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CraftPreview {
    private CraftPreview() {
    }

    public static final class Gain {
        private final ItemKey key;
        private final int count;

        public Gain(ItemKey key, int count) {
            this.key = key;
            this.count = count;
        }

        public ItemKey key() {
            return key;
        }

        public int count() {
            return count;
        }

        public ItemStack toStack() {
            return key.toStack(1);
        }
    }

    public static final class Result {
        private final int craftable;
        private final int requested;
        private final List<Gain> gained;

        public Result(int craftable, int requested, List<Gain> gained) {
            this.craftable = craftable;
            this.requested = requested;
            this.gained = gained;
        }

        public int craftable() {
            return craftable;
        }

        public int requested() {
            return requested;
        }

        public List<Gain> gained() {
            return gained;
        }

        public boolean full() {
            return requested > 0 && craftable >= requested;
        }
    }

    public static Result simulate(CraftNode root, Map<ItemKey, Integer> have, ItemStack target, int quantity) {
        return simulate(root, have, target, quantity, null);
    }

    public static Result simulate(CraftNode root, Map<ItemKey, Integer> have, ItemStack target,
                                  int quantity, EmcBank bank) {
        VirtualPool initial = new VirtualPool();
        for (Map.Entry<ItemKey, Integer> entry : have.entrySet()) {
            initial.add(entry.getKey(), entry.getValue());
        }
        VirtualPool working = initial.copy();
        working.setEmc(bank);
        CraftExecutor.simulate(root, working);

        final ItemKey targetKey = ItemKey.of(target);
        int crafted = Math.max(0, working.count(targetKey) - initial.count(targetKey));
        if (bank != null && crafted < quantity && bank.supplies(targetKey)) {
            int buy = Math.min(quantity - crafted, bank.affordable(targetKey));
            if (buy > 0 && bank.buy(targetKey, buy)) working.produce(targetKey, buy);
        }
        int craftable = Math.max(0, working.count(targetKey) - initial.count(targetKey));

        List<Gain> gained = new ArrayList<Gain>();
        for (ItemKey key : working.counts().keySet()) {
            int delta = working.count(key) - initial.count(key);
            if (delta > 0) gained.add(new Gain(key, delta));
        }
        gained.sort(new Comparator<Gain>() {
            @Override
            public int compare(Gain a, Gain b) {
                int tierA = a.key().equals(targetKey) ? 0 : 1;
                int tierB = b.key().equals(targetKey) ? 0 : 1;
                if (tierA != tierB) return tierA - tierB;
                return Integer.compare(b.count(), a.count());
            }
        });

        return new Result(Math.min(craftable, Math.max(0, quantity)), quantity, gained);
    }
}
