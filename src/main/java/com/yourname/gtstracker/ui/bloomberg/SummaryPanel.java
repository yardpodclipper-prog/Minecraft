package com.yourname.gtstracker.ui.bloomberg;

import com.yourname.gtstracker.data.ListingSnapshot;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class SummaryPanel {
    private static final int BACKGROUND_COLOR = 0xFF16181D;
    private static final int BORDER_COLOR = 0xFF2A2E39;
    private static final int PRIMARY_TEXT = 0xFFE2E7F4;
    private static final int SECONDARY_TEXT = 0xFF97A4C2;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private int x;
    private int y;
    private int width;
    private int height;

    public SummaryPanel(int x, int y, int width, int height) {
        resize(x, y, width, height);
    }

    public void resize(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, TextRenderer textRenderer, ListingSnapshot snapshot) {
        context.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        context.drawBorder(x, y, width, height, BORDER_COLOR);

        int lineY = y + 10;
        context.drawText(textRenderer, Text.literal("Summary"), x + 10, lineY, PRIMARY_TEXT, false);
        lineY += 14;

        context.drawText(
            textRenderer,
            Text.literal("Total Active Listings: " + snapshot.totalActiveListings()),
            x + 10,
            lineY,
            SECONDARY_TEXT,
            false
        );
        lineY += 12;

        String time = snapshot.lastIngestTime().equals(java.time.Instant.EPOCH)
            ? "N/A"
            : TIME_FORMAT.format(snapshot.lastIngestTime());

        context.drawText(
            textRenderer,
            Text.literal("Latest Ingest: " + time),
            x + 10,
            lineY,
            SECONDARY_TEXT,
            false
        );
    }
}
