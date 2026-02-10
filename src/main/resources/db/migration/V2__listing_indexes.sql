-- Added to support DAO access patterns and prevent table scans in price-sample queries.
CREATE INDEX IF NOT EXISTS idx_listings_status_created_at
    ON listings(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_listings_updated_at
    ON listings(updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_listings_pokemon_samples
    ON listings(listing_type, pokemon_species, is_shiny, iv_total, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_listings_item_samples
    ON listings(listing_type, item_name, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_listings_lifecycle
    ON listings(status, last_seen_at, expires_at);
