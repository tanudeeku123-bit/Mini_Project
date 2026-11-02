public class DisruptionRecord {
    private int id;
    private String type;
    private String target;
    private String action;
    private String result;
    private String timestamp;

    public DisruptionRecord(int id, String type, String target, String action, String result, String timestamp) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.action = action;
        this.result = result;
        this.timestamp = timestamp;
    }

    public DisruptionRecord(String type, String target, String action, String result) {
        this(0, type, target, action, result, null);
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public String getTarget() { return target; }
    public String getAction() { return action; }
    public String getResult() { return result; }
    public String getTimestamp() { return timestamp; }
}

