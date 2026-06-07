package fueltracker.gui;

import fueltracker.model.Station;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ListPanel extends JPanel {
    private static final Color CARD_BORDER = new Color(0xE2E8F0);
    private static final Color MUTED_FG = new Color(0x64748B);

    private final JTable table;
    private final StationTableModel tableModel;
    private final JLabel countLabel;

    public ListPanel() {
        this.tableModel = new StationTableModel();
        this.table = new JTable(tableModel);
        this.countLabel = new JLabel("No stations loaded");

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 13f));
        countLabel.setForeground(MUTED_FG);
        add(countLabel, BorderLayout.NORTH);

        JPanel tableFrame = new JPanel(new BorderLayout());
        tableFrame.setBackground(Color.WHITE);
        tableFrame.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1, true));

        setupTable();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableFrame.add(scrollPane, BorderLayout.CENTER);
        add(tableFrame, BorderLayout.CENTER);
    }

    private void setupTable() {
        TableRowSorter<StationTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        table.getColumnModel().getColumn(2).setCellRenderer(new DistanceRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new PriceRenderer());
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(360);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
    }

    public void updateList(List<Station> stations) {
        tableModel.setStations(stations);
        countLabel.setText(stations.isEmpty()
                ? "No stations found for this search"
                : String.format("Showing %d station%s — click column headers to sort",
                stations.size(), stations.size() == 1 ? "" : "s"));
    }

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
                price = price / 100.0;
            }

            return switch (col) {
                case 0 -> s.getBrand();
                case 1 -> s.getAddress();
                case 2 -> s.getDistance();
                case 3 -> price;
                default -> null;
            };
        }

        @Override
        public Class<?> getColumnClass(int col) {
            return switch (col) {
                case 2, 3 -> Double.class;
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
                if (!isSel) {
                    setForeground(new Color(0x1E3A8A));
                    setFont(getFont().deriveFont(Font.BOLD));
                }
            }
            setHorizontalAlignment(SwingConstants.RIGHT);
            return this;
        }
    }
}
