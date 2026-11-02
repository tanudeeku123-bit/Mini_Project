import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DataStore {
    private static final TransportNetwork network = new TransportNetwork();
    private static boolean initialized = false;

    private DataStore() {}

    public static synchronized TransportNetwork getNetwork() {
        if (!initialized) {
            initDataStore();
        }
        return network;
    }

    public static synchronized void initDataStore() {
        if (initialized) return;
        try {
            DatabaseManager.init();
            DatabaseManager.loadNetwork(network);
            initialized = true;
            System.out.println("DataStore loaded " + network.getStops().size() + " stops, " +
                    network.getRoads().size() + " roads, " +
                    network.getBuses().size() + " buses, and " +
                    network.getRequests().size() + " requests from SQLite.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            String errorMsg = "Critical Database Error: Could not connect to or initialize the local SQLite database at:\n"
                    + DatabaseManager.getDatabasePath() + "\n\nError details: " + ex.getMessage();
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, errorMsg, "Database Connection Failed", JOptionPane.ERROR_MESSAGE);
            }
            throw new RuntimeException("Database initialization failed: " + ex.getMessage(), ex);
        }
    }

    public static synchronized void reloadFromDatabase() {
        try {
            DatabaseManager.loadNetwork(network);
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(null, "Failed to reload from database: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
