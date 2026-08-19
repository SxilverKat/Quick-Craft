package com.sxilverr.quickcraft.client;

import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public final class PlayerHeadIcon {
    private static ItemStack cached = ItemStack.EMPTY;
    private static UUID cachedId;

    private PlayerHeadIcon() {
    }

    public static ItemStack get() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return new ItemStack(Items.PLAYER_HEAD);
        if (cached.isEmpty() || !player.getUUID().equals(cachedId)) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
            cached = head;
            cachedId = player.getUUID();
        }
        return cached;
    }
}
