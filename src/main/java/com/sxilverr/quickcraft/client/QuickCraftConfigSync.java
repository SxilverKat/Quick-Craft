package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.QuickCraftConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = QuickCraft.MODID, value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class QuickCraftConfigSync {
    private QuickCraftConfigSync() {
    }

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (QuickCraft.MODID.equals(event.getModID())) QuickCraftConfig.reload();
    }
}
