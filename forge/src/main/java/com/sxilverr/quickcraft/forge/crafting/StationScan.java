package com.sxilverr.quickcraft.forge.crafting;

import com.sxilverr.quickcraft.crafting.ModStations;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

public final class StationScan {
    private StationScan() {
    }

    public static Stations detect(Level level, Player player) {
        BlockPos center = player.blockPosition();
        boolean vanillaTable = false;
        boolean smithing = false;
        boolean stonecutter = false;
        boolean gunSmith = false;
        boolean ammoAssembly = false;
        boolean attachment = false;
        boolean extreme = false;
        Item rsGrid = null;
        Item smithingSource = null;
        Item stonecutterSource = null;
        Map<Block, Station> modBlocks = ModStations.blocks();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -Stations.RANGE; x <= Stations.RANGE; x++) {
            for (int y = -Stations.RANGE; y <= Stations.RANGE; y++) {
                for (int z = -Stations.RANGE; z <= Stations.RANGE; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    Block block = level.getBlockState(pos).getBlock();
                    if (block == Blocks.CRAFTING_TABLE) {
                        vanillaTable = true;
                    } else if (block == Blocks.SMITHING_TABLE) {
                        smithing = true;
                        smithingSource = Items.SMITHING_TABLE;
                    } else if (block == Blocks.STONECUTTER) {
                        stonecutter = true;
                        stonecutterSource = Items.STONECUTTER;
                    } else if (!modBlocks.isEmpty()) {
                        Station station = modBlocks.get(block);
                        if (station == Station.GUN_SMITH_TABLE) gunSmith = true;
                        else if (station == Station.AMMO_ASSEMBLY_TABLE) ammoAssembly = true;
                        else if (station == Station.ATTACHMENT_TABLE) attachment = true;
                        else if (station == Station.EXTREME_CRAFTING) extreme = true;
                        else if (station == Station.CRAFTING && rsGrid == null) rsGrid = block.asItem();
                    }
                }
            }
        }

        boolean ae2Terminal = ModList.get().isLoaded("ae2") && Ae2Stations.craftingTerminalNearby(level, center, Stations.RANGE);
        Item ae2Wireless = inventoryItem(player, "ae2:wireless_crafting_terminal");
        Item rsWireless = inventoryItemPowered(player, "refinedstorageaddons:wireless_crafting_grid");
        Item rsWirelessCreative = inventoryItem(player, "refinedstorageaddons:creative_wireless_crafting_grid");
        Item tomsWireless = inventoryItem(player, "toms_storage:ts.adv_wireless_terminal");

        Item craftingSource = null;
        if (vanillaTable) craftingSource = Items.CRAFTING_TABLE;
        else if (rsGrid != null && rsGrid != Items.AIR) craftingSource = rsGrid;
        else if (ae2Terminal) craftingSource = registryItem("ae2:crafting_terminal");
        else if (ae2Wireless != null) craftingSource = ae2Wireless;
        else if (rsWireless != null) craftingSource = rsWireless;
        else if (rsWirelessCreative != null) craftingSource = rsWirelessCreative;
        else if (tomsWireless != null) craftingSource = tomsWireless;

        if (ModList.get().isLoaded("sophisticatedbackpacks")) {
            SbBackpackStations.Result bp = SbBackpackStations.detect(player);
            if (bp.crafting() && craftingSource == null) craftingSource = registryItem("sophisticatedbackpacks:crafting_upgrade");
            if (bp.smithing()) {
                smithing = true;
                if (smithingSource == null) smithingSource = registryItem("sophisticatedbackpacks:smithing_upgrade");
            }
            if (bp.stonecutter()) {
                stonecutter = true;
                if (stonecutterSource == null) stonecutterSource = registryItem("sophisticatedbackpacks:stonecutter_upgrade");
            }
        }

        boolean crafting = craftingSource != null;
        return new Stations(crafting ? 3 : 2, smithing, stonecutter, gunSmith, ammoAssembly, attachment, extreme,
                craftingSource, smithingSource, stonecutterSource);
    }

    private static Item inventoryItem(Player player, String id) {
        Item item = registryItem(id);
        if (item == null) return null;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() == item) return item;
        }
        return null;
    }

    private static Item inventoryItemPowered(Player player, String id) {
        Item item = registryItem(id);
        if (item == null) return null;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == item && hasEnergy(stack)) return item;
        }
        return null;
    }

    private static boolean hasEnergy(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).map(energy -> energy.getEnergyStored() > 0).orElse(true);
    }

    private static Item registryItem(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) return null;
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        return item == Items.AIR ? null : item;
    }
}
