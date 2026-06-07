package fueltracker.gui;

import com.formdev.flatlaf.FlatClientProperties;
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
    private static final Color MUTED_FG = new Color(0x64748B);
    private static final Color CARD_BORDER = new Color(0xE2E8F0);
    private static final Color PRIMARY = new Color(0x1E3A8A);

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
    private JLabel modeBadge;

    public MainFrame() {
        this.trackerService = new FuelTrackerService();

        setTitle("Sydney Fuel Price & Map Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);

        initComponents();
        performSearch();
    }

    private void initComponents() {
        JPanel mainContent = new JPanel(new BorderLayout(0, 16));
        mainContent.setBorder(new EmptyBorder(20, 24, 16, 24));
        mainContent.setBackground(UIManager.getColor("Panel.background"));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);
        topSection.add(buildHeader());
        topSection.add(Box.createVerticalStrut(16));
        topSection.add(buildSearchCard());
        mainContent.add(topSection, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(buildTabbedContent(), BorderLayout.CENTER);
        body.add(buildStatusBar(), BorderLayout.SOUTH);
        mainContent.add(body, BorderLayout.CENTER);

        setContentPane(mainContent);
        handleLocationChange(null);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel title = new JLabel("Sydney Fuel Tracker");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(PRIMARY);
        titles.add(title);

        JLabel subtitle = new JLabel("Compare live fuel prices from stations across Sydney");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 14f));
        subtitle.setForeground(MUTED_FG);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        titles.add(subtitle);

        header.add(titles, BorderLayout.WEST);
        return header;
    }

    private JPanel buildSearchCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        form.setOpaque(false);

        locationComboBox = new JComboBox<>(getLocationOptions());
        locationComboBox.setPreferredSize(new Dimension(180, 36));
        locationComboBox.addActionListener(this::handleLocationChange);
        form.add(createFieldGroup("Location", locationComboBox));

        fuelComboBox = new JComboBox<>(getFuelOptions());
        fuelComboBox.setPreferredSize(new Dimension(200, 36));
        form.add(createFieldGroup("Fuel type", fuelComboBox));

        radiusComboBox = new JComboBox<>(new Integer[]{5, 10, 20, 50});
        radiusComboBox.setSelectedItem(20);
        radiusComboBox.setPreferredSize(new Dimension(100, 36));
        form.add(createFieldGroup("Radius (km)", radiusComboBox));

        latField = new JTextField("-33.8688", 10);
        latField.setPreferredSize(new Dimension(110, 36));
        form.add(createFieldGroup("Latitude", latField));

        lonField = new JTextField("151.2093", 10);
        lonField.setPreferredSize(new Dimension(110, 36));
        form.add(createFieldGroup("Longitude", lonField));

        searchButton = new JButton("Search stations");
        searchButton.setPreferredSize(new Dimension(140, 36));
        searchButton.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        searchButton.addActionListener(e -> performSearch());
        form.add(createFieldGroup(" ", searchButton));

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFieldGroup(String label, JComponent field) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        lbl.setForeground(MUTED_FG);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(lbl);
        group.add(Box.createVerticalStrut(6));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.add(field);
        return group;
    }

    private JTabbedPane buildTabbedContent() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE,
                FlatClientProperties.TABBED_PANE_TAB_TYPE_CARD);

        mapPanel = new MapPanel();
        listPanel = new ListPanel();

        tabbedPane.addTab("Map", mapPanel);
        tabbedPane.addTab("Station list", listPanel);
        return tabbedPane;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 2, 0, 2));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        statusLabel.setForeground(MUTED_FG);
        bar.add(statusLabel, BorderLayout.WEST);

        modeBadge = new JLabel(" ");
        modeBadge.setFont(modeBadge.getFont().deriveFont(Font.BOLD, 12f));
        modeBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        modeBadge.setOpaque(true);
        modeBadge.setVisible(false);
        bar.add(modeBadge, BorderLayout.EAST);

        return bar;
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

            statusLabel.setText("Fetching prices…");
            updateModeBadge(null);
            searchButton.setEnabled(false);

            SwingWorker<List<Station>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<Station> doInBackground() {
                    return trackerService.searchStations(lat, lon, radius, fuel.apiCode);
                }

                @Override
                protected void done() {
                    try {
                        List<Station> stations = get();
                        mapPanel.updateMap(stations, lat, lon, radius);
                        listPanel.updateList(stations);

                        boolean offline = trackerService.isLastSearchWasOffline();
                        statusLabel.setText(String.format(
                                "Found %d station%s within %d km",
                                stations.size(), stations.size() == 1 ? "" : "s", radius
                        ));
                        updateModeBadge(offline);
                    } catch (Exception ex) {
                        statusLabel.setText("Search failed: " + ex.getMessage());
                        updateModeBadge(null);
                    } finally {
                        searchButton.setEnabled(true);
                    }
                }
            };
            worker.execute();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Please enter valid numeric latitude and longitude coordinates.",
                    "Invalid Coordinates", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateModeBadge(Boolean offline) {
        if (offline == null) {
            modeBadge.setText(" ");
            modeBadge.setVisible(false);
            return;
        }
        modeBadge.setVisible(true);
        if (offline) {
            modeBadge.setText("Offline");
            modeBadge.setForeground(new Color(0x92400E));
            modeBadge.setBackground(new Color(0xFEF3C7));
        } else {
            modeBadge.setText("Live");
            modeBadge.setForeground(new Color(0x166534));
            modeBadge.setBackground(new Color(0xDCFCE7));
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

    public static void main(String[] args) {
        FlatLightLaf.setup();
        FlatLaf.registerCustomDefaultsSource("themes");

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
