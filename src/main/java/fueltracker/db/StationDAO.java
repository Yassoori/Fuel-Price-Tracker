package fueltracker.db;

import fueltracker.model.Station;
import java.util.List;

public interface StationDAO {
    /**
     * Saves a list of stations and their prices for a specific fuel type.
     * Inserts new stations or updates existing ones.
     */
    void saveStations(List<Station> stations, String fuelType);

    /**
     * Retrieves all stations within a given radius (km) from a reference coordinate,
     * containing prices for the specified fuel type, sorted by proximity (distance).
     */
    List<Station> getStationsNear(double latitude, double longitude, double radiusKm, String fuelType);
}
