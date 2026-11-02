import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardPanel extends JPanel {
    private static final Color NAVY_DARK = new Color(10, 30, 70);
    private static final Color LIGHT_BLUE = new Color(230, 240, 255);
    private static final Color ALERT_RED = new Color(255, 235, 235);
    private static final Color ALERT_GREEN = new Color(235, 255, 235);
    private static final Color ALERT_ORANGE = new Color(255, 245, 230);

    // Existing count labels
    private JLabel lblStops, lblRoads, lblBuses, lblAvailableBuses, lblRequests, lblPending;
    private JButton btnAddStop, btnAddRoad, btnAddBus, btnAddRequest;
    private JTable recentActivityTable;
    private DefaultTableModel activityModel;

    // Corridor Dynamic Passenger & Seat Flow Components
    private JPanel pnlCorridorStopsGrid;
    private JLabel[] lblStopNames = new JLabel[4];
    private JLabel[] lblStopBoarding = new JLabel[4];
    private JLabel[] lblStopAlighting = new JLabel[4];
    private JLabel[] lblStopOccupied = new JLabel[4];
    private JLabel[] lblStopAvailable = new JLabel[4];
    private JProgressBar[] barStopSeats = new JProgressBar[4];
    private JButton btnRunDemoFlow, btnCompleteTrip, btnResetTrip, btnCustomBoarding;

    // Route Optimization & Corridor Monitor Components
    private JComboBox<String> cbRouteSource;
    private JComboBox<String> cbRouteDest;
    private JButton btnCheckRoute;

    private JLabel lblRouteStatusBadge;
    private JLabel lblRoutePath;
    private JLabel lblEstimatedTime;
    private JLabel lblBusInfo;
    private JLabel lblCapacityStats;
    private JTextArea txtBlockedRoads;

    public DashboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        initStatsPanel();
        initCenterDashboard();
        initBottomPanel();

        refreshStats();
    }

    private void initStatsPanel() {
        JPanel stats = new JPanel(new GridLayout(2, 3, 10, 10));
        stats.setBackground(Color.WHITE);

        lblStops = createStatLabel("Stops: 0");
        lblRoads = createStatLabel("Roads: 0");
        lblBuses = createStatLabel("Buses: 0");
        lblAvailableBuses = createStatLabel("Available Buses: 0");
        lblRequests = createStatLabel("Requests: 0");
        lblPending = createStatLabel("Pending: 0");

        stats.add(lblStops);
        stats.add(lblRoads);
        stats.add(lblBuses);
        stats.add(lblAvailableBuses);
        stats.add(lblRequests);
        stats.add(lblPending);

        add(stats, BorderLayout.NORTH);
    }

    private JLabel createStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setOpaque(true);
        l.setBackground(LIGHT_BLUE);
        l.setForeground(NAVY_DARK);
        l.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        return l;
    }

    private void initCenterDashboard() {
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setBackground(Color.WHITE);

        // 1. Dynamic Corridor Passenger & Seat Tracker Panel
        initCorridorFlowPanel(centerContainer);

        centerContainer.add(Box.createRigidArea(new Dimension(0, 10)));

        // 2. Dijkstra Route Optimization & Road Disruption Monitor
        initRouteMonitorPanel(centerContainer);

        centerContainer.add(Box.createRigidArea(new Dimension(0, 10)));

        // 3. Quick Actions Row
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        actions.setBackground(Color.WHITE);
        btnAddStop = new JButton("Add Stop");
        btnAddRoad = new JButton("Add Road");
        btnAddBus = new JButton("Add Bus");
        btnAddRequest = new JButton("Add Request");

        btnAddStop.addActionListener(e -> MainFrame.getInstance().showCard("Bus Stops"));
        btnAddRoad.addActionListener(e -> MainFrame.getInstance().showCard("Road Network"));
        btnAddBus.addActionListener(e -> MainFrame.getInstance().showCard("Bus Fleet"));
        btnAddRequest.addActionListener(e -> MainFrame.getInstance().showCard("Passenger Requests"));

        actions.add(new JLabel("Quick Navigation:"));
        actions.add(btnAddStop);
        actions.add(btnAddRoad);
        actions.add(btnAddBus);
        actions.add(btnAddRequest);

        centerContainer.add(actions);

        JScrollPane centerScroll = new JScrollPane(centerContainer);
        centerScroll.setBorder(null);
        centerScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(centerScroll, BorderLayout.CENTER);
    }

    private void initCorridorFlowPanel(JPanel container) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 205, 235), 1),
                " Corridor Dynamic Passenger & Seat Flow (TRIP-101 | BUS-101 | Cap: 40) ",
                0, 0, new Font("SansSerif", Font.BOLD, 15), NAVY_DARK
        ));

        // Subheader with formula
        JPanel formulaPanel = new JPanel(new BorderLayout(5, 5));
        formulaPanel.setBackground(new Color(245, 248, 255));
        formulaPanel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JLabel lblFormula = new JLabel("<html><b>Seat Calculation Formula:</b> " +
                "<font color='#004080'>available seats = previous available seats + passengers getting down - passengers boarding</font></html>");
        lblFormula.setFont(new Font("SansSerif", Font.PLAIN, 13));
        formulaPanel.add(lblFormula, BorderLayout.WEST);

        // Simulation Toolbar Buttons
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        toolbar.setBackground(new Color(245, 248, 255));

        btnRunDemoFlow = new JButton("▶ Run Demo Flow (40➔35➔27➔30➔40)");
        styleActionButton(btnRunDemoFlow, new Color(20, 110, 50));
        btnRunDemoFlow.addActionListener(this::handleRunDemoFlow);

        btnCompleteTrip = new JButton("✔ Complete Trip (All Alight)");
        styleActionButton(btnCompleteTrip, new Color(30, 80, 160));
        btnCompleteTrip.addActionListener(this::handleCompleteTrip);

        btnResetTrip = new JButton("↺ Reset Trip (40 Seats)");
        styleActionButton(btnResetTrip, new Color(110, 110, 110));
        btnResetTrip.addActionListener(this::handleResetTrip);

        btnCustomBoarding = new JButton("✎ Custom Boarding...");
        styleActionButton(btnCustomBoarding, NAVY_DARK);
        btnCustomBoarding.addActionListener(this::handleCustomBoarding);

        toolbar.add(btnRunDemoFlow);
        toolbar.add(btnCompleteTrip);
        toolbar.add(btnResetTrip);
        toolbar.add(btnCustomBoarding);
        formulaPanel.add(toolbar, BorderLayout.EAST);

        panel.add(formulaPanel, BorderLayout.NORTH);

        // 4 Stops Visual Flow Grid
        pnlCorridorStopsGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        pnlCorridorStopsGrid.setBackground(Color.WHITE);
        pnlCorridorStopsGrid.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] stopNames = {"Erode (Stop #1)", "Nandha College (Stop #2)", "Perundurai (Stop #3)", "Kongu College (Stop #4 - End)"};

        for (int i = 0; i < 4; i++) {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBackground(new Color(250, 252, 255));
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(210, 225, 245), 1, true),
                    new EmptyBorder(10, 12, 10, 12)
            ));

            lblStopNames[i] = new JLabel(stopNames[i]);
            lblStopNames[i].setFont(new Font("SansSerif", Font.BOLD, 14));
            lblStopNames[i].setForeground(NAVY_DARK);

            lblStopBoarding[i] = new JLabel("Boarding: 0");
            lblStopBoarding[i].setFont(new Font("SansSerif", Font.PLAIN, 12));

            lblStopAlighting[i] = new JLabel("Alighting: 0");
            lblStopAlighting[i].setFont(new Font("SansSerif", Font.PLAIN, 12));

            lblStopOccupied[i] = new JLabel("On Board: 0");
            lblStopOccupied[i].setFont(new Font("SansSerif", Font.PLAIN, 12));

            lblStopAvailable[i] = new JLabel("Available Seats: 40 / 40");
            lblStopAvailable[i].setFont(new Font("SansSerif", Font.BOLD, 13));
            lblStopAvailable[i].setForeground(new Color(20, 120, 40));

            barStopSeats[i] = new JProgressBar(0, 40);
            barStopSeats[i].setValue(40);
            barStopSeats[i].setStringPainted(true);
            barStopSeats[i].setString("40 seats");
            barStopSeats[i].setForeground(new Color(20, 140, 50));
            barStopSeats[i].setPreferredSize(new Dimension(160, 20));

            card.add(lblStopNames[i]);
            card.add(Box.createRigidArea(new Dimension(0, 6)));
            card.add(lblStopBoarding[i]);
            card.add(Box.createRigidArea(new Dimension(0, 3)));
            card.add(lblStopAlighting[i]);
            card.add(Box.createRigidArea(new Dimension(0, 3)));
            card.add(lblStopOccupied[i]);
            card.add(Box.createRigidArea(new Dimension(0, 5)));
            card.add(lblStopAvailable[i]);
            card.add(Box.createRigidArea(new Dimension(0, 5)));
            card.add(barStopSeats[i]);

            pnlCorridorStopsGrid.add(card);
        }

        panel.add(pnlCorridorStopsGrid, BorderLayout.CENTER);
        container.add(panel);
    }

    private void styleActionButton(JButton btn, Color bg) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
    }

    private void initRouteMonitorPanel(JPanel container) {
        JPanel corridorMonitor = new JPanel(new BorderLayout(8, 8));
        corridorMonitor.setBackground(Color.WHITE);
        corridorMonitor.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 205, 235), 1),
                " Live Dijkstra Route Optimization & Network Disruption Monitor ",
                0, 0, new Font("SansSerif", Font.BOLD, 15), NAVY_DARK
        ));

        // Selector Bar
        JPanel selectorBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        selectorBar.setBackground(new Color(245, 248, 255));

        selectorBar.add(new JLabel("Departure Stop:"));
        cbRouteSource = new JComboBox<>();
        selectorBar.add(cbRouteSource);

        selectorBar.add(new JLabel("Destination Stop:"));
        cbRouteDest = new JComboBox<>();
        selectorBar.add(cbRouteDest);

        cbRouteSource.addActionListener(e -> updateRouteDestCombo());

        btnCheckRoute = new JButton("Calculate Best Route");
        btnCheckRoute.setBackground(NAVY_DARK);
        btnCheckRoute.setForeground(Color.WHITE);
        btnCheckRoute.setFocusPainted(false);
        btnCheckRoute.addActionListener(e -> updateRouteDisplay());
        selectorBar.add(btnCheckRoute);

        corridorMonitor.add(selectorBar, BorderLayout.NORTH);

        // Details Panel (Grid of 2 Cards: Route & Blockages)
        JPanel detailsGrid = new JPanel(new GridLayout(1, 2, 10, 0));
        detailsGrid.setBackground(Color.WHITE);

        // Card 1: Route Path, Travel Time, Bus Details, Seat Availability
        JPanel routeCard = new JPanel();
        routeCard.setLayout(new BoxLayout(routeCard, BoxLayout.Y_AXIS));
        routeCard.setBackground(Color.WHITE);
        routeCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 220, 245), 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        lblRouteStatusBadge = new JLabel("● ROUTE STATUS: CHECKING...");
        lblRouteStatusBadge.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblRouteStatusBadge.setOpaque(true);
        lblRouteStatusBadge.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        lblRoutePath = new JLabel("Path: Erode ➔ Nandha College ➔ Perundurai ➔ Kongu College");
        lblRoutePath.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblRoutePath.setForeground(NAVY_DARK);

        lblEstimatedTime = new JLabel("Estimated Travel Time: 35 minutes");
        lblEstimatedTime.setFont(new Font("SansSerif", Font.PLAIN, 13));

        lblBusInfo = new JLabel("Assigned Bus: BUS-101 (Standard) | Status: Operational");
        lblBusInfo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        lblCapacityStats = new JLabel("Capacity: 40 | Reserved: 0 seats | Available: 40 seats");
        lblCapacityStats.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblCapacityStats.setForeground(new Color(20, 90, 40));

        routeCard.add(lblRouteStatusBadge);
        routeCard.add(Box.createRigidArea(new Dimension(0, 8)));
        routeCard.add(lblRoutePath);
        routeCard.add(Box.createRigidArea(new Dimension(0, 6)));
        routeCard.add(lblEstimatedTime);
        routeCard.add(Box.createRigidArea(new Dimension(0, 6)));
        routeCard.add(lblBusInfo);
        routeCard.add(Box.createRigidArea(new Dimension(0, 6)));
        routeCard.add(lblCapacityStats);

        // Card 2: Blocked Roads & Disruption Alerts
        JPanel blockageCard = new JPanel(new BorderLayout(5, 5));
        blockageCard.setBackground(Color.WHITE);
        blockageCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 200, 200), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblBlockageTitle = new JLabel("Road Network Status & Disruptions");
        lblBlockageTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblBlockageTitle.setForeground(new Color(150, 30, 30));

        txtBlockedRoads = new JTextArea(5, 25);
        txtBlockedRoads.setEditable(false);
        txtBlockedRoads.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtBlockedRoads.setBackground(new Color(255, 250, 250));
        JScrollPane spBlockage = new JScrollPane(txtBlockedRoads);

        blockageCard.add(lblBlockageTitle, BorderLayout.NORTH);
        blockageCard.add(spBlockage, BorderLayout.CENTER);

        detailsGrid.add(routeCard);
        detailsGrid.add(blockageCard);
        corridorMonitor.add(detailsGrid, BorderLayout.CENTER);

        container.add(corridorMonitor);
    }

    private void initBottomPanel() {
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        bottom.setBackground(Color.WHITE);

        JLabel lblActivity = new JLabel("Recent System Activity & Disruption Log:");
        lblActivity.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblActivity.setForeground(NAVY_DARK);
        bottom.add(lblActivity, BorderLayout.NORTH);

        activityModel = new DefaultTableModel(new Object[]{"Time", "Type", "Target", "Action", "Result"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        recentActivityTable = new JTable(activityModel);
        recentActivityTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
        recentActivityTable.setRowHeight(20);
        JScrollPane sp = new JScrollPane(recentActivityTable);
        sp.setPreferredSize(new Dimension(400, 110));
        bottom.add(sp, BorderLayout.CENTER);

        JLabel msg = new JLabel("Smart Public Transportation System | Erode ➔ Nandha College ➔ Perundurai ➔ Kongu College Corridor Active", SwingConstants.CENTER);
        msg.setFont(new Font("SansSerif", Font.ITALIC, 12));
        msg.setForeground(new Color(100, 115, 135));
        bottom.add(msg, BorderLayout.SOUTH);

        add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Refreshes stats, stop comboboxes, live corridor seat flow, live route calculation, and activity log.
     */
    public void refreshStats() {
        TransportNetwork net = DataStore.getNetwork();
        lblStops.setText("Stops: " + net.getStops().size());
        lblRoads.setText("Roads: " + net.getRoads().size());
        lblBuses.setText("Buses: " + net.getBuses().size());

        long available = net.getBuses().stream().filter(Bus::isAvailable).count();
        lblAvailableBuses.setText("Available Buses: " + available);
        lblRequests.setText("Requests: " + net.getRequests().size());

        long pending = net.getRequests().stream().filter(r -> "Pending".equalsIgnoreCase(r.getStatus())).count();
        lblPending.setText("Pending: " + pending);

        // Refresh corridor stops flow cards
        refreshCorridorFlow();

        // Update stop combos
        String currentSrc = (String) cbRouteSource.getSelectedItem();
        String currentDst = (String) cbRouteDest.getSelectedItem();

        cbRouteSource.removeAllItems();
        for (Stop s : net.getStops()) {
            cbRouteSource.addItem(s.getId());
        }

        if (currentSrc != null && net.getStop(currentSrc) != null) {
            cbRouteSource.setSelectedItem(currentSrc);
        } else if (net.getStop("Erode") != null) {
            cbRouteSource.setSelectedItem("Erode");
        }

        updateRouteDestCombo();

        if (currentDst != null && !currentDst.equals(cbRouteSource.getSelectedItem()) && net.getStop(currentDst) != null) {
            cbRouteDest.setSelectedItem(currentDst);
        } else if (cbRouteDest.getItemCount() > 0) {
            cbRouteDest.setSelectedIndex(cbRouteDest.getItemCount() - 1);
        }

        updateRouteDisplay();
        loadRecentActivity();
    }

    private void updateRouteDestCombo() {
        if (cbRouteSource == null || cbRouteDest == null) return;
        String src = (String) cbRouteSource.getSelectedItem();
        String prevDst = (String) cbRouteDest.getSelectedItem();
        cbRouteDest.removeAllItems();
        if (src != null) {
            for (Stop s : DataStore.getNetwork().getStops()) {
                if (!s.getId().equalsIgnoreCase(src)) {
                    cbRouteDest.addItem(s.getId());
                }
            }
        }
        if (prevDst != null && !prevDst.equalsIgnoreCase(src)) {
            for (int i = 0; i < cbRouteDest.getItemCount(); i++) {
                if (cbRouteDest.getItemAt(i).equalsIgnoreCase(prevDst)) {
                    cbRouteDest.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshCorridorFlow() {
        try {
            List<TripSeatTracker.SegmentStatus> statuses = TripSeatTracker.getSegmentStatuses("TRIP-101");
            for (int i = 0; i < statuses.size() && i < 4; i++) {
                TripSeatTracker.SegmentStatus s = statuses.get(i);
                lblStopBoarding[i].setText("Boarding: " + s.boarding);
                lblStopAlighting[i].setText("Alighting: " + s.alighting);
                lblStopOccupied[i].setText("On Board: " + s.occupied);
                lblStopAvailable[i].setText("Available: " + s.available + " / " + s.capacity);

                barStopSeats[i].setMaximum(s.capacity);
                barStopSeats[i].setValue(s.available);
                barStopSeats[i].setString(s.available + " seats");

                if (s.available > 20) {
                    lblStopAvailable[i].setForeground(new Color(20, 120, 40));
                    barStopSeats[i].setForeground(new Color(20, 140, 50));
                } else if (s.available >= 10) {
                    lblStopAvailable[i].setForeground(new Color(190, 110, 0));
                    barStopSeats[i].setForeground(new Color(200, 120, 0));
                } else {
                    lblStopAvailable[i].setForeground(new Color(190, 20, 20));
                    barStopSeats[i].setForeground(new Color(190, 20, 20));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error refreshing corridor flow: " + ex.getMessage());
        }
    }

    private void updateRouteDisplay() {
        TransportNetwork net = DataStore.getNetwork();
        String src = (String) cbRouteSource.getSelectedItem();
        String dst = (String) cbRouteDest.getSelectedItem();

        // 1. Identify Closed / Blocked Roads
        List<Road> closedRoads = new ArrayList<>();
        for (Road r : net.getRoads()) {
            if (r.getStatus() == RoadStatus.CLOSED) {
                closedRoads.add(r);
            }
        }

        StringBuilder sbBlocked = new StringBuilder();
        if (closedRoads.isEmpty()) {
            sbBlocked.append("✓ All corridor road segments are OPEN.\nTraffic flowing normally.");
        } else {
            sbBlocked.append("⛔ BLOCKED / CLOSED ROAD SEGMENTS:\n");
            for (Road cr : closedRoads) {
                sbBlocked.append(" • ")
                        .append(cr.getStartStopId()).append(" ➔ ").append(cr.getEndStopId())
                        .append(" (Status: CLOSED)\n");
            }
        }
        txtBlockedRoads.setText(sbBlocked.toString());

        if (src == null || dst == null || src.equals(dst)) {
            lblRouteStatusBadge.setText("● SELECT DISTINCT SOURCE & DESTINATION");
            lblRouteStatusBadge.setBackground(ALERT_ORANGE);
            lblRouteStatusBadge.setForeground(new Color(150, 90, 10));
            lblRoutePath.setText("Path: -");
            lblEstimatedTime.setText("Estimated Travel Time: -");
            return;
        }

        // 2. Compute Dijkstra Shortest Path using currently open roads
        RouteResult result = net.findShortestPath(src, dst);

        if (result == null) {
            lblRouteStatusBadge.setText("● NO AVAILABLE ROUTE - CONNECTING ROADS BLOCKED");
            lblRouteStatusBadge.setBackground(ALERT_RED);
            lblRouteStatusBadge.setForeground(new Color(180, 20, 20));

            lblRoutePath.setText("Path: Unreachable. All connecting paths between " + src + " and " + dst + " are closed.");
            lblEstimatedTime.setText("Estimated Travel Time: N/A (Route Disrupted)");
            lblCapacityStats.setText("Capacity: - | Reserved: - | Available: -");
            return;
        }

        // Route found! Check if an alternative detour is being used
        boolean detourActive = false;
        for (Road r : closedRoads) {
            if (r.getStartStopId().equals(src) || r.getEndStopId().equals(dst) ||
                    r.getStartStopId().equals("Nandha College") || r.getEndStopId().equals("Perundurai")) {
                detourActive = true;
                break;
            }
        }

        if (detourActive) {
            lblRouteStatusBadge.setText("● ALTERNATIVE ROUTE ACTIVE (Bypassing Closed Roads)");
            lblRouteStatusBadge.setBackground(ALERT_ORANGE);
            lblRouteStatusBadge.setForeground(new Color(140, 80, 10));
        } else {
            lblRouteStatusBadge.setText("● OPTIMAL ROUTE ACTIVE (Dijkstra Shortest Path)");
            lblRouteStatusBadge.setBackground(ALERT_GREEN);
            lblRouteStatusBadge.setForeground(new Color(20, 120, 40));
        }

        // Format Path String
        StringBuilder pathStr = new StringBuilder("Path: ").append(src);
        for (Road r : result.getPath()) {
            pathStr.append(" ➔ ").append(r.getEndStopId());
        }
        lblRoutePath.setText(pathStr.toString());
        lblEstimatedTime.setText("Estimated Travel Time: " + result.getTotalTime() + " minutes (" + result.getPath().size() + " segments)");

        // 3. Bus Details and Seat Capacity (evaluated for this specific journey)
        AllocationResult alloc = net.evaluateJourneyAllocation(src, dst, 1);
        if (alloc.isAllocated()) {
            Bus activeBus = alloc.getBus();
            lblBusInfo.setText("Active Bus: " + activeBus.getId() + " (" + activeBus.getType() + ") | Status: Available");
            int totalCap = alloc.getBusCapacity();
            int rem = alloc.getRemainingSeats();
            int reserved = Math.max(0, totalCap - rem);
            lblCapacityStats.setText("Total Capacity: " + totalCap + " seats | Reserved: " + reserved + " seats | Available: " + rem + " seats");
        } else {
            lblBusInfo.setText("Assigned Bus: None (" + alloc.getFailureReason() + ")");
            lblCapacityStats.setText("Capacity: - | Reserved: - | Available: -");
        }
    }

    private void handleRunDemoFlow(ActionEvent e) {
        try {
            TripSeatTracker.runCorridorDemoFlow("TRIP-101");
            MainFrame.getInstance().refreshDashboard();
            JOptionPane.showMessageDialog(this,
                    "Dynamic Passenger Flow Demonstration Executed:\n\n" +
                            "1. Erode: 5 passengers board ➔ Available seats = 35 (Occupied: 5)\n" +
                            "2. Nandha College: 10 board, 2 get down ➔ Available seats = 27 (Occupied: 13)\n" +
                            "3. Perundurai: 3 get down ➔ Available seats = 30 (Occupied: 10)\n" +
                            "4. Kongu College: all remaining 10 get down ➔ Available seats = 40 (Occupied: 0)\n\n" +
                            "Calculation: available = previous + alighting - boarding.\n" +
                            "All stop records updated in SQLite database (transport.db).",
                    "Demo Flow Completed", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error running demo flow: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCompleteTrip(ActionEvent e) {
        try {
            TripSeatTracker.completeTrip("TRIP-101");
            MainFrame.getInstance().refreshDashboard();
            JOptionPane.showMessageDialog(this,
                    "Trip TRIP-101 completed successfully!\n" +
                            "All remaining passengers have alighted.\n" +
                            "Accepted requests marked as Completed.\n" +
                            "Seats reset to 40 for the next scheduled service.",
                    "Trip Completed", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error completing trip: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleResetTrip(ActionEvent e) {
        try {
            TripSeatTracker.resetTrip("TRIP-101");
            MainFrame.getInstance().refreshDashboard();
            JOptionPane.showMessageDialog(this,
                    "Trip TRIP-101 reset to full capacity (40 available seats at all stops).",
                    "Trip Reset", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error resetting trip: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCustomBoarding(ActionEvent e) {
        JComboBox<String> cbStops = new JComboBox<>(new String[]{"Erode", "Nandha College", "Perundurai", "Kongu College"});
        JTextField tfBoarding = new JTextField("5", 5);
        JTextField tfAlighting = new JTextField("0", 5);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Select Stop:"));
        panel.add(cbStops);
        panel.add(new JLabel("Passengers Boarding:"));
        panel.add(tfBoarding);
        panel.add(new JLabel("Passengers Alighting:"));
        panel.add(tfAlighting);

        int result = JOptionPane.showConfirmDialog(this, panel, "Record Boarding / Alighting", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String stop = (String) cbStops.getSelectedItem();
                int b = Integer.parseInt(tfBoarding.getText().trim());
                int a = Integer.parseInt(tfAlighting.getText().trim());

                List<TripStopRecord> records = TripSeatTracker.getStopRecords("TRIP-101");
                for (TripStopRecord r : records) {
                    if (r.getStopId().equals(stop)) {
                        r.setBoardingCount(Math.max(0, b));
                        r.setAlightingCount(Math.max(0, a));
                        break;
                    }
                }
                TripSeatTracker.applySeatCalculationFormula(40, records);

                // Save to SQLite
                try (java.sql.Connection conn = DatabaseManager.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        String sql = "UPDATE trip_stop_records SET boarding_count = ?, alighting_count = ?, seats_available = ? WHERE trip_id = ? AND stop_order = ?";
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                            for (TripStopRecord r : records) {
                                ps.setInt(1, r.getBoardingCount());
                                ps.setInt(2, r.getAlightingCount());
                                ps.setInt(3, r.getSeatsAvailable());
                                ps.setString(4, "TRIP-101");
                                ps.setInt(5, r.getStopOrder());
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                        conn.commit();
                    } catch (SQLException ex) {
                        conn.rollback();
                        throw ex;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }

                MainFrame.getInstance().refreshDashboard();
                JOptionPane.showMessageDialog(this, "Updated stop counts for " + stop + " in database.", "Updated", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadRecentActivity() {
        try {
            activityModel.setRowCount(0);
            List<DisruptionRecord> records = DatabaseManager.loadDisruptions();
            for (DisruptionRecord dr : records) {
                activityModel.addRow(new Object[]{
                        dr.getTimestamp() != null ? dr.getTimestamp() : "Recent",
                        dr.getType(),
                        dr.getTarget(),
                        dr.getAction(),
                        dr.getResult()
                });
            }
        } catch (Exception ex) {
            System.err.println("Could not load activity: " + ex.getMessage());
        }
    }
}
