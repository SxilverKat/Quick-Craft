package com.sxilverr.quickcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
            head.getOrCreateTag().put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), player.getGameProfile()));
            cached = head;
            cachedId = player.getUUID();
        }
        return cached;
    }
}
