package com.mohuia.better_looting.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 通用的滑动条 UI 组件。
 * 继承自 Minecraft 原版的 AbstractSliderButton。
 * 通过传入 Consumer 回调函数，极大地提高了组件的复用性，
 * 使得在模组配置界面中快速创建任意数值范围的滑动条变得非常简单。
 */
public class CommonSlider extends AbstractSliderButton {
    // 实际业务逻辑中代表的最小值和最大值
    private final double min, max;
    // 当滑动条的值发生变化时触发的回调函数，用于将新值同步给后端的配置对象
    private final Consumer<Double> setter;
    // 滑动条上显示的文本前缀（例如："拾取半径" 或 "发光高亮透明度"）
    private final Component prefix;

    public CommonSlider(int x, int y, int width, int height, Component prefix, double min, double max, double current, Consumer<Double> setter) {
        // 所以在初始化时，我们必须将传入的当前实际值 (current) 逆向映射到 [0, 1] 的区间内。
        // 公式: (当前值 - 最小值) / (最大值 - 最小值)
        super(x, y, width, height, prefix, (current - min) / (max - min));
        this.prefix = prefix;
        this.min = min;
        this.max = max;
        this.setter = setter;

        // 初始化时必须手动调用一次，确保按钮刚渲染时显示的文字就是正确的初始值
        updateMessage();
    }

    /**
     * 更新滑动条上显示的文本。
     * 每次玩家拖动滑动条导致 this.value 发生变化时，原版底层逻辑都会自动调用此方法。
     */
    @Override
    protected void updateMessage() {
        // 将内部的 [0, 1] 百分比值 (this.value) 映射回实际的业务逻辑值。
        double val = min + (value * (max - min));

        // 格式化输出文字。这里固定保留了两位小数 (%.2f)。
        // 意图：使用 prefix.copy() 是一个极佳的实践，可以防止意外修改或污染原始的 Component 实例，
        // 确保后续拼接的数值字符串不会被累加到原来的前缀上。
        setMessage(prefix.copy().append(": " + String.format("%.2f", val)));
    }

    /**
     * 应用新值。
     * 当滑动条的值发生改变时（玩家拖动过程中或松开鼠标后），触发实际的业务逻辑。
     */
    @Override
    protected void applyValue() {
        // 再次计算出当前的实际值，并通过回调函数 (setter) 将其传递出去。
        // 这种设计实现了彻底的解耦：滑动条组件完全不需要知道数据最终被保存到了哪个 Config 文件里。
        setter.accept(min + (value * (max - min)));
    }
}