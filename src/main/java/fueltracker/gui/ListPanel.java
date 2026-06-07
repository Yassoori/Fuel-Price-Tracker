package fueltracker.gui;

import fueltracker.model.Station;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ListPanel extends JPanel {
    private final JTable table;
    private final StationTableModel tableModel;

    public ListPanel() {
        this.tableModel = new StationTableModel();
        this.table = new JTable(tableModel);

        setLayout(new BorderLayout());
        setupTable();

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void setupTable() {
        // Enable row sorting on columns
        TableRowSorter<StationTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Custom renderers for formatted columns
        table.getColumnModel().getColumn(2).setCellRenderer(new DistanceRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new PriceRenderer());
        
        // Disable column reordering for a cleaner visual layout
        table.getTableHeader().setReorderingAllowed(false);
    }

    public void updateList(List<Station> stations) {
        tableModel.setStations(stations);
    }

    // ==========================================
    // INNER CLASSES: TableModel & Custom Renderers
    // ==========================================

    private static class StationTableModel extends AbstractTableModel {
        private final String[] columnNames = {"Brand", "Address", "Distance", "Price"};
        private final List<Station> stations = new ArrayList<>();

        public void setStations(List<Station> stations) {
            this.stations.clear();
            this.stations.addAll(stations);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return stations.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= stations.size()) return null;
            Station s = stations.get(row);
            
            double price = s.getPrice();
            if (price > 10.0) {
                price = price / 100.0; // Convert cents to dollars
            }

            return switch (col) {
                case 0 -> s.getBrand();
                case 1 -> s.getAddress();
                case 2 -> s.getDistance(); // Raw Double for sorting
                case 3 -> price;           // Raw Double for sorting
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return switch (col) {
                case 2, 3 -> Double.class; // Sort numerically rather than lexicographically
                default -> String.class;
            };
        }
    }

    private static class DistanceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSel, boolean hasFocus, int r, int c) {
            super.getTableCellRendererComponent(t, value, isSel, hasFocus, r, c);
            if (value instanceof Double val) {
                setText(String.format("%.2f km", val));
            }
            setHorizontalAlignment(SwingConstants.RIGHT);
            return this;
        }
    }

    private static class PriceRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSel, boolean hasFocus, int r, int c) {
            super.getTableCellRendererComponent(t, value, isSel, hasFocus, r, c);
            if (value instanceof Double val) {
                setText(String.format("$%.2f", val));
            }
            setHorizontalAlignment(SwingConstants.RIGHT);
            return this;
        }
    }
}
