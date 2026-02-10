package com.yourname.gtstracker.chat;

import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.PokemonListing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GTSMessageParser {
    private static final Pattern POKEMON_PATTERN = Pattern.compile(
        "\\[GTS\\]\\s+(\\w+)\\s+listed\\s+(Shiny\\s+)?([A-Za-z\\- ]+)\\s+Lv(\\d+)\\s+for\\s+\\$(\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ITEM_PATTERN = Pattern.compile(
        "\\[GTS\\]\\s+(\\w+)\\s+listed\\s+(\\d+)x\\s+(.+?)\\s+for\\s+\\$(\\d+)",
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
        Matcher pokemon = POKEMON_PATTERN.matcher(message);
        if (pokemon.find()) {
            PokemonListing listing = new PokemonListing();
            listing.setSeller(pokemon.group(1));
            listing.setShiny(pokemon.group(2) != null && !pokemon.group(2).isBlank());
            listing.setSpecies(pokemon.group(3).trim());
            listing.setLevel(Integer.parseInt(pokemon.group(4)));
            listing.setPrice(Integer.parseInt(pokemon.group(5)));
            return listing;
        }

        Matcher item = ITEM_PATTERN.matcher(message);
        if (item.find()) {
            ItemListing listing = new ItemListing();
            listing.setSeller(item.group(1));
            listing.setQuantity(Integer.parseInt(item.group(2)));
            listing.setItemName(item.group(3).trim());
            listing.setPrice(Integer.parseInt(item.group(4)));
            return listing;
        }

        return null;
    }
}
