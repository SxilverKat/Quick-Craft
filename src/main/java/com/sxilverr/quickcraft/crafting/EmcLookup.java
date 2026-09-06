package com.sxilverr.quickcraft.crafting;

public interface EmcLookup {
    EmcLookup NONE = new EmcLookup() {
        @Override
        public boolean obtainable(ItemKey key) {
            return false;
        }
    };

    boolean obtainable(ItemKey key);
}
