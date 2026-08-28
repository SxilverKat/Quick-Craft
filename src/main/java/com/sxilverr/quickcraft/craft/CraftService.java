package com.sxilverr.quickcraft.craft;

import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.CraftTrees;
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

        ItemSource source = extractionSource(labeled);
        List<ItemStack> snapshot = source.snapshot();
        Availability availability = Availability.Factory.of(ownedCounts(snapshot));
        Stations stations = StationScan.detect(player.world, player);

        CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations,
                QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes());
        Station missing = CraftTrees.missingStation(root);

        ItemKey targetKey = ItemKey.of(target);
        VirtualPool initial = poolFrom(snapshot);
        VirtualPool working = initial.copy();

        EmcSession emc = openEmcSession(player);
        EmcBank bank = null;
        if (emc != null) {
            bank = emc.bank(collectKeys(root, new HashSet<ItemKey>()));
            working.setEmc(bank);
        }

        CraftExecutor.simulate(root, working);

        if (bank != null) {
            int made = Math.max(0, working.count(targetKey) - initial.count(targetKey));
            if (made < qty && bank.supplies(targetKey)) {
                int buy = Math.min(qty - made, bank.affordable(targetKey));
                if (buy > 0 && bank.buy(targetKey, buy)) working.produce(targetKey, buy);
            }
        }

        if (!commit(source, deposit, initial, working, targetKey, emc, bank, player, destinationId)) {
            return CraftSummary.aborted(qty);
        }
        if (emc != null) emc.apply(bank, working.producedKeys());

        int crafted = Math.max(0, working.count(targetKey) - initial.count(targetKey));
        if (crafted > 0) playCraftSound(player);
        return new CraftSummary(Math.min(crafted, qty), qty, missing == null ? null : missing.displayName(),
                deposit.placements(), deposit.dropped(), deposit.byproducts());
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
        CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations,
                QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes());

        EmcSession emc = openEmcSession(player);
        EmcBank bank = emc == null ? null : emc.bank(collectKeys(root, new HashSet<ItemKey>()));
        return CraftPreview.simulate(root, owned, target, qty, bank);
    }

    private static int creativeQuantity(ItemStack target, int requested) {
        return Math.min(requested, Math.max(1, target.getMaxStackSize()) * INVENTORY_SLOTS);
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

    private static ItemSource extractionSource(List<LabeledSource> labeled) {
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

    private static boolean commit(ItemSource source, Deposit deposit, VirtualPool initial, VirtualPool working,
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
            if (source.extractMatching(entry.getKey().toStack(1), entry.getValue(), true) < entry.getValue()) {
                return false;
            }
        }

        Map<ItemKey, Integer> taken = new LinkedHashMap<ItemKey, Integer>();
        for (Map.Entry<ItemKey, Integer> entry : consumed.entrySet()) {
            int got = source.extractMatching(entry.getKey().toStack(1), entry.getValue(), false);
            if (got > 0) taken.put(entry.getKey(), got);
            if (got < entry.getValue()) {
                refund(source, taken, player);
                return false;
            }
        }

        for (Map.Entry<ItemKey, Integer> entry : produced.entrySet()) {
            ItemKey key = entry.getKey();
            int amount = entry.getValue();
            if (depositToEmc) {
                long value = emc.value(key.toStack(1));
                if (value > 0L) {
                    bank.gain(BigInteger.valueOf(value).multiply(BigInteger.valueOf(amount)));
                    deposit.toEmc(amount, key.equals(targetKey));
                    continue;
                }
            }
            deposit.put(key, amount, key.equals(targetKey));
        }
        return true;
    }

    private static void refund(ItemSource source, Map<ItemKey, Integer> taken, EntityPlayerMP player) {
        for (Map.Entry<ItemKey, Integer> entry : taken.entrySet()) {
            ItemKey key = entry.getKey();
            int max = Math.max(1, key.toStack(1).getMaxStackSize());
            int left = entry.getValue();
            while (left > 0) {
                int n = Math.min(left, max);
                ItemStack remainder = source.insert(key.toStack(n), false);
                if (!remainder.isEmpty()) player.dropItem(remainder, false);
                left -= n;
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
