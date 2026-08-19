package com.sxilverr.quickcraft.forge.storage;

import com.sxilverr.quickcraft.storage.LabeledSource;
import com.sxilverr.quickcraft.DepositBlacklist;
import com.tom.storagemod.block.AbstractStorageTerminalBlock;
import com.tom.storagemod.block.AbstractStorageTerminalBlock.TerminalPos;
import com.tom.storagemod.item.AdvWirelessTerminalItem;
import com.tom.storagemod.tile.CraftingTerminalBlockEntity;
import com.tom.storagemod.tile.InventoryConnectorBlockEntity;
import com.tom.storagemod.tile.StorageTerminalBlockEntity;
import com.tom.storagemod.util.IProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Set;

public final class TomsStorageItemSource {
    private TomsStorageItemSource() {
    }

    public static void addBlocks(List<BlockEntity> tomsBlocks, Level level, Set<Object> seen,
                                 DepositBlacklist blacklist, List<LabeledSource> out) {
        for (BlockEntity be : tomsBlocks) {
            if (!(be instanceof StorageTerminalBlockEntity)) continue;
            BlockPos pos = be.getBlockPos();
            IItemHandler handler = terminalHandler(level, pos);
            if (handler == null || !seen.add(handler)) continue;
            String name = be instanceof CraftingTerminalBlockEntity ? "Crafting Terminal" : "Storage Terminal";
            ItemStack icon = new ItemStack(be.getBlockState().getBlock());
            out.add(new LabeledSource("toms:" + ItemSourceFactory.posKey(pos), name, icon, pos,
                    new HandlerItemSource(handler, icon), depositable(be, blacklist)));
        }
        for (BlockEntity be : tomsBlocks) {
            if (!(be instanceof InventoryConnectorBlockEntity connector)) continue;
            IItemHandler handler = resolve(connector.getInventory().orElse(null));
            if (handler == null || !seen.add(handler)) continue;
            BlockPos pos = be.getBlockPos();
            ItemStack icon = new ItemStack(be.getBlockState().getBlock());
            out.add(new LabeledSource("toms:" + ItemSourceFactory.posKey(pos), "Inventory Connector", icon, pos,
                    new HandlerItemSource(handler, icon), depositable(be, blacklist)));
        }
    }

    public static void addWireless(ServerPlayer player, Set<Object> seen, List<LabeledSource> out) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof AdvWirelessTerminalItem)) continue;
            IItemHandler handler = boundHandler(player, stack);
            if (handler == null || !seen.add(handler)) continue;
            ItemStack icon = stack.copy();
            out.add(new LabeledSource("tomsw:" + i, "Storage Terminal (Wireless)", icon, null,
                    new HandlerItemSource(handler, icon), true));
        }
    }

    private static boolean depositable(BlockEntity be, DepositBlacklist blacklist) {
        return !blacklist.matches(be.getBlockState().getBlock());
    }

    private static IItemHandler boundHandler(ServerPlayer player, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("BindX")) return null;
        MinecraftServer server = player.getServer();
        if (server == null) return null;
        ResourceLocation dimId = ResourceLocation.tryParse(tag.getString("BindDim"));
        if (dimId == null) return null;
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (level == null) return null;
        BlockPos pos = new BlockPos(tag.getInt("BindX"), tag.getInt("BindY"), tag.getInt("BindZ"));
        if (!level.isLoaded(pos)) return null;
        if (!(level.getBlockEntity(pos) instanceof StorageTerminalBlockEntity)) return null;
        return terminalHandler(level, pos);
    }

    private static IItemHandler terminalHandler(Level level, BlockPos termPos) {
        BlockState st = level.getBlockState(termPos);
        if (!(st.getBlock() instanceof AbstractStorageTerminalBlock)) return null;
        Direction d = st.getValue(AbstractStorageTerminalBlock.FACING);
        TerminalPos p = st.getValue(AbstractStorageTerminalBlock.TERMINAL_POS);
        if (p == TerminalPos.UP) d = Direction.UP;
        else if (p == TerminalPos.DOWN) d = Direction.DOWN;
        BlockEntity invTile = level.getBlockEntity(termPos.relative(d));
        if (invTile == null) return null;
        return resolve(invTile.getCapability(ForgeCapabilities.ITEM_HANDLER, d.getOpposite()).orElse(null));
    }

    private static IItemHandler resolve(IItemHandler handler) {
        return handler == null ? null : IProxy.resolve(handler);
    }
}
