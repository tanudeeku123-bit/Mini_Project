import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class StopsPanel extends JPanel {
    private JTextField txtId, txtName, txtDesc;
    private JButton btnAdd, btnUpdate, btnClear, btnRemove;
    private JTable stopTable;
    private DefaultTableModel tableModel;

    public StopsPanel() {
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
        form.add(new JLabel("Stop ID:"), gbc);
        gbc.gridx = 1;
        txtId = new JTextField(10);
        form.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        txtName = new JTextField(15);
        form.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        txtDesc = new JTextField(20);
        form.add(txtDesc, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        btnAdd = new JButton("Add Stop");
        btnUpdate = new JButton("Update Stop");
        btnClear = new JButton("Clear Form");
        btnRemove = new JButton("Remove Selected");
        buttons.add(btnAdd);
        buttons.add(btnUpdate);
        buttons.add(btnClear);
        buttons.add(btnRemove);
        form.add(buttons, gbc);

        add(form, BorderLayout.NORTH);

        // Action listeners
        btnAdd.addActionListener(this::handleAdd);
        btnUpdate.addActionListener(this::handleUpdate);
        btnClear.addActionListener(e -> clearForm());
        btnRemove.addActionListener(this::handleRemove);
    }

    private void initTable() {
        tableModel = new DefaultTableModel(new Object[]{"ID","Name","Description"},0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        stopTable = new JTable(tableModel);
        stopTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stopTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int sel = stopTable.getSelectedRow();
                if (sel != -1) {
                    String id = (String) tableModel.getValueAt(sel, 0);
                    String name = (String) tableModel.getValueAt(sel, 1);
                    String desc = (String) tableModel.getValueAt(sel, 2);
                    txtId.setText(id != null ? id : "");
                    txtId.setEditable(false);
                    txtName.setText(name != null ? name : "");
                    txtDesc.setText(desc != null ? desc : "");
                }
            }
        });
        JScrollPane sp = new JScrollPane(stopTable);
        sp.setPreferredSize(new Dimension(400,200));
        add(sp, BorderLayout.CENTER);
    }

    private void clearForm() {
        txtId.setText("");
        txtId.setEditable(true);
        txtName.setText("");
        txtDesc.setText("");
        stopTable.clearSelection();
    }

    private void handleAdd(ActionEvent e) {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        if (id.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "ID and Name cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Stop stop = new Stop(id, name, desc);
        boolean ok = DataStore.getNetwork().addStop(stop);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Stop ID or Name already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
        JOptionPane.showMessageDialog(this, "Stop '" + id + "' added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleUpdate(ActionEvent e) {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        if (id.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a stop and ensure ID and Name are not empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Stop existing = DataStore.getNetwork().getStop(id);
        int seq = (existing != null) ? existing.getSequenceOrder() : 0;
        Stop stop = new Stop(id, name, desc, seq);
        boolean ok = DataStore.getNetwork().updateStop(stop);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Failed to update stop.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
        JOptionPane.showMessageDialog(this, "Stop '" + id + "' updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRemove(ActionEvent e) {
        int sel = stopTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Select a stop to remove.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String stopId = (String) tableModel.getValueAt(sel, 0);
        boolean ok = DataStore.getNetwork().removeStop(stopId);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Cannot remove stop; it may be referenced by roads, trips, or requests.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
        JOptionPane.showMessageDialog(this, "Stop '" + stopId + "' removed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshTable() {
        List<Stop> stops = DataStore.getNetwork().getStops();
        tableModel.setRowCount(0);
        for (Stop s : stops) {
            tableModel.addRow(new Object[]{s.getId(), s.getName(), s.getDescription()});
        }
    }
}
