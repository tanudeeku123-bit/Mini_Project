import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;

/**
 * Authentication screen for Transport Authority and staff.
 * Validates credentials against SQLite database users table.
 */
public class LoginPanel extends JPanel {
    private static final Color NAVY_DARK = new Color(10, 30, 70);
    private static final Color NAVY_HOVER = new Color(15, 45, 100);
    private static final Color BG_LIGHT = new Color(245, 248, 252);

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JButton btnLogin;
    private JButton btnBack;

    public LoginPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_LIGHT);

        initHeader();
        initCenter();
    }

    private void initHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(NAVY_DARK);
        header.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel title = new JLabel("Transport Authority Authentication");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in with authorized staff credentials to manage the transit network");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(205, 225, 255));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 5)));
        header.add(subtitle);

        add(header, BorderLayout.NORTH);
    }

    private void initCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_LIGHT);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
                new EmptyBorder(30, 40, 30, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel formTitle = new JLabel("Staff Login");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        formTitle.setForeground(NAVY_DARK);
        formTitle.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(formTitle, gbc);

        gbc.gridy++;
        lblError = new JLabel(" ");
        lblError.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblError.setForeground(new Color(180, 20, 20));
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblError, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        JLabel lblUser = new JLabel("Username:");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 14));
        card.add(lblUser, gbc);

        gbc.gridx = 1;
        txtUsername = new JTextField(16);
        txtUsername.setFont(new Font("SansSerif", Font.PLAIN, 14));
        card.add(txtUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setFont(new Font("SansSerif", Font.BOLD, 14));
        card.add(lblPass, gbc);

        gbc.gridx = 1;
        txtPassword = new JPasswordField(16);
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 14));
        card.add(txtPassword, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 15, 0));
        buttonRow.setBackground(Color.WHITE);

        btnLogin = new JButton("Login");
        stylePrimaryButton(btnLogin);

        btnBack = new JButton("← Back");
        styleSecondaryButton(btnBack);

        buttonRow.add(btnBack);
        buttonRow.add(btnLogin);
        card.add(buttonRow, gbc);

        // Actions
        btnLogin.addActionListener(this::handleLogin);
        txtPassword.addActionListener(this::handleLogin);
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());
        btnBack.addActionListener(e -> {
            clearFields();
            MainFrame.getInstance().showWelcomeScreen();
        });

        center.add(card);
        add(center, BorderLayout.CENTER);
    }

    private void handleLogin(ActionEvent e) {
        String username = txtUsername.getText().trim();
        char[] password = txtPassword.getPassword();

        System.out.println("[AUTH-UI] Login button clicked for username: '" + username + "' (password length: " + password.length + ")");

        if (username.isEmpty() || password.length == 0) {
            lblError.setText("Please enter both username and password.");
            System.out.println("[AUTH-UI] Validation failed: username or password is empty.");
            return;
        }

        try {
            TransportUser user = DatabaseManager.authenticateUser(username, password);
            if (user != null) {
                System.out.println("[AUTH-UI] Authentication successful for user: " + user.getUsername() + ". Opening Transport Dashboard.");
                lblError.setText(" ");
                clearFields();
                MainFrame.getInstance().setAuthenticatedUser(user);
                MainFrame.getInstance().showTransportPortal();
            } else {
                System.out.println("[AUTH-UI] Authentication failed: DatabaseManager.authenticateUser returned null.");
                lblError.setText("Invalid username or password.");
                JOptionPane.showMessageDialog(this,
                        "Authentication failed: Invalid credentials.\nPlease check your username and password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                txtPassword.setText("");
                txtPassword.requestFocusInWindow();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.println("[AUTH-UI] SQLException during authentication: " + ex.getMessage());
            lblError.setText("Database error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Database error during authentication: " + ex.getMessage(),
                    "System Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearFields() {
        txtUsername.setText("");
        txtPassword.setText("");
        lblError.setText(" ");
    }

    private void stylePrimaryButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setBackground(NAVY_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(NAVY_HOVER, 1),
                new EmptyBorder(10, 16, 10, 16)
        ));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(NAVY_HOVER);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(NAVY_DARK);
            }
        });
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 15));
        btn.setBackground(new Color(235, 240, 250));
        btn.setForeground(NAVY_DARK);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 200, 230), 1),
                new EmptyBorder(10, 16, 10, 16)
        ));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(220, 230, 245));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(235, 240, 250));
            }
        });
    }
}

