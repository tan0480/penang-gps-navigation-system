package gpsnavigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of Dijkstra's minimum-distance route search. */
public final class DijkstraResult {
    private final List<String> route;
    private final double totalDistanceKm;
    private final List<String> visitedLocations;

    DijkstraResult(List<String> route, double totalDistanceKm) {
        this(route, totalDistanceKm, route);
    }

    DijkstraResult(List<String> route, double totalDistanceKm, List<String> visitedLocations) {
        this.route = Collections.unmodifiableList(new ArrayList<>(route));
        this.totalDistanceKm = totalDistanceKm;
        this.visitedLocations = Collections.unmodifiableList(new ArrayList<>(visitedLocations));
    }

    public List<String> getRoute() {
        return route;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public List<String> getVisitedLocations() {
        return visitedLocations;
    }

    public boolean isFound() {
        return !route.isEmpty();
    }
}
