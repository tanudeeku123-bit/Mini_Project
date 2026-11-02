public class PassengerRequest {
    private String id;
    private String sourceStopId;
    private String destinationStopId;
    private int passengerCount;
    private String urgencyLevel;
    private int waitingTimeMinutes;
    private String status; // Pending, Assigned, Completed

    public PassengerRequest(String id, String sourceStopId, String destinationStopId, int passengerCount,
                            String urgencyLevel, int waitingTimeMinutes) {
        this.id = id;
        this.sourceStopId = sourceStopId;
        this.destinationStopId = destinationStopId;
        this.passengerCount = passengerCount;
        this.urgencyLevel = urgencyLevel;
        this.waitingTimeMinutes = waitingTimeMinutes;
        this.status = "Pending";
    }

    public String getId() { return id; }
    public String getSourceStopId() { return sourceStopId; }
    public String getDestinationStopId() { return destinationStopId; }
    public int getPassengerCount() { return passengerCount; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public int getWaitingTimeMinutes() { return waitingTimeMinutes; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}
