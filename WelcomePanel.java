import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Welcome screen presenting the user with role-based navigation:
 * "Transport" (Admin/Staff) and "Passenger" (Commuter).
 */
public class WelcomePanel extends JPanel {
    private static final Color NAVY_DARK = new Color(10, 30, 70);
    private static final Color NAVY_HOVER = new Color(15, 45, 100);
    private static final Color BG_LIGHT = new Color(245, 248, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(215, 225, 240);

    public WelcomePanel() {
        setLayout(new BorderLayout());
        setBackground(BG_LIGHT);

        initHeader();
        initCenter();
        initFooter();
    }

    private void initHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(NAVY_DARK);
        header.setBorder(new EmptyBorder(35, 40, 35, 40));

        JLabel title = new JLabel("Smart Public Transportation");
        title.setFont(new Font("SansSerif", Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Corridor Route Optimization & Intelligent Transit Management");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitle.setForeground(new Color(205, 225, 255));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createRigidArea(new Dimension(0, 8)));
        header.add(subtitle);

        add(header, BorderLayout.NORTH);
    }

    private void initCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(BG_LIGHT);
        center.setBorder(new EmptyBorder(40, 60, 40, 60));

        JLabel prompt = new JLabel("Select your role to access the system:");
        prompt.setFont(new Font("SansSerif", Font.BOLD, 18));
        prompt.setForeground(NAVY_DARK);
        prompt.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(prompt);
        center.add(Box.createRigidArea(new Dimension(0, 30)));

        // Cards Container
        JPanel cardsContainer = new JPanel(new GridLayout(1, 2, 40, 0));
        cardsContainer.setBackground(BG_LIGHT);
        cardsContainer.setMaximumSize(new Dimension(850, 350));

        // Transport Card
        JPanel transportCard = createRoleCard(
                "Transport",
                "<html><b>Transport Administration & Operations</b><br><br>" +
                        "• Full network and stop management<br>" +
                        "• Road condition & disruption controls<br>" +
                        "• Bus fleet allocation & Dijkstra routing<br>" +
                        "• System reports, statistics & analytics</html>",
                "Transport",
                () -> MainFrame.getInstance().showLoginScreen()
        );

        // Passenger Card
        JPanel passengerCard = createRoleCard(
                "Passenger",
                "<html><b>Commuter & Passenger Services</b><br><br>" +
                        "• Dedicated 4-stop corridor exploration<br>" +
                        "• Real-time seat availability tracking<br>" +
                        "• Fast transit request booking<br>" +
                        "• Direct service without admin login</html>",
                "Passenger",
                () -> MainFrame.getInstance().showPassengerPortal()
        );

        cardsContainer.add(transportCard);
        cardsContainer.add(passengerCard);
        cardsContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(cardsContainer);
        add(center, BorderLayout.CENTER);
    }

    private JPanel createRoleCard(String titleText, String descHtml, String buttonText, Runnable onClick) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(25, 25, 25, 25)
        ));

        // Title
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(NAVY_DARK);

        // Description
        JLabel desc = new JLabel(descHtml);
        desc.setFont(new Font("SansSerif", Font.PLAIN, 14));
        desc.setForeground(new Color(60, 70, 90));
        desc.setVerticalAlignment(SwingConstants.TOP);

        // Button
        JButton btn = new JButton(buttonText);
        styleActionButton(btn);
        btn.addActionListener(e -> onClick.run());

        card.add(title, BorderLayout.NORTH);
        card.add(desc, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);

        return card;
    }

    private void styleActionButton(JButton btn) {
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setBackground(NAVY_DARK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(NAVY_HOVER, 1),
                new EmptyBorder(12, 20, 12, 20)
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

    private void initFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footer.setBackground(BG_LIGHT);
        footer.setBorder(new EmptyBorder(10, 10, 20, 10));

        JLabel status = new JLabel("● Local SQLite Connected (transport.db) | Active Corridor: Erode ➔ Nandha College ➔ Perundurai ➔ Kongu College");
        status.setFont(new Font("SansSerif", Font.ITALIC, 13));
        status.setForeground(new Color(100, 115, 140));

        footer.add(status);
        add(footer, BorderLayout.SOUTH);
    }
}

