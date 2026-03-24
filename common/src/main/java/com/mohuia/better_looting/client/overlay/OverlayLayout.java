package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.client.Utils;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;

public class OverlayLayout {
    public final float baseX, baseY;
    public final float finalScale;
    public final float slideOffset;
    public final float globalAlpha;
    public final float visibleRows;
    public final int panelWidth;
    public final int startY;
    public final int itemHeightTotal;

    private final int scX, scW;
    private final int scY_strict, scH_strict;
    private final int scY_loose, scH_loose;

    public OverlayLayout(Minecraft mc, float popupProgress) {
        var cfg = BetterLootingConfig.get();
        this.globalAlpha = (float) cfg.globalAlpha;
        this.panelWidth = cfg.panelWidth;
        this.visibleRows = (float) cfg.visibleRows;

        Window win = mc.getWindow();
        double guiScale = win.getGuiScale();

        this.baseX = (float) (win.getGuiScaledWidth() / 2.0f + cfg.xOffset);
        this.baseY = (float) (win.getGuiScaledHeight() / 2.0f + cfg.yOffset);

        this.finalScale = (float) (cfg.uiScale * Utils.easeOutBack(popupProgress));
        this.slideOffset = (1.0f - popupProgress) * 30.0f;

        this.startY = -(Constants.ITEM_HEIGHT / 2);
        this.itemHeightTotal = Constants.ITEM_HEIGHT + 2;

        double localLeft = Constants.LIST_X - 25.0;
        double currentXOnScreen = this.baseX + ((localLeft + slideOffset) * finalScale);
        this.scX = (int) (currentXOnScreen * guiScale);
        this.scW = (int) ((panelWidth + 30.0) * finalScale * guiScale);

        double listTopOnScreen = this.baseY + (this.startY * finalScale);

        int listPhyH = (int) ((visibleRows * itemHeightTotal) * finalScale * guiScale);

        int topBuffer = (int) (itemHeightTotal * 1.5 * finalScale * guiScale);

        this.scY_strict = (int) (win.getHeight() - (listTopOnScreen * guiScale) - listPhyH);
        this.scH_strict = listPhyH + topBuffer;

        int looseExt = (int) (50.0 * guiScale);
        this.scY_loose = scY_strict - looseExt;
        this.scH_loose = scH_strict + (looseExt * 2);
    }

    public void applyStrictScissor() {
        setScissor(scY_strict, scH_strict);
    }

    public void applyLooseScissor() {
        setScissor(scY_loose, scH_loose);
    }

    private void setScissor(int y, int h) {
        RenderSystem.enableScissor(
                Math.max(0, scX),
                Math.max(0, y),
                Math.max(1, scW),
                Math.max(1, h)
        );
    }
}