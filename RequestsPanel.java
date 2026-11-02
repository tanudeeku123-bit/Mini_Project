import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RequestsPanel extends JPanel {
    private static final Color NAVY_DARK = new Color(10, 30, 70);
    private static final Color NAVY_HOVER = new Color(15, 45, 100);

    private JComboBox<String> cbSource, cbDest;
    private JTextField tfCount, tfUrgency, tfWaiting;
    private JButton btnAdd, btnClear, btnAccept, btnCancel, btnRemove, btnRefresh;
    private JComboBox<String> cbStatusFilter;
    private JTable requestTable;
    private DefaultTableModel tableModel;
    private static int requestCounter = 1;

    public RequestsPanel() {
        setLayout(new BorderLayout(10, 10));
        initForm();
        initTable();
        refreshTable();
    }

    private void initForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 215, 235), 1),
                " Passenger Request Management & Booking ",
                0, 0, new Font("SansSerif", Font.BOLD, 14), NAVY_DARK
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0;

        form.add(new JLabel("Source Stop:"), gbc);
        gbc.gridx = 1;
        cbSource = new JComboBox<>();
        form.add(cbSource, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Destination Stop:"), gbc);
        gbc.gridx = 1;
        cbDest = new JComboBox<>();
        form.add(cbDest, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Passengers:"), gbc);
        gbc.gridx = 1;
        tfCount = new JTextField(5);
        form.add(tfCount, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Urgency (Low/Med/High):"), gbc);
        gbc.gridx = 1;
        tfUrgency = new JTextField(10);
        tfUrgency.setText("Medium");
        form.add(tfUrgency, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Waiting Time (min):"), gbc);
        gbc.gridx = 1;
        tfWaiting = new JTextField(5);
        tfWaiting.setText("10");
        form.add(tfWaiting, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        btnAdd = new JButton("Add Request (Pending)");
        styleButton(btnAdd, NAVY_DARK);

        btnAccept = new JButton("✔ Accept Request");
        styleButton(btnAccept, new Color(20, 110, 50));

        btnCancel = new JButton("✖ Cancel Request");
        styleButton(btnCancel, new Color(160, 40, 40));

        btnRemove = new JButton("Delete Request");
        styleButton(btnRemove, new Color(100, 100, 100));

        btnClear = new JButton("Clear Form");

        btnPanel.add(btnAdd);
        btnPanel.add(btnAccept);
        btnPanel.add(btnCancel);
        btnPanel.add(btnRemove);
        btnPanel.add(btnClear);
        form.add(btnPanel, gbc);

        add(form, BorderLayout.NORTH);

        // Listeners
        btnAdd.addActionListener(this::handleAdd);
        btnAccept.addActionListener(this::handleAccept);
        btnCancel.addActionListener(this::handleCancel);
        btnRemove.addActionListener(this::handleRemove);
        btnClear.addActionListener(e -> clearForm());

        cbSource.addActionListener(e -> updateDestinationsDropdown());
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
    }

    private void initTable() {
        JPanel tableContainer = new JPanel(new BorderLayout(5, 5));
        tableContainer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 215, 235), 1),
                " Passenger Requests List ",
                0, 0, new Font("SansSerif", Font.BOLD, 14), NAVY_DARK
        ));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterBar.add(new JLabel("Filter by Status:"));
        cbStatusFilter = new JComboBox<>(new String[]{"All Statuses", "Pending", "Accepted", "Cancelled", "Completed"});
        cbStatusFilter.addActionListener(e -> refreshTable());
        filterBar.add(cbStatusFilter);

        btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> refreshTable());
        filterBar.add(btnRefresh);

        tableContainer.add(filterBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Source", "Dest", "Count", "Urgency", "Waiting", "Status"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        requestTable = new JTable(tableModel);
        requestTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        requestTable.setRowHeight(24);

        // Status column colored cell renderer
        requestTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setHorizontalAlignment(SwingConstants.CENTER);
                c.setFont(new Font("SansSerif", Font.BOLD, 12));
                String st = value != null ? value.toString() : "";
                if ("Pending".equalsIgnoreCase(st)) {
                    c.setForeground(new Color(180, 100, 0));
                } else if ("Accepted".equalsIgnoreCase(st) || "Assigned".equalsIgnoreCase(st)) {
                    c.setForeground(new Color(20, 120, 40));
                } else if ("Cancelled".equalsIgnoreCase(st)) {
                    c.setForeground(new Color(180, 30, 30));
                } else if ("Completed".equalsIgnoreCase(st)) {
                    c.setForeground(new Color(30, 80, 160));
                } else {
                    c.setForeground(Color.DARK_GRAY);
                }
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(requestTable);
        sp.setPreferredSize(new Dimension(650, 220));
        tableContainer.add(sp, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    private void clearForm() {
        if (cbSource.getItemCount() > 0) cbSource.setSelectedIndex(0);
        updateDestinationsDropdown();
        tfCount.setText("");
        tfUrgency.setText("Medium");
        tfWaiting.setText("10");
        if (requestTable != null) {
            requestTable.clearSelection();
        }
    }

    public void refreshStopCombos() {
        List<Stop> stops = DataStore.getNetwork().getStops();
        String prevSrc = (String) cbSource.getSelectedItem();

        cbSource.removeAllItems();
        for (Stop s : stops) {
            cbSource.addItem(s.getId());
        }

        if (prevSrc != null && containsItem(cbSource, prevSrc)) {
            cbSource.setSelectedItem(prevSrc);
        } else if (cbSource.getItemCount() > 0) {
            cbSource.setSelectedIndex(0);
        }
        updateDestinationsDropdown();
    }

    private boolean containsItem(JComboBox<String> cb, String item) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).equals(item)) return true;
        }
        return false;
    }

    /**
     * Enforces the rule: only allow destinations that occur strictly AFTER the boarding stop.
     */
    private void updateDestinationsDropdown() {
        String src = (String) cbSource.getSelectedItem();
        cbDest.removeAllItems();
        if (src == null) return;

        List<Stop> stops = DataStore.getNetwork().getStops();
        int srcOrder = -1;
        for (Stop s : stops) {
            if (s.getId().equals(src)) {
                srcOrder = s.getSequenceOrder();
                break;
            }
        }

        for (Stop s : stops) {
            if (s.getSequenceOrder() > srcOrder) {
                cbDest.addItem(s.getId());
            }
        }
    }

    public void refreshTable() {
        refreshStopCombos();
        List<PassengerRequest> reqs = DataStore.getNetwork().getRequests();
        String filter = (String) cbStatusFilter.getSelectedItem();
        if (filter == null) filter = "All Statuses";

        tableModel.setRowCount(0);
        for (PassengerRequest r : reqs) {
            if (!"All Statuses".equals(filter)) {
                if ("Accepted".equalsIgnoreCase(filter)) {
                    if (!"Accepted".equalsIgnoreCase(r.getStatus()) && !"Assigned".equalsIgnoreCase(r.getStatus())) {
                        continue;
                    }
                } else if (!filter.equalsIgnoreCase(r.getStatus())) {
                    continue;
                }
            }

            tableModel.addRow(new Object[]{
                    r.getId(),
                    r.getSourceStopId(),
                    r.getDestinationStopId(),
                    r.getPassengerCount(),
                    r.getUrgencyLevel(),
                    r.getWaitingTimeMinutes(),
                    r.getStatus()
            });

            try {
                if (r.getId() != null && r.getId().startsWith("R")) {
                    int num = Integer.parseInt(r.getId().substring(1));
                    if (num >= requestCounter) {
                        requestCounter = num + 1;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void handleAdd(ActionEvent e) {
        String source = (String) cbSource.getSelectedItem();
        String dest = (String) cbDest.getSelectedItem();
        if (source == null || dest == null) {
            JOptionPane.showMessageDialog(this,
                    "Select both source and valid destination stops.\nNote: Destinations must occur after the boarding stop.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int count;
        try {
            count = Integer.parseInt(tfCount.getText().trim());
            if (count <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Passenger count must be a positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String urgency = tfUrgency.getText().trim();
        int waiting;
        try {
            waiting = Integer.parseInt(tfWaiting.getText().trim());
            if (waiting < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Waiting time must be a non‑negative integer.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String id = "R" + (requestCounter++);
        PassengerRequest request = new PassengerRequest(id, source, dest, count, urgency, waiting);
        request.setStatus("Pending"); // Pending requests do NOT take seats until accepted

        boolean ok = DataStore.getNetwork().addRequest(request);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Failed to add request (duplicate ID?).", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
        JOptionPane.showMessageDialog(this,
                "Request " + id + " registered with status 'Pending'.\nClick 'Accept Request' to allocate seats on the bus.",
                "Request Registered", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleAccept(ActionEvent e) {
        int sel = requestTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a pending request to accept.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String reqId = (String) tableModel.getValueAt(sel, 0);
        String status = (String) tableModel.getValueAt(sel, 6);

        if (!"Pending".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only Pending requests can be accepted.\nCurrent status: " + status,
                    "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean accepted = TripSeatTracker.acceptRequest("TRIP-101", reqId);
            if (accepted) {
                // Update network in-memory model
                DataStore.getNetwork().updateRequestStatus(reqId, "Accepted");
                refreshTable();
                MainFrame.getInstance().refreshDashboard();
                JOptionPane.showMessageDialog(this,
                        "Request " + reqId + " accepted successfully!\nSeats have been reserved across all affected corridor segments.",
                        "Request Accepted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cannot accept request " + reqId + ": Insufficient available seats on the requested route segments.\n" +
                                "Accepting this request would exceed bus capacity (overbooking).",
                        "Insufficient Capacity", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error accepting request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancel(ActionEvent e) {
        int sel = requestTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a request to cancel.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String reqId = (String) tableModel.getValueAt(sel, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel request " + reqId + "?\nIf previously accepted, its reserved seats will be released.",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean cancelled = TripSeatTracker.cancelRequest("TRIP-101", reqId);
            if (cancelled) {
                DataStore.getNetwork().updateRequestStatus(reqId, "Cancelled");
                refreshTable();
                MainFrame.getInstance().refreshDashboard();
                JOptionPane.showMessageDialog(this, "Request " + reqId + " cancelled and seats released.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to cancel request.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cancelling request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRemove(ActionEvent e) {
        int sel = requestTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a request to remove.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String reqId = (String) tableModel.getValueAt(sel, 0);
        String status = (String) tableModel.getValueAt(sel, 6);

        if ("Pending".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
            DataStore.getNetwork().removeRequest(reqId);
            refreshTable();
            MainFrame.getInstance().refreshDashboard();
            JOptionPane.showMessageDialog(this, "Request " + reqId + " deleted from database.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Only Pending or Cancelled requests can be deleted.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
