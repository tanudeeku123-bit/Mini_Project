import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ReportsPanel extends JPanel {
    // Summary labels
    private JLabel lblStops, lblRoads, lblOpenRoads, lblClosedRoads;
    private JLabel lblBuses, lblAvailableBuses;
    private JLabel lblRequests, lblPendingRequests, lblAssignedRequests, lblCompletedRequests;

    // Tables
    private JTable tblStops, tblRoads, tblBuses, tblRequests;
    private DefaultTableModel modelStops, modelRoads, modelBuses, modelRequests;

    // Refresh button & Export button
    private JButton btnRefresh, btnExport;

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        initSummaryPanel();
        initTabs();
        initActions();
        refreshAll();
    }

    private void initSummaryPanel() {
        JPanel summary = new JPanel(new GridLayout(2, 5, 8, 8));
        lblStops = createStatLabel("Stops: 0");
        lblRoads = createStatLabel("Roads: 0");
        lblOpenRoads = createStatLabel("Open Roads: 0");
        lblClosedRoads = createStatLabel("Closed Roads: 0");
        lblBuses = createStatLabel("Buses: 0");
        lblAvailableBuses = createStatLabel("Available Buses: 0");
        lblRequests = createStatLabel("Requests: 0");
        lblPendingRequests = createStatLabel("Pending: 0");
        lblAssignedRequests = createStatLabel("Assigned: 0");
        lblCompletedRequests = createStatLabel("Completed: 0");
        summary.add(lblStops);
        summary.add(lblRoads);
        summary.add(lblOpenRoads);
        summary.add(lblClosedRoads);
        summary.add(lblBuses);
        summary.add(lblAvailableBuses);
        summary.add(lblRequests);
        summary.add(lblPendingRequests);
        summary.add(lblAssignedRequests);
        summary.add(lblCompletedRequests);
        add(summary, BorderLayout.NORTH);
    }

    private JLabel createStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setOpaque(true);
        l.setBackground(new Color(230, 240, 255));
        l.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        return l;
    }

    private void initTabs() {
        JTabbedPane tabs = new JTabbedPane();
        // Stops tab
        modelStops = new DefaultTableModel(new Object[]{"ID", "Name", "Description"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblStops = new JTable(modelStops);
        tabs.addTab("Stops", new JScrollPane(tblStops));
        // Roads tab
        modelRoads = new DefaultTableModel(new Object[]{"Start", "End", "Time", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblRoads = new JTable(modelRoads);
        tabs.addTab("Roads", new JScrollPane(tblRoads));
        // Buses tab
        modelBuses = new DefaultTableModel(new Object[]{"ID", "Capacity", "Available"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblBuses = new JTable(modelBuses);
        tabs.addTab("Buses", new JScrollPane(tblBuses));
        // Requests tab
        modelRequests = new DefaultTableModel(new Object[]{"ID", "Source", "Dest", "Count", "Status"}, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tblRequests = new JTable(modelRequests);
        tabs.addTab("Requests", new JScrollPane(tblRequests));
        add(tabs, BorderLayout.CENTER);
    }

    private void initActions() {
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefresh = new JButton("Refresh");
        btnExport = new JButton("Export CSV");
        btnRefresh.addActionListener(this::handleRefresh);
        btnExport.addActionListener(this::handleExport);
        btnPanel.add(btnRefresh);
        btnPanel.add(btnExport);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void handleRefresh(ActionEvent e) {
        refreshAll();
    }

    /** Refreshes all statistics and table contents */
    public void refreshAll() {
        TransportNetwork net = DataStore.getNetwork();
        // Summary stats
        lblStops.setText("Stops: " + net.getStops().size());
        lblRoads.setText("Roads: " + net.getRoads().size());
        long open = net.getRoads().stream().filter(r -> r.getStatus() == RoadStatus.OPEN).count();
        long closed = net.getRoads().stream().filter(r -> r.getStatus() == RoadStatus.CLOSED).count();
        lblOpenRoads.setText("Open Roads: " + open);
        lblClosedRoads.setText("Closed Roads: " + closed);
        lblBuses.setText("Buses: " + net.getBuses().size());
        long available = net.getBuses().stream().filter(Bus::isAvailable).count();
        lblAvailableBuses.setText("Available Buses: " + available);
        lblRequests.setText("Requests: " + net.getRequests().size());
        long pending = net.getRequests().stream().filter(r -> "Pending".equalsIgnoreCase(r.getStatus())).count();
        lblPendingRequests.setText("Pending: " + pending);
        long assigned = net.getRequests().stream().filter(r -> "Accepted".equalsIgnoreCase(r.getStatus()) || "Assigned".equalsIgnoreCase(r.getStatus())).count();
        lblAssignedRequests.setText("Accepted: " + assigned);
        long completed = net.getRequests().stream().filter(r -> "Completed".equalsIgnoreCase(r.getStatus())).count();
        lblCompletedRequests.setText("Completed: " + completed);
        // Tables
        refreshStopsTable();
        refreshRoadsTable();
        refreshBusesTable();
        refreshRequestsTable();
    }

    private void refreshStopsTable() {
        List<Stop> stops = DataStore.getNetwork().getStops();
        modelStops.setRowCount(0);
        for (Stop s : stops) {
            modelStops.addRow(new Object[]{s.getId(), s.getName(), s.getDescription()});
        }
    }

    private void refreshRoadsTable() {
        List<Road> roads = DataStore.getNetwork().getRoads();
        modelRoads.setRowCount(0);
        for (Road r : roads) {
            modelRoads.addRow(new Object[]{r.getStartStopId(), r.getEndStopId(), r.getTravelTime(), r.getStatus()});
        }
    }

    private void refreshBusesTable() {
        List<Bus> buses = DataStore.getNetwork().getBuses();
        modelBuses.setRowCount(0);
        for (Bus b : buses) {
            modelBuses.addRow(new Object[]{b.getId(), b.getCapacity(), b.isAvailable()});
        }
    }

    private void refreshRequestsTable() {
        List<PassengerRequest> reqs = DataStore.getNetwork().getRequests();
        modelRequests.setRowCount(0);
        for (PassengerRequest r : reqs) {
            modelRequests.addRow(new Object[]{r.getId(), r.getSourceStopId(), r.getDestinationStopId(), r.getPassengerCount(), r.getStatus()});
        }
    }

    private void handleExport(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Report CSV");
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (FileWriter fw = new FileWriter(chooser.getSelectedFile())) {
            // Write summary
            fw.write("Summary Statistics\n");
            fw.write("Stops," + DataStore.getNetwork().getStops().size() + "\n");
            fw.write("Roads," + DataStore.getNetwork().getRoads().size() + "\n");
            fw.write("Open Roads," + DataStore.getNetwork().getRoads().stream().filter(r -> r.getStatus() == RoadStatus.OPEN).count() + "\n");
            fw.write("Closed Roads," + DataStore.getNetwork().getRoads().stream().filter(r -> r.getStatus() == RoadStatus.CLOSED).count() + "\n");
            fw.write("Buses," + DataStore.getNetwork().getBuses().size() + "\n");
            fw.write("Available Buses," + DataStore.getNetwork().getBuses().stream().filter(Bus::isAvailable).count() + "\n");
            fw.write("Requests," + DataStore.getNetwork().getRequests().size() + "\n");
            fw.write("Pending Requests," + DataStore.getNetwork().getRequests().stream().filter(r -> "Pending".equals(r.getStatus())).count() + "\n");
            fw.write("Completed Requests," + DataStore.getNetwork().getRequests().stream().filter(r -> "Completed".equals(r.getStatus())).count() + "\n\n");
            // Tables
            writeTable(fw, "Stops", modelStops);
            writeTable(fw, "Roads", modelRoads);
            writeTable(fw, "Buses", modelBuses);
            writeTable(fw, "Requests", modelRequests);
            JOptionPane.showMessageDialog(this, "Report exported successfully.", "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to export report: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeTable(FileWriter fw, String title, DefaultTableModel model) throws IOException {
        fw.write(title + "\n");
        // header
        for (int c = 0; c < model.getColumnCount(); c++) {
            fw.write(model.getColumnName(c));
            if (c < model.getColumnCount() - 1) fw.write(",");
        }
        fw.write("\n");
        // rows
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                fw.write(String.valueOf(model.getValueAt(r, c)));
                if (c < model.getColumnCount() - 1) fw.write(",");
            }
            fw.write("\n");
        }
        fw.write("\n");
    }
}
