package chat;

import config.ConfigManager;
import config.ConfigModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GTSMessageParserTest {

    @Test
    void parsesPokemonListingWithCommaPrice() {
        GTSMessageParser parser = new GTSMessageParser(ConfigManager.getConfig());

        String message = "[Star_Trainer] listed Shiny Charizard for $1,250,000";
        Optional<GTSMessageParser.ListingParseResult> result = parser.parse(message);

        assertTrue(result.isPresent());
        assertEquals(GTSMessageParser.ListingType.POKEMON, result.get().type());
        assertEquals("Star_Trainer", result.get().seller());
        assertEquals("Shiny Charizard", result.get().name());
        assertEquals(1250000L, result.get().price());
    }

    @Test
    void parsesItemListingWithApostrophesAndSpaces() {
        GTSMessageParser parser = new GTSMessageParser(ConfigManager.getConfig());

        String message = "seller: Oak's_Helper item: King's Rock price: 22,500";
        Optional<GTSMessageParser.ListingParseResult> result = parser.parse(message);

        assertTrue(result.isPresent());
        assertEquals(GTSMessageParser.ListingType.ITEM, result.get().type());
        assertEquals("Oak's_Helper", result.get().seller());
        assertEquals("King's Rock", result.get().name());
        assertEquals(22500L, result.get().price());
    }

    @Test
    void supportsMultipleRegexVariantsPerListingType() {
        ConfigModel configModel = new ConfigModel();
        configModel.setPokemonParserPatterns(List.of(
                "(?i)pokemon=(?<species>[A-Za-z0-9_ ']+) by (?<seller>[A-Za-z0-9_ ']+) price=(?<price>[0-9,]+)",
                "(?i)unused-variant"
        ));
        configModel.setItemParserPatterns(List.of(
                "(?i)item=(?<item>[A-Za-z0-9_ ']+) by (?<seller>[A-Za-z0-9_ ']+) price=(?<price>[0-9,]+)",
                "(?i)unused-item-variant"
        ));

        GTSMessageParser parser = new GTSMessageParser(configModel);
        Optional<GTSMessageParser.ListingParseResult> result = parser.parse("pokemon=Mr Mime by Space_User price=99,999");

        assertTrue(result.isPresent());
        assertEquals("Space_User", result.get().seller());
        assertEquals("Mr Mime", result.get().name());
        assertEquals(99999L, result.get().price());
    }

    @Test
    void fallsBackToTokenHeuristicsWhenRegexDoesNotMatch() {
        ConfigModel model = new ConfigModel();
        model.setPokemonParserPatterns(List.of("(?i)^DOES_NOT_MATCH$"));
        model.setItemParserPatterns(List.of("(?i)^DOES_NOT_MATCH$"));

        GTSMessageParser parser = new GTSMessageParser(model);
        String message = "Nova_Player is selling Focus Sash for $12,000";
        Optional<GTSMessageParser.ListingParseResult> result = parser.parse(message);

        assertTrue(result.isPresent());
        assertEquals(GTSMessageParser.ListingType.ITEM, result.get().type());
        assertEquals("Nova_Player", result.get().seller());
        assertEquals("Focus Sash", result.get().name());
        assertEquals(12000L, result.get().price());
    }

    @Test
    void normalizesPriceContainingSeparators() {
        assertEquals(1250000L, GTSMessageParser.normalizePrice("1,250,000"));
        assertEquals(1250000L, GTSMessageParser.normalizePrice("1 250 000"));
        assertEquals(1250000L, GTSMessageParser.normalizePrice("$1_250_000"));
    }
}
