package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.util.Reg;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public final class Buf {
    private static final ResourceLocation AIR = new ResourceLocation("minecraft", "air");

    private Buf() {
    }

    public static void writeStack(ByteBuf buf, ItemStack stack) {
        ByteBufUtils.writeItemStack(buf, stack == null ? ItemStack.EMPTY : stack);
    }

    public static ItemStack readStack(ByteBuf buf) {
        ItemStack stack = ByteBufUtils.readItemStack(buf);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static void writeString(ByteBuf buf, String value) {
        ByteBufUtils.writeUTF8String(buf, value == null ? "" : value);
    }

    public static String readString(ByteBuf buf) {
        String value = ByteBufUtils.readUTF8String(buf);
        return value == null ? "" : value;
    }

    public static void writeId(ByteBuf buf, ResourceLocation id) {
        writeString(buf, id == null ? AIR.toString() : id.toString());
    }

    public static ResourceLocation readId(ByteBuf buf) {
        return Reg.rl(readString(buf));
    }

    public static void writeItem(ByteBuf buf, Item item) {
        writeId(buf, item == null ? AIR : Reg.idOf(item));
    }

    public static Item readItem(ByteBuf buf) {
        ResourceLocation id = readId(buf);
        return id == null ? null : Reg.item(id.toString());
    }
}
