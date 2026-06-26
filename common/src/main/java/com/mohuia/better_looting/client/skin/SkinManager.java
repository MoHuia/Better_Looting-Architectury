package com.mohuia.better_looting.client.skin;

import com.google.gson.Gson;
import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.Constants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 玩家自定义皮肤管理器（客户端单例）。
 *
 * <p>职责：扫描 {@code config/better_looting/skins/} 下的皮肤文件夹，解析 skin.json、
 * 校验并加载外部 PNG 为运行时动态纹理，收集加载错误，并向渲染器/配置界面提供查询。
 *
 * <p>内置皮肤（vanilla / stardew）不归本类管理，仍由 OverlayRenderer 走打包资源路径；
 * 本类只处理外部皮肤，二者在可用列表与渲染层合并。
 *
 * <p>纹理生命周期：每次 {@link #rescan()} 全量释放上一轮注册的 DynamicTexture 再重建，
 * 因此"修改已有皮肤的图片后重开配置界面"即可看到新效果（无需重启游戏）。
 */
public class SkinManager {
    public static final SkinManager INSTANCE = new SkinManager();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    /** 内置皮肤名（优先级高于外部同名皮肤）。 */
    public static final String[] BUILTIN_SKINS = { "vanilla", "stardew", "terraria" };

    /** 已成功加载的外部皮肤，按文件夹名（即配置中存储的皮肤标识）保序存放。 */
    private final Map<String, LoadedSkin> externalSkins = new LinkedHashMap<>();

    /** 本轮已注册、待下次 rescan 释放的动态纹理 id。 */
    private final List<ResourceLocation> registeredTextures = new ArrayList<>();

    /** 本轮扫描收集到的面向用户的错误信息（中文，含皮肤名+原因）。 */
    private final List<String> errors = new ArrayList<>();

    private Path skinsDir;
    private boolean initialized = false;

    private SkinManager() {}

    /**
     * 一个已加载的外部皮肤的全部渲染数据。
     */
    public static class LoadedSkin {
        public final String name;              // 文件夹名 = 皮肤标识
        public final String displayName;       // 界面显示名
        public final ResourceLocation normalTex;
        public final ResourceLocation selectedTex;
        public final int texSize;              // 纹理边长（正方形，16 的倍数），用于九宫格切片
        public final int textColorNormal;
        public final int textColorSelected;
        public final boolean rarityBarGroove;
        public final int newLabelColor;

        LoadedSkin(String name, String displayName, ResourceLocation normalTex, ResourceLocation selectedTex,
                   int texSize, int textColorNormal, int textColorSelected,
                   boolean rarityBarGroove, int newLabelColor) {
            this.name = name;
            this.displayName = displayName;
            this.normalTex = normalTex;
            this.selectedTex = selectedTex;
            this.texSize = texSize;
            this.textColorNormal = textColorNormal;
            this.textColorSelected = textColorSelected;
            this.rarityBarGroove = rarityBarGroove;
            this.newLabelColor = newLabelColor;
        }
    }

    /**
     * 首次初始化：确保 skins 目录存在并写入说明文件。安全可重复调用。
     */
    public void init() {
        if (initialized) return;
        initialized = true;
        try {
            this.skinsDir = Platform.getConfigFolder().resolve("better_looting").resolve("skins");
            if (!Files.exists(skinsDir)) Files.createDirectories(skinsDir);
            writeReadmeIfAbsent();
        } catch (Exception e) {
            LOGGER.error("[BetterLooting] 初始化皮肤目录失败", e);
        }
    }

    // ===== 查询接口 =====

    /** 指定皮肤名是否为内置皮肤。 */
    public static boolean isBuiltin(String name) {
        for (String b : BUILTIN_SKINS) if (b.equals(name)) return true;
        return false;
    }

    /** 该皮肤名当前是否可用（内置恒可用；外部需成功加载）。 */
    public boolean isAvailable(String name) {
        return isBuiltin(name) || externalSkins.containsKey(name);
    }

    /** 获取已加载的外部皮肤数据；内置或未加载返回 null。 */
    public LoadedSkin getExternalSkin(String name) {
        return externalSkins.get(name);
    }

    /** 可用皮肤名有序列表：内置在前，外部按扫描顺序在后。 */
    public List<String> getAvailableSkins() {
        List<String> list = new ArrayList<>();
        for (String b : BUILTIN_SKINS) list.add(b);
        list.addAll(externalSkins.keySet());
        return list;
    }

    /** 本轮扫描的错误信息（只读快照）。 */
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    // ===== 扫描与加载 =====

    /**
     * 全量重扫描 skins 目录。释放上一轮纹理后逐个皮肤独立加载，
     * 单个皮肤出错只剔除该皮肤并记录错误，不影响其它皮肤。
     */
    public void rescan() {
        init();
        releaseTextures();
        externalSkins.clear();
        errors.clear();
        usedSlugs.clear();

        if (skinsDir == null || !Files.isDirectory(skinsDir)) return;

        List<Path> dirs = new ArrayList<>();
        try (var stream = Files.list(skinsDir)) {
            stream.filter(Files::isDirectory).forEach(dirs::add);
        } catch (Exception e) {
            LOGGER.error("[BetterLooting] 扫描皮肤目录失败", e);
            errors.add("无法读取皮肤目录: " + e.getMessage());
            return;
        }

        for (Path dir : dirs) {
            String folderName = dir.getFileName().toString();
            Path jsonPath = dir.resolve("skin.json");
            // 没有 skin.json 的子文件夹静默忽略（不是皮肤）
            if (!Files.isRegularFile(jsonPath)) continue;

            // 内置同名皮肤优先，外部跳过并报错
            if (isBuiltin(folderName)) {
                errors.add("皮肤名 \"" + folderName + "\" 与内置皮肤冲突，已跳过");
                continue;
            }

            try {
                loadSkin(folderName, dir, jsonPath);
            } catch (Exception e) {
                errors.add("皮肤 \"" + folderName + "\" 加载失败: " + e.getMessage());
                LOGGER.warn("[BetterLooting] 皮肤 {} 加载失败", folderName, e);
            }
        }
    }

    /**
     * 加载单个皮肤。任何校验失败抛异常，由 rescan 捕获为该皮肤的错误。
     */
    private void loadSkin(String folderName, Path dir, Path jsonPath) throws Exception {
        SkinDefinition def;
        try (var reader = Files.newBufferedReader(jsonPath)) {
            def = GSON.fromJson(reader, SkinDefinition.class);
        }
        if (def == null) throw new IllegalArgumentException("skin.json 内容为空或格式错误");

        Path normalFile = dir.resolve(def.getNormalTexture());
        Path selectedFile = dir.resolve(def.getSelectedTexture());
        if (!Files.isRegularFile(normalFile))
            throw new IllegalArgumentException("找不到普通行贴图: " + def.getNormalTexture());
        if (!Files.isRegularFile(selectedFile))
            throw new IllegalArgumentException("找不到选中行贴图: " + def.getSelectedTexture());

        String slug = makeUniqueSlug(folderName);
        TexResult normal = registerTexture(normalFile, "dynamic/skin/" + slug + "/row");
        TexResult selected = registerTexture(selectedFile, "dynamic/skin/" + slug + "/row_selected");
        if (normal.size != selected.size)
            throw new IllegalArgumentException("普通行与选中行贴图尺寸必须一致 ("
                    + normal.size + " vs " + selected.size + ")");

        // 颜色：缺省或解析失败回退 vanilla 默认（解析失败追加一条错误但不阻断皮肤）
        int textNormal = Constants.COLOR_TEXT_DIM;
        int textSelected = Constants.COLOR_TEXT_WHITE;
        if (def.textColorNormal != null && !def.textColorNormal.trim().isEmpty()) {
            try { textNormal = SkinDefinition.parseColor(def.textColorNormal); }
            catch (Exception e) { errors.add("皮肤 \"" + folderName + "\" 普通文字颜色无效，使用默认值: " + e.getMessage()); }
        }
        if (def.textColorSelected != null && !def.textColorSelected.trim().isEmpty()) {
            try { textSelected = SkinDefinition.parseColor(def.textColorSelected); }
            catch (Exception e) { errors.add("皮肤 \"" + folderName + "\" 选中文字颜色无效，使用默认值: " + e.getMessage()); }
        }

        // 稀有度条凹槽开关，缺省 false
        boolean rarityBarGroove = def.rarityBarGroove;

        // NEW 标签颜色，缺省/解析失败用亮橙色
        int newLabelColor = Constants.COLOR_NEW_LABEL;
        if (def.newLabelColor != null && !def.newLabelColor.trim().isEmpty()) {
            try { newLabelColor = SkinDefinition.parseColor(def.newLabelColor); }
            catch (Exception e) { errors.add("皮肤 \"" + folderName + "\" NEW 标签颜色无效，使用默认值: " + e.getMessage()); }
        }

        String displayName = (def.displayName == null || def.displayName.trim().isEmpty())
                ? folderName : def.displayName.trim();

        externalSkins.put(folderName, new LoadedSkin(
                folderName, displayName, normal.id, selected.id, normal.size,
                textNormal, textSelected, rarityBarGroove, newLabelColor));
    }

    /** registerTexture 的返回值：纹理 id + 边长。 */
    private record TexResult(ResourceLocation id, int size) {}

    /**
     * 读取外部 PNG，校验"正方形 + 16 的倍数"，注册为运行时动态纹理。
     */
    private TexResult registerTexture(Path file, String pathSuffix) throws Exception {
        NativeImage image;
        try (InputStream in = Files.newInputStream(file)) {
            image = NativeImage.read(in);
        }
        int w = image.getWidth();
        int h = image.getHeight();
        if (w != h) {
            image.close();
            throw new IllegalArgumentException("贴图必须为正方形 (当前 " + w + "x" + h + "): " + file.getFileName());
        }
        if (w < 16 || w % 16 != 0) {
            image.close();
            throw new IllegalArgumentException("贴图边长必须是 16 的倍数 (当前 " + w + "): " + file.getFileName());
        }

        ResourceLocation id = new ResourceLocation(BetterLooting.MODID, pathSuffix);
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        registeredTextures.add(id);
        return new TexResult(id, w);
    }

    /** 释放上一轮注册的所有动态纹理，避免显存泄漏。 */
    private void releaseTextures() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation id : registeredTextures) {
            try { mc.getTextureManager().release(id); } catch (Exception ignored) {}
        }
        registeredTextures.clear();
    }

    /**
     * 将文件夹名清洗为合法 ResourceLocation path 段（仅 a-z0-9_），
     * 与本轮已用 slug 撞名则追加序号区分。
     */
    private final List<String> usedSlugs = new ArrayList<>();
    private String makeUniqueSlug(String folderName) {
        StringBuilder sb = new StringBuilder();
        for (char c : folderName.toLowerCase().toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') sb.append(c);
            else sb.append('_');
        }
        String base = sb.length() == 0 ? "skin" : sb.toString();
        String slug = base;
        int i = 1;
        while (usedSlugs.contains(slug)) slug = base + "_" + (i++);
        usedSlugs.add(slug);
        return slug;
    }

    // ===== 说明文件 =====

    private void writeReadmeIfAbsent() throws Exception {
        Path readme = skinsDir.resolve("README.txt");
        if (Files.exists(readme)) return;
        Files.writeString(readme, README_CONTENT);
    }

    private static final String README_CONTENT = """
            ===== Better Looting 自定义皮肤说明 =====

            本文件夹用于存放悬浮窗物品行的自定义背景皮肤。

            【目录结构】
            每个皮肤是一个独立的子文件夹，例如：
              skins/
                我的皮肤/
                  skin.json
                  row.png
                  row_selected.png

            子文件夹里必须有 skin.json 才会被识别为皮肤。
            文件夹名就是皮肤标识，可用中文/大写/空格，并作为默认显示名。

            【贴图要求】
            - 必须是正方形 PNG，边长为 16 的倍数（如 16x16、32x32、64x64）。
            - 采用横向三段九宫格渲染：左右边框按比例保留，中段横向拉伸。
            - row.png        普通行背景
            - row_selected.png 选中行背景

            【skin.json 字段】(全部可选，缺省走默认值)
            - displayName        界面显示名，缺省用文件夹名
            - normalTexture      普通行贴图文件名，缺省 row.png
            - selectedTexture    选中行贴图文件名，缺省 row_selected.png
            - textColorNormal    普通行文字颜色，#RRGGBB 或 #AARRGGBB
            - textColorSelected  选中行文字颜色，#RRGGBB 或 #AARRGGBB
            - rarityBarGroove    是否显示稀有度条凹槽(true/false)，缺省 false
            - newLabelColor      NEW 标签文字颜色，#RRGGBB 或 #AARRGGBB

            【JSON 范例】
            {
              "displayName": "我的皮肤",
              "normalTexture": "row.png",
              "selectedTexture": "row_selected.png",
              "textColorNormal": "#5A3A1E",
              "textColorSelected": "#3A2410",
              "rarityBarGroove": true,
              "newLabelColor": "#C38935"
            }

            【使用方法】
            1. 在本文件夹下新建你的皮肤文件夹，按上面结构放入 skin.json 和贴图。
            2. 进入游戏，打开模组配置界面（每次打开都会重新扫描）。
            3. 在"其它设置"里循环切换皮肤即可。
            4. 若皮肤格式有误，会在聊天栏和配置界面顶部提示原因，并自动回退默认皮肤。
            """;
}

