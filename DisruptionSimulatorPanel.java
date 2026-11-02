import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Panel that simulates disruptions on roads (closure, traffic increase, restoration)
 * and optionally bus breakdowns. It records a history of actions and can show the
 * impact on routes using the existing Dijkstra implementation.
 */
public class DisruptionSimulatorPanel extends JPanel {
    private JComboBox<String> cbRoads; // "start->end"
    private JButton btnCloseRoad;
    private JButton btnIncreaseTraffic;
    private JButton btnRestoreRoad;
    private JComboBox<String> cbBuses;
    private JButton btnBreakdownBus;
    private JButton btnRestoreBus;

    private JTable historyTable;
    private DefaultTableModel historyModel;

    private static class RoadState {
        RoadStatus status;
        int travelTime;
        RoadState(RoadStatus s, int t) { status = s; travelTime = t; }
    }
    private final Map<String, RoadState> originalRoadStates = new HashMap<>();
    // Helper to refresh both combos at once
    public void refreshAllCombos(){
        refreshRoadCombo();
        refreshBusCombo();
    }

    public DisruptionSimulatorPanel() {
        setLayout(new BorderLayout(10,10));
        initControls();
        initHistory();
        refreshRoadCombo();
        refreshBusCombo();
    }

    private void initControls() {
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0;
        top.add(new JLabel("Select Road:"), gbc);
        gbc.gridx = 1;
        cbRoads = new JComboBox<>();
        top.add(cbRoads, gbc);

        gbc.gridx = 0; gbc.gridy++;
        btnCloseRoad = new JButton("Close Road");
        top.add(btnCloseRoad, gbc);
        gbc.gridx = 1;
        btnRestoreRoad = new JButton("Restore Road");
        top.add(btnRestoreRoad, gbc);

        gbc.gridx = 0; gbc.gridy++;
        btnIncreaseTraffic = new JButton("Increase Travel Time");
        top.add(btnIncreaseTraffic, gbc);
        gbc.gridx = 1;
        top.add(new JLabel(""), gbc);

        gbc.gridx = 0; gbc.gridy++;
        top.add(new JLabel("Select Bus:"), gbc);
        gbc.gridx = 1;
        cbBuses = new JComboBox<>();
        top.add(cbBuses, gbc);

        gbc.gridx = 0; gbc.gridy++;
        btnBreakdownBus = new JButton("Mark Unavailable");
        top.add(btnBreakdownBus, gbc);
        gbc.gridx = 1;
        btnRestoreBus = new JButton("Make Available");
        top.add(btnRestoreBus, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JButton btnClear = new JButton("Clear Selection");
        top.add(btnClear, gbc);

        add(top, BorderLayout.NORTH);

        btnCloseRoad.addActionListener(this::handleCloseRoad);
        btnRestoreRoad.addActionListener(this::handleRestoreRoad);
        btnIncreaseTraffic.addActionListener(this::handleIncreaseTraffic);
        btnBreakdownBus.addActionListener(this::handleBreakdownBus);
        btnRestoreBus.addActionListener(this::handleRestoreBus);
        btnClear.addActionListener(e -> {
            if (cbRoads.getItemCount() > 0) cbRoads.setSelectedIndex(0);
            if (cbBuses.getItemCount() > 0) cbBuses.setSelectedIndex(0);
            if (historyTable != null) historyTable.clearSelection();
        });
    }

    private void initHistory() {
        historyModel = new DefaultTableModel(new Object[]{"Type","Target","Action","Result"},0){
            public boolean isCellEditable(int row, int col){return false;}
        };
        historyTable = new JTable(historyModel);
        JScrollPane sp = new JScrollPane(historyTable);
        sp.setPreferredSize(new Dimension(500,150));
        add(sp, BorderLayout.CENTER);

        try {
            List<DisruptionRecord> records = DatabaseManager.loadDisruptions();
            for (DisruptionRecord dr : records) {
                historyModel.addRow(new Object[]{dr.getType(), dr.getTarget(), dr.getAction(), dr.getResult()});
            }
        } catch (Exception ex) {
            System.err.println("Could not load disruption history from database: " + ex.getMessage());
        }
    }

    public void refreshRoadCombo(){
        List<Road> roads = DataStore.getNetwork().getRoads();
        cbRoads.removeAllItems();
        for (Road r : roads) {
            cbRoads.addItem(r.getStartStopId() + "->" + r.getEndStopId());
        }
        boolean hasRoads = cbRoads.getItemCount() > 0;
        btnCloseRoad.setEnabled(hasRoads);
        btnRestoreRoad.setEnabled(hasRoads);
        btnIncreaseTraffic.setEnabled(hasRoads);
    }
    public void refreshBusCombo(){
        List<Bus> buses = DataStore.getNetwork().getBuses();
        cbBuses.removeAllItems();
        for (Bus b : buses) {
            cbBuses.addItem(b.getId());
        }
        boolean hasBuses = cbBuses.getItemCount() > 0;
        btnBreakdownBus.setEnabled(hasBuses);
        btnRestoreBus.setEnabled(hasBuses);
    }

    private void handleCloseRoad(ActionEvent e){
        String key = (String)cbRoads.getSelectedItem();
        if (key == null) { showError("Select a road first."); return; }
        String[] parts = key.split("->");
        if (parts.length != 2) { showError("Invalid road identifier."); return; }
        Road road = DataStore.getNetwork().getRoad(parts[0], parts[1]);
        if (road == null) { showError("Road not found."); return; }
        originalRoadStates.putIfAbsent(key, new RoadState(road.getStatus(), road.getTravelTime()));
        boolean ok = DataStore.getNetwork().updateRoad(parts[0], parts[1], road.getTravelTime(), RoadStatus.CLOSED);
        if (ok) {
            addHistory("Road", key, "Close", "Success");
            MainFrame.getInstance().refreshDashboard();
        } else {
            addHistory("Road", key, "Close", "Failed");
        }
    }

    private void handleRestoreRoad(ActionEvent e){
        String key = (String)cbRoads.getSelectedItem();
        if (key == null) { showError("Select a road first."); return; }
        RoadState orig = originalRoadStates.get(key);
        if (orig == null) { showError("No recorded original state."); return; }
        String[] parts = key.split("->");
        boolean ok = DataStore.getNetwork().updateRoad(parts[0], parts[1], orig.travelTime, orig.status);
        if (ok) {
            addHistory("Road", key, "Restore", "Success");
            originalRoadStates.remove(key);
            MainFrame.getInstance().refreshDashboard();
        } else {
            addHistory("Road", key, "Restore", "Failed");
        }
    }

    private void handleIncreaseTraffic(ActionEvent e){
        String key = (String)cbRoads.getSelectedItem();
        if (key == null) { showError("Select a road first."); return; }
        String[] parts = key.split("->");
        Road road = DataStore.getNetwork().getRoad(parts[0], parts[1]);
        if (road == null) { showError("Road not found."); return; }
        String input = JOptionPane.showInputDialog(this, "Additional travel time (min):", "Traffic Impact", JOptionPane.PLAIN_MESSAGE);
        if (input == null) return;
        int add;
        try { add = Integer.parseInt(input.trim()); if (add <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException ex) { showError("Enter a positive integer."); return; }
        originalRoadStates.putIfAbsent(key, new RoadState(road.getStatus(), road.getTravelTime()));
        int newTime = road.getTravelTime() + add;
        boolean ok = DataStore.getNetwork().updateRoad(parts[0], parts[1], newTime, road.getStatus());
        if (ok) {
            addHistory("Road", key, "Increase+"+add, "Success");
            MainFrame.getInstance().refreshDashboard();
        } else {
            addHistory("Road", key, "Increase+"+add, "Failed");
        }
    }

    private void handleBreakdownBus(ActionEvent e){
        String busId = (String)cbBuses.getSelectedItem();
        if (busId == null) { showError("Select a bus first."); return; }
        Bus bus = DataStore.getNetwork().getBuses().stream().filter(b->b.getId().equals(busId)).findFirst().orElse(null);
        if (bus == null) { showError("Bus not found."); return; }
        boolean ok = DataStore.getNetwork().updateBusAvailability(busId, false);
        if (ok) {
            addHistory("Bus", busId, "Breakdown", "Success");
            MainFrame.getInstance().refreshDashboard();
        } else {
            addHistory("Bus", busId, "Breakdown", "Failed");
        }
    }

    private void handleRestoreBus(ActionEvent e){
        String busId = (String)cbBuses.getSelectedItem();
        if (busId == null) { showError("Select a bus first."); return; }
        Bus bus = DataStore.getNetwork().getBuses().stream().filter(b->b.getId().equals(busId)).findFirst().orElse(null);
        if (bus == null) { showError("Bus not found."); return; }
        boolean ok = DataStore.getNetwork().updateBusAvailability(busId, true);
        if (ok) {
            addHistory("Bus", busId, "Restore", "Success");
            MainFrame.getInstance().refreshDashboard();
        } else {
            addHistory("Bus", busId, "Restore", "Failed");
        }
    }

    private void showError(String msg){
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    private void addHistory(String type, String target, String action, String result){
        historyModel.addRow(new Object[]{type, target, action, result});
        try {
            DatabaseManager.insertDisruption(type, target, action, result);
        } catch (Exception ex) {
            System.err.println("Could not log disruption to database: " + ex.getMessage());
        }
    }
}
