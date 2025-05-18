import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MetroApp extends Application {
    private ComboBox<Station> startStationCombo;
    private ComboBox<Station> endStationCombo;
    private ListView<Route> routesList;
    private Label routeSummaryLabel;
    private MetroMap metroMap;
    private Map<Integer, Station> stationMap;
    private Map<Integer, List<MetroConnection>> connectionMap;
    private RouteFinder routeFinder;
    private boolean selectingStartStation = true; // true = selecting start station, false = selecting end station

    @Override
    public void start(Stage primaryStage) {
        try {
            // Инициализация данных
            initializeData();

            // Настройка пользовательского интерфейса
            setupUI(primaryStage);

        } catch (SQLException e) {
            showAlertAndExit("Критическая ошибка",
                    "Не удалось инициализировать приложение: " + e.getMessage());
        }
    }

    private void initializeData() throws SQLException {
        MetroDAO metroDAO = new MetroDAO();
        stationMap = metroDAO.getStationMap();
        connectionMap = metroDAO.getConnectionMap();
        routeFinder = new RouteFinder(stationMap, connectionMap);
        metroMap = new MetroMap(stationMap, connectionMap);

        // Set up station selection callback
        metroMap.setOnStationSelected(this::handleStationSelection);
    }

    /**
     * Handles station selection from the map
     * @param station The selected station
     */
    private void handleStationSelection(Station station) {
        if (selectingStartStation) {
            // First click - select start station
            startStationCombo.setValue(station);
            selectingStartStation = false;
        } else {
            // Second click - select end station
            endStationCombo.setValue(station);
            selectingStartStation = true;

            // Automatically find routes after selecting both stations
            findRoutes();
        }
    }

    private void setupUI(Stage stage) {
        // Создание компонентов UI
        ObservableList<Station> stationsList = FXCollections.observableArrayList(stationMap.values());
        startStationCombo = new ComboBox<>();
        endStationCombo = new ComboBox<>();
        Button findRouteButton = new Button("Найти маршрут");
        routesList = new ListView<>();
        routesList.setPrefWidth(450); // Устанавливаем предпочтительную ширину

        // Настройка компонентов
        startStationCombo.setPromptText("Введите название станции");
        endStationCombo.setPromptText("Введите название станции");
        startStationCombo.setMinWidth(200);
        endStationCombo.setMinWidth(200);

        // Make ComboBoxes editable to enable search
        startStationCombo.setEditable(true);
        endStationCombo.setEditable(true);

        // Set up search functionality for start station ComboBox
        setupStationSearch(startStationCombo, stationsList);

        // Set up search functionality for end station ComboBox
        setupStationSearch(endStationCombo, stationsList);

        // Настройка отображения маршрутов
        routesList.setCellFactory(param -> new ListCell<Route>() {
            @Override
            protected void updateItem(Route item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Get the index of the item in the list
                    int index = getIndex();
                    // Format the route with the appropriate label based on its index
                    setText(formatRoute(item, index));
                }
            }
        });

        // Обработчики событий
        findRouteButton.setOnAction(e -> findRoutes());

        // Выбор маршрута для отображения на карте
        routesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                metroMap.highlightRoute(newVal.getStations());
            }
        });

        // Разметка
        GridPane controlPanel = new GridPane();
        controlPanel.setHgap(10);
        controlPanel.setVgap(10);
        controlPanel.setPadding(new Insets(20));
        controlPanel.add(new Label("Откуда:"), 0, 0);
        controlPanel.add(startStationCombo, 1, 0);
        controlPanel.add(new Label("Куда:"), 0, 1);
        controlPanel.add(endStationCombo, 1, 1);
        controlPanel.add(findRouteButton, 1, 2);

        // Основной макет
        HBox root = new HBox(10); // Use HBox instead of BorderPane for more control
        root.setPadding(new Insets(10));

        VBox leftContainer = new VBox(10, controlPanel, metroMap);
        leftContainer.setPadding(new Insets(10));
        HBox.setHgrow(leftContainer, Priority.ALWAYS); // Allow left container to grow

        VBox rightContainer = new VBox(10);
        rightContainer.setPadding(new Insets(10));
        rightContainer.getChildren().add(new Label("Доступные маршруты:"));
        rightContainer.getChildren().add(routesList);
        rightContainer.setPrefWidth(450);

        root.getChildren().addAll(leftContainer, rightContainer);

        // Настройка сцены
        Scene scene = new Scene(root, 1450, 900); // Увеличиваем ширину окна
        stage.setTitle("Поиск маршрутов Московского метро");
        stage.setScene(scene);
        stage.show();
    }

// Метод для форматирования маршрута
private String formatRoute(Route route, int index) {
    if (route == null || route.getStations().isEmpty()) {
        return "";
    }

    List<Station> stations = route.getStations();
    Station firstStation = stations.get(0);
    Station lastStation = stations.get(stations.size() - 1);

    StringBuilder routeText = new StringBuilder();

    // Add label based on index
    if (index == 0) {
        routeText.append("Самый быстрый маршрут по количеству станций\n");
    } else if (index == 1) {
        routeText.append("Самый быстрый маршрут по времени\n");
    } else if (index == 2) {
        routeText.append("Самый быстрый маршрут по количеству пересадок\n");
    }

    routeText.append(String.format("%s (%s) - ", 
        firstStation.getName(), 
        firstStation.getLine()));
    routeText.append(String.format("%s (%s)\n", 
        lastStation.getName(), 
        lastStation.getLine()));
    routeText.append(String.format("Время в пути - %d минут\n", route.getTotalTime()));

    // Add transfers information
    if (stations.size() > 2) {
        routeText.append("\nПересадки:\n");

        // Track the current line and station to detect transfers
        int currentLineId = stations.get(0).getLineId();
        Station previousStation = stations.get(0);

        for (int i = 1; i < stations.size(); i++) {
            Station station = stations.get(i);

            // Check if this is a transfer (line changed)
            if (station.getLineId() != currentLineId) {
                // Find the previous station on the previous line
                Station transferFromStation = previousStation;

                // Format the transfer information
                routeText.append(String.format("• %s (%s) → %s (%s)\n", 
                    transferFromStation.getName(), 
                    transferFromStation.getLine(),
                    station.getName(), 
                    station.getLine()));

                currentLineId = station.getLineId();
            }

            previousStation = station;
        }
    }

    return routeText.toString();
}

    private void findRoutes() {
        Station start = startStationCombo.getValue();
        Station end = endStationCombo.getValue();

        // If user typed a station name but didn't select from dropdown
        if (start == null) {
            start = findStationByName(startStationCombo.getEditor().getText());
        }

        if (end == null) {
            end = findStationByName(endStationCombo.getEditor().getText());
        }

        if (start == null || end == null) {
            showAlert("Ошибка", "Пожалуйста, выберите начальную и конечную станции");
            return;
        }

        if (start.equals(end)) {
            showAlert("Ошибка", "Начальная и конечная станции совпадают");
            return;
        }

        try {
            List<Route> routes = routeFinder.findRoutes(start.getId(), end.getId());
            if (routes.isEmpty()) {
                showAlert("Маршрут не найден", "Не найден маршрут между выбранными станциями");
                routesList.setItems(FXCollections.observableArrayList());
                metroMap.highlightRoute(null);
                routeSummaryLabel.setText("Маршрут не найден");
            } else {
                routesList.setItems(FXCollections.observableArrayList(routes));
                routesList.getSelectionModel().selectFirst();
                // The route summary will be updated by the selection listener
            }
        } catch (Exception e) {
            showAlert("Ошибка", "Не удалось найти маршрут: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlertAndExit(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        Platform.exit();
    }



    /**
     * Gets the travel time between two stations
     * @param station1 The first station
     * @param station2 The second station
     * @return The travel time in seconds, or 0 if not found
     */
    private int getTravelTimeBetweenStations(Station station1, Station station2) {
        // Get connections for the first station
        List<MetroConnection> connections = connectionMap.get(station1.getId());
        if (connections == null) {
            return 0;
        }

        // Find the connection to the second station
        for (MetroConnection conn : connections) {
            if (conn.getStation2Id() == station2.getId()) {
                return conn.getTravelTime();
            }
        }

        return 0;
    }


    /**
     * Finds a station by its name
     * @param name The name of the station to find
     * @return The station with the given name, or null if not found
     */
    private Station findStationByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        // Search for exact match first (case-insensitive)
        String searchName = name.trim().toLowerCase();
        for (Station station : stationMap.values()) {
            if (station.getName().toLowerCase().equals(searchName)) {
                return station;
            }
        }

        // If no exact match, try partial match
        for (Station station : stationMap.values()) {
            if (station.getName().toLowerCase().contains(searchName)) {
                return station;
            }
        }

        return null;
    }

    /**
     * Sets up search functionality for a station ComboBox
     * @param comboBox The ComboBox to set up search for
     * @param allStations The complete list of stations
     */
    private void setupStationSearch(ComboBox<Station> comboBox, ObservableList<Station> allStations) {
        // Изначально заполняем всеми станциями
        comboBox.setItems(allStations);

        // Получаем редактор из ComboBox
        TextField editor = comboBox.getEditor();

        // Устанавливаем конвертер для правильного преобразования String в Station
        comboBox.setConverter(new StringConverter<Station>() {
            @Override
            public String toString(Station station) {
                return station == null ? "" : station.getName();
            }

            @Override
            public Station fromString(String string) {
                return findStationByName(string);
            }
        });

        // Добавляем слушатель к свойству текста редактора
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            // Если текст пустой, показываем все станции
            if (newValue == null || newValue.isEmpty()) {
                comboBox.setItems(allStations);
                comboBox.hide();
                return;
            }

            // Фильтруем станции на основе введенного текста (без учета регистра)
            String searchText = newValue.toLowerCase();
            List<Station> filteredStations = allStations.stream()
                    .filter(station -> station.getName().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());

            // Обновляем элементы ComboBox отфильтрованным списком
            comboBox.setItems(FXCollections.observableArrayList(filteredStations));

            // Показываем выпадающий список с отфильтрованными результатами
            if (!filteredStations.isEmpty()) {
                comboBox.show();
            }
        });
    }

}
