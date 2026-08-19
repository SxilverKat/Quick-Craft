package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.neoforge.craft.CraftService;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class DepositTargetsRequestPacket implements CustomPacketPayload {
    public static final DepositTargetsRequestPacket INSTANCE = new DepositTargetsRequestPacket();

    public static final Type<DepositTargetsRequestPacket> TYPE = new Type<>(QuickCraftNetwork.id("deposit_targets_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositTargetsRequestPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private DepositTargetsRequestPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DepositTargetsRequestPacket msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        List<LabeledSource> targets = CraftService.depositTargets(player);
        QuickCraftNetwork.sendDepositTargets(player, targets);
    }
}
