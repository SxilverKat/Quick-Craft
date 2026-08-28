package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.craft.CraftService;
import com.sxilverr.quickcraft.crafting.ItemKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CraftPreviewRequestPacket implements IMessage {
    private static final int MAX_OVERRIDES = 8192;

    private ItemStack target = ItemStack.EMPTY;
    private int quantity;
    private Map<ItemKey, ResourceLocation> overrides = new HashMap<ItemKey, ResourceLocation>();
    private Map<String, Item> ingredientChoices = new HashMap<String, Item>();

    public CraftPreviewRequestPacket() {
    }

    public CraftPreviewRequestPacket(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                                     Map<String, Item> ingredientChoices) {
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        Buf.writeStack(buf, target);
        buf.writeInt(quantity);
        buf.writeInt(overrides.size());
        for (Map.Entry<ItemKey, ResourceLocation> entry : overrides.entrySet()) {
            Buf.writeStack(buf, entry.getKey().toStack(1));
            Buf.writeId(buf, entry.getValue());
        }
        buf.writeInt(ingredientChoices.size());
        for (Map.Entry<String, Item> entry : ingredientChoices.entrySet()) {
            Buf.writeString(buf, entry.getKey());
            Buf.writeItem(buf, entry.getValue());
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        target = Buf.readStack(buf);
        quantity = buf.readInt();
        int count = Math.min(MAX_OVERRIDES, buf.readInt());
        overrides = new HashMap<ItemKey, ResourceLocation>();
        for (int i = 0; i < count; i++) {
            ItemStack representative = Buf.readStack(buf);
            ResourceLocation recipe = Buf.readId(buf);
            if (recipe != null) overrides.put(ItemKey.of(representative), recipe);
        }
        int choiceCount = Math.min(MAX_OVERRIDES, buf.readInt());
        ingredientChoices = new HashMap<String, Item>();
        for (int i = 0; i < choiceCount; i++) {
            String signature = Buf.readString(buf);
            Item item = Buf.readItem(buf);
            if (item != null) ingredientChoices.put(signature, item);
        }
    }

    public static class Handler implements IMessageHandler<CraftPreviewRequestPacket, IMessage> {
        @Override
        public IMessage onMessage(final CraftPreviewRequestPacket msg, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (msg.target.isEmpty()) return;
                    CraftPreview.Result result;
                    try {
                        result = CraftService.preview(player, msg.target, msg.quantity,
                                msg.overrides, msg.ingredientChoices);
                    } catch (Throwable t) {
                        QuickCraft.LOGGER.error("Quick Craft preview failed for " + msg.target, t);
                        result = new CraftPreview.Result(0, Math.max(1, msg.quantity),
                                Collections.<CraftPreview.Gain>emptyList());
                    }
                    QuickCraftNetwork.sendCraftPreview(player, result);
                }
            });
            return null;
        }
    }
}
