import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete result and explanation of evaluating bus allocation
 * for a specific passenger journey from source to destination.
 */
public class AllocationResult {
    private final boolean allocated;
    private final Bus bus;
    private final int passengerCount;
    private final int busCapacity;
    private final int remainingSeats;
    private final List<Road> route;
    private final int travelTime;
    private final String explanation;
    private final String failureReason;

    public AllocationResult(boolean allocated, Bus bus, int passengerCount,
                            int busCapacity, int remainingSeats, List<Road> route,
                            int travelTime, String explanation, String failureReason) {
        this.allocated = allocated;
        this.bus = bus;
        this.passengerCount = passengerCount;
        this.busCapacity = busCapacity;
        this.remainingSeats = remainingSeats;
        this.route = route;
        this.travelTime = travelTime;
        this.explanation = explanation;
        this.failureReason = failureReason;
    }

    public boolean isAllocated() {
        return allocated;
    }

    public Bus getBus() {
        return bus;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public int getBusCapacity() {
        return busCapacity;
    }

    public int getRemainingSeats() {
        return remainingSeats;
    }

    public List<Road> getRoute() {
        return route != null ? Collections.unmodifiableList(route) : Collections.emptyList();
    }

    public int getTravelTime() {
        return travelTime;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getFailureReason() {
        return failureReason;
    }
}

