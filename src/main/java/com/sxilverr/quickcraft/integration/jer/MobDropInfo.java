package com.sxilverr.quickcraft.integration.jer;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class MobDropInfo {
    public final ResourceLocation entityId;
    public final String mobName;
    public final List<String> biomes;
    public final String lightLevel;
    public final String exp;
    public final List<DropLine> drops;

    public MobDropInfo(ResourceLocation entityId, String mobName, List<String> biomes,
                       String lightLevel, String exp, List<DropLine> drops) {
        this.entityId = entityId;
        this.mobName = mobName;
        this.biomes = biomes;
        this.lightLevel = lightLevel;
        this.exp = exp;
        this.drops = drops;
    }
}
