package com.mohuia.better_looting.client.gui;

/**
 * 组件悬停过渡进度：将布尔的 hover/选中态平滑为 0.0~1.0 的连续值，
 * 供各控件在颜色、位移上做淡入淡出。基于墙钟时间帧率无关推进。
 * <p>
 * 使用 easeOutCubic 在固定时长 200ms 内完成动画，保证必然到达终点，
 * 消除指数缓出渐近线导致的末尾卡顿。
 */
public class HoverAnim {

    private static final double DURATION_MS = 200.0;

    private double value = 0.0;       // 当前进度
    private double animStart = 0.0;   // 动画起始值
    private double animTarget = 0.0;  // 动画目标值
    private long animStartNs = 0;     // 动画起始时间（纳秒）

    /** 直接跳到目标值，不带动画。用于初始化时避免首次渲染闪烁。 */
    public void snap(boolean active) {
        this.value = active ? 1.0 : 0.0;
        this.animTarget = this.value;
        this.animStart = this.value;
    }

    /**
     * 朝目标（活跃时 1，否则 0）推进一帧并返回当前进度。
     * 目标变化时以当前值为起点启动新动画，保证过渡平滑可中断。
     */
    public float update(boolean active) {
        long now = System.nanoTime();
        double target = active ? 1.0 : 0.0;

        // 目标变化 → 以当前值作为动画起点
        if (Math.abs(target - animTarget) > 0.001) {
            animStart = value;
            animTarget = target;
            animStartNs = now;
        }

        double elapsed = (animStartNs == 0) ? DURATION_MS : (now - animStartNs) / 1_000_000.0;
        double t = Math.min(elapsed / DURATION_MS, 1.0);

        // easeOutCubic：快起慢停，t=1 必然到达目标
        value = animStart + (animTarget - animStart) * (1.0 - Math.pow(1.0 - t, 3.0));

        return (float) value;
    }

    public float value() {
        return (float) value;
    }
}
