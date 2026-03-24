package com.mohuia.better_looting.client.core;

import com.mohuia.better_looting.config.BetterLootingConfig;
import java.util.ArrayList;
import java.util.List;

public class SelectionManager {
    private List<VisualItemEntry> nearbyItems = new ArrayList<>();
    private int selectedIndex = 0;
    private int targetScrollOffset = 0;

    public void updateItems(List<VisualItemEntry> items) {
        this.nearbyItems = items;
        validateSelection();
    }

    public void performScroll(double delta) {
        if (nearbyItems.isEmpty()) return;
        selectedIndex += (delta > 0) ? -1 : 1;
        validateSelection();
    }

    private void validateSelection() {
        int size = nearbyItems.size();
        if (size == 0) {
            selectedIndex = 0;
            targetScrollOffset = 0;
            return;
        }
        if (selectedIndex < 0) selectedIndex = size - 1;
        if (selectedIndex >= size) selectedIndex = 0;

        double visibleRows = BetterLootingConfig.get().visibleRows;
        if (size > visibleRows) {
            if (selectedIndex + 1 > targetScrollOffset + visibleRows)
                targetScrollOffset = (int) Math.ceil(selectedIndex - visibleRows + 1);
            if (selectedIndex < targetScrollOffset) targetScrollOffset = selectedIndex;
        } else {
            targetScrollOffset = 0;
        }
    }

    public List<VisualItemEntry> getNearbyItems() { return nearbyItems; }
    public int getSelectedIndex() { return selectedIndex; }
    public int getTargetScrollOffset() { return targetScrollOffset; }
}