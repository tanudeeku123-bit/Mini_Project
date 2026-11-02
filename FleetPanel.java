import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class FleetPanel extends JPanel {
    private JTable busTable;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnClear;
    private JTextField txtId, txtType, txtCapacity;

    public FleetPanel() {
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
        form.add(new JLabel("Bus ID:"), gbc);
        gbc.gridx = 1;
        txtId = new JTextField(10);
        form.add(txtId, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        txtType = new JTextField(10);
        form.add(txtType, gbc);

        gbc.gridx = 0; gbc.gridy++;
        form.add(new JLabel("Capacity:"), gbc);
        gbc.gridx = 1;
        txtCapacity = new JTextField(5);
        form.add(txtCapacity, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        btnAdd = new JButton("Add Bus");
        btnClear = new JButton("Clear Form");
        btnPanel.add(btnAdd);
        btnPanel.add(btnClear);
        form.add(btnPanel, gbc);

        add(form, BorderLayout.NORTH);

        btnAdd.addActionListener(this::handleAdd);
        btnClear.addActionListener(e -> clearForm());
    }

    private void initTable() {
        tableModel = new DefaultTableModel(new Object[]{"ID","Type","Capacity","Available"},0){
            public boolean isCellEditable(int row, int col){return false;}
        };
        busTable = new JTable(tableModel);
        JScrollPane sp = new JScrollPane(busTable);
        sp.setPreferredSize(new Dimension(500,200));
        add(sp, BorderLayout.CENTER);
    }

    private void clearForm(){
        txtId.setText("");
        txtType.setText("");
        txtCapacity.setText("");
        if (busTable != null) busTable.clearSelection();
    }

    private void handleAdd(ActionEvent e){
        String id = txtId.getText().trim();
        String type = txtType.getText().trim();
        String capStr = txtCapacity.getText().trim();
        if (id.isEmpty() || type.isEmpty() || capStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int capacity;
        try {
            capacity = Integer.parseInt(capStr);
            if (capacity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Capacity must be a positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Bus bus = new Bus(id, type, capacity);
        boolean ok = DataStore.getNetwork().addBus(bus);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Bus ID already exists.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        refreshTable();
        MainFrame.getInstance().refreshDashboard();
        clearForm();
    }

    public void refreshTable(){
        List<Bus> buses = DataStore.getNetwork().getBuses();
        tableModel.setRowCount(0);
        for (Bus b : buses) {
            tableModel.addRow(new Object[]{b.getId(), b.getType(), b.getCapacity(), b.isAvailable() ? "Yes" : "No"});
        }
    }
}
