import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * TripSeatTracker manages real-time, trip-specific stop-by-stop passenger
 * boarding, alighting, seat calculations, and overbooking prevention using SQLite.
 *
 * Formula:
 * available seats = previous available seats + passengers getting down - passengers boarding
 */
public class TripSeatTracker {

    public static class SegmentStatus {
        public final String stopId;
        public final int stopOrder;
        public final int boarding;
        public final int alighting;
        public final int occupied;
        public final int available;
        public final int capacity;

        public SegmentStatus(String stopId, int stopOrder, int boarding, int alighting,
                             int occupied, int available, int capacity) {
            this.stopId = stopId;
            this.stopOrder = stopOrder;
            this.boarding = boarding;
            this.alighting = alighting;
            this.occupied = occupied;
            this.available = available;
            this.capacity = capacity;
        }
    }

    /**
     * Resolves the capacity for a given trip. Defaults to 40 if not found.
     */
    public static int getCapacity(String tripId) {
        String sql = "SELECT b.capacity FROM trips t JOIN buses b ON t.bus_id = b.id WHERE t.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("capacity");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error reading trip capacity: " + ex.getMessage());
        }
        return 40;
    }

    /**
     * Retrieves stop-by-stop records for a trip ordered by stop_order ASC.
     */
    public static List<TripStopRecord> getStopRecords(String tripId) throws SQLException {
        List<TripStopRecord> list = new ArrayList<>();
        String sql = "SELECT id, trip_id, stop_id, stop_order, boarding_count, alighting_count, seats_available " +
                "FROM trip_stop_records WHERE trip_id = ? ORDER BY stop_order ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TripStopRecord(
                            rs.getInt("id"),
                            rs.getString("trip_id"),
                            rs.getString("stop_id"),
                            rs.getInt("stop_order"),
                            rs.getInt("boarding_count"),
                            rs.getInt("alighting_count"),
                            rs.getInt("seats_available")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Gets available seats at a specific stop for a given trip.
     */
    public static int getAvailableSeatsAtStop(String tripId, String stopId) {
        try {
            List<TripStopRecord> records = getStopRecords(tripId);
            for (TripStopRecord r : records) {
                if (r.getStopId().equals(stopId)) {
                    return r.getSeatsAvailable();
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error reading stop seats: " + ex.getMessage());
        }
        return getCapacity(tripId);
    }

    /**
     * Checks if a reservation can be accommodated without causing overbooking
     * or negative seat availability on any affected segment.
     */
    public static synchronized boolean canReserve(String tripId, String sourceStopId, String destStopId, int passengerCount) {
        if (passengerCount <= 0) return false;
        try {
            List<TripStopRecord> records = getStopRecords(tripId);
            if (records.isEmpty()) return false;

            int sourceOrder = -1;
            int destOrder = -1;
            for (TripStopRecord r : records) {
                if (r.getStopId().equals(sourceStopId)) sourceOrder = r.getStopOrder();
                if (r.getStopId().equals(destStopId)) destOrder = r.getStopOrder();
            }

            // Destinations must occur strictly after boarding stop
            if (sourceOrder == -1 || destOrder == -1 || sourceOrder >= destOrder) {
                return false;
            }

            // Every intermediate segment from sourceOrder to destOrder - 1 must have sufficient seats
            for (TripStopRecord r : records) {
                if (r.getStopOrder() >= sourceOrder && r.getStopOrder() < destOrder) {
                    if (r.getSeatsAvailable() < passengerCount) {
                        return false; // Insufficient seats on this segment
                    }
                }
            }
            return true;
        } catch (SQLException ex) {
            System.err.println("Error checking reservation feasibility: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Accepts a pending passenger request:
     * 1. Validates that the request is currently Pending.
     * 2. Checks that every affected route segment has sufficient available seats.
     * 3. Atomically updates request status to 'Accepted' in SQLite.
     * 4. Updates boarding count at source and alighting count at destination.
     * 5. Recalculates available seats along the entire route using the formula:
     *    available seats = previous available seats + passengers getting down - passengers boarding
     */
    public static synchronized boolean acceptRequest(String tripId, String requestId) throws SQLException {
        PassengerRequest req = null;
        String querySql = "SELECT id, source_stop_id, destination_stop_id, passenger_count, status FROM passenger_requests WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    req = new PassengerRequest(
                            rs.getString("id"),
                            rs.getString("source_stop_id"),
                            rs.getString("destination_stop_id"),
                            rs.getInt("passenger_count"),
                            "Medium", 0
                    );
                    req.setStatus(rs.getString("status"));
                }
            }
        }

        if (req == null) {
            throw new IllegalArgumentException("Request not found: " + requestId);
        }

        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalStateException("Only Pending requests can be accepted. Current status: " + req.getStatus());
        }

        if (!canReserve(tripId, req.getSourceStopId(), req.getDestinationStopId(), req.getPassengerCount())) {
            return false;
        }

        int capacity = getCapacity(tripId);
        List<TripStopRecord> records = getStopRecords(tripId);

        // Update boarding and alighting
        for (TripStopRecord r : records) {
            if (r.getStopId().equals(req.getSourceStopId())) {
                r.setBoardingCount(r.getBoardingCount() + req.getPassengerCount());
            }
            if (r.getStopId().equals(req.getDestinationStopId())) {
                r.setAlightingCount(r.getAlightingCount() + req.getPassengerCount());
            }
        }

        // Apply formula
        applySeatCalculationFormula(capacity, records);

        // Transactional commit to SQLite
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update request status to Accepted
                String updateReq = "UPDATE passenger_requests SET status = 'Accepted' WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateReq)) {
                    ps.setString(1, requestId);
                    ps.executeUpdate();
                }

                // Update trip stop records
                String updateRecords = "UPDATE trip_stop_records SET boarding_count = ?, alighting_count = ?, seats_available = ? " +
                        "WHERE trip_id = ? AND stop_order = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateRecords)) {
                    for (TripStopRecord r : records) {
                        ps.setInt(1, r.getBoardingCount());
                        ps.setInt(2, r.getAlightingCount());
                        ps.setInt(3, r.getSeatsAvailable());
                        ps.setString(4, tripId);
                        ps.setInt(5, r.getStopOrder());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Cancels a passenger request:
     * - If it was Accepted/Assigned, releases the reserved seats across affected segments.
     * - Updates request status to 'Cancelled' in SQLite.
     */
    public static synchronized boolean cancelRequest(String tripId, String requestId) throws SQLException {
        PassengerRequest req = null;
        String querySql = "SELECT id, source_stop_id, destination_stop_id, passenger_count, status FROM passenger_requests WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(querySql)) {
            ps.setString(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    req = new PassengerRequest(
                            rs.getString("id"),
                            rs.getString("source_stop_id"),
                            rs.getString("destination_stop_id"),
                            rs.getInt("passenger_count"),
                            "Medium", 0
                    );
                    req.setStatus(rs.getString("status"));
                }
            }
        }

        if (req == null) return false;

        boolean wasAccepted = "Accepted".equalsIgnoreCase(req.getStatus()) || "Assigned".equalsIgnoreCase(req.getStatus());
        int capacity = getCapacity(tripId);
        List<TripStopRecord> records = getStopRecords(tripId);

        if (wasAccepted) {
            // Deduct boarding and alighting
            for (TripStopRecord r : records) {
                if (r.getStopId().equals(req.getSourceStopId())) {
                    r.setBoardingCount(Math.max(0, r.getBoardingCount() - req.getPassengerCount()));
                }
                if (r.getStopId().equals(req.getDestinationStopId())) {
                    r.setAlightingCount(Math.max(0, r.getAlightingCount() - req.getPassengerCount()));
                }
            }
            applySeatCalculationFormula(capacity, records);
        }

        // Transactional commit
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String updateReq = "UPDATE passenger_requests SET status = 'Cancelled' WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateReq)) {
                    ps.setString(1, requestId);
                    ps.executeUpdate();
                }

                if (wasAccepted) {
                    String updateRecords = "UPDATE trip_stop_records SET boarding_count = ?, alighting_count = ?, seats_available = ? " +
                            "WHERE trip_id = ? AND stop_order = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateRecords)) {
                        for (TripStopRecord r : records) {
                            ps.setInt(1, r.getBoardingCount());
                            ps.setInt(2, r.getAlightingCount());
                            ps.setInt(3, r.getSeatsAvailable());
                            ps.setString(4, tripId);
                            ps.setInt(5, r.getStopOrder());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Executes the exact Phase 2 demonstration:
     * - Erode: 5 board -> 35 seats
     * - Nandha College: 10 board, 2 get down -> 27 seats
     * - Perundurai: 3 get down -> 30 seats
     * - Kongu College: all remaining passengers (10) get down -> 40 seats.
     */
    public static synchronized void runCorridorDemoFlow(String tripId) throws SQLException {
        int capacity = getCapacity(tripId);
        List<TripStopRecord> records = getStopRecords(tripId);
        if (records.size() < 4) return;

        // Stop 1: Erode (Order 1) - 5 board, 0 alight
        records.get(0).setBoardingCount(5);
        records.get(0).setAlightingCount(0);

        // Stop 2: Nandha College (Order 2) - 10 board, 2 alight
        records.get(1).setBoardingCount(10);
        records.get(1).setAlightingCount(2);

        // Stop 3: Perundurai (Order 3) - 0 board, 3 alight
        records.get(2).setBoardingCount(0);
        records.get(2).setAlightingCount(3);

        // Stop 4: Kongu College (Order 4) - All remaining passengers (10) alight
        records.get(3).setBoardingCount(0);
        records.get(3).setAlightingCount(10);

        applySeatCalculationFormula(capacity, records);

        // Save in SQLite transaction
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlTrip = "UPDATE trips SET status = 'In-Progress' WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlTrip)) {
                    ps.setString(1, tripId);
                    ps.executeUpdate();
                }

                String updateRecords = "UPDATE trip_stop_records SET boarding_count = ?, alighting_count = ?, seats_available = ? " +
                        "WHERE trip_id = ? AND stop_order = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateRecords)) {
                    for (TripStopRecord r : records) {
                        ps.setInt(1, r.getBoardingCount());
                        ps.setInt(2, r.getAlightingCount());
                        ps.setInt(3, r.getSeatsAvailable());
                        ps.setString(4, tripId);
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
    }

    /**
     * Resets a trip to full capacity (e.g. 40 seats available at all stops).
     */
    public static synchronized void resetTrip(String tripId) throws SQLException {
        int capacity = getCapacity(tripId);
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlTrip = "UPDATE trips SET status = 'Scheduled' WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlTrip)) {
                    ps.setString(1, tripId);
                    ps.executeUpdate();
                }

                String sqlRecords = "UPDATE trip_stop_records SET boarding_count = 0, alighting_count = 0, seats_available = ? " +
                        "WHERE trip_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlRecords)) {
                    ps.setInt(1, capacity);
                    ps.setString(2, tripId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Completes the trip:
     * - All remaining passengers alight.
     * - Trip status is set to 'Completed'.
     * - Marks accepted passenger requests as 'Completed'.
     * - Seats reset to full capacity for the next service.
     */
    public static synchronized void completeTrip(String tripId) throws SQLException {
        int capacity = getCapacity(tripId);
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlTrip = "UPDATE trips SET status = 'Completed' WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlTrip)) {
                    ps.setString(1, tripId);
                    ps.executeUpdate();
                }

                // Mark accepted requests as Completed
                String sqlReqs = "UPDATE passenger_requests SET status = 'Completed' WHERE status IN ('Accepted', 'Assigned')";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sqlReqs);
                }

                // Reset records for next scheduled service
                String sqlRecords = "UPDATE trip_stop_records SET boarding_count = 0, alighting_count = 0, seats_available = ? WHERE trip_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlRecords)) {
                    ps.setInt(1, capacity);
                    ps.setString(2, tripId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public static synchronized void completeTrip(String tripId, int capacity) throws SQLException {
        completeTrip(tripId);
    }

    public static synchronized boolean reserveSeats(String tripId, String sourceStopId, String destStopId, int passengerCount) throws SQLException {
        if (!canReserve(tripId, sourceStopId, destStopId, passengerCount)) {
            return false;
        }
        int capacity = getCapacity(tripId);
        List<TripStopRecord> records = getStopRecords(tripId);

        for (TripStopRecord r : records) {
            if (r.getStopId().equals(sourceStopId)) {
                r.setBoardingCount(r.getBoardingCount() + passengerCount);
            }
            if (r.getStopId().equals(destStopId)) {
                r.setAlightingCount(r.getAlightingCount() + passengerCount);
            }
        }

        applySeatCalculationFormula(capacity, records);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String updateRecords = "UPDATE trip_stop_records SET boarding_count = ?, alighting_count = ?, seats_available = ? " +
                        "WHERE trip_id = ? AND stop_order = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateRecords)) {
                    for (TripStopRecord r : records) {
                        ps.setInt(1, r.getBoardingCount());
                        ps.setInt(2, r.getAlightingCount());
                        ps.setInt(3, r.getSeatsAvailable());
                        ps.setString(4, tripId);
                        ps.setInt(5, r.getStopOrder());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Core calculation formula:
     * available seats = previous available seats + passengers getting down - passengers boarding
     */
    public static void applySeatCalculationFormula(int capacity, List<TripStopRecord> records) {
        int previousAvailable = capacity;
        for (int i = 0; i < records.size(); i++) {
            TripStopRecord r = records.get(i);
            int gettingDown = r.getAlightingCount();
            int boarding = r.getBoardingCount();

            int avail;
            if (i == records.size() - 1) {
                // Final destination stop (e.g. Kongu College):
                // All remaining passengers on board get down, so bus is emptied back to full capacity
                avail = capacity;
            } else {
                avail = previousAvailable + gettingDown - boarding;
                avail = Math.max(0, Math.min(capacity, avail));
                previousAvailable = avail;
            }
            r.setSeatsAvailable(avail);
        }
    }

    /**
     * Retrieves high-level segment status for display.
     */
    public static List<SegmentStatus> getSegmentStatuses(String tripId) throws SQLException {
        int capacity = getCapacity(tripId);
        List<TripStopRecord> records = getStopRecords(tripId);
        List<SegmentStatus> list = new ArrayList<>();

        for (TripStopRecord r : records) {
            int occ = Math.max(0, capacity - r.getSeatsAvailable());
            list.add(new SegmentStatus(
                    r.getStopId(),
                    r.getStopOrder(),
                    r.getBoardingCount(),
                    r.getAlightingCount(),
                    occ,
                    r.getSeatsAvailable(),
                    capacity
            ));
        }
        return list;
    }
}
