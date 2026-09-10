package com.sxilverr.quickcraft.crafting;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static boolean truncated(CraftNode node) {
        if (node.truncated) return true;
        for (CraftNode child : node.children) {
            if (truncated(child)) return true;
        }
        return false;
    }

    public static Map<ItemKey, Integer> leafTotals(CraftNode root) {
        Map<ItemKey, Integer> totals = new LinkedHashMap<>();
        collect(root, root.requiredCount, totals, null);
        return totals;
    }

    public static Map<ItemKey, CraftNode> leafSamples(CraftNode root) {
        Map<ItemKey, CraftNode> samples = new HashMap<>();
        collect(root, root.requiredCount, new HashMap<>(), samples);
        return samples;
    }

    private static void collect(CraftNode node, int required, Map<ItemKey, Integer> totals, Map<ItemKey, CraftNode> samples) {
        if (node.reference != null) {
            collect(node.reference, required, totals, samples);
            return;
        }
        boolean hasRealChild = false;
        for (CraftNode child : node.children) {
            if (!child.isMobSource()) {
                hasRealChild = true;
                break;
            }
        }
        if (!hasRealChild) {
            ItemKey key = ItemKey.of(node.output);
            totals.merge(key, required, Integer::sum);
            if (samples != null) samples.putIfAbsent(key, node);
            return;
        }
        int crafts = ceilDiv(required, Math.max(1, node.resultPerCraft));
        int ownCrafts = Math.max(1, node.craftsNeeded);
        for (CraftNode child : node.children) {
            if (child.isMobSource()) continue;
            int childNeed = child.catalyst ? child.requiredCount : child.requiredCount / ownCrafts * crafts;
            collect(child, childNeed, totals, samples);
        }
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
