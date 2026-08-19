package com.sxilverr.quickcraft.neoforge.crafting;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class Ae2Stations {
    private static final ResourceLocation CRAFTING_TERMINAL =
            ResourceLocation.fromNamespaceAndPath("ae2", "crafting_terminal");

    private Ae2Stations() {
    }

    public static boolean craftingTerminalNearby(Level level, BlockPos center, int range) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof IPartHost host && hasCraftingTerminal(host)) return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCraftingTerminal(IPartHost host) {
        if (isCraftingTerminal(host.getPart(null))) return true;
        for (Direction dir : Direction.values()) {
            if (isCraftingTerminal(host.getPart(dir))) return true;
        }
        return false;
    }

    private static boolean isCraftingTerminal(IPart part) {
        if (part == null) return false;
        IPartItem<?> item = part.getPartItem();
        if (item == null) return false;
        return CRAFTING_TERMINAL.equals(IPartItem.getId(item));
    }
}
