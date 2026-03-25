package com.mohuia.better_looting.client.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.architectury.platform.Platform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 过滤器白名单数据管理器。
 * 负责在内存中维护过滤列表，并与本地 JSON 文件 (whitelist.json) 进行同步。
 * 采用单例模式。
 */
public class FilterWhitelist {
    public static final FilterWhitelist INSTANCE = new FilterWhitelist();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 使用 LinkedHashSet 确保物品既不重复，又能保持添加时的顺序
    private final Set<WhitelistEntry> entries = new LinkedHashSet<>();
    private Path configPath;

    // UI 渲染缓存，避免在每帧渲染时重复解析 NBT 和创建 ItemStack
    private final List<ItemStack> displayCache = new ArrayList<>();
    private boolean isDirty = true; // 当列表发生变化时标记为 true，触发缓存重建

    public void init() {
        // 使用 Architectury API 获取跨平台的配置文件夹路径
        Path configDir = Platform.getConfigFolder().resolve("better_looting");
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            this.configPath = configDir.resolve("whitelist.json");
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize filter whitelist", e);
        }
    }

    public void add(ItemStack stack) {
        if (stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getPath().equals("air")) return; // 防御性编程：防止空气方块进入白名单

        // 提取 NBT 为字符串进行持久化存储
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        String nbtStr = customData.isEmpty() ? null : customData.getUnsafe().toString();
        WhitelistEntry entry = new WhitelistEntry(id.toString(), nbtStr);

        if (entries.add(entry)) {
            isDirty = true;
            save();
        }
    }

    public void remove(ItemStack stack) {
        if (stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        String nbtStr = customData.isEmpty() ? null : customData.getUnsafe().toString();
        WhitelistEntry entry = new WhitelistEntry(id.toString(), nbtStr);

        if (entries.remove(entry)) {
            isDirty = true;
            save();
        }
    }

    public void clear() {
        if (entries.isEmpty()) return;
        entries.clear();
        isDirty = true;
        save();
    }

    /**
     * 核心逻辑：检查给定的物品是否符合白名单规则。
     * 被战利品拾取逻辑调用。
     */
    public boolean contains(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (WhitelistEntry entry : entries) {
            if (entry.matches(stack)) return true;
        }
        return false;
    }

    /**
     * 获取用于在 UI 中渲染的物品列表。
     * 意图：仅在数据改变 (isDirty) 时重新生成 ItemStack，极大降低渲染性能开销。
     */
    public List<ItemStack> getDisplayItems() {
        if (isDirty) {
            displayCache.clear();
            for (WhitelistEntry entry : entries) {
                ItemStack stack = entry.createStack();
                if (!stack.isEmpty()) displayCache.add(stack);
            }
            isDirty = false;
        }
        return displayCache;
    }

    private void save() {
        if (configPath == null) return;
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(entries, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to save whitelist", e);
        }
    }

    private void load() {
        if (configPath == null || !Files.exists(configPath)) return;
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Set<WhitelistEntry> loaded = GSON.fromJson(reader, new TypeToken<LinkedHashSet<WhitelistEntry>>(){}.getType());
            if (loaded != null) {
                entries.clear();
                entries.addAll(loaded);
                isDirty = true;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load whitelist", e);
        }
    }

    /**
     * 代表白名单中的一个条目，包含物品 ID 和 NBT 字符串。
     * 专用于序列化和反序列化。
     */
    public static class WhitelistEntry {
        public String id;
        public String nbt;

        // 瞬态变量 (transient) 不会被 Gson 序列化到 JSON 中
        private transient CompoundTag cachedTag;
        private transient boolean tagParsed = false;

        public WhitelistEntry() {}
        public WhitelistEntry(String id, String nbt) {
            this.id = id;
            this.nbt = nbt;
        }

        /**
         * 懒加载解析 NBT 字符串为 CompoundTag 对象。
         */
        private CompoundTag getTag() {
            if (!tagParsed) {
                if (nbt != null && !nbt.isEmpty()) {
                    try {
                        cachedTag = TagParser.parseTag(nbt);
                    } catch (Exception e) {
                        cachedTag = null;
                    }
                }
                tagParsed = true;
            }
            return cachedTag;
        }

        /**
         * 验证传入的 ItemStack 是否与此条目匹配。
         */
        public boolean matches(ItemStack stack) {
            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!stackId.toString().equals(this.id)) return false;

            CompoundTag entryTag = getTag();

            CustomData stackData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            // 将其转换为旧版的 CompoundTag 以便进行兼容性比较
            CompoundTag stackTag = stackData.isEmpty() ? null : stackData.getUnsafe();

            if (entryTag == null || entryTag.isEmpty()) {
                return stackTag == null || stackTag.isEmpty();
            } else {
                if (stackTag == null) return false;
                // 继续使用原版的子集匹配逻辑，但这现在只对 CustomData 生效
                return NbtUtils.compareNbt(entryTag, stackTag, true);
            }
        }

        /**
         * 根据条目数据还原出具体的 ItemStack 实例，主要用于在 UI 中渲染。
         */
        public ItemStack createStack() {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc == null) return ItemStack.EMPTY;

            var item = BuiltInRegistries.ITEM.get(loc);
            if (item == Items.AIR) return ItemStack.EMPTY;

            ItemStack stack = new ItemStack(item);
            CompoundTag tag = getTag();

            if (tag != null && !tag.isEmpty()) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
            }
            return stack;
        }

        // 重写 equals 和 hashCode 是使用 HashSet/LinkedHashSet 存储去重的必要条件
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WhitelistEntry that)) return false;
            return Objects.equals(id, that.id) && Objects.equals(nbt, that.nbt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, nbt);
        }
    }
}