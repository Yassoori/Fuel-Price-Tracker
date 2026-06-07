package fueltracker.gui;

import fueltracker.model.Station;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter;
import org.jxmapviewer.viewer.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapPanel extends JPanel {

    private static final Color MARKER_DOT = new Color(0x3B82F6);
    private static final Color MARKER_TEXT = new Color(0x1E3A8A);
    private static final int DEFAULT_ZOOM = 6; // Much closer initial zoom

    private final JXMapViewer mapViewer;
    private final Set<Waypoint> waypoints;
    private final WaypointPainter<Waypoint> waypointPainter;

    public MapPanel() {
        this.mapViewer = new JXMapViewer();
        this.waypoints = new HashSet<>();
        this.waypointPainter = new WaypointPainter<>();

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel mapFrame = new JPanel(new BorderLayout());
        mapFrame.setBackground(Color.WHITE);
        // Removed border for a flatter, modern web app aesthetic

        setupMap();
        mapFrame.add(mapViewer, BorderLayout.CENTER);
        add(mapFrame, BorderLayout.CENTER);
    }

    private void setupMap() {
        TileFactoryInfo info = new LightMapTileFactoryInfo();
        DefaultTileFactory tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        PanMouseInputListener panner = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(panner);
        mapViewer.addMouseMotionListener(panner);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCenter(mapViewer));

        GeoPosition sydneyCBD = new GeoPosition(-33.8688, 151.2093);
        mapViewer.setAddressLocation(sydneyCBD);
        mapViewer.setZoom(DEFAULT_ZOOM);

        waypointPainter.setRenderer(new PriceWaypointRenderer());
        mapViewer.setOverlayPainter(waypointPainter);
    }

    public void updateMap(List<Station> stations, double searchLat, double searchLon, int radiusKm) {
        waypoints.clear();
        for (Station s : stations) {
            waypoints.add(new StationWaypoint(s));
        }
        waypointPainter.setWaypoints(waypoints);

        GeoPosition searchPoint = new GeoPosition(searchLat, searchLon);
        mapViewer.setAddressLocation(searchPoint);
        mapViewer.setZoom(zoomForRadius(radiusKm));

        mapViewer.repaint();
    }

    private static int zoomForRadius(int radiusKm) {
        return switch (radiusKm) {
            case 5 -> 4;
            case 10 -> 5;
            case 50 -> 8;
            default -> 6; // 20km radius
        };
    }

    private static class LightMapTileFactoryInfo extends TileFactoryInfo {
        LightMapTileFactoryInfo() {
            super("Carto Light", 1, 20, 19, 256, true, true,
                    "https://a.basemaps.cartocdn.com/light_all", "x", "y", "z");
        }

        @Override
        public String getTileUrl(int x, int y, int zoom) {
            int z = this.getTotalMapZoom() - zoom;
            return "https://a.basemaps.cartocdn.com/light_all/" + z + "/" + x + "/" + y + ".png";
        }
    }

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
            Point2D pt = map.convertGeoPositionToPoint(wp.getPosition());
            int x = (int) pt.getX();
            int y = (int) pt.getY();

            double price = station.getPrice();
            if (price > 10.0) {
                price = price / 100.0;
            }
            String priceLabel = String.format("$%.2f", price);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();

            int padX = 5;
            int padY = 3;
            int labelW = fm.stringWidth(priceLabel) + padX * 2;
            int labelH = fm.getHeight() + padY;
            int labelX = x - labelW / 2;
            int labelY = y - labelH - 10;

            // Soft shadow for legibility on the map
            g.setColor(new Color(0, 0, 0, 30));
            g.fillRoundRect(labelX + 1, labelY + 1, labelW, labelH, 6, 6);

            // Compact price label
            g.setColor(new Color(255, 255, 255, 245));
            g.fillRoundRect(labelX, labelY, labelW, labelH, 6, 6);
            g.setColor(new Color(0xCBD5E1));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(labelX, labelY, labelW, labelH, 6, 6);

            g.setColor(MARKER_TEXT);
            g.drawString(priceLabel, labelX + padX, labelY + fm.getAscent() + padY / 2);

            // Small dot pin at the exact station location
            g.setColor(MARKER_DOT);
            g.fillOval(x - 4, y - 4, 8, 8);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(x - 4, y - 4, 8, 8);
        }
    }
}
