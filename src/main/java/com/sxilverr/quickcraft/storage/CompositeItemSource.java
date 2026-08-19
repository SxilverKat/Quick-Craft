package com.sxilverr.quickcraft.storage;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CompositeItemSource implements ItemSource {
    private final List<ItemSource> sources;

    public CompositeItemSource(List<ItemSource> sources) {
        this.sources = sources;
    }

    @Override
    public List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemSource source : sources) {
            out.addAll(source.snapshot());
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        int extracted = 0;
        for (ItemSource source : sources) {
            if (extracted >= amount) break;
            extracted += source.extract(representative, amount - extracted, simulate);
        }
        return extracted;
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (ItemSource source : sources) {
            if (remaining.isEmpty()) break;
            remaining = source.insert(remaining, simulate);
        }
        return remaining;
    }

    @Override
    public Optional<ItemStack> sourceIconFor(ItemStack representative) {
        boolean present = false;
        for (ItemSource source : sources) {
            Optional<ItemStack> icon = source.sourceIconFor(representative);
            if (icon.isEmpty()) continue;
            present = true;
            if (!icon.get().isEmpty()) return icon;
        }
        return present ? Optional.of(ItemStack.EMPTY) : Optional.empty();
    }
}
