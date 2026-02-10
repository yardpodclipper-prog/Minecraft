package config;

import java.util.ArrayList;
import java.util.List;

public class ConfigModel {
    private List<String> pokemonParserPatterns = new ArrayList<>();
    private List<String> itemParserPatterns = new ArrayList<>();

    public List<String> getPokemonParserPatterns() {
        return pokemonParserPatterns;
    }

    public void setPokemonParserPatterns(List<String> pokemonParserPatterns) {
        this.pokemonParserPatterns = pokemonParserPatterns;
    }

    public List<String> getItemParserPatterns() {
        return itemParserPatterns;
    }

    public void setItemParserPatterns(List<String> itemParserPatterns) {
        this.itemParserPatterns = itemParserPatterns;
    }
}
