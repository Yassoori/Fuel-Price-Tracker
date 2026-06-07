package fueltracker.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fueltracker.api.NSWApiClient;
import fueltracker.db.StationDAO;
import fueltracker.db.StationDAOImpl;
import fueltracker.model.Station;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FuelTrackerService {
    private final NSWApiClient apiClient;
    private final StationDAO stationDAO;
    private final Gson gson;
    private boolean lastSearchWasOffline;
    private boolean forceOffline = false;

    public FuelTrackerService() {
        this.apiClient = new NSWApiClient();
        this.stationDAO = new StationDAOImpl();
        this.gson = new Gson();
        this.lastSearchWasOffline = false;
    }

    public void setForceOffline(boolean forceOffline) {
        this.forceOffline = forceOffline;
    }

    /**
     * Executes the search by trying the live API first. Falls back to local resource files
     * if the API is offline or unconfigured. Caches all data to Derby and returns proximity-sorted stations.
     */
    public List<Station> searchStations(double lat, double lon, double radiusKm, String fuelType) {
        List<Station> stations = new ArrayList<>();
        lastSearchWasOffline = false;

        if (apiClient.isConfigured() && !forceOffline) {
            try {
                System.out.println("Querying NSW Fuel Check API (Online Mode) for nearby stations...");
                stations = apiClient.fetchNearbyStations(lat, lon, radiusKm, fuelType);
                System.out.println("Online Mode successful: retrieved " + stations.size() + " stations.");
            } catch (Exception e) {
                System.err.println("API error encountered. Switching to Offline Fallback Mode. Reason: " + e.getMessage());
                stations = loadFallbackStations(fuelType);
                lastSearchWasOffline = true;
            }
        } else {
            System.out.println("API Client is unconfigured. Running in Offline Fallback Mode...");
            stations = loadFallbackStations(fuelType);
            lastSearchWasOffline = true;
        }

        // Cache the retrieved stations in the Derby DB
        if (!stations.isEmpty()) {
            stationDAO.saveStations(stations, fuelType);
        }

        // Retrieve sorted stations from the database to ensure separation of concerns
        return stationDAO.getStationsNear(lat, lon, radiusKm, fuelType);
    }

    /**
     * Loads stations from the pre-packaged sydney_stations_fallback.json file
     * and adjusts prices depending on the selected fuel type.
     */
    private List<Station> loadFallbackStations(String fuelType) {
        List<Station> list = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/sydney_stations_fallback.json")) {
            if (is == null) {
                System.err.println("Fallback JSON file not found in classpath.");
                return list;
            }

            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root.has("stations")) {
                    JsonArray stationsArr = root.getAsJsonArray("stations");
                    for (JsonElement elem : stationsArr) {
                        JsonObject sJson = elem.getAsJsonObject();
                        String code = sJson.get("stationcode").getAsString();
                        String name = sJson.get("name").getAsString();
                        String address = sJson.get("address").getAsString();
                        String brand = sJson.get("brand").getAsString();
                        double latitude = sJson.get("latitude").getAsDouble();
                        double longitude = sJson.get("longitude").getAsDouble();
                        double basePrice = sJson.get("price").getAsDouble(); // default 91 price

                        // Apply dynamic offset for other fuels to make offline testing realistic
                        double price = basePrice;
                        if ("P95".equalsIgnoreCase(fuelType)) {
                            price += 15.0; // 95 Premium offset
                        } else if ("DL".equalsIgnoreCase(fuelType)) {
                            price += 10.0; // Diesel offset
                        } else if ("P98".equalsIgnoreCase(fuelType)) {
                            price += 25.0; // 98 Premium offset
                        } else if ("E10".equalsIgnoreCase(fuelType)) {
                            price -= 4.0;  // E10 discount
                        }

                        Station s = new Station(code, name, address, brand, latitude, longitude);
                        s.setPrice(price);
                        list.add(s);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading fallback JSON: " + e.getMessage());
        }
        return list;
    }

    public boolean isLastSearchWasOffline() {
        return lastSearchWasOffline;
    }
}
