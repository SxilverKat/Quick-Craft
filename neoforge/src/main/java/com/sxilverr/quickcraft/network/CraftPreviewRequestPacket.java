package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.neoforge.craft.CraftService;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftPreviewRequestPacket implements CustomPacketPayload {
    private static final int MAX_OVERRIDES = 8192;

    public static final Type<CraftPreviewRequestPacket> TYPE = new Type<>(QuickCraftNetwork.id("craft_preview_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPreviewRequestPacket> STREAM_CODEC =
            StreamCodec.of(CraftPreviewRequestPacket::write, CraftPreviewRequestPacket::read);

    private final ItemStack target;
    private final int quantity;
    private final Map<ItemKey, ResourceLocation> overrides;
    private final Map<String, Item> ingredientChoices;

    public CraftPreviewRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                     Map<String, Item> ingredientChoices) {
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, CraftPreviewRequestPacket msg) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.target);
        buf.writeVarInt(msg.quantity);
        buf.writeVarInt(msg.overrides.size());
        for (Map.Entry<ItemKey, ResourceLocation> entry : msg.overrides.entrySet()) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.getKey().toStack(1));
            buf.writeResourceLocation(entry.getValue());
        }
        buf.writeVarInt(msg.ingredientChoices.size());
        for (Map.Entry<String, Item> entry : msg.ingredientChoices.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(itemId(entry.getValue()));
        }
    }

    private static CraftPreviewRequestPacket read(RegistryFriendlyByteBuf buf) {
        ItemStack target = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int quantity = buf.readVarInt();
        int count = Math.min(MAX_OVERRIDES, buf.readVarInt());
        Map<ItemKey, ResourceLocation> overrides = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ItemStack representative = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            ResourceLocation recipe = buf.readResourceLocation();
            overrides.put(ItemKey.of(representative), recipe);
        }
        int choiceCount = Math.min(MAX_OVERRIDES, buf.readVarInt());
        Map<String, Item> ingredientChoices = new HashMap<>();
        for (int i = 0; i < choiceCount; i++) {
            String signature = buf.readUtf();
            Item item = BuiltInRegistries.ITEM.getOptional(buf.readResourceLocation()).orElse(null);
            if (item != null) ingredientChoices.put(signature, item);
        }
        return new CraftPreviewRequestPacket(target, quantity, overrides, ingredientChoices);
    }

    private static ResourceLocation itemId(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? ResourceLocation.withDefaultNamespace("air") : id;
    }

    public static void handle(CraftPreviewRequestPacket msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || msg.target.isEmpty()) return;
        CraftPreview.Result result;
        try {
            result = CraftService.preview(player, msg.target, msg.quantity, msg.overrides, msg.ingredientChoices);
        } catch (Throwable t) {
            QuickCraftCommon.LOGGER.error("Quick Craft preview failed for {}", msg.target, t);
            result = new CraftPreview.Result(0, Math.max(1, msg.quantity), List.of());
        }
        QuickCraftNetwork.sendCraftPreview(player, result);
    }
}
