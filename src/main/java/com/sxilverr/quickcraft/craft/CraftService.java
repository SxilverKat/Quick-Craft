package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.CraftTrees;
import com.sxilverr.quickcraft.crafting.EmcLookup;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.RecipeResolver;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.crafting.TreeBuilder;
import com.sxilverr.quickcraft.integration.projecte.EmcDeposit;
import com.sxilverr.quickcraft.integration.projecte.EmcSession;
import com.sxilverr.quickcraft.integration.projecte.ProjectESupport;
import com.sxilverr.quickcraft.station.StationScan;
import com.sxilverr.quickcraft.storage.CompositeItemSource;
import com.sxilverr.quickcraft.storage.DamageMatch;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.storage.ItemSourceFactory;
import com.sxilverr.quickcraft.storage.LabeledSource;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CraftService {
    private static final int MAX_QUANTITY = 1000000;
    private static final int INVENTORY_SLOTS = 36;
    private static final int MAX_COMMIT_ATTEMPTS = 8;

    private static final class Shortfall {
        private final ItemKey key;
        private final int wanted;
        private final int got;

        private Shortfall(ItemKey key, int wanted, int got) {
            this.key = key;
            this.wanted = wanted;
            this.got = got;
        }
    }

    private CraftService() {
    }

    public static CraftSummary execute(EntityPlayerMP player, ItemStack target, int quantity,
                                       Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices,
                                       String destinationId) {
        if (target.isEmpty()) return CraftSummary.empty();
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        Deposit deposit = Deposit.to(labeled, destinationId, player);
        if (EmcDeposit.isEmc(destinationId)) {
            deposit.setEmcLabel(EmcDeposit.label(destinationId, EmcDeposit.targets(player)));
        }

        if (QuickCraftConfig.creativeBypass() && player.capabilities.isCreativeMode) {
            int given = creativeQuantity(target, qty);
            deposit.put(ItemKey.of(target), given, true);
            playCraftSound(player);
            return new CraftSummary(given, given, null, deposit.placements(), deposit.dropped(), deposit.byproducts());
        }

        RecipeResolver resolver = ServerRecipeCache.get();
        TreeBuilder builder = new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());

        CompositeItemSource source = extractionSource(labeled);
        VirtualPool initial = poolFrom(source.snapshot());
        Stations stations = StationScan.detect(player.world, player);

        EmcSession emc = openEmcSession(player);
        builder.setEmcLookup(lookupFor(emc));

        ItemKey targetKey = ItemKey.of(target);
        Shortfall shortfall = null;
        for (int attempt = 0; attempt < MAX_COMMIT_ATTEMPTS; attempt++) {
            Availability availability = Availability.Factory.of(new HashMap<ItemKey, Integer>(initial.counts()));
            CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations,
                    QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes());

            EmcBank bank = emc == null ? null : emc.bank(collectKeys(root, new HashSet<ItemKey>()));
            VirtualPool working = initial.copy();
            working.setEmc(bank);
            CraftExecutor.simulate(root, working);
            buyTarget(bank, initial, working, targetKey, qty);

            shortfall = commit(source, deposit, initial, working, targetKey, emc, bank, player, destinationId);
            if (shortfall != null) {
                initial.limit(shortfall.key, shortfall.got);
                continue;
            }
            if (emc != null) emc.apply(bank, working.producedKeys());

            int crafted = Math.max(0, working.count(targetKey) - initial.count(targetKey));
            if (crafted > 0) playCraftSound(player);
            Station missing = CraftTrees.missingStation(root);
            return new CraftSummary(Math.min(crafted, qty), qty, missing == null ? null : missing.displayName(),
                    deposit.placements(), deposit.dropped(), deposit.byproducts());
        }
        return CraftSummary.aborted(qty, shortfall.key.toStack(1), shortfall.wanted);
    }

    public static CraftPreview.Result preview(EntityPlayerMP player, ItemStack target, int quantity,
                                              Map<ItemKey, ResourceLocation> overrides,
                                              Map<String, Item> ingredientChoices) {
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));
        if (target.isEmpty()) {
            return new CraftPreview.Result(0, qty, Collections.<CraftPreview.Gain>emptyList());
        }

        if (QuickCraftConfig.creativeBypass() && player.capabilities.isCreativeMode) {
            int given = creativeQuantity(target, qty);
            List<CraftPreview.Gain> gains = new ArrayList<CraftPreview.Gain>();
            gains.add(new CraftPreview.Gain(ItemKey.of(target), given));
            return new CraftPreview.Result(given, given, gains);
        }

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        ItemSource source = extractionSource(labeled);
        Map<ItemKey, Integer> owned = ownedCounts(source.snapshot());

        RecipeResolver resolver = ServerRecipeCache.get();
        TreeBuilder builder = new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());

        Availability availability = Availability.Factory.of(owned);
        Stations stations = StationScan.detect(player.world, player);

        EmcSession emc = openEmcSession(player);
        builder.setEmcLookup(lookupFor(emc));

        CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations,
                QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes());
        EmcBank bank = emc == null ? null : emc.bank(collectKeys(root, new HashSet<ItemKey>()));
        return CraftPreview.simulate(root, owned, target, qty, bank);
    }

    private static int creativeQuantity(ItemStack target, int requested) {
        return Math.min(requested, Math.max(1, target.getMaxStackSize()) * INVENTORY_SLOTS);
    }

    private static EmcLookup lookupFor(final EmcSession emc) {
        if (emc == null) return EmcLookup.NONE;
        return new EmcLookup() {
            @Override
            public boolean obtainable(ItemKey key) {
                ItemStack stack = key.toStack(1);
                return emc.learned(stack) && emc.value(stack) > 0L;
            }
        };
    }

    private static EmcSession openEmcSession(EntityPlayerMP player) {
        if (!QuickCraftConfig.useProjectEEmc() || !ProjectESupport.available()) return null;
        return EmcSession.open(player, QuickCraftConfig.containerScanRange());
    }

    private static Set<ItemKey> collectKeys(CraftNode node, Set<ItemKey> out) {
        out.add(ItemKey.of(node.output));
        for (CraftNode child : node.children) collectKeys(child, out);
        return out;
    }

    private static void buyTarget(EmcBank bank, VirtualPool initial, VirtualPool working, ItemKey targetKey, int qty) {
        if (bank == null || !bank.supplies(targetKey)) return;
        int made = Math.max(0, working.count(targetKey) - initial.count(targetKey));
        if (made >= qty) return;
        int buy = Math.min(qty - made, bank.affordable(targetKey));
        if (buy > 0 && bank.buy(targetKey, buy)) working.produce(targetKey, buy);
    }

    public static final class AvailabilitySnapshot {
        private final Map<ItemKey, Integer> counts;
        private final Map<ItemKey, ItemStack> sources;
        private final Map<ItemKey, ItemStack> samples;

        public AvailabilitySnapshot(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                    Map<ItemKey, ItemStack> samples) {
            this.counts = counts;
            this.sources = sources;
            this.samples = samples;
        }

        public Map<ItemKey, Integer> counts() {
            return counts;
        }

        public Map<ItemKey, ItemStack> sources() {
            return sources;
        }

        public Map<ItemKey, ItemStack> samples() {
            return samples;
        }
    }

    public static AvailabilitySnapshot availability(EntityPlayerMP player, Set<ItemKey> keys) {
        Map<ItemKey, Integer> counts = new HashMap<ItemKey, Integer>();
        Map<ItemKey, ItemStack> sources = new HashMap<ItemKey, ItemStack>();
        Map<ItemKey, ItemStack> samples = new HashMap<ItemKey, ItemStack>();
        if (keys.isEmpty()) return new AvailabilitySnapshot(counts, sources, samples);

        ItemSource source = ItemSourceFactory.forPlayer(player, QuickCraftConfig.containerScanRange());
        List<ItemStack> snapshot = null;
        for (ItemKey key : keys) {
            ItemStack rep = key.toStack(1);
            int available = source.extractMatching(rep, Integer.MAX_VALUE, true);
            counts.put(key, available);
            if (available <= 0) continue;
            ItemStack icon = source.sourceIconFor(rep);
            if (icon != null && !icon.isEmpty()) sources.put(key, icon);
            if (!DamageMatch.tolerant(rep)) continue;
            if (snapshot == null) snapshot = source.snapshot();
            ItemStack sample = DamageMatch.worst(snapshot, rep);
            if (!sample.isEmpty()) samples.put(key, sample);
        }
        return new AvailabilitySnapshot(counts, sources, samples);
    }

    public static List<LabeledSource> depositTargets(EntityPlayerMP player) {
        List<LabeledSource> out = new ArrayList<LabeledSource>();
        for (LabeledSource labeled : ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange())) {
            if (labeled.depositable()) out.add(labeled);
        }
        out.addAll(EmcDeposit.targets(player));
        return out;
    }

    private static CompositeItemSource extractionSource(List<LabeledSource> labeled) {
        List<ItemSource> sources = new ArrayList<ItemSource>(labeled.size());
        for (LabeledSource l : labeled) sources.add(l.source());
        return new CompositeItemSource(sources);
    }

    private static Map<ItemKey, Integer> ownedCounts(List<ItemStack> snapshot) {
        Map<ItemKey, Integer> owned = new HashMap<ItemKey, Integer>();
        for (ItemStack stack : snapshot) {
            ItemKey key = ItemKey.of(stack);
            Integer existing = owned.get(key);
            owned.put(key, existing == null ? stack.getCount() : existing + stack.getCount());
        }
        return owned;
    }

    private static VirtualPool poolFrom(List<ItemStack> snapshot) {
        VirtualPool pool = new VirtualPool();
        for (ItemStack stack : snapshot) pool.addStack(stack);
        return pool;
    }

    private static Shortfall commit(CompositeItemSource source, Deposit deposit, VirtualPool initial, VirtualPool working,
                                    ItemKey targetKey, EmcSession emc, EmcBank bank, EntityPlayerMP player,
                                    String destinationId) {
        boolean depositToEmc = emc != null && bank != null && EmcDeposit.isEmc(destinationId);

        Map<ItemKey, Integer> consumed = new LinkedHashMap<ItemKey, Integer>();
        Map<ItemKey, Integer> produced = new LinkedHashMap<ItemKey, Integer>();
        Set<ItemKey> keys = new HashSet<ItemKey>(initial.counts().keySet());
        keys.addAll(working.counts().keySet());
        for (ItemKey key : keys) {
            int delta = working.count(key) - initial.count(key);
            if (delta < 0) consumed.put(key, -delta);
            else if (delta > 0) produced.put(key, delta);
        }

        for (Map.Entry<ItemKey, Integer> entry : consumed.entrySet()) {
            int can = source.extractMatching(entry.getKey().toStack(1), entry.getValue(), true);
            if (can < entry.getValue()) return new Shortfall(entry.getKey(), entry.getValue(), can);
        }

        Map<ItemSource, List<ItemStack>> taken = new LinkedHashMap<ItemSource, List<ItemStack>>();
        for (Map.Entry<ItemKey, Integer> entry : consumed.entrySet()) {
            ItemStack rep = entry.getKey().toStack(1);
            int remaining = entry.getValue();
            for (ItemSource part : source.sources()) {
                if (remaining <= 0) break;
                List<ItemStack> pulled = part.pull(rep, remaining);
                if (pulled.isEmpty()) continue;
                List<ItemStack> bucket = taken.get(part);
                if (bucket == null) {
                    bucket = new ArrayList<ItemStack>();
                    taken.put(part, bucket);
                }
                bucket.addAll(pulled);
                for (ItemStack stack : pulled) remaining -= stack.getCount();
            }
            if (remaining > 0) {
                refund(source, taken, player);
                return new Shortfall(entry.getKey(), entry.getValue(), Math.max(0, entry.getValue() - remaining));
            }
        }

        for (Map.Entry<ItemKey, Integer> entry : produced.entrySet()) {
            ItemKey key = entry.getKey();
            int amount = entry.getValue();
            if (depositToEmc) {
                long value = emc.sellValue(key.toStack(1));
                if (value > 0L) {
                    bank.gain(BigInteger.valueOf(value).multiply(BigInteger.valueOf(amount)));
                    deposit.toEmc(amount, key.equals(targetKey));
                    continue;
                }
            }
            deposit.put(key, amount, key.equals(targetKey));
        }
        return null;
    }

    private static void refund(ItemSource fallback, Map<ItemSource, List<ItemStack>> taken, EntityPlayerMP player) {
        for (Map.Entry<ItemSource, List<ItemStack>> entry : taken.entrySet()) {
            for (ItemStack stack : entry.getValue()) {
                int max = Math.max(1, stack.getMaxStackSize());
                int left = stack.getCount();
                while (left > 0) {
                    int n = Math.min(left, max);
                    ItemStack chunk = stack.copy();
                    chunk.setCount(n);
                    ItemStack remainder = entry.getKey().insert(chunk, false);
                    if (!remainder.isEmpty()) remainder = fallback.insert(remainder, false);
                    if (!remainder.isEmpty()) player.dropItem(remainder, false);
                    left -= n;
                }
            }
        }
    }

    private static void playCraftSound(EntityPlayerMP player) {
        if (!QuickCraftConfig.craftSoundEnabled()) return;
        ResourceLocation id = QuickCraftConfig.craftSound();
        if (id == null) return;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(id);
        if (sound == null) return;
        player.world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }
}
