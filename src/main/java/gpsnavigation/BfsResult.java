package gpsnavigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of destination-aware BFS. BFS minimises road connections, not kilometres. */
public final class BfsResult {
    private final List<String> traversalOrder;
    private final List<String> route;
    private final int roadConnections;

    BfsResult(List<String> traversalOrder, List<String> route, int roadConnections) {
        this.traversalOrder = Collections.unmodifiableList(new ArrayList<>(traversalOrder));
        this.route = Collections.unmodifiableList(new ArrayList<>(route));
        this.roadConnections = roadConnections;
    }

    public List<String> getTraversalOrder() {
        return traversalOrder;
    }

    public List<String> getRoute() {
        return route;
    }

    public int getRoadConnections() {
        return roadConnections;
    }

    public boolean isFound() {
        return !route.isEmpty();
    }
}
