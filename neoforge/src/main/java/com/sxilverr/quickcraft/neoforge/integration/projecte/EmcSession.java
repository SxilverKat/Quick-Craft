package com.sxilverr.quickcraft.neoforge.integration.projecte;

import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.crafting.ItemKey;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EmcSession {
    private final IKnowledgeProvider provider;
    private final boolean fullKnowledge;
    private final ServerPlayer serverPlayer;

    private EmcSession(IKnowledgeProvider provider, ServerPlayer serverPlayer) {
        this.provider = provider;
        this.fullKnowledge = safeFullKnowledge(provider);
        this.serverPlayer = serverPlayer;
    }

    public static EmcSession open(ServerPlayer player, int range) {
        IKnowledgeProvider kp = providerFor(player);
        if (kp == null) return null;
        if (!ProjectEIntegration.hasAccess(player, player.serverLevel(), player.blockPosition(), range)) return null;
        return new EmcSession(kp, player);
    }

    public static EmcSession openClient(Player player, int range) {
        if (player == null) return null;
        IKnowledgeProvider kp = providerFor(player);
        if (kp == null) return null;
        if (!ProjectEIntegration.hasAccess(player, player.level(), player.blockPosition(), range)) return null;
        return new EmcSession(kp, null);
    }

    private static IKnowledgeProvider providerFor(Player player) {
        try {
            return player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean safeFullKnowledge(IKnowledgeProvider provider) {
        try {
            return provider.hasFullKnowledge();
        } catch (Throwable t) {
            return false;
        }
    }

    public BigInteger emc() {
        try {
            BigInteger value = provider.getEmc();
            return value == null ? BigInteger.ZERO : value;
        } catch (Throwable t) {
            return BigInteger.ZERO;
        }
    }

    public long value(ItemStack stack) {
        try {
            return IEMCProxy.INSTANCE.getValue(stack);
        } catch (Throwable t) {
            return 0L;
        }
    }

    public boolean learned(ItemStack stack) {
        try {
            return fullKnowledge || provider.hasKnowledge(stack);
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean emcable(ItemStack stack) {
        return value(stack) > 0L;
    }

    public Map<ItemKey, Long> values(Set<ItemKey> keys) {
        Map<ItemKey, Long> values = new HashMap<>();
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
            try {
                provider.setEmc(bank.remaining());
                changed = true;
            } catch (Throwable ignored) {
            }
        }
        if (!fullKnowledge && producedKeys != null) {
            for (ItemKey key : producedKeys) {
                ItemStack stack = key.toStack(1);
                try {
                    if (value(stack) > 0L && !provider.hasKnowledge(stack) && provider.addKnowledge(stack)) {
                        changed = true;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        if (changed) {
            try {
                provider.sync(serverPlayer);
            } catch (Throwable ignored) {
            }
        }
    }
}
