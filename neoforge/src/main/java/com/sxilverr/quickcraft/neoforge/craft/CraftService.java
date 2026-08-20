package com.sxilverr.quickcraft.neoforge.craft;

import com.sxilverr.quickcraft.craft.CraftExecutor;
import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.craft.CraftSummary;
import com.sxilverr.quickcraft.craft.Deposit;
import com.sxilverr.quickcraft.craft.EmcBank;
import com.sxilverr.quickcraft.craft.VirtualPool;
import com.sxilverr.quickcraft.neoforge.QuickCraftConfig;
import com.sxilverr.quickcraft.crafting.Availability;
import com.sxilverr.quickcraft.crafting.CraftNode;
import com.sxilverr.quickcraft.crafting.CraftTrees;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.RecipeResolver;
import com.sxilverr.quickcraft.crafting.ServerRecipeCache;
import com.sxilverr.quickcraft.crafting.Station;
import com.sxilverr.quickcraft.neoforge.crafting.StationScan;
import com.sxilverr.quickcraft.crafting.Stations;
import com.sxilverr.quickcraft.crafting.TreeBuilder;
import com.sxilverr.quickcraft.neoforge.integration.projecte.EmcSession;
import com.sxilverr.quickcraft.neoforge.integration.projecte.ProjectEIntegration;
import com.sxilverr.quickcraft.storage.CompositeItemSource;
import com.sxilverr.quickcraft.storage.DamageMatch;
import com.sxilverr.quickcraft.storage.ItemSource;
import com.sxilverr.quickcraft.neoforge.storage.ItemSourceFactory;
import com.sxilverr.quickcraft.storage.LabeledSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CraftService {
    private static final int MAX_QUANTITY = 1000000;

    private CraftService() {
    }

    public static CraftSummary execute(ServerPlayer player, ItemStack target, int quantity,
                                       Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices,
                                       String destinationId) {
        if (target.isEmpty()) return CraftSummary.empty();
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        Deposit deposit = Deposit.to(labeled, destinationId, player);

        if (QuickCraftConfig.creativeBypass() && player.getAbilities().instabuild) {
            deposit.put(ItemKey.of(target), qty, true);
            playCraftSound(player);
            return new CraftSummary(qty, qty, null, deposit.placements(), deposit.dropped(), deposit.byproducts());
        }

        ServerLevel level = player.serverLevel();
        RecipeResolver resolver = ServerRecipeCache.get(level.getRecipeManager(), level.registryAccess());
        TreeBuilder builder = new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());

        ItemSource source = extractionSource(labeled);
        List<ItemStack> snapshot = source.snapshot();
        Availability availability = Availability.of(ownedCounts(snapshot));
        Stations stations = StationScan.detect(level, player);

        CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations,
                QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes());
        Station missing = CraftTrees.missingStation(root);

        ItemKey targetKey = ItemKey.of(target);
        VirtualPool initial = poolFrom(snapshot);
        VirtualPool working = initial.copy();

        EmcSession emc = openEmcSession(player);
        EmcBank bank = null;
        if (emc != null) {
            bank = emc.bank(collectKeys(root, new HashSet<>()));
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

        if (!commit(source, deposit, initial, working, targetKey, emc, bank, player)) {
            return CraftSummary.aborted(qty);
        }
        if (emc != null) emc.apply(bank, working.producedKeys());

        int crafted = Math.max(0, working.count(targetKey) - initial.count(targetKey));
        if (crafted > 0) playCraftSound(player);
        return new CraftSummary(Math.min(crafted, qty), qty, missing == null ? null : missing.displayName(),
                deposit.placements(), deposit.dropped(), deposit.byproducts());
    }

    public static CraftPreview.Result preview(ServerPlayer player, ItemStack target, int quantity,
                                              Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices) {
        int qty = Math.max(1, Math.min(MAX_QUANTITY, quantity));
        if (target.isEmpty()) return new CraftPreview.Result(0, qty, List.of());

        if (QuickCraftConfig.creativeBypass() && player.getAbilities().instabuild) {
            return new CraftPreview.Result(qty, qty, List.of(new CraftPreview.Gain(ItemKey.of(target), qty)));
        }

        List<LabeledSource> labeled = ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange());
        ItemSource source = extractionSource(labeled);
        Map<ItemKey, Integer> owned = ownedCounts(source.snapshot());

        ServerLevel level = player.serverLevel();
        RecipeResolver resolver = ServerRecipeCache.get(level.getRecipeManager(), level.registryAccess());
        TreeBuilder builder = new TreeBuilder(resolver, QuickCraftConfig.preferredItems(),
                QuickCraftConfig.maxTreeDepth(), QuickCraftConfig.maxTreeNodes());

        Availability availability = Availability.of(owned);
        Stations stations = StationScan.detect(level, player);
        CraftNode root = builder.build(target, qty, overrides, ingredientChoices, availability, stations,
                QuickCraftConfig.collapseOwnedItems(), QuickCraftConfig.hideLoopingRecipes());

        EmcSession emc = openEmcSession(player);
        EmcBank bank = emc == null ? null : emc.bank(collectKeys(root, new HashSet<>()));
        return CraftPreview.simulate(root, owned, target, qty, bank);
    }

    private static EmcSession openEmcSession(ServerPlayer player) {
        if (!QuickCraftConfig.useProjectEEmc() || !ProjectEIntegration.available()) return null;
        return EmcSession.open(player, QuickCraftConfig.containerScanRange());
    }

    private static Set<ItemKey> collectKeys(CraftNode node, Set<ItemKey> out) {
        out.add(ItemKey.of(node.output));
        for (CraftNode child : node.children) collectKeys(child, out);
        return out;
    }

    public record AvailabilitySnapshot(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                      Map<ItemKey, ItemStack> samples) {
    }

    public static AvailabilitySnapshot availability(ServerPlayer player, Set<ItemKey> keys) {
        Map<ItemKey, Integer> counts = new HashMap<>();
        Map<ItemKey, ItemStack> sources = new HashMap<>();
        Map<ItemKey, ItemStack> samples = new HashMap<>();
        if (keys.isEmpty()) return new AvailabilitySnapshot(counts, sources, samples);
        ItemSource source = ItemSourceFactory.forPlayer(player, QuickCraftConfig.containerScanRange());
        List<ItemStack> snapshot = null;
        for (ItemKey key : keys) {
            ItemStack rep = key.toStack(1);
            int available = source.extractMatching(rep, Integer.MAX_VALUE, true);
            counts.put(key, available);
            if (available <= 0) continue;
            source.sourceIconFor(rep).filter(icon -> !icon.isEmpty()).ifPresent(icon -> sources.put(key, icon));
            if (!DamageMatch.tolerant(rep)) continue;
            if (snapshot == null) snapshot = source.snapshot();
            ItemStack sample = DamageMatch.worst(snapshot, rep);
            if (!sample.isEmpty()) samples.put(key, sample);
        }
        return new AvailabilitySnapshot(counts, sources, samples);
    }

    public static List<LabeledSource> depositTargets(ServerPlayer player) {
        List<LabeledSource> out = new ArrayList<>();
        for (LabeledSource labeled : ItemSourceFactory.scan(player, QuickCraftConfig.containerScanRange())) {
            if (labeled.depositable()) out.add(labeled);
        }
        return out;
    }

    private static ItemSource extractionSource(List<LabeledSource> labeled) {
        List<ItemSource> sources = new ArrayList<>(labeled.size());
        for (LabeledSource l : labeled) sources.add(l.source());
        return new CompositeItemSource(sources);
    }

    private static Map<ItemKey, Integer> ownedCounts(List<ItemStack> snapshot) {
        Map<ItemKey, Integer> owned = new HashMap<>();
        for (ItemStack stack : snapshot) {
            owned.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
        }
        return owned;
    }

    private static VirtualPool poolFrom(List<ItemStack> snapshot) {
        VirtualPool pool = new VirtualPool();
        for (ItemStack stack : snapshot) pool.addStack(stack);
        return pool;
    }

    private static boolean commit(ItemSource source, Deposit deposit, VirtualPool initial, VirtualPool working,
                                  ItemKey targetKey, EmcSession emc, EmcBank bank, ServerPlayer player) {
        Map<ItemKey, Integer> consumed = new LinkedHashMap<>();
        Map<ItemKey, Integer> produced = new LinkedHashMap<>();
        Set<ItemKey> keys = new HashSet<>(initial.counts().keySet());
        keys.addAll(working.counts().keySet());
        for (ItemKey key : keys) {
            int delta = working.count(key) - initial.count(key);
            if (delta < 0) consumed.put(key, -delta);
            else if (delta > 0) produced.put(key, delta);
        }

        for (Map.Entry<ItemKey, Integer> entry : consumed.entrySet()) {
            if (source.extractMatching(entry.getKey().toStack(1), entry.getValue(), true) < entry.getValue()) return false;
        }

        Map<ItemKey, Integer> taken = new LinkedHashMap<>();
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
            if (emc != null && bank != null) {
                long value = emc.value(key.toStack(1));
                if (value > 0L) {
                    bank.gain(BigInteger.valueOf(value).multiply(BigInteger.valueOf(amount)));
                    deposit.toEmc(amount);
                    continue;
                }
            }
            deposit.put(key, amount, key.equals(targetKey));
        }
        return true;
    }

    private static void refund(ItemSource source, Map<ItemKey, Integer> taken, ServerPlayer player) {
        for (Map.Entry<ItemKey, Integer> entry : taken.entrySet()) {
            ItemKey key = entry.getKey();
            int max = Math.max(1, key.toStack(1).getMaxStackSize());
            int left = entry.getValue();
            while (left > 0) {
                int n = Math.min(left, max);
                ItemStack remainder = source.insert(key.toStack(n), false);
                if (!remainder.isEmpty()) player.drop(remainder, false);
                left -= n;
            }
        }
    }

    private static void playCraftSound(ServerPlayer player) {
        if (!QuickCraftConfig.craftSoundEnabled()) return;
        ResourceLocation id = QuickCraftConfig.craftSound();
        if (id == null) return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id).orElse(null);
        if (sound == null) return;
        player.playNotifySound(sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
