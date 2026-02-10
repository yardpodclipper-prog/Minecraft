package com.yourname.gtstracker.database.models;

public class PokemonListing extends ListingData {
    private String species;
    private int level;
    private boolean shiny;
    private IVStats ivs;
    private String nature;
    private String ability;

    @Override
    public ListingType getType() {
        return ListingType.POKEMON;
    }

    @Override
    public String getDisplayName() {
        return (shiny ? "Shiny " : "") + species + " Lv" + level;
    }

    @Override
    public String getSearchKey() {
        return (species == null ? "unknown" : species.toLowerCase()) + (shiny ? "_shiny" : "");
    }

    public int getTotalIVs() {
        return ivs == null ? 0 : ivs.getTotal();
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isShiny() {
        return shiny;
    }

    public void setShiny(boolean shiny) {
        this.shiny = shiny;
    }

    public IVStats getIvs() {
        return ivs;
    }

    public void setIvs(IVStats ivs) {
        this.ivs = ivs;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }
}
