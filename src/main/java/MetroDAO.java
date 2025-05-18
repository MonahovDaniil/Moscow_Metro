import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MetroDAO {
    public List<Station> getAllStations() throws SQLException {
        List<Station> stations = new ArrayList<>();
        Map<Integer, Set<Integer>> stationConnections = getStationConnections();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM stations")) {

            while (rs.next()) {
                int stationId = rs.getInt("id");
                // A station is a transfer station if it has connections to stations on different lines
                boolean isTransfer = isTransferStation(stationId, stationConnections);

                stations.add(new Station(
                        stationId,
                        rs.getString("name"),
                        rs.getString("line"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        isTransfer
                ));
            }
        }

        return stations;
    }

    private Map<Integer, Set<Integer>> getStationConnections() throws SQLException {
        Map<Integer, Set<Integer>> stationConnections = new HashMap<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT station1_id, station2_id FROM connections")) {

            while (rs.next()) {
                int station1Id = rs.getInt("station1_id");
                int station2Id = rs.getInt("station2_id");

                stationConnections.computeIfAbsent(station1Id, k -> new HashSet<>()).add(station2Id);
                stationConnections.computeIfAbsent(station2Id, k -> new HashSet<>()).add(station1Id);
            }
        }

        return stationConnections;
    }

    private boolean isTransferStation(int stationId, Map<Integer, Set<Integer>> stationConnections) throws SQLException {
        // If the station has no connections, it's not a transfer station
        if (!stationConnections.containsKey(stationId)) {
            return false;
        }

        // Get the line of this station
        String stationLine = getStationLine(stationId);
        if (stationLine == null) {
            return false;
        }

        // Check if any connected station is on a different line
        for (int connectedStationId : stationConnections.get(stationId)) {
            String connectedStationLine = getStationLine(connectedStationId);
            if (connectedStationLine != null && !connectedStationLine.equals(stationLine)) {
                return true;
            }
        }

        return false;
    }

    private String getStationLine(int stationId) throws SQLException {
        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT line FROM stations WHERE id = ?")) {

            stmt.setInt(1, stationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("line");
                }
            }
        }

        return null;
    }

    public List<MetroConnection> getAllConnections() throws SQLException {
        List<MetroConnection> connections = new ArrayList<>();

        try (java.sql.Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM connections")) {

            while (rs.next()) {
                connections.add(new MetroConnection(
                        rs.getInt("id"),
                        rs.getInt("station1_id"),
                        rs.getInt("station2_id"),
                        rs.getInt("travel_time")
                ));
            }
        }

        return connections;
    }

    public Map<Integer, Station> getStationMap() throws SQLException {
        Map<Integer, Station> stationMap = new HashMap<>();
        List<Station> stations = getAllStations();

        for (Station station : stations) {
            stationMap.put(station.getId(), station);
        }

        return stationMap;
    }

    public Map<Integer, List<MetroConnection>> getConnectionMap() throws SQLException {
        Map<Integer, List<MetroConnection>> connectionMap = new HashMap<>();
        List<MetroConnection> connections = getAllConnections();

        for (MetroConnection conn : connections) {
            connectionMap.computeIfAbsent(conn.getStation1Id(), k -> new ArrayList<>()).add(conn);
            // Add reverse connection for bidirectional travel
            // Using the backward-compatible constructor to maintain the isTransfer property
            connectionMap.computeIfAbsent(conn.getStation2Id(), k -> new ArrayList<>()).add(
                    new MetroConnection(conn.getStation2Id(), conn.getStation1Id(),
                            conn.getTravelTime(), conn.isTransfer())
            );
        }

        return connectionMap;
    }
}
