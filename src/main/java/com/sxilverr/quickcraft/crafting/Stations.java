package com.sxilverr.quickcraft.crafting;

import net.minecraft.world.item.Item;

public record Stations(int gridSize, boolean smithingTable, boolean stonecutter, boolean gunSmithTable,
                       boolean ammoAssemblyTable, boolean attachmentTable, boolean extremeCrafting,
                       Item craftingSource, Item smithingSource, Item stonecutterSource) {
    public static final int RANGE = 6;

    public static Stations inventoryOnly() {
        return new Stations(2, false, false, false, false, false, false, null, null, null);
    }

    public boolean has(Station station) {
        return switch (station) {
            case CRAFTING -> gridSize >= 3;
            case SMITHING -> smithingTable;
            case STONECUTTER -> stonecutter;
            case GUN_SMITH_TABLE -> gunSmithTable;
            case AMMO_ASSEMBLY_TABLE -> ammoAssemblyTable;
            case ATTACHMENT_TABLE -> attachmentTable;
            case EXTREME_CRAFTING -> extremeCrafting;
        };
    }

    public Item sourceFor(Station station) {
        return switch (station) {
            case CRAFTING -> craftingSource;
            case SMITHING -> smithingSource;
            case STONECUTTER -> stonecutterSource;
            default -> null;
        };
    }
}
