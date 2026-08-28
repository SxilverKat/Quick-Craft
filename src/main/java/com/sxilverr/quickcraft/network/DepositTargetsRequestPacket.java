package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.craft.CraftService;
import com.sxilverr.quickcraft.storage.LabeledSource;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

public class DepositTargetsRequestPacket implements IMessage {
    public DepositTargetsRequestPacket() {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<DepositTargetsRequestPacket, IMessage> {
        @Override
        public IMessage onMessage(DepositTargetsRequestPacket msg, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    List<LabeledSource> targets = CraftService.depositTargets(player);
                    QuickCraftNetwork.sendDepositTargets(player, targets);
                }
            });
            return null;
        }
    }
}
