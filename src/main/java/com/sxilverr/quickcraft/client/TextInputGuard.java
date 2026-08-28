package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiCommandBlock;
import net.minecraft.client.gui.inventory.GuiEditCommandBlockMinecart;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.gui.inventory.GuiEditStructure;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.GuiTextField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TextInputGuard {
    private static final int MAX_DEPTH = 3;
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new HashMap<Class<?>, List<Field>>();

    private TextInputGuard() {
    }

    public static boolean isTyping(GuiScreen screen) {
        if (screen == null) return false;
        if (screen instanceof GuiChat
                || screen instanceof GuiEditSign
                || screen instanceof GuiScreenBook
                || screen instanceof GuiCommandBlock
                || screen instanceof GuiEditCommandBlockMinecart
                || screen instanceof GuiEditStructure) return true;
        if (QuickCraftIntegrations.isTextInputFocused()) return true;
        return scan(screen, 0, new java.util.IdentityHashMap<Object, Boolean>());
    }

    private static boolean scan(Object owner, int depth, Map<Object, Boolean> seen) {
        if (owner == null || depth > MAX_DEPTH || seen.put(owner, Boolean.TRUE) != null) return false;

        for (Field field : fieldsOf(owner.getClass())) {
            Object value;
            try {
                value = field.get(owner);
            } catch (Throwable t) {
                continue;
            }
            if (value == null) continue;
            if (value instanceof GuiTextField) {
                if (((GuiTextField) value).isFocused()) return true;
            } else if (value instanceof Collection) {
                for (Object element : (Collection<?>) value) {
                    if (element instanceof GuiTextField && ((GuiTextField) element).isFocused()) return true;
                    if (isGuiObject(element) && scan(element, depth + 1, seen)) return true;
                }
            } else if (isGuiObject(value) && scan(value, depth + 1, seen)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGuiObject(Object value) {
        if (value == null) return false;
        Class<?> type = value.getClass();
        if (type.isPrimitive() || type.isArray() || type.isEnum()) return false;
        String name = type.getName();
        return name.startsWith("net.minecraft.client.gui") || name.startsWith("com.sxilverr.quickcraft");
    }

    private static List<Field> fieldsOf(Class<?> type) {
        List<Field> cached = FIELD_CACHE.get(type);
        if (cached != null) return cached;

        List<Field> fields = new ArrayList<Field>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Field[] declared;
            try {
                declared = c.getDeclaredFields();
            } catch (Throwable t) {
                break;
            }
            for (Field field : declared) {
                Class<?> fieldType = field.getType();
                if (fieldType.isPrimitive()) continue;
                if (!GuiTextField.class.isAssignableFrom(fieldType)
                        && !Collection.class.isAssignableFrom(fieldType)
                        && !fieldType.getName().startsWith("net.minecraft.client.gui")
                        && !fieldType.getName().startsWith("com.sxilverr.quickcraft")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    fields.add(field);
                } catch (Throwable ignored) {
                }
            }
        }
        FIELD_CACHE.put(type, fields);
        return fields;
    }
}
