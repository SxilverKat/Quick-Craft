package com.sxilverr.quickcraft.forge.integration.projecte;

import com.sxilverr.quickcraft.integration.projecte.EmcPlan;
import com.sxilverr.quickcraft.craft.CraftExecutor;
import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.craft.VirtualPool;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ProjectEClient {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "P", "E"};
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);

    private ProjectEClient() {
    }

    public static EmcPlan plan(Player player, int range, CraftNode root, Map<ItemKey, Integer> have,
                               ItemStack target, int quantity, Set<ItemKey> keys) {
        EmcSession session = EmcSession.openClient(player, range);
        if (session == null) return EmcPlan.none();
        String total = format(session.emc());
        if (root == null || target == null || target.isEmpty()) {
            return new EmcPlan(true, Map.of(), null, total);
        }
        EmcBank bank = session.bank(keys);
        VirtualPool pool = new VirtualPool();
        for (Map.Entry<ItemKey, Integer> entry : have.entrySet()) pool.add(entry.getKey(), entry.getValue());
        pool.setEmc(bank);
        CraftExecutor.simulate(root, pool);
        ItemKey targetKey = ItemKey.of(target);
        int made = Math.max(0, pool.count(targetKey) - have.getOrDefault(targetKey, 0));
        if (made < quantity && bank.supplies(targetKey)) {
            int buy = Math.min(quantity - made, bank.affordable(targetKey));
            if (buy > 0) bank.buy(targetKey, buy);
        }
        Map<ItemKey, Integer> supplied = new HashMap<>(bank.purchased());
        BigInteger cost = bank.spentEmc();
        return new EmcPlan(true, supplied, cost.signum() > 0 ? format(cost) : null, total);
    }

    public static String format(BigInteger value) {
        if (value == null || value.signum() <= 0) return "0";
        int mag = 0;
        BigInteger n = value;
        while (n.compareTo(THOUSAND) >= 0 && mag < SUFFIXES.length - 1) {
            n = n.divide(THOUSAND);
            mag++;
        }
        if (mag == 0) return value.toString();
        BigInteger scale = BigInteger.TEN.pow(mag * 3);
        BigInteger tenths = value.multiply(BigInteger.TEN).divide(scale);
        long whole = tenths.longValue() / 10;
        long frac = tenths.longValue() % 10;
        return frac == 0 ? whole + SUFFIXES[mag] : whole + "." + frac + SUFFIXES[mag];
    }
}
