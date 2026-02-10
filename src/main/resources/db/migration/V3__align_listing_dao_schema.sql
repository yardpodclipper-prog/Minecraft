-- Final alignment with DAO schema expectations and cleanup of obsolete indexes.
DROP INDEX IF EXISTS idx_listings_status;
DROP INDEX IF EXISTS idx_listings_type;
DROP INDEX IF EXISTS idx_listings_last_seen;
DROP INDEX IF EXISTS idx_pokemon_species;

CREATE INDEX IF NOT EXISTS idx_item_name
    ON item_listings(item_name);
