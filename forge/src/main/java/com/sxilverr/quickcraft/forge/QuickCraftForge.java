package com.sxilverr.quickcraft.forge;

import com.sxilverr.quickcraft.forge.QuickCraftClientConfig;
import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.forge.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(QuickCraftCommon.MODID)
public class QuickCraftForge {

    public QuickCraftForge(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, QuickCraftConfig.SPEC);
        context.registerConfig(ModConfig.Type.CLIENT, QuickCraftClientConfig.SPEC);
        QuickCraftNetwork.register();
        MinecraftForge.EVENT_BUS.addListener(QuickCraftForge::onServerStopped);
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ServerRecipeCache.clear();
    }
}
