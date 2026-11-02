import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Dedicated passenger portal with 4-stop corridor navigation,
 * real-time dynamic seat tracking, and transit request booking.
 */
public class PassengerPanel extends JPanel {
    private static final Color NAVY_DARK = new Color(10, 30, 70);
    private static final Color NAVY_HOVER = new Color(15, 45, 100);
    private static final Color BG_LIGHT = new Color(245, 248, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(215, 225, 240);

    private CardLayout innerCardLayout;
    private JPanel innerContainer;

    // View 1 components for dynamic stop cards
    private JPanel grid;
    private final java.util.Map<String, JLabel> mapStopAvailableSeats = new java.util.HashMap<>();

    // Stop Details Components
    private JLabel lblSelectedStopName;
    private JLabel lblSelectedStopDesc;
    private JLabel lblAvailableSeats;
    private JLabel lblOccupiedSeats;
    private JLabel lblStopPassengerCounts;
    private JProgressBar progressSeats;

    private JComboBox<String> cbDest;
    private JSpinner spPassengerCount;
    private JComboBox<String> cbUrgency;
    private JTextField txtWaitingTime;
    private JButton btnSubmitRequest;
    private JButton btnClearForm;
    private JLabel lblTerminalNotice;

    private JTable passengerRequestsTable;
    private DefaultTableModel requestsTableModel;
    private JButton btnCancelPassengerRequest;

    private String currentSelectedStop = "Erode";

    public PassengerPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_LIGHT);

        innerCardLayout = new CardLayout();
        innerContainer = new JPanel(innerCardLayout);
        innerContainer.setBackground(BG_LIGHT);

        initStopsSelectionView();
        initStopDetailsView();

        add(innerContainer, BorderLayout.CENTER);
        showStopsSelection();
    }

    // -------------------------------------------------------------
    // Screen 1: The Four Stops Selection Grid
    // -------------------------------------------------------------
    private void initStopsSelectionView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setBackground(BG_LIGHT);

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(NAVY_DARK);
        header.setBorder(new EmptyBorder(25, 35, 25, 35));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(NAVY_DARK);

        JLabel title = new JLabel("Passenger Transit Portal");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);

        JButton btnBackWelcome = new JButton("← Welcome Screen");
        styleSecondaryButton(btnBackWelcome);
        btnBackWelcome.addActionListener(e -> MainFrame.getInstance().showWelcomeScreen());

        topBar.add(title, BorderLayout.WEST);
        topBar.add(btnBackWelcome, BorderLayout.EAST);

        JLabel subtitle = new JLabel("Select your departure bus stop along the active transport network corridor");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitle.setForeground(new Color(205, 225, 255));

        header.add(topBar);
        header.add(Box.createRigidArea(new Dimension(0, 6)));
        header.add(subtitle);

        view.add(header, BorderLayout.NORTH);

        // Stops Grid Container
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG_LIGHT);

        grid = new JPanel();
        grid.setBackground(BG_LIGHT);
        rebuildStopsGrid();

        JScrollPane scrollGrid = new JScrollPane(grid);
        scrollGrid.setBorder(BorderFactory.createEmptyBorder());
        scrollGrid.setBackground(BG_LIGHT);
        scrollGrid.getViewport().setBackground(BG_LIGHT);
        scrollGrid.setPreferredSize(new Dimension(880, 420));

        centerPanel.add(scrollGrid);
        view.add(centerPanel, BorderLayout.CENTER);

        innerContainer.add(view, "STOPS_LIST");
    }

    private void rebuildStopsGrid() {
        if (grid == null) return;
        grid.removeAll();
        mapStopAvailableSeats.clear();

        List<Stop> stops = DataStore.getNetwork().getStops();
        int count = stops.size();
        int rows = Math.max(2, (int) Math.ceil(count / 2.0));
        grid.setLayout(new GridLayout(rows, 2, 25, 25));

        for (int i = 0; i < count; i++) {
            Stop s = stops.get(i);
            final String stopName = s.getId();
            final String stopDesc = (s.getDescription() != null && !s.getDescription().trim().isEmpty())
                    ? s.getDescription() : s.getName();
            final int stopNum = i + 1;

            JPanel stopCard = createStopCard(stopNum, stopName, stopDesc, () -> openStopDetails(stopName, stopDesc));
            grid.add(stopCard);
        }
        grid.revalidate();
        grid.repaint();
    }

    private JPanel createStopCard(int stopNum, String name, String desc, Runnable onSelect) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(18, 20, 18, 20)
        ));

        // Header with Stop Badge and Seat Badge
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(CARD_BG);

        JLabel badge = new JLabel(" STOP #" + stopNum + " ");
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));
        badge.setOpaque(true);
        badge.setBackground(new Color(230, 240, 255));
        badge.setForeground(NAVY_DARK);
        badge.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        JLabel lblSeats = new JLabel("Available Seats: Calculating...");
        lblSeats.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblSeats.setForeground(new Color(20, 120, 40));
        mapStopAvailableSeats.put(name, lblSeats);

        top.add(badge, BorderLayout.WEST);
        top.add(lblSeats, BorderLayout.EAST);

        // Stop Name & Desc
        JPanel middle = new JPanel();
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.setBackground(CARD_BG);

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblName.setForeground(NAVY_DARK);

        JLabel lblDesc = new JLabel("<html>" + desc + "</html>");
        lblDesc.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblDesc.setForeground(new Color(80, 90, 110));

        middle.add(lblName);
        middle.add(Box.createRigidArea(new Dimension(0, 4)));
        middle.add(lblDesc);

        // Action Button
        JButton btnSelect = new JButton("Select " + name + " →");
        stylePrimaryButton(btnSelect);
        btnSelect.addActionListener(e -> onSelect.run());

        card.add(top, BorderLayout.NORTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(btnSelect, BorderLayout.SOUTH);

        return card;
    }

    // -------------------------------------------------------------
    // Screen 2: Single Stop Passenger Page & Request Form
    // -------------------------------------------------------------
    private void initStopDetailsView() {
        JPanel view = new JPanel(new BorderLayout());
        view.setBackground(BG_LIGHT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY_DARK);
        header.setBorder(new EmptyBorder(18, 30, 18, 30));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBackground(NAVY_DARK);

        lblSelectedStopName = new JLabel("Stop: Erode");
        lblSelectedStopName.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblSelectedStopName.setForeground(Color.WHITE);

        lblSelectedStopDesc = new JLabel("Central Bus Terminal, Erode");
        lblSelectedStopDesc.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSelectedStopDesc.setForeground(new Color(205, 225, 255));

        titleBox.add(lblSelectedStopName);
        titleBox.add(Box.createRigidArea(new Dimension(0, 4)));
        titleBox.add(lblSelectedStopDesc);

        JButton btnBackToStops = new JButton("← Back to Stops");
        styleSecondaryButton(btnBackToStops);
        btnBackToStops.addActionListener(e -> showStopsSelection());

        header.add(titleBox, BorderLayout.WEST);
        header.add(btnBackToStops, BorderLayout.EAST);
        view.add(header, BorderLayout.NORTH);

        // Center Split: Left = Seat status & booking form; Right = Live requests list
        JPanel center = new JPanel(new GridLayout(1, 2, 15, 0));
        center.setBackground(BG_LIGHT);
        center.setBorder(new EmptyBorder(15, 25, 15, 25));

        // LEFT CARD: Seat Tracker & Form
        JPanel contentCard = new JPanel();
        contentCard.setLayout(new BoxLayout(contentCard, BoxLayout.Y_AXIS));
        contentCard.setBackground(Color.WHITE);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(20, 25, 20, 25)
        ));

        // Seat Info Box
        JPanel seatBox = new JPanel();
        seatBox.setLayout(new BoxLayout(seatBox, BoxLayout.Y_AXIS));
        seatBox.setBackground(new Color(238, 246, 255));
        seatBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 215, 255), 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        lblAvailableSeats = new JLabel("Available Seats: Calculating...");
        lblAvailableSeats.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblAvailableSeats.setForeground(new Color(10, 50, 120));

        lblOccupiedSeats = new JLabel("Bus Capacity: 40 seats | Occupied: 0 | Available: 40");
        lblOccupiedSeats.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblOccupiedSeats.setForeground(new Color(50, 70, 100));

        progressSeats = new JProgressBar(0, 40);
        progressSeats.setValue(40);
        progressSeats.setStringPainted(true);
        progressSeats.setFont(new Font("SansSerif", Font.BOLD, 12));
        progressSeats.setForeground(new Color(20, 140, 50));
        progressSeats.setPreferredSize(new Dimension(300, 22));

        lblStopPassengerCounts = new JLabel("Boarding at this stop: 0 | Alighting: 0");
        lblStopPassengerCounts.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblStopPassengerCounts.setForeground(new Color(80, 90, 110));

        seatBox.add(lblAvailableSeats);
        seatBox.add(Box.createRigidArea(new Dimension(0, 4)));
        seatBox.add(lblOccupiedSeats);
        seatBox.add(Box.createRigidArea(new Dimension(0, 6)));
        seatBox.add(progressSeats);
        seatBox.add(Box.createRigidArea(new Dimension(0, 4)));
        seatBox.add(lblStopPassengerCounts);

        contentCard.add(seatBox);
        contentCard.add(Box.createRigidArea(new Dimension(0, 15)));

        // Form Section Header
        JLabel formHeader = new JLabel("Book Passenger Transit Request");
        formHeader.setFont(new Font("SansSerif", Font.BOLD, 17));
        formHeader.setForeground(NAVY_DARK);
        contentCard.add(formHeader);
        contentCard.add(Box.createRigidArea(new Dimension(0, 10)));

        lblTerminalNotice = new JLabel("<html><b style='color:#c02020;'>Kongu College is the final stop on this corridor.</b><br>All passengers alight here. Boarding is not available.</html>");
        lblTerminalNotice.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblTerminalNotice.setVisible(false);
        contentCard.add(lblTerminalNotice);

        JPanel formGrid = new JPanel(new GridLayout(4, 2, 8, 8));
        formGrid.setBackground(Color.WHITE);

        formGrid.add(new JLabel("Destination Stop:"));
        cbDest = new JComboBox<>();
        formGrid.add(cbDest);

        formGrid.add(new JLabel("Passenger Count:"));
        spPassengerCount = new JSpinner(new SpinnerNumberModel(1, 1, 40, 1));
        formGrid.add(spPassengerCount);

        formGrid.add(new JLabel("Urgency Level:"));
        cbUrgency = new JComboBox<>(new String[]{"Low", "Medium", "High"});
        cbUrgency.setSelectedItem("Medium");
        formGrid.add(cbUrgency);

        formGrid.add(new JLabel("Waiting Time (mins):"));
        txtWaitingTime = new JTextField("10", 6);
        formGrid.add(txtWaitingTime);

        contentCard.add(formGrid);
        contentCard.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setBackground(Color.WHITE);

        btnSubmitRequest = new JButton("Submit Passenger Request (Pending)");
        stylePrimaryButton(btnSubmitRequest);
        btnSubmitRequest.addActionListener(this::handleSubmitRequest);

        btnClearForm = new JButton("Clear");
        styleSecondaryButton(btnClearForm);
        btnClearForm.addActionListener(e -> clearRequestForm());

        buttonRow.add(btnSubmitRequest);
        buttonRow.add(btnClearForm);
        contentCard.add(buttonRow);

        // RIGHT CARD: Live Corridor Requests Table
        JPanel requestsCard = new JPanel(new BorderLayout(8, 8));
        requestsCard.setBackground(Color.WHITE);
        requestsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel lblReqTitle = new JLabel("Corridor Passenger Requests");
        lblReqTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblReqTitle.setForeground(NAVY_DARK);
        requestsCard.add(lblReqTitle, BorderLayout.NORTH);

        requestsTableModel = new DefaultTableModel(new Object[]{"ID", "From", "To", "Count", "Status"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        passengerRequestsTable = new JTable(requestsTableModel);
        passengerRequestsTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        passengerRequestsTable.setRowHeight(22);

        // Status Renderer
        passengerRequestsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
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
                }
                return c;
            }
        });

        requestsCard.add(new JScrollPane(passengerRequestsTable), BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionRow.setBackground(Color.WHITE);

        btnCancelPassengerRequest = new JButton("Cancel Selected Request");
        styleSecondaryButton(btnCancelPassengerRequest);
        btnCancelPassengerRequest.addActionListener(this::handleCancelPassengerRequest);
        actionRow.add(btnCancelPassengerRequest);

        requestsCard.add(actionRow, BorderLayout.SOUTH);

        center.add(contentCard);
        center.add(requestsCard);
        view.add(center, BorderLayout.CENTER);

        innerContainer.add(view, "STOP_DETAILS");
    }

    public void clearRequestForm() {
        if (cbDest != null && cbDest.getItemCount() > 0) {
            cbDest.setSelectedIndex(0);
        }
        if (spPassengerCount != null) {
            spPassengerCount.setValue(1);
        }
        if (cbUrgency != null) {
            cbUrgency.setSelectedItem("Medium");
        }
        if (txtWaitingTime != null) {
            txtWaitingTime.setText("10");
        }
    }

    private void openStopDetails(String stopName, String stopDesc) {
        this.currentSelectedStop = stopName;
        lblSelectedStopName.setText("Departure Stop: " + stopName);
        lblSelectedStopDesc.setText(stopDesc != null ? stopDesc : stopName);

        // Populate destination dropdown strictly with stops AFTER the boarding stop
        cbDest.removeAllItems();
        List<Stop> stops = DataStore.getNetwork().getStops();
        int stopIdx = -1;
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).getId().equalsIgnoreCase(stopName)) {
                stopIdx = i;
                break;
            }
        }

        if (stopIdx != -1) {
            for (int i = stopIdx + 1; i < stops.size(); i++) {
                cbDest.addItem(stops.get(i).getId());
            }
        }

        boolean isTerminal = (stopIdx == stops.size() - 1);
        lblTerminalNotice.setText("<html><b style='color:#c02020;'>" + stopName + " is the final stop on this corridor.</b><br>All passengers alight here. Boarding is not available.</html>");
        lblTerminalNotice.setVisible(isTerminal);
        cbDest.setEnabled(!isTerminal);
        spPassengerCount.setEnabled(!isTerminal);
        cbUrgency.setEnabled(!isTerminal);
        txtWaitingTime.setEnabled(!isTerminal);
        btnSubmitRequest.setEnabled(!isTerminal);
        if (btnClearForm != null) btnClearForm.setEnabled(!isTerminal);

        refreshSeatAvailability();
        innerCardLayout.show(innerContainer, "STOP_DETAILS");
    }

    public void refreshSeatAvailability() {
        // Refresh grid card labels
        for (java.util.Map.Entry<String, JLabel> entry : mapStopAvailableSeats.entrySet()) {
            String sId = entry.getKey();
            JLabel lbl = entry.getValue();
            if (lbl != null) {
                int seats = DatabaseManager.getAvailableSeatsForStop(sId);
                int cap = TripSeatTracker.getCapacity("TRIP-101");
                lbl.setText("Available Seats: " + seats + " / " + (cap > 0 ? cap : 40));
                if (seats > 20) {
                    lbl.setForeground(new Color(20, 120, 40));
                } else if (seats >= 10) {
                    lbl.setForeground(new Color(180, 110, 0));
                } else {
                    lbl.setForeground(new Color(180, 20, 20));
                }
            }
        }

        // Refresh detail view
        int avail = DatabaseManager.getAvailableSeatsForStop(currentSelectedStop);
        int cap = TripSeatTracker.getCapacity("TRIP-101");
        int occ = Math.max(0, cap - avail);

        lblAvailableSeats.setText("Available Seats at " + currentSelectedStop + ": " + avail + " / " + cap);
        lblOccupiedSeats.setText("Bus Capacity: " + cap + " seats | Occupied: " + occ + " | Available: " + avail);

        progressSeats.setMaximum(cap);
        progressSeats.setValue(avail);
        progressSeats.setString(avail + " seats available");
        if (avail > 20) {
            progressSeats.setForeground(new Color(20, 140, 50));
        } else if (avail >= 10) {
            progressSeats.setForeground(new Color(200, 120, 0));
        } else {
            progressSeats.setForeground(new Color(190, 20, 20));
        }

        // Boarding and alighting numbers
        try {
            List<TripStopRecord> records = TripSeatTracker.getStopRecords("TRIP-101");
            for (TripStopRecord r : records) {
                if (r.getStopId().equals(currentSelectedStop)) {
                    lblStopPassengerCounts.setText("Boarding at this stop: " + r.getBoardingCount() + " | Alighting: " + r.getAlightingCount());
                    break;
                }
            }
        } catch (SQLException ignored) {}

        // Refresh requests table
        refreshPassengerRequestsTable();
    }

    private void refreshPassengerRequestsTable() {
        requestsTableModel.setRowCount(0);
        List<PassengerRequest> reqs = DataStore.getNetwork().getRequests();
        for (PassengerRequest r : reqs) {
            requestsTableModel.addRow(new Object[]{
                    r.getId(),
                    r.getSourceStopId(),
                    r.getDestinationStopId(),
                    r.getPassengerCount(),
                    r.getStatus()
            });
        }
    }

    private void handleSubmitRequest(ActionEvent e) {
        String dest = (String) cbDest.getSelectedItem();
        if (dest == null || dest.equals(currentSelectedStop)) {
            JOptionPane.showMessageDialog(this,
                    "Destination stop must be selected and occur after the departure stop.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int count = (Integer) spPassengerCount.getValue();
        if (count <= 0) {
            JOptionPane.showMessageDialog(this, "Passenger count must be at least 1.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int waitingTime;
        try {
            waitingTime = Integer.parseInt(txtWaitingTime.getText().trim());
            if (waitingTime < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Waiting time must be a non-negative integer.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String urgency = (String) cbUrgency.getSelectedItem();
        String reqId = "PASS-" + (System.currentTimeMillis() % 100000);

        // Pre-validate route segment capacity
        boolean canBook = TripSeatTracker.canReserve("TRIP-101", currentSelectedStop, dest, count);
        if (!canBook) {
            int currentAvail = DatabaseManager.getAvailableSeatsForStop(currentSelectedStop);
            JOptionPane.showMessageDialog(this,
                    "Notice: Bus currently has only " + currentAvail + " seats available for this segment.\n" +
                            "Your request for " + count + " passengers will be registered as Pending,\n" +
                            "but Transport Admin acceptance may be restricted unless seats are freed.",
                    "Capacity Notice", JOptionPane.WARNING_MESSAGE);
        }

        // Create with status Pending (Pending requests do NOT take seats until accepted by transport admin)
        PassengerRequest req = new PassengerRequest(reqId, currentSelectedStop, dest, count, urgency, waitingTime);
        req.setStatus("Pending");

        boolean ok = DataStore.getNetwork().addRequest(req);
        if (ok) {
            MainFrame.getInstance().refreshDashboard();
            refreshSeatAvailability();
            JOptionPane.showMessageDialog(this,
                    "Passenger request submitted successfully!\n\n" +
                            "• Request ID: " + reqId + "\n" +
                            "• From: " + currentSelectedStop + "\n" +
                            "• To: " + dest + "\n" +
                            "• Passengers: " + count + "\n" +
                            "• Status: Pending (Awaiting Transport Admin acceptance)\n\n" +
                            "Your request is now visible in the Transport system.",
                    "Request Confirmed", JOptionPane.INFORMATION_MESSAGE);
            spPassengerCount.setValue(1);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to register request in the database. Please try again.", "Submission Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelPassengerRequest(ActionEvent e) {
        int sel = passengerRequestsTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a request from the table to cancel.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String reqId = (String) requestsTableModel.getValueAt(sel, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel request " + reqId + "?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = TripSeatTracker.cancelRequest("TRIP-101", reqId);
            if (ok) {
                DataStore.getNetwork().updateRequestStatus(reqId, "Cancelled");
                MainFrame.getInstance().refreshDashboard();
                refreshSeatAvailability();
                JOptionPane.showMessageDialog(this, "Request " + reqId + " cancelled successfully.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Could not cancel request " + reqId, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void showStopsSelection() {
        rebuildStopsGrid();
        refreshSeatAvailability();
        innerCardLayout.show(innerContainer, "STOPS_LIST");
    }

    public void refreshStopsView() {
        rebuildStopsGrid();
        refreshSeatAvailability();
        if (currentSelectedStop != null) {
            Stop s = DataStore.getNetwork().getStop(currentSelectedStop);
            if (s != null) {
                List<Stop> stops = DataStore.getNetwork().getStops();
                int stopIdx = -1;
                for (int i = 0; i < stops.size(); i++) {
                    if (stops.get(i).getId().equalsIgnoreCase(currentSelectedStop)) {
                        stopIdx = i;
                        break;
                    }
                }
                if (cbDest != null && stopIdx != -1) {
                    String prevDest = (String) cbDest.getSelectedItem();
                    cbDest.removeAllItems();
                    for (int i = stopIdx + 1; i < stops.size(); i++) {
                        cbDest.addItem(stops.get(i).getId());
                    }
                    if (prevDest != null) {
                        cbDest.setSelectedItem(prevDest);
                    }
                }
            }
        }
    }

    private void stylePrimaryButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(NAVY_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(NAVY_HOVER, 1),
                new EmptyBorder(10, 16, 10, 16)
        ));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(NAVY_HOVER); }
            public void mouseExited(MouseEvent e) { btn.setBackground(NAVY_DARK); }
        });
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(new Color(235, 240, 250));
        btn.setForeground(NAVY_DARK);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 1),
                new EmptyBorder(8, 14, 8, 14)
        ));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(220, 230, 245)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(new Color(235, 240, 250)); }
        });
    }
}
