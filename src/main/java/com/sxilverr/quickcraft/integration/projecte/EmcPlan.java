package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.crafting.ItemKey;

import java.util.Map;

public record EmcPlan(boolean access, Map<ItemKey, Integer> supplied, String costText, String totalText) {
    public static EmcPlan none() {
        return new EmcPlan(false, Map.of(), null, null);
    }
}
