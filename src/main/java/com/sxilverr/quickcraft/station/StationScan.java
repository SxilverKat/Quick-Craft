package com.sxilverr.quickcraft.station;

import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.integration.ae2.Ae2Support;
import com.sxilverr.quickcraft.integration.avaritia.AvaritiaSupport;
import com.sxilverr.quickcraft.integration.rs.RsSupport;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Map;

public final class StationScan {
    private static final String RS_GRID = "refinedstorage:grid";

    private StationScan() {
    }

    public static Stations detect(World world, EntityPlayer player) {
        if (world == null || player == null) return Stations.inventoryOnly();

        BlockPos center = player.getPosition();
        boolean vanillaTable = false;
        boolean extreme = false;
        Item rsGrid = null;
        Item extremeSource = null;
        boolean avaritia = AvaritiaSupport.available();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (!world.isBlockLoaded(pos)) continue;
                    IBlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();
                    if (block == Blocks.CRAFTING_TABLE) {
                        vanillaTable = true;
                    } else if (avaritia && !extreme && AvaritiaSupport.isExtremeCraftingBlock(state)) {
                        extreme = true;
                        extremeSource = Item.getItemFromBlock(block);
                    } else if (rsGrid == null && isRefinedStorageCraftingGrid(block, state)) {
                        rsGrid = Item.getItemFromBlock(block);
                    }
                }
            }
        }

        boolean ae2Terminal = Reg.loaded(Ae2Support.MODID)
                && Ae2Stations.craftingTerminalNearby(world, center, Stations.RANGE);
        Item ae2Wireless = inventoryItem(player, "appliedenergistics2:wireless_crafting_terminal");
        Item rsWireless = inventoryItem(player, "refinedstorageaddons:wireless_crafting_grid");
        Item rsWirelessCreative = inventoryItem(player, "refinedstorageaddons:creative_wireless_crafting_grid");

        Item craftingSource = null;
        if (vanillaTable) craftingSource = Item.getItemFromBlock(Blocks.CRAFTING_TABLE);
        else if (rsGrid != null && rsGrid != Items.AIR) craftingSource = rsGrid;
        else if (ae2Terminal) craftingSource = Reg.item("appliedenergistics2:part");
        else if (ae2Wireless != null) craftingSource = ae2Wireless;
        else if (rsWireless != null) craftingSource = rsWireless;
        else if (rsWirelessCreative != null) craftingSource = rsWirelessCreative;

        if (extreme && extremeSource == null) extremeSource = Reg.item("avaritia:extreme_crafting_table");

        return new Stations(craftingSource != null ? 3 : 2, extreme, craftingSource, extremeSource);
    }

    private static boolean isRefinedStorageCraftingGrid(Block block, IBlockState state) {
        if (!RsSupport.available()) return false;
        ResourceLocation id = Reg.idOf(block);
        if (id == null || !RS_GRID.equals(id.toString())) return false;
        for (Map.Entry<IProperty<?>, Comparable<?>> entry : state.getProperties().entrySet()) {
            if (!"type".equals(entry.getKey().getName())) continue;
            String value = String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT);
            return value.contains("crafting") || value.contains("pattern");
        }
        return false;
    }

    private static Item inventoryItem(EntityPlayer player, String id) {
        Item item = Reg.item(id);
        if (item == null) return null;
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) return item;
        }
        return null;
    }
}
