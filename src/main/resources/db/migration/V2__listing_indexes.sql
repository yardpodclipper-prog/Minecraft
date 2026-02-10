-- Introduce composite indexes used by price sampling and active listing queries.
CREATE INDEX IF NOT EXISTS idx_listings_status_last_seen
    ON listings(status, last_seen DESC);

CREATE INDEX IF NOT EXISTS idx_listings_type_status_last_seen
    ON listings(listing_type, status, last_seen DESC);

CREATE INDEX IF NOT EXISTS idx_pokemon_species_shiny_iv
    ON pokemon_listings(species, is_shiny, iv_total);
