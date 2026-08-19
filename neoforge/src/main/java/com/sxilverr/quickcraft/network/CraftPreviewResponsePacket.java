package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientNetworkHandler;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class CraftPreviewResponsePacket implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 65536;

    public static final Type<CraftPreviewResponsePacket> TYPE = new Type<>(QuickCraftNetwork.id("craft_preview_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewResponsePacket> STREAM_CODEC =
            StreamCodec.of(CraftPreviewResponsePacket::write, CraftPreviewResponsePacket::read);

    private final int craftable;
    private final int requested;
    private final List<CraftPreview.Gain> gained;

    public CraftPreviewResponsePacket(int craftable, int requested, List<CraftPreview.Gain> gained) {
        this.craftable = craftable;
        this.requested = requested;
        this.gained = gained;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, CraftPreviewResponsePacket msg) {
        buf.writeVarInt(msg.craftable);
        buf.writeVarInt(msg.requested);
        buf.writeVarInt(msg.gained.size());
        for (CraftPreview.Gain gain : msg.gained) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, gain.key().toStack(1));
            buf.writeVarInt(gain.count());
        }
    }

    private static CraftPreviewResponsePacket read(RegistryFriendlyByteBuf buf) {
        int craftable = buf.readVarInt();
        int requested = buf.readVarInt();
        int count = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<CraftPreview.Gain> gained = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int amount = buf.readVarInt();
            if (!stack.isEmpty()) gained.add(new CraftPreview.Gain(ItemKey.of(stack), amount));
        }
        return new CraftPreviewResponsePacket(craftable, requested, gained);
    }

    public static void handle(CraftPreviewResponsePacket msg, IPayloadContext ctx) {
        ClientNetworkHandler.onCraftPreview(msg.craftable, msg.requested, msg.gained);
    }
}
