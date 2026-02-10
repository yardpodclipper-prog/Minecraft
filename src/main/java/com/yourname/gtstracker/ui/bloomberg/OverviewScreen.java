package com.yourname.gtstracker.ui.bloomberg;

import com.yourname.gtstracker.data.ListingSnapshot;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class OverviewScreen {
    private final ListingTableWidget table;
    private final SummaryPanel summary;

    public OverviewScreen(int x, int y, int width, int height) {
        int summaryWidth = 220;
        this.table = new ListingTableWidget(x, y, width - summaryWidth - 8, height);
        this.summary = new SummaryPanel(x + width - summaryWidth, y, summaryWidth, height);
    }

    public void resize(int x, int y, int width, int height) {
        int summaryWidth = Math.min(220, Math.max(180, width / 3));
        table.resize(x, y, width - summaryWidth - 8, height);
        summary.resize(x + width - summaryWidth, y, summaryWidth, height);
    }

    public void render(DrawContext context, TextRenderer textRenderer, ListingSnapshot snapshot) {
        table.render(context, textRenderer, snapshot.listings());
        summary.render(context, textRenderer, snapshot);
    }
}
