package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

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
        private final List<CraftPlanner.Blocker> blockers;

        public Result(int craftable, int requested, List<Gain> gained) {
            this(craftable, requested, gained, Collections.<CraftPlanner.Blocker>emptyList());
        }

        public Result(int craftable, int requested, List<Gain> gained, List<CraftPlanner.Blocker> blockers) {
            this.craftable = craftable;
            this.requested = requested;
            this.gained = gained;
            this.blockers = blockers;
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

        public List<CraftPlanner.Blocker> blockers() {
            return blockers;
        }

        public boolean full() {
            return requested > 0 && craftable >= requested;
        }
    }
}
