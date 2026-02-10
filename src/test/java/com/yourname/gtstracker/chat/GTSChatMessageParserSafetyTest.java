package com.yourname.gtstracker.chat;

import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.PokemonListing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GTSChatMessageParserSafetyTest {

    @Test
    void returnsNullForPokemonLevelOverflow() {
        ListingData parsed = GTSMessageParser.parse("[GTS] Ash listed Pikachu Lv999999999999 for $5000");
        assertNull(parsed);
    }

    @Test
    void returnsNullForPriceOverflow() {
        ListingData parsed = GTSMessageParser.parse("[GTS] Ash listed 5x Rare Candy for $999999999999999999");
        assertNull(parsed);
    }

    @Test
    void returnsNullForMalformedNumericTokens() {
        assertNull(GTSMessageParser.parse("[GTS] Ash listed Charizard Lvabc for $5000"));
        assertNull(GTSMessageParser.parse("[GTS] Ash listed 2x Ultra Ball for $15k"));
        assertNull(GTSMessageParser.parse("[GTS] Ash listed 2x Ultra Ball for $5000x"));
    }

    @Test
    void stillParsesValidNumericValues() {
        ListingData pokemon = GTSMessageParser.parse("[GTS] Ash listed Shiny Gengar Lv55 for $250000");
        assertInstanceOf(PokemonListing.class, pokemon);
        assertEquals(55, ((PokemonListing) pokemon).getLevel());
        assertEquals(250000, pokemon.getPrice());

        ListingData item = GTSMessageParser.parse("[GTS] Misty listed 3x Master Ball for $90000");
        assertInstanceOf(ItemListing.class, item);
        assertEquals(3, ((ItemListing) item).getQuantity());
        assertEquals(90000, item.getPrice());
    }
}
