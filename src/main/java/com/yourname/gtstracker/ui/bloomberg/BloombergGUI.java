package com.yourname.gtstracker.ui.bloomberg;

import com.yourname.gtstracker.data.ListingSnapshotCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BloombergGUI extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger(BloombergGUI.class);
    private static final int BG_COLOR = 0xFF0D1018;
    private static final int HEADER_BG = 0xFF171B27;
    private static final int FOOTER_BG = 0xFF171B27;
    private static final int BORDER = 0xFF2A2E39;
    private static final int TEXT = 0xFFE2E7F4;

    private static final long CACHE_REFRESH_MS = 1_500L;

    private final ListingSnapshotCache snapshotCache;
    private OverviewScreen overview;
    private boolean refreshErrorLogged;

    public BloombergGUI(ListingSnapshotCache snapshotCache) {
        super(Text.literal("GTS Bloomberg"));
        this.snapshotCache = snapshotCache;
    }

    @Override
    protected void init() {
        int contentTop = 28;
        int contentBottom = this.height - 22;
        this.overview = new OverviewScreen(12, contentTop, this.width - 24, contentBottom - contentTop);
    }

    @Override
    public void tick() {
        try {
            snapshotCache.refreshIfStale(System.currentTimeMillis(), CACHE_REFRESH_MS);
            refreshErrorLogged = false;
        } catch (RuntimeException e) {
            if (!refreshErrorLogged) {
                LOGGER.error("Failed to refresh snapshot cache for Bloomberg GUI.", e);
                refreshErrorLogged = true;
            }
        }
    }

    @Override
    public void resize(net.minecraft.client.MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        if (overview != null) {
            int contentTop = 28;
            int contentBottom = this.height - 22;
            overview.resize(12, contentTop, this.width - 24, contentBottom - contentTop);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, this.width, this.height, BG_COLOR);

        renderHeader(context);
        renderFooter(context);

        if (overview != null) {
            try {
                overview.render(context, this.textRenderer, snapshotCache.getSnapshot());
            } catch (RuntimeException e) {
                LOGGER.error("Failed to render Bloomberg overview panel.", e);
                context.drawText(textRenderer, Text.literal("GTSTracker GUI error. Check latest.log."), 12, 30, 0xFFFF6B6B, false);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context) {
        context.fill(0, 0, width, 24, HEADER_BG);
        context.drawBorder(0, 0, width, 24, BORDER);
        context.drawText(textRenderer, Text.literal("GTS TRACKER // BLOOMBERG"), 10, 8, TEXT, false);
    }

    private void renderFooter(DrawContext context) {
        int y = height - 18;
        context.fill(0, y, width, height, FOOTER_BG);
        context.drawBorder(0, y, width, 18, BORDER);
        context.drawText(textRenderer, Text.literal("Press ESC to close"), 10, y + 5, TEXT, false);
    }
}
