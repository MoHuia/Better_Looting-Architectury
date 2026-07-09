package com.mohuia.better_looting.client.overlay;

import com.mohuia.better_looting.client.Constants;
import com.mohuia.better_looting.BetterLooting;
import com.mohuia.better_looting.client.Core;
import com.mohuia.better_looting.config.FilterMode;
import com.mohuia.better_looting.client.KeyInit;
import com.mohuia.better_looting.client.Utils;
import net.minecraft.client.KeyMapping;
import com.mohuia.better_looting.client.core.pipeline.VisualItemEntry;
import com.mohuia.better_looting.client.skin.SkinManager;
import com.mohuia.better_looting.config.BetterLootingConfig;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.Optional;

/**
 * 负责渲染“更好拾取”模组客户端界面的核心渲染器。
 * 处理所有 UI 元素的绘制，包括物品列表、过滤器标签、滚动条以及按键提示等。
 */
public class OverlayRenderer {
    private final Minecraft mc;

    public OverlayRenderer(Minecraft mc) {
        this.mc = mc;
    }

    /**
     * 渲染顶部的过滤器标签（例如：全部、仅稀有）。
     */
    public void renderFilterTabs(GuiGraphics gui, int x, int y) {
        RenderSystem.enableBlend();
        var mode = Core.INSTANCE.getFilterMode();
        drawTab(gui, x, y, mode == FilterMode.ALL, 0xFFFFFFFF);
        drawTab(gui, x + 9, y, mode == FilterMode.RARE_ONLY, 0xFFFFD700);
    }

    /**
     * 绘制单个标签的背景与边框。
     * 根据是否处于激活状态 (active) 来应用不同的透明度和颜色。
     */
    private void drawTab(GuiGraphics gui, int x, int y, boolean active, int color) {
        int bg = active ? (color & 0x00FFFFFF) | 0x80000000 : 0x40000000;
        int border = active ? color : Utils.colorWithAlpha(color, 136);
        renderRoundedRect(gui, x, y - 8, 6, 6, bg);
        gui.renderOutline(x, y - 8, 6, 6, border);
    }

    /**
     * 渲染物品列表中的单行条目。
     * 包括背景色块、物品图标、数量、名称以及 "NEW" 标签。
     */
    public void renderItemRow(GuiGraphics gui, int x, int y, int width, VisualItemEntry entry, boolean selected, float bgAlpha, float textAlpha, boolean isNew) {
        renderItemRow(gui, x, y, width, entry, selected, bgAlpha, textAlpha, isNew, false);
    }

    /**
     * 渲染物品列表中的单行条目。
     * 包括背景色块、物品图标、数量、名称以及 "NEW" 标签。
     *
     * @param useSkin 为 true 时用九宫格贴图背景（HUD 悬浮窗），否则用原版纯色矩形（背包列表 / 配置预览）。
     */
    public void renderItemRow(GuiGraphics gui, int x, int y, int width, VisualItemEntry entry, boolean selected, float bgAlpha, float textAlpha, boolean isNew, boolean useSkin) {
        ItemStack stack = entry.getItem();
        int count = entry.getCount();

        // 渲染条目背景（选中状态会有不同的颜色高亮）
        if (useSkin) {
            renderRowBackgroundTexture(gui, x, y, width, Constants.ITEM_HEIGHT, selected, bgAlpha);
        } else {
            int bgColor = selected ? Constants.COLOR_BG_SELECTED : Constants.COLOR_BG_NORMAL;
            renderRoundedRect(gui, x, y, width, Constants.ITEM_HEIGHT, Utils.applyAlpha(bgColor, bgAlpha));
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int alpha255 = (int) (textAlpha * 255);

        // HUD 贴图模式下，行内内容整体右移让出左侧高亮条
        int ix = x + (useSkin ? HUD_CONTENT_INSET : 0);

        // 绘制基于物品稀有度或自定义颜色的左侧指示条
        // 稀有度条凹槽：在亮底皮肤下先垫一层深色衬底，凸显稀有度条
        if (useSkin && skinRarityBarGroove) {
            gui.fill(ix + 19, y + 2, ix + 22, y + Constants.ITEM_HEIGHT - 2,
                    Utils.colorWithAlpha(0xFF3A2410, alpha255));
        }
        gui.fill(ix + 20, y + 3, ix + 21, y + Constants.ITEM_HEIGHT - 3,
                Utils.colorWithAlpha(Utils.getItemStackDisplayColor(stack), alpha255));

        // 渲染物品模型及数量
        gui.renderItem(stack, ix + 3, y + 3);
        String countText = (count > 1) ? compactCount(count) : null;
        gui.renderItemDecorations(mc.font, stack, ix + 3, y + 3, countText);

        // 当透明度过低时跳过文本渲染以优化性能
        if (alpha255 <= 10) return;

        var pose = gui.pose();
        // 贴图模式套用皮肤文字主题；纯色底模式沿用原版亮色
        int baseTextColor = useSkin
                ? (selected ? skinTextSelected : skinTextNormal)
                : (selected ? Constants.COLOR_TEXT_WHITE : Constants.COLOR_TEXT_DIM);
        int textColor = Utils.colorWithAlpha(baseTextColor, alpha255);

        // 使用 PoseStack 进行缩放，使文本适应 UI 比例
        pose.pushPose();
        pose.translate(ix + 26, y + 8, 0);
        pose.scale(0.75f, 0.75f, 1.0f);

        // 特殊处理附魔书：如果物品是附魔书，优先显示第一个附魔的名称而不是统一的“附魔书”
        Component displayName = stack.getHoverName();
        if (stack.getItem() instanceof EnchantedBookItem) {
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            if (!enchants.isEmpty()) {
                Map.Entry<Enchantment, Integer> first = enchants.entrySet().iterator().next();
                displayName = first.getKey().getFullname(first.getValue());
            }
        }

        gui.drawString(mc.font, displayName, 0, 0, textColor, false);
        pose.popPose();

        // 渲染新物品标签提醒（文本可自定义，留空则隐藏）
        String label = BetterLootingConfig.get().newLabelText;
        if (isNew && label != null && !label.isEmpty()) {
            pose.pushPose();
            pose.translate(x + width - 22, y + 8, 0);
            pose.scale(0.75f, 0.75f, 1.0f);
            // 皮肤模式用皮肤指定的 NEW 标签颜色，否则用默认亮橙色
            int newColor = useSkin ? skinNewLabelColor : Constants.COLOR_NEW_LABEL;
            gui.drawString(mc.font, label, 0, 0, Utils.colorWithAlpha(newColor, alpha255), false);
            pose.popPose();
        }
    }

    /**
     * 将超过 10000 的数量格式化为 k 单位（例如 12000 -> 12k）。
     */
    private String compactCount(int count) {
        if (count >= 10000) return (count / 1000) + "k";
        return String.valueOf(count);
    }

    /**
     * 渲染滚动条轨道与滑块。
     */
    public void renderScrollBar(GuiGraphics gui, int total, float maxVis, int x, int y, int h, float alpha, float scroll) {
        gui.fill(x, y, x + 2, y + h, Utils.applyAlpha(Constants.COLOR_SCROLL_TRACK, alpha));
        float ratio = maxVis / total;
        int thumbH = Math.max(10, (int) (h * ratio)); // 滑块最小高度限制为 10px
        float progress = (total - maxVis > 0) ? Mth.clamp(scroll / (total - maxVis), 0f, 1f) : 0f;

        renderRoundedRect(gui, x, y + (int) ((h - thumbH) * progress), 2, thumbH,
                Utils.applyAlpha(Constants.COLOR_SCROLL_THUMB, alpha));
    }

    /**
     * 渲染当前选中项左侧的交互按键提示（如拾取按键）。
     * 包含长文本的滚动动画和长按拾取进度条。
     */
    public void renderKeyPrompt(GuiGraphics gui, int x, int startY, int itemHeight, int selIndex, float scroll, float visibleRows, float bgAlpha) {
        float relSel = selIndex - scroll;
        // 如果选中项在可见范围外，则不渲染提示
        if (relSel <= -1.0f || relSel >= visibleRows + 0.5f) return;

        int y = startY + (int) (relSel * itemHeight) + (itemHeight - 14) / 2;

        // 计算边缘淡出透明度，使超出列表边界时的过渡更自然
        float finalAlpha = bgAlpha * (relSel < 0 ? (1f + relSel) : Mth.clamp((visibleRows + 0.5f) - relSel, 0f, 1f));
        if (finalAlpha <= 0.05f) return;

        int boxX = x - 21, boxY = y, boxSize = 14;
        renderRoundedRect(gui, boxX, boxY, boxSize, boxSize, Utils.applyAlpha(Constants.COLOR_KEY_BG, finalAlpha));

        // 渲染长按拾取进度的填充遮罩
        float progress = Core.INSTANCE.getPickupProgress();
        if (progress > 0.0f) {
            int p = 2, s = boxSize - p * 2;
            int fh = (int) (s * progress);
            gui.fill(boxX + p, boxY + boxSize - p - fh, boxX + p + s, boxY + boxSize - p, Utils.colorWithAlpha(0x80808080, (int) (finalAlpha * 255)));
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        KeyMapping displayKey = KeyInit.PICKUP.isUnbound() ? KeyInit.PICKUP_ALT : KeyInit.PICKUP;
        String text = displayKey.getTranslatedKeyMessage().getString().toUpperCase();
        int tw = mc.font.width(text);
        int tc = Utils.colorWithAlpha(Constants.COLOR_TEXT_WHITE, (int) (finalAlpha * 255));
        int margin = 2, mw = boxSize - (margin * 2);

        gui.pose().pushPose();
        gui.pose().translate(0, 0, 10);

        if (tw <= mw) {
            // 文本较短，直接居中绘制
            gui.drawString(mc.font, text, boxX + (boxSize - tw) / 2, boxY + 3, tc, false);
        } else {
            // 文本超长，使用 Scissor (裁剪测试) 并配合时间函数进行左右滚动显示
            int scX = boxX + margin;
            Matrix4f mat = gui.pose().last().pose();
            Vector4f min = mat.transform(new Vector4f(scX, boxY, 0, 1.0f));
            Vector4f max = mat.transform(new Vector4f(scX + mw, boxY + boxSize, 0, 1.0f));

            Window win = mc.getWindow();
            double s = win.getGuiScale();
            int sx = (int) (min.x() * s), sy = (int) ((win.getGuiScaledHeight() - max.y()) * s);
            int sw = (int) ((max.x() - min.x()) * s), sh = (int) ((max.y() - min.y()) * s);

            RenderSystem.enableScissor(Math.max(0, sx), Math.max(0, sy), Math.max(1, sw), Math.max(1, sh));

            // 使用余弦函数实现平滑的往复滚动效果
            int offset = (int) (((Math.cos(Util.getMillis() / 1000.0) + 1.0) / 2.0) * (tw - mw));
            gui.drawString(mc.font, text, scX - offset, boxY + 3, tc, false);

            RenderSystem.disableScissor();
        }
        gui.pose().popPose();
    }

    /**
     * 渲染物品的原版工具提示 (Tooltip)。
     * 包含了防止 Tooltip 越出屏幕边界的安全判断逻辑。
     */
    public void renderTooltip(GuiGraphics gui, ItemStack stack, int screenW, int screenH, OverlayLayout layout, float scroll, int sel) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        var lines = stack.getTooltipLines(mc.player, mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
        if (lines.isEmpty()) return;

        // 获取模组可能注入的自定义 Tooltip 组件（如图标、进度条等）
        Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> component = stack.getTooltipImage();

        int textW = lines.stream().mapToInt(mc.font::width).max().orElse(0);
        int tw = textW + 20;
        // 有自定义组件时预估额外高度，确保不会被截断
        int th = lines.size() * 10 + 12 + (component.isPresent() ? 20 : 0);

        float relY = sel - scroll;
        int listRight = (int) (layout.baseX + layout.slideOffset + (layout.panelWidth + Constants.LIST_X) * layout.finalScale);
        int listLeft = (int) (layout.baseX + layout.slideOffset + Constants.LIST_X * layout.finalScale);
        int itemCenterY = (int) (layout.baseY + (layout.startY + (relY + 0.5f) * layout.itemHeightTotal) * layout.finalScale);

        // 确保 Tooltip 始终在屏幕可视范围内
        int y = Math.max(8, Math.min(screenH - th - 8, itemCenterY - (th / 2)));
        int gap = 12;
        int x = (listRight + gap + tw < screenW - 8) ? listRight + gap : Math.max(8, listLeft - gap - tw);

        gui.renderTooltip(mc.font, lines, component, x, y + 10);
    }

    /**
     * 使用原版矩形渲染拼凑出一个简单的圆角矩形。
     */
    private void renderRoundedRect(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x + 1, y, x + w - 1, y + h, color);
        gui.fill(x, y + 1, x + w, y + h - 1, color);
    }

    // ===== 九宫格贴图背景 =====
    private static final int BUILTIN_TEX_SIZE = 32;     // 内置皮肤源图尺寸 32x32
    private static final int LEFT_INSET = 6;    // 32px 基准下左段宽度（含左侧高亮条 x=0~3）
    private static final int RIGHT_INSET = 4;   // 32px 基准下右段宽度
    private static final int HUD_CONTENT_INSET = 4;  // HUD 贴图模式行内内容右移量，让出左侧高亮条

    // 左右边框相对图宽的比例（由 32px 基准换算），用于适配 16/32/64 等不同尺寸的外部皮肤
    private static final float LEFT_INSET_RATIO = (float) LEFT_INSET / BUILTIN_TEX_SIZE;   // 6/32
    private static final float RIGHT_INSET_RATIO = (float) RIGHT_INSET / BUILTIN_TEX_SIZE; // 4/32

    private String cachedSkin = null;
    private ResourceLocation skinNormal;
    private ResourceLocation skinSelected;
    private int skinTexSize = BUILTIN_TEX_SIZE;  // 当前皮肤源图尺寸

    // 皮肤文字主题与稀有度条凹槽
    private boolean skinRarityBarGroove = false;
    private int skinTextSelected = Constants.COLOR_TEXT_WHITE;
    private int skinTextNormal = Constants.COLOR_TEXT_DIM;
    private int skinNewLabelColor = Constants.COLOR_NEW_LABEL;

    // 预览皮肤覆盖名：配置界面预览时设为 viewModel.overlaySkin，使切换皮肤后无需保存即可预览；
    // 为 null 时使用全局配置的 overlaySkin（真实 HUD 场景）。
    private String previewSkinOverride = null;

    /** 设置预览皮肤覆盖名（配置界面调用）；传 null 恢复读取全局配置。 */
    public void setPreviewSkin(String skin) {
        this.previewSkinOverride = skin;
    }

    private void ensureSkinTextures() {
        String requested = previewSkinOverride != null
                ? previewSkinOverride
                : BetterLootingConfig.get().overlaySkin;

        // 选中皮肤缺失时回退 vanilla（不改写配置）
        if (requested == null || !SkinManager.INSTANCE.isAvailable(requested)) {
            if (!SkinManager.isBuiltin(requested)) requested = "vanilla";
        }

        if (requested.equals(cachedSkin)) return;
        cachedSkin = requested;

        SkinManager.LoadedSkin ext = SkinManager.INSTANCE.getExternalSkin(requested);
        if (ext != null) {
            // 外部皮肤：使用动态注册的纹理与 JSON 文字主题
            skinNormal = ext.normalTex;
            skinSelected = ext.selectedTex;
            skinTexSize = ext.texSize;
            skinRarityBarGroove = ext.rarityBarGroove;
            skinTextNormal = ext.textColorNormal;
            skinTextSelected = ext.textColorSelected;
            skinNewLabelColor = ext.newLabelColor;
        } else {
            // 内置皮肤：打包资源路径 + 硬编码主题
            skinNormal = new ResourceLocation(BetterLooting.MODID,
                    "textures/overlay/" + requested + "/row.png");
            skinSelected = new ResourceLocation(BetterLooting.MODID,
                    "textures/overlay/" + requested + "/row_selected.png");
            skinTexSize = BUILTIN_TEX_SIZE;
            applySkinTheme(requested);
        }
    }

    /**
     * 根据内置皮肤名设置对应的文字配色。亮底皮肤使用深色文字以保证可读性。
     */
    private void applySkinTheme(String skin) {
        switch (skin) {
            case "stardew" -> {
                skinRarityBarGroove = true;
                skinTextSelected = 0xFF3A2410; // 深棕（选中）
                skinTextNormal = 0xFF5A3A1E;   // 稍浅棕（普通）
                skinNewLabelColor = 0xFFC38935; // 黄棕色，在亮底上醒目
            }
            default -> {
                skinRarityBarGroove = false;
                skinTextSelected = Constants.COLOR_TEXT_WHITE;
                skinTextNormal = Constants.COLOR_TEXT_DIM;
                skinNewLabelColor = Constants.COLOR_NEW_LABEL;
            }
        }
    }

    /**
     * 横向三段式九宫格绘制物品行背景贴图：
     * 左段（含左侧高亮条）与右段按比例保留、不横向拉伸；中段横向拉伸到剩余宽度。
     * 边框比例按 32px 基准换算，确保 16/32/64 等不同尺寸皮肤的边框视觉一致。
     * 垂直方向行高恒等于图高，整图不做纵向拉伸。透明度通过 shaderColor 的 alpha 通道应用。
     */
    private void renderRowBackgroundTexture(GuiGraphics gui, int x, int y, int width, int height, boolean selected, float alpha) {
        ensureSkinTextures();
        ResourceLocation tex = selected ? skinSelected : skinNormal;
        int texSize = skinTexSize;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Mth.clamp(alpha, 0f, 1f));

        // 源图左右段宽度（按比例换算到当前 texSize，至少 1px）
        int srcLeft = Math.max(1, Math.round(texSize * LEFT_INSET_RATIO));
        int srcRight = Math.max(1, Math.round(texSize * RIGHT_INSET_RATIO));
        int srcMid = Math.max(1, texSize - srcLeft - srcRight);

        // 目标左右段固定用 32px 基准的像素宽度，保持各皮肤在屏幕上的边框观感一致
        int dstLeft = LEFT_INSET;
        int dstRight = RIGHT_INSET;
        int dstMid = Math.max(0, width - dstLeft - dstRight);

        // 左段（含高亮条，整高）
        blitStretch(gui, tex, x, y, dstLeft, height, 0, 0, srcLeft, texSize, texSize);
        // 中段（横向拉伸，整高）
        blitStretch(gui, tex, x + dstLeft, y, dstMid, height, srcLeft, 0, srcMid, texSize, texSize);
        // 右段（整高）
        blitStretch(gui, tex, x + width - dstRight, y, dstRight, height, texSize - srcRight, 0, srcRight, texSize, texSize);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** 将源区域拉伸到目标尺寸。 */
    private void blitStretch(GuiGraphics gui, ResourceLocation tex, int dx, int dy, int dw, int dh, int u, int v, int sw, int sh, int texSize) {
        if (dw <= 0 || dh <= 0) return;
        gui.blit(tex, dx, dy, dw, dh, (float) u, (float) v, sw, sh, texSize, texSize);
    }
}