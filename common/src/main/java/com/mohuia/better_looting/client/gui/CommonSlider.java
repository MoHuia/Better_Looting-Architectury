package com.mohuia.better_looting.client.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class CommonSlider extends AbstractSliderButton {
    private final double min, max;
    private final Consumer<Double> setter;
    private final Component prefix;

    public CommonSlider(int x, int y, int width, int height, Component prefix, double min, double max, double current, Consumer<Double> setter) {
        super(x, y, width, height, prefix, (current - min) / (max - min));
        this.prefix = prefix;
        this.min = min;
        this.max = max;
        this.setter = setter;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        double val = min + (value * (max - min));
        // 这里可以根据需要格式化小数位数
        setMessage(prefix.copy().append(": " + String.format("%.2f", val)));
    }

    @Override
    protected void applyValue() {
        setter.accept(min + (value * (max - min)));
    }
}