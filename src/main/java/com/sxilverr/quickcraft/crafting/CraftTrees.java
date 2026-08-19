package com.sxilverr.quickcraft.crafting;

public final class CraftTrees {
    private CraftTrees() {
    }

    public static boolean hasStationBlock(CraftNode node) {
        return missingStation(node) != null;
    }

    public static Station missingStation(CraftNode node) {
        if (node.isBlockedByStation()) return node.requiredStation();
        for (CraftNode child : node.children) {
            Station station = missingStation(child);
            if (station != null) return station;
        }
        return null;
    }
}
