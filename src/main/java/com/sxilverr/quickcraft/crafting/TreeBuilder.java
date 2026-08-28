package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TreeBuilder {
    //? if >=1.20.5 {
    /*public static final ResourceLocation MANUAL = ResourceLocation.fromNamespaceAndPath("quickcraft", "manual");
    *///?} else {
    public static final ResourceLocation MANUAL = new ResourceLocation("quickcraft", "manual");
    //?}

    private final RecipeResolver resolver;
    private final List<Item> preferred;
    private final int maxDepth;
    private final int maxNodes;

    private Map<ItemKey, ResourceLocation> recipeOverrides = Map.of();
    private Map<String, Item> ingredientChoices = Map.of();
    private Availability availability = Availability.NONE;
    private Stations stations = new Stations(3, true, false, false, false, false, false, null, null, null);
    private boolean collapseOwned = true;
    private boolean hideLooping = true;
    private final Map<ItemKey, Integer> claimedStock = new HashMap<>();
    private final Set<ItemKey> loopIngredients = new HashSet<>();
    private int nodeCount;

    public TreeBuilder(RecipeResolver resolver, List<Item> preferred, int maxDepth, int maxNodes) {
        this.resolver = resolver;
        this.preferred = preferred;
        this.maxDepth = maxDepth;
        this.maxNodes = maxNodes;
    }

    public CraftNode build(ItemStack target, int quantity, Map<ItemKey, ResourceLocation> overrides,
                           Map<String, Item> ingredientChoices,
                           Availability availability, Stations stations, boolean collapseOwned, boolean hideLooping) {
        this.recipeOverrides = overrides == null ? Map.of() : overrides;
        this.ingredientChoices = ingredientChoices == null ? Map.of() : ingredientChoices;
        this.availability = availability == null ? Availability.NONE : availability;
        this.stations = stations;
        this.collapseOwned = collapseOwned;
        this.hideLooping = hideLooping;
        this.claimedStock.clear();
        this.loopIngredients.clear();
        this.nodeCount = 0;
        return buildNode(target, Math.max(1, quantity), 0, new HashSet<ItemKey>(), true);
    }

    private CraftNode buildNode(ItemStack output, int requiredCount, int depth, Set<ItemKey> path, boolean parentReachable) {
        nodeCount++;
        List<RecipeOption> alternatives = visibleRecipes(output, resolver.recipesFor(output));
        CraftNode node = new CraftNode(output.copy(), requiredCount, alternatives, depth);

        ItemKey outputKey = ItemKey.of(output);
        int claimed = claimedStock.getOrDefault(outputKey, 0);
        int freeStock = availability.available(outputKey) - claimed;
        node.freeStock = Math.max(0, freeStock);

        node.autoRecipe = alternatives.isEmpty() ? -1 : autoBestIndex(output, alternatives, requiredCount);
        node.selectedRecipe = resolveSelection(output, alternatives, node.autoRecipe);
        if (node.selectedRecipe < 0) {
            claimedStock.merge(outputKey, Math.min(node.freeStock, requiredCount), Integer::sum);
            return node;
        }

        boolean root = depth == 0;
        RecipeOption option = node.selected();
        if (!root && collapseOwned && freeStock >= requiredCount) {
            node.owned = true;
            claimedStock.merge(outputKey, requiredCount, Integer::sum);
            return node;
        }

        node.fitsStation = option.fits(stations);
        node.craftReachable = parentReachable && node.fitsStation;

        if (depth >= maxDepth || nodeCount >= maxNodes) return node;
        if (path.contains(outputKey)) {
            node.cyclic = true;
            node.selectedRecipe = -1;
            return node;
        }

        int resultPer = Math.max(1, option.resultCount());
        node.resultPerCraft = resultPer;
        int crafts = ceilDiv(requiredCount, resultPer);
        node.craftsNeeded = crafts;

        Map<ItemKey, Integer> needs = new LinkedHashMap<>();
        Map<ItemKey, ItemStack[]> childOptions = new LinkedHashMap<>();
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey key = ItemKey.of(choice);
            needs.merge(key, crafts, Integer::sum);
            childOptions.putIfAbsent(key, items);
        }

        path.add(outputKey);
        for (Map.Entry<ItemKey, Integer> entry : needs.entrySet()) {
            CraftNode child = buildNode(entry.getKey().toStack(1), entry.getValue(), depth + 1, path, node.craftReachable);
            applyTagOptions(child, childOptions.get(entry.getKey()));
            node.children.add(child);
        }
        path.remove(outputKey);
        return node;
    }

    private static void applyTagOptions(CraftNode child, ItemStack[] options) {
        if (options == null || options.length <= 1) return;
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : options) list.add(stack.copy());
        child.tagOptions = list;
        child.tagSignature = ingredientSignature(options);
    }

    public static String ingredientSignature(ItemStack[] items) {
        List<String> ids = new ArrayList<>();
        for (ItemStack stack : items) {
            ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            ids.add(rl == null ? "?" : rl.toString());
        }
        Collections.sort(ids);
        return String.join(",", ids);
    }

    private int resolveSelection(ItemStack output, List<RecipeOption> alternatives, int auto) {
        if (alternatives.isEmpty()) return -1;

        ResourceLocation override = recipeOverrides.get(ItemKey.of(output));
        if (MANUAL.equals(override)) return -1;
        if (override != null) {
            for (int i = 0; i < alternatives.size(); i++) {
                if (alternatives.get(i).id().equals(override)) return i;
            }
        }
        return auto;
    }

    private int autoBestIndex(ItemStack output, List<RecipeOption> alternatives, int requiredCount) {
        int best = 0;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < alternatives.size(); i++) {
            int score = recipeScore(output, alternatives.get(i), requiredCount);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private int recipeScore(ItemStack output, RecipeOption option, int requiredCount) {
        boolean fits = option.fits(stations);
        boolean selfReferencing = referencesOutput(output, option);
        int resultPer = Math.max(1, option.resultCount());
        int crafts = ceilDiv(Math.max(1, requiredCount), resultPer);

        Map<ItemKey, Integer> needs = new HashMap<>();
        int preferredHits = 0;
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            if (ingredientAcceptsPreferred(ingredient)) preferredHits++;
            ItemStack choice = chooseIngredient(ingredient);
            if (!choice.isEmpty()) needs.merge(ItemKey.of(choice), crafts, Integer::sum);
        }

        int fullyAvailable = 0;
        int anyAvailable = 0;
        for (Map.Entry<ItemKey, Integer> entry : needs.entrySet()) {
            int have = availability.available(entry.getKey());
            if (have > 0) anyAvailable++;
            if (have >= entry.getValue()) fullyAvailable++;
        }

        return (fits ? 1_000_000 : 0)
                - (selfReferencing ? 500_000 : 0)
                + fullyAvailable * 1000
                + anyAvailable * 100
                + preferredHits * 10
                - needs.size();
    }

    private boolean referencesOutput(ItemStack output, RecipeOption option) {
        if (option == null) return false;
        ItemKey outputKey = ItemKey.of(output);
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey choiceKey = ItemKey.of(choice);
            if (choiceKey.equals(outputKey) || craftableFrom(choice, outputKey)) return true;
        }
        return false;
    }

    private boolean ingredientAcceptsPreferred(Ingredient ingredient) {
        for (ItemStack stack : ingredient.getItems()) {
            if (preferred.contains(stack.getItem())) return true;
        }
        return false;
    }

    private List<RecipeOption> visibleRecipes(ItemStack output, List<RecipeOption> alternatives) {
        if (!hideLooping || alternatives.isEmpty()) return alternatives;
        List<RecipeOption> visible = new ArrayList<>();
        for (RecipeOption option : alternatives) {
            if (!hidesAsLoop(output, option)) visible.add(option);
        }
        return visible.isEmpty() ? alternatives : visible;
    }

    private boolean hidesAsLoop(ItemStack output, RecipeOption option) {
        ItemKey outputKey = ItemKey.of(output);
        boolean hidden = false;
        for (Ingredient ingredient : option.inputs()) {
            if (ingredient.isEmpty()) continue;
            ItemStack choice = chooseIngredient(ingredient);
            if (choice.isEmpty()) continue;
            ItemKey choiceKey = ItemKey.of(choice);
            boolean loops = choiceKey.equals(outputKey) || craftableFrom(choice, outputKey);
            if (!loops) continue;
            loopIngredients.add(choiceKey);
            if (availability.available(choiceKey) <= 0) hidden = true;
        }
        return hidden;
    }

    public Set<ItemKey> loopIngredientKeys() {
        return loopIngredients;
    }

    private boolean craftableFrom(ItemStack target, ItemKey source) {
        for (RecipeOption option : resolver.recipesFor(target)) {
            for (Ingredient ingredient : option.inputs()) {
                if (ingredient.isEmpty()) continue;
                for (ItemStack stack : ingredient.getItems()) {
                    if (source.equals(ItemKey.of(stack))) return true;
                }
            }
        }
        return false;
    }

    private ItemStack chooseIngredient(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        if (items.length > 1) {
            Item chosen = ingredientChoices.get(ingredientSignature(items));
            if (chosen != null) {
                for (ItemStack stack : items) {
                    if (stack.getItem() == chosen) return stack.copy();
                }
            }
        }
        for (Item pref : preferred) {
            for (ItemStack stack : items) {
                if (stack.getItem() == pref && availability.available(ItemKey.of(stack)) > 0) return stack.copy();
            }
        }
        for (ItemStack stack : items) {
            if (availability.available(ItemKey.of(stack)) > 0) return stack.copy();
        }
        for (Item pref : preferred) {
            for (ItemStack stack : items) {
                if (stack.getItem() == pref) return stack.copy();
            }
        }
        return items[0].copy();
    }

    private static int ceilDiv(int a, int b) {
        return (a + b - 1) / b;
    }
}
