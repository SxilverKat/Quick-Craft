package com.sxilverr.quickcraft.neoforge;

import com.sxilverr.quickcraft.neoforge.QuickCraftClientConfig;
import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.neoforge.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(QuickCraftCommon.MODID)
public final class QuickCraftNeoForge {

    public QuickCraftNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, QuickCraftConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, QuickCraftClientConfig.SPEC);
        modBus.addListener(QuickCraftNetwork::register);
        NeoForge.EVENT_BUS.addListener(QuickCraftNeoForge::onServerStopped);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            QuickCraftNeoForgeClient.init(modBus);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        ServerRecipeCache.clear();
    }
}
