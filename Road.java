public class Road {
    private String startStopId;
    private String endStopId;
    private int travelTime; // in minutes
    private RoadStatus status;

    public Road(String startStopId, String endStopId, int travelTime, RoadStatus status) {
        this.startStopId = startStopId;
        this.endStopId = endStopId;
        this.travelTime = travelTime;
        this.status = status;
    }

    public String getStartStopId() { return startStopId; }
    public String getEndStopId() { return endStopId; }
    public int getTravelTime() { return travelTime; }
    public RoadStatus getStatus() { return status; }

    public void setTravelTime(int travelTime) { this.travelTime = travelTime; }
    public void setStatus(RoadStatus status) { this.status = status; }
}
