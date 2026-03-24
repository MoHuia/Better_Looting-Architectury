package com.mohuia.better_looting.config;

/**
 * 拖拽交互控制器。
 * 处理鼠标的点击、拖拽和释放事件，识别鼠标目前悬停的热区（如：整体、右边缘、底边缘、角落），
 * 并调用 ViewModel 实施对应的形变或位移。
 */
public class DragController {

    public enum DragMode {
        NONE,
        MOVE,          // 移动整体面板
        RESIZE_WIDTH,  // 拖拽右侧边缘调整宽度
        RESIZE_HEIGHT, // 拖拽底部边缘调整可见行数
        RESIZE_SCALE   // 拖拽右下角调整整体缩放比例
    }

    private DragMode currentDragMode = DragMode.NONE;

    // 鼠标按下时的初始坐标
    private double dragStartX, dragStartY;

    // 当前检测面板的四个边界
    private float boxLeft, boxTop, boxRight, boxBottom;

    /**
     * 更新控制器的判定边界。该方法应在每次渲染并计算出新的 Bounds 后调用。
     */
    public void updateBounds(float l, float t, float r, float b) {
        this.boxLeft = l;
        this.boxTop = t;
        this.boxRight = r;
        this.boxBottom = b;
    }

    public DragMode getCurrentDragMode() {
        return currentDragMode;
    }

    // --- 热区检测 (Hit Detection) ---
    // 注意：热区判定向外扩展了 10 个像素（+10），向内扩展了 2 个像素（-2），
    // 这是为了增加鼠标抓取边缘时的容错率，改善用户体验。

    public boolean isOverRight(double x, double y) {
        return x >= boxRight && x <= boxRight + 10 && y >= boxTop && y <= boxBottom;
    }

    public boolean isOverBottom(double x, double y) {
        return x >= boxLeft && x <= boxRight && y >= boxBottom && y <= boxBottom + 10;
    }

    public boolean isOverCorner(double x, double y) {
        return x >= boxRight - 2 && x <= boxRight + 10 && y >= boxBottom - 2 && y <= boxBottom + 10;
    }

    public boolean isOverBody(double x, double y) {
        return x >= boxLeft && x <= boxRight && y >= boxTop && y <= boxBottom;
    }

    /**
     * 处理鼠标点击事件。
     * 检测点击位置属于哪个热区，切换到对应的拖拽模式，并让 ViewModel 抓取当前状态快照。
     * @return 如果点击在面板相关区域并成功拦截了事件，则返回 true。
     */
    public boolean onMouseClicked(double mx, double my, ConfigViewModel model) {
        // 判定优先级：角落(缩放) > 边缘(宽高) > 主体(移动)
        if (isOverCorner(mx, my)) {
            currentDragMode = DragMode.RESIZE_SCALE;
        } else if (isOverRight(mx, my)) {
            currentDragMode = DragMode.RESIZE_WIDTH;
        } else if (isOverBottom(mx, my)) {
            currentDragMode = DragMode.RESIZE_HEIGHT;
        } else if (isOverBody(mx, my)) {
            currentDragMode = DragMode.MOVE;
        } else {
            return false; // 点击在空白区域
        }

        this.dragStartX = mx;
        this.dragStartY = my;
        model.captureSnapshot(); // 记录快照，用于后续差值计算
        return true;
    }

    /**
     * 处理鼠标释放事件。重置拖拽状态。
     * @return 如果释放前正在进行拖拽，返回 true；否则返回 false。
     */
    public boolean onMouseReleased() {
        boolean wasDragging = currentDragMode != DragMode.NONE;
        currentDragMode = DragMode.NONE;
        return wasDragging;
    }

    /**
     * 处理鼠标拖拽事件。
     * 计算当前鼠标位置与初始按下位置的差值，并分发给 ViewModel 的相应更新方法。
     */
    public void onMouseDragged(double mx, double my, ConfigViewModel model) {
        if (currentDragMode == DragMode.NONE) return;

        double dx = mx - dragStartX;
        double dy = my - dragStartY;

        switch (currentDragMode) {
            case MOVE -> model.updatePosition(dx, dy);
            case RESIZE_WIDTH -> model.updateWidth(dx);
            case RESIZE_HEIGHT -> model.updateRows(dy);
            case RESIZE_SCALE -> model.updateScale(dx, dy);
        }
    }
}