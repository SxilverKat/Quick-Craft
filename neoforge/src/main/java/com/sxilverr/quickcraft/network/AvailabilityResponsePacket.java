package com.sxilverr.quickcraft.network;

import com.sxilverr.quickcraft.client.ClientNetworkHandler;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public class AvailabilityResponsePacket implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 65536;

    public static final Type<AvailabilityResponsePacket> TYPE = new Type<>(QuickCraftNetwork.id("availability_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AvailabilityResponsePacket> STREAM_CODEC =
            StreamCodec.of(AvailabilityResponsePacket::write, AvailabilityResponsePacket::read);

    private final Map<ItemKey, Integer> counts;
    private final Map<ItemKey, ItemStack> sources;
    private final Stations stations;

    public AvailabilityResponsePacket(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources, Stations stations) {
        this.counts = counts;
        this.sources = sources;
        this.stations = stations;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buf, AvailabilityResponsePacket msg) {
        buf.writeVarInt(msg.counts.size());
        for (Map.Entry<ItemKey, Integer> entry : msg.counts.entrySet()) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.getKey().toStack(1));
            buf.writeVarInt(entry.getValue());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, msg.sources.getOrDefault(entry.getKey(), ItemStack.EMPTY));
        }
        writeStations(msg.stations, buf);
    }

    private static AvailabilityResponsePacket read(RegistryFriendlyByteBuf buf) {
        int count = Math.min(MAX_ENTRIES, buf.readVarInt());
        Map<ItemKey, Integer> counts = new HashMap<>();
        Map<ItemKey, ItemStack> sources = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int amount = buf.readVarInt();
            ItemStack source = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            if (stack.isEmpty()) continue;
            ItemKey key = ItemKey.of(stack);
            counts.put(key, amount);
            if (!source.isEmpty()) sources.put(key, source);
        }
        Stations stations = readStations(buf);
        return new AvailabilityResponsePacket(counts, sources, stations);
    }

    private static void writeStations(Stations s, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(s.gridSize());
        buf.writeBoolean(s.smithingTable());
        buf.writeBoolean(s.stonecutter());
        buf.writeBoolean(s.gunSmithTable());
        buf.writeBoolean(s.ammoAssemblyTable());
        buf.writeBoolean(s.attachmentTable());
        buf.writeBoolean(s.extremeCrafting());
        writeItem(s.craftingSource(), buf);
        writeItem(s.smithingSource(), buf);
        writeItem(s.stonecutterSource(), buf);
    }

    private static Stations readStations(RegistryFriendlyByteBuf buf) {
        int gridSize = buf.readVarInt();
        boolean smithing = buf.readBoolean();
        boolean stonecutter = buf.readBoolean();
        boolean gunSmith = buf.readBoolean();
        boolean ammoAssembly = buf.readBoolean();
        boolean attachment = buf.readBoolean();
        boolean extreme = buf.readBoolean();
        Item craftingSource = readItem(buf);
        Item smithingSource = readItem(buf);
        Item stonecutterSource = readItem(buf);
        return new Stations(gridSize, smithing, stonecutter, gunSmith, ammoAssembly, attachment, extreme,
                craftingSource, smithingSource, stonecutterSource);
    }

    private static void writeItem(Item item, RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, item == null ? ItemStack.EMPTY : new ItemStack(item));
    }

    private static Item readItem(RegistryFriendlyByteBuf buf) {
        ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        return stack.isEmpty() ? null : stack.getItem();
    }

    public static void handle(AvailabilityResponsePacket msg, IPayloadContext ctx) {
        ClientNetworkHandler.onAvailability(msg.counts, msg.sources, msg.stations);
    }
}
