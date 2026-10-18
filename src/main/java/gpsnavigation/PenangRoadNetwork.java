package gpsnavigation;

/** Initial weighted network from the revised proposal. */
public final class PenangRoadNetwork {
    private PenangRoadNetwork() {
    }

    /**
     * Creates the 17-location network with 22 confirmed weighted roads.
     */
    public static WeightedGraph createInitialGraph() {
        WeightedGraph graph = new WeightedGraph();
        String[] locations = {
                "Teluk Bahang", "Batu Ferringhi", "Tanjung Bungah", "Air Itam",
                "George Town", "Balik Pulau", "Gelugor", "Bayan Lepas", "Butterworth",
                "Perai", "Kepala Batas", "Sungai Petani", "Tasek Gelugor",
                "Bukit Mertajam", "Kulim", "Batu Kawan", "Nibong Tebal"
        };
        for (String location : locations) {
            graph.addLocation(location);
        }

        add(graph, "Teluk Bahang", "Batu Ferringhi", 5);
        add(graph, "Teluk Bahang", "Balik Pulau", 20);
        add(graph, "Batu Ferringhi", "Tanjung Bungah", 7);
        add(graph, "Tanjung Bungah", "Air Itam", 9);
        add(graph, "Tanjung Bungah", "George Town", 9);
        add(graph, "Air Itam", "Balik Pulau", 14);
        add(graph, "Air Itam", "George Town", 7);
        add(graph, "George Town", "Gelugor", 7);
        add(graph, "George Town", "Butterworth", 5);
        add(graph, "Butterworth", "Perai", 4);
        add(graph, "Balik Pulau", "Bayan Lepas", 16);
        add(graph, "Gelugor", "Bayan Lepas", 9);
        add(graph, "Gelugor", "Perai", 10);
        add(graph, "Bayan Lepas", "Batu Kawan", 24);
        add(graph, "Butterworth", "Kepala Batas", 15);
        add(graph, "Perai", "Bukit Mertajam", 11);
        add(graph, "Kepala Batas", "Sungai Petani", 18);
        add(graph, "Kepala Batas", "Tasek Gelugor", 11);
        add(graph, "Tasek Gelugor", "Bukit Mertajam", 16);
        add(graph, "Bukit Mertajam", "Kulim", 15);
        add(graph, "Bukit Mertajam", "Batu Kawan", 20);
        add(graph, "Batu Kawan", "Nibong Tebal", 11);
        return graph;
    }

    private static void add(WeightedGraph graph, String first, String second, double distanceKm) {
        graph.addRoad(first, second, distanceKm);
    }
}
