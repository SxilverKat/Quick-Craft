package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.forge.craft.CraftService;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class DepositTargetsRequestPacket {
    public DepositTargetsRequestPacket() {
    }

    public static void encode(DepositTargetsRequestPacket msg, FriendlyByteBuf buf) {
    }

    public static DepositTargetsRequestPacket decode(FriendlyByteBuf buf) {
        return new DepositTargetsRequestPacket();
    }

    public static void handle(DepositTargetsRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            List<LabeledSource> targets = CraftService.depositTargets(player);
            QuickCraftNetwork.sendDepositTargets(player, targets);
        });
        context.setPacketHandled(true);
    }
}
