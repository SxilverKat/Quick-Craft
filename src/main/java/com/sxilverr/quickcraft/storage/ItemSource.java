package com.sxilverr.quickcraft.storage;

import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public interface ItemSource {
    List<ItemStack> snapshot();

    int extract(ItemStack representative, int amount, boolean simulate);

    ItemStack insert(ItemStack stack, boolean simulate);

    default int extractMatching(ItemStack representative, int amount, boolean simulate) {
        if (!DamageMatch.tolerant(representative)) return extract(representative, amount, simulate);
        int extracted = 0;
        for (ItemStack variant : DamageMatch.variants(snapshot(), representative)) {
            if (extracted >= amount) break;
            extracted += extract(variant, amount - extracted, simulate);
        }
        if (extracted < amount) extracted += extract(representative, amount - extracted, simulate);
        return extracted;
    }

    default List<ItemStack> pull(ItemStack representative, int amount) {
        List<ItemStack> taken = new ArrayList<ItemStack>();
        List<ItemStack> candidates = new ArrayList<ItemStack>();
        if (DamageMatch.tolerant(representative)) candidates.addAll(DamageMatch.variants(snapshot(), representative));
        candidates.add(representative);
        int remaining = amount;
        for (ItemStack candidate : candidates) {
            if (remaining <= 0) break;
            int got = extract(candidate, remaining, false);
            if (got <= 0) continue;
            ItemStack copy = candidate.copy();
            copy.setCount(got);
            taken.add(copy);
            remaining -= got;
        }
        return taken;
    }

    default ItemStack sourceIcon() {
        return ItemStack.EMPTY;
    }

    default ItemStack sourceIconFor(ItemStack representative) {
        return extractMatching(representative, 1, true) > 0 ? sourceIcon() : null;
    }

    default int freeSlots() {
        return -1;
    }

    default int totalSlots() {
        return -1;
    }
}
