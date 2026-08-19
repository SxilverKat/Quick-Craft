package com.sxilverr.quickcraft.neoforge;

import com.sxilverr.quickcraft.platform.Services;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class QuickCraftClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> COLOR_AVAILABLE = color(
            "colorAvailable", "FF55FF55", "Color for items you already have. Hex like FF55FF55 or 55FF55.");
    private static final ModConfigSpec.ConfigValue<String> COLOR_CRAFTED = color(
            "colorCrafted", "FFFFC64B", "Color for items that will be crafted.");
    private static final ModConfigSpec.ConfigValue<String> COLOR_MISSING = color(
            "colorMissing", "FFFF5555", "Color for missing materials.");
    private static final ModConfigSpec.ConfigValue<String> COLOR_NO_STATION = color(
            "colorNoStation", "FF5A5A5A", "Color for steps you have no station to craft.");
    private static final ModConfigSpec.ConfigValue<String> COLOR_TARGET = color(
            "colorTarget", "FF4AA3FF", "Color for the item you are crafting.");
    private static final ModConfigSpec.ConfigValue<String> COLOR_NODE_BACKGROUND = color(
            "colorNodeBackground", "F01A1A1A", "Fill color behind each step.");
    private static final ModConfigSpec.ConfigValue<String> COLOR_LINES = color(
            "colorLines", "FF7A7A7A", "Color of the lines linking steps.");

    private static final boolean JER = jerLoaded();
    private static final boolean PROJECTE = projectELoaded();
    private static ModConfigSpec.BooleanValue ENTITY_SPIN;
    private static ModConfigSpec.BooleanValue ENTITY_IDLE;
    private static ModConfigSpec.BooleanValue ENTITY_WALK;
    private static ModConfigSpec.DoubleValue ENTITY_SPIN_SPEED;
    private static ModConfigSpec.BooleanValue SHOW_EMC;

    static {
        if (JER) {
            BUILDER.comment("3D mob preview, shown when Just Enough Resources is installed.").push("mobPreview");
            ENTITY_SPIN = BUILDER.comment("Spin the mob preview.").define("entitySpin", true);
            ENTITY_IDLE = BUILDER.comment("Play the mob idle animation.").define("entityIdleAnimation", true);
            ENTITY_WALK = BUILDER.comment("Play the mob walk animation.").define("entityWalkAnimation", false);
            ENTITY_SPIN_SPEED = BUILDER.comment("Mob preview spin speed multiplier.").defineInRange("entitySpinSpeed", 1.0, 0.0, 20.0);
            BUILDER.pop();
        }
        if (PROJECTE) {
            BUILDER.comment("ProjectE integration, shown when ProjectE is installed.").push("projecte");
            SHOW_EMC = BUILDER.comment("Show your total EMC in the Quick Craft header when a transmutation table or tablet is available.")
                    .define("showEmc", true);
            BUILDER.pop();
        }
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private QuickCraftClientConfig() {
    }

    private static boolean jerLoaded() {
        try {
            return com.sxilverr.quickcraft.platform.Services.PLATFORM.isModLoaded("jeresources");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean projectELoaded() {
        try {
            return com.sxilverr.quickcraft.platform.Services.PLATFORM.isModLoaded("projecte");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean showEmc() {
        return SHOW_EMC == null || SHOW_EMC.get();
    }

    private static ModConfigSpec.ConfigValue<String> color(String name, String def, String comment) {
        return BUILDER.comment(comment).define(name, def, QuickCraftClientConfig::valid);
    }

    private static boolean valid(Object obj) {
        if (!(obj instanceof String s)) return false;
        String h = clean(s);
        if (h.isEmpty() || h.length() > 8) return false;
        try {
            Long.parseLong(h, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String clean(String s) {
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() > 1 && (h.startsWith("0x") || h.startsWith("0X"))) h = h.substring(2);
        if (h.length() == 6) h = "FF" + h;
        return h;
    }

    private static int parse(String s, int def) {
        try {
            return (int) Long.parseLong(clean(s), 16);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int colorAvailable() {
        return parse(COLOR_AVAILABLE.get(), 0xFF55FF55);
    }

    public static int colorCrafted() {
        return parse(COLOR_CRAFTED.get(), 0xFFFFC64B);
    }

    public static int colorMissing() {
        return parse(COLOR_MISSING.get(), 0xFFFF5555);
    }

    public static int colorNoStation() {
        return parse(COLOR_NO_STATION.get(), 0xFF5A5A5A);
    }

    public static int colorTarget() {
        return parse(COLOR_TARGET.get(), 0xFF4AA3FF);
    }

    public static int colorNodeBackground() {
        return parse(COLOR_NODE_BACKGROUND.get(), 0xF01A1A1A);
    }

    public static int colorLines() {
        return parse(COLOR_LINES.get(), 0xFF7A7A7A);
    }

    public static boolean entitySpin() {
        return ENTITY_SPIN == null || ENTITY_SPIN.get();
    }

    public static boolean entityIdle() {
        return ENTITY_IDLE == null || ENTITY_IDLE.get();
    }

    public static boolean entityWalk() {
        return ENTITY_WALK != null && ENTITY_WALK.get();
    }

    public static double entitySpinSpeed() {
        return ENTITY_SPIN_SPEED == null ? 1.0 : ENTITY_SPIN_SPEED.get();
    }
}
