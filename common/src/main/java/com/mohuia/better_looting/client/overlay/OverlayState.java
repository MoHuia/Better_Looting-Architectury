package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.config.BetterLootingConfig;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 维护覆盖层 UI 的动画和滚动状态。
 * 利用基于 Delta Time 的数学函数，确保动画和滚动在不同帧率下都能保持平滑一致。
 */
public class OverlayState {
    public float currentScroll = 0f;
    public float popupProgress = 0f;

    // 追踪每个物品实体 (Entity ID) 进入列表时的淡入/滑入动画进度
    private final Int2FloatMap itemEntryAnimations = new Int2FloatOpenHashMap();

    private long lastFrameTime = -1;
    private float deltaTime = 0f;

    /**
     * 每帧调用，更新 UI 的滚动值与弹出动画状态。
     *
     * @param shouldShow  界面当前是否应该显示
     * @param targetScroll 目标滚动位置
     * @param itemCount    列表中物品的总数量
     * @param visibleRows  当前可视区域内能显示的行数
     */
    public void tick(boolean shouldShow, float targetScroll, int itemCount, float visibleRows) {
        long now = System.nanoTime();
        if (lastFrameTime == -1) lastFrameTime = now;

        // 计算两次 tick 之间的时间差（秒），并限制最大值为 0.1 秒以防止长时间卡顿后发生突变
        this.deltaTime = Math.min((float) ((now - lastFrameTime) / 1_000_000_000.0), 0.1f);
        this.lastFrameTime = now;

        // 更新界面弹出的渐变进度 (0.0 完全隐藏 -> 1.0 完全显示)
        float targetPopup = shouldShow ? 1.0f : 0.0f;
        float speedMultiplier = getAnimationSpeedMultiplier();

        if (speedMultiplier <= 0f) {
            // 关闭动画：直接设置到目标值
            this.popupProgress = targetPopup;
        } else {
            this.popupProgress = damp(this.popupProgress, targetPopup, 10.0f * speedMultiplier, deltaTime);
            // 收敛吸附：damp 指数衰减末尾有数帧微变，slideOffset 慢漂导致
            // 矩阵 translate 亚像素位移 → 纹理/字体/边框逐帧抖动。0.97 时
            // easeOutCubic≈0.9987、slideOffset=0.9px，一跳完全不可见。
            if (Math.abs(this.popupProgress - targetPopup) < 0.005f) {
                this.popupProgress = targetPopup;
            }
        }

        // 如果界面已隐藏且动画结束，清空动画缓存释放内存
        if (!shouldShow && this.popupProgress < 0.001f) {
            this.popupProgress = 0f;
            this.itemEntryAnimations.clear();
            return;
        }

        // 处理平滑滚动逻辑
        float maxScroll = Math.max(0, itemCount - visibleRows);
        float clampedTarget = Mth.clamp(targetScroll, 0, maxScroll);

        // 0.04≈0.8px 视觉差，配合 Math.round 给足安全间距，杜绝末尾亚像素抖变
        if (Math.abs(this.currentScroll - clampedTarget) < 0.04f) {
            this.currentScroll = clampedTarget;
        } else if (speedMultiplier <= 0f) {
            this.currentScroll = clampedTarget;
        } else {
            this.currentScroll = damp(this.currentScroll, clampedTarget, 15.0f * speedMultiplier, deltaTime);
        }
    }

    /**
     * 获取指定实体的 UI 进场动画进度，并推动进度条。
     * * @param entityId 物品实体的 ID
     * @return 0.0f 到 1.0f 之间的浮点数，代表动画完成度
     */
    public float getItemEntryProgress(int entityId) {
        float current = itemEntryAnimations.get(entityId);
        if (current >= 1.0f) return 1.0f;

        float multiplier = getAnimationSpeedMultiplier();
        if (multiplier <= 0f) {
            // 关闭动画：直接完成
            itemEntryAnimations.put(entityId, 1.0f);
            return 1.0f;
        }

        // 以每秒进度增加 6.0 * multiplier 的速度播放动画
        float next = Math.min(1.0f, current + (6.0f * multiplier * deltaTime));
        // 收敛吸附：entryProgress 末尾微变 → entryOffset 亚像素漂移 → 行内所有元素抖动
        if (next >= 0.97f) next = 1.0f;
        itemEntryAnimations.put(entityId, next);
        return next;
    }

    /**
     * 清理已经不在视野范围内的物品动画状态，避免内存泄漏。
     * 通常在获取到新的物品列表快照后调用。
     */
    public void cleanupAnimations(List<VisualItemEntry> currentItems) {
        if (itemEntryAnimations.isEmpty()) return;

        Set<Integer> currentIds = currentItems.stream()
                .map(VisualItemEntry::getPrimaryId)
                .collect(Collectors.toSet());

        // 保留仍在列表中的实体 ID，丢弃其他的
        itemEntryAnimations.keySet().retainAll(currentIds);
    }

    /**
     * 基于指数衰减的帧率无关平滑阻尼函数 (Frame-rate independent damping)。
     * 替代简单的线性插值 (Lerp)，保证在高低帧率下动画手感一致。
     */
    private float damp(float current, float target, float speed, float dt) {
        return Mth.lerp(1.0f - (float) Math.exp(-speed * dt), current, target);
    }

    /**
     * 根据配置中的动画速度设置返回对应的速度倍率。
     * SLOW=0.5, MEDIUM=1.0, FAST=2.0, OFF=0（关闭动画，直接跳到目标值）
     */
    private float getAnimationSpeedMultiplier() {
        return switch (BetterLootingConfig.get().animationSpeed) {
            case SLOW -> 0.5f;
            case MEDIUM -> 1.0f;
            case FAST -> 2.0f;
            case OFF -> 0.0f;
        };
    }
}
