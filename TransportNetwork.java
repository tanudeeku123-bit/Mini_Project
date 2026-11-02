import java.awt.GraphicsEnvironment;
import java.sql.SQLException;
import java.util.*;
import javax.swing.JOptionPane;

public class TransportNetwork {
    private List<Stop> stops = new ArrayList<>();
    private Map<String, Stop> stopById = new HashMap<>();

    private List<Road> roads = new ArrayList<>();
    private Map<String, Road> roadKeyMap = new HashMap<>(); // key: startId+"->"+endId

    private List<Bus> buses = new ArrayList<>();
    private Map<String, Bus> busById = new HashMap<>();

    private List<PassengerRequest> requests = new ArrayList<>();
    private Map<String, PassengerRequest> requestById = new HashMap<>();

    private List<AllocationRecord> allocations = new ArrayList<>();
    private boolean allocationDone = false;

    private List<Trip> trips = new ArrayList<>();
    private Map<String, Trip> tripById = new HashMap<>();

    public void clearNetwork() {
        stops.clear();
        stopById.clear();
        roads.clear();
        roadKeyMap.clear();
        buses.clear();
        busById.clear();
        requests.clear();
        requestById.clear();
        allocations.clear();
        trips.clear();
        tripById.clear();
        allocationDone = false;
    }

    private void showDatabaseError(String message) {
        System.err.println("Database Error: " + message);
        if (!GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, message, "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------------
    // DB Hydration Methods (called when loading from SQLite)
    // -------------------------------------------------------------
    public void loadStop(Stop stop) {
        if (!stopById.containsKey(stop.getId())) {
            stops.add(stop);
            stopById.put(stop.getId(), stop);
        }
    }

    public void loadRoad(Road road) {
        String key = roadKey(road.getStartStopId(), road.getEndStopId());
        if (!roadKeyMap.containsKey(key)) {
            roads.add(road);
            roadKeyMap.put(key, road);
        }
    }

    public void loadBus(Bus bus) {
        if (!busById.containsKey(bus.getId())) {
            buses.add(bus);
            busById.put(bus.getId(), bus);
        }
    }

    public void loadRequest(PassengerRequest request) {
        if (!requestById.containsKey(request.getId())) {
            requests.add(request);
            requestById.put(request.getId(), request);
        }
    }

    public void loadAllocation(AllocationRecord ar) {
        allocations.add(ar);
        allocationDone = true;
    }

    public void loadTrip(Trip trip) {
        if (!tripById.containsKey(trip.getId())) {
            trips.add(trip);
            tripById.put(trip.getId(), trip);
        }
    }

    // -------------------------------------------------------------
    // Stop management
    // -------------------------------------------------------------
    public boolean addStop(Stop stop) {
        // Prevent duplicate stop IDs or duplicate names (case-insensitive)
        for (Stop s : stops) {
            if (s.getId().equalsIgnoreCase(stop.getId()) || s.getName().equalsIgnoreCase(stop.getName())) {
                return false;
            }
        }
        // Auto-assign sequence order if not specified
        if (stop.getSequenceOrder() <= 0) {
            int maxOrder = 0;
            for (Stop s : stops) {
                if (s.getSequenceOrder() > maxOrder) {
                    maxOrder = s.getSequenceOrder();
                }
            }
            stop.setSequenceOrder(maxOrder + 1);
        }
        try {
            DatabaseManager.insertStop(stop);
            DatabaseManager.addStopToActiveTrips(stop.getId(), stop.getSequenceOrder());
        } catch (SQLException ex) {
            showDatabaseError("Failed to save stop to database: " + ex.getMessage());
            return false;
        }
        stops.add(stop);
        stopById.put(stop.getId(), stop);
        return true;
    }

    public boolean updateStop(Stop stop) {
        Stop existing = stopById.get(stop.getId());
        if (existing == null) {
            return false;
        }
        try {
            DatabaseManager.updateStop(stop);
        } catch (SQLException ex) {
            showDatabaseError("Failed to update stop in database: " + ex.getMessage());
            return false;
        }
        existing.setName(stop.getName());
        existing.setDescription(stop.getDescription());
        existing.setSequenceOrder(stop.getSequenceOrder());
        return true;
    }

    public boolean removeStop(String stopId) {
        // Ensure no road or request references this stop
        for (Road r : roads) {
            if (r.getStartStopId().equalsIgnoreCase(stopId) || r.getEndStopId().equalsIgnoreCase(stopId)) {
                return false; // referenced
            }
        }
        for (PassengerRequest pr : requests) {
            if (pr.getSourceStopId().equalsIgnoreCase(stopId) || pr.getDestinationStopId().equalsIgnoreCase(stopId)) {
                return false;
            }
        }
        try {
            DatabaseManager.deleteStop(stopId);
        } catch (SQLException ex) {
            showDatabaseError("Failed to delete stop from database: " + ex.getMessage());
            return false;
        }
        Stop removed = stopById.remove(stopId);
        if (removed != null) {
            stops.remove(removed);
            return true;
        }
        return false;
    }

    public List<Stop> getStops() {
        return Collections.unmodifiableList(stops);
    }

    public Stop getStop(String id) {
        return stopById.get(id);
    }

    // -------------------------------------------------------------
    // Road management
    // -------------------------------------------------------------
    private String roadKey(String start, String end) {
        return start + "->" + end;
    }

    public boolean addRoad(Road road) {
        String key = roadKey(road.getStartStopId(), road.getEndStopId());
        if (roadKeyMap.containsKey(key)) {
            return false; // duplicate
        }
        // ensure stops exist
        if (!stopById.containsKey(road.getStartStopId()) || !stopById.containsKey(road.getEndStopId())) {
            return false;
        }
        try {
            DatabaseManager.insertRoad(road);
        } catch (SQLException ex) {
            showDatabaseError("Failed to save road to database: " + ex.getMessage());
            return false;
        }
        roads.add(road);
        roadKeyMap.put(key, road);
        return true;
    }

    public boolean updateRoad(String start, String end, int travelTime, RoadStatus status) {
        Road r = roadKeyMap.get(roadKey(start, end));
        if (r == null) return false;
        try {
            DatabaseManager.updateRoad(start, end, travelTime, status);
        } catch (SQLException ex) {
            showDatabaseError("Failed to update road in database: " + ex.getMessage());
            return false;
        }
        r.setTravelTime(travelTime);
        r.setStatus(status);
        return true;
    }

    public boolean removeRoad(String start, String end) {
        String key = roadKey(start, end);
        Road r = roadKeyMap.get(key);
        if (r == null) return false;
        try {
            DatabaseManager.deleteRoad(start, end);
        } catch (SQLException ex) {
            showDatabaseError("Failed to delete road from database: " + ex.getMessage());
            return false;
        }
        roads.remove(r);
        roadKeyMap.remove(key);
        return true;
    }

    public Road getRoad(String startId, String endId) {
        return roadKeyMap.get(startId + "->" + endId);
    }

    public List<Road> getRoads() {
        return Collections.unmodifiableList(roads);
    }

    // -------------------------------------------------------------
    // Bus management
    // -------------------------------------------------------------
    public boolean addBus(Bus bus) {
        if (busById.containsKey(bus.getId())) return false;
        try {
            DatabaseManager.insertBus(bus);
            DatabaseManager.ensureTripForBus(bus);
        } catch (SQLException ex) {
            showDatabaseError("Failed to save bus to database: " + ex.getMessage());
            return false;
        }
        buses.add(bus);
        busById.put(bus.getId(), bus);
        return true;
    }

    public boolean updateBusAvailability(String busId, boolean available) {
        Bus bus = busById.get(busId);
        if (bus == null) return false;
        try {
            DatabaseManager.updateBusAvailability(busId, available);
        } catch (SQLException ex) {
            showDatabaseError("Failed to update bus availability in database: " + ex.getMessage());
            return false;
        }
        bus.setAvailable(available);
        return true;
    }

    public List<Bus> getBuses() {
        return Collections.unmodifiableList(buses);
    }

    public Bus getBus(String id) {
        return busById.get(id);
    }

    // -------------------------------------------------------------
    // Passenger request management
    // -------------------------------------------------------------
    public boolean addRequest(PassengerRequest request) {
        if (requestById.containsKey(request.getId())) return false;
        // ensure stops exist
        if (!stopById.containsKey(request.getSourceStopId()) || !stopById.containsKey(request.getDestinationStopId())) {
            return false;
        }
        try {
            DatabaseManager.insertRequest(request);
        } catch (SQLException ex) {
            showDatabaseError("Failed to save passenger request to database: " + ex.getMessage());
            return false;
        }
        requests.add(request);
        requestById.put(request.getId(), request);
        return true;
    }

    public boolean removeRequest(String requestId) {
        PassengerRequest pr = requestById.get(requestId);
        if (pr == null) return false;
        try {
            DatabaseManager.deleteRequest(requestId);
        } catch (SQLException ex) {
            showDatabaseError("Failed to delete request from database: " + ex.getMessage());
            return false;
        }
        requestById.remove(requestId);
        requests.remove(pr);
        return true;
    }

    public List<PassengerRequest> getRequests() {
        return Collections.unmodifiableList(requests);
    }

    public boolean updateRequestStatus(String requestId, String status) {
        PassengerRequest pr = requestById.get(requestId);
        if (pr == null) return false;
        try {
            DatabaseManager.updateRequestStatus(requestId, status);
        } catch (SQLException ex) {
            showDatabaseError("Failed to update request status in database: " + ex.getMessage());
            return false;
        }
        pr.setStatus(status);
        return true;
    }

    // -------------------------------------------------------------
    // Trips & Stop-by-Stop Seat Records
    // -------------------------------------------------------------
    public List<Trip> getTrips() {
        return Collections.unmodifiableList(trips);
    }

    public void setTrips(List<Trip> trips) {
        this.trips = new ArrayList<>(trips);
        this.tripById.clear();
        for (Trip t : trips) {
            this.tripById.put(t.getId(), t);
        }
    }

    // -------------------------------------------------------------
    // Bus Allocation with Transactional Database Persistence
    // and Stop-by-Stop Seat Tracking
    // -------------------------------------------------------------
    public boolean doesBusCoverJourney(Bus bus, String sourceId, String destId, List<Trip> trips) {
        if (sourceId == null || destId == null || sourceId.equalsIgnoreCase(destId)) {
            return false;
        }
        // 1. Check trip stop records if this bus has an active trip
        if (trips != null) {
            for (Trip t : trips) {
                if (t.getBusId().equalsIgnoreCase(bus.getId())) {
                    List<TripStopRecord> recs = t.getStopRecords();
                    int srcOrder = -1;
                    int dstOrder = -1;
                    for (TripStopRecord r : recs) {
                        if (r.getStopId().equalsIgnoreCase(sourceId)) {
                            srcOrder = r.getStopOrder();
                        }
                        if (r.getStopId().equalsIgnoreCase(destId)) {
                            dstOrder = r.getStopOrder();
                        }
                    }
                    if (srcOrder != -1 && dstOrder != -1) {
                        return srcOrder < dstOrder; // Forward corridor direction only
                    }
                }
            }
        }
        // 2. Fallback: check sequence order in stopById
        Stop s1 = stopById.get(sourceId);
        Stop s2 = stopById.get(destId);
        if (s1 != null && s2 != null) {
            return s1.getSequenceOrder() < s2.getSequenceOrder();
        }
        return false;
    }

    /**
     * Evaluates bus allocation for a specific journey with clear, descriptive explanations.
     */
    public AllocationResult evaluateJourneyAllocation(String sourceId, String destId, int passengerCount) {
        if (sourceId == null || destId == null) {
            return new AllocationResult(false, null, passengerCount, 0, 0, null, -1,
                    "Source and destination stops must be selected.", "Invalid selection");
        }
        if (sourceId.equalsIgnoreCase(destId)) {
            return new AllocationResult(false, null, passengerCount, 0, 0, null, -1,
                    "Source and destination stops cannot be the same stop.", "Source and destination are identical");
        }

        // 1. Check Dijkstra path on OPEN roads
        RouteResult result = findShortestPath(sourceId, destId);
        if (result == null) {
            return new AllocationResult(false, null, passengerCount, 0, 0, null, -1,
                    "No reachable route between " + sourceId + " and " + destId + " because road segments are closed or missing.",
                    "Road blocked / Unreachable");
        }

        // 2. Load trips for coverage check
        List<Trip> currentTrips = new ArrayList<>();
        try {
            currentTrips = DatabaseManager.loadTrips();
        } catch (Exception ex) {
            System.err.println("Notice: Could not load trips: " + ex.getMessage());
        }

        // 3. Find candidate buses covering this route corridor in forward order
        List<Bus> coveringBuses = new ArrayList<>();
        for (Bus b : buses) {
            if (doesBusCoverJourney(b, sourceId, destId, currentTrips)) {
                coveringBuses.add(b);
            }
        }

        if (coveringBuses.isEmpty()) {
            return new AllocationResult(false, null, passengerCount, 0, 0, result.getPath(), result.getTotalTime(),
                    "No bus route covers journey from " + sourceId + " to " + destId + ". Active bus corridors do not serve this direction.",
                    "No bus route covers journey (Unserved corridor or reverse direction)");
        }

        // 4. Check operational availability
        List<Bus> availableBuses = new ArrayList<>();
        for (Bus b : coveringBuses) {
            if (b.isAvailable()) {
                availableBuses.add(b);
            }
        }

        if (availableBuses.isEmpty()) {
            Bus b = coveringBuses.get(0);
            return new AllocationResult(false, null, passengerCount, b.getCapacity(), 0, result.getPath(), result.getTotalTime(),
                    "Bus " + b.getId() + " covers this corridor but is currently UNAVAILABLE (Breakdown / Out-of-service).",
                    "Bus " + b.getId() + " is unavailable (Breakdown / Maintenance)");
        }

        // 5. Check capacity
        Bus selectedBus = null;
        for (Bus b : availableBuses) {
            if (b.getAvailableSeats() >= passengerCount) {
                selectedBus = b;
                break;
            }
        }

        if (selectedBus == null) {
            Bus b = availableBuses.get(0);
            return new AllocationResult(false, null, passengerCount, b.getCapacity(), b.getAvailableSeats(), result.getPath(), result.getTotalTime(),
                    "Bus " + b.getId() + " serves this corridor but has insufficient seats (Requested: " + passengerCount + ", Available: " + b.getAvailableSeats() + ").",
                    "Bus capacity full (Requested: " + passengerCount + ", Available: " + b.getAvailableSeats() + ")");
        }

        // 6. Successful allocation!
        int rem = selectedBus.getAvailableSeats() - passengerCount;
        String explanation = "Bus " + selectedBus.getId() + " (" + selectedBus.getType() + ") is allocated for route " +
                sourceId + " -> " + destId + ".\n" +
                "• Capacity: " + selectedBus.getCapacity() + " seats\n" +
                "• Remaining Seats: " + rem + " seats\n" +
                "• Estimated Travel Time: " + result.getTotalTime() + " minutes\n" +
                "• Status: Operational & Available";

        return new AllocationResult(true, selectedBus, passengerCount, selectedBus.getCapacity(), rem,
                result.getPath(), result.getTotalTime(), explanation, null);
    }

    public List<AllocationRecord> allocateBuses() {
        List<AllocationRecord> newAllocations = new ArrayList<>();
        List<Bus> busesToUpdate = new ArrayList<>();
        List<PassengerRequest> requestsToUpdate = new ArrayList<>();
        List<TripStopRecord> tripRecordsToUpdate = new ArrayList<>();

        // Create working copies of bus available seats and availability
        Map<String, Integer> tempBusSeats = new HashMap<>();
        Map<String, Boolean> tempBusAvail = new HashMap<>();
        for (Bus b : buses) {
            tempBusSeats.put(b.getId(), b.getAvailableSeats());
            tempBusAvail.put(b.getId(), b.isAvailable());
        }

        // Load current trip stop records if available
        List<Trip> currentTrips = new ArrayList<>();
        try {
            currentTrips = DatabaseManager.loadTrips();
        } catch (Exception ex) {
            System.err.println("Could not load trips for seat tracking: " + ex.getMessage());
        }

        // Sort requests by passenger count descending (priority)
        List<PassengerRequest> sortedRequests = new ArrayList<>(requests);
        sortedRequests.sort((a, b) -> Integer.compare(b.getPassengerCount(), a.getPassengerCount()));

        for (PassengerRequest pr : sortedRequests) {
            // Find shortest path using Dijkstra
            RouteResult result = findShortestPath(pr.getSourceStopId(), pr.getDestinationStopId());
            if (result == null) {
                newAllocations.add(new AllocationRecord(pr.getId(), null, pr.getPassengerCount(),
                        0, 0, null, -1, "No reachable route: Road segments closed or blocked"));
                continue;
            }

            // Check if any bus covers this journey corridor
            List<Bus> coveringBuses = new ArrayList<>();
            for (Bus bus : buses) {
                if (doesBusCoverJourney(bus, pr.getSourceStopId(), pr.getDestinationStopId(), currentTrips)) {
                    coveringBuses.add(bus);
                }
            }

            if (coveringBuses.isEmpty()) {
                newAllocations.add(new AllocationRecord(pr.getId(), null, pr.getPassengerCount(),
                        0, 0, result.getPath(), result.getTotalTime(),
                        "No bus route covers journey (" + pr.getSourceStopId() + " -> " + pr.getDestinationStopId() + ")"));
                continue;
            }

            // Find first available bus that covers the route and has enough capacity
            Bus selectedBus = null;
            boolean hadCoveringBus = false;
            boolean hadAvailableBus = false;
            int maxSeatsFound = 0;

            for (Bus bus : coveringBuses) {
                hadCoveringBus = true;
                boolean isAvail = tempBusAvail.getOrDefault(bus.getId(), false);
                int seats = tempBusSeats.getOrDefault(bus.getId(), 0);
                if (isAvail) {
                    hadAvailableBus = true;
                    if (seats > maxSeatsFound) maxSeatsFound = seats;
                    if (seats >= pr.getPassengerCount()) {
                        selectedBus = bus;
                        break;
                    }
                }
            }

            if (selectedBus == null) {
                String failReason;
                if (!hadAvailableBus) {
                    failReason = "Assigned bus " + coveringBuses.get(0).getId() + " is unavailable (Breakdown / Maintenance)";
                } else {
                    failReason = "Bus capacity full (Requested: " + pr.getPassengerCount() + ", Available: " + maxSeatsFound + ")";
                }
                newAllocations.add(new AllocationRecord(pr.getId(), null, pr.getPassengerCount(),
                        0, 0, result.getPath(), result.getTotalTime(), failReason));
                continue;
            }

            // Deduct seats and update bus state
            int remainingSeats = tempBusSeats.get(selectedBus.getId()) - pr.getPassengerCount();
            tempBusSeats.put(selectedBus.getId(), remainingSeats);
            if (remainingSeats == 0) {
                tempBusAvail.put(selectedBus.getId(), false);
            }

            // Record request update
            pr.setStatus("Assigned");
            requestsToUpdate.add(pr);

            newAllocations.add(new AllocationRecord(pr.getId(), selectedBus.getId(),
                    pr.getPassengerCount(), selectedBus.getCapacity(),
                    remainingSeats, result.getPath(),
                    result.getTotalTime(), null));

            // Stop-by-stop seat tracking calculation on trip
            for (Trip trip : currentTrips) {
                if (trip.getBusId().equals(selectedBus.getId())) {
                    List<TripStopRecord> stopRecords = trip.getStopRecords();
                    boolean inJourney = false;
                    for (TripStopRecord tsr : stopRecords) {
                        if (tsr.getStopId().equals(pr.getSourceStopId())) {
                            tsr.setBoardingCount(tsr.getBoardingCount() + pr.getPassengerCount());
                            inJourney = true;
                        }
                        if (inJourney) {
                            int newAvail = Math.max(0, tsr.getSeatsAvailable() - pr.getPassengerCount());
                            tsr.setSeatsAvailable(newAvail);
                            tripRecordsToUpdate.add(tsr);
                        }
                        if (tsr.getStopId().equals(pr.getDestinationStopId())) {
                            tsr.setAlightingCount(tsr.getAlightingCount() + pr.getPassengerCount());
                            inJourney = false;
                        }
                    }
                }
            }
        }

        // Prepare bus list for DB update
        for (Bus b : buses) {
            int newSeats = tempBusSeats.get(b.getId());
            boolean newAvail = tempBusAvail.get(b.getId());
            if (newSeats != b.getAvailableSeats() || newAvail != b.isAvailable()) {
                Bus updatedBus = new Bus(b.getId(), b.getType(), b.getCapacity());
                updatedBus.setAvailableSeats(newSeats);
                updatedBus.setAvailable(newAvail);
                busesToUpdate.add(updatedBus);
            }
        }

        // Commit transaction to SQLite
        try {
            DatabaseManager.saveAllocationsTransaction(newAllocations, busesToUpdate, requestsToUpdate, tripRecordsToUpdate);
        } catch (SQLException ex) {
            showDatabaseError("Failed to save bus allocation transaction to database: " + ex.getMessage());
            return new ArrayList<>(allocations);
        }

        // Apply changes to in-memory state only upon successful DB transaction
        for (Bus updated : busesToUpdate) {
            Bus inMemory = busById.get(updated.getId());
            if (inMemory != null) {
                inMemory.setAvailableSeats(updated.getAvailableSeats());
                inMemory.setAvailable(updated.isAvailable());
            }
        }
        allocations.clear();
        allocations.addAll(newAllocations);
        allocationDone = true;

        return new ArrayList<>(allocations);
    }

    public List<AllocationRecord> getAllocations() {
        return Collections.unmodifiableList(allocations);
    }

    // -------------------------------------------------------------
    // Dijkstra shortest path (ignores CLOSED roads)
    // -------------------------------------------------------------
    public RouteResult findShortestPath(String sourceId, String destId) {
        if (!stopById.containsKey(sourceId) || !stopById.containsKey(destId)) {
            return null;
        }
        // Build adjacency list
        Map<String, List<Road>> adj = new HashMap<>();
        for (Road r : roads) {
            if (r.getStatus() == RoadStatus.CLOSED) continue;
            adj.computeIfAbsent(r.getStartStopId(), k -> new ArrayList<>()).add(r);
        }
        // Dijkstra algorithm
        Map<String, Integer> dist = new HashMap<>();
        Map<String, Road> prevRoad = new HashMap<>();
        for (Stop s : stops) {
            dist.put(s.getId(), Integer.MAX_VALUE);
        }
        dist.put(sourceId, 0);
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(sourceId);
        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(destId)) break;
            List<Road> outs = adj.getOrDefault(u, Collections.emptyList());
            for (Road r : outs) {
                String v = r.getEndStopId();
                int alt = dist.get(u) + r.getTravelTime();
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prevRoad.put(v, r);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }
        if (dist.get(destId) == Integer.MAX_VALUE) {
            return null; // no route
        }
        // Reconstruct path
        List<Road> path = new ArrayList<>();
        String cur = destId;
        while (!cur.equals(sourceId)) {
            Road r = prevRoad.get(cur);
            if (r == null) break;
            path.add(0, r);
            cur = r.getStartStopId();
        }
        return new RouteResult(path, dist.get(destId));
    }
}
