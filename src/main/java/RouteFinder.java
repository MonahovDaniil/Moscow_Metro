import java.util.*;

public class RouteFinder {
    private final Map<Integer, Station> stationMap;
    private final Map<Integer, List<MetroConnection>> connectionMap;

    public RouteFinder(Map<Integer, Station> stationMap,
                       Map<Integer, List<MetroConnection>> connectionMap) {
        this.stationMap = stationMap;
        this.connectionMap = connectionMap;
    }

    public List<Route> findRoutes(int startId, int endId) {
        List<Route> routes = new ArrayList<>();

        // Find shortest path by stations count
        Route shortestByStations = findShortestPath(startId, endId, false);
        if (shortestByStations != null) {
            routes.add(shortestByStations);
        }

        // Find fastest path by time
        Route fastestByTime = findShortestPath(startId, endId, true);
        if (fastestByTime != null) {
            routes.add(fastestByTime);
        }

        // Find path with minimum transfers
        Route minTransfersRoute = findMinTransfersPath(startId, endId);
        if (minTransfersRoute != null) {
            routes.add(minTransfersRoute);
        }

        return routes;
    }

    private Route findShortestPath(int startId, int endId, boolean useTime) {
        PriorityQueue<RouteNode> queue = new PriorityQueue<>();
        Map<Integer, Integer> distances = new HashMap<>();
        Map<Integer, RouteNode> previousNodes = new HashMap<>();

        // Initialize distances
        for (Integer stationId : stationMap.keySet()) {
            distances.put(stationId, Integer.MAX_VALUE);
        }
        distances.put(startId, 0);

        queue.add(new RouteNode(startId, 0, 0));

        while (!queue.isEmpty()) {
            RouteNode current = queue.poll();

            if (current.stationId == endId) {
                return buildRoute(current, previousNodes);
            }

            if (connectionMap.containsKey(current.stationId)) {
                for (MetroConnection conn : connectionMap.get(current.stationId)) {
                    int neighborId = conn.getStation2Id();
                    int newDistance = useTime ?
                            current.totalTime + conn.getTravelTime() :
                            current.stationsCount + 1;

                    if (newDistance < distances.get(neighborId)) {
                        distances.put(neighborId, newDistance);
                        RouteNode neighborNode = new RouteNode(
                                neighborId,
                                current.stationsCount + 1,
                                current.totalTime + conn.getTravelTime()
                        );
                        previousNodes.put(neighborId, current);
                        queue.add(neighborNode);
                    }
                }
            }
        }

        return null;
    }

    private Route findMinTransfersPath(int startId, int endId) {
        // Create a priority queue that prioritizes routes with fewer transfers
        PriorityQueue<RouteNode> queue = new PriorityQueue<>(
            (a, b) -> {
                // First compare by number of transfers
                int transferComparison = Integer.compare(a.transfers, b.transfers);
                if (transferComparison != 0) {
                    return transferComparison;
                }
                // If transfers are equal, compare by station count
                return Integer.compare(a.stationsCount, b.stationsCount);
            }
        );

        // Track both minimum transfers and minimum stations for each transfer count
        Map<Integer, Integer> minTransfers = new HashMap<>();
        Map<Integer, Integer> minStations = new HashMap<>();
        Map<Integer, RouteNode> previousNodes = new HashMap<>();

        // Initialize min transfers and min stations
        for (Integer stationId : stationMap.keySet()) {
            minTransfers.put(stationId, Integer.MAX_VALUE);
            minStations.put(stationId, Integer.MAX_VALUE);
        }
        minTransfers.put(startId, 0);
        minStations.put(startId, 0);

        queue.add(new RouteNode(startId, 0, 0, 0));

        while (!queue.isEmpty()) {
            RouteNode current = queue.poll();

            if (current.stationId == endId) {
                return buildRoute(current, previousNodes);
            }

            if (connectionMap.containsKey(current.stationId)) {
                Station currentStation = stationMap.get(current.stationId);

                for (MetroConnection conn : connectionMap.get(current.stationId)) {
                    int neighborId = conn.getStation2Id();
                    Station neighborStation = stationMap.get(neighborId);

                    // Check if this connection involves a transfer (different lines)
                    boolean isTransfer = currentStation.getLineId() != neighborStation.getLineId();
                    int newTransfers = current.transfers + (isTransfer ? 1 : 0);

                    // Calculate new station count
                    int newStationCount = current.stationsCount + 1;

                    // If we found a path with fewer transfers, or same transfers but fewer stations
                    if (newTransfers < minTransfers.get(neighborId) || 
                        (newTransfers == minTransfers.get(neighborId) && 
                         newStationCount < minStations.get(neighborId))) {

                        // Update both transfers and stations count
                        minTransfers.put(neighborId, newTransfers);
                        minStations.put(neighborId, newStationCount);

                        RouteNode neighborNode = new RouteNode(
                                neighborId,
                                newStationCount,
                                current.totalTime + conn.getTravelTime(),
                                newTransfers
                        );
                        previousNodes.put(neighborId, current);
                        queue.add(neighborNode);
                    }
                }
            }
        }

        return null;
    }

    private Route buildRoute(RouteNode endNode, Map<Integer, RouteNode> previousNodes) {
        LinkedList<Station> path = new LinkedList<>();
        RouteNode current = endNode;

        while (current != null) {
            path.addFirst(stationMap.get(current.stationId));
            current = previousNodes.get(current.stationId);
        }

        return new Route(path, endNode.stationsCount, endNode.totalTime);
    }

    private static class RouteNode implements Comparable<RouteNode> {
        int stationId;
        int stationsCount;
        int totalTime;
        int transfers;

        RouteNode(int stationId, int stationsCount, int totalTime) {
            this.stationId = stationId;
            this.stationsCount = stationsCount;
            this.totalTime = totalTime;
            this.transfers = 0;
        }

        RouteNode(int stationId, int stationsCount, int totalTime, int transfers) {
            this.stationId = stationId;
            this.stationsCount = stationsCount;
            this.totalTime = totalTime;
            this.transfers = transfers;
        }

        @Override
        public int compareTo(RouteNode other) {
            return Integer.compare(this.totalTime, other.totalTime);
        }
    }
}
