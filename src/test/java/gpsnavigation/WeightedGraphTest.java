package gpsnavigation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;

/** Simple no-build-tool test runner for the VS Code JavaFX setup. */
public final class WeightedGraphTest {
    public static void main(String[] args) {
        proposalGraphContainsAllLocationsAndConfirmedRoads();
        locationNamesAreCaseInsensitiveAcrossOperationsAndRoutes();
        neighbourCountScenariosRemainAvailableToTheRenderer();
        dynamicPositionRejectsTheSpecifiedEdgeCrossing();
        addAndRemoveWeightedUndirectedRoadMaintainsBothDirections();
        bfsAndDijkstraProduceTheProposalResults();
        invalidDistancesAndDisconnectedRoutesAreHandled();
        System.out.println("All graph tests passed.");
    }

    private static void proposalGraphContainsAllLocationsAndConfirmedRoads() {
        WeightedGraph graph = PenangRoadNetwork.createInitialGraph();
        checkEquals(17, graph.getLocationCount(), "location count");
        checkEquals(22, graph.getRoadCount(), "confirmed road count");

        String[][] roads = {
                {"Teluk Bahang", "Batu Ferringhi", "5"},
                {"Teluk Bahang", "Balik Pulau", "20"},
                {"Batu Ferringhi", "Tanjung Bungah", "7"},
                {"Tanjung Bungah", "Air Itam", "9"},
                {"Tanjung Bungah", "George Town", "9"},
                {"Air Itam", "Balik Pulau", "14"},
                {"Air Itam", "George Town", "7"},
                {"George Town", "Butterworth", "5"},
                {"George Town", "Gelugor", "7"},
                {"Balik Pulau", "Bayan Lepas", "16"},
                {"Gelugor", "Bayan Lepas", "9"},
                {"Gelugor", "Perai", "10"},
                {"Bayan Lepas", "Batu Kawan", "24"},
                {"Butterworth", "Kepala Batas", "15"},
                {"Butterworth", "Perai", "4"},
                {"Perai", "Bukit Mertajam", "11"},
                {"Kepala Batas", "Sungai Petani", "18"},
                {"Kepala Batas", "Tasek Gelugor", "11"},
                {"Tasek Gelugor", "Bukit Mertajam", "16"},
                {"Bukit Mertajam", "Kulim", "15"},
                {"Bukit Mertajam", "Batu Kawan", "20"},
                {"Batu Kawan", "Nibong Tebal", "11"}
        };
        for (String[] road : roads) {
            assertRoad(graph, road[0], road[1], Double.parseDouble(road[2]));
        }
    }

    private static void addAndRemoveWeightedUndirectedRoadMaintainsBothDirections() {
        WeightedGraph graph = new WeightedGraph();
        graph.addLocation("A");
        graph.addLocation("B");
        check(graph.addRoad("A", "B", 4.5), "road can be added");
        checkEquals(1, graph.getRoadCount(), "road count after add");
        check(graph.containsRoad("A", "B"), "forward road exists");
        check(graph.containsRoad("B", "A"), "reverse road exists");
        check(!graph.addRoad("B", "A", 4.5), "duplicate road is rejected");
        check(graph.removeRoad("B", "A"), "road can be removed");
        check(!graph.containsRoad("A", "B"), "forward road removed");
        check(!graph.containsRoad("B", "A"), "reverse road removed");
    }

    private static void neighbourCountScenariosRemainAvailableToTheRenderer() {
        WeightedGraph graph = PenangRoadNetwork.createInitialGraph();
        check(graph.addLocation("Isolated Test Location"), "location with no roads can be added");
        check(graph.addLocation("Single-Neighbour Test"), "one-neighbour location can be added");
        check(graph.addLocation("Two-Neighbour Test"), "two-neighbour location can be added");
        check(graph.addLocation("Three-Neighbour Test"), "three-neighbour location can be added");
        check(graph.addLocation("Late Connection Test"), "location for a later road can be added");

        check(graph.addRoad("single-neighbour test", "GEORGE TOWN", 12),
                "one-neighbour road can be added");
        check(graph.addRoad("Two-Neighbour Test", "George Town", 13),
                "first two-neighbour road can be added");
        check(graph.addRoad("Two-Neighbour Test", "Air Itam", 14),
                "second two-neighbour road can be added");
        check(graph.addRoad("Three-Neighbour Test", "Teluk Bahang", 15),
                "first three-neighbour road can be added");
        check(graph.addRoad("Three-Neighbour Test", "Kepala Batas", 20),
                "second three-neighbour road can be added");
        check(graph.addRoad("Three-Neighbour Test", "Kulim", 25),
                "third three-neighbour road can be added");
        check(graph.getRoadCount() == 28, "all neighbour-count roads are counted once");

        check(graph.addRoad("late connection test", "Gelugor", 10),
                "road can be added after the location was created");
        check(graph.containsRoad("LATE CONNECTION TEST", "gelugor"),
                "later road is available in both case forms");
        check(graph.getRoadCount() == 29, "later road increments the unique road count once");
    }

    private static void dynamicPositionRejectsTheSpecifiedEdgeCrossing() {
        try {
            Platform.startup(() -> {
                // Initialise JavaFX controls before the Application instance
                // is created by this non-JavaFX test runner.
            });
            GpsNavigationApp app = new GpsNavigationApp();
            Field graphField = GpsNavigationApp.class.getDeclaredField("graph");
            graphField.setAccessible(true);
            WeightedGraph graph = (WeightedGraph) graphField.get(app);
            check(graph.addLocation("dd"), "dynamic crossing-test location can be added");
            check(graph.addRoad("dd", "Bukit Mertajam", 6), "dd-Bukit Mertajam test road");
            check(graph.addRoad("dd", "Kulim", 6), "dd-Kulim test road");
            check(graph.addRoad("dd", "Nibong Tebal", 6), "dd-Nibong Tebal test road");

            Field nodePositionsField = GpsNavigationApp.class.getDeclaredField("NODE_POSITIONS");
            nodePositionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Point2D> fixedPositions =
                    (Map<String, Point2D>) nodePositionsField.get(null);
            Map<String, Point2D> positions = new LinkedHashMap<>(fixedPositions);

            Method calculateBestPosition = GpsNavigationApp.class.getDeclaredMethod(
                    "calculateBestPosition", String.class, Map.class, Map.class);
            calculateBestPosition.setAccessible(true);
            Point2D selectedPosition = (Point2D) calculateBestPosition.invoke(
                    app, "dd", graph.getAdjacencyListSnapshot(), positions);
            positions.put("dd", selectedPosition);

            Method createsAnyIllegalEdgeCrossing = GpsNavigationApp.class.getDeclaredMethod(
                    "createsAnyIllegalEdgeCrossing", String.class, Point2D.class,
                    List.class, Map.class);
            createsAnyIllegalEdgeCrossing.setAccessible(true);
            boolean selectedPositionCrosses = (Boolean) createsAnyIllegalEdgeCrossing.invoke(
                    app, "dd", selectedPosition,
                    Arrays.asList("Bukit Mertajam", "Kulim", "Nibong Tebal"), positions);
            check(!selectedPositionCrosses,
                    "selected dd position has no illegal edge crossing");

            Method segmentsProperlyIntersect = GpsNavigationApp.class.getDeclaredMethod(
                    "segmentsProperlyIntersect", Point2D.class, Point2D.class,
                    Point2D.class, Point2D.class);
            segmentsProperlyIntersect.setAccessible(true);
            Point2D badCandidate = new Point2D(900, 100);
            Point2D nibongTebal = fixedPositions.get("Nibong Tebal");
            Point2D bukitMertajam = fixedPositions.get("Bukit Mertajam");
            Point2D kulim = fixedPositions.get("Kulim");
            boolean specifiedCrossing = (Boolean) segmentsProperlyIntersect.invoke(
                    app, badCandidate, nibongTebal, bukitMertajam, kulim);
            check(specifiedCrossing,
                    "dd-Nibong Tebal crossing of Bukit Mertajam-Kulim is detected");

            boolean sharedEndpointIsAllowed = (Boolean) segmentsProperlyIntersect.invoke(
                    app, bukitMertajam, nibongTebal, bukitMertajam, kulim);
            check(!sharedEndpointIsAllowed,
                    "shared endpoint is not treated as a proper crossing");

            Method locationLabelPosition = GpsNavigationApp.class.getDeclaredMethod(
                    "locationLabelPosition", Label.class, Point2D.class, double.class);
            locationLabelPosition.setAccessible(true);
            Label shortLabel = new Label("ddd");
            Point2D shortCentre = new Point2D(500, 300);
            Point2D shortPosition = (Point2D) locationLabelPosition.invoke(
                    app, shortLabel, shortCentre, 14.0);
            checkEquals(520.0, shortPosition.getX(),
                    "short label keeps the fixed gap from its node");
            checkClose(shortCentre.getY(), shortPosition.getY() + labelHeight(shortLabel) / 2,
                    "short label remains centred on its node");

            Label longLabel = new Label("A Very Long Dynamic Location Name");
            Point2D rightEdgeCentre = new Point2D(870, 300);
            Point2D longPosition = (Point2D) locationLabelPosition.invoke(
                    app, longLabel, rightEdgeCentre, 14.0);
            check(longPosition.getX() < rightEdgeCentre.getX(),
                    "long label switches side near the canvas boundary");
            checkClose(rightEdgeCentre.getY(), longPosition.getY() + labelHeight(longLabel) / 2,
                    "long label remains centred on its node");

            Method distanceLabelPosition = GpsNavigationApp.class.getDeclaredMethod(
                    "getDistanceLabelPosition", Point2D.class, Point2D.class, double.class);
            distanceLabelPosition.setAccessible(true);
            Point2D horizontalLabelPosition = (Point2D) distanceLabelPosition.invoke(
                    app, new Point2D(100, 100), new Point2D(200, 100), 14.0);
            checkClose(150.0, horizontalLabelPosition.getX(),
                    "horizontal distance label is centred on the road");
            checkClose(114.0, horizontalLabelPosition.getY(),
                    "horizontal distance label uses the 14-pixel offset");

            Point2D verticalLabelPosition = (Point2D) distanceLabelPosition.invoke(
                    app, new Point2D(100, 100), new Point2D(100, 200), 14.0);
            checkClose(86.0, verticalLabelPosition.getX(),
                    "vertical distance label uses the perpendicular direction");
            checkClose(150.0, verticalLabelPosition.getY(),
                    "vertical distance label is centred on the road");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("dynamic layout crossing test could not run", exception);
        } finally {
            Platform.exit();
        }
    }

    private static void locationNamesAreCaseInsensitiveAcrossOperationsAndRoutes() {
        WeightedGraph graph = PenangRoadNetwork.createInitialGraph();

        check(graph.containsLocation("george town"), "mixed-case location lookup");
        check(graph.containsRoad("GEORGE TOWN", "butterworth"), "mixed-case road lookup");
        check(!graph.addLocation("GEORGE TOWN"), "case-only duplicate location is rejected");

        check(graph.addLocation("  North Connector  "), "new canonical location can be added");
        check(graph.containsLocation("north connector"), "new location has case-insensitive lookup");
        check(graph.getLocations().contains("North Connector"), "new location keeps display spelling");
        check(graph.addRoad("north connector", "GEORGE TOWN", 12),
                "mixed-case road endpoints can be added");
        check(graph.containsRoad("NORTH CONNECTOR", "george town"),
                "mixed-case added road lookup");

        BfsResult addedBfs = graph.bfsRoute("NORTH CONNECTOR", "bAyAn LePaS");
        DijkstraResult addedDijkstra = graph.dijkstraRoute("north connector", "BAYAN LEPAS");
        checkEquals(Arrays.asList("North Connector", "George Town", "Gelugor", "Bayan Lepas"),
                addedBfs.getRoute(), "BFS route after adding a location and road");
        checkEquals(Arrays.asList("North Connector", "George Town", "Gelugor", "Bayan Lepas"),
                addedDijkstra.getRoute(), "Dijkstra route after adding a location and road");
        checkEquals(28.0, addedDijkstra.getTotalDistanceKm(),
                "Dijkstra distance after adding a location and road");

        check(graph.removeRoad("NORTH CONNECTOR", "gEoRgE tOwN"),
                "mixed-case road endpoints can be removed");
        check(graph.removeLocation("NORTH CONNECTOR"), "mixed-case location can be removed");

        BfsResult bfs = graph.bfsRoute("aIr ItAm", "bAyAn LePaS");
        DijkstraResult dijkstra = graph.dijkstraRoute("AIR ITAM", "BAYAN LEPAS");
        checkEquals(Arrays.asList("Air Itam", "Balik Pulau", "Bayan Lepas"), bfs.getRoute(),
                "mixed-case BFS route");
        checkEquals(Arrays.asList("Air Itam", "George Town", "Gelugor", "Bayan Lepas"),
                dijkstra.getRoute(), "mixed-case Dijkstra route");

        checkEquals(Arrays.asList("Teluk Bahang", "Balik Pulau"),
                graph.bfsRoute("TELUK BAHANG", "balik pulau").getRoute(),
                "different route can be searched after the first route");

        try {
            graph.bfsRoute("Not A Location", "Bayan Lepas");
            throw new AssertionError("unknown BFS location should be rejected");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("Unknown location: Not A Location"),
                    "unknown location message is clear");
        }
    }

    private static void bfsAndDijkstraProduceTheProposalResults() {
        WeightedGraph graph = PenangRoadNetwork.createInitialGraph();
        BfsResult bfs = graph.bfsRoute("Air Itam", "Bayan Lepas");
        DijkstraResult dijkstra = graph.dijkstraRoute("Air Itam", "Bayan Lepas");

        checkEquals(Arrays.asList("Air Itam", "Balik Pulau", "Bayan Lepas"), bfs.getRoute(), "BFS route");
        checkEquals(2, bfs.getRoadConnections(), "BFS connections");
        checkEquals(Arrays.asList("Air Itam", "George Town", "Gelugor", "Bayan Lepas"),
                dijkstra.getRoute(), "Dijkstra route");
        checkEquals(23.0, dijkstra.getTotalDistanceKm(), "Dijkstra distance");
        checkEquals(30.0, graph.routeDistance(bfs.getRoute()), "BFS route distance");

        System.out.println("BFS route: " + bfs.getRoute() + " | hops=" + bfs.getRoadConnections());
        System.out.println("Dijkstra route: " + dijkstra.getRoute() + " | distance="
                + dijkstra.getTotalDistanceKm() + " km");
    }

    private static void invalidDistancesAndDisconnectedRoutesAreHandled() {
        WeightedGraph graph = new WeightedGraph();
        graph.addLocation("A");
        graph.addLocation("B");
        graph.addLocation("C");
        try {
            graph.addRoad("A", "B", 0);
            throw new AssertionError("zero distance should be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected validation result.
        }
        try {
            graph.addRoad("A", "A", 1);
            throw new AssertionError("a road must not connect a location to itself");
        } catch (IllegalArgumentException expected) {
            checkEquals("A road must connect two different locations.", expected.getMessage(),
                    "self-road validation message");
        }
        checkEquals(Collections.emptyList(), graph.bfsRoute("A", "C").getRoute(), "disconnected BFS");
        checkEquals(Collections.emptyList(), graph.dijkstraRoute("A", "C").getRoute(),
                "disconnected Dijkstra");
    }

    private static void assertRoad(WeightedGraph graph, String first, String second, double distanceKm) {
        check(graph.containsRoad(first, second), first + "-" + second + " road exists");
        check(graph.containsRoad(second, first), second + "-" + first + " reverse road exists");
        Map<String, java.util.List<Edge>> adjacency = graph.getAdjacencyListSnapshot();
        Edge forward = adjacency.get(first).stream()
                .filter(edge -> edge.getDestination().equals(second))
                .findFirst().orElseThrow();
        Edge reverse = adjacency.get(second).stream()
                .filter(edge -> edge.getDestination().equals(first))
                .findFirst().orElseThrow();
        checkEquals(distanceKm, forward.getDistanceKm(), first + "-" + second + " distance");
        checkEquals(distanceKm, reverse.getDistanceKm(), second + "-" + first + " distance");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError("Failed: " + description);
        }
    }

    private static void checkEquals(Object expected, Object actual, String description) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Failed: " + description + "; expected " + expected + " but got " + actual);
        }
    }

    private static double labelHeight(Label label) {
        double height = label.prefHeight(-1);
        return height > 0 && Double.isFinite(height) ? height : 18.0;
    }

    private static void checkClose(double expected, double actual, String description) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError("Failed: " + description + "; expected " + expected
                    + " but got " + actual);
        }
    }
}
