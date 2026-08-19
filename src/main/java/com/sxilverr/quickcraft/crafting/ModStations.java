package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModStations {
    public record Def(String modId, Station station, List<String> recipeTypeIds, List<String> blockIds) {
    }

    private static final Set<String> VANILLA_RECIPE_TYPES =
            Set.of("minecraft:crafting", "minecraft:smithing", "minecraft:stonecutting");

    private static final List<Def> DEFS = List.of(
            new Def("tacz", Station.GUN_SMITH_TABLE,
                    List.of("tacz:gun_smith_table"),
                    List.of("tacz:gun_smith_table")),
            new Def("tacz", Station.AMMO_ASSEMBLY_TABLE,
                    List.of(),
                    List.of("tacz:workbench_a")),
            new Def("tacz", Station.ATTACHMENT_TABLE,
                    List.of(),
                    List.of("tacz:workbench_c")),
            new Def("avaritia", Station.EXTREME_CRAFTING,
                    List.of("avaritia:extreme_shaped", "avaritia:extreme_shapeless", "avaritia:extreme_crafting"),
                    List.of("avaritia:extreme_crafting_table")),
            new Def("refinedstorage", Station.CRAFTING,
                    List.of(),
                    List.of("refinedstorage:crafting_grid")),
            new Def("toms_storage", Station.CRAFTING,
                    List.of(),
                    List.of("toms_storage:ts.storage_terminal", "toms_storage:ts.crafting_terminal",
                            "toms_storage:storage_terminal", "toms_storage:crafting_terminal"))
    );

    private static Map<String, Station> recipeTypeCache;
    private static Map<Block, Station> blockCache;

    private ModStations() {
    }

    public static boolean isSupportedRecipeType(ResourceLocation typeId) {
        if (typeId == null) return false;
        String key = typeId.toString();
        return VANILLA_RECIPE_TYPES.contains(key) || recipeTypeStations().containsKey(key);
    }

    public static Map<String, Station> recipeTypeStations() {
        if (recipeTypeCache != null) return recipeTypeCache;
        Map<String, Station> map = new HashMap<>();
        for (Def def : DEFS) {
            if (!Services.PLATFORM.isModLoaded(def.modId())) continue;
            for (String id : def.recipeTypeIds()) {
                map.put(id, def.station());
            }
        }
        recipeTypeCache = map;
        return map;
    }

    public static Map<Block, Station> blocks() {
        if (blockCache != null) return blockCache;
        Map<Block, Station> map = new HashMap<>();
        for (Def def : DEFS) {
            if (!Services.PLATFORM.isModLoaded(def.modId())) continue;
            for (String id : def.blockIds()) {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                if (rl == null) continue;
                Block block = BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
                if (block != null && block != Blocks.AIR) map.put(block, def.station());
            }
        }
        blockCache = map;
        return map;
    }
}
