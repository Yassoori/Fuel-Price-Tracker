package fueltracker.gui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import fueltracker.model.Station;
import fueltracker.service.FuelTrackerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class MainFrame extends JFrame {
    private final FuelTrackerService trackerService;

    // Search Controls
    private JComboBox<LocationOption> locationComboBox;
    private JTextField latField;
    private JTextField lonField;
    private JComboBox<FuelOption> fuelComboBox;
    private JComboBox<Integer> radiusComboBox;
    private JButton searchButton;

    // Tab Panels
    private MapPanel mapPanel;
    private ListPanel listPanel;
    private JLabel statusLabel;

    public MainFrame() {
        this.trackerService = new FuelTrackerService();

        setTitle("Sydney Fuel Price & Map Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        initComponents();
        performSearch(); // Perform initial search on startup
    }

    private void initComponents() {
        // Layout structure: Border Layout
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 1. Top Panel: Search Controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("Search Settings"));

        // Location selection
        topPanel.add(new JLabel("Location:"));
        locationComboBox = new JComboBox<>(getLocationOptions());
        locationComboBox.addActionListener(this::handleLocationChange);
        topPanel.add(locationComboBox);

        topPanel.add(new JLabel("Lat:"));
        latField = new JTextField("-33.8688", 8);
        topPanel.add(latField);

        topPanel.add(new JLabel("Lon:"));
        lonField = new JTextField("151.2093", 8);
        topPanel.add(lonField);

        // Fuel Type selector (Default U91)
        topPanel.add(new JLabel("Fuel Type:"));
        fuelComboBox = new JComboBox<>(getFuelOptions());
        topPanel.add(fuelComboBox);

        // Radius Selector (Default 20 km)
        topPanel.add(new JLabel("Radius:"));
        radiusComboBox = new JComboBox<>(new Integer[]{5, 10, 20, 50});
        radiusComboBox.setSelectedItem(20);
        topPanel.add(radiusComboBox);

        // Search Button
        searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());
        topPanel.add(searchButton);

        mainContent.add(topPanel, BorderLayout.NORTH);

        // 2. Center Panel: Tabbed Interface (Map and List)
        JTabbedPane tabbedPane = new JTabbedPane();
        
        mapPanel = new MapPanel();
        listPanel = new ListPanel();

        tabbedPane.addTab("Map View", mapPanel);
        tabbedPane.addTab("List View", listPanel);

        mainContent.add(tabbedPane, BorderLayout.CENTER);

        // 3. Bottom Panel: Status Bar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(6, 4, 0, 4));
        statusLabel = new JLabel("Application Ready.");
        bottomPanel.add(statusLabel, BorderLayout.WEST);

        mainContent.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainContent);

        // Initial setup for location fields
        handleLocationChange(null);
    }

    private void handleLocationChange(ActionEvent e) {
        LocationOption selected = (LocationOption) locationComboBox.getSelectedItem();
        if (selected != null) {
            if (selected.isCustom) {
                latField.setEnabled(true);
                lonField.setEnabled(true);
            } else {
                latField.setText(String.valueOf(selected.lat));
                lonField.setText(String.valueOf(selected.lon));
                latField.setEnabled(false);
                lonField.setEnabled(false);
            }
        }
    }

    private void performSearch() {
        try {
            double lat = Double.parseDouble(latField.getText().trim());
            double lon = Double.parseDouble(lonField.getText().trim());
            FuelOption fuel = (FuelOption) fuelComboBox.getSelectedItem();
            int radius = (int) radiusComboBox.getSelectedItem();

            if (fuel == null) return;

            statusLabel.setText("Fetching prices...");
            searchButton.setEnabled(false);

            // Execute service search in a background thread to prevent UI freezing
            SwingWorker<List<Station>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<Station> doInBackground() {
                    return trackerService.searchStations(lat, lon, radius, fuel.apiCode);
                }

                @Override
                protected void done() {
                    try {
                        List<Station> stations = get();
                        mapPanel.updateMap(stations, lat, lon);
                        listPanel.updateList(stations);

                        String mode = trackerService.isLastSearchWasOffline() ? "Offline Mode (Fallback Loaded)" : "Online Mode";
                        statusLabel.setText(String.format("Found %d stations within %d km of search point. (Mode: %s)", 
                                stations.size(), radius, mode));
                    } catch (Exception ex) {
                        statusLabel.setText("Search failed: " + ex.getMessage());
                    } finally {
                        searchButton.setEnabled(true);
                    }
                }
            };
            worker.execute();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric latitude and longitude coordinates.", 
                    "Invalid Coordinates", JOptionPane.ERROR_MESSAGE);
        }
    }

    private LocationOption[] getLocationOptions() {
        return new LocationOption[]{
                new LocationOption("Sydney CBD", -33.8688, 151.2093, false),
                new LocationOption("Surry Hills", -33.8861, 151.2116, false),
                new LocationOption("Chatswood", -33.8012, 151.1789, false),
                new LocationOption("Parramatta", -33.8085, 151.0042, false),
                new LocationOption("Custom Coordinates", 0.0, 0.0, true)
        };
    }

    private FuelOption[] getFuelOptions() {
        return new FuelOption[]{
                new FuelOption("Unleaded 91 (U91)", "U91"),
                new FuelOption("E10 Ethanol (E10)", "E10"),
                new FuelOption("Premium 95 (P95)", "P95"),
                new FuelOption("Premium 98 (P98)", "P98"),
                new FuelOption("Diesel (DL)", "DL")
        };
    }

    // ==========================================
    // INNER HELPER CLASSES
    // ==========================================

    private static class LocationOption {
        final String name;
        final double lat;
        final double lon;
        final boolean isCustom;

        LocationOption(String name, double lat, double lon, boolean isCustom) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.isCustom = isCustom;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class FuelOption {
        final String displayName;
        final String apiCode;

        FuelOption(String displayName, String apiCode) {
            this.displayName = displayName;
            this.apiCode = apiCode;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Main executable launcher for GUI testing
    public static void main(String[] args) {
        // Setup FlatLaf look and feel
        FlatLightLaf.setup();
        FlatLaf.registerCustomDefaultsSource("themes");

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
