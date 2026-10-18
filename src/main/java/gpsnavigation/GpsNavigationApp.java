package gpsnavigation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/** JavaFX user interface for the weighted Penang GPS navigation prototype. */
public final class GpsNavigationApp extends Application {
    private static final double LAYOUT_WIDTH = 930;
    private static final double LAYOUT_HEIGHT = 530;
    private static final double SAFE_AREA_MARGIN = 80;
    private static final double MINIMUM_NODE_DISTANCE = 100;
    private static final double MINIMUM_EDGE_DISTANCE = 55;
    private static final double MINIMUM_LABEL_NODE_DISTANCE = 28;
    private static final double MINIMUM_LABEL_GAP = 4;
    private static final double LOCATION_LABEL_GAP = 6;
    private static final double PREFERRED_BOUNDARY_CLEARANCE = 60;
    private static final double NEW_EDGE_NODE_CLEARANCE = 35;
    private static final double NEW_EDGE_OVERLAP_ANGLE_DEGREES = 20;
    private static final double TEMPORARY_GRID_START_X = 110;
    private static final double TEMPORARY_GRID_START_Y = 100;
    private static final double TEMPORARY_GRID_SPACING = 95;
    private static final double CANVAS_CONTENT_MARGIN = 120;

    private static final Map<String, Point2D> NODE_POSITIONS = Map.ofEntries(
            Map.entry("Teluk Bahang", new Point2D(80, 80)),
            Map.entry("Batu Ferringhi", new Point2D(190, 65)),
            Map.entry("Tanjung Bungah", new Point2D(305, 125)),
            Map.entry("Air Itam", new Point2D(300, 260)),
            Map.entry("Balik Pulau", new Point2D(135, 365)),
            Map.entry("George Town", new Point2D(450, 190)),
            Map.entry("Gelugor", new Point2D(475, 335)),
            Map.entry("Bayan Lepas", new Point2D(350, 470)),
            Map.entry("Butterworth", new Point2D(615, 115)),
            Map.entry("Perai", new Point2D(665, 250)),
            Map.entry("Kepala Batas", new Point2D(755, 75)),
            Map.entry("Sungai Petani", new Point2D(870, 55)),
            Map.entry("Tasek Gelugor", new Point2D(835, 185)),
            Map.entry("Bukit Mertajam", new Point2D(755, 350)),
            Map.entry("Kulim", new Point2D(850, 470)),
            Map.entry("Batu Kawan", new Point2D(590, 455)),
            Map.entry("Nibong Tebal", new Point2D(720, 485)));

    private final WeightedGraph graph = PenangRoadNetwork.createInitialGraph();
    private final Pane graphCanvas = new Pane();
    private final ScrollPane graphScrollPane = new ScrollPane(graphCanvas);
    private final Label graphSummary = new Label();
    private final TextArea networkOutput = new TextArea();
    private final TextArea resultOutput = new TextArea();
    private final Label status = new Label();
    private final TextField locationInput = new TextField();
    private final ComboBox<String> removeLocationInput = new ComboBox<>();
    private final ComboBox<String> firstLocationInput = new ComboBox<>();
    private final ComboBox<String> secondLocationInput = new ComboBox<>();
    private final TextField distanceInput = new TextField();
    private final ComboBox<String> startInput = new ComboBox<>();
    private final ComboBox<String> destinationInput = new ComboBox<>();
    private final Set<String> highlightedRouteNodes = new HashSet<>();
    private final Set<String> highlightedRouteRoads = new HashSet<>();
    private final Map<String, Point2D> dynamicNodePositions = new LinkedHashMap<>();
    private final Map<String, Integer> dynamicNodeNeighbourCounts = new LinkedHashMap<>();

    private record DistanceLabelPlacement(Point2D topLeft, Point2D centre,
            double width, double height) {
    }

    private record LabelDimensions(double width, double height) {
    }

    private record LocationLabelPlacement(Label label, Point2D topLeft,
            double width, double height) {
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Penang GPS Navigation System");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(14));
        root.setTop(createHeader());
        root.setLeft(createControls());
        root.setCenter(createOutputPanel());
        refreshNetworkOutput();

        // The visual graph is deliberately wide enough to show the full
        // Penang network without clipping its right-most mainland nodes.
        Scene scene = new Scene(root, 1450, 980);
        stage.setScene(scene);
        stage.show();
        graphScrollPane.setHvalue(0.0);
        graphScrollPane.setVvalue(0.0);
    }

    private VBox createHeader() {
        Label title = heading1("Penang GPS Navigation System");
        Label subtitle = new Label("Weighted, undirected graph using an adjacency list");
        subtitle.setStyle("-fx-font-size: 14px;");
        VBox header = new VBox(3, title, subtitle);
        header.setPadding(new Insets(0, 0, 12, 0));
        return header;
    }

    private VBox createControls() {
        locationInput.setPromptText("e.g. Butterworth");
        distanceInput.setPromptText("e.g. 5 or 4.5");

        locationInput.setTooltip(new Tooltip(
                "Enter a new location name. Use this field with Add Location."));
        configureLocationSelector(removeLocationInput, "Select a location",
                "Select an existing location to remove. Its connected roads will also be removed.");
        configureLocationSelector(firstLocationInput, "Select first location",
                "Select the first existing endpoint of the road.");
        configureLocationSelector(secondLocationInput, "Select second location",
                "Select the second existing endpoint of the road.");
        distanceInput.setTooltip(new Tooltip(
                "Enter a positive road distance in kilometres for Add Road."));

        GridPane graphForm = new GridPane();
        graphForm.setHgap(7);
        graphForm.setVgap(7);
        graphForm.add(heading3("Location Operations"), 0, 0, 2, 1);
        graphForm.add(new Label("New Location:"), 0, 1);
        graphForm.add(locationInput, 1, 1);
        Button addLocation = new Button("Add Location");
        addLocation.setTooltip(new Tooltip(
                "Add a new location. Duplicate location names are rejected."));
        graphForm.add(addLocation, 1, 2);
        graphForm.add(new Label("Remove Location:"), 0, 3);
        graphForm.add(removeLocationInput, 1, 3);
        Button removeLocation = new Button("Remove Location");
        removeLocation.setTooltip(new Tooltip(
                "Remove the location and every road connected to it."));
        graphForm.add(removeLocation, 1, 4);

        graphForm.add(heading3("Road Operations"), 0, 5, 2, 1);
        graphForm.add(new Label("First Location:"), 0, 6);
        graphForm.add(firstLocationInput, 1, 6);
        graphForm.add(new Label("Second Location:"), 0, 7);
        graphForm.add(secondLocationInput, 1, 7);
        graphForm.add(new Label("Distance (km):"), 0, 8);
        graphForm.add(distanceInput, 1, 8);
        Button addRoad = new Button("Add Road");
        Button removeRoad = new Button("Remove Road");
        addRoad.setTooltip(new Tooltip(
                "Add a weighted road in both directions. Select two different locations and enter a positive distance."));
        removeRoad.setTooltip(new Tooltip(
                "Remove the road in both directions. The distance field is not required."));
        graphForm.add(new HBox(5, addRoad, removeRoad), 1, 9);

        Button reset = new Button("Reload Proposal Graph");
        reset.setTooltip(new Tooltip(
                "Restore the original proposal locations and confirmed roads. Discards your edits."));
        graphForm.add(reset, 1, 10);

        addLocation.setOnAction(event -> addLocation());
        removeLocation.setOnAction(event -> removeLocation());
        addRoad.setOnAction(event -> addRoad());
        removeRoad.setOnAction(event -> removeRoad());
        reset.setOnAction(event -> reloadProposalGraph());

        GridPane searchForm = new GridPane();
        searchForm.setHgap(7);
        searchForm.setVgap(7);
        configureLocationSelector(startInput, "Select start location",
                "Select an existing starting location for BFS or Dijkstra.");
        configureLocationSelector(destinationInput, "Select destination",
                "Select an existing destination location for BFS or Dijkstra.");
        searchForm.add(new Label("Start:"), 0, 0);
        searchForm.add(startInput, 1, 0);
        searchForm.add(new Label("Destination:"), 0, 1);
        searchForm.add(destinationInput, 1, 1);
        Button bfs = new Button("Run BFS Algorithm");
        Button dijkstra = new Button("Run Dijkstra Algorithm");
        bfs.setTooltip(new Tooltip(
                "Find a route with the fewest road connections. BFS ignores distances."));
        dijkstra.setTooltip(new Tooltip(
                "Find a route with the smallest total distance in kilometres."));
        searchForm.add(new HBox(5, bfs, dijkstra), 1, 2);
        bfs.setOnAction(event -> runBfs());
        dijkstra.setOnAction(event -> runDijkstra());

        refreshLocationDropdowns();

        status.setWrapText(true);
        status.setStyle("-fx-text-fill: #1f5f32;");

        VBox resultBox = new VBox(5, heading2("Search Result"), resultOutput);
        resultBox.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(resultOutput, Priority.ALWAYS);

        VBox controls = new VBox(10,
                heading2("Graph Operations"), graphForm,
                heading2("Route Algorithms"), searchForm,
                status, resultBox);
        controls.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(resultBox, Priority.ALWAYS);
        controls.setPrefWidth(350);
        controls.setPadding(new Insets(0, 16, 0, 0));
        return controls;
    }

    private void configureLocationSelector(ComboBox<String> selector, String promptText,
            String tooltipText) {
        selector.setPromptText(promptText);
        selector.setTooltip(new Tooltip(tooltipText));
        selector.setMaxWidth(Double.MAX_VALUE);
    }

    /** Refreshes every selector from the graph while preserving valid choices. */
    private void refreshLocationDropdowns() {
        List<String> locations = new ArrayList<>(graph.getLocations());
        locations.sort(String.CASE_INSENSITIVE_ORDER);

        refreshLocationDropdown(removeLocationInput, locations);
        refreshLocationDropdown(firstLocationInput, locations);
        refreshLocationDropdown(secondLocationInput, locations);
        refreshLocationDropdown(startInput, locations);
        refreshLocationDropdown(destinationInput, locations);

        if (startInput.getValue() == null) {
            selectIfPresent(startInput, locations, "Air Itam");
        }
        if (destinationInput.getValue() == null) {
            selectIfPresent(destinationInput, locations, "Bayan Lepas");
        }
    }

    private void refreshLocationDropdown(ComboBox<String> selector, List<String> locations) {
        String previousSelection = selector.getValue();
        selector.getItems().setAll(locations);
        selector.setValue(findMatchingLocation(previousSelection, locations));
    }

    private void selectIfPresent(ComboBox<String> selector, List<String> locations,
            String requestedLocation) {
        String matchingLocation = findMatchingLocation(requestedLocation, locations);
        if (matchingLocation != null) {
            selector.setValue(matchingLocation);
        }
    }

    private static String findMatchingLocation(String requestedLocation,
            List<String> locations) {
        if (requestedLocation == null) {
            return null;
        }
        for (String location : locations) {
            if (location.equalsIgnoreCase(requestedLocation.trim())) {
                return location;
            }
        }
        return null;
    }

    private VBox createOutputPanel() {
        graphCanvas.setPrefSize(LAYOUT_WIDTH, LAYOUT_HEIGHT);
        graphCanvas.setMinSize(0, 0);
        graphCanvas.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        graphCanvas.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #b8c2cc;"
                + " -fx-border-width: 1px;");
        graphScrollPane.setPannable(true);
        graphScrollPane.setFitToWidth(false);
        graphScrollPane.setFitToHeight(false);
        graphScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        graphScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        graphScrollPane.setPrefViewportWidth(LAYOUT_WIDTH);
        graphScrollPane.setPrefViewportHeight(LAYOUT_HEIGHT);
        graphScrollPane.setMinHeight(500);
        graphScrollPane.setPrefHeight(650);
        graphScrollPane.setMaxHeight(Double.MAX_VALUE);
        graphScrollPane.setMaxWidth(Double.MAX_VALUE);
        graphScrollPane.setTooltip(new Tooltip(
                "Use the scroll bars or drag the graph to view additional locations."));
        graphSummary.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        networkOutput.setEditable(false);
        networkOutput.setWrapText(true);
        networkOutput.setTooltip(new Tooltip(
                "The current weighted adjacency list. Each undirected road appears under both locations."));
        networkOutput.setPrefRowCount(9);
        resultOutput.setEditable(false);
        resultOutput.setWrapText(true);
        resultOutput.setTooltip(new Tooltip(
                "Results from the selected BFS or Dijkstra route search."));
        resultOutput.setPrefRowCount(7);
        resultOutput.setText("No route search has been run.");

        VBox graphBox = new VBox(5, heading2("Current Road Network"), graphSummary, graphScrollPane);
        VBox.setVgrow(graphScrollPane, Priority.ALWAYS);
        VBox.setVgrow(graphBox, Priority.ALWAYS);
        VBox networkBox = new VBox(5, heading3("Adjacency List"), networkOutput);
        VBox output = new VBox(12, graphBox, networkBox);
        output.setAlignment(Pos.TOP_LEFT);
        return output;
    }

    private Label heading2(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
    }

    private Label heading1(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        return label;
    }

    private Label heading3(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return label;
    }

    private void addLocation() {
        try {
            String location = locationInput.getText();
            if (!graph.addLocation(location)) {
                throw new IllegalArgumentException(
                        "Location already exists (location names are case-insensitive).");
            }
            clearRouteHighlight();
            refreshDynamicNodePositions();
            complete("Location added successfully.");
        } catch (IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    private void removeLocation() {
        try {
            String location = selectedLocation(removeLocationInput,
                    "Select a location to remove.");
            if (!graph.removeLocation(location)) {
                throw new IllegalArgumentException("Location does not exist.");
            }
            clearRouteHighlight();
            refreshDynamicNodePositions();
            complete("Location and its roads removed successfully.");
        } catch (IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    private void addRoad() {
        try {
            String first = selectedLocation(firstLocationInput,
                    "Select the first road location.");
            String second = selectedLocation(secondLocationInput,
                    "Select the second road location.");
            double distance = Double.parseDouble(distanceInput.getText().trim());
            if (!graph.addRoad(first, second, distance)) {
                throw new IllegalArgumentException("Road already exists.");
            }
            clearRouteHighlight();
            refreshDynamicNodePositions();
            complete("Road added successfully in both directions.");
        } catch (NumberFormatException exception) {
            fail("Distance must be a positive number.");
        } catch (IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    private void removeRoad() {
        try {
            String first = selectedLocation(firstLocationInput,
                    "Select the first road location.");
            String second = selectedLocation(secondLocationInput,
                    "Select the second road location.");
            if (!graph.removeRoad(first, second)) {
                throw new IllegalArgumentException("Road does not exist.");
            }
            clearRouteHighlight();
            refreshDynamicNodePositions();
            complete("Road removed successfully from both directions.");
        } catch (IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    private void reloadProposalGraph() {
        clearRouteHighlight();
        dynamicNodePositions.clear();
        dynamicNodeNeighbourCounts.clear();
        graph.clear();
        WeightedGraph initial = PenangRoadNetwork.createInitialGraph();
        for (String location : initial.getLocations()) {
            graph.addLocation(location);
        }
        for (var entry : initial.getAdjacencyListSnapshot().entrySet()) {
            for (Edge edge : entry.getValue()) {
                if (!graph.containsRoad(entry.getKey(), edge.getDestination())) {
                    graph.addRoad(entry.getKey(), edge.getDestination(), edge.getDistanceKm());
                }
            }
        }
        complete("Proposal graph reloaded.");
    }

    private void runBfs() {
        clearRouteHighlight();
        try {
            String start = selectedLocation(startInput, "Select a start location.");
            String destination = selectedLocation(destinationInput,
                    "Select a destination location.");
            BfsResult result = graph.bfsRoute(start, destination);
            String routeText = result.isFound()
                    ? result.getRoute().toString()
                    : "No route found.";
            String roadConnections = result.isFound()
                    ? Integer.toString(result.getRoadConnections())
                    : "N/A";
            StringBuilder output = new StringBuilder("Algorithm: BFS\n")
                    .append("Start: ").append(start).append('\n')
                    .append("Destination: ").append(destination).append('\n')
                    .append("Route: ").append(routeText).append('\n')
                    .append("Road Connections: ").append(roadConnections).append('\n')
                    .append("Visited Locations: ").append(result.getTraversalOrder());
            if (result.isFound()) {
                highlightRoute(result.getRoute());
            }
            resultOutput.setText(output.toString());
            complete("BFS completed.");
            String routeDistance = result.isFound()
                    ? "Route Distance (not optimised by BFS): "
                            + formatKm(graph.routeDistance(result.getRoute())) + " km"
                    : "Route Distance: N/A";
            showRouteSearchResult("BFS", start, destination, "Route", routeText,
                    "Road Connections: " + roadConnections + "\n" + routeDistance,
                    result.getTraversalOrder().toString());
        } catch (IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    private void runDijkstra() {
        clearRouteHighlight();
        try {
            String start = selectedLocation(startInput, "Select a start location.");
            String destination = selectedLocation(destinationInput,
                    "Select a destination location.");
            DijkstraResult result = graph.dijkstraRoute(start, destination);
            String routeText = result.isFound()
                    ? result.getRoute().toString()
                    : "No route found.";
            String totalDistance = result.isFound()
                    ? formatKm(result.getTotalDistanceKm()) + " km"
                    : "N/A";
            StringBuilder output = new StringBuilder("Algorithm: Dijkstra\n")
                    .append("Start: ").append(start).append('\n')
                    .append("Destination: ").append(destination).append('\n')
                    .append("Shortest Route: ").append(routeText).append('\n')
                    .append("Total Distance: ").append(totalDistance).append('\n')
                    .append("Visited Locations: ").append(result.getVisitedLocations());
            if (result.isFound()) {
                highlightRoute(result.getRoute());
            }
            resultOutput.setText(output.toString());
            complete("Dijkstra completed.");
            showRouteSearchResult("Dijkstra", start, destination, "Shortest Route", routeText,
                    "Total Distance: " + totalDistance,
                    result.getVisitedLocations().toString());
        } catch (IllegalArgumentException exception) {
            fail(exception.getMessage());
        }
    }

    private void showRouteSearchResult(String algorithm, String start, String destination,
            String routeLabel, String route, String metric, String visitedLocations) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Route Search Result");
        alert.setHeaderText(algorithm + " route search");
        alert.setContentText("Algorithm: " + algorithm
                + "\nStart: " + start
                + "\nDestination: " + destination
                + "\n" + routeLabel + ": " + route
                + "\n" + metric
                + "\nVisited Locations: " + visitedLocations);
        alert.showAndWait();
    }

    private String selectedLocation(ComboBox<String> selector, String errorMessage) {
        String selectedLocation = selector.getValue();
        if (selectedLocation == null || selectedLocation.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return selectedLocation;
    }

    private void refreshNetworkOutput() {
        refreshLocationDropdowns();
        graphSummary.setText("Locations: " + graph.getLocationCount()
                + " | Confirmed roads: " + graph.getRoadCount());
        networkOutput.setText(graph.formatAdjacencyList());
        refreshGraphVisualization();
    }

    private void refreshGraphVisualization() {
        graphCanvas.getChildren().clear();
        Map<String, List<Edge>> adjacencyList = graph.getAdjacencyListSnapshot();
        Map<String, Point2D> positions = displayPositionsForGraph();
        for (Point2D position : positions.values()) {
            ensureCanvasContains(position);
        }

        Map<String, LocationLabelPlacement> locationLabels = new LinkedHashMap<>();
        for (String location : graph.getLocations()) {
            Point2D position = positions.get(location);
            boolean routeNode = highlightedRouteNodes.contains(location);
            double nodeRadius = routeNode ? 16 : 14;
            Label label = createLocationLabel(location, routeNode);
            LabelDimensions dimensions = measureLabel(label);
            Point2D labelPosition = locationLabelPosition(position, nodeRadius,
                    dimensions.width(), dimensions.height());
            locationLabels.put(location, new LocationLabelPlacement(label, labelPosition,
                    dimensions.width(), dimensions.height()));
        }

        Set<String> drawnRoads = new HashSet<>();
        List<DistanceLabelPlacement> placedDistanceLabels = new ArrayList<>();

        for (Map.Entry<String, List<Edge>> entry : adjacencyList.entrySet()) {
            Point2D from = positions.get(entry.getKey());
            for (Edge edge : entry.getValue()) {
                String roadKey = canonicalRoadKey(entry.getKey(), edge.getDestination());
                if (drawnRoads.add(roadKey)) {
                    Point2D to = positions.get(edge.getDestination());
                    Line road = new Line(from.getX(), from.getY(), to.getX(), to.getY());
                    boolean routeRoad = highlightedRouteRoads.contains(roadKey);
                    road.setStroke(routeRoad ? Color.web("#dc2626") : Color.web("#718096"));
                    road.setStrokeWidth(routeRoad ? 4.0 : 2.0);
                    graphCanvas.getChildren().add(road);

                    Label distance = new Label(formatKm(edge.getDistanceKm()) + " km");
                    distance.setStyle(routeRoad
                            ? "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #b91c1c;"
                                    + " -fx-background-color: #fff7ed; -fx-padding: 1px 2px;"
                            : "-fx-font-size: 10px; -fx-text-fill: #334155;"
                                    + " -fx-background-color: white; -fx-padding: 1px 2px;");
                    String distanceText = formatKm(edge.getDistanceKm()) + " km";
                    DistanceLabelPlacement labelPlacement = findDistanceLabelPlacement(
                            from, to, distanceText, positions, locationLabels.values(),
                            placedDistanceLabels);
                    distance.setLayoutX(labelPlacement.topLeft().getX());
                    distance.setLayoutY(labelPlacement.topLeft().getY());
                    distance.setMouseTransparent(true);
                    graphCanvas.getChildren().add(distance);
                    placedDistanceLabels.add(labelPlacement);
                }
            }
        }

        for (String location : graph.getLocations()) {
            Point2D position = positions.get(location);
            boolean routeNode = highlightedRouteNodes.contains(location);
            double nodeRadius = routeNode ? 16 : 14;
            Circle node = new Circle(position.getX(), position.getY(), nodeRadius,
                    routeNode ? Color.web("#fed7aa") : Color.web("#dbeafe"));
            node.setStroke(routeNode ? Color.web("#c2410c") : Color.web("#1d4ed8"));
            node.setStrokeWidth(routeNode ? 3.0 : 2.0);
            graphCanvas.getChildren().add(node);

            LocationLabelPlacement locationLabel = locationLabels.get(location);
            Label label = locationLabel.label();
            label.setLayoutX(locationLabel.topLeft().getX());
            label.setLayoutY(locationLabel.topLeft().getY());
            label.setMouseTransparent(true);
            graphCanvas.getChildren().add(label);
        }
    }

    /**
     * Returns fixed drawing coordinates. The original proposal locations are
     * never rescaled when a dynamic location is added. Dynamic coordinates are
     * stored and reused so a pane redraw cannot move an existing location.
     */
    private Map<String, Point2D> displayPositionsForGraph() {
        Map<String, Point2D> positions = new LinkedHashMap<>();
        for (String location : graph.getLocations()) {
            Point2D fixedPosition = NODE_POSITIONS.get(location);
            if (fixedPosition != null) {
                positions.put(location, fixedPosition);
            } else if (dynamicNodePositions.containsKey(location)) {
                positions.put(location, dynamicNodePositions.get(location));
            } else {
                // This is only a defensive fallback. Normal graph edits call
                // refreshDynamicNodePositions(), which stores this position.
                positions.put(location, findTemporaryPosition(location, positions));
            }
        }
        return positions;
    }

    /**
     * Recalculates dynamic locations only after a graph edit. A saved
     * location is left unchanged during ordinary redraws. Once a dynamic
     * location receives its first meaningful connection, its position is
     * finalised so later child locations do not move it.
     */
    private void refreshDynamicNodePositions() {
        Map<String, List<Edge>> adjacencyList = graph.getAdjacencyListSnapshot();
        dynamicNodePositions.keySet().removeIf(location -> !adjacencyList.containsKey(location));
        dynamicNodeNeighbourCounts.keySet().removeIf(location -> !adjacencyList.containsKey(location));

        Map<String, Point2D> positions = displayPositionsForGraph();
        for (String location : graph.getLocations()) {
            if (NODE_POSITIONS.containsKey(location)) {
                continue;
            }

            int neighbourCount = adjacencyList.getOrDefault(location, List.of()).size();
            if (neighbourCount == 0) {
                positions.remove(location);
                Point2D temporaryPosition = findTemporaryPosition(location, positions);
                dynamicNodePositions.put(location, temporaryPosition);
                dynamicNodeNeighbourCounts.put(location, 0);
                positions.put(location, temporaryPosition);
                ensureCanvasContains(temporaryPosition);
                continue;
            }

            int previousNeighbourCount = dynamicNodeNeighbourCounts.getOrDefault(location, -1);
            boolean connectionCountChanged = previousNeighbourCount != neighbourCount;
            if (dynamicNodePositions.containsKey(location) && !connectionCountChanged) {
                continue;
            }

            positions.remove(location);
            Point2D chosenPosition = calculateBestPosition(location, adjacencyList, positions);
            dynamicNodePositions.put(location, chosenPosition);
            dynamicNodeNeighbourCounts.put(location, neighbourCount);
            positions.put(location, chosenPosition);
            ensureCanvasContains(chosenPosition);
        }
    }

    /**
     * Chooses the cleanest position around the centroid of every neighbour.
     * A dynamic location is recalculated only when its own neighbour count
     * changes; ordinary redraws therefore keep positions stable.
     */
    private Point2D calculateBestPosition(String location,
            Map<String, List<Edge>> adjacencyList,
            Map<String, Point2D> positions) {
        List<String> neighbours = new ArrayList<>();
        List<Point2D> neighbourPositions = new ArrayList<>();
        for (Edge edge : adjacencyList.getOrDefault(location, List.of())) {
            neighbours.add(edge.getDestination());
            Point2D neighbourPosition = positions.get(edge.getDestination());
            if (neighbourPosition == null) {
                neighbourPosition = findTemporaryPosition(edge.getDestination(), positions);
                positions.put(edge.getDestination(), neighbourPosition);
            }
            neighbourPositions.add(neighbourPosition);
        }

        double totalX = 0;
        double totalY = 0;
        for (Point2D neighbour : neighbourPositions) {
            totalX += neighbour.getX();
            totalY += neighbour.getY();
        }
        Point2D anchor = new Point2D(totalX / neighbourPositions.size(),
                totalY / neighbourPositions.size());
        List<Point2D> candidates = generateCandidatePositions(anchor,
                new double[] {90, 120, 150, 180, 220});
        boolean safeCandidateAvailable = false;
        for (Point2D candidate : candidates) {
            if (isInsideSafeArea(candidate)) {
                safeCandidateAvailable = true;
                break;
            }
        }

        PositionSearchResult normalSearch = searchCandidatePositions(location, candidates,
                neighbours, neighbourPositions, positions, safeCandidateAvailable);
        if (normalSearch.bestValidPosition() != null) {
            return normalSearch.bestValidPosition();
        }

        // Expand only if the normal search has no valid zero-crossing result.
        // The search remains centred on the neighbours rather than switching
        // to an unrelated global position.
        List<Point2D> extendedCandidates = generateCandidatePositions(anchor,
                new double[] {250, 280, 310, 340});
        PositionSearchResult extendedSearch = searchCandidatePositions(location,
                extendedCandidates, neighbours, neighbourPositions, positions, false);
        if (extendedSearch.bestValidPosition() != null) {
            return extendedSearch.bestValidPosition();
        }

        PositionSearchResult fallback = betterFallback(normalSearch, extendedSearch);
        if (fallback.fallbackPosition() != null) {
            // No zero-crossing position was found in the complete search. The
            // least-crossing candidate is retained as a deterministic last
            // resort; normal examples should resolve before this branch.
            return fallback.fallbackPosition();
        }
        return new Point2D(
                Math.max(SAFE_AREA_MARGIN, anchor.getX() + 250),
                Math.max(SAFE_AREA_MARGIN, anchor.getY()));
    }

    private List<Point2D> generateCandidatePositions(Point2D anchor, double[] radii) {
        List<Point2D> candidates = new ArrayList<>();
        for (double radius : radii) {
            for (int index = 0; index < 16; index++) {
                double angle = 2.0 * Math.PI * index / 16.0;
                candidates.add(new Point2D(
                        anchor.getX() + Math.cos(angle) * radius,
                        anchor.getY() + Math.sin(angle) * radius));
            }
        }
        return candidates;
    }

    private PositionSearchResult searchCandidatePositions(String location,
            List<Point2D> candidates, List<String> neighbours,
            List<Point2D> neighbourPositions, Map<String, Point2D> positions,
            boolean restrictToOriginalSafeArea) {
        Point2D bestValidPosition = null;
        double bestValidScore = Double.POSITIVE_INFINITY;
        Point2D fallbackPosition = null;
        int fallbackCrossings = Integer.MAX_VALUE;
        double fallbackScore = Double.POSITIVE_INFINITY;

        for (Point2D candidate : candidates) {
            if (restrictToOriginalSafeArea && !isInsideSafeArea(candidate)) {
                continue;
            }
            if (!isInExpandableSafeArea(candidate)) {
                continue;
            }

            int crossings = countNewEdgeCrossings(location, candidate, neighbours, positions);
            boolean illegalCrossing = createsAnyIllegalEdgeCrossing(
                    location, candidate, neighbours, positions);
            double score = scoreCandidate(location, candidate, neighbours, neighbourPositions,
                    positions);
            if (crossings < fallbackCrossings
                    || (crossings == fallbackCrossings && score < fallbackScore)) {
                fallbackPosition = candidate;
                fallbackCrossings = crossings;
                fallbackScore = score;
            }

            boolean valid = !isPositionTooClose(candidate, positions, MINIMUM_NODE_DISTANCE)
                    && !isPositionTooCloseToAnyRoad(location, candidate, positions,
                            MINIMUM_EDGE_DISTANCE)
                    && !illegalCrossing;
            if (valid && score < bestValidScore) {
                bestValidPosition = candidate;
                bestValidScore = score;
            }
        }
        return new PositionSearchResult(bestValidPosition, bestValidScore,
                fallbackPosition, fallbackCrossings, fallbackScore);
    }

    private PositionSearchResult betterFallback(PositionSearchResult first,
            PositionSearchResult second) {
        if (first.fallbackPosition() == null) {
            return second;
        }
        if (second.fallbackPosition() == null) {
            return first;
        }
        if (second.fallbackCrossings() < first.fallbackCrossings()
                || (second.fallbackCrossings() == first.fallbackCrossings()
                        && second.fallbackScore() < first.fallbackScore())) {
            return second;
        }
        return first;
    }

    private record PositionSearchResult(Point2D bestValidPosition, double bestValidScore,
            Point2D fallbackPosition, int fallbackCrossings, double fallbackScore) {
    }

    private double scoreCandidate(String location, Point2D candidate,
            List<String> neighbours, List<Point2D> neighbourPositions,
            Map<String, Point2D> positions) {
        double score = 0;

        for (Point2D existing : positions.values()) {
            double distance = candidate.distance(existing);
            if (distance < 45) {
                score += 100000;
            } else if (distance < MINIMUM_NODE_DISTANCE) {
                score += 20000 + (MINIMUM_NODE_DISTANCE - distance) * 100;
            } else {
                score += 300.0 / distance;
            }
        }

        for (String neighbour : neighbours) {
            Point2D neighbourPosition = positions.get(neighbour);
            if (neighbourPosition != null) {
                score += candidate.distance(neighbourPosition) * 0.05;
            }
        }

        double nearestRoad = minimumDistanceToUnrelatedRoad(location, candidate, positions);
        if (nearestRoad < MINIMUM_EDGE_DISTANCE) {
            score += 30000 + (MINIMUM_EDGE_DISTANCE - nearestRoad) * 150;
        } else if (Double.isFinite(nearestRoad)) {
            score += 100.0 / nearestRoad;
        }

        score += boundaryPenalty(candidate);
        score += countNewEdgeOverlaps(candidate, neighbourPositions) * 7000;
        score += scoreNewEdgeNodeClearance(location, candidate, neighbours, positions);
        score += scoreNewEdgeClearance(location, candidate, neighbours, positions);
        return score;
    }

    private double boundaryPenalty(Point2D candidate) {
        double distanceToBoundary = Math.min(
                Math.min(candidate.getX() - SAFE_AREA_MARGIN,
                        LAYOUT_WIDTH - SAFE_AREA_MARGIN - candidate.getX()),
                Math.min(candidate.getY() - SAFE_AREA_MARGIN,
                        LAYOUT_HEIGHT - SAFE_AREA_MARGIN - candidate.getY()));
        if (distanceToBoundary < 0) {
            return 5000 + (-distanceToBoundary * 50);
        }
        if (distanceToBoundary < PREFERRED_BOUNDARY_CLEARANCE) {
            return (PREFERRED_BOUNDARY_CLEARANCE - distanceToBoundary) * 30;
        }
        return 0;
    }

    private int countNewEdgeOverlaps(Point2D candidate,
            List<Point2D> neighbourPositions) {
        int overlaps = 0;
        double minimumCosine = Math.cos(Math.toRadians(NEW_EDGE_OVERLAP_ANGLE_DEGREES));
        for (int firstIndex = 0; firstIndex < neighbourPositions.size(); firstIndex++) {
            Point2D firstNeighbour = neighbourPositions.get(firstIndex);
            Point2D firstVector = firstNeighbour.subtract(candidate);
            double firstLength = firstVector.magnitude();
            if (firstLength == 0) {
                continue;
            }
            for (int secondIndex = firstIndex + 1;
                    secondIndex < neighbourPositions.size(); secondIndex++) {
                Point2D secondNeighbour = neighbourPositions.get(secondIndex);
                Point2D secondVector = secondNeighbour.subtract(candidate);
                double secondLength = secondVector.magnitude();
                if (secondLength == 0) {
                    continue;
                }
                double cosine = firstVector.dotProduct(secondVector)
                        / (firstLength * secondLength);
                if (cosine > minimumCosine) {
                    overlaps++;
                }
            }
        }
        return overlaps;
    }

    private double scoreNewEdgeNodeClearance(String location, Point2D candidate,
            List<String> neighbours, Map<String, Point2D> positions) {
        double score = 0;
        for (String neighbour : neighbours) {
            Point2D neighbourPosition = positions.get(neighbour);
            if (neighbourPosition == null) {
                continue;
            }
            for (Map.Entry<String, Point2D> entry : positions.entrySet()) {
                if (entry.getKey().equals(location) || entry.getKey().equals(neighbour)) {
                    continue;
                }
                double distance = distancePointToSegment(
                        entry.getValue(), candidate, neighbourPosition);
                if (distance < NEW_EDGE_NODE_CLEARANCE) {
                    score += 4000 + (NEW_EDGE_NODE_CLEARANCE - distance) * 100;
                }
            }
        }
        return score;
    }

    private double scoreNewEdgeClearance(String location, Point2D candidate,
            List<String> neighbours, Map<String, Point2D> positions) {
        double score = 0;
        Map<String, List<Edge>> adjacencyList = graph.getAdjacencyListSnapshot();

        for (String neighbour : neighbours) {
            Point2D neighbourPosition = positions.get(neighbour);
            if (neighbourPosition == null) {
                continue;
            }
            Set<String> checkedRoads = new HashSet<>();
            for (Map.Entry<String, List<Edge>> entry : adjacencyList.entrySet()) {
                Point2D first = positions.get(entry.getKey());
                if (first == null) {
                    continue;
                }
                for (Edge edge : entry.getValue()) {
                    String roadKey = canonicalRoadKey(entry.getKey(), edge.getDestination());
                    if (!checkedRoads.add(roadKey)
                            || entry.getKey().equals(neighbour)
                            || edge.getDestination().equals(neighbour)
                            || entry.getKey().equals(location)
                            || edge.getDestination().equals(location)) {
                        continue;
                    }
                    Point2D second = positions.get(edge.getDestination());
                    if (second == null) {
                        continue;
                    }
                    double distance = distanceBetweenSegments(
                            candidate, neighbourPosition, first, second);
                    if (distance < MINIMUM_EDGE_DISTANCE) {
                        score += 2500 + (MINIMUM_EDGE_DISTANCE - distance) * 60;
                    } else {
                        score += 25.0 / distance;
                    }
                }
            }
        }
        return score;
    }

    private int countNewEdgeCrossings(String location, Point2D candidate,
            List<String> neighbours, Map<String, Point2D> positions) {
        int crossings = 0;
        Map<String, List<Edge>> adjacencyList = graph.getAdjacencyListSnapshot();

        for (String neighbour : neighbours) {
            Set<String> checkedRoads = new HashSet<>();
            Point2D neighbourPosition = positions.get(neighbour);
            if (neighbourPosition == null) {
                continue;
            }
            for (Map.Entry<String, List<Edge>> entry : adjacencyList.entrySet()) {
                Point2D first = positions.get(entry.getKey());
                if (first == null) {
                    continue;
                }
                for (Edge edge : entry.getValue()) {
                    String roadKey = canonicalRoadKey(entry.getKey(), edge.getDestination());
                    if (!checkedRoads.add(roadKey)
                            || entry.getKey().equals(neighbour)
                            || edge.getDestination().equals(neighbour)
                            || entry.getKey().equals(location)
                            || edge.getDestination().equals(location)) {
                        continue;
                    }
                    Point2D second = positions.get(edge.getDestination());
                    if (second != null && segmentsProperlyIntersect(
                            candidate, neighbourPosition, first, second)) {
                        crossings++;
                    }
                }
            }
        }
        return crossings;
    }

    private boolean createsAnyIllegalEdgeCrossing(String location, Point2D candidate,
            List<String> neighbours, Map<String, Point2D> positions) {
        for (String neighbour : neighbours) {
            Point2D neighbourPosition = positions.get(neighbour);
            if (neighbourPosition == null) {
                continue;
            }
            for (Map.Entry<String, List<Edge>> entry : graph.getAdjacencyListSnapshot().entrySet()) {
                Point2D first = positions.get(entry.getKey());
                if (first == null) {
                    continue;
                }
                for (Edge edge : entry.getValue()) {
                    if (entry.getKey().equals(neighbour)
                            || edge.getDestination().equals(neighbour)
                            || entry.getKey().equals(location)
                            || edge.getDestination().equals(location)) {
                        continue;
                    }
                    Point2D second = positions.get(edge.getDestination());
                    if (second != null && segmentsProperlyIntersect(
                            candidate, neighbourPosition, first, second)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns true only when both segments meet in their interiors. Touching
     * at an endpoint is not a crossing; incident roads are also excluded by
     * createsAnyIllegalEdgeCrossing() before this method is called.
     */
    private boolean segmentsProperlyIntersect(Point2D first, Point2D second,
            Point2D third, Point2D fourth) {
        double firstOrientation = orientation(first, second, third);
        double secondOrientation = orientation(first, second, fourth);
        double thirdOrientation = orientation(third, fourth, first);
        double fourthOrientation = orientation(third, fourth, second);
        double epsilon = 1e-9;
        boolean firstOpposite = (firstOrientation > epsilon && secondOrientation < -epsilon)
                || (firstOrientation < -epsilon && secondOrientation > epsilon);
        boolean secondOpposite = (thirdOrientation > epsilon && fourthOrientation < -epsilon)
                || (thirdOrientation < -epsilon && fourthOrientation > epsilon);
        return firstOpposite && secondOpposite;
    }

    private double orientation(Point2D first, Point2D second, Point2D third) {
        return (second.getX() - first.getX()) * (third.getY() - first.getY())
                - (second.getY() - first.getY()) * (third.getX() - first.getX());
    }

    /**
     * Finds a free temporary slot inside the original graph area. This keeps
     * an isolated location from appearing far below the graph and avoids a
     * large fit-to-view shrink. If the original area is full, the last resort
     * is an adjacent slot that can be shown by the scroll pane.
     */
    private Point2D findTemporaryPosition(String location, Map<String, Point2D> positions) {
        int numberOfRows = (int) ((LAYOUT_HEIGHT - SAFE_AREA_MARGIN
                - TEMPORARY_GRID_START_Y) / TEMPORARY_GRID_SPACING) + 1;
        int numberOfColumns = (int) ((LAYOUT_WIDTH - SAFE_AREA_MARGIN
                - TEMPORARY_GRID_START_X) / TEMPORARY_GRID_SPACING) + 1;

        for (int row = 0; row < numberOfRows; row++) {
            for (int column = 0; column < numberOfColumns; column++) {
                Point2D candidate = new Point2D(
                        TEMPORARY_GRID_START_X + column * TEMPORARY_GRID_SPACING,
                        TEMPORARY_GRID_START_Y + row * TEMPORARY_GRID_SPACING);
                if (!isPositionTooClose(candidate, positions, MINIMUM_NODE_DISTANCE)
                        && !isPositionTooCloseToAnyRoad(location, candidate, positions,
                                MINIMUM_EDGE_DISTANCE)) {
                    return candidate;
                }
            }
        }

        int sideIndex = positions.size();
        Point2D candidate = new Point2D(
                LAYOUT_WIDTH + CANVAS_CONTENT_MARGIN + (sideIndex % 4) * TEMPORARY_GRID_SPACING,
                TEMPORARY_GRID_START_Y + (sideIndex / 4) * TEMPORARY_GRID_SPACING);
        while (isPositionTooClose(candidate, positions, MINIMUM_NODE_DISTANCE)
                || isPositionTooCloseToAnyRoad(location, candidate, positions,
                        MINIMUM_EDGE_DISTANCE)) {
            sideIndex++;
            candidate = new Point2D(
                    LAYOUT_WIDTH + CANVAS_CONTENT_MARGIN + (sideIndex % 4) * TEMPORARY_GRID_SPACING,
                    TEMPORARY_GRID_START_Y + (sideIndex / 4) * TEMPORARY_GRID_SPACING);
        }
        return candidate;
    }

    /**
     * Places a distance label beside the road rather than directly on its
     * centre line. Perpendicular alternatives reduce collisions at junctions;
     * location-name bounds and previously placed distance labels are checked
     * without changing the node positions.
     */
    private DistanceLabelPlacement findDistanceLabelPlacement(Point2D first, Point2D second,
            String text, Map<String, Point2D> positions,
            Iterable<LocationLabelPlacement> locationLabels,
            List<DistanceLabelPlacement> placedLabels) {
        double labelWidth = Math.max(30, text.length() * 6.2 + 8);
        double labelHeight = 18;
        double dx = second.getX() - first.getX();
        double dy = second.getY() - first.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            Point2D labelPosition = getDistanceLabelPosition(first, second, 0.0);
            Point2D centre = keepLabelInsideCanvas(labelPosition, labelWidth, labelHeight);
            return new DistanceLabelPlacement(
                    new Point2D(centre.getX() - labelWidth / 2, centre.getY() - labelHeight / 2),
                    centre, labelWidth, labelHeight);
        }

        Point2D alongRoad = new Point2D(dx / length, dy / length);
        double[] perpendicularOffsets = {14, -14};
        double[] parallelOffsets = {0, 8, -8};
        DistanceLabelPlacement bestPlacement = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (double perpendicularOffset : perpendicularOffsets) {
            for (double parallelOffset : parallelOffsets) {
                Point2D labelPosition = getDistanceLabelPosition(
                        first, second, perpendicularOffset);
                Point2D candidateCentre = labelPosition
                        .add(alongRoad.multiply(parallelOffset));
                Point2D centre = keepLabelInsideCanvas(candidateCentre, labelWidth, labelHeight);
                if (isDistanceLabelPositionFree(centre, labelWidth, labelHeight,
                        first, second, positions, locationLabels, placedLabels)) {
                    DistanceLabelPlacement candidate = toLabelPlacement(
                            centre, labelWidth, labelHeight);
                    double score = distanceLabelSpacingScore(candidate, locationLabels,
                            placedLabels);
                    if (score < bestScore) {
                        bestScore = score;
                        bestPlacement = candidate;
                    }
                }
            }
        }

        if (bestPlacement != null) {
            return bestPlacement;
        }

        // A label is still required even in a crowded layout. Choose the
        // candidate with the least overlap rather than moving it far from its road.
        for (double perpendicularOffset : perpendicularOffsets) {
            for (double parallelOffset : parallelOffsets) {
                Point2D labelPosition = getDistanceLabelPosition(
                        first, second, perpendicularOffset);
                Point2D candidateCentre = labelPosition
                        .add(alongRoad.multiply(parallelOffset));
                Point2D centre = keepLabelInsideCanvas(candidateCentre, labelWidth, labelHeight);
                DistanceLabelPlacement candidate = toLabelPlacement(
                        centre, labelWidth, labelHeight);
                double score = distanceLabelSpacingScore(candidate, locationLabels,
                        placedLabels);
                if (score < bestScore) {
                    bestScore = score;
                    bestPlacement = candidate;
                }
            }
        }
        return bestPlacement;
    }

    private Point2D getDistanceLabelPosition(Point2D a, Point2D b, double offset) {
        double midX = (a.getX() + b.getX()) / 2.0;
        double midY = (a.getY() + b.getY()) / 2.0;
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            return new Point2D(midX, midY);
        }

        double perpX = -dy / length;
        double perpY = dx / length;
        return new Point2D(midX + perpX * offset, midY + perpY * offset);
    }

    private DistanceLabelPlacement toLabelPlacement(Point2D centre,
            double width, double height) {
        return new DistanceLabelPlacement(
                new Point2D(centre.getX() - width / 2, centre.getY() - height / 2),
                centre, width, height);
    }

    private Point2D keepLabelInsideCanvas(Point2D centre, double width, double height) {
        double canvasWidth = Math.max(LAYOUT_WIDTH, graphCanvas.getPrefWidth());
        double canvasHeight = Math.max(LAYOUT_HEIGHT, graphCanvas.getPrefHeight());
        double x = Math.max(width / 2 + 4, Math.min(canvasWidth - width / 2 - 4, centre.getX()));
        double y = Math.max(height / 2 + 4, Math.min(canvasHeight - height / 2 - 4, centre.getY()));
        return new Point2D(x, y);
    }

    private boolean isDistanceLabelPositionFree(Point2D centre, double width, double height,
            Point2D first, Point2D second, Map<String, Point2D> positions,
            Iterable<LocationLabelPlacement> locationLabels,
            List<DistanceLabelPlacement> placedLabels) {
        for (Point2D nodePosition : positions.values()) {
            if (nodePosition.equals(first) || nodePosition.equals(second)) {
                continue;
            }
            if (centre.distance(nodePosition) < MINIMUM_LABEL_NODE_DISTANCE) {
                return false;
            }
        }

        if (overlapsLocationLabel(centre, width, height, locationLabels)) {
            return false;
        }

        for (DistanceLabelPlacement placed : placedLabels) {
            boolean overlapsHorizontally = Math.abs(centre.getX() - placed.centre().getX())
                    < (width + placed.width()) / 2 + MINIMUM_LABEL_GAP;
            boolean overlapsVertically = Math.abs(centre.getY() - placed.centre().getY())
                    < (height + placed.height()) / 2 + MINIMUM_LABEL_GAP;
            if (overlapsHorizontally && overlapsVertically) {
                return false;
            }
        }
        return true;
    }

    private boolean overlapsLocationLabel(Point2D centre, double width, double height,
            Iterable<LocationLabelPlacement> locationLabels) {
        for (LocationLabelPlacement locationLabel : locationLabels) {
            Point2D labelCentre = new Point2D(
                    locationLabel.topLeft().getX() + locationLabel.width() / 2,
                    locationLabel.topLeft().getY() + locationLabel.height() / 2);
            boolean overlapsHorizontally = Math.abs(centre.getX() - labelCentre.getX())
                    < (width + locationLabel.width()) / 2 + MINIMUM_LABEL_GAP;
            boolean overlapsVertically = Math.abs(centre.getY() - labelCentre.getY())
                    < (height + locationLabel.height()) / 2 + MINIMUM_LABEL_GAP;
            if (overlapsHorizontally && overlapsVertically) {
                return true;
            }
        }
        return false;
    }

    private double distanceLabelSpacingScore(DistanceLabelPlacement candidate,
            Iterable<LocationLabelPlacement> locationLabels,
            List<DistanceLabelPlacement> placedLabels) {
        double score = 0;
        for (LocationLabelPlacement locationLabel : locationLabels) {
            double overlapArea = rectangleOverlapArea(candidate.topLeft(), candidate.width(),
                    candidate.height(), locationLabel.topLeft(), locationLabel.width(),
                    locationLabel.height());
            score += overlapArea > 0
                    ? 1_000_000 + overlapArea
                    : 1.0 / (1.0 + rectangleDistance(candidate.topLeft(), candidate.width(),
                            candidate.height(), locationLabel.topLeft(), locationLabel.width(),
                            locationLabel.height()));
        }
        for (DistanceLabelPlacement placedLabel : placedLabels) {
            double overlapArea = rectangleOverlapArea(candidate.topLeft(), candidate.width(),
                    candidate.height(), placedLabel.topLeft(), placedLabel.width(),
                    placedLabel.height());
            score += overlapArea > 0
                    ? 1_000_000 + overlapArea
                    : 1.0 / (1.0 + rectangleDistance(candidate.topLeft(), candidate.width(),
                            candidate.height(), placedLabel.topLeft(), placedLabel.width(),
                            placedLabel.height()));
        }
        return score;
    }

    private double rectangleOverlapArea(Point2D firstTopLeft, double firstWidth,
            double firstHeight, Point2D secondTopLeft, double secondWidth,
            double secondHeight) {
        double overlapWidth = Math.min(firstTopLeft.getX() + firstWidth,
                secondTopLeft.getX() + secondWidth)
                - Math.max(firstTopLeft.getX(), secondTopLeft.getX());
        double overlapHeight = Math.min(firstTopLeft.getY() + firstHeight,
                secondTopLeft.getY() + secondHeight)
                - Math.max(firstTopLeft.getY(), secondTopLeft.getY());
        return overlapWidth > 0 && overlapHeight > 0
                ? overlapWidth * overlapHeight
                : 0;
    }

    private double rectangleDistance(Point2D firstTopLeft, double firstWidth,
            double firstHeight, Point2D secondTopLeft, double secondWidth,
            double secondHeight) {
        double horizontalGap = Math.max(
                secondTopLeft.getX() - (firstTopLeft.getX() + firstWidth),
                firstTopLeft.getX() - (secondTopLeft.getX() + secondWidth));
        double verticalGap = Math.max(
                secondTopLeft.getY() - (firstTopLeft.getY() + firstHeight),
                firstTopLeft.getY() - (secondTopLeft.getY() + secondHeight));
        return Math.hypot(Math.max(0, horizontalGap), Math.max(0, verticalGap));
    }

    /**
     * Prevents a dynamic node from being placed on top of an unrelated road.
     * Roads incident to the node being positioned are excluded because the
     * new road is expected to start at the new node's chosen position.
     */
    private boolean isPositionTooCloseToAnyRoad(String location, Point2D candidate,
            Map<String, Point2D> positions, double minimumDistance) {
        Set<String> checkedRoads = new HashSet<>();
        for (Map.Entry<String, List<Edge>> entry : graph.getAdjacencyListSnapshot().entrySet()) {
            if (entry.getKey().equals(location)) {
                continue;
            }
            Point2D first = positions.get(entry.getKey());
            if (first == null) {
                continue;
            }
            for (Edge edge : entry.getValue()) {
                if (edge.getDestination().equals(location)) {
                    continue;
                }
                String roadKey = canonicalRoadKey(entry.getKey(), edge.getDestination());
                if (!checkedRoads.add(roadKey)) {
                    continue;
                }
                Point2D second = positions.get(edge.getDestination());
                if (second != null
                        && distancePointToSegment(candidate, first, second) < minimumDistance) {
                    return true;
                }
            }
        }
        return false;
    }

    private double minimumDistanceToUnrelatedRoad(String location, Point2D candidate,
            Map<String, Point2D> positions) {
        double minimumDistance = Double.POSITIVE_INFINITY;
        Set<String> checkedRoads = new HashSet<>();
        for (Map.Entry<String, List<Edge>> entry : graph.getAdjacencyListSnapshot().entrySet()) {
            if (entry.getKey().equals(location)) {
                continue;
            }
            Point2D first = positions.get(entry.getKey());
            if (first == null) {
                continue;
            }
            for (Edge edge : entry.getValue()) {
                if (edge.getDestination().equals(location)) {
                    continue;
                }
                String roadKey = canonicalRoadKey(entry.getKey(), edge.getDestination());
                if (!checkedRoads.add(roadKey)) {
                    continue;
                }
                Point2D second = positions.get(edge.getDestination());
                if (second != null) {
                    minimumDistance = Math.min(minimumDistance,
                            distancePointToSegment(candidate, first, second));
                }
            }
        }
        return minimumDistance;
    }

    private double distancePointToSegment(Point2D point, Point2D first, Point2D second) {
        double dx = second.getX() - first.getX();
        double dy = second.getY() - first.getY();
        if (dx == 0 && dy == 0) {
            return point.distance(first);
        }

        double projection = ((point.getX() - first.getX()) * dx
                + (point.getY() - first.getY()) * dy) / (dx * dx + dy * dy);
        double clampedProjection = Math.max(0, Math.min(1, projection));
        Point2D nearestPoint = new Point2D(
                first.getX() + clampedProjection * dx,
                first.getY() + clampedProjection * dy);
        return point.distance(nearestPoint);
    }

    private double distanceBetweenSegments(Point2D first, Point2D second,
            Point2D third, Point2D fourth) {
        if (segmentsProperlyIntersect(first, second, third, fourth)) {
            return 0;
        }
        return Math.min(
                Math.min(distancePointToSegment(first, third, fourth),
                        distancePointToSegment(second, third, fourth)),
                Math.min(distancePointToSegment(third, first, second),
                        distancePointToSegment(fourth, first, second)));
    }

    private boolean isPositionTooClose(Point2D candidate, Map<String, Point2D> positions,
            double minimumDistance) {
        for (Point2D existing : positions.values()) {
            if (candidate.distance(existing) < minimumDistance) {
                return true;
            }
        }
        return false;
    }

    private boolean isInsideSafeArea(Point2D position) {
        return position.getX() >= SAFE_AREA_MARGIN
                && position.getX() <= LAYOUT_WIDTH - SAFE_AREA_MARGIN
                && position.getY() >= SAFE_AREA_MARGIN
                && position.getY() <= LAYOUT_HEIGHT - SAFE_AREA_MARGIN;
    }

    private boolean isInExpandableSafeArea(Point2D position) {
        return position.getX() >= SAFE_AREA_MARGIN
                && position.getY() >= SAFE_AREA_MARGIN;
    }

    private void ensureCanvasContains(Point2D position) {
        if (position == null || !Double.isFinite(position.getX())
                || !Double.isFinite(position.getY())) {
            return;
        }
        double requiredWidth = Math.max(LAYOUT_WIDTH,
                position.getX() + CANVAS_CONTENT_MARGIN);
        double requiredHeight = Math.max(LAYOUT_HEIGHT,
                position.getY() + CANVAS_CONTENT_MARGIN);
        if (requiredWidth > graphCanvas.getPrefWidth()) {
            graphCanvas.setPrefWidth(requiredWidth);
        }
        if (requiredHeight > graphCanvas.getPrefHeight()) {
            graphCanvas.setPrefHeight(requiredHeight);
        }
    }

    /**
     * Places a location label from the final node centre in the same graph
     * coordinate system. The label stays beside the circle with one fixed gap;
     * only its side changes when the available canvas boundary requires it.
     */
    private Label createLocationLabel(String location, boolean routeNode) {
        Label label = new Label(location);
        label.setStyle(routeNode
                ? "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #9a3412;"
                        + " -fx-background-color: #fff7ed; -fx-padding: 1px 2px;"
                : "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #0f172a;"
                        + " -fx-background-color: white; -fx-padding: 1px 2px;");
        return label;
    }

    private LabelDimensions measureLabel(Label label) {
        double width = label.prefWidth(-1);
        double height = label.prefHeight(-1);
        if (!Double.isFinite(width) || width <= 0) {
            width = Math.max(60, label.getText().length() * 6.3 + 8);
        }
        if (!Double.isFinite(height) || height <= 0) {
            height = 18;
        }
        return new LabelDimensions(width, height);
    }

    private Point2D locationLabelPosition(Label label, Point2D nodeCentre,
            double nodeRadius) {
        LabelDimensions dimensions = measureLabel(label);
        return locationLabelPosition(nodeCentre, nodeRadius,
                dimensions.width(), dimensions.height());
    }

    private Point2D locationLabelPosition(Point2D nodeCentre, double nodeRadius,
            double labelWidth, double labelHeight) {
        double canvasWidth = Math.max(LAYOUT_WIDTH, graphCanvas.getPrefWidth());
        double canvasHeight = Math.max(LAYOUT_HEIGHT, graphCanvas.getPrefHeight());
        double labelX = nodeCentre.getX() + nodeRadius + LOCATION_LABEL_GAP;
        if (labelX + labelWidth > canvasWidth - 4) {
            labelX = nodeCentre.getX() - nodeRadius - LOCATION_LABEL_GAP - labelWidth;
        }
        if (labelX < 4) {
            labelX = nodeCentre.getX() + nodeRadius + LOCATION_LABEL_GAP;
        }

        double labelY = nodeCentre.getY() - labelHeight / 2;
        labelY = Math.max(4, Math.min(labelY, canvasHeight - labelHeight - 4));
        return new Point2D(labelX, labelY);
    }

    private static String canonicalRoadKey(String first, String second) {
        return first.compareTo(second) < 0 ? first + "|" + second : second + "|" + first;
    }

    private void highlightRoute(List<String> route) {
        highlightedRouteNodes.clear();
        highlightedRouteRoads.clear();
        highlightedRouteNodes.addAll(route);
        for (int index = 0; index + 1 < route.size(); index++) {
            highlightedRouteRoads.add(canonicalRoadKey(route.get(index), route.get(index + 1)));
        }
    }

    private void clearRouteHighlight() {
        highlightedRouteNodes.clear();
        highlightedRouteRoads.clear();
        resultOutput.setText("No route search result.");
        refreshGraphVisualization();
    }

    private void complete(String message) {
        status.setStyle("-fx-text-fill: #1f5f32;");
        status.setText(message);
        refreshNetworkOutput();
    }

    private void fail(String message) {
        status.setStyle("-fx-text-fill: #a51d2d;");
        status.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid operation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String formatKm(double value) {
        return value == Math.rint(value)
                ? String.format(Locale.ROOT, "%.0f", value)
                : String.format(Locale.ROOT, "%.2f", value);
    }
}
