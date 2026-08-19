package com.sxilverr.quickcraft.storage;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public interface ItemSource {
    List<ItemStack> snapshot();

    int extract(ItemStack representative, int amount, boolean simulate);

    ItemStack insert(ItemStack stack, boolean simulate);

    default ItemStack sourceIcon() {
        return ItemStack.EMPTY;
    }

    default Optional<ItemStack> sourceIconFor(ItemStack representative) {
        return extract(representative, 1, true) > 0 ? Optional.of(sourceIcon()) : Optional.empty();
    }

    default int freeSlots() {
        return -1;
    }

    default int totalSlots() {
        return -1;
    }
}
