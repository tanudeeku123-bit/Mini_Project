import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    // Singleton instance for easy access from panels
    private static MainFrame instance;

    // Root Navigation (Role-based)
    private CardLayout rootCardLayout;
    private JPanel rootPanel;
    private WelcomePanel welcomePanel;
    private LoginPanel loginPanel;
    private PassengerPanel passengerPanel;
    private JPanel transportContainerPanel;

    // Authentication session state
    private TransportUser authenticatedUser = null;

    // Existing Transport Management Panels
    private DashboardPanel dashboardPanel;
    private StopsPanel stopsPanel;
    private RoadsPanel roadsPanel;
    private FleetPanel fleetPanel;
    private RequestsPanel requestsPanel;
    private RouteOptimizationPanel routeOptimizationPanel;
    private DisruptionSimulatorPanel disruptionSimulatorPanel;
    private DecisionExplanationPanel decisionExplanationPanel;
    private ReportsPanel reportsPanel;

    private JPanel sidebar;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public MainFrame() {
        instance = this;
        setTitle("Smart Public Transportation Route Optimization System");
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Ensure database and persistence layer are initialized
        DataStore.initDataStore();

        rootCardLayout = new CardLayout();
        rootPanel = new JPanel(rootCardLayout);

        // Initialize role screens
        welcomePanel = new WelcomePanel();
        loginPanel = new LoginPanel();
        passengerPanel = new PassengerPanel();
        initTransportContainer();

        rootPanel.add(welcomePanel, "WELCOME");
        rootPanel.add(loginPanel, "LOGIN");
        rootPanel.add(passengerPanel, "PASSENGER");
        rootPanel.add(transportContainerPanel, "TRANSPORT");

        setContentPane(rootPanel);
        showWelcomeScreen();

        setVisible(true);
    }

    // -------------------------------------------------------------
    // Transport Container Setup (Sidebar + Existing 9 Panels)
    // -------------------------------------------------------------
    private void initTransportContainer() {
        transportContainerPanel = new JPanel(new BorderLayout());
        initSidebar();
        initMainPanel();
        transportContainerPanel.add(sidebar, BorderLayout.WEST);
        transportContainerPanel.add(mainPanel, BorderLayout.CENTER);
    }

    private void initSidebar() {
        sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(10, 30, 70)); // dark navy

        JPanel menuItems = new JPanel(new GridLayout(0, 1, 0, 4));
        menuItems.setBackground(new Color(10, 30, 70));

        JLabel lblTitle = new JLabel("Transport Portal", SwingConstants.LEFT);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTitle.setForeground(new Color(205, 225, 255));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        menuItems.add(lblTitle);

        String[] sections = {
                "Dashboard",
                "Bus Stops",
                "Road Network",
                "Bus Fleet",
                "Passenger Requests",
                "Route Optimization",
                "Disruption Simulator",
                "Reports",
                "Decision Explanation"
        };
        for (String sec : sections) {
            JButton btn = new JButton(sec);
            styleSidebarButton(btn);
            btn.addActionListener(e -> showCard(sec));
            menuItems.add(btn);
        }

        sidebar.add(menuItems, BorderLayout.NORTH);

        // Logout Section at bottom of sidebar
        JPanel bottomBar = new JPanel(new GridLayout(1, 1, 0, 0));
        bottomBar.setBackground(new Color(10, 30, 70));
        bottomBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 18, 15));

        JButton btnLogout = new JButton("⎋  Logout");
        styleLogoutButton(btnLogout);
        btnLogout.addActionListener(e -> logout());
        bottomBar.add(btnLogout);

        sidebar.add(bottomBar, BorderLayout.SOUTH);
    }

    private void styleSidebarButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(10, 30, 70));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(15, 45, 100));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(10, 30, 70));
            }
        });
    }

    private void styleLogoutButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setForeground(new Color(255, 215, 215));
        btn.setBackground(new Color(75, 20, 30));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(130, 40, 50), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(110, 25, 40));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(75, 20, 30));
            }
        });
    }

    private void initMainPanel() {
        mainPanel = new JPanel();
        dashboardPanel = new DashboardPanel();
        stopsPanel = new StopsPanel();
        roadsPanel = new RoadsPanel();
        fleetPanel = new FleetPanel();
        requestsPanel = new RequestsPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(stopsPanel, "Bus Stops");
        mainPanel.add(roadsPanel, "Road Network");
        mainPanel.add(fleetPanel, "Bus Fleet");
        mainPanel.add(requestsPanel, "Passenger Requests");

        routeOptimizationPanel = new RouteOptimizationPanel();
        mainPanel.add(routeOptimizationPanel, "Route Optimization");

        disruptionSimulatorPanel = new DisruptionSimulatorPanel();
        mainPanel.add(disruptionSimulatorPanel, "Disruption Simulator");

        reportsPanel = new ReportsPanel();
        mainPanel.add(reportsPanel, "Reports");

        decisionExplanationPanel = new DecisionExplanationPanel();
        mainPanel.add(decisionExplanationPanel, "Decision Explanation");
    }

    // -------------------------------------------------------------
    // Role-Based Screen Navigation
    // -------------------------------------------------------------
    public void showWelcomeScreen() {
        authenticatedUser = null;
        rootCardLayout.show(rootPanel, "WELCOME");
    }

    public void showLoginScreen() {
        if (loginPanel != null) {
            loginPanel.clearFields();
        }
        rootCardLayout.show(rootPanel, "LOGIN");
    }

    public void showPassengerPortal() {
        if (passengerPanel != null) {
            passengerPanel.refreshStopsView();
        }
        rootCardLayout.show(rootPanel, "PASSENGER");
    }

    public void showTransportPortal() {
        if (authenticatedUser == null) {
            showLoginScreen();
            return;
        }
        refreshDashboard();
        rootCardLayout.show(rootPanel, "TRANSPORT");
        showCard("Dashboard");
    }

    public void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to log out of the Transport management portal?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            authenticatedUser = null;
            showWelcomeScreen();
        }
    }

    public void setAuthenticatedUser(TransportUser user) {
        this.authenticatedUser = user;
    }

    public TransportUser getAuthenticatedUser() {
        return authenticatedUser;
    }

    // -------------------------------------------------------------
    // Transport Admin Card Switching (Guarded by Authentication)
    // -------------------------------------------------------------
    public void showCard(String name) {
        if (authenticatedUser == null) {
            showLoginScreen();
            return;
        }

        rootCardLayout.show(rootPanel, "TRANSPORT");
        cardLayout.show(mainPanel, name);

        // Ensure any panel that depends on the stop list has fresh data
        refreshStopCombos();
        if ("Disruption Simulator".equals(name) && disruptionSimulatorPanel != null) {
            disruptionSimulatorPanel.refreshAllCombos();
        }
        if ("Reports".equals(name) && reportsPanel != null) {
            reportsPanel.refreshAll();
        }
        if ("Decision Explanation".equals(name) && decisionExplanationPanel != null) {
            decisionExplanationPanel.refreshStopCombos();
        }
    }

    /** Allows other panels to request a dashboard refresh */
    public void refreshDashboard() {
        if (dashboardPanel != null) {
            dashboardPanel.refreshStats();
        }
        if (stopsPanel != null) {
            stopsPanel.refreshTable();
        }
        if (roadsPanel != null) {
            roadsPanel.refreshTable();
        }
        if (fleetPanel != null) {
            fleetPanel.refreshTable();
        }
        if (requestsPanel != null) {
            requestsPanel.refreshTable();
        }
        if (passengerPanel != null) {
            passengerPanel.refreshStopsView();
            passengerPanel.refreshSeatAvailability();
        }
        if (reportsPanel != null) {
            reportsPanel.refreshAll();
        }
        if (disruptionSimulatorPanel != null) {
            disruptionSimulatorPanel.refreshAllCombos();
        }
        if (decisionExplanationPanel != null) {
            decisionExplanationPanel.refreshStopCombos();
        }
        refreshStopCombos();
    }

    /** Refresh stop combo boxes across panels */
    public void refreshStopCombos() {
        if (roadsPanel != null) {
            roadsPanel.refreshStopCombos();
        }
        if (requestsPanel != null) {
            requestsPanel.refreshStopCombos();
        }
        if (routeOptimizationPanel != null) {
            routeOptimizationPanel.refreshStopCombos();
        }
        if (decisionExplanationPanel != null) {
            decisionExplanationPanel.refreshStopCombos();
        }
        if (passengerPanel != null) {
            passengerPanel.refreshStopsView();
        }
        if (dashboardPanel != null) {
            dashboardPanel.refreshStats();
        }
    }

    /** Global accessor for panels that need the main frame */
    public static MainFrame getInstance() {
        return instance;
    }

    // Simple placeholder panel for unfinished sections
    private static class PlaceholderPanel extends JPanel {
        public PlaceholderPanel(String title) {
            setLayout(new BorderLayout());
            JLabel lbl = new JLabel("<html><h2>" + title + "</h2><p>Feature coming soon.</p></html>", SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 18));
            add(lbl, BorderLayout.CENTER);
        }
    }
}
