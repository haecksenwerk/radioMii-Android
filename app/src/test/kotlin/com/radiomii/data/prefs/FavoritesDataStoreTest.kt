package com.radiomii.data.prefs

import androidx.datastore.core.DataStoreFactory
import com.radiomii.domain.model.Station
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

// Pure JVM unit tests for FavoritesDataStore.
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesDataStoreTest {

    private lateinit var testScope: TestScope
    private lateinit var tempFile: File
    private lateinit var store: FavoritesDataStore

    private fun station(id: String, name: String = "Station $id") = Station(
        stationuuid = id,
        name = name,
        url = "https://stream.example.com/$id",
        urlResolved = "https://stream.example.com/$id",
    )

    @Before
    fun setUp() {
        testScope = TestScope(UnconfinedTestDispatcher())
        tempFile = File.createTempFile("favorites_test_", ".json").also { it.deleteOnExit() }

        val dataStore = DataStoreFactory.create(
            serializer = FavoritesSerializer,
            scope = testScope.backgroundScope,
            produceFile = { tempFile },
        )
        store = FavoritesDataStore(dataStore)
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun `addFavorite stores station and returns true`() = testScope.runTest {
        val s = station("aaa")
        assertTrue(store.addFavorite(s))
        assertEquals(listOf(s), store.favoritesFlow.first())
    }

    @Test
    fun `addFavorite with duplicate is idempotent and returns true`() = testScope.runTest {
        val s = station("bbb")
        store.addFavorite(s)
        assertTrue(store.addFavorite(s))
        assertEquals(1, store.favoritesFlow.first().size)
    }

    @Test
    fun `addFavorite preserves insertion order`() = testScope.runTest {
        val ids = listOf("z1", "a2", "m3")
        ids.forEach { store.addFavorite(station(it)) }
        assertEquals(ids, store.favoritesFlow.first().map { it.stationuuid })
    }

    @Test
    fun `removeFavorite removes station by uuid`() = testScope.runTest {
        store.addFavorite(station("c1"))
        store.addFavorite(station("c2"))
        store.removeFavorite("c1")
        val uuids = store.favoritesFlow.first().map { it.stationuuid }
        assertFalse("c1" in uuids)
        assertTrue("c2" in uuids)
    }

    @Test
    fun `removeFavorite also cleans up filterMap`() = testScope.runTest {
        store.addFavorite(station("d1"))
        store.createFilter("Rock")
        store.toggleStationFilter("d1", "Rock")
        store.removeFavorite("d1")
        assertNull(store.filterMapFlow.first()["d1"])
    }

    @Test
    fun `removeFavorite on non-existent uuid is a no-op`() = testScope.runTest {
        store.addFavorite(station("e1"))
        store.removeFavorite("non-existent")
        assertEquals(1, store.favoritesFlow.first().size)
    }

    @Test
    fun `updateFavicon replaces favicon for matching station`() = testScope.runTest {
        store.addFavorite(station("f1"))
        store.updateFavicon("f1", "https://example.com/icon.png")
        assertEquals("https://example.com/icon.png", store.favoritesFlow.first().first().favicon)
    }

    @Test
    fun `updateFavicon does not mutate other stations`() = testScope.runTest {
        store.addFavorite(station("g1"))
        store.addFavorite(station("g2"))
        store.updateFavicon("g1", "https://example.com/icon.png")
        assertEquals("", store.favoritesFlow.first().first { it.stationuuid == "g2" }.favicon)
    }

    @Test
    fun `createFilter adds a new filter and returns true`() = testScope.runTest {
        assertTrue(store.createFilter("Jazz"))
        assertTrue("Jazz" in store.filtersFlow.first())
    }

    @Test
    fun `createFilter with duplicate name returns false`() = testScope.runTest {
        store.createFilter("Pop")
        assertFalse(store.createFilter("Pop"))
        assertEquals(1, store.filtersFlow.first().count { it == "Pop" })
    }

    @Test
    fun `createFilter is case-insensitive for duplicates`() = testScope.runTest {
        store.createFilter("Rock")
        assertFalse(store.createFilter("ROCK"))
    }

    @Test
    fun `deleteFilter removes filter and cleans filterMap`() = testScope.runTest {
        store.addFavorite(station("h1"))
        store.createFilter("Metal")
        store.toggleStationFilter("h1", "Metal")
        store.deleteFilter("Metal")
        assertFalse("Metal" in store.filtersFlow.first())
        assertNull(store.filterMapFlow.first()["h1"])
    }

    @Test
    fun `reorderFilters persists new order`() = testScope.runTest {
        store.createFilter("A")
        store.createFilter("B")
        store.createFilter("C")
        store.reorderFilters(listOf("C", "A", "B"))
        assertEquals(listOf("C", "A", "B"), store.filtersFlow.first())
    }

    @Test
    fun `toggleStationFilter adds assignment on first call`() = testScope.runTest {
        store.addFavorite(station("i1"))
        store.createFilter("Indie")
        store.toggleStationFilter("i1", "Indie")
        assertTrue("Indie" in (store.filterMapFlow.first()["i1"] ?: emptyList()))
    }

    @Test
    fun `toggleStationFilter removes assignment on second call`() = testScope.runTest {
        store.addFavorite(station("j1"))
        store.createFilter("Soul")
        store.toggleStationFilter("j1", "Soul")
        store.toggleStationFilter("j1", "Soul")
        assertNull(store.filterMapFlow.first()["j1"])
    }

    @Test
    fun `clearAll with alsoFilters=true resets everything`() = testScope.runTest {
        store.addFavorite(station("k1"))
        store.createFilter("Blues")
        store.clearAll(alsoFilters = true)
        assertTrue(store.favoritesFlow.first().isEmpty())
        assertTrue(store.filtersFlow.first().isEmpty())
        assertTrue(store.filterMapFlow.first().isEmpty())
    }

    @Test
    fun `clearAll with alsoFilters=false keeps filters`() = testScope.runTest {
        store.addFavorite(station("l1"))
        store.createFilter("Country")
        store.clearAll(alsoFilters = false)
        assertTrue(store.favoritesFlow.first().isEmpty())
        assertTrue("Country" in store.filtersFlow.first())
    }

    @Test
    fun `replaceAll fully replaces stored data`() = testScope.runTest {
        store.addFavorite(station("m1"))
        val replacement = FavoritesData(
            stations = listOf(station("n1"), station("n2")),
            filters = listOf("Reggae"),
            filterMap = mapOf("n1" to listOf("Reggae")),
        )
        store.replaceAll(replacement)
        val snapshot = store.getSnapshot()
        assertEquals(replacement, snapshot)
    }
}

