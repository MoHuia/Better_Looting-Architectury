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


public class FilterWhitelist {
    public static final FilterWhitelist INSTANCE = new FilterWhitelist();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<WhitelistEntry> entries = new LinkedHashSet<>();
    private Path configPath;
    private final List<ItemStack> displayCache = new ArrayList<>();
    private boolean isDirty = true;

    public void init() {
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
        if (id.getPath().equals("air")) return;

        String nbtStr = stack.hasTag() ? stack.getTag().toString() : null;
        WhitelistEntry entry = new WhitelistEntry(id.toString(), nbtStr);

        if (entries.add(entry)) {
            isDirty = true;
            save();
        }
    }

    public void remove(ItemStack stack) {
        if (stack.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String nbtStr = stack.hasTag() ? stack.getTag().toString() : null;
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

    public boolean contains(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (WhitelistEntry entry : entries) {
            if (entry.matches(stack)) return true;
        }
        return false;
    }

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

    public static class WhitelistEntry {
        public String id;
        public String nbt;
        private transient CompoundTag cachedTag;
        private transient boolean tagParsed = false;

        public WhitelistEntry() {}
        public WhitelistEntry(String id, String nbt) {
            this.id = id;
            this.nbt = nbt;
        }

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

        public boolean matches(ItemStack stack) {
            ResourceLocation stackId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (!stackId.toString().equals(this.id)) return false;

            CompoundTag stackTag = stack.getTag();
            CompoundTag entryTag = getTag();

            if (entryTag == null || entryTag.isEmpty()) {
                return stackTag == null || stackTag.isEmpty();
            } else {
                if (stackTag == null) return false;
                return NbtUtils.compareNbt(entryTag, stackTag, true);
            }
        }

        public ItemStack createStack() {
            ResourceLocation loc = ResourceLocation.tryParse(id);
            if (loc == null) return ItemStack.EMPTY;

            var item = BuiltInRegistries.ITEM.get(loc);
            if (item == Items.AIR) return ItemStack.EMPTY;

            ItemStack stack = new ItemStack(item);
            CompoundTag tag = getTag();
            if (tag != null) {
                stack.setTag(tag.copy());
            }
            return stack;
        }

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