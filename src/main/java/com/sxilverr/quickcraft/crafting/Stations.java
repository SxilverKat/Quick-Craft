package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.Item;

public final class Stations {
    public static final int RANGE = 6;

    private final int gridSize;
    private final boolean extremeCrafting;
    private final Item craftingSource;
    private final Item extremeSource;

    public Stations(int gridSize, boolean extremeCrafting, Item craftingSource, Item extremeSource) {
        this.gridSize = gridSize;
        this.extremeCrafting = extremeCrafting;
        this.craftingSource = craftingSource;
        this.extremeSource = extremeSource;
    }

    public static Stations inventoryOnly() {
        return new Stations(2, false, null, null);
    }

    public int gridSize() {
        return gridSize;
    }

    public boolean extremeCrafting() {
        return extremeCrafting;
    }

    public Item craftingSource() {
        return craftingSource;
    }

    public Item extremeSource() {
        return extremeSource;
    }

    public boolean has(Station station) {
        switch (station) {
            case CRAFTING:
                return gridSize >= 3;
            case EXTREME_CRAFTING:
                return extremeCrafting;
            default:
                return false;
        }
    }

    public Item sourceFor(Station station) {
        switch (station) {
            case CRAFTING:
                return craftingSource;
            case EXTREME_CRAFTING:
                return extremeSource;
            default:
                return null;
        }
    }
}
