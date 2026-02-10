-- Align DAO query indexes with the canonical normalized schema:
-- listings + pokemon_listings + item_listings.
DROP INDEX IF EXISTS idx_listings_status_created_at;
DROP INDEX IF EXISTS idx_listings_updated_at;
DROP INDEX IF EXISTS idx_listings_pokemon_samples;
DROP INDEX IF EXISTS idx_listings_item_samples;
DROP INDEX IF EXISTS idx_listings_lifecycle;

CREATE INDEX IF NOT EXISTS idx_listings_status_last_seen
    ON listings(status, last_seen DESC);

CREATE INDEX IF NOT EXISTS idx_listings_type_status_last_seen
    ON listings(listing_type, status, last_seen DESC);

CREATE INDEX IF NOT EXISTS idx_pokemon_species_shiny_iv
    ON pokemon_listings(species, is_shiny, iv_total);

CREATE INDEX IF NOT EXISTS idx_item_name
    ON item_listings(item_name);
