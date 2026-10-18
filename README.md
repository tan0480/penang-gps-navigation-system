# Penang GPS Navigation System

This JavaFX coursework prototype models a weighted, undirected Penang road network without Maven. BFS finds routes with the fewest road connections; Dijkstra finds routes with the shortest total distance. The graph uses a fixed demonstration dataset, not live GPS or traffic information.

## Setup requirements

- A Java 21 JDK. Set `JAVA_HOME` to its installation directory (the directory containing `bin`).
- JavaFX SDK 21.x for Windows, matching the JDK architecture. See the [official JavaFX setup guide](https://openjfx.io/openjfx-docs/).
- Create `lib` and copy the SDK's JavaFX JAR files there. Create `bin` and copy the SDK's Windows native runtime DLLs there.
- Optionally, VS Code with the Java Extension Pack. Configure the main class as `gpsnavigation.GpsNavigationApp`, the module path as `lib`, and the native library path as `bin`.

JavaFX SDK binaries, local IDE settings and compilation output are intentionally excluded from Git. The commands below use your own JDK rather than a machine-specific VS Code extension runtime. Keep the JavaFX SDK's accompanying licence files in your local SDK installation.

## Compile and run from PowerShell

From the repository root, after setting `JAVA_HOME` and preparing `lib` and `bin`:

```powershell
$jdk = $env:JAVA_HOME
if (-not $jdk -or -not (Test-Path -LiteralPath "$jdk\bin\javac.exe")) { throw 'Set JAVA_HOME to a Java 21 JDK' }
$mainFiles = Get-ChildItem -LiteralPath src\main\java\gpsnavigation -File | Where-Object Extension -eq '.java'
New-Item -ItemType Directory -Force out | Out-Null
& "$jdk\bin\javac.exe" --release 21 -d out --module-path lib --add-modules javafx.controls,javafx.fxml $mainFiles.FullName
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$jdk\bin\java.exe" "-Djava.library.path=$PWD\bin" --module-path lib --add-modules javafx.controls,javafx.fxml -cp out gpsnavigation.GpsNavigationApp
```

If you configure a local VS Code launch profile, you can also run the app through Run and Debug. The JavaFX window starts with the proposal's 17 locations and 22 confirmed weighted roads, including Butterworth-Perai at 4 km.

The `Current Road Network` panel renders the same adjacency-list data as a graph: each location is a labelled node, each unique undirected road is one line, and each line has its distance label. The adjacency list remains below the visual graph on the right, while the persistent Search Result panel is anchored at the bottom of the left controls and grows into the remaining height below Route Algorithms. The original proposal coordinates, node radius, and label size stay fixed. The graph is inside a pannable `ScrollPane` with a vertically growing viewport; its drawing pane grows when a dynamic location needs more room, so adding locations does not rescale or shrink the original graph.

Location lookup is case-insensitive. For example, `george town`, `George Town`, and `GEORGE TOWN` all resolve to the canonical displayed name `George Town`. A new location keeps its trimmed spelling as its canonical display name, and a case-only duplicate is rejected.

The interface uses a text field only for adding a new location. The remove-location, road-endpoint, and route start/destination inputs are non-editable location dropdowns populated from the current graph. The shared Start and Destination dropdowns are used by both BFS and Dijkstra; they default to Air Itam and Bayan Lepas when those locations are present. All dropdowns refresh after a successful graph change or proposal-graph reload, preserve valid selections where possible, and clear selections that are no longer available.

New locations use the existing proposal coordinates when available. Otherwise, an isolated location receives a free temporary grid position inside the original graph area rather than being placed far below it. When a dynamic location has roads, the renderer calculates the centroid of all connected neighbours and evaluates 64 radial candidates. The candidate score considers node overlap, distance from unrelated road segments, existing-edge crossings, overlap between the proposed new roads, clearance from other nodes, boundary proximity, and the total length of the new roads. Candidates stay inside the safe area when possible and use the expandable canvas only when necessary; a connected node never falls back to an unrelated global grid. A dynamic position is stored and remains stable during ordinary redraws, but is recalculated only when that location's neighbour count changes so a later road can improve a crowded layout without moving the original 17 locations.

Running BFS or Dijkstra highlights the exact returned route: route nodes use orange styling, route roads use thicker red lines, and their distance labels remain visible. Starting another search clears the old highlight; resetting or successfully changing the graph also clears it. An unavailable route displays `No route found.` and does not highlight unrelated graph elements.

## Run the no-build-tool tests

The test runner is plain Java so it does not require JUnit or Maven:

```powershell
$jdk = $env:JAVA_HOME
if (-not $jdk -or -not (Test-Path -LiteralPath "$jdk\bin\javac.exe")) { throw 'Set JAVA_HOME to a Java 21 JDK' }
$mainFiles = Get-ChildItem -LiteralPath src\main\java\gpsnavigation -File | Where-Object Extension -eq '.java'
$testFiles = Get-ChildItem -LiteralPath src\test\java\gpsnavigation -File | Where-Object Extension -eq '.java'
New-Item -ItemType Directory -Force out | Out-Null
& "$jdk\bin\javac.exe" --release 21 -d out --module-path lib --add-modules javafx.controls,javafx.fxml $mainFiles.FullName $testFiles.FullName
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
& "$jdk\bin\java.exe" "-Djava.library.path=$PWD\bin" --module-path lib --add-modules javafx.controls,javafx.fxml -cp out gpsnavigation.WeightedGraphTest
```

## Supported graph operations

`WeightedGraph` uses `Map<String, List<Edge>>` as an adjacency list. `addLocation`, `removeLocation`, `addRoad`, `removeRoad`, `containsLocation`, `containsRoad`, graph display, BFS, and Dijkstra are implemented. Every road is stored in both endpoint lists, so the graph is undirected. Each `Edge` stores a destination and positive distance in kilometres. Lists are kept alphabetically for deterministic output.

The revised proposal resolves the earlier weighting conflict by requiring a weighted graph and Dijkstra. It still correctly specifies that BFS ignores weights and minimises the number of road connections. The confirmed Butterworth-Perai distance of 4 km is included in the initial graph.

The graph contains all 17 proposal locations and all 22 roads. `sample-output.txt` demonstrates the required Air Itam-to-Bayan Lepas comparison: BFS returns 2 connections/30 km, while Dijkstra returns 3 connections/23 km.

The custom `IndexedMinPriorityQueue` uses decrease-key semantics with one active entry per vertex. This is the priority-queue strategy used by Dijkstra and supports the proposal's `O((V + E) log V)` worst-case time and `O(V)` queue space.

## Proposal-aligned pseudocode

```text
ADD_ROAD(A, B, distance)
    validate that A and B exist, are different, and distance > 0
    reject the road if A-B already exists
    insert (B, distance) into A's list
    insert (A, distance) into B's list

BFS_ROUTE(start, destination)
    queue <- [start]; parent[start] <- null; hops[start] <- 0
    while queue is not empty and destination not found
        current <- dequeue queue; add current to traversal order
        for each edge of current in alphabetical order
            if neighbour is not visited
                parent[neighbour] <- current
                hops[neighbour] <- hops[current] + 1
                enqueue neighbour
                stop when neighbour is destination
    reconstruct the route by following parent links backwards

DIJKSTRA_ROUTE(start, destination)
    distance of start <- 0; distance of every other location <- infinity
    insert start into an indexed minimum heap
    while heap is not empty
        current <- remove the location with minimum tentative distance
        for each edge (current, neighbour, weight)
            if distance[current] + weight < distance[neighbour]
                update distance and parent; decrease heap priority
    reconstruct the minimum-distance route using parent links
```

For `V` locations, `E` undirected roads, and `deg(v)` neighbours of one location, the adjacency list stores `2E` edge records. Graph construction and display are `O(V + E)` time and storage. Add/remove road operations are `O(deg(A) + deg(B))` worst-case because the array lists may be searched; add location is `O(1)` expected; and remove location is `O(V + E)` worst-case. BFS is `O(1)` best case when start equals destination and `O(V + E)` worst case, using `O(1)` best-case and `O(V)` worst-case auxiliary search space. Dijkstra is `O(1)` best case when start equals destination and `O((V + E) log V)` worst case with the indexed decrease-key heap, using `O(1)` best-case and `O(V)` worst-case auxiliary search space.

## Reference implementation mapping

The supplied `UnweightedGraph.java`, `Graph.java`, and `AbstractGraph.java` were used only as structural references for adjacency lists, edges, queues, and parent-based searches. Their unweighted integer-index API was not copied because the revised proposal requires named locations, kilometre weights, and Dijkstra's algorithm. The deleted `pom.xml` and JUnit dependency were not required by the attached no-build-tools setup; the test source was retained as a plain Java test runner.

## Coding contributions

The module ownership below follows the project's AMCS2034 report and the team's confirmed import plan.

| Contributor | Coding responsibility | Source mapping |
| --- | --- | --- |
| [Tan Yi Heng](https://github.com/tan0480) | Weighted adjacency-list graph, location/road operations, validation and graph-operation testing. | Graph-operation portions of `WeightedGraph.java`, `Edge.java`, `PenangRoadNetwork.java`. |
| [Lam Yi Hen](https://github.com/lamyihen77-beep) | BFS for the fewest road connections, queue/parent tracking, route reconstruction and BFS testing. | `WeightedGraph.bfsRoute()`, `BfsResult.java`, shared route reconstruction. |
| [Lim Jun Yi](https://github.com/ljunyi0212) | Dijkstra shortest-distance search, minimum-priority queue, relaxation, route reconstruction and shortest-path testing. | `WeightedGraph.dijkstraRoute()`, `DijkstraResult.java`, `IndexedMinPriorityQueue.java`, shared route reconstruction. |
| [Cheah Yong Shun](https://github.com/ys012007) | JavaFX controls, graph visualisation, route highlighting, UI integration and overall system testing. | `GpsNavigationApp.java`; integration tests in `WeightedGraphTest.java`, with the other three contributors co-authored for their module tests. |

This repository imports an existing coursework project, not a reconstruction of its original development dates. `WeightedGraph.java` is imported in graph/BFS/Dijkstra portions to preserve module attribution without restructuring the final source. Shared work uses co-author trailers. Tan Yi Heng prepares the import commits and pushes them; the author identifies the contributor to the imported module.

The repository includes seven application Java files, one Java test runner, this README, `.gitignore` and `sample-output.txt`. Assignment reports and third-party SDK binaries are not uploaded. Tests include graph operations, case-insensitive lookup, invalid/disconnected routes, BFS/Dijkstra results and selected layout checks; they do not replace a complete manual UI test.
