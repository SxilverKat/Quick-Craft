package com.sxilverr.quickcraft.forge.integration.projecte;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.function.Predicate;

final class CuriosTablet {
    private CuriosTablet() {
    }

    static ItemStack find(Player player, Predicate<ItemStack> matcher) {
        try {
            ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).resolve().orElse(null);
            if (handler == null) return ItemStack.EMPTY;
            return handler.findFirstCurio(matcher).map(SlotResult::stack).orElse(ItemStack.EMPTY);
        } catch (RuntimeException | LinkageError e) {
            return ItemStack.EMPTY;
        }
    }
}
