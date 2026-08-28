package com.sxilverr.quickcraft.craft;

import java.util.Collections;
import java.util.List;

public final class CraftSummary {
    public static final class Placement {
        private final String where;
        private final int count;

        public Placement(String where, int count) {
            this.where = where;
            this.count = count;
        }

        public String where() {
            return where;
        }

        public int count() {
            return count;
        }
    }

    private final int crafted;
    private final int requested;
    private final String missingStation;
    private final List<Placement> placements;
    private final int dropped;
    private final int byproducts;
    private final boolean aborted;

    public CraftSummary(int crafted, int requested, String missingStation,
                        List<Placement> placements, int dropped, int byproducts, boolean aborted) {
        this.crafted = crafted;
        this.requested = requested;
        this.missingStation = missingStation;
        this.placements = placements;
        this.dropped = dropped;
        this.byproducts = byproducts;
        this.aborted = aborted;
    }

    public CraftSummary(int crafted, int requested, String missingStation,
                        List<Placement> placements, int dropped, int byproducts) {
        this(crafted, requested, missingStation, placements, dropped, byproducts, false);
    }

    public static CraftSummary empty() {
        return new CraftSummary(0, 0, null, Collections.<Placement>emptyList(), 0, 0);
    }

    public static CraftSummary aborted(int requested) {
        return new CraftSummary(0, requested, null, Collections.<Placement>emptyList(), 0, 0, true);
    }

    public int crafted() {
        return crafted;
    }

    public int requested() {
        return requested;
    }

    public String missingStation() {
        return missingStation;
    }

    public List<Placement> placements() {
        return placements;
    }

    public int dropped() {
        return dropped;
    }

    public int byproducts() {
        return byproducts;
    }

    public boolean aborted() {
        return aborted;
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
