package fueltracker.model;

public class Station {
    private String stationcode;
    private String name;
    private String address;
    private String brand;
    private double latitude;
    private double longitude;
    
    // Runtime properties (calculated relative to a search context)
    private double price;       // Price of the selected fuel type
    private double distance;    // Calculated distance in km from search center

    public Station() {}

    public Station(String stationcode, String name, String address, String brand, double latitude, double longitude) {
        this.stationcode = stationcode;
        this.name = name;
        this.address = address;
        this.brand = brand;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getStationcode() {
        return stationcode;
    }

    public void setStationcode(String stationcode) {
        this.stationcode = stationcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return String.format("%s - %s ($%.2f, %.2f km)", brand, name, price, distance);
    }
}
