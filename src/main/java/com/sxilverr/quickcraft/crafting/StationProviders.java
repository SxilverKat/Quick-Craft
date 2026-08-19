package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StationProviders {
    private static final Map<Station, List<String>> IDS = Map.of(
            Station.CRAFTING, List.of(
                    "minecraft:crafting_table",
                    "refinedstorage:crafting_grid",
                    "ae2:crafting_terminal",
                    "ae2:wireless_crafting_terminal",
                    "refinedstorageaddons:wireless_crafting_grid",
                    "toms_storage:ts.storage_terminal",
                    "toms_storage:ts.crafting_terminal",
                    "toms_storage:ts.adv_wireless_terminal",
                    "sophisticatedbackpacks:crafting_upgrade"),
            Station.SMITHING, List.of("minecraft:smithing_table", "sophisticatedbackpacks:smithing_upgrade"),
            Station.STONECUTTER, List.of("minecraft:stonecutter", "sophisticatedbackpacks:stonecutter_upgrade"),
            Station.GUN_SMITH_TABLE, List.of("tacz:gun_smith_table"),
            Station.AMMO_ASSEMBLY_TABLE, List.of("tacz:workbench_a"),
            Station.ATTACHMENT_TABLE, List.of("tacz:workbench_c"),
            Station.EXTREME_CRAFTING, List.of("avaritia:extreme_crafting_table")
    );

    private StationProviders() {
    }

    public static List<ItemStack> icons(Station station) {
        List<ItemStack> out = new ArrayList<>();
        for (String id : IDS.getOrDefault(station, List.of())) {
            ItemStack stack = iconFor(id);
            if (!stack.isEmpty()) out.add(stack);
        }
        return out;
    }

    public static ItemStack iconFor(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        String blockIndex = switch (id) {
            case "tacz:workbench_a" -> "tacz:ammo_workbench";
            case "tacz:workbench_c" -> "tacz:attachment_workbench";
            default -> null;
        };
        if (blockIndex != null) {
            //? if <1.20.5 {
            stack.getOrCreateTag().putString("BlockId", blockIndex);
            //?}
        }
        return stack;
    }
}
