package com.sxilverr.quickcraft.forge;

import com.sxilverr.quickcraft.DepositBlacklist;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuickCraftConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final List<String> DEFAULT_PREFERRED_IDS = List.of("minecraft:oak_planks", "minecraft:oak_log");

    private static final List<String> DEFAULT_DEPOSIT_BLACKLIST = List.of(
            "ae2:drive", "ae2:controller", "ae2:charger", "ae2:wireless_access_point",
            "refinedstorage:disk_drive", "toms_storage:ts.inventory_connector",
            "@chargers");

    private static final List<String> DEFAULT_EXTRA_SOURCES = List.of();

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> PREFERRED_ITEMS = BUILDER
            .comment("Priority list of item ids to prefer when a recipe ingredient or recipe is ambiguous (such as tags).",
                    "Earlier entries win, and recipes that use these items are favored.",
                    "Leave empty to use the built-in defaults.")
            .defineListAllowEmpty("preferredItems", DEFAULT_PREFERRED_IDS, QuickCraftConfig::validateItemId);

    private static final ForgeConfigSpec.IntValue CONTAINER_SCAN_RANGE = BUILDER
            .comment("Block radius around the player to pull items from and deposit results into. 0 disables nearby containers.")
            .defineInRange("containerScanRange", 8, 0, 64);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DEPOSIT_BLACKLIST = BUILDER
            .comment("Blocks that will not be shown as options to deposit results into.",
                    "Entries can be a block id (ae2:charger), a whole mod id (ae2),",
                    "or a block tag prefixed with @ or # (@chargers or #forge:chargers).")
            .defineListAllowEmpty("depositBlacklist", DEFAULT_DEPOSIT_BLACKLIST, QuickCraftConfig::validateBlacklistEntry);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_SOURCES = BUILDER
            .comment("Extra block or item ids to pull from and deposit into. They must hold an item inventory.")
            .defineListAllowEmpty("extraSources", DEFAULT_EXTRA_SOURCES, QuickCraftConfig::validateResourceId);

    private static final ForgeConfigSpec.BooleanValue COLLAPSE_OWNED_ITEMS = BUILDER
            .comment("Items you already have enough of are not expanded into their own recipe tree.")
            .define("collapseOwnedItems", true);

    private static final ForgeConfigSpec.BooleanValue CRAFT_SOUND_ENABLED = BUILDER
            .comment("Play a sound when a Quick Craft finishes crafting something.")
            .define("craftSoundEnabled", true);

    private static final ForgeConfigSpec.ConfigValue<String> CRAFT_SOUND = BUILDER
            .comment("The sound id played when a Quick Craft finishes.")
            .define("craftSound", "minecraft:entity.arrow.hit_player");

    private static final ForgeConfigSpec.IntValue MAX_TREE_DEPTH = BUILDER
            .comment("Maximum depth the crafting tree will expand to.")
            .defineInRange("maxTreeDepth", 32, 1, 128);

    private static final ForgeConfigSpec.IntValue MAX_TREE_NODES = BUILDER
            .comment("Safety cap on the total number of nodes in a crafting tree.")
            .defineInRange("maxTreeNodes", 512, 16, 8192);

    private static final ForgeConfigSpec.BooleanValue ANIMATIONS_ENABLED = BUILDER
            .comment("Enable Animations")
            .define("animationsEnabled", true);

    private static final ForgeConfigSpec.BooleanValue CREATIVE_BYPASS = BUILDER
            .comment("Players in creative mode craft the requested item instantly without needing or consuming any ingredients.")
            .define("creativeBypass", false);

    private static final ForgeConfigSpec.BooleanValue PINNED_LIST_ANIMATION = BUILDER
            .comment("The pinned (bookmarked) ingredients list eases its rows in when it appears.")
            .define("pinnedListAnimation", true);

    private static final ForgeConfigSpec.BooleanValue HIDE_LOOPING_RECIPES = BUILDER
            .comment("Recipes that loop back on themselves are hidden as options unless you already have the looping item on hand.")
            .define("hideLoopingRecipes", true);

    private static final ForgeConfigSpec.BooleanValue OPEN_ONLY_WITH_RECIPE = BUILDER
            .comment("Pressing the Quick Craft key on an item that has no supported recipe does nothing instead of opening an empty menu.")
            .define("openOnlyWithRecipe", true);

    private static final ForgeConfigSpec.BooleanValue HOVER_BULGE = BUILDER
            .comment("Tab bulges when hovering over it")
            .define("hoverBulge", true);

    private static final ForgeConfigSpec.BooleanValue SIZE_TAB_TO_FIT = BUILDER
            .comment("Size a tab to fit a item name")
            .define("sizeTabToFit", true);

    private static final ForgeConfigSpec.ConfigValue<String> SHIFT_CRAFT_AMOUNT = BUILDER
            .comment("How many items should be crafted when holding SHIFT. Accepts Max")
            .define("shiftCraftAmount", "64", QuickCraftConfig::validateShiftAmount);

    private static final ForgeConfigSpec.BooleanValue USE_PROJECT_E_EMC = BUILDER
            .comment("When ProjectE is installed and a transmutation table is nearby or a transmutation tablet is in your inventory,",
                    "use your EMC to supply missing learned materials and learn the items you craft. Requires ProjectE.")
            .define("useProjectEEmc", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static List<Item> preferredItems() {
        List<? extends String> configured = PREFERRED_ITEMS.get();
        List<? extends String> ids = configured.isEmpty() ? DEFAULT_PREFERRED_IDS : configured;
        List<Item> out = new ArrayList<>();
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl == null) continue;
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != Items.AIR) out.add(item);
        }
        return out;
    }

    public static int containerScanRange() {
        return CONTAINER_SCAN_RANGE.get();
    }

    public static DepositBlacklist depositBlacklist() {
        return DepositBlacklist.parse(DEPOSIT_BLACKLIST.get());
    }

    public static Set<ResourceLocation> extraSources() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String id : EXTRA_SOURCES.get()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) out.add(rl);
        }
        return out;
    }

    public static boolean collapseOwnedItems() {
        return COLLAPSE_OWNED_ITEMS.get();
    }

    public static boolean craftSoundEnabled() {
        return CRAFT_SOUND_ENABLED.get();
    }

    public static ResourceLocation craftSound() {
        return ResourceLocation.tryParse(CRAFT_SOUND.get());
    }

    public static int maxTreeDepth() {
        return MAX_TREE_DEPTH.get();
    }

    public static int maxTreeNodes() {
        return MAX_TREE_NODES.get();
    }

    public static boolean animationsEnabled() {
        return ANIMATIONS_ENABLED.get();
    }

    public static boolean creativeBypass() {
        return CREATIVE_BYPASS.get();
    }

    public static boolean pinnedListAnimation() {
        return PINNED_LIST_ANIMATION.get();
    }

    public static boolean hideLoopingRecipes() {
        return HIDE_LOOPING_RECIPES.get();
    }

    public static boolean openOnlyWithRecipe() {
        return OPEN_ONLY_WITH_RECIPE.get();
    }

    public static boolean hoverBulge() {
        return HOVER_BULGE.get();
    }

    public static boolean sizeTabToFit() {
        return SIZE_TAB_TO_FIT.get();
    }

    public static boolean shiftCraftIsMax() {
        return SHIFT_CRAFT_AMOUNT.get().trim().equalsIgnoreCase("max");
    }

    public static boolean useProjectEEmc() {
        return USE_PROJECT_E_EMC.get();
    }

    public static int shiftCraftAmount() {
        try {
            return Math.max(1, Math.min(1_000_000, Integer.parseInt(SHIFT_CRAFT_AMOUNT.get().trim())));
        } catch (NumberFormatException e) {
            return 64;
        }
    }

    private static boolean validateShiftAmount(final Object obj) {
        if (!(obj instanceof String s)) return false;
        if (s.trim().equalsIgnoreCase("max")) return true;
        try {
            int value = Integer.parseInt(s.trim());
            return value >= 1 && value <= 1_000_000;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean validateItemId(final Object obj) {
        if (!(obj instanceof String s)) return false;
        ResourceLocation rl = ResourceLocation.tryParse(s);
        return rl != null && ForgeRegistries.ITEMS.containsKey(rl);
    }

    private static boolean validateResourceId(final Object obj) {
        return obj instanceof String s && ResourceLocation.tryParse(s) != null;
    }

    private static boolean validateBlacklistEntry(final Object obj) {
        if (!(obj instanceof String raw)) return false;
        String entry = raw.trim();
        if (entry.isEmpty()) return false;
        if (entry.startsWith("@") || entry.startsWith("#")) {
            String tag = entry.substring(1).trim();
            if (tag.isEmpty()) return false;
            return tag.contains(":") ? ResourceLocation.tryParse(tag) != null : isValidResourcePart(tag, true);
        }
        if (entry.contains(":")) {
            String[] parts = entry.split(":", 2);
            if (parts[1].isEmpty() || parts[1].equals("*")) return isValidResourcePart(parts[0], false);
            return ResourceLocation.tryParse(entry) != null;
        }
        return isValidResourcePart(entry, false);
    }

    private static boolean isValidResourcePart(String s, boolean allowSlash) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = c == '_' || c == '-' || c == '.'
                    || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || (allowSlash && c == '/');
            if (!ok) return false;
        }
        return true;
    }
}
