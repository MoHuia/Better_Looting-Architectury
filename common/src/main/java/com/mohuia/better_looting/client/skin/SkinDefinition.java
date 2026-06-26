package com.mohuia.better_looting.client.skin;

/**
 * 玩家自定义皮肤的 skin.json 数据模型，供 Gson 直接反序列化。
 * 所有字段均为可选；缺失字段保持 Java 默认值（null / false），由 {@link SkinManager}
 * 在加载时回退到内置 vanilla 皮肤的默认值。
 *
 * <p>JSON 示例：
 * <pre>
 * {
 *   "displayName": "星露谷-粉色",
 *   "normalTexture": "row.png",
 *   "selectedTexture": "row_selected.png",
 *   "textColorNormal": "#FF5A3A1E",
 *   "textColorSelected": "#FF3A2410",
 *   "rarityBarGroove": true,
 *   "newLabelColor": "#FFC38935"
 * }
 * </pre>
 */
public class SkinDefinition {
    /** 界面显示名；缺省时由 SkinManager 回退为文件夹名。 */
    public String displayName;

    /** 普通行背景贴图文件名（相对皮肤文件夹）；缺省 row.png。 */
    public String normalTexture;

    /** 选中行背景贴图文件名（相对皮肤文件夹）；缺省 row_selected.png。 */
    public String selectedTexture;

    /** 普通行文字颜色，#AARRGGBB 或 #RRGGBB；缺省/解析失败用 vanilla 默认。 */
    public String textColorNormal;

    /** 选中行文字颜色，#AARRGGBB 或 #RRGGBB；缺省/解析失败用 vanilla 默认。 */
    public String textColorSelected;

    /** 是否显示稀有度条凹槽（深色衬底，凸显稀有度条）；缺省 false。 */
    public boolean rarityBarGroove;

    /** NEW 标签文字颜色，#AARRGGBB 或 #RRGGBB；缺省/解析失败用亮橙色。 */
    public String newLabelColor;

    public String getNormalTexture() {
        return (normalTexture == null || normalTexture.trim().isEmpty()) ? "row.png" : normalTexture.trim();
    }

    public String getSelectedTexture() {
        return (selectedTexture == null || selectedTexture.trim().isEmpty()) ? "row_selected.png" : selectedTexture.trim();
    }

    /**
     * 解析 #AARRGGBB / #RRGGBB 颜色字符串为 ARGB int。
     * 6 位（#RRGGBB）自动补 0xFF 不透明。无法解析时抛出 {@link IllegalArgumentException}。
     */
    public static int parseColor(String hex) {
        if (hex == null) throw new IllegalArgumentException("颜色为空");
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 6) {
            return 0xFF000000 | (int) (Long.parseLong(s, 16) & 0xFFFFFF);
        } else if (s.length() == 8) {
            return (int) (Long.parseLong(s, 16) & 0xFFFFFFFFL);
        }
        throw new IllegalArgumentException("颜色格式应为 #RRGGBB 或 #AARRGGBB: " + hex);
    }
}
