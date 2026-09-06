package com.sxilverr.quickcraft.crafting;

@FunctionalInterface
public interface EmcLookup {
    EmcLookup NONE = key -> false;

    boolean obtainable(ItemKey key);
}
