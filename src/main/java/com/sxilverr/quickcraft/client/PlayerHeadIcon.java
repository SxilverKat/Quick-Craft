package com.sxilverr.quickcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

public final class PlayerHeadIcon {
    private static ItemStack cached = ItemStack.EMPTY;
    private static UUID cachedId;

    private PlayerHeadIcon() {
    }

    public static ItemStack get() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return new ItemStack(Items.SKULL, 1, 3);
        if (cached.isEmpty() || !player.getUniqueID().equals(cachedId)) {
            ItemStack head = new ItemStack(Items.SKULL, 1, 3);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("SkullOwner", player.getGameProfile().getName());
            head.setTagCompound(tag);
            cached = head;
            cachedId = player.getUniqueID();
        }
        return cached;
    }
}
