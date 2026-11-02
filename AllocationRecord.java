import java.util.List;

public class AllocationRecord {
    private final String requestId;
    private final String busId; // may be null if not allocated
    private final int passengerCount;
    private final int busCapacity;
    private final int remainingSeats;
    private final List<Road> route; // may be null if no route
    private final int travelTime; // total minutes, -1 if no route
    private final String reason; // non-null if allocation failed

    public AllocationRecord(String requestId, String busId, int passengerCount, int busCapacity,
                            int remainingSeats, List<Road> route, int travelTime, String reason) {
        this.requestId = requestId;
        this.busId = busId;
        this.passengerCount = passengerCount;
        this.busCapacity = busCapacity;
        this.remainingSeats = remainingSeats;
        this.route = route;
        this.travelTime = travelTime;
        this.reason = reason;
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getBusId() { return busId; }
    public int getPassengerCount() { return passengerCount; }
    public int getBusCapacity() { return busCapacity; }
    public int getRemainingSeats() { return remainingSeats; }
    public List<Road> getRoute() { return route; }
    public int getTravelTime() { return travelTime; }
    public String getReason() { return reason; }
}
