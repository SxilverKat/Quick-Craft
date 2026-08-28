package com.sxilverr.quickcraft.integration.rs;

import com.sxilverr.quickcraft.util.Reflect;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

public final class RsSupport {
    public static final String MODID = "refinedstorage";

    private static boolean resolved;
    private static boolean available;

    private static Capability<?> proxyCapability;
    private static Method getNode;
    private static Method getNetwork;
    private static Method canRun;
    private static Method extractItem;
    private static Method insertItem;
    private static Method getItemStorageCache;
    private static Method getList;
    private static Method getStacks;
    private static Object actionSimulate;
    private static Object actionPerform;

    private RsSupport() {
    }

    public static synchronized boolean available() {
        if (!resolved) {
            resolved = true;
            available = resolve();
        }
        return available;
    }

    private static boolean resolve() {
        Class<?> capClass = Reflect.cls("com.raoulvdberge.refinedstorage.capability.CapabilityNetworkNodeProxy");
        Object cap = Reflect.get(Reflect.field(capClass, "NETWORK_NODE_PROXY_CAPABILITY"), null);
        if (!(cap instanceof Capability)) return false;
        proxyCapability = (Capability<?>) cap;

        Class<?> proxy = Reflect.cls("com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeProxy");
        getNode = Reflect.methodByName(proxy, "getNode", 0);

        Class<?> node = Reflect.cls("com.raoulvdberge.refinedstorage.api.network.node.INetworkNode");
        getNetwork = Reflect.methodByName(node, "getNetwork", 0);

        Class<?> network = Reflect.cls("com.raoulvdberge.refinedstorage.api.network.INetwork");
        canRun = Reflect.methodByName(network, "canRun", 0);
        extractItem = Reflect.methodByName(network, "extractItem", 3);
        insertItem = Reflect.methodByName(network, "insertItem", 3);
        getItemStorageCache = Reflect.methodByName(network, "getItemStorageCache", 0);

        Class<?> cache = Reflect.cls("com.raoulvdberge.refinedstorage.api.storage.IStorageCache");
        getList = Reflect.methodByName(cache, "getList", 0);

        Class<?> stackList = Reflect.cls("com.raoulvdberge.refinedstorage.api.util.IStackList");
        getStacks = Reflect.methodByName(stackList, "getStacks", 0);

        Class<?> actionClass = Reflect.cls("com.raoulvdberge.refinedstorage.api.util.Action");
        actionSimulate = Reflect.enumValue(actionClass, "SIMULATE");
        actionPerform = Reflect.enumValue(actionClass, "PERFORM");

        return getNode != null && getNetwork != null && canRun != null && extractItem != null
                && insertItem != null && getItemStorageCache != null && getList != null
                && getStacks != null && actionSimulate != null && actionPerform != null;
    }

    static Object networkOf(TileEntity tile) {
        if (proxyCapability == null || tile == null) return null;
        if (!tile.hasCapability(proxyCapability, null)) return null;
        Object proxy = tile.getCapability(proxyCapability, null);
        if (proxy == null) return null;
        Object node = Reflect.invoke(getNode, proxy);
        if (node == null) return null;
        return Reflect.invoke(getNetwork, node);
    }

    static boolean running(Object network) {
        return network != null && Reflect.boolValue(Reflect.invoke(canRun, network), false);
    }

    static Collection<?> stacks(Object network) {
        Object cache = Reflect.invoke(getItemStorageCache, network);
        if (cache == null) return Collections.emptyList();
        Object list = Reflect.invoke(getList, cache);
        if (list == null) return Collections.emptyList();
        Object stacks = Reflect.invoke(getStacks, list);
        return stacks instanceof Collection ? (Collection<?>) stacks : Collections.emptyList();
    }

    static ItemStack extract(Object network, ItemStack stack, int size, boolean simulate) {
        Object result = Reflect.invoke(extractItem, network, stack, size,
                simulate ? actionSimulate : actionPerform);
        return result instanceof ItemStack ? (ItemStack) result : ItemStack.EMPTY;
    }

    static ItemStack insert(Object network, ItemStack stack, int size, boolean simulate) {
        Object result = Reflect.invoke(insertItem, network, stack, size,
                simulate ? actionSimulate : actionPerform);
        return result instanceof ItemStack ? (ItemStack) result : ItemStack.EMPTY;
    }
}
