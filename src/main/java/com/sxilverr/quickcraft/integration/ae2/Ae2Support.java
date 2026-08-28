package com.sxilverr.quickcraft.integration.ae2;

import com.sxilverr.quickcraft.util.Reflect;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;

public final class Ae2Support {
    public static final String MODID = "appliedenergistics2";

    private static boolean resolved;
    private static boolean available;

    static Class<?> gridHostClass;
    static Class<?> actionableClass;
    static Object actionableSimulate;
    static Object actionableModulate;
    static Object partLocationInternal;

    private static Method getGridNode;
    private static Method getGrid;
    private static Method getCache;
    private static Method getInventory;
    private static Method createStack;
    private static Method getStorageList;
    private static Method getStackSize;
    private static Method createItemStack;
    private static Method extractItems;
    private static Method injectItems;
    private static Method isNetworkPowered;

    private static Object itemChannel;
    private static Class<?> storageGridClass;
    private static Class<?> energyGridClass;

    private static Class<?> actionSourceClass;
    private static Constructor<?> playerSourceCtor;

    private Ae2Support() {
    }

    public static synchronized boolean available() {
        if (!resolved) {
            resolved = true;
            available = resolve();
        }
        return available;
    }

    private static boolean resolve() {
        gridHostClass = Reflect.cls("appeng.api.networking.IGridHost");
        if (gridHostClass == null) return false;

        Class<?> partLocation = Reflect.cls("appeng.api.util.AEPartLocation");
        partLocationInternal = Reflect.enumValue(partLocation, "INTERNAL");
        if (partLocationInternal == null) return false;

        getGridNode = Reflect.methodByName(gridHostClass, "getGridNode", 1);
        Class<?> gridNode = Reflect.cls("appeng.api.networking.IGridNode");
        getGrid = Reflect.method(gridNode, "getGrid");

        Class<?> grid = Reflect.cls("appeng.api.networking.IGrid");
        getCache = Reflect.methodByName(grid, "getCache", 1);

        storageGridClass = Reflect.cls("appeng.api.networking.storage.IStorageGrid");
        energyGridClass = Reflect.cls("appeng.api.networking.energy.IEnergyGrid");
        if (storageGridClass == null || energyGridClass == null) return false;

        getInventory = Reflect.methodByName(storageGridClass, "getInventory", 1);
        isNetworkPowered = Reflect.methodByName(energyGridClass, "isNetworkPowered", 0);

        itemChannel = resolveItemChannel();
        if (itemChannel == null) return false;
        createStack = Reflect.methodByName(itemChannel.getClass(), "createStack", 1);

        Class<?> meMonitor = Reflect.cls("appeng.api.storage.IMEMonitor");
        getStorageList = Reflect.methodByName(meMonitor, "getStorageList", 0);

        Class<?> meInventory = Reflect.cls("appeng.api.storage.IMEInventory");
        extractItems = Reflect.methodByName(meInventory, "extractItems", 3);
        injectItems = Reflect.methodByName(meInventory, "injectItems", 3);

        Class<?> aeItemStack = Reflect.cls("appeng.api.storage.data.IAEItemStack");
        getStackSize = Reflect.methodByName(Reflect.cls("appeng.api.storage.data.IAEStack"), "getStackSize", 0);
        if (getStackSize == null) getStackSize = Reflect.methodByName(aeItemStack, "getStackSize", 0);
        createItemStack = Reflect.methodByName(aeItemStack, "createItemStack", 0);

        actionableClass = Reflect.cls("appeng.api.config.Actionable");
        actionableSimulate = Reflect.enumValue(actionableClass, "SIMULATE");
        actionableModulate = Reflect.enumValue(actionableClass, "MODULATE");

        actionSourceClass = Reflect.cls("appeng.api.networking.security.IActionSource");
        Class<?> playerSource = Reflect.cls("appeng.me.helpers.PlayerSource");
        Class<?> actionHost = Reflect.cls("appeng.api.networking.security.IActionHost");
        if (playerSource != null && actionHost != null) {
            try {
                playerSourceCtor = playerSource.getConstructor(EntityPlayer.class, actionHost);
                playerSourceCtor.setAccessible(true);
            } catch (Throwable ignored) {
                playerSourceCtor = null;
            }
        }

        return getGridNode != null && getGrid != null && getCache != null && getInventory != null
                && createStack != null && getStorageList != null && getStackSize != null
                && createItemStack != null && extractItems != null && injectItems != null
                && actionableSimulate != null && actionableModulate != null;
    }

    private static Object resolveItemChannel() {
        Class<?> api = Reflect.cls("appeng.api.AEApi");
        Object instance = Reflect.invoke(Reflect.method(api, "instance"), null);
        if (instance == null) return null;
        Object storage = Reflect.invoke(Reflect.methodByName(instance.getClass(), "storage", 0), instance);
        if (storage == null) return null;
        Class<?> itemChannelClass = Reflect.cls("appeng.api.storage.channels.IItemStorageChannel");
        if (itemChannelClass == null) return null;
        return Reflect.invoke(Reflect.methodByName(storage.getClass(), "getStorageChannel", 1), storage, itemChannelClass);
    }

    static Object gridOf(Object gridHost) {
        Object node = Reflect.invoke(getGridNode, gridHost, partLocationInternal);
        if (node == null) return null;
        return Reflect.invoke(getGrid, node);
    }

    static Object storageGrid(Object grid) {
        return grid == null ? null : Reflect.invoke(getCache, grid, storageGridClass);
    }

    static Object energyGrid(Object grid) {
        return grid == null ? null : Reflect.invoke(getCache, grid, energyGridClass);
    }

    static boolean powered(Object energyGrid) {
        return energyGrid != null && Reflect.boolValue(Reflect.invoke(isNetworkPowered, energyGrid), false);
    }

    static Object itemInventory(Object storageGrid) {
        return storageGrid == null ? null : Reflect.invoke(getInventory, storageGrid, itemChannel);
    }

    static Object aeStack(ItemStack stack) {
        return Reflect.invoke(createStack, itemChannel, stack);
    }

    static Iterable<?> storageList(Object monitor) {
        Object list = Reflect.invoke(getStorageList, monitor);
        return list instanceof Iterable ? (Iterable<?>) list : null;
    }

    static long stackSize(Object aeStack) {
        return Reflect.longValue(Reflect.invoke(getStackSize, aeStack), 0L);
    }

    static ItemStack toStack(Object aeStack) {
        Object stack = Reflect.invoke(createItemStack, aeStack);
        return stack instanceof ItemStack ? (ItemStack) stack : ItemStack.EMPTY;
    }

    static Object extract(Object monitor, Object request, boolean simulate, Object source) {
        return Reflect.invoke(extractItems, monitor, request,
                simulate ? actionableSimulate : actionableModulate, source);
    }

    static Object inject(Object monitor, Object request, boolean simulate, Object source) {
        return Reflect.invoke(injectItems, monitor, request,
                simulate ? actionableSimulate : actionableModulate, source);
    }

    static Object actionSource(final EntityPlayer player) {
        if (playerSourceCtor != null) {
            try {
                return playerSourceCtor.newInstance(player, null);
            } catch (Throwable ignored) {
                playerSourceCtor = null;
            }
        }
        if (actionSourceClass == null) return null;
        try {
            return Proxy.newProxyInstance(actionSourceClass.getClassLoader(),
                    new Class<?>[]{actionSourceClass}, new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) {
                            String name = method.getName();
                            if ("player".equals(name)) return Optional.of(player);
                            if ("machine".equals(name) || "context".equals(name)) return Optional.empty();
                            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
                            if ("equals".equals(name)) return args != null && proxy == args[0];
                            if ("toString".equals(name)) return "QuickCraftActionSource";
                            return null;
                        }
                    });
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isGridHost(Object candidate) {
        return gridHostClass != null && candidate != null && gridHostClass.isInstance(candidate);
    }
}
