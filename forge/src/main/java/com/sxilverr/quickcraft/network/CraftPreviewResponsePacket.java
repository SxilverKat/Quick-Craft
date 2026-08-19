package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientNetworkHandler;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CraftPreviewResponsePacket {
    private static final int MAX_ENTRIES = 65536;

    private final int craftable;
    private final int requested;
    private final List<CraftPreview.Gain> gained;

    public CraftPreviewResponsePacket(int craftable, int requested, List<CraftPreview.Gain> gained) {
        this.craftable = craftable;
        this.requested = requested;
        this.gained = gained;
    }

    public static void encode(CraftPreviewResponsePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.craftable);
        buf.writeVarInt(msg.requested);
        buf.writeVarInt(msg.gained.size());
        for (CraftPreview.Gain gain : msg.gained) {
            buf.writeItem(gain.key().toStack(1));
            buf.writeVarInt(gain.count());
        }
    }

    public static CraftPreviewResponsePacket decode(FriendlyByteBuf buf) {
        int craftable = buf.readVarInt();
        int requested = buf.readVarInt();
        int count = Math.min(MAX_ENTRIES, buf.readVarInt());
        List<CraftPreview.Gain> gained = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = buf.readItem();
            int amount = buf.readVarInt();
            if (!stack.isEmpty()) gained.add(new CraftPreview.Gain(ItemKey.of(stack), amount));
        }
        return new CraftPreviewResponsePacket(craftable, requested, gained);
    }

    public static void handle(CraftPreviewResponsePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientNetworkHandler.onCraftPreview(msg.craftable, msg.requested, msg.gained)));
        context.setPacketHandled(true);
    }
}
