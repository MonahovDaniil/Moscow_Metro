import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MetroMap extends Pane {
    private static final double STATION_RADIUS = 5;
    private static final double HIGHLIGHTED_STATION_RADIUS = 8;
    private static final double LINE_WIDTH = 2;
    private static final double HIGHLIGHTED_LINE_WIDTH = 4;
    private static final double SCALE_FACTOR = 0.7; // Scale factor to make the map smaller

    private final Canvas canvas;
    private final Map<Integer, Station> stationMap;
    private final Map<Integer, List<MetroConnection>> connectionMap;
    private List<Station> highlightedRoute;
    private final Map<Integer, Color> lineColors;
    private Tooltip tooltip;
    private Consumer<Station> onStationSelected;

    public MetroMap(Map<Integer, Station> stationMap,
                    Map<Integer, List<MetroConnection>> connectionMap) {
        this.stationMap = stationMap;
        this.connectionMap = connectionMap;

        // Calculate canvas size based on station coordinates
        double maxX = 0;
        double maxY = 0;
        for (Station station : stationMap.values()) {
            maxX = Math.max(maxX, station.getX());
            maxY = Math.max(maxY, station.getY());
        }

        // Add padding to ensure stations at the edges are fully visible
        maxX += 50;
        maxY += 50;

        // Apply scaling factor to make the map smaller
        maxX *= SCALE_FACTOR;
        maxY *= SCALE_FACTOR;

        this.canvas = new Canvas(maxX, maxY);
        getChildren().add(canvas);

        // Инициализация цветов линий
        this.lineColors = Map.of(
                1, Color.BROWN,    // Кольцевая
                2, Color.RED,      // Сокольническая
                3, Color.GREEN,    // Замоскворецкая
                4, Color.BLUE,     // Арбатско-Покровская
                5, Color.ORANGE,   // Калужско-Рижская
                6, Color.LIGHTBLUE // Филёвская
        );

        // Инициализация tooltip для отображения названий станций при наведении
        this.tooltip = new Tooltip();
        tooltip.setAutoHide(true);

        // Добавление обработчиков событий мыши
        canvas.setOnMouseMoved(this::handleMouseMoved);
        canvas.setOnMouseExited(event -> tooltip.hide());
        canvas.setOnMouseClicked(this::handleMouseClicked);

        drawMap();
    }

    private void handleMouseMoved(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();

        // Поиск станции под курсором
        Station station = findStationAt(mouseX, mouseY);

        if (station != null) {
            // Показываем tooltip с названием станции
            tooltip.setText(station.getName());
            tooltip.show(canvas, event.getScreenX(), event.getScreenY() + 10);
        } else {
            // Скрываем tooltip, если курсор не над станцией
            tooltip.hide();
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        double mouseX = event.getX();
        double mouseY = event.getY();

        // Поиск станции под курсором
        Station station = findStationAt(mouseX, mouseY);

        if (station != null && onStationSelected != null) {
            // Вызываем callback с выбранной станцией
            onStationSelected.accept(station);
        }
    }

    /**
     * Sets a callback to be called when a station is selected on the map
     * @param callback The callback to call when a station is selected
     */
    public void setOnStationSelected(Consumer<Station> callback) {
        this.onStationSelected = callback;
    }

    private Station findStationAt(double x, double y) {
        // Convert mouse coordinates back to original scale for comparison
        double originalX = x / SCALE_FACTOR;
        double originalY = y / SCALE_FACTOR;

        // Проверяем все станции
        for (Station station : stationMap.values()) {
            double distance = Math.sqrt(
                Math.pow(station.getX() - originalX, 2) + 
                Math.pow(station.getY() - originalY, 2)
            );

            // Если курсор находится в пределах радиуса станции (с небольшим запасом)
            double radius = station.isTransfer() || 
                           (highlightedRoute != null && highlightedRoute.contains(station)) ? 
                           HIGHLIGHTED_STATION_RADIUS : STATION_RADIUS;

            // Adjust the detection radius based on the scale factor
            radius = radius / SCALE_FACTOR;

            if (distance <= radius + 2) {
                return station;
            }
        }

        return null;
    }

    public void highlightRoute(List<Station> route) {
        this.highlightedRoute = route;
        drawMap();
    }

    private void drawMap() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Рисуем соединения сначала (чтобы линии были под станциями)
        drawConnections(gc);

        // Затем рисуем станции
        drawStations(gc);
    }

    private void drawConnections(GraphicsContext gc) {
        // Сначала рисуем все обычные соединения
        for (List<MetroConnection> connections : connectionMap.values()) {
            for (MetroConnection conn : connections) {
                Station s1 = stationMap.get(conn.getStation1Id());
                Station s2 = stationMap.get(conn.getStation2Id());

                if (s1 != null && s2 != null && !conn.isTransfer()) {
                    drawConnection(gc, s1, s2, false);
                }
            }
        }

        // Затем рисуем выделенные соединения (если есть маршрут)
        if (highlightedRoute != null && !highlightedRoute.isEmpty()) {
            for (int i = 0; i < highlightedRoute.size() - 1; i++) {
                Station current = highlightedRoute.get(i);
                Station next = highlightedRoute.get(i + 1);

                // Проверяем, есть ли прямое соединение между этими станциями
                if (connectionMap.containsKey(current.getId())) {
                    for (MetroConnection conn : connectionMap.get(current.getId())) {
                        if (conn.getStation2Id() == next.getId()) {
                            drawConnection(gc, current, next, true);
                            break;
                        }
                    }
                }
            }
        }

        // В конце рисуем пересадки (чтобы они были поверх всех линий)
        for (List<MetroConnection> connections : connectionMap.values()) {
            for (MetroConnection conn : connections) {
                Station s1 = stationMap.get(conn.getStation1Id());
                Station s2 = stationMap.get(conn.getStation2Id());

                if (s1 != null && s2 != null && conn.isTransfer()) {
                    drawConnection(gc, s1, s2, false);
                }
            }
        }
    }

    private void drawConnection(GraphicsContext gc, Station s1, Station s2, boolean isHighlighted) {
        // Get line colors for both stations
        Color line1Color = lineColors.getOrDefault(s1.getLineId(), Color.GRAY);
        Color line2Color = lineColors.getOrDefault(s2.getLineId(), Color.GRAY);

        // Check if this is a transfer connection (stations on different lines)
        boolean isDifferentLines = s1.getLineId() != s2.getLineId();

        if (isHighlighted) {
            // Highlighted connections are black
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(HIGHLIGHTED_LINE_WIDTH);
        } else if (isDifferentLines) {
            // For transfer connections between different lines, use a dashed line with the color of the first station
            gc.setStroke(line1Color);
            gc.setLineWidth(LINE_WIDTH);
            gc.setLineDashes(5); // Set dashed line pattern
        } else {
            // For regular connections on the same line, use the line color
            gc.setStroke(line1Color);
            gc.setLineWidth(LINE_WIDTH);
            gc.setLineDashes(null); // Reset to solid line
        }

        // Apply scaling factor to station coordinates
        double x1 = s1.getX() * SCALE_FACTOR;
        double y1 = s1.getY() * SCALE_FACTOR;
        double x2 = s2.getX() * SCALE_FACTOR;
        double y2 = s2.getY() * SCALE_FACTOR;

        gc.strokeLine(x1, y1, x2, y2);

        // Reset line dashes to solid for subsequent drawing
        gc.setLineDashes(null);
    }

    private void drawStations(GraphicsContext gc) {
        // Сначала рисуем все обычные станции
        for (Station station : stationMap.values()) {
            drawStation(gc, station, false);
        }

        // Затем рисуем выделенные станции (если есть маршрут)
        if (highlightedRoute != null) {
            for (Station station : highlightedRoute) {
                drawStation(gc, station, true);
            }
        }
    }

    private void drawStation(GraphicsContext gc, Station station, boolean isHighlighted) {
        Color lineColor = lineColors.getOrDefault(station.getLineId(), Color.BLACK);

        // Apply scaling factor to station coordinates
        double x = station.getX() * SCALE_FACTOR;
        double y = station.getY() * SCALE_FACTOR;

        // Рисуем кружок станции
        double radius = isHighlighted ? HIGHLIGHTED_STATION_RADIUS : STATION_RADIUS;
        gc.setFill(isHighlighted ? Color.BLACK : lineColor);
        gc.fillOval(x - radius, y - radius,
                radius * 2, radius * 2);

        // Добавляем обводку для лучшей видимости
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(0.5);
        gc.strokeOval(x - radius, y - radius,
                radius * 2, radius * 2);

        // Имена станций теперь отображаются только при наведении курсора
    }

    public void setSize(double width, double height) {
        canvas.setWidth(width);
        canvas.setHeight(height);
        drawMap();
    }
}
