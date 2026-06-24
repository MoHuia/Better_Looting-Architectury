package com.mohuia.better_looting.client.gui;

/**
 * 横向滑动过渡动画控制器。
 * 基于墙钟时间推进，使用 easeInOutCubic 缓动，供标签页左右切换使用。
 * direction > 0：新页从右侧进入（旧页向左滑出）；direction < 0：反向。
 */
public class SlideAnimator {

    /** 单次滑动时长（毫秒）。 */
    private static final long DURATION_MS = 260;

    private boolean animating = false;
    private int direction = 1;
    private long startTime = 0;

    /** 启动一次滑动动画。 */
    public void start(int direction) {
        this.direction = direction >= 0 ? 1 : -1;
        this.startTime = System.currentTimeMillis();
        this.animating = true;
    }

    /** 是否仍在动画中（超时后自动结束）。 */
    public boolean isAnimating() {
        if (animating && elapsed() >= DURATION_MS) {
            animating = false;
        }
        return animating;
    }

    /** 缓动后的进度，范围 0.0 ~ 1.0。 */
    public double progress() {
        double t = Math.min(1.0, (double) elapsed() / DURATION_MS);
        return easeInOutCubic(t);
    }

    public int direction() {
        return direction;
    }

    private long elapsed() {
        return System.currentTimeMillis() - startTime;
    }

    private static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }
}
