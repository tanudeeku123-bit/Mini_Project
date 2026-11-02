import java.util.ArrayList;
import java.util.List;

public class Trip {
    private String id;
    private String busId;
    private String tripName;
    private String status; // Scheduled, In-Progress, Completed
    private List<TripStopRecord> stopRecords = new ArrayList<>();

    public Trip(String id, String busId, String tripName, String status) {
        this.id = id;
        this.busId = busId;
        this.tripName = tripName;
        this.status = status;
    }

    public String getId() { return id; }
    public String getBusId() { return busId; }
    public String getTripName() { return tripName; }
    public String getStatus() { return status; }
    public List<TripStopRecord> getStopRecords() { return stopRecords; }

    public void setBusId(String busId) { this.busId = busId; }
    public void setTripName(String tripName) { this.tripName = tripName; }
    public void setStatus(String status) { this.status = status; }
    public void setStopRecords(List<TripStopRecord> records) { this.stopRecords = records; }
}

