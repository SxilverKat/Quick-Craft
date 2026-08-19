package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.neoforge.craft.CraftService;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

public class CraftRequestPacket implements CustomPacketPayload {
    private static final int MAX_OVERRIDES = 8192;

    public static final Type<CraftRequestPacket> TYPE = new Type<>(QuickCraftNetwork.id("craft_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftRequestPacket> STREAM_CODEC =
            StreamCodec.of(CraftRequestPacket::write, CraftRequestPacket::read);

    private final ItemStack target;
    private final int quantity;
    private final Map<ItemKey, ResourceLocation> overrides;
    private final Map<String, Item> ingredientChoices;
    private final String destinationId;

    public CraftRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                              Map<String, Item> ingredientChoices, String destinationId) {
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
        this.destinationId = destinationId == null ? "" : destinationId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, CraftRequestPacket msg) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.target);
        buf.writeVarInt(msg.quantity);
        buf.writeUtf(msg.destinationId);
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

    private static CraftRequestPacket read(RegistryFriendlyByteBuf buf) {
        ItemStack target = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int quantity = buf.readVarInt();
        String destinationId = buf.readUtf();
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
        return new CraftRequestPacket(target, quantity, overrides, ingredientChoices, destinationId);
    }

    private static ResourceLocation itemId(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? ResourceLocation.withDefaultNamespace("air") : id;
    }

    public static void handle(CraftRequestPacket msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || msg.target.isEmpty()) return;
        CraftSummary summary = CraftService.execute(player, msg.target, msg.quantity, msg.overrides,
                msg.ingredientChoices, msg.destinationId);
        player.displayClientMessage(feedback(summary, msg.target), false);
    }

    private static Component feedback(CraftSummary summary, ItemStack target) {
        Component name = target.getHoverName();
        if (summary.aborted()) {
            return Component.literal("Quick Craft: storage changed while crafting, nothing was taken for ")
                    .append(name).withStyle(ChatFormatting.RED);
        }
        if (summary.full()) {
            MutableComponent msg = Component.literal("Quick Craft: crafted " + summary.crafted() + "x ")
                    .append(name).withStyle(ChatFormatting.GREEN);
            return appendPlacements(msg, summary);
        }
        if (summary.partial()) {
            MutableComponent msg = Component.literal("Quick Craft: crafted " + summary.crafted() + "/" + requestedLabel(summary.requested()) + " ")
                    .append(name).append(Component.literal(" - ran out of materials")).withStyle(ChatFormatting.YELLOW);
            return appendPlacements(msg, summary);
        }
        if (summary.missingStation() != null) {
            return Component.literal("Quick Craft: needs a " + summary.missingStation() + " nearby to craft ")
                    .append(name).withStyle(ChatFormatting.RED);
        }
        return Component.literal("Quick Craft: not enough materials to craft ")
                .append(name).withStyle(ChatFormatting.RED);
    }

    private static MutableComponent appendPlacements(MutableComponent msg, CraftSummary summary) {
        List<CraftSummary.Placement> placements = summary.placements();
        if (placements.isEmpty() && summary.dropped() <= 0 && summary.byproducts() <= 0) return msg;
        StringBuilder sb = new StringBuilder(" → ");
        boolean first = true;
        for (CraftSummary.Placement placement : placements) {
            if (!first) sb.append(", ");
            sb.append(placement.count()).append(" to ").append(placement.where());
            first = false;
        }
        if (summary.dropped() > 0) {
            if (!first) sb.append(", ");
            sb.append(summary.dropped()).append(" dropped at your feet");
            first = false;
        }
        if (summary.byproducts() > 0) {
            if (!first) sb.append(", ");
            sb.append("+").append(summary.byproducts()).append(" leftover items");
        }
        return msg.append(Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY));
    }

    private static String requestedLabel(int requested) {
        return requested >= 1_000_000 ? "Max" : requested + "x";
    }
}
