package fueltracker;

import fueltracker.db.DatabaseManager;
import fueltracker.db.StationDAO;
import fueltracker.db.StationDAOImpl;
import fueltracker.model.Station;
import fueltracker.service.FuelTrackerService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FuelTrackerTest {
    private static DatabaseManager dbManager;
    private StationDAO stationDAO;

    @BeforeClass
    public static void setUpClass() {
        dbManager = DatabaseManager.getInstance();
    }

    @AfterClass
    public static void tearDownClass() {
        // Shutdown Derby connection
        dbManager.shutdown();
    }

    @Before
    public void setUp() {
        this.stationDAO = new StationDAOImpl();
    }

    @Test
    public void test1_DatabaseConnectionAndSchema() throws Exception {
        Connection conn = dbManager.getConnection();
        assertNotNull("Database connection should not be null.", conn);
        assertFalse("Database connection should be open.", conn.isClosed());

        // Verify tables exist by querying schema metadata
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM STATIONS")) {
                assertTrue("Should query STATIONS successfully.", rs.next());
            }
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM FUEL_PRICES")) {
                assertTrue("Should query FUEL_PRICES successfully.", rs.next());
            }
        }
    }

    @Test
    public void test2_SaveAndRetrieveStations() {
        List<Station> stations = new ArrayList<>();
        Station s1 = new Station("TEST1", "Test Station 1", "123 Test St", "TestBrand", -33.8688, 151.2093);
        s1.setPrice(190.5);
        stations.add(s1);

        stationDAO.saveStations(stations, "U91");

        // Retrieve the station within a 5km radius of its exact location
        List<Station> retrieved = stationDAO.getStationsNear(-33.8688, 151.2093, 5.0, "U91");
        assertFalse("Retrieved stations list should not be empty.", retrieved.isEmpty());
        
        Station found = null;
        for (Station s : retrieved) {
            if ("TEST1".equals(s.getStationcode())) {
                found = s;
                break;
            }
        }

        assertNotNull("Saved station should be found in queries.", found);
        assertEquals("Test Station 1", found.getName());
        assertEquals("TestBrand", found.getBrand());
        assertEquals(190.5, found.getPrice(), 0.001);
    }

    @Test
    public void test3_ProximitySorting() {
        // Clear old database records if any, then insert two stations at different distances
        List<Station> stations = new ArrayList<>();
        // CBD center: -33.8688, 151.2093
        Station closeStation = new Station("CLOSE", "Close Station", "1 Main St", "BP", -33.8700, 151.2100); // Very close
        closeStation.setPrice(185.0);
        Station farStation = new Station("FAR", "Far Station", "100 Border Rd", "Caltex", -33.8000, 151.1000);   // Farther (~12km)
        farStation.setPrice(189.0);

        stations.add(farStation);
        stations.add(closeStation);

        stationDAO.saveStations(stations, "U91");

        // Retrieve nearby stations within 20 km
        List<Station> retrieved = stationDAO.getStationsNear(-33.8688, 151.2093, 20.0, "U91");
        assertTrue("Should retrieve at least our two test stations.", retrieved.size() >= 2);

        // Verify proximity sorting order (closest must be first)
        int indexClose = -1;
        int indexFar = -1;
        for (int i = 0; i < retrieved.size(); i++) {
            if ("CLOSE".equals(retrieved.get(i).getStationcode())) {
                indexClose = i;
            } else if ("FAR".equals(retrieved.get(i).getStationcode())) {
                indexFar = i;
            }
        }

        assertTrue("Both test stations should be retrieved.", indexClose != -1 && indexFar != -1);
        assertTrue("Close station should be ordered before far station.", indexClose < indexFar);
    }

    @Test
    public void test4_FuelPriceOffsetCalculation() {
        FuelTrackerService service = new FuelTrackerService();

        // Perform a search in offline mode (by targeting a default coords with no API configuration)
        // Verify fallback loading and fuel offsets
        List<Station> u91Stations = service.searchStations(-33.8688, 151.2093, 20.0, "U91");
        List<Station> p95Stations = service.searchStations(-33.8688, 151.2093, 20.0, "P95");

        assertFalse(u91Stations.isEmpty());
        assertFalse(p95Stations.isEmpty());

        // Find the same station in both lists to compare price offset
        Station s91 = u91Stations.get(0);
        Station s95 = null;
        for (Station s : p95Stations) {
            if (s.getStationcode().equals(s91.getStationcode())) {
                s95 = s;
                break;
            }
        }

        assertNotNull(s95);
        // Premium 95 should have an offset of +15.0 cents added to the base price
        assertEquals(s91.getPrice() + 15.0, s95.getPrice(), 0.001);
    }

    @Test
    public void test5_OfflineSearchFallback() {
        FuelTrackerService service = new FuelTrackerService();

        // Querying chatswood coordinates (-33.8012, 151.1789)
        List<Station> results = service.searchStations(-33.8012, 151.1789, 5.0, "U91");

        assertNotNull(results);
        assertFalse("Should load fallback stations successfully.", results.isEmpty());
        assertTrue("Service should flag that the search was executed offline.", service.isLastSearchWasOffline());

        // The closest station to Chatswood coordinates should be 'Chatswood 7-Eleven'
        Station closest = results.get(0);
        assertEquals("7-Eleven Chatswood", closest.getName());
    }
}
