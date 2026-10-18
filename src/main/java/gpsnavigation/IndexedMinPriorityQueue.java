package gpsnavigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binary min-heap with decrease-key support. There is at most one active
 * entry per vertex, which makes Dijkstra's priority updates O(log V) and its
 * auxiliary queue space O(V).
 */
final class IndexedMinPriorityQueue {
    static final class Entry {
        private final String vertex;
        private final double priority;

        Entry(String vertex, double priority) {
            this.vertex = vertex;
            this.priority = priority;
        }

        String getVertex() {
            return vertex;
        }

        double getPriority() {
            return priority;
        }
    }

    private final List<Entry> heap = new ArrayList<>();
    private final Map<String, Integer> positions = new HashMap<>();

    boolean isEmpty() {
        return heap.isEmpty();
    }

    void addOrDecrease(String vertex, double priority) {
        Integer position = positions.get(vertex);
        if (position == null) {
            positions.put(vertex, heap.size());
            heap.add(new Entry(vertex, priority));
            siftUp(heap.size() - 1);
        } else if (priority < heap.get(position).getPriority()) {
            heap.set(position, new Entry(vertex, priority));
            siftUp(position);
        }
    }

    Entry removeMin() {
        if (heap.isEmpty()) {
            throw new IllegalStateException("Priority queue is empty.");
        }

        Entry result = heap.get(0);
        Entry last = heap.remove(heap.size() - 1);
        positions.remove(result.getVertex());
        if (!heap.isEmpty()) {
            heap.set(0, last);
            positions.put(last.getVertex(), 0);
            siftDown(0);
        }
        return result;
    }

    private void siftUp(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (compare(heap.get(parent), heap.get(index)) <= 0) {
                break;
            }
            swap(parent, index);
            index = parent;
        }
    }

    private void siftDown(int index) {
        while (true) {
            int left = index * 2 + 1;
            int right = left + 1;
            int smallest = index;
            if (left < heap.size() && compare(heap.get(left), heap.get(smallest)) < 0) {
                smallest = left;
            }
            if (right < heap.size() && compare(heap.get(right), heap.get(smallest)) < 0) {
                smallest = right;
            }
            if (smallest == index) {
                return;
            }
            swap(index, smallest);
            index = smallest;
        }
    }

    private static int compare(Entry first, Entry second) {
        int priorityComparison = Double.compare(first.getPriority(), second.getPriority());
        return priorityComparison != 0
                ? priorityComparison
                : first.getVertex().compareToIgnoreCase(second.getVertex());
    }

    private void swap(int first, int second) {
        Entry temporary = heap.get(first);
        heap.set(first, heap.get(second));
        heap.set(second, temporary);
        positions.put(heap.get(first).getVertex(), first);
        positions.put(heap.get(second).getVertex(), second);
    }
}
