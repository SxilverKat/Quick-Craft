package com.sxilverr.quickcraft.storage;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

public class HandlerItemSource implements ItemSource {
    private static final int MAX_DRAIN_ROUNDS = 256;

    private final IItemHandler handler;
    private final ItemStack icon;

    public HandlerItemSource(IItemHandler handler) {
        this(handler, ItemStack.EMPTY);
    }

    public HandlerItemSource(IItemHandler handler, ItemStack icon) {
        this.handler = handler;
        this.icon = icon;
    }

    @Override
    public ItemStack sourceIcon() {
        return icon;
    }

    @Override
    public List<ItemStack> snapshot() {
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || !extractable(i)) continue;
            out.add(stack.copy());
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        int remaining = amount;
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || !sameItem(stack, representative)) continue;
            remaining -= simulate ? probe(i, stack, remaining) : drain(i, representative, remaining);
        }
        return amount - remaining;
    }

    private boolean extractable(int slot) {
        try {
            return !handler.extractItem(slot, 1, true).isEmpty();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private int probe(int slot, ItemStack stack, int wanted) {
        ItemStack sample = handler.extractItem(slot, wanted, true);
        if (sample.isEmpty()) return 0;
        return Math.min(wanted, Math.max(sample.getCount(), stack.getCount()));
    }

    private int drain(int slot, ItemStack representative, int wanted) {
        int taken = 0;
        for (int round = 0; round < MAX_DRAIN_ROUNDS && taken < wanted; round++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || !sameItem(stack, representative)) break;
            ItemStack got = handler.extractItem(slot, wanted - taken, false);
            if (got.isEmpty()) break;
            taken += got.getCount();
        }
        return taken;
    }

    private static boolean sameItem(ItemStack a, ItemStack b) {
        return ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    @Override
    public ItemStack insert(ItemStack stack, boolean simulate) {
        return ItemHandlerHelper.insertItemStacked(handler, stack.copy(), simulate);
    }

    @Override
    public int totalSlots() {
        return handler.getSlots();
    }

    @Override
    public int freeSlots() {
        int free = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (handler.getStackInSlot(i).isEmpty()) free++;
        }
        return free;
    }
}
