package com.sxilverr.quickcraft.neoforge.integration.projecte;

import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class ProjectEIntegration {
    public static final String MODID = "projecte";
    private static final ResourceLocation TABLE_ID = ResourceLocation.fromNamespaceAndPath(MODID, "transmutation_table");
    private static final ResourceLocation TABLET_ID = ResourceLocation.fromNamespaceAndPath(MODID, "transmutation_tablet");

    private static Boolean loaded;

    private ProjectEIntegration() {
    }

    public static boolean available() {
        if (loaded == null) loaded = Services.PLATFORM.isModLoaded(MODID);
        return loaded;
    }

    public static Block tableBlock() {
        return BuiltInRegistries.BLOCK.getOptional(TABLE_ID).orElse(null);
    }

    public static ItemStack findTablet(Player player) {
        Item tablet = BuiltInRegistries.ITEM.getOptional(TABLET_ID).orElse(null);
        if (tablet == null || player == null) return ItemStack.EMPTY;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(tablet)) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static BlockPos findTable(Level level, BlockPos center, int range) {
        if (level == null || center == null || range <= 0) return null;
        Block table = tableBlock();
        if (table == null) return null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(cursor).is(table)) return cursor.immutable();
                }
            }
        }
        return null;
    }

    public static boolean hasAccess(Player player, Level level, BlockPos center, int range) {
        return !findTablet(player).isEmpty() || findTable(level, center, range) != null;
    }
}
