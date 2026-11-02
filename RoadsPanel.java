import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class RoadsPanel extends JPanel {
    private JComboBox<String> cbStart, cbEnd;
    private JTextField tfTravelTime;
    private JComboBox<RoadStatus> cbStatus;
    private JButton btnAdd, btnUpdate, btnRemove, btnClear;
    private JTable roadTable;
    private DefaultTableModel tableModel;

    public RoadsPanel() {
        setLayout(new BorderLayout(10,10));
        initForm();
        initTable();
        refreshTable();
    }

    private void initForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("Start Stop:"), gbc);
        gbc.gridx = 1;
        cbStart = new JComboBox<>();
        form.add(cbStart, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("End Stop:"), gbc);
        gbc.gridx = 1;
        cbEnd = new JComboBox<>();
        form.add(cbEnd, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Travel Time (min):"), gbc);
        gbc.gridx = 1;
        tfTravelTime = new JTextField(5);
        form.add(tfTravelTime, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Status:"), gbc);
        gbc.gridx = 1;
        cbStatus = new JComboBox<>(RoadStatus.values());
        form.add(cbStatus, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        btnAdd = new JButton("Add Road");
        btnUpdate = new JButton("Update Selected");
        btnRemove = new JButton("Remove Selected");
        btnClear = new JButton("Clear Form");
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnRemove);
        btnPanel.add(btnClear);
        form.add(btnPanel, gbc);

        add(form, BorderLayout.NORTH);

        // listeners
        btnAdd.addActionListener(this::handleAdd);
        btnUpdate.addActionListener(this::handleUpdate);
        btnRemove.addActionListener(this::handleRemove);
        btnClear.addActionListener(e -> clearForm());

        cbStart.addActionListener(e -> updateEndCombo());
    }

    private void clearForm() {
        if (cbStart.getItemCount() > 0) cbStart.setSelectedIndex(0);
        updateEndCombo();
        tfTravelTime.setText("");
        cbStatus.setSelectedItem(RoadStatus.OPEN);
        if (roadTable != null) roadTable.clearSelection();
    }

    private boolean contains(JComboBox<String> cb, String item) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).equals(item)) return true;
        }
        return false;
    }

    private void updateEndCombo() {
        String start = (String) cbStart.getSelectedItem();
        String prevEnd = (String) cbEnd.getSelectedItem();
        cbEnd.removeAllItems();
        if (start != null) {
            for (Stop s : DataStore.getNetwork().getStops()) {
                if (!s.getId().equalsIgnoreCase(start)) {
                    cbEnd.addItem(s.getId());
                }
            }
        }
        if (prevEnd != null && !prevEnd.equalsIgnoreCase(start) && contains(cbEnd, prevEnd)) {
            cbEnd.setSelectedItem(prevEnd);
        }
    }

    private void initTable() {
        tableModel = new DefaultTableModel(new Object[]{"Start","End","Time","Status"},0){
            public boolean isCellEditable(int row, int col){return false;}
        };
        roadTable = new JTable(tableModel);
        roadTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roadTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int sel = roadTable.getSelectedRow();
                if (sel != -1) {
                    String start = (String) tableModel.getValueAt(sel, 0);
                    String end = (String) tableModel.getValueAt(sel, 1);
                    Object time = tableModel.getValueAt(sel, 2);
                    Object status = tableModel.getValueAt(sel, 3);
                    if (start != null) cbStart.setSelectedItem(start);
                    updateEndCombo();
                    if (end != null) cbEnd.setSelectedItem(end);
                    if (time != null) tfTravelTime.setText(time.toString());
                    if (status instanceof RoadStatus) cbStatus.setSelectedItem(status);
                    else if (status != null) cbStatus.setSelectedItem(RoadStatus.valueOf(status.toString()));
                }
            }
        });
        JScrollPane sp = new JScrollPane(roadTable);
        sp.setPreferredSize(new Dimension(500,200));
        add(sp, BorderLayout.CENTER);
    }

    public void refreshStopCombos(){
        List<Stop> stops = DataStore.getNetwork().getStops();
        String prevStart = (String) cbStart.getSelectedItem();
        cbStart.removeAllItems();
        for (Stop s : stops) {
            cbStart.addItem(s.getId());
        }
        if (prevStart != null && contains(cbStart, prevStart)) {
            cbStart.setSelectedItem(prevStart);
        }
        updateEndCombo();
    }

    public void refreshTable(){
        refreshStopCombos();
        List<Road> roads = DataStore.getNetwork().getRoads();
        tableModel.setRowCount(0);
        for (Road r : roads) {
            tableModel.addRow(new Object[]{r.getStartStopId(), r.getEndStopId(), r.getTravelTime(), r.getStatus()});
        }
    }

    private void handleAdd(ActionEvent e){
        String start = (String)cbStart.getSelectedItem();
        String end = (String)cbEnd.getSelectedItem();
        if (start == null || end == null) {
            JOptionPane.showMessageDialog(this, "Select both start and end stops.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (start.equals(end)) {
            JOptionPane.showMessageDialog(this, "Start and end stops must differ.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int time;
        try {
            time = Integer.parseInt(tfTravelTime.getText().trim());
            if (time <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Travel time must be a positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        RoadStatus status = (RoadStatus)cbStatus.getSelectedItem();
        Road road = new Road(start, end, time, status);
        boolean ok = DataStore.getNetwork().addRoad(road);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Road already exists or invalid stops.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
    }

    private void handleUpdate(ActionEvent e){
        int sel = roadTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a road to update.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String start = (String)tableModel.getValueAt(sel, 0);
        String end = (String)tableModel.getValueAt(sel, 1);
        int newTime;
        try {
            newTime = Integer.parseInt(tfTravelTime.getText().trim());
            if (newTime <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive travel time for update.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        RoadStatus newStatus = (RoadStatus)cbStatus.getSelectedItem();
        boolean ok = DataStore.getNetwork().updateRoad(start, end, newTime, newStatus);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Failed to update road.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
    }

    private void handleRemove(ActionEvent e){
        int sel = roadTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a road to remove.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String start = (String)tableModel.getValueAt(sel, 0);
        String end = (String)tableModel.getValueAt(sel, 1);
        boolean ok = DataStore.getNetwork().removeRoad(start, end);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Failed to remove road.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
    }
}
