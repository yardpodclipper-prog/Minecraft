package chat;

import config.ConfigModel;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GTSMessageParser {
    private final List<Pattern> pokemonPatterns;
    private final List<Pattern> itemPatterns;

    public GTSMessageParser(ConfigModel configModel) {
        this.pokemonPatterns = compile(configModel.getPokemonParserPatterns());
        this.itemPatterns = compile(configModel.getItemParserPatterns());
    }

    public Optional<ListingParseResult> parse(String message) {
        Optional<ListingParseResult> pokemonResult = matchPatterns(message, pokemonPatterns, ListingType.POKEMON, "species");
        if (pokemonResult.isPresent()) {
            return pokemonResult;
        }

        Optional<ListingParseResult> itemResult = matchPatterns(message, itemPatterns, ListingType.ITEM, "item");
        if (itemResult.isPresent()) {
            return itemResult;
        }

        return fallbackTokenParse(message);
    }

    private Optional<ListingParseResult> matchPatterns(String message,
                                                       List<Pattern> patterns,
                                                       ListingType listingType,
                                                       String objectGroup) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String seller = safeGroup(matcher, "seller");
                String object = safeGroup(matcher, objectGroup);
                long price = normalizePrice(safeGroup(matcher, "price"));
                if (seller != null && object != null && price >= 0) {
                    return Optional.of(new ListingParseResult(listingType, seller.trim(), object.trim(), price));
                }
            }
        }

        return Optional.empty();
    }

    private Optional<ListingParseResult> fallbackTokenParse(String message) {
        String compact = message.replaceAll("\s+", " ").trim();
        String lower = compact.toLowerCase();
        int forIndex = lower.lastIndexOf(" for ");
        if (forIndex < 0) {
            return Optional.empty();
        }

        String left = compact.substring(0, forIndex).trim();
        String right = compact.substring(forIndex + 5).trim();

        String seller = null;
        String name = null;
        String[] verbs = {" is selling ", " listed ", " selling "};
        String leftLower = left.toLowerCase();
        for (String verb : verbs) {
            int verbIndex = leftLower.indexOf(verb);
            if (verbIndex >= 0) {
                seller = left.substring(0, verbIndex).trim();
                name = left.substring(verbIndex + verb.length()).trim();
                break;
            }
        }

        if (seller == null || seller.isBlank() || name == null || name.isBlank()) {
            return Optional.empty();
        }

        long price = normalizePrice(right);
        if (price < 0) {
            return Optional.empty();
        }

        ListingType listingType = looksLikePokemon(name) ? ListingType.POKEMON : ListingType.ITEM;
        return Optional.of(new ListingParseResult(listingType, seller, name, price));
    }

    static long normalizePrice(String priceText) {
        if (priceText == null) {
            return -1;
        }

        String normalized = priceText.replaceAll("[^0-9]", "");
        if (normalized.isEmpty()) {
            return -1;
        }

        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean looksLikePokemon(String value) {
        String lower = value.toLowerCase();
        String[] commonItemTokens = {"ball", "potion", "orb", "sash", "rock", "band", "stone", "berry", "capsule"};
        for (String token : commonItemTokens) {
            if (lower.contains(token)) {
                return false;
            }
        }

        return lower.contains("shiny") || lower.contains("lv") || lower.contains("alolan") || lower.contains("galarian");
    }

    private static String safeGroup(Matcher matcher, String groupName) {
        try {
            return matcher.group(groupName);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static List<Pattern> compile(List<String> patterns) {
        return patterns.stream().map(Pattern::compile).toList();
    }

    public enum ListingType {
        POKEMON,
        ITEM
    }

    public record ListingParseResult(ListingType type, String seller, String name, long price) {
    }
}
