package com.sxilverr.quickcraft.crafting;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public interface Availability {
    Availability NONE = new Availability() {
        @Override
        public int available(ItemKey key) {
            return 0;
        }

        @Override
        public int availableItem(Item item) {
            return 0;
        }
    };

    int available(ItemKey key);

    int availableItem(Item item);

    static Availability of(Map<ItemKey, Integer> counts) {
        Map<Item, Integer> byItem = new HashMap<>();
        for (Map.Entry<ItemKey, Integer> entry : counts.entrySet()) {
            byItem.merge(entry.getKey().item(), entry.getValue(), Integer::sum);
        }
        return new Availability() {
            @Override
            public int available(ItemKey key) {
                return counts.getOrDefault(key, 0);
            }

            @Override
            public int availableItem(Item item) {
                return byItem.getOrDefault(item, 0);
            }
        };
    }
}
