package gpsnavigation;

import java.util.Locale;

/** A weighted adjacency-list entry for one directed view of an undirected road. */
public final class Edge {
    private final String destination;
    private final double distanceKm;

    public Edge(String destination, double distanceKm) {
        this.destination = destination;
        this.distanceKm = distanceKm;
    }

    public String getDestination() {
        return destination;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    @Override
    public String toString() {
        return destination + " (" + formatDistance(distanceKm) + " km)";
    }

    private static String formatDistance(double distance) {
        if (distance == Math.rint(distance)) {
            return String.format(Locale.ROOT, "%.0f", distance);
        }
        return String.format(Locale.ROOT, "%.2f", distance);
    }
}
