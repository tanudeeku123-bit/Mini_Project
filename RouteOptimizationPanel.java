import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class RouteOptimizationPanel extends JPanel {
    private JComboBox<String> cbSource, cbDest;
    private JButton btnFind, btnClear;
    private JTextArea txtResult;

    public RouteOptimizationPanel() {
        setLayout(new BorderLayout(10,10));
        initForm();
        initResultArea();
    }

    private void initForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
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
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnFind = new JButton("Find Best Route");
        btnClear = new JButton("Clear Form");
        btnPanel.add(btnFind);
        btnPanel.add(btnClear);
        form.add(btnPanel, gbc);

        btnFind.addActionListener(this::handleFind);
        btnClear.addActionListener(e -> clearForm());
        cbSource.addActionListener(e -> updateDestCombo());
        add(form, BorderLayout.NORTH);
    }

    private void clearForm() {
        if (cbSource.getItemCount() > 0) cbSource.setSelectedIndex(0);
        updateDestCombo();
        txtResult.setText("");
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

    private void initResultArea() {
        txtResult = new JTextArea(15, 50);
        txtResult.setEditable(false);
        txtResult.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(txtResult);
        add(sp, BorderLayout.CENTER);
    }

    /** Refresh stop combos, called by MainFrame when data changes */
    public void refreshStopCombos() {
        List<Stop> stops = DataStore.getNetwork().getStops();
        String prevSrc = (String) cbSource.getSelectedItem();
        cbSource.removeAllItems();
        for (Stop s : stops) {
            cbSource.addItem(s.getId());
        }
        if (prevSrc != null && contains(cbSource, prevSrc)) {
            cbSource.setSelectedItem(prevSrc);
        }
        updateDestCombo();
    }

    private void handleFind(ActionEvent e) {
        String src = (String) cbSource.getSelectedItem();
        String dst = (String) cbDest.getSelectedItem();
        if (src == null || dst == null) {
            JOptionPane.showMessageDialog(this, "Select both source and destination stops.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (src.equals(dst)) {
            JOptionPane.showMessageDialog(this, "Source and destination must differ.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        RouteResult result = DataStore.getNetwork().findShortestPath(src, dst);
        if (result == null) {
            txtResult.setText("No route found between " + src + " and " + dst + ".");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Shortest route from ").append(src).append(" to ").append(dst).append(":\n\n");
        int step = 1;
        for (Road r : result.getPath()) {
            sb.append(step++)
              .append('.').append(' ')
              .append(r.getStartStopId()).append(" -> ")
              .append(r.getEndStopId())
              .append(" (Time: ").append(r.getTravelTime()).append(" min, Status: ").append(r.getStatus()).append(")\n");
        }
        sb.append("\nTotal travel time: ").append(result.getTotalTime()).append(" minutes");
        txtResult.setText(sb.toString());
    }
}
