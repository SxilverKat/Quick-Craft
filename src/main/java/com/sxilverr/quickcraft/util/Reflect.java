package com.sxilverr.quickcraft.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Reflect {
    private Reflect() {
    }

    public static Class<?> cls(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Method method(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) return null;
        try {
            Method m = owner.getMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Method methodByName(Class<?> owner, String name, int paramCount) {
        if (owner == null) return null;
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            for (Method m : c.getMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == paramCount) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    public static Field field(Class<?> owner, String name) {
        if (owner == null) return null;
        try {
            Field f = owner.getField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object invoke(Method method, Object target, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object get(Field field, Object target) {
        if (field == null) return null;
        try {
            return field.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int intValue(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    public static long longValue(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    public static boolean boolValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object enumValue(Class<?> enumClass, String name) {
        if (enumClass == null || !enumClass.isEnum()) return null;
        try {
            return Enum.valueOf((Class<Enum>) enumClass, name);
        } catch (Throwable t) {
            return null;
        }
    }
}
