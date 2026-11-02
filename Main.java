public class Main {
    public static void main(String[] args) {
        // Initialize local SQLite database before creating Swing UI
        try {
            DataStore.initDataStore();
        } catch (Exception ex) {
            System.err.println("Fatal: Database initialization failed. " + ex.getMessage());
            System.exit(1);
        }

        // Ensure UI updates are performed on the Event Dispatch Thread
        javax.swing.SwingUtilities.invokeLater(() -> {
            new MainFrame();
        });
    }
}
