package com.sxilverr.quickcraft.platform;

import net.minecraft.world.item.ItemStack;

import java.nio.file.Path;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();

    ItemStack getCraftingRemainder(ItemStack stack);
}
