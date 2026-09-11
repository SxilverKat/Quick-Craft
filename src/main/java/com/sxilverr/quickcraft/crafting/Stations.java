package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.Item;

public final class Stations {
    public static final int RANGE = 6;

    private final int gridSize;
    private final boolean extremeCrafting;
    private final boolean stonecutter;
    private final boolean smithing;
    private final Item craftingSource;
    private final Item extremeSource;
    private final Item stonecutterSource;
    private final Item smithingSource;

    public Stations(int gridSize, boolean extremeCrafting, boolean stonecutter, boolean smithing, Item craftingSource,
                    Item extremeSource, Item stonecutterSource, Item smithingSource) {
        this.gridSize = gridSize;
        this.extremeCrafting = extremeCrafting;
        this.stonecutter = stonecutter;
        this.smithing = smithing;
        this.craftingSource = craftingSource;
        this.extremeSource = extremeSource;
        this.stonecutterSource = stonecutterSource;
        this.smithingSource = smithingSource;
    }

    public static Stations inventoryOnly() {
        return new Stations(2, false, false, false, null, null, null, null);
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

    public boolean stonecutter() {
        return stonecutter;
    }

    public boolean smithing() {
        return smithing;
    }

    public Item stonecutterSource() {
        return stonecutterSource;
    }

    public Item smithingSource() {
        return smithingSource;
    }

    public boolean has(Station station) {
        switch (station) {
            case CRAFTING:
                return gridSize >= 3;
            case EXTREME_CRAFTING:
                return extremeCrafting;
            case STONECUTTER:
                return stonecutter;
            case SMITHING:
                return smithing;
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
            case STONECUTTER:
                return stonecutterSource;
            case SMITHING:
                return smithingSource;
            default:
                return null;
        }
    }
}
