package com.sxilverr.quickcraft.forge.integration.projecte;

import com.sxilverr.quickcraft.forge.QuickCraftConfig;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EmcDeposit {
    public static final String TABLET_ID = "emc:tablet";
    public static final String TABLE_ID = "emc:table";

    private static final ItemSource SINK = new ItemSource() {
        @Override
        public List<ItemStack> snapshot() {
            return List.of();
        }

        @Override
        public int extract(ItemStack representative, int amount, boolean simulate) {
            return 0;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public Optional<ItemStack> sourceIconFor(ItemStack representative) {
            return Optional.empty();
        }
    };

    private EmcDeposit() {
    }

    public static boolean isEmc(String id) {
        return TABLET_ID.equals(id) || TABLE_ID.equals(id);
    }

    public static List<LabeledSource> targets(ServerPlayer player) {
        List<LabeledSource> out = new ArrayList<>();
        if (!QuickCraftConfig.useProjectEEmc() || !ProjectEIntegration.available() || player == null) return out;

        ItemStack tablet = ProjectEIntegration.findTablet(player);
        if (!tablet.isEmpty()) {
            ItemStack icon = tablet.copy();
            icon.setCount(1);
            out.add(new LabeledSource(TABLET_ID, icon.getHoverName().getString(), icon, null, SINK, true));
        }

        BlockPos table = ProjectEIntegration.findTable(player.level(), player.blockPosition(),
                QuickCraftConfig.containerScanRange());
        if (table != null) {
            Block block = ProjectEIntegration.tableBlock();
            ItemStack icon = block == null ? ItemStack.EMPTY : new ItemStack(block);
            String name = icon.isEmpty() ? "Transmutation Table" : icon.getHoverName().getString();
            out.add(new LabeledSource(TABLE_ID, name, icon, table, SINK, true));
        }
        return out;
    }

    public static String label(String id, List<LabeledSource> targets) {
        for (LabeledSource target : targets) {
            if (target.id().equals(id)) return target.name();
        }
        return "your EMC";
    }
}
