package com.sxilverr.quickcraft.integration.projecte;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import java.math.BigInteger;

final class EmcHolders {
    private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

    private EmcHolders() {
    }

    static BigInteger stored(EntityPlayer player) {
        BigInteger total = BigInteger.ZERO;
        if (player == null) return total;
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!ProjectESupport.isEmcHolder(stack)) continue;
            long amount = ProjectESupport.storedEmc(stack);
            if (amount > 0L) total = total.add(BigInteger.valueOf(amount));
        }
        return total;
    }

    static void drain(EntityPlayer player, BigInteger amount) {
        if (player == null || amount == null || amount.signum() <= 0) return;
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory() && amount.signum() > 0; i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!ProjectESupport.isEmcHolder(stack)) continue;
            long got = ProjectESupport.extractEmc(stack, amount.min(LONG_MAX).longValue());
            if (got > 0L) amount = amount.subtract(BigInteger.valueOf(got));
        }
    }
}
