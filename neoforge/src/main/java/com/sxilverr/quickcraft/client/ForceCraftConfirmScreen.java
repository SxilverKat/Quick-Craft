package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class ForceCraftConfirmScreen extends Screen {
    private static final int COLOR_TARGET = 0xFF55FF55;
    private static final int COLOR_PANEL = 0xF0080808;
    private static final int COLOR_PANEL_HEAD = 0xF0202020;
    private static final int FORCE_W = 150;
    private static final int FORCE_H = 24;
    private static final int MAX_ROWS = 7;

    private final Screen parent;
    private final ItemStack target;
    private final int quantity;
    private final Map<ItemKey, ResourceLocation> overrides;
    private final Map<String, Item> ingredientChoices;
    private final CraftPreview.Result preview;

    private int forceX;
    private int forceY;
    private boolean sent;

    public ForceCraftConfirmScreen(Screen parent, ItemStack target, int quantity,
                                   Map<ItemKey, ResourceLocation> overrides, Map<String, Item> ingredientChoices,
                                   CraftPreview.Result preview) {
        super(Component.literal("Force Craft?"));
        this.parent = parent;
        this.target = target;
        this.quantity = quantity;
        this.overrides = overrides;
        this.ingredientChoices = ingredientChoices;
        this.preview = preview;
    }

    @Override
    protected void init() {
        forceX = this.width / 2 - FORCE_W / 2;
        forceY = this.height / 2 - 34;

        addRenderableWidget(Button.builder(Component.literal("Force Craft"), b -> forceCraft())
                .bounds(forceX, forceY, FORCE_W, FORCE_H).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 36, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        g.drawCenteredString(this.font, "You do not have enough items to craft this fully",
                cx, this.height / 2 - 78, 0xFFFF5555);

        String sub = "Can craft " + preview.craftable() + " of " + quantity + " × "
                + target.getHoverName().getString();
        g.drawCenteredString(this.font, sub, cx, this.height / 2 - 62, 0xFFC0C0C0);
        g.drawCenteredString(this.font, "Force Craft makes as many as your materials allow.",
                cx, this.height / 2 - 50, 0xFF909090);

        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }

        if (overForceButton(mouseX, mouseY)) {
            renderAcquire(g, cx);
        }
    }

    private boolean overForceButton(int mouseX, int mouseY) {
        return mouseX >= forceX && mouseX <= forceX + FORCE_W
                && mouseY >= forceY && mouseY <= forceY + FORCE_H;
    }

    private void renderAcquire(GuiGraphics g, int cx) {
        List<CraftPreview.Gain> list = preview.gained();
        int panelW = 224;
        int rowH = 18;
        int headerH = 15;
        int shown = Math.min(list.size(), MAX_ROWS);
        boolean more = list.size() > shown;
        int bodyRows = list.isEmpty() ? 1 : shown;
        int panelH = headerH + bodyRows * rowH + (more ? 11 : 0) + 4;

        int px = cx - panelW / 2;
        int py = forceY + FORCE_H + 6;

        g.fill(px, py, px + panelW, py + panelH, COLOR_PANEL);
        g.fill(px, py, px + panelW, py + headerH, COLOR_PANEL_HEAD);
        g.drawString(this.font, "Acquire items:", px + 6, py + 4, 0xFFFFFFFF, false);

        if (list.isEmpty()) {
            g.drawString(this.font, "Nothing - not enough materials to make any.",
                    px + 8, py + headerH + 5, 0xFFAAAAAA, false);
            return;
        }

        int y = py + headerH + 2;
        for (int i = 0; i < shown; i++) {
            CraftPreview.Gain gain = list.get(i);
            ItemStack icon = gain.toStack();
            g.renderItem(icon, px + 5, y);
            int color = gain.key().item() == target.getItem() ? COLOR_TARGET : 0xFFFFFFFF;
            String label = trim(icon.getHoverName().getString(), 22) + "  ×" + gain.count();
            g.drawString(this.font, label, px + 26, y + 4, color, false);
            y += rowH;
        }
        if (more) {
            g.drawString(this.font, "… +" + (list.size() - shown) + " more",
                    px + 8, y + 1, 0xFF909090, false);
        }
    }

    private void forceCraft() {
        if (sent) return;
        sent = true;
        CraftHistory.record(target, preview.craftable());
        QuickCraftNetwork.sendCraftRequest(target, quantity, overrides, ingredientChoices, ClientDepositTargets.selectedId());
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    private static String trim(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
