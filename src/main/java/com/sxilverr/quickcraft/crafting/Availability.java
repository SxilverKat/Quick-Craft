package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.Item;

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

    final class Factory {
        private Factory() {
        }

        public static Availability of(final Map<ItemKey, Integer> counts) {
            final Map<Item, Integer> byItem = new HashMap<Item, Integer>();
            for (Map.Entry<ItemKey, Integer> entry : counts.entrySet()) {
                Item item = entry.getKey().item();
                Integer existing = byItem.get(item);
                byItem.put(item, existing == null ? entry.getValue() : existing + entry.getValue());
            }
            return new Availability() {
                @Override
                public int available(ItemKey key) {
                    Integer value = counts.get(key);
                    return value == null ? 0 : value;
                }

                @Override
                public int availableItem(Item item) {
                    Integer value = byItem.get(item);
                    return value == null ? 0 : value;
                }
            };
        }
    }
}
