public class Bus {
    private String id;
    private String type;
    private int capacity;
    private int availableSeats;
    private boolean available;

    public Bus(String id, String type, int capacity) {
        this.id = id;
        this.type = type;
        this.capacity = capacity;
        this.availableSeats = capacity;
        this.available = true;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public int getAvailableSeats() { return availableSeats; }
    public boolean isAvailable() { return available; }

    public void setAvailable(boolean available) { this.available = available; }
    public void setAvailableSeats(int seats) { this.availableSeats = seats; }
}
