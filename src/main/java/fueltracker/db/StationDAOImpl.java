package fueltracker.db;

import fueltracker.model.Station;
import java.sql.*;
import java.util.ArrayList;

import java.util.Comparator;
import java.util.List;

public class StationDAOImpl implements StationDAO {
    private final DatabaseManager dbManager;

    public StationDAOImpl() {
        this.dbManager = DatabaseManager.getInstance();
    }

    @Override
    public void saveStations(List<Station> stations, String fuelType) {
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            System.err.println("Cannot save stations: database connection is null.");
            return;
        }

        String insertStationSql = "INSERT INTO STATIONS (code, name, address, brand, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)";
        String updateStationSql = "UPDATE STATIONS SET name = ?, address = ?, brand = ?, latitude = ?, longitude = ? WHERE code = ?";
        String insertPriceSql = "INSERT INTO FUEL_PRICES (stationcode, fueltype, price) VALUES (?, ?, ?)";
        String updatePriceSql = "UPDATE FUEL_PRICES SET price = ? WHERE stationcode = ? AND fueltype = ?";

        try {
            // Disable auto-commit for transactional batch operations
            conn.setAutoCommit(false);

            try (PreparedStatement insertStation = conn.prepareStatement(insertStationSql);
                 PreparedStatement updateStation = conn.prepareStatement(updateStationSql);
                 PreparedStatement insertPrice = conn.prepareStatement(insertPriceSql);
                 PreparedStatement updatePrice = conn.prepareStatement(updatePriceSql)) {

                for (Station s : stations) {
                    // 1. Try to insert station, update if key exists
                    try {
                        insertStation.setString(1, s.getStationcode());
                        insertStation.setString(2, s.getName());
                        insertStation.setString(3, s.getAddress());
                        insertStation.setString(4, s.getBrand());
                        insertStation.setDouble(5, s.getLatitude());
                        insertStation.setDouble(6, s.getLongitude());
                        insertStation.executeUpdate();
                    } catch (SQLException e) {
                        if ("23505".equals(e.getSQLState())) { // Duplicate key in Derby
                            updateStation.setString(1, s.getName());
                            updateStation.setString(2, s.getAddress());
                            updateStation.setString(3, s.getBrand());
                            updateStation.setDouble(4, s.getLatitude());
                            updateStation.setDouble(5, s.getLongitude());
                            updateStation.setString(6, s.getStationcode());
                            updateStation.executeUpdate();
                        } else {
                            throw e;
                        }
                    }

                    // 2. Try to insert fuel price, update if key exists
                    try {
                        insertPrice.setString(1, s.getStationcode());
                        insertPrice.setString(2, fuelType);
                        insertPrice.setDouble(3, s.getPrice());
                        insertPrice.executeUpdate();
                    } catch (SQLException e) {
                        if ("23505".equals(e.getSQLState())) { // Duplicate key
                            updatePrice.setDouble(1, s.getPrice());
                            updatePrice.setString(2, s.getStationcode());
                            updatePrice.setString(3, fuelType);
                            updatePrice.executeUpdate();
                        } else {
                            throw e;
                        }
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error saving stations/prices in database: " + e.getMessage());
        }
    }

    @Override
    public List<Station> getStationsNear(double searchLat, double searchLon, double radiusKm, String fuelType) {
        List<Station> nearStations = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            return nearStations;
        }

        String selectSql = "SELECT s.code, s.name, s.address, s.brand, s.latitude, s.longitude, p.price " +
                           "FROM STATIONS s " +
                           "JOIN FUEL_PRICES p ON s.code = p.stationcode " +
                           "WHERE p.fueltype = ?";

        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, fuelType);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    String name = rs.getString("name");
                    String addr = rs.getString("address");
                    String brand = rs.getString("brand");
                    double lat = rs.getDouble("latitude");
                    double lon = rs.getDouble("longitude");
                    double price = rs.getDouble("price");

                    double distance = calculateDistance(searchLat, searchLon, lat, lon);

                    if (distance <= radiusKm) {
                        Station station = new Station(code, name, addr, brand, lat, lon);
                        station.setPrice(price);
                        station.setDistance(distance);
                        nearStations.add(station);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying database for nearby stations: " + e.getMessage());
        }

        // Sort by distance (proximity) ascending
        nearStations.sort(Comparator.comparingDouble(Station::getDistance));

        return nearStations;
    }

    /**
     * Calculates the distance in kilometers between two latitude/longitude points
     * using the Haversine formula.
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Radius of the Earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
