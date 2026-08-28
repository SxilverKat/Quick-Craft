package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.crafting.ItemKey;

import java.util.Collections;
import java.util.Map;

public final class EmcPlan {
    private final boolean access;
    private final Map<ItemKey, Integer> supplied;
    private final String costText;
    private final String totalText;
    private final boolean affordable;
    private final Map<ItemKey, Integer> capacity;

    public EmcPlan(boolean access, Map<ItemKey, Integer> supplied, String costText, String totalText,
                   boolean affordable, Map<ItemKey, Integer> capacity) {
        this.access = access;
        this.supplied = supplied;
        this.costText = costText;
        this.totalText = totalText;
        this.affordable = affordable;
        this.capacity = capacity;
    }

    public Map<ItemKey, Integer> capacity() {
        return capacity;
    }

    public boolean affordable() {
        return affordable;
    }

    public static EmcPlan none() {
        return new EmcPlan(false, Collections.<ItemKey, Integer>emptyMap(), null, null, true,
                Collections.<ItemKey, Integer>emptyMap());
    }

    public boolean access() {
        return access;
    }

    public Map<ItemKey, Integer> supplied() {
        return supplied;
    }

    public String costText() {
        return costText;
    }

    public String totalText() {
        return totalText;
    }
}
