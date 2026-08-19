package com.sxilverr.quickcraft.integration.jer;

public final class MobItemSource {
    public final MobDropInfo mob;
    public final DropLine drop;

    public MobItemSource(MobDropInfo mob, DropLine drop) {
        this.mob = mob;
        this.drop = drop;
    }
}
