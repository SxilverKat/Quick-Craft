package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.forge.craft.CraftService;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CraftPreviewRequestPacket {
    private static final int MAX_OVERRIDES = 8192;

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

    public static void encode(CraftPreviewRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.target);
        buf.writeVarInt(msg.quantity);
        buf.writeVarInt(msg.overrides.size());
        for (Map.Entry<ItemKey, ResourceLocation> entry : msg.overrides.entrySet()) {
            buf.writeItem(entry.getKey().toStack(1));
            buf.writeResourceLocation(entry.getValue());
        }
        buf.writeVarInt(msg.ingredientChoices.size());
        for (Map.Entry<String, Item> entry : msg.ingredientChoices.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(itemId(entry.getValue()));
        }
    }

    public static CraftPreviewRequestPacket decode(FriendlyByteBuf buf) {
        ItemStack target = buf.readItem();
        int quantity = buf.readVarInt();
        int count = Math.min(MAX_OVERRIDES, buf.readVarInt());
        Map<ItemKey, ResourceLocation> overrides = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ItemStack representative = buf.readItem();
            ResourceLocation recipe = buf.readResourceLocation();
            overrides.put(ItemKey.of(representative), recipe);
        }
        int choiceCount = Math.min(MAX_OVERRIDES, buf.readVarInt());
        Map<String, Item> ingredientChoices = new HashMap<>();
        for (int i = 0; i < choiceCount; i++) {
            String signature = buf.readUtf();
            Item item = ForgeRegistries.ITEMS.getValue(buf.readResourceLocation());
            if (item != null) ingredientChoices.put(signature, item);
        }
        return new CraftPreviewRequestPacket(target, quantity, overrides, ingredientChoices);
    }

    private static ResourceLocation itemId(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id == null ? new ResourceLocation("minecraft", "air") : id;
    }

    public static void handle(CraftPreviewRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || msg.target.isEmpty()) return;
            CraftPreview.Result result;
            try {
                result = CraftService.preview(player, msg.target, msg.quantity,
                        msg.overrides, msg.ingredientChoices);
            } catch (Throwable t) {
                QuickCraftCommon.LOGGER.error("Quick Craft preview failed for {}", msg.target, t);
                result = new CraftPreview.Result(0, Math.max(1, msg.quantity), List.of());
            }
            QuickCraftNetwork.sendCraftPreview(player, result);
        });
        context.setPacketHandled(true);
    }
}
