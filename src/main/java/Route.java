import java.util.List;

public class Route {
    private final List<Station> stations;
    private final int totalStations;
    private final int totalTime;

    public Route(List<Station> stations, int totalStations, int totalTime) {
        this.stations = stations;
        this.totalStations = totalStations;
        this.totalTime = totalTime;
    }

    // Getters
    public List<Station> getStations() { return stations; }
    public int getTotalStations() { return totalStations; }
    public int getTotalTime() { return totalTime; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Route (").append(totalStations).append(" stations, ")
                .append(totalTime).append(" seconds):\n");

        for (Station station : stations) {
            sb.append("• ").append(station.getName()).append("\n");
        }

        return sb.toString();
    }
}