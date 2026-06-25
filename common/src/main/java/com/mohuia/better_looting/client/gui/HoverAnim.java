package com.mohuia.better_looting.client.gui;

/**
 * 组件悬停过渡进度：将布尔的 hover/选中态平滑为 0.0~1.0 的连续值，
 * 供各控件在颜色、位移上做淡入淡出，避免瞬时跳变。基于墙钟时间帧率无关推进。
 */
public class HoverAnim {

    /** 过渡时间常数（毫秒），越大越慢越柔。 */
    private static final double TAU = 90.0;

    private double value = 0.0;       // 当前进度
    private long lastMs = 0;          // 上一帧时间戳

    /** 朝目标（hover 时 1，否则 0）推进一帧并返回当前进度。 */
    public float update(boolean active) {
        long now = System.currentTimeMillis();
        long dt = (lastMs == 0) ? 16 : (now - lastMs);
        lastMs = now;

        double target = active ? 1.0 : 0.0;
        double diff = target - value;
        if (Math.abs(diff) < 0.001) {
            value = target;
        } else {
            double alpha = 1.0 - Math.exp(-dt / TAU);
            value += diff * alpha;
        }
        return (float) value;
    }

    public float value() {
        return (float) value;
    }
}
