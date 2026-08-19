package com.sxilverr.quickcraft.forge.integration.projecte;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class ProjectEIntegration {
    public static final String MODID = "projecte";
    private static final ResourceLocation TABLE_ID = new ResourceLocation(MODID, "transmutation_table");
    private static final ResourceLocation TABLET_ID = new ResourceLocation(MODID, "transmutation_tablet");

    private static Boolean loaded;

    private ProjectEIntegration() {
    }

    public static boolean available() {
        if (loaded == null) loaded = ModList.get().isLoaded(MODID);
        return loaded;
    }

    public static boolean hasAccess(Player player, Level level, BlockPos center, int range) {
        if (hasTablet(player)) return true;
        if (level == null || range <= 0) return false;
        Block table = ForgeRegistries.BLOCKS.getValue(TABLE_ID);
        if (table == null) return false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(cursor).is(table)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasTablet(Player player) {
        Item tablet = ForgeRegistries.ITEMS.getValue(TABLET_ID);
        if (tablet == null) return false;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(tablet)) return true;
        }
        return false;
    }
}
