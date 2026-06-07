package fueltracker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:derby:FuelTrackerDB;create=true";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            // Register Derby Embedded Driver (good practice for NetBeans environments)
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            this.connection = DriverManager.getConnection(DB_URL);
            initializeSchema();
        } catch (ClassNotFoundException e) {
            System.err.println("Derby driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            System.err.println("Failed to re-establish connection: " + e.getMessage());
        }
        return connection;
    }

    private void initializeSchema() {
        try (Statement stmt = connection.createStatement()) {
            // Create STATIONS table
            try {
                stmt.execute(
                    "CREATE TABLE STATIONS (" +
                    "code VARCHAR(50) PRIMARY KEY, " +
                    "name VARCHAR(150), " +
                    "address VARCHAR(250), " +
                    "brand VARCHAR(100), " +
                    "latitude DOUBLE, " +
                    "longitude DOUBLE" +
                    ")"
                );
                System.out.println("Created table STATIONS.");
            } catch (SQLException e) {
                // Table already exists (SQLState X0Y32 in Derby)
                if (!e.getSQLState().equals("X0Y32")) {
                    throw e;
                }
            }

            // Create FUEL_PRICES table
            try {
                stmt.execute(
                    "CREATE TABLE FUEL_PRICES (" +
                    "stationcode VARCHAR(50), " +
                    "fueltype VARCHAR(20), " +
                    "price DOUBLE, " +
                    "PRIMARY KEY (stationcode, fueltype), " +
                    "FOREIGN KEY (stationcode) REFERENCES STATIONS(code) ON DELETE CASCADE" +
                    ")"
                );
                System.out.println("Created table FUEL_PRICES.");
            } catch (SQLException e) {
                if (!e.getSQLState().equals("X0Y32")) {
                    throw e;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error initializing schema: " + e.getMessage());
        }
    }

    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            // Shutting down Derby embedded databases is standard via driver shutdown URL
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            // Derby shutdown throws an exception by design to confirm success (SQLState 08006 or XJ015)
            if (!e.getSQLState().equals("XJ015") && !e.getSQLState().equals("08006")) {
                System.err.println("Error during DB shutdown: " + e.getMessage());
            }
        }
    }
}
