import javafx.application.Application;
public class Main {
    public static void main(String[] args) {
        // Test database connection
        DatabaseConnection.testConnection();

        // Launch JavaFX application
        Application.launch(MetroApp.class, args);
    }
}
