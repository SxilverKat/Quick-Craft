package com.sxilverr.quickcraft.storage;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

public class HandlerItemSource implements ItemSource {
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
            if (!stack.isEmpty()) out.add(stack.copy());
        }
        return out;
    }

    @Override
    public int extract(ItemStack representative, int amount, boolean simulate) {
        int remaining = amount;
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty() || !sameItem(stack, representative)) continue;
            ItemStack taken = handler.extractItem(i, remaining, simulate);
            remaining -= taken.getCount();
        }
        return amount - remaining;
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
