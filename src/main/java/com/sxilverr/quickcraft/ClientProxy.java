package com.sxilverr.quickcraft;

import com.sxilverr.quickcraft.client.QuickCraftKeys;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        super.preInit();
        QuickCraftKeys.register();
    }
}
