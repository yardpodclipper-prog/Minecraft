CREATE TABLE IF NOT EXISTS listings (
    id TEXT PRIMARY KEY,
    listing_type TEXT CHECK(listing_type IN ('POKEMON', 'ITEM')) NOT NULL,
    seller TEXT NOT NULL,
    price INTEGER NOT NULL,
    first_seen INTEGER NOT NULL,
    last_seen INTEGER NOT NULL,
    status TEXT CHECK(status IN ('active', 'sold', 'expired', 'unknown')) DEFAULT 'active',
    source_first TEXT CHECK(source_first IN ('chat', 'screen')) NOT NULL,
    source_last TEXT CHECK(source_last IN ('chat', 'screen')) NOT NULL
);

CREATE TABLE IF NOT EXISTS pokemon_listings (
    listing_id TEXT PRIMARY KEY,
    species TEXT NOT NULL,
    level INTEGER,
    is_shiny BOOLEAN DEFAULT 0,
    iv_hp INTEGER,
    iv_atk INTEGER,
    iv_def INTEGER,
    iv_spatk INTEGER,
    iv_spdef INTEGER,
    iv_speed INTEGER,
    iv_total INTEGER GENERATED ALWAYS AS (
        COALESCE(iv_hp, 0) + COALESCE(iv_atk, 0) + COALESCE(iv_def, 0) +
        COALESCE(iv_spatk, 0) + COALESCE(iv_spdef, 0) + COALESCE(iv_speed, 0)
    ) STORED,
    nature TEXT,
    ability TEXT,
    gender TEXT,
    pokeball TEXT,
    extra_data TEXT,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS item_listings (
    listing_id TEXT PRIMARY KEY,
    item_name TEXT NOT NULL,
    quantity INTEGER DEFAULT 1,
    extra_data TEXT,
    FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_listings_status ON listings(status);
CREATE INDEX IF NOT EXISTS idx_listings_type ON listings(listing_type);
CREATE INDEX IF NOT EXISTS idx_listings_last_seen ON listings(last_seen);
CREATE INDEX IF NOT EXISTS idx_pokemon_species ON pokemon_listings(species);
CREATE INDEX IF NOT EXISTS idx_item_name ON item_listings(item_name);
