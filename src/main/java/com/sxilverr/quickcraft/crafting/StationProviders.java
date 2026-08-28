package com.sxilverr.quickcraft.crafting;

import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class StationProviders {
    private static final Map<Station, List<String>> IDS = new EnumMap<Station, List<String>>(Station.class);

    static {
        IDS.put(Station.CRAFTING, Arrays.asList(
                "minecraft:crafting_table",
                "refinedstorage:grid@1",
                "appliedenergistics2:part@340",
                "appliedenergistics2:wireless_crafting_terminal",
                "refinedstorageaddons:wireless_crafting_grid"));
        IDS.put(Station.EXTREME_CRAFTING, Arrays.asList(
                "avaritia:extreme_crafting_table",
                "avaritia:extreme_crafting"));
    }

    private StationProviders() {
    }

    public static List<ItemStack> icons(Station station) {
        List<String> ids = IDS.get(station);
        if (ids == null) return Collections.emptyList();
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (String id : ids) {
            ItemStack stack = iconFor(id);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    public static ItemStack iconFor(String id) {
        return Reg.stack(id);
    }
}
