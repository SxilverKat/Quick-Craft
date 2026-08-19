package com.sxilverr.quickcraft.craft;

import java.util.List;

public record CraftSummary(int crafted, int requested, String missingStation,
                           List<Placement> placements, int dropped, int byproducts, boolean aborted) {
    public record Placement(String where, int count) {
    }

    public CraftSummary(int crafted, int requested, String missingStation,
                        List<Placement> placements, int dropped, int byproducts) {
        this(crafted, requested, missingStation, placements, dropped, byproducts, false);
    }

    public static CraftSummary empty() {
        return new CraftSummary(0, 0, null, List.of(), 0, 0);
    }

    public static CraftSummary aborted(int requested) {
        return new CraftSummary(0, requested, null, List.of(), 0, 0, true);
    }

    public boolean full() {
        return requested > 0 && crafted >= requested;
    }

    public boolean partial() {
        return crafted > 0 && crafted < requested;
    }

    public boolean nothing() {
        return crafted <= 0;
    }
}
