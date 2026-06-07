package fueltracker.gui;

import fueltracker.model.Station;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapPanel extends JPanel {
    private final JXMapViewer mapViewer;
    private final Set<Waypoint> waypoints;
    private final WaypointPainter<Waypoint> waypointPainter;

    public MapPanel() {
        this.mapViewer = new JXMapViewer();
        this.waypoints = new HashSet<>();
        this.waypointPainter = new WaypointPainter<>();

        setLayout(new BorderLayout());
        setupMap();
        add(mapViewer, BorderLayout.CENTER);
    }

    private void setupMap() {
        // Create an OpenStreetMap tile factory
        TileFactoryInfo info = new OSMTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        // Enable pan mouse listeners
        PanMouseInputListener panner = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panner);
        mapViewer.addMouseMotionListener(panner);

        // Enable zoom wheel listener
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));

        // Default focus: Center of Sydney CBD
        GeoPosition sydneyCBD = new GeoPosition(-33.8688, 151.2093);
        mapViewer.setAddressLocation(sydneyCBD);
        mapViewer.setZoom(5);

        // Set custom waypoint painter with our price rendering logic
        waypointPainter.setRenderer(new PriceWaypointRenderer());
        mapViewer.setOverlayPainter(waypointPainter);
    }

    /**
     * Updates the map with a list of stations, clearing old pins
     * and auto-focusing on the reference search coordinate.
     */
    public void updateMap(List<Station> stations, double searchLat, double searchLon) {
        waypoints.clear();
        for (Station s : stations) {
            waypoints.add(new StationWaypoint(s));
        }
        waypointPainter.setWaypoints(waypoints);

        // Re-focus and center on the search coordinates
        GeoPosition searchPoint = new GeoPosition(searchLat, searchLon);
        mapViewer.setAddressLocation(searchPoint);
        mapViewer.setZoom(6); // A suitable view zoom level for 20km radius

        mapViewer.repaint();
    }

    // ==========================================
    // INNER CLASSES: Waypoint & Custom Renderer
    // ==========================================

    private static class StationWaypoint extends DefaultWaypoint {
        private final Station station;

        public StationWaypoint(Station station) {
            super(new GeoPosition(station.getLatitude(), station.getLongitude()));
            this.station = station;
        }

        public Station getStation() {
            return station;
        }
    }

    private static class PriceWaypointRenderer implements WaypointRenderer<Waypoint> {
        @Override
        public void paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint wp) {
            if (!(wp instanceof StationWaypoint stationWaypoint)) {
                return;
            }

            Station station = stationWaypoint.getStation();

            // Convert geo coordinates to 2D pixel coordinates on screen
            Point2D pt = map.getTileFactory().geoToPoint(wp.getPosition(), map.getZoom());
            int x = (int) pt.getX();
            int y = (int) pt.getY();

            // Format price: divide by 100 if stored in cents
            double price = station.getPrice();
            if (price > 10.0) {
                price = price / 100.0;
            }
            String priceLabel = String.format("$%.2f", price);

            // Configure Graphics rendering hints for smooth text
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setFont(new Font("Inter", Font.BOLD, 11));
            FontMetrics fm = g.getFontMetrics();
            int width = fm.stringWidth(priceLabel) + 10;
            int height = fm.getHeight() + 6;

            // Draw stem pointing to the location
            g.setColor(new Color(30, 58, 138)); // Primary brand color (#1E3A8A)
            g.setStroke(new BasicStroke(2));
            g.drawLine(x, y, x, y + 8);

            // Draw price bubble background
            g.setColor(new Color(30, 58, 138));
            g.fillRoundRect(x - width / 2, y - height, width, height, 8, 8);

            // Draw contrast border
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(x - width / 2, y - height, width, height, 8, 8);

            // Draw text
            g.setColor(Color.WHITE);
            g.drawString(priceLabel, x - width / 2 + 5, y - height / 2 + fm.getAscent() / 2 - 1);
        }
    }
}
