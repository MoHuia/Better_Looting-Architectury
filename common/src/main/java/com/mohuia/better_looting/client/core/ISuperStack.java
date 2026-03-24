package com.mohuia.better_looting.client.core;

public interface ISuperStack {
    /**
     * 获取超出 64 堆叠上限的额外物品数量
     */
    int betterlooting$getExtraCount();

    /**
     * 设置额外物品数量
     */
    void betterlooting$setExtraCount(int count);

    /**
     * 增加额外物品数量
     */
    void betterlooting$addExtraCount(int count);
}