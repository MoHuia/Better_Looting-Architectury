package com.mohuia.better_looting.platform.fabric;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 软依赖 KubeJS 兼容层 (Fabric 端) - 高兼容正式版
 * 使用纯反射调用 KubeJS 拾取事件，适配 KubeJS 多版本路径。
 * 优化：无 KubeJS 环境下静默跳过，不再向玩家聊天框输出刷屏报错。
 */
public class KubeJSCompat {

    private static boolean initialized = false;
    private static Object pickedUpEventHandler = null;
    private static Method postMethod = null;
    private static Constructor<?> eventConstructor = null;

    public static void firePickup(ServerPlayer player, ItemEntity itemEntity, ItemStack stack) {
        if (!initialized) initReflection();

        // 如果成功找到了 KubeJS 的目标类和方法，则执行调用 (没找到说明没装KubeJS，直接静默跳过)
        if (pickedUpEventHandler != null && postMethod != null && eventConstructor != null) {
            try {
                Object eventJS = null;
                int paramCount = eventConstructor.getParameterCount();

                // 智能适配 KubeJS 不同版本的 Event 构造函数
                if (paramCount == 3) {
                    eventJS = eventConstructor.newInstance(player, itemEntity, stack);
                } else if (paramCount == 2) {
                    eventJS = eventConstructor.newInstance(player, itemEntity);
                }

                // 抛出事件
                if (eventJS != null) {
                    int postParamCount = postMethod.getParameterCount();
                    if (postParamCount == 1) postMethod.invoke(pickedUpEventHandler, eventJS);
                    else if (postParamCount == 2) postMethod.invoke(pickedUpEventHandler, null, eventJS);
                }
            } catch (Throwable t) {
                // 仅在执行阶段出错（说明装了KubeJS但内部API发生未知变更）时才在后台打印，不打扰普通玩家
                t.printStackTrace();
            }
        }
    }

    // 移除入参 player，因为我们不再向玩家发消息了
    private static void initReflection() {
        initialized = true;
        try {
            // 1. 寻找 ItemEvents.PICKED_UP 静态事件处理器实例 (适配 KubeJS 6 结构变动)
            String[] possibleItemEventsClasses = {
                    "dev.latvian.mods.kubejs.item.ItemEvents",
                    "dev.latvian.mods.kubejs.bindings.ItemEvents"
            };

            for (String className : possibleItemEventsClasses) {
                try {
                    Class<?> itemEventsClass = Class.forName(className);
                    Field pickedUpField = itemEventsClass.getField("PICKED_UP");
                    pickedUpEventHandler = pickedUpField.get(null);
                    break;
                } catch (ClassNotFoundException | NoSuchFieldException ignored) {}
            }

            // 2. 获取 EventHandler 类的 post() 方法
            try {
                Class<?> eventHandlerClass = Class.forName("dev.latvian.mods.kubejs.event.EventHandler");
                for (Method method : eventHandlerClass.getMethods()) {
                    if (method.getName().equals("post") && method.getParameterCount() >= 1) {
                        postMethod = method;
                        break;
                    }
                }
            } catch (ClassNotFoundException ignored) {}

            // 3. 获取 ItemPickupEventJS 内部类及构造函数 (穷举了 KubeJS 可能的所有拾取事件类名)
            String[] possibleEventClasses = {
                    "dev.latvian.mods.kubejs.item.ItemPickupEventJS",
                    "dev.latvian.mods.kubejs.item.ItemPickedUpEventJS",
                    "dev.latvian.mods.kubejs.item.entity.ItemPickupEventJS",
                    "dev.latvian.mods.kubejs.item.entity.ItemPickedUpEventJS",
                    "dev.latvian.mods.kubejs.item.custom.ItemPickupEventJS",
                    "dev.latvian.mods.kubejs.item.custom.ItemPickedUpEventJS",
                    "dev.latvian.mods.kubejs.item.ItemEntityPickedUpEventJS"
            };

            for (String className : possibleEventClasses) {
                try {
                    Class<?> eventJSClass = Class.forName(className);
                    Constructor<?>[] constructors = eventJSClass.getConstructors();
                    if (constructors.length > 0) {
                        eventConstructor = constructors[0];
                        // 寻找参数最多的构造函数
                        for (Constructor<?> c : constructors) {
                            if (c.getParameterCount() > eventConstructor.getParameterCount()) {
                                eventConstructor = c;
                            }
                        }
                        break; // 找到了就跳出循环
                    }
                } catch (ClassNotFoundException ignored) {}
            }

            // 【注】移除了手动抛出 RuntimeException 的校验逻辑。
            // 因为如果是纯净端没装 KubeJS，这三者必然是 null，这是正常的预期行为，不需要报错。

        } catch (Throwable t) {
            // 捕获不可预见的底层错误，改为后台打印
            t.printStackTrace();
        }
    }
}