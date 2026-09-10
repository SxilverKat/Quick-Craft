package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class CraftPreview {
    private CraftPreview() {
    }

    public record Gain(ItemKey key, int count) {
        public ItemStack toStack() {
            return key.toStack(1);
        }
    }

    public record Result(int craftable, int requested, List<Gain> gained, List<CraftPlanner.Blocker> blockers) {
        public Result(int craftable, int requested, List<Gain> gained) {
            this(craftable, requested, gained, List.of());
        }

        public boolean full() {
            return requested > 0 && craftable >= requested;
        }
    }
}
