package com.sxilverr.quickcraft.integration.projecte;

import com.sxilverr.quickcraft.craft.CraftExecutor;
import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.craft.VirtualPool;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.ItemKey;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ProjectEClient {
    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "P", "E"};
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    private static final BigInteger UNLIMITED = BigInteger.ONE.shiftLeft(96);
    private static final BigInteger CAPACITY_CAP = BigInteger.valueOf(1000000);

    private ProjectEClient() {
    }

    public static EmcPlan plan(EntityPlayer player, int range, CraftNode root, Map<ItemKey, Integer> have,
                               ItemStack target, int quantity, Set<ItemKey> keys) {
        EmcSession session = EmcSession.openClient(player, range);
        if (session == null) return EmcPlan.none();
        BigInteger owned = session.emc();
        String total = format(owned);
        if (root == null || target == null || target.isEmpty()) {
            return new EmcPlan(true, Collections.<ItemKey, Integer>emptyMap(), null, total, true,
                    Collections.<ItemKey, Integer>emptyMap());
        }

        Map<ItemKey, Integer> capacity = new HashMap<ItemKey, Integer>();
        for (Map.Entry<ItemKey, Long> entry : session.values(keys).entrySet()) {
            long unit = entry.getValue();
            if (unit <= 0L) continue;
            BigInteger max = owned.divide(BigInteger.valueOf(unit));
            capacity.put(entry.getKey(), max.compareTo(CAPACITY_CAP) >= 0
                    ? CAPACITY_CAP.intValue() : max.intValue());
        }

        EmcBank affordableBank = session.bank(keys, owned);
        spend(affordableBank, root, have, target, quantity);
        Map<ItemKey, Integer> supplied = new HashMap<ItemKey, Integer>(affordableBank.purchased());

        EmcBank fullBank = session.bank(keys, UNLIMITED);
        spend(fullBank, root, have, target, quantity);
        BigInteger required = fullBank.spentEmc();

        boolean affordable = required.compareTo(owned) <= 0;
        return new EmcPlan(true, supplied, required.signum() > 0 ? format(required) : null, total, affordable,
                capacity);
    }

    private static void spend(EmcBank bank, CraftNode root, Map<ItemKey, Integer> have,
                              ItemStack target, int quantity) {
        VirtualPool pool = new VirtualPool();
        for (Map.Entry<ItemKey, Integer> entry : have.entrySet()) pool.add(entry.getKey(), entry.getValue());
        pool.setEmc(bank);
        CraftExecutor.simulate(root, pool);
        ItemKey targetKey = ItemKey.of(target);
        Integer already = have.get(targetKey);
        int made = Math.max(0, pool.count(targetKey) - (already == null ? 0 : already));
        if (made < quantity && bank.supplies(targetKey)) {
            int buy = Math.min(quantity - made, bank.affordable(targetKey));
            if (buy > 0) bank.buy(targetKey, buy);
        }
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
