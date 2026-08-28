package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class CraftPreviewResponsePacket implements IMessage {
    private static final int MAX_ENTRIES = 65536;

    int craftable;
    int requested;
    List<CraftPreview.Gain> gained = new ArrayList<CraftPreview.Gain>();

    public CraftPreviewResponsePacket() {
    }

    public CraftPreviewResponsePacket(int craftable, int requested, List<CraftPreview.Gain> gained) {
        this.craftable = craftable;
        this.requested = requested;
        this.gained = gained;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(craftable);
        buf.writeInt(requested);
        buf.writeInt(gained.size());
        for (CraftPreview.Gain gain : gained) {
            Buf.writeStack(buf, gain.key().toStack(1));
            buf.writeInt(gain.count());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        craftable = buf.readInt();
        requested = buf.readInt();
        int count = Math.min(MAX_ENTRIES, buf.readInt());
        gained = new ArrayList<CraftPreview.Gain>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = Buf.readStack(buf);
            int amount = buf.readInt();
            if (!stack.isEmpty()) gained.add(new CraftPreview.Gain(ItemKey.of(stack), amount));
        }
    }

    public static class Handler implements IMessageHandler<CraftPreviewResponsePacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(final CraftPreviewResponsePacket msg, MessageContext ctx) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    com.sxilverr.quickcraft.client.ClientNetworkHandler.onCraftPreview(
                            msg.craftable, msg.requested, msg.gained);
                }
            });
            return null;
        }
    }
}
