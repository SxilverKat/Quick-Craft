package com.sxilverr.quickcraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.util.Reg;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RecipePreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> recipes = new LinkedHashMap<String, String>();
    private static final Map<String, String> ingredients = new LinkedHashMap<String, String>();
    private static boolean loaded;

    private RecipePreferences() {
    }

    private static Path file() {
        return new File(Loader.instance().getConfigDir(), "quickcraft-preferences.json").toPath();
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(text, JsonObject.class);
            if (root == null) return;
            readSection(root, "recipes", recipes);
            readSection(root, "ingredients", ingredients);
        } catch (Exception e) {
            QuickCraft.LOGGER.warn("Quick Craft: failed to read recipe preferences", e);
        }
    }

    private static void readSection(JsonObject root, String name, Map<String, String> into) {
        if (!root.has(name) || !root.get(name).isJsonObject()) return;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject(name).entrySet()) {
            if (entry.getValue().isJsonPrimitive()) into.put(entry.getKey(), entry.getValue().getAsString());
        }
    }

    private static synchronized void save() {
        JsonObject root = new JsonObject();
        root.add("recipes", toJson(recipes));
        root.add("ingredients", toJson(ingredients));
        try {
            Files.write(file(), GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            QuickCraft.LOGGER.warn("Quick Craft: failed to save recipe preferences", e);
        }
    }

    private static JsonObject toJson(Map<String, String> map) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String> entry : map.entrySet()) obj.addProperty(entry.getKey(), entry.getValue());
        return obj;
    }

    public static Map<ItemKey, ResourceLocation> recipeOverrides() {
        ensureLoaded();
        Map<ItemKey, ResourceLocation> out = new HashMap<ItemKey, ResourceLocation>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            ItemKey key = ItemKey.parse(entry.getKey());
            ResourceLocation id = Reg.rl(entry.getValue());
            if (key != null && id != null) out.put(key, id);
        }
        return out;
    }

    public static Map<String, Item> ingredientChoices() {
        ensureLoaded();
        Map<String, Item> out = new HashMap<String, Item>();
        for (Map.Entry<String, String> entry : ingredients.entrySet()) {
            Item item = Reg.item(entry.getValue());
            if (item != null) out.put(entry.getKey(), item);
        }
        return out;
    }

    public static void setRecipe(ItemKey key, ResourceLocation recipeId) {
        ensureLoaded();
        if (key == null || recipeId == null) return;
        recipes.put(key.toKey(), recipeId.toString());
        save();
    }

    public static void clearRecipe(ItemKey key) {
        ensureLoaded();
        if (key == null) return;
        if (recipes.remove(key.toKey()) != null) save();
    }

    public static void setIngredient(String signature, Item item) {
        ensureLoaded();
        if (signature == null || signature.isEmpty() || item == null) return;
        ResourceLocation id = Reg.idOf(item);
        if (id == null) return;
        ingredients.put(signature, id.toString());
        save();
    }

    public static void clearIngredient(String signature) {
        ensureLoaded();
        if (signature == null || signature.isEmpty()) return;
        if (ingredients.remove(signature) != null) save();
    }
}
