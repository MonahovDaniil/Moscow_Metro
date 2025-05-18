public class MetroConnection {
    private final int id;
    private final int station1Id;
    private final int station2Id;
    private final int travelTime;
    private final boolean isTransfer;

    public MetroConnection(int id, int station1Id, int station2Id, int travelTime) {
        this.id = id;
        this.station1Id = station1Id;
        this.station2Id = station2Id;
        this.travelTime = travelTime;
        // Determine if this is a transfer connection based on station IDs
        // This is a simplification; in a real app, you might need more complex logic
        this.isTransfer = false;
    }

    // Constructor for backward compatibility
    public MetroConnection(int station1Id, int station2Id, int travelTime, boolean isTransfer) {
        this.id = 0; // Default ID
        this.station1Id = station1Id;
        this.station2Id = station2Id;
        this.travelTime = travelTime;
        this.isTransfer = isTransfer;
    }

    // Getters
    public int getId() { return id; }
    public int getStation1Id() { return station1Id; }
    public int getStation2Id() { return station2Id; }
    public int getTravelTime() { return travelTime; }
    public boolean isTransfer() { return isTransfer; }
}
