package fueltracker.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fueltracker.model.Station;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class NSWApiClient {
    private static final String CONFIG_FILE = "api_config.properties";
    private static final String BASE_URL = "https://api.onegov.nsw.gov.au";
    
    private final HttpClient httpClient;
    private final Gson gson;
    private String apiKey;
    private String apiSecret;
    private String cachedToken;
    private long tokenExpiryTime;

    public NSWApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        loadConfig();
    }

    private void loadConfig() {
        Properties props = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                this.apiKey = props.getProperty("nsw.api.key");
                this.apiSecret = props.getProperty("nsw.api.secret");
            } catch (IOException e) {
                System.err.println("Failed to read " + CONFIG_FILE + ": " + e.getMessage());
            }
        }
    }

    /**
     * Checks if the API has been fully configured with an API Key.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * Obtains an OAuth Access Token from OneGov NSW Client Credentials endpoint.
     */
    private synchronized String getAccessToken() throws Exception {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return cachedToken;
        }

        if (!isConfigured() || apiSecret == null || apiSecret.trim().isEmpty()) {
            throw new IllegalStateException("API Key or Secret is not configured in " + CONFIG_FILE);
        }

        String authString = apiKey + ":" + apiSecret;
        String basicAuth = Base64.getEncoder().encodeToString(authString.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/oauth/client_credential/accesstoken?grant_type=client_credentials"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch token: HTTP " + response.statusCode() + " - " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        this.cachedToken = json.get("access_token").getAsString();
        // Token typically expires in 3600 seconds, set expiry with buffer (e.g. 55 mins)
        long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600;
        this.tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 300) * 1000;

        return cachedToken;
    }

    /**
     * Queries the POST /FuelPriceCheck/v2/fuel/prices/nearby endpoint.
     */
    public List<Station> fetchNearbyStations(double lat, double lon, double radiusKm, String fuelType) throws Exception {
        String token = getAccessToken();

        // OneGov API headers requirement
        String transactionId = UUID.randomUUID().toString();
        String timestamp = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a").format(new Date());

        // Prepare request body
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("fueltype", fuelType);
        bodyMap.put("latitude", String.valueOf(lat));
        bodyMap.put("longitude", String.valueOf(lon));
        bodyMap.put("radius", String.valueOf(radiusKm));
        String requestBody = gson.toJson(bodyMap);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/FuelPriceCheck/v2/fuel/prices/nearby"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("Apikey", apiKey)
                .header("Transactionid", transactionId)
                .header("Requesttimestamp", timestamp)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API Error: HTTP " + response.statusCode() + " - " + response.body());
        }

        try (java.io.FileWriter writer = new java.io.FileWriter("api_response_debug.json")) {
            writer.write(response.body());
        } catch (Exception e) {
            System.err.println("Failed to write api_response_debug.json: " + e.getMessage());
        }

        return parseApiResponse(response.body());
    }

    private List<Station> parseApiResponse(String responseBody) {
        List<Station> list = new ArrayList<>();
        JsonObject root = gson.fromJson(responseBody, JsonObject.class);

        // Map station code to price from the 'prices' array
        Map<String, Double> priceMap = new HashMap<>();
        if (root.has("prices")) {
            JsonArray pricesArr = root.getAsJsonArray("prices");
            for (JsonElement elem : pricesArr) {
                JsonObject pJson = elem.getAsJsonObject();
                String sCode = "";
                if (pJson.has("stationcode")) {
                    sCode = String.valueOf(pJson.get("stationcode").getAsLong());
                } else if (pJson.has("code")) {
                    sCode = String.valueOf(pJson.get("code").getAsLong());
                }
                
                double price = pJson.has("price") ? pJson.get("price").getAsDouble() : 0.0;
                if (!sCode.isEmpty()) {
                    priceMap.put(sCode, price);
                }
            }
        }

        if (root.has("stations")) {
            JsonArray stationsArr = root.getAsJsonArray("stations");
            for (JsonElement elem : stationsArr) {
                JsonObject sJson = elem.getAsJsonObject();
                String code = "";
                if (sJson.has("code")) {
                    code = String.valueOf(sJson.get("code").getAsLong());
                } else if (sJson.has("stationcode")) {
                    code = String.valueOf(sJson.get("stationcode").getAsLong());
                }
                
                if (code.isEmpty()) {
                    continue;
                }

                String name = sJson.has("name") ? sJson.get("name").getAsString() : "Unknown Station";
                String address = sJson.has("address") ? sJson.get("address").getAsString() : "No Address Provided";
                String brand = sJson.has("brand") ? sJson.get("brand").getAsString() : "Independent";
                
                double latitude = 0.0;
                double longitude = 0.0;
                if (sJson.has("location") && sJson.get("location").isJsonObject()) {
                    JsonObject loc = sJson.getAsJsonObject("location");
                    latitude = loc.has("latitude") ? loc.get("latitude").getAsDouble() : 0.0;
                    longitude = loc.has("longitude") ? loc.get("longitude").getAsDouble() : 0.0;
                } else {
                    latitude = sJson.has("latitude") ? sJson.get("latitude").getAsDouble() : 
                                      (sJson.has("lat") ? sJson.get("lat").getAsDouble() : 0.0);
                    longitude = sJson.has("longitude") ? sJson.get("longitude").getAsDouble() : 
                                       (sJson.has("long") ? sJson.get("long").getAsDouble() : 0.0);
                }

                double price = priceMap.getOrDefault(code, 0.0);
                if (price == 0.0 && sJson.has("price")) {
                    price = sJson.get("price").getAsDouble();
                }

                // Create Station domain model
                Station station = new Station(code, name, address, brand, latitude, longitude);
                station.setPrice(price);
                list.add(station);
            }
        }
        return list;
    }
}
