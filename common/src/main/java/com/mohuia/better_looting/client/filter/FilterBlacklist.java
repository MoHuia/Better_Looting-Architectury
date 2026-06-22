package com.mohuia.better_looting.client.filter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 过滤器黑名单数据管理器。
 * 黑名单中的物品在拾取列表中会被屏蔽（不显示、不可拾取）。
 * 采用单例模式，结构与 FilterWhitelist 一致。
 */
public class FilterBlacklist {
    public static final FilterBlacklist INSTANCE = new FilterBlacklist();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<FilterWhitelist.WhitelistEntry> entries = new LinkedHashSet<>();
    private Path configPath;

    private final List<ItemStack> displayCache = new ArrayList<>();
    private boolean isDirty = true;

    public void init() {
        Path configDir = Platform.getConfigFolder().resolve("better_looting");
        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            this.configPath = configDir.resolve("blacklist.json");
            load();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize filter blacklist", e);
        }
    }

    public void add(ItemStack stack) {
        if (stack.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.getPath().equals("air")) return;
        String nbtStr = stack.hasTag() ? stack.getTag().toString() : null;
        FilterWhitelist.WhitelistEntry entry = new FilterWhitelist.WhitelistEntry(id.toString(), nbtStr);
        if (entries.add(entry)) {
            isDirty = true;
            save();
        }
    }

    public void remove(ItemStack stack) {
        if (stack.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String nbtStr = stack.hasTag() ? stack.getTag().toString() : null;
        FilterWhitelist.WhitelistEntry entry = new FilterWhitelist.WhitelistEntry(id.toString(), nbtStr);
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

    public boolean contains(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (FilterWhitelist.WhitelistEntry entry : entries) {
            if (entry.matches(stack)) return true;
        }
        return false;
    }

    public List<ItemStack> getDisplayItems() {
        if (isDirty) {
            displayCache.clear();
            for (FilterWhitelist.WhitelistEntry entry : entries) {
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
            LOGGER.error("Failed to save blacklist", e);
        }
    }

    private void load() {
        if (configPath == null || !Files.exists(configPath)) return;
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Set<FilterWhitelist.WhitelistEntry> loaded = GSON.fromJson(reader,
                    new TypeToken<LinkedHashSet<FilterWhitelist.WhitelistEntry>>(){}.getType());
            if (loaded != null) {
                entries.clear();
                entries.addAll(loaded);
                isDirty = true;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load blacklist", e);
        }
    }
}
