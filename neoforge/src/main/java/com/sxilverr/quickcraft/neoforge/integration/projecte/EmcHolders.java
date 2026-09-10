package com.sxilverr.quickcraft.neoforge.integration.projecte;

import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;

final class EmcHolders {
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private EmcHolders() {
    }

    static BigInteger stored(Player player) {
        BigInteger total = BigInteger.ZERO;
        if (player == null) return total;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            IItemEmcHolder holder = holder(stack);
            if (holder == null) continue;
            long amount = stored(holder, stack);
            if (amount > 0L) total = total.add(BigInteger.valueOf(amount));
        }
        return total;
    }

    static void drain(Player player, BigInteger amount) {
        if (player == null || amount == null || amount.signum() <= 0) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && amount.signum() > 0; i++) {
            ItemStack stack = inv.getItem(i);
            IItemEmcHolder holder = holder(stack);
            if (holder == null) continue;
            long got = extract(holder, stack, amount.min(LONG_MAX).longValue());
            if (got > 0L) amount = amount.subtract(BigInteger.valueOf(got));
        }
    }

    private static IItemEmcHolder holder(ItemStack stack) {
        if (stack.isEmpty()) return null;
        try {
            return stack.getCapability(PECapabilities.EMC_HOLDER_ITEM_CAPABILITY);
        } catch (Throwable t) {
            return null;
        }
    }

    private static long stored(IItemEmcHolder holder, ItemStack stack) {
        try {
            return holder.getStoredEmc(stack);
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static long extract(IItemEmcHolder holder, ItemStack stack, long amount) {
        try {
            return holder.extractEmc(stack, amount, IEmcStorage.EmcAction.EXECUTE);
        } catch (Throwable t) {
            return 0L;
        }
    }
}
