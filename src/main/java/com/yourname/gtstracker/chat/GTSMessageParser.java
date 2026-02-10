package com.yourname.gtstracker.chat;

import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.PokemonListing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GTSMessageParser {
    private static final Pattern POKEMON_PATTERN = Pattern.compile(
        "\\[GTS\\]\\s+(\\w+)\\s+listed\\s+(Shiny\\s+)?([A-Za-z\\- ]+)\\s+Lv([^\\s]+)\\s+for\\s+\\$([^\\s]+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ITEM_PATTERN = Pattern.compile(
        "\\[GTS\\]\\s+(\\w+)\\s+listed\\s+([^\\s]+)x\\s+(.+?)\\s+for\\s+\\$([^\\s]+)",
        Pattern.CASE_INSENSITIVE
    );

    private GTSMessageParser() {
    }

    public static boolean isGTSMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return message.contains("[GTS]") || message.toLowerCase().contains(" listed ");
    }

    public static ListingData parse(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher pokemon = POKEMON_PATTERN.matcher(message);
        if (pokemon.find()) {
            int level = parseIntSafe(pokemon.group(4), -1, 1, 100);
            int price = parseIntSafe(pokemon.group(5), -1, 1, Integer.MAX_VALUE);
            if (level < 0 || price < 0) {
                return null;
            }

            PokemonListing listing = new PokemonListing();
            listing.setSeller(pokemon.group(1));
            listing.setShiny(pokemon.group(2) != null && !pokemon.group(2).isBlank());
            listing.setSpecies(pokemon.group(3).trim());
            listing.setLevel(level);
            listing.setPrice(price);
            return listing;
        }

        Matcher item = ITEM_PATTERN.matcher(message);
        if (item.find()) {
            int quantity = parseIntSafe(item.group(2), -1, 1, Integer.MAX_VALUE);
            int price = parseIntSafe(item.group(4), -1, 1, Integer.MAX_VALUE);
            if (quantity < 0 || price < 0) {
                return null;
            }

            ItemListing listing = new ItemListing();
            listing.setSeller(item.group(1));
            listing.setQuantity(quantity);
            listing.setItemName(item.group(3).trim());
            listing.setPrice(price);
            return listing;
        }

        return null;
    }

    private static int parseIntSafe(String raw, int defaultValue, int min, int max) {
        if (raw == null) {
            return defaultValue;
        }

        String normalized = raw.trim().replace(",", "");
        if (!normalized.matches("-?\\d+")) {
            return defaultValue;
        }

        try {
            int value = Integer.parseInt(normalized);
            if (value < min || value > max) {
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
