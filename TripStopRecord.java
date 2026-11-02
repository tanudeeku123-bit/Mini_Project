public class TripStopRecord {
    private int id;
    private String tripId;
    private String stopId;
    private int stopOrder;
    private int boardingCount;
    private int alightingCount;
    private int seatsAvailable;

    public TripStopRecord(int id, String tripId, String stopId, int stopOrder,
                          int boardingCount, int alightingCount, int seatsAvailable) {
        this.id = id;
        this.tripId = tripId;
        this.stopId = stopId;
        this.stopOrder = stopOrder;
        this.boardingCount = boardingCount;
        this.alightingCount = alightingCount;
        this.seatsAvailable = seatsAvailable;
    }

    public TripStopRecord(String tripId, String stopId, int stopOrder,
                          int boardingCount, int alightingCount, int seatsAvailable) {
        this(0, tripId, stopId, stopOrder, boardingCount, alightingCount, seatsAvailable);
    }

    public int getId() { return id; }
    public String getTripId() { return tripId; }
    public String getStopId() { return stopId; }
    public int getStopOrder() { return stopOrder; }
    public int getBoardingCount() { return boardingCount; }
    public int getAlightingCount() { return alightingCount; }
    public int getSeatsAvailable() { return seatsAvailable; }

    public void setId(int id) { this.id = id; }
    public void setBoardingCount(int count) { this.boardingCount = count; }
    public void setAlightingCount(int count) { this.alightingCount = count; }
    public void setSeatsAvailable(int seats) { this.seatsAvailable = seats; }
}

