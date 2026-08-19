package com.sxilverr.quickcraft.neoforge.crafting;

import com.sxilverr.quickcraft.crafting.ModStations;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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
        Item modGrid = null;
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
                        else if (station == Station.CRAFTING && modGrid == null) modGrid = block.asItem();
                    }
                }
            }
        }

        boolean ae2 = Services.PLATFORM.isModLoaded("ae2");
        boolean ae2Terminal = ae2 && Ae2Stations.craftingTerminalNearby(level, center, Stations.RANGE);
        Item ae2Wireless = ae2 ? inventoryItem(player, "ae2:wireless_crafting_terminal") : null;
        Item tomsWireless = Services.PLATFORM.isModLoaded("toms_storage")
                ? inventoryItem(player, "toms_storage:adv_wireless_terminal") : null;

        Item craftingSource = null;
        if (vanillaTable) craftingSource = Items.CRAFTING_TABLE;
        else if (modGrid != null && modGrid != Items.AIR) craftingSource = modGrid;
        else if (ae2Terminal) craftingSource = registryItem("ae2:crafting_terminal");
        else if (ae2Wireless != null) craftingSource = ae2Wireless;
        else if (tomsWireless != null) craftingSource = tomsWireless;

        if (Services.PLATFORM.isModLoaded("sophisticatedbackpacks")) {
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

    private static Item registryItem(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        return item == null || item == Items.AIR ? null : item;
    }
}
