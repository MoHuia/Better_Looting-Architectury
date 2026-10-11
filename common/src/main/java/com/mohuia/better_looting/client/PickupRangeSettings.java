package com.mohuia.better_looting.client;

import com.mohuia.better_looting.config.ConfigViewModel;
import com.mohuia.better_looting.network.PickupRangeRules;
import net.minecraft.client.Minecraft;

/** 设置页通过原版指令通道提交，权限和数值最终由服务端 /bl range 校验。 */
public final class PickupRangeSettings {
    private PickupRangeSettings() {}

    public static boolean canEdit() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null || !mc.player.hasPermissions(2)) return false;
        var bl = mc.getConnection().getCommands().getRoot().getChild("bl");
        return bl != null && bl.getChild("range") != null;
    }

    public static void submit(ConfigViewModel model) {
        if (!model.pickupRangeChanged || !canEdit() || !PickupRangeRules.isValid(model.scanRangeXZ, model.scanRangeY)) return;
        Minecraft.getInstance().getConnection().sendCommand("bl range " + model.scanRangeXZ + " " + model.scanRangeY);
        model.pickupRangeChanged = false;
    }
}
