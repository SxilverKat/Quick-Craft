package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VirtualPool {
    private final Map<ItemKey, Integer> counts = new HashMap<>();
    private final Map<ItemKey, Integer> produced = new HashMap<>();
    private EmcBank emc;

    public void setEmc(EmcBank emc) {
        this.emc = emc;
    }

    public boolean hasEmc() {
        return emc != null;
    }

    public long emcValue(ItemKey key) {
        return emc == null ? 0L : emc.value(key);
    }

    public boolean emcAfford(BigInteger cost) {
        return emc != null && emc.canAfford(cost);
    }

    public void add(ItemKey key, int amount) {
        if (amount <= 0) return;
        counts.merge(key, amount, Integer::sum);
    }

    public void addStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        add(ItemKey.of(stack), stack.getCount());
    }

    public void produce(ItemKey key, int amount) {
        if (amount <= 0) return;
        add(key, amount);
        produced.merge(key, amount, Integer::sum);
    }

    public Set<ItemKey> producedKeys() {
        return produced.keySet();
    }

    public int count(ItemKey key) {
        return counts.getOrDefault(key, 0);
    }

    public boolean take(ItemKey key, int amount) {
        int have = counts.getOrDefault(key, 0);
        if (have >= amount) {
            if (have == amount) {
                counts.remove(key);
            } else {
                counts.put(key, have - amount);
            }
            return true;
        }
        if (emc != null && emc.buy(key, amount - have)) {
            counts.remove(key);
            return true;
        }
        return false;
    }

    public Map<ItemKey, Integer> counts() {
        return counts;
    }

    public VirtualPool copy() {
        VirtualPool other = new VirtualPool();
        other.counts.putAll(this.counts);
        other.emc = this.emc;
        return other;
    }
}
