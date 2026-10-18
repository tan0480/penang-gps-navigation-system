package gpsnavigation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Weighted, undirected graph backed by Map<String, List<Edge>>.
 * Each road is stored twice, once for each direction.
 */
public final class WeightedGraph {
    private final Map<String, List<Edge>> adjacencyList = new LinkedHashMap<>();
    private final Map<String, String> canonicalNames = new HashMap<>();

    public void clear() {
        adjacencyList.clear();
        canonicalNames.clear();
    }

    public boolean addLocation(String location) {
        String name = requireName(location);
        String lookupKey = normalizeName(name);
        if (canonicalNames.containsKey(lookupKey)) {
            return false;
        }
        adjacencyList.put(name, new ArrayList<>());
        canonicalNames.put(lookupKey, name);
        return true;
    }

    public boolean removeLocation(String location) {
        String name = resolveName(location);
        if (name == null) {
            return false;
        }
        adjacencyList.remove(name);
        canonicalNames.remove(normalizeName(name));
        for (List<Edge> edges : adjacencyList.values()) {
            edges.removeIf(edge -> edge.getDestination().equals(name));
        }
        return true;
    }

    public boolean addRoad(String first, String second, double distanceKm) {
        String left = requireExistingLocation(first);
        String right = requireExistingLocation(second);
        validateEndpoints(left, right);
        if (!Double.isFinite(distanceKm) || distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero.");
        }
        if (containsRoad(left, right)) {
            return false;
        }
        insertSorted(adjacencyList.get(left), new Edge(right, distanceKm));
        insertSorted(adjacencyList.get(right), new Edge(left, distanceKm));
        return true;
    }

    public boolean removeRoad(String first, String second) {
        String left = requireExistingLocation(first);
        String right = requireExistingLocation(second);
        validateEndpoints(left, right);
        int leftIndex = findEdgeIndex(adjacencyList.get(left), right);
        int rightIndex = findEdgeIndex(adjacencyList.get(right), left);
        if (leftIndex < 0 || rightIndex < 0) {
            return false;
        }
        adjacencyList.get(left).remove(leftIndex);
        adjacencyList.get(right).remove(rightIndex);
        return true;
    }

    public boolean containsLocation(String location) {
        return resolveName(location) != null;
    }

    public boolean containsRoad(String first, String second) {
        String left = resolveName(first);
        String right = resolveName(second);
        if (left == null || right == null || left.equals(right)) {
            return false;
        }
        List<Edge> edges = adjacencyList.get(left);
        return edges != null && findEdgeIndex(edges, right) >= 0;
    }

    public int getLocationCount() {
        return adjacencyList.size();
    }

    public int getRoadCount() {
        int adjacencyEntryCount = 0;
        for (List<Edge> edges : adjacencyList.values()) {
            adjacencyEntryCount += edges.size();
        }
        return adjacencyEntryCount / 2;
    }

    public List<String> getLocations() {
        return Collections.unmodifiableList(new ArrayList<>(adjacencyList.keySet()));
    }

    public Map<String, List<Edge>> getAdjacencyListSnapshot() {
        Map<String, List<Edge>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, List<Edge>> entry : adjacencyList.entrySet()) {
            snapshot.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public double routeDistance(List<String> route) {
        double total = 0.0;
        for (int index = 0; index + 1 < route.size(); index++) {
            String current = requireExistingLocation(route.get(index));
            String next = requireExistingLocation(route.get(index + 1));
            int edgeIndex = findEdgeIndex(adjacencyList.get(current), next);
            if (edgeIndex < 0) {
                throw new IllegalArgumentException("Route contains a missing road.");
            }
            total += adjacencyList.get(current).get(edgeIndex).getDistanceKm();
        }
        return total;
    }

    public String formatAdjacencyList() {
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, List<Edge>> entry : adjacencyList.entrySet()) {
            output.append("  ").append(entry.getKey()).append(" -> ")
                    .append(entry.getValue()).append(System.lineSeparator());
        }
        return output.toString();
    }

    private void validateEndpoints(String first, String second) {
        if (first.equals(second)) {
            throw new IllegalArgumentException("A road must connect two different locations.");
        }
    }

    private String requireExistingLocation(String location) {
        String requestedName = requireName(location);
        String name = canonicalNames.get(normalizeName(requestedName));
        if (name == null) {
            throw new IllegalArgumentException("Unknown location: " + requestedName);
        }
        return name;
    }

    private String resolveName(String location) {
        String requestedName = requireName(location);
        return canonicalNames.get(normalizeName(requestedName));
    }

    private static String requireName(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be blank.");
        }
        return location.trim();
    }

    private static String normalizeName(String location) {
        return location.toLowerCase(Locale.ROOT);
    }

    private static void insertSorted(List<Edge> edges, Edge edge) {
        int index = 0;
        while (index < edges.size()
                && edges.get(index).getDestination().compareToIgnoreCase(edge.getDestination()) < 0) {
            index++;
        }
        edges.add(index, edge);
    }

    private static int findEdgeIndex(List<Edge> edges, String destination) {
        for (int index = 0; index < edges.size(); index++) {
            if (edges.get(index).getDestination().equals(destination)) {
                return index;
            }
        }
        return -1;
    }

    private static List<String> buildRoute(Map<String, String> parent, String target) {
        Deque<String> reverseRoute = new ArrayDeque<>();
        for (String current = target; current != null; current = parent.get(current)) {
            reverseRoute.addFirst(current);
        }
        return new ArrayList<>(reverseRoute);
    }
}
