import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Simplified panel for decision explanation.
 * Shows route explanation controls and allocation results.
 */
public class DecisionExplanationPanel extends JPanel {
    // Route explanation components
    private JComboBox<String> cbSource, cbDest;
    private JButton btnExplainRoute, btnRunAllocation, btnClear;
    private JTextArea txtRouteResult;

    // Allocation components
    private DefaultTableModel modelAssignments;
    private JTable tblAssignments;
    private DefaultTableModel modelUnallocated;
    private JTable tblUnallocated;

    // Status label
    private JLabel lblStatus;

    public DecisionExplanationPanel() {
        setLayout(new BorderLayout(10, 10));
        initTopPanel();
        initCenterPanel();
        initBottomPanel();
        refreshStopCombos();
    }

    private void initTopPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.setBorder(BorderFactory.createTitledBorder("Route & Allocation Controls"));

        cbSource = new JComboBox<>();
        cbDest = new JComboBox<>();
        btnExplainRoute = new JButton("Explain Route");
        btnRunAllocation = new JButton("Run Bus Allocation");
        btnClear = new JButton("Clear Form");

        top.add(new JLabel("Source:"));
        top.add(cbSource);
        top.add(new JLabel("Destination:"));
        top.add(cbDest);
        top.add(btnExplainRoute);
        top.add(btnRunAllocation);
        top.add(btnClear);

        btnExplainRoute.addActionListener(this::handleExplainRoute);
        btnRunAllocation.addActionListener(this::handleRunAllocation);
        btnClear.addActionListener(e -> clearForm());
        cbSource.addActionListener(e -> updateDestCombo());

        add(top, BorderLayout.NORTH);

        txtRouteResult = new JTextArea(12, 50);
        txtRouteResult.setEditable(false);
        txtRouteResult.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(txtRouteResult), BorderLayout.WEST);
    }

    private void clearForm() {
        if (cbSource.getItemCount() > 0) cbSource.setSelectedIndex(0);
        updateDestCombo();
        txtRouteResult.setText("");
        modelAssignments.setRowCount(0);
        modelUnallocated.setRowCount(0);
        lblStatus.setText("Form cleared.");
    }

    private boolean contains(JComboBox<String> cb, String item) {
        for (int i = 0; i < cb.getItemCount(); i++) {
            if (cb.getItemAt(i).equals(item)) return true;
        }
        return false;
    }

    private void updateDestCombo() {
        String src = (String) cbSource.getSelectedItem();
        String prevDst = (String) cbDest.getSelectedItem();
        cbDest.removeAllItems();
        if (src != null) {
            for (Stop s : DataStore.getNetwork().getStops()) {
                if (!s.getId().equalsIgnoreCase(src)) {
                    cbDest.addItem(s.getId());
                }
            }
        }
        if (prevDst != null && !prevDst.equalsIgnoreCase(src) && contains(cbDest, prevDst)) {
            cbDest.setSelectedItem(prevDst);
        }
    }

    private void initCenterPanel() {
        modelAssignments = new DefaultTableModel(new String[]{"Request / Journey", "Bus ID", "Req Size", "Capacity", "Remaining Seats", "Travel Time", "Route"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblAssignments = new JTable(modelAssignments);
        modelUnallocated = new DefaultTableModel(new String[]{"Request / Journey", "Reason"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tblUnallocated = new JTable(modelUnallocated);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tblAssignments), new JScrollPane(tblUnallocated));
        split.setResizeWeight(0.55);
        split.setBorder(BorderFactory.createTitledBorder("Allocation Results"));
        add(split, BorderLayout.CENTER);
    }

    private void initBottomPanel() {
        lblStatus = new JLabel(" ");
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(lblStatus, BorderLayout.SOUTH);
    }

    private void handleExplainRoute(ActionEvent e) {
        try {
            String src = (String) cbSource.getSelectedItem();
            String dst = (String) cbDest.getSelectedItem();
            if (src == null || dst == null) {
                lblStatus.setText("Select both source and destination stops.");
                JOptionPane.showMessageDialog(this, "Select both source and destination stops.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (src.equalsIgnoreCase(dst)) {
                lblStatus.setText("Source and destination must differ.");
                JOptionPane.showMessageDialog(this, "Source and destination must differ.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=========================================\n");
            sb.append("        ROUTE & BUS DECISION EXPLANATION\n");
            sb.append("=========================================\n\n");
            sb.append("Journey: ").append(src).append(" ➔ ").append(dst).append("\n\n");

            // 1. Dijkstra Path
            RouteResult result = DataStore.getNetwork().findShortestPath(src, dst);
            if (result == null) {
                sb.append("[ROUTE ANALYSIS: UNREACHABLE]\n");
                sb.append("No active path connects ").append(src).append(" and ").append(dst).append(".\n");
                sb.append("Connecting road segments are CLOSED or missing.\n\n");
                sb.append("[BUS ALLOCATION: NOT ALLOCATED]\n");
                sb.append("Reason: Connecting road segments are blocked or closed.\n");
                txtRouteResult.setText(sb.toString());
                lblStatus.setText("No reachable route between " + src + " and " + dst + ".");
                return;
            }

            sb.append("[DIJKSTRA SHORTEST PATH ANALYSIS]\n");
            int step = 1;
            for (Road r : result.getPath()) {
                sb.append("  ").append(step++).append(". ")
                  .append(r.getStartStopId()).append(" ➔ ").append(r.getEndStopId())
                  .append(" (").append(r.getTravelTime()).append(" mins, Status: ")
                  .append(r.getStatus()).append(")\n");
            }
            sb.append("Total Travel Time: ").append(result.getTotalTime()).append(" minutes\n");
            sb.append("Selection Logic: Dijkstra algorithm selected minimum-weight path avoiding CLOSED roads.\n\n");

            // 2. Bus Allocation Analysis
            AllocationResult alloc = DataStore.getNetwork().evaluateJourneyAllocation(src, dst, 1);
            if (alloc.isAllocated()) {
                Bus b = alloc.getBus();
                sb.append("[BUS ALLOCATION STATUS: ALLOCATED]\n");
                sb.append("  • Assigned Bus: ").append(b.getId()).append(" (").append(b.getType()).append(")\n");
                sb.append("  • Bus Total Capacity: ").append(alloc.getBusCapacity()).append(" seats\n");
                sb.append("  • Available Seats: ").append(alloc.getRemainingSeats()).append(" seats\n");
                sb.append("  • Operational Status: Operational & Available\n");
                sb.append("  • Route Coverage: Verified forward corridor coverage\n");
                lblStatus.setText("Route & Bus explanation complete: Bus " + b.getId() + " is available and allocated.");
            } else {
                sb.append("[BUS ALLOCATION STATUS: NOT ALLOCATED]\n");
                sb.append("  • Failure Reason: ").append(alloc.getFailureReason()).append("\n");
                sb.append("  • Explanation: ").append(alloc.getExplanation()).append("\n");
                lblStatus.setText("Route explained. Bus allocation rejected: " + alloc.getFailureReason());
            }

            txtRouteResult.setText(sb.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            lblStatus.setText("Error during route explanation: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error during route explanation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRunAllocation(ActionEvent e) {
        try {
            List<AllocationRecord> allocations = DataStore.getNetwork().allocateBuses();
            modelAssignments.setRowCount(0);
            modelUnallocated.setRowCount(0);
            int assigned = 0;

            for (AllocationRecord ar : allocations) {
                if (ar.getBusId() != null) {
                    assigned++;
                    String routeStr = "";
                    if (ar.getRoute() != null && !ar.getRoute().isEmpty()) {
                        java.util.List<String> parts = new java.util.ArrayList<>();
                        for (Road r : ar.getRoute()) {
                            parts.add(r.getStartStopId() + "->" + r.getEndStopId());
                        }
                        routeStr = String.join(", ", parts);
                    }
                    modelAssignments.addRow(new Object[]{
                        ar.getRequestId(),
                        ar.getBusId(),
                        ar.getPassengerCount(),
                        ar.getBusCapacity(),
                        ar.getRemainingSeats(),
                        ar.getTravelTime() + " min",
                        routeStr
                    });
                } else {
                    modelUnallocated.addRow(new Object[]{ar.getRequestId(), ar.getReason()});
                }
            }

            // Also evaluate currently selected source and destination journey on-demand
            String src = (String) cbSource.getSelectedItem();
            String dst = (String) cbDest.getSelectedItem();
            if (src != null && dst != null && !src.equalsIgnoreCase(dst)) {
                AllocationResult alloc = DataStore.getNetwork().evaluateJourneyAllocation(src, dst, 1);
                String journeyLabel = "Selected (" + src + " -> " + dst + ")";
                if (alloc.isAllocated()) {
                    Bus b = alloc.getBus();
                    String routeStr = "";
                    if (alloc.getRoute() != null && !alloc.getRoute().isEmpty()) {
                        java.util.List<String> parts = new java.util.ArrayList<>();
                        for (Road r : alloc.getRoute()) {
                            parts.add(r.getStartStopId() + "->" + r.getEndStopId());
                        }
                        routeStr = String.join(", ", parts);
                    }
                    modelAssignments.addRow(new Object[]{
                        journeyLabel,
                        b.getId(),
                        1,
                        alloc.getBusCapacity(),
                        alloc.getRemainingSeats(),
                        alloc.getTravelTime() + " min",
                        routeStr
                    });
                    assigned++;
                } else {
                    modelUnallocated.addRow(new Object[]{journeyLabel, alloc.getFailureReason()});
                }
                handleExplainRoute(null);
            }

            int unallocated = modelUnallocated.getRowCount();
            lblStatus.setText("Bus Allocation Completed: " + assigned + " allocated, " + unallocated + " unallocated.");
        } catch (Exception ex) {
            ex.printStackTrace();
            lblStatus.setText("Error during allocation: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error during allocation: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Refresh stop combo boxes – called by MainFrame when data changes */
    public void refreshStopCombos() {
        List<Stop> stops = DataStore.getNetwork().getStops();
        String prevSrc = (String) cbSource.getSelectedItem();
        if (cbSource != null) {
            cbSource.removeAllItems();
            for (Stop s : stops) {
                cbSource.addItem(s.getId());
            }
            if (prevSrc != null && contains(cbSource, prevSrc)) {
                cbSource.setSelectedItem(prevSrc);
            }
            updateDestCombo();
        }
    }
}
