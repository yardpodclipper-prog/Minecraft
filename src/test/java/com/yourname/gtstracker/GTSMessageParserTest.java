package com.yourname.gtstracker;

import com.yourname.gtstracker.chat.GTSMessageParser;
import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.database.models.PokemonListing;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GTSMessageParserTest {

    @Test
    void parsesPokemonMessageIntoPokemonListing() {
        String message = "[GTS] Ash listed Shiny Gengar Lv55 for $250000";

        ListingData parsed = GTSMessageParser.parse(message);

        assertInstanceOf(PokemonListing.class, parsed);
        PokemonListing pokemon = (PokemonListing) parsed;
        assertEquals("Ash", pokemon.getSeller());
        assertEquals("Gengar", pokemon.getSpecies());
        assertTrue(pokemon.isShiny());
        assertEquals(55, pokemon.getLevel());
        assertEquals(250000, pokemon.getPrice());
    }

    @Test
    void parsesItemMessageIntoItemListing() {
        String message = "[GTS] Brock listed 3x Master Ball for $90000";

        ListingData parsed = GTSMessageParser.parse(message);

        assertInstanceOf(ItemListing.class, parsed);
        ItemListing item = (ItemListing) parsed;
        assertEquals("Brock", item.getSeller());
        assertEquals("Master Ball", item.getItemName());
        assertEquals(3, item.getQuantity());
        assertEquals(90000, item.getPrice());
    }

    @Test
    void rejectsNonGtsMessage() {
        assertFalse(GTSMessageParser.isGTSMessage("normal chat"));
        assertNull(GTSMessageParser.parse("normal chat"));
    }
}
