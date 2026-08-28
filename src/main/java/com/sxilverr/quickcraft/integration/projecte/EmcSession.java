package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EmcSession {
    private static final BigInteger LONG_CAP = BigInteger.valueOf(Long.MAX_VALUE);

    private final Object provider;
    private final boolean fullKnowledge;
    private final EntityPlayerMP serverPlayer;

    private EmcSession(Object provider, EntityPlayerMP serverPlayer) {
        this.provider = provider;
        this.fullKnowledge = ProjectESupport.fullKnowledge(provider);
        this.serverPlayer = serverPlayer;
    }

    public static EmcSession open(EntityPlayerMP player, int range) {
        Object provider = ProjectESupport.knowledgeProvider(player);
        if (provider == null) return null;
        if (!ProjectESupport.hasAccess(player, player.world, player.getPosition(), range)) return null;
        return new EmcSession(provider, player);
    }

    public static EmcSession openClient(EntityPlayer player, int range) {
        if (player == null) return null;
        Object provider = ProjectESupport.knowledgeProvider(player);
        if (provider == null) return null;
        if (!ProjectESupport.hasAccess(player, player.world, player.getPosition(), range)) return null;
        return new EmcSession(provider, null);
    }

    public BigInteger emc() {
        return BigInteger.valueOf(Math.max(0L, ProjectESupport.emc(provider)));
    }

    public long value(ItemStack stack) {
        return ProjectESupport.value(stack);
    }

    public boolean learned(ItemStack stack) {
        return fullKnowledge || ProjectESupport.hasKnowledge(provider, stack);
    }

    public boolean emcable(ItemStack stack) {
        return value(stack) > 0L;
    }

    public Map<ItemKey, Long> values(Set<ItemKey> keys) {
        Map<ItemKey, Long> values = new HashMap<ItemKey, Long>();
        for (ItemKey key : keys) {
            ItemStack stack = key.toStack(1);
            if (!learned(stack)) continue;
            long v = value(stack);
            if (v > 0L) values.put(key, v);
        }
        return values;
    }

    public EmcBank bank(Set<ItemKey> keys) {
        return new EmcBank(values(keys), emc());
    }

    public EmcBank bank(Set<ItemKey> keys, BigInteger budget) {
        return new EmcBank(values(keys), budget);
    }

    public void apply(EmcBank bank, Set<ItemKey> producedKeys) {
        if (serverPlayer == null) return;
        boolean changed = false;
        if (bank != null && bank.changed()) {
            BigInteger remaining = bank.remaining();
            if (remaining.signum() < 0) remaining = BigInteger.ZERO;
            if (remaining.compareTo(LONG_CAP) > 0) remaining = LONG_CAP;
            ProjectESupport.setEmc(provider, remaining.longValue());
            changed = true;
        }
        if (!fullKnowledge && producedKeys != null) {
            for (ItemKey key : producedKeys) {
                ItemStack stack = key.toStack(1);
                if (value(stack) > 0L && !ProjectESupport.hasKnowledge(provider, stack)
                        && ProjectESupport.addKnowledge(provider, stack)) {
                    changed = true;
                }
            }
        }
        if (changed) ProjectESupport.sync(provider, serverPlayer);
    }
}
