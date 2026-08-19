package com.sxilverr.quickcraft.neoforge.crafting;

import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;

public final class SbBackpackStations {
    public record Result(boolean crafting, boolean smithing, boolean stonecutter) {
    }

    private static final Result NONE = new Result(false, false, false);

    private SbBackpackStations() {
    }

    public static Result detect(Player player) {
        if (player.level().isClientSide) return NONE;
        boolean[] flags = new boolean[3];
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
            IBackpackWrapper wrapper = wrapperOf(backpack);
            if (wrapper != null) scan(wrapper.getUpgradeHandler(), flags);
            return false;
        });
        scanNearbyBlocks(player, flags);
        return flags[0] || flags[1] || flags[2] ? new Result(flags[0], flags[1], flags[2]) : NONE;
    }

    private static IBackpackWrapper wrapperOf(ItemStack stack) {
        try {
            return BackpackWrapper.fromStack(stack);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void scanNearbyBlocks(Player player, boolean[] flags) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) {
                        IBackpackWrapper wrapper = backpack.getBackpackWrapper();
                        if (wrapper != null) scan(wrapper.getUpgradeHandler(), flags);
                    }
                }
            }
        }
    }

    private static void scan(UpgradeHandler upgrades, boolean[] flags) {
        for (int u = 0; u < upgrades.getSlots(); u++) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(upgrades.getStackInSlot(u).getItem());
            if (id == null || !"sophisticatedbackpacks".equals(id.getNamespace())) continue;
            switch (id.getPath()) {
                case "crafting_upgrade" -> flags[0] = true;
                case "smithing_upgrade" -> flags[1] = true;
                case "stonecutter_upgrade" -> flags[2] = true;
                default -> {
                }
            }
        }
    }
}
