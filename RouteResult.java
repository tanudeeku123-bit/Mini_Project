import java.util.*;

public class RouteResult {
    private List<Road> path;
    private int totalTime;

    public RouteResult(List<Road> path, int totalTime) {
        this.path = path;
        this.totalTime = totalTime;
    }

    public List<Road> getPath() {
        return path;
    }

    public int getTotalTime() {
        return totalTime;
    }
}
