package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.core.VisualItemEntry;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OverlayState {
    public float currentScroll = 0f;
    public float popupProgress = 0f;

    private final Int2FloatMap itemEntryAnimations = new Int2FloatOpenHashMap();

    private long lastFrameTime = -1;
    private float deltaTime = 0f;

    public void tick(boolean shouldShow, float targetScroll, int itemCount, float visibleRows) {
        long now = System.nanoTime();
        if (lastFrameTime == -1) lastFrameTime = now;

        this.deltaTime = Math.min((float) ((now - lastFrameTime) / 1_000_000_000.0), 0.1f);
        this.lastFrameTime = now;

        float targetPopup = shouldShow ? 1.0f : 0.0f;
        this.popupProgress = damp(this.popupProgress, targetPopup, 10.0f, deltaTime);

        if (!shouldShow && this.popupProgress < 0.001f) {
            this.popupProgress = 0f;
            this.itemEntryAnimations.clear();
            return;
        }

        float maxScroll = Math.max(0, itemCount - visibleRows);
        float clampedTarget = Mth.clamp(targetScroll, 0, maxScroll);

        if (Math.abs(this.currentScroll - clampedTarget) < 0.001f) {
            this.currentScroll = clampedTarget;
        } else {
            this.currentScroll = damp(this.currentScroll, clampedTarget, 15.0f, deltaTime);
        }
    }

    public float getItemEntryProgress(int entityId) {
        float current = itemEntryAnimations.get(entityId);
        if (current >= 1.0f) return 1.0f;

        float next = Math.min(1.0f, current + (6.0f * deltaTime));
        itemEntryAnimations.put(entityId, next);
        return next;
    }

    public void cleanupAnimations(List<VisualItemEntry> currentItems) {
        if (itemEntryAnimations.isEmpty()) return;

        Set<Integer> currentIds = currentItems.stream()
                .map(VisualItemEntry::getPrimaryId)
                .collect(Collectors.toSet());

        itemEntryAnimations.keySet().retainAll(currentIds);
    }

    private float damp(float current, float target, float speed, float dt) {
        // Mth.lerp(fraction, start, end)
        return Mth.lerp(1.0f - (float) Math.exp(-speed * dt), current, target);
    }
}