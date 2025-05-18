public class Station {
    private final int id;
    private final String name;
    private final String line;
    private final int x;
    private final int y;
    private final boolean isTransfer;

    // Constructor, getters and setters
    public Station(int id, String name, String line, int x, int y, boolean isTransfer) {
        this.id = id;
        this.name = name;
        this.line = line;
        this.x = x;
        this.y = y;
        this.isTransfer = isTransfer;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getLine() { return line; }
    public int getLineId() { 
        // For backward compatibility, try to parse line as integer
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            // Map line names to line IDs
            switch(line.toLowerCase()) {
                case "кольцевая": return 1;
                case "сокольническая": return 2;
                case "замоскворецкая": return 3;
                case "арбатско-покровская": return 4;
                case "калужско-рижская": return 5;
                case "филёвская": return 6;
                default: return 1; // Default to 1 if line name is not recognized
            }
        }
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isTransfer() { return isTransfer; }

    @Override
    public String toString() {
        return name;
    }
}
