package com.yourname.gtstracker.ui.bloomberg;

import com.yourname.gtstracker.data.MarketListing;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ListingTableWidget {
    private static final int HEADER_BG = 0xFF1F2230;
    private static final int BODY_BG = 0xFF141720;
    private static final int BORDER = 0xFF2A2E39;
    private static final int HEADER_TEXT = 0xFFE2E7F4;
    private static final int BODY_TEXT = 0xFFC5CEE4;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private int x;
    private int y;
    private int width;
    private int height;

    public ListingTableWidget(int x, int y, int width, int height) {
        resize(x, y, width, height);
    }

    public void resize(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, TextRenderer textRenderer, List<MarketListing> rows) {
        context.fill(x, y, x + width, y + height, BODY_BG);
        context.drawBorder(x, y, width, height, BORDER);

        int headerHeight = 14;
        context.fill(x, y, x + width, y + headerHeight, HEADER_BG);

        int col1 = x + 8;
        int col2 = x + (int) (width * 0.45f);
        int col3 = x + (int) (width * 0.65f);
        int col4 = x + (int) (width * 0.82f);

        context.drawText(textRenderer, Text.literal("Display Name"), col1, y + 3, HEADER_TEXT, false);
        context.drawText(textRenderer, Text.literal("Price"), col2, y + 3, HEADER_TEXT, false);
        context.drawText(textRenderer, Text.literal("Last Seen"), col3, y + 3, HEADER_TEXT, false);
        context.drawText(textRenderer, Text.literal("Status"), col4, y + 3, HEADER_TEXT, false);

        int rowHeight = 12;
        int maxRows = Math.max(0, (height - headerHeight - 4) / rowHeight);
        int drawY = y + headerHeight + 2;

        for (int i = 0; i < Math.min(rows.size(), maxRows); i++) {
            MarketListing listing = rows.get(i);
            context.drawText(textRenderer, Text.literal(listing.displayName()), col1, drawY, BODY_TEXT, false);
            context.drawText(textRenderer, Text.literal(String.format("$%,.2f", listing.price())), col2, drawY, BODY_TEXT, false);
            context.drawText(textRenderer, Text.literal(TIME_FORMAT.format(listing.lastSeen())), col3, drawY, BODY_TEXT, false);
            context.drawText(textRenderer, Text.literal(listing.status().name()), col4, drawY, BODY_TEXT, false);
            drawY += rowHeight;
        }
    }
}
