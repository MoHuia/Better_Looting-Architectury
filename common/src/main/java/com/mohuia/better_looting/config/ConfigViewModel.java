package com.mohuia.better_looting.config;

import com.mohuia.better_looting.client.Constants;
import net.minecraft.util.Mth;

public class ConfigViewModel {

    public float xOffset, yOffset, uiScale;
    public int panelWidth;
    public float visibleRows;
    public float globalAlpha;

    public BetterLootingConfig.ActivationMode activationMode;
    public BetterLootingConfig.ScrollMode scrollMode;
    public float lookDownAngle;

    private float initX, initY, initScale, initRows;
    private int initWidth;

    public ConfigViewModel() {
        loadFromConfig();
    }

    public void loadFromConfig() {
        BetterLootingConfig cfg = BetterLootingConfig.get();
        this.xOffset = cfg.xOffset;
        this.yOffset = cfg.yOffset;
        this.uiScale = cfg.uiScale;
        this.panelWidth = cfg.panelWidth;
        this.visibleRows = cfg.visibleRows;
        this.globalAlpha = cfg.globalAlpha;
        this.activationMode = cfg.activationMode;
        this.scrollMode = cfg.scrollMode;
        this.lookDownAngle = cfg.lookDownAngle;
    }

    public void saveToConfig() {
        BetterLootingConfig cfg = BetterLootingConfig.get();
        cfg.xOffset = this.xOffset;
        cfg.yOffset = this.yOffset;
        cfg.uiScale = this.uiScale;
        cfg.panelWidth = this.panelWidth;
        cfg.visibleRows = this.visibleRows;
        cfg.globalAlpha = this.globalAlpha;
        cfg.activationMode = this.activationMode;
        cfg.scrollMode = this.scrollMode;
        cfg.lookDownAngle = this.lookDownAngle;

        cfg.validate(); // 保存前最后校验一次
        BetterLootingConfig.save();
    }

    public void resetToDefault() {
        BetterLootingConfig defaults = new BetterLootingConfig();
        this.xOffset = defaults.xOffset;
        this.yOffset = defaults.yOffset;
        this.uiScale = defaults.uiScale;
        this.panelWidth = defaults.panelWidth;
        this.visibleRows = defaults.visibleRows;
        this.globalAlpha = defaults.globalAlpha;
        this.activationMode = defaults.activationMode;
        this.scrollMode = defaults.scrollMode;
        this.lookDownAngle = defaults.lookDownAngle;
    }

    public record PreviewBounds(float left, float top, float right, float bottom) {}

    public PreviewBounds calculatePreviewBounds(int screenWidth, int screenHeight) {
        float baseX = (float) (screenWidth / 2.0f + this.xOffset);
        float baseY = (float) (screenHeight / 2.0f + this.yOffset);
        float scale = this.uiScale;

        float itemHeight = Constants.ITEM_HEIGHT;
        float startY = -(itemHeight / 2);
        float localMinY = startY - 14;
        float localHeight = (this.visibleRows * (itemHeight + 2)) + 14;

        float left = baseX + (Constants.LIST_X * scale);
        float right = left + (this.panelWidth * scale);
        float top = baseY + (localMinY * scale);
        float bottom = top + (localHeight * scale);

        return new PreviewBounds(left, top, right, bottom);
    }

    public void captureSnapshot() {
        this.initX = xOffset;
        this.initY = yOffset;
        this.initScale = uiScale;
        this.initWidth = panelWidth;
        this.initRows = visibleRows;
    }

    public void updatePosition(double deltaX, double deltaY) {
        this.xOffset = initX + (float) deltaX;
        this.yOffset = initY + (float) deltaY;
    }

    public void updateWidth(double deltaX) {
        float scaledDelta = (float) deltaX / uiScale;
        this.panelWidth = (int) Mth.clamp(initWidth + scaledDelta, 80, 500);
    }

    public void updateRows(double deltaY) {
        float itemTotalHeight = Constants.ITEM_HEIGHT + 2;
        float scaledDelta = (float) deltaY / uiScale;
        float rowDelta = scaledDelta / itemTotalHeight;
        this.visibleRows = Mth.clamp(initRows + rowDelta, 1.0f, 20.0f);
    }

    public void updateScale(double deltaX, double deltaY) {
        float sensitivity = 0.005f;
        this.uiScale = Mth.clamp(initScale + (float) (deltaX + deltaY) * sensitivity, 0.1f, 4.0f);
    }
}