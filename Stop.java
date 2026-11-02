public class Stop {
    private String id;
    private String name;
    private String description;
    private int sequenceOrder;

    public Stop(String id, String name, String description) {
        this(id, name, description, 0);
    }

    public Stop(String id, String name, String description, int sequenceOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sequenceOrder = sequenceOrder;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getSequenceOrder() { return sequenceOrder; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
}
