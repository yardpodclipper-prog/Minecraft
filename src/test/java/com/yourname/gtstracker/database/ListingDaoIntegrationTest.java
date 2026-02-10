package com.yourname.gtstracker.database;

import com.yourname.gtstracker.database.models.DataSource;
import com.yourname.gtstracker.database.models.IVStats;
import com.yourname.gtstracker.database.models.ItemListing;
import com.yourname.gtstracker.database.models.ListingStatus;
import com.yourname.gtstracker.database.models.PokemonListing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ListingDaoIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void activeListingsQueryUsesCanonicalSchema() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("listing-dao-active.sqlite").toAbsolutePath();
        prepareSchema(jdbcUrl);

        com.yourname.gtstracker.database.DatabaseManager writeManager = new com.yourname.gtstracker.database.DatabaseManager();
        Connection writeConnection = DriverManager.getConnection(jdbcUrl);
        injectConnection(writeManager, writeConnection);

        PokemonListing pokemon = new PokemonListing();
        pokemon.setId("poke-1");
        pokemon.setSeller("Red");
        pokemon.setPrice(125000);
        pokemon.setFirstSeen(1_700_000_000_000L);
        pokemon.setLastSeen(1_700_000_100_000L);
        pokemon.setStatus(ListingStatus.ACTIVE);
        pokemon.setSourceFirst(DataSource.CHAT);
        pokemon.setSourceLast(DataSource.SCREEN);
        pokemon.setSpecies("Garchomp");
        pokemon.setLevel(70);
        pokemon.setShiny(true);
        pokemon.setIvs(new IVStats(31, 31, 31, 31, 31, 31));
        pokemon.setNature("Jolly");
        pokemon.setAbility("Rough Skin");
        writeManager.upsertListing(pokemon);

        SQLiteDataSource readDataSource = new SQLiteDataSource();
        readDataSource.setUrl(jdbcUrl);

        ListingDAO listingDAO = new ListingDAO(readDataSource);
        listingDAO.ensureIndexes();

        List<ListingDAO.ListingRow> rows = listingDAO.getActiveListings(10, 0);
        assertEquals(1, rows.size());
        assertEquals("poke-1", rows.get(0).id);
        assertEquals("POKEMON", rows.get(0).listingType);
        assertEquals("Garchomp", rows.get(0).pokemonSpecies);
        assertEquals(new BigDecimal("125000"), rows.get(0).price);
        assertNotNull(rows.get(0).firstSeen);
        assertNotNull(rows.get(0).lastSeen);

        writeConnection.close();
    }

    @Test
    void pokemonAndItemPriceSampleQueriesUseSubtypeTables() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDir.resolve("listing-dao-samples.sqlite").toAbsolutePath();
        prepareSchema(jdbcUrl);

        com.yourname.gtstracker.database.DatabaseManager writeManager = new com.yourname.gtstracker.database.DatabaseManager();
        Connection writeConnection = DriverManager.getConnection(jdbcUrl);
        injectConnection(writeManager, writeConnection);

        PokemonListing pokemon = new PokemonListing();
        pokemon.setId("poke-sample");
        pokemon.setSeller("Blue");
        pokemon.setPrice(99000);
        pokemon.setFirstSeen(1_700_010_000_000L);
        pokemon.setLastSeen(1_700_010_050_000L);
        pokemon.setStatus(ListingStatus.ACTIVE);
        pokemon.setSourceFirst(DataSource.CHAT);
        pokemon.setSourceLast(DataSource.CHAT);
        pokemon.setSpecies("Metagross");
        pokemon.setLevel(100);
        pokemon.setShiny(true);
        pokemon.setIvs(new IVStats(31, 31, 31, 31, 31, 30));
        writeManager.upsertListing(pokemon);

        ItemListing item = new ItemListing();
        item.setId("item-sample");
        item.setSeller("Green");
        item.setPrice(15000);
        item.setFirstSeen(1_700_010_100_000L);
        item.setLastSeen(1_700_010_200_000L);
        item.setStatus(ListingStatus.ACTIVE);
        item.setSourceFirst(DataSource.SCREEN);
        item.setSourceLast(DataSource.SCREEN);
        item.setItemName("Master Ball");
        item.setQuantity(2);
        writeManager.upsertListing(item);

        SQLiteDataSource readDataSource = new SQLiteDataSource();
        readDataSource.setUrl(jdbcUrl);

        PokemonListingDAO pokemonListingDAO = new PokemonListingDAO(readDataSource);
        List<PokemonListingDAO.PriceSample> pokemonSamples = pokemonListingDAO.getPokemonPriceSamples(
            "Metagross",
            true,
            180,
            Instant.ofEpochMilli(1_700_009_000_000L)
        );
        assertEquals(1, pokemonSamples.size());
        assertEquals(new BigDecimal("99000"), pokemonSamples.get(0).price);

        ItemListingDAO itemListingDAO = new ItemListingDAO(readDataSource);
        List<ItemListingDAO.PriceSample> itemSamples = itemListingDAO.getItemPriceSamples(
            "Master Ball",
            Instant.ofEpochMilli(1_700_009_000_000L)
        );
        assertEquals(1, itemSamples.size());
        assertEquals(new BigDecimal("15000"), itemSamples.get(0).price);

        writeConnection.close();
    }

    private static void prepareSchema(String jdbcUrl) throws Exception {
        com.yourname.gtstracker.database.DatabaseManager schemaManager = new com.yourname.gtstracker.database.DatabaseManager(jdbcUrl);
        schemaManager.initialize();
    }

    private static void injectConnection(com.yourname.gtstracker.database.DatabaseManager manager, Connection connection) throws Exception {
        var field = com.yourname.gtstracker.database.DatabaseManager.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(manager, connection);
    }
}
