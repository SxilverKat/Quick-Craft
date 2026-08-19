package com.sxilverr.quickcraft.forge.storage;

import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.List;

public final class SophisticatedBackpackSource {
    private SophisticatedBackpackSource() {
    }

    public static void addBackpacks(ServerPlayer player, List<LabeledSource> out) {
        PlayerInventoryProvider.get().runOnBackpacks(player, (backpack, inventoryName, identifier, slot) -> {
            IBackpackWrapper wrapper = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance())
                    .resolve().orElse(null);
            if (wrapper != null) {
                ItemStack icon = backpack.copy();
                out.add(new LabeledSource("sbp:" + inventoryName + ":" + slot, backpack.getHoverName().getString(),
                        icon, null, new HandlerItemSource(wrapper.getInventoryHandler(), icon), true));
            }
            return false;
        });
    }

    public static void addBlock(BlockEntity be, BlockPos pos, List<LabeledSource> out) {
        if (!(be instanceof BackpackBlockEntity backpack)) return;
        IBackpackWrapper wrapper = backpack.getBackpackWrapper();
        if (wrapper == null) return;
        ItemStack backpackStack = wrapper.getBackpack();
        ItemStack icon = backpackStack.isEmpty() ? new ItemStack(be.getBlockState().getBlock()) : backpackStack.copy();
        out.add(new LabeledSource("sbpb:" + ItemSourceFactory.posKey(pos), icon.getHoverName().getString(), icon, pos,
                new HandlerItemSource(wrapper.getInventoryHandler(), icon), true));
    }
}
