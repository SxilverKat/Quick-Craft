package com.sxilverr.quickcraft.storage;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        List<ItemStack> taken = new ArrayList<>();
        int remaining = amount;
        if (DamageMatch.tolerant(representative)) {
            for (ItemStack variant : DamageMatch.variants(snapshot(), representative)) {
                if (remaining <= 0) break;
                remaining -= pullExact(variant, remaining, taken);
            }
        }
        if (remaining > 0) pullExact(representative, remaining, taken);
        return taken;
    }

    private int pullExact(ItemStack stack, int amount, List<ItemStack> taken) {
        int got = extract(stack, amount, false);
        if (got <= 0) return 0;
        ItemStack copy = stack.copy();
        copy.setCount(got);
        taken.add(copy);
        return got;
    }

    default ItemStack sourceIcon() {
        return ItemStack.EMPTY;
    }

    default Optional<ItemStack> sourceIconFor(ItemStack representative) {
        return extractMatching(representative, 1, true) > 0 ? Optional.of(sourceIcon()) : Optional.empty();
    }

    default int freeSlots() {
        return -1;
    }

    default int totalSlots() {
        return -1;
    }
}
