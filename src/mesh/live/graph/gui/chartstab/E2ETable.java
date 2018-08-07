package mesh.live.graph.gui.chartstab;
/**
 * @author haytham
 */
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import mesh.live.graph.gui.LocalHost;
import mesh.live.graph.gui.graphelements.Nodes;
import mesh.live.graph.gui.SensorTable;

/**
 * Sets a table in the chart tab that contains the E2E delay of every node <br>
 * Fetches data from the Metrics table using the local host <br>
 */
public class E2ETable {
    /*/**********************************
     * Specify the columns of the table
     * ---------+-----------+
     *  Address + E2E Delay +
     * ---------+-----------+
     ************************************/
    private TableColumn<LocalHost.SensorData, String> addressCol   = new TableColumn<>("Address");
    private TableColumn<LocalHost.SensorData, String> timestampCol = new TableColumn<>("E2E Delay");
    private Task e2eListeningTask; // Background task to get StartE2E and EndE2E from the localhost
    private Object synchObject; // Object to synchronize (Start/pause) the task
    private boolean synchFlag; // flag to control synchronization
    static private TableView table; // The tableview that holds the address and timestamp columns
    
    // Live Chart
//    // Maximum number of points on the graph. After MAX_DATA_POINTS, the graph is shifted to the left
//    private static final int MAX_DATA_POINTS = 30;
//    private int xSeriesData = 0;
//    // XY Chart series to display the values
//    private XYChart.Series<Number, Number > series1 = new XYChart.Series<>();
//    // Queue holding the values to be displayed
//    private ConcurrentLinkedQueue<Long> dataQ1 = new ConcurrentLinkedQueue<>();
    // Axes of the chart
    //private NumberAxis xAxis,yAxis;
    //final static private long PACKET_SIZE = 10;
    //final LineChart<Number, Number> lineChart;
    /**
     * Constructor to start the E2E Task and fetch data from the local host 
     * @param localHost To run fetch queries without connecting again to the localhost
     * @param chartsGridPane Grid Pane located in the charts tab
     * @param width width of the screen to resize the columns
     * @param nodesList Nodes in the network
     * @throws SQLException
     * @throws InterruptedException 
     */
    public E2ETable(LocalHost localHost, GridPane chartsGridPane, double width, ArrayList<Nodes> nodesList) throws SQLException, InterruptedException {
        table = new TableView();
        // Divide half the right screen into two columns
        addressCol.setPrefWidth(width / 4);
        timestampCol.setPrefWidth(width / 4);
        // Increase the font size by CSS
        table.setStyle("-fx-font-size:20px;");
        // Grab the variable names of Row Metrics Class
        addressCol.setCellValueFactory  (new PropertyValueFactory<>("address"));
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        // Add the columns to the table
        table.getColumns().addAll(addressCol, timestampCol);
        synchObject = new Object();
        
        // E2E listening task 
        e2eListeningTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while(true){
                    synchronized(synchObject){
                        if(synchFlag){
                            System.err.println("[GUI] E2E Task : Paused");
                            synchObject.wait();
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run(){
                                try {
                                    System.err.println("[GUI] E2E Task - Running");
                                    /**
                                     * Loop over the nodes. Get each node and search the database for
                                     * [GUI] Address-StartE2E
                                     * [Gui] Address-EndE2E
                                     * Then get the difference between the two timestamps
                                     */
                                    for(Nodes node : nodesList){
                                        ArrayList<LocalHost.RowMetrics> row = localHost.FetchMetricsE2E(node.getAddress());
                                        if(row!= null){
                                            long e2e = Math.abs(timeToMilliSeconds(row.get(0).getTimestamp()) - 
                                                timeToMilliSeconds(row.get(1).getTimestamp()));
                                            row.get(0).setTimestamp(String.valueOf(e2e));
                                            table.getItems().add(row.get(0));
                                            //addValueToGraph(String.valueOf(1000*PACKET_SIZE/e2e));                                     
                                        }
                                    }
                                } catch (SQLException | InterruptedException ex) {
                                    Logger.getLogger(SensorTable.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });
                    }
                    Thread.sleep(500);
                }
            }
        };
        // Start the task 
        Thread thread = new Thread(e2eListeningTask);
        thread.setDaemon(true);
        thread.start();
        chartsGridPane.add(table,1,1);
        
//        xAxis = new NumberAxis(0, MAX_DATA_POINTS, MAX_DATA_POINTS / 10);
//        xAxis.setForceZeroInRange(false);
//        xAxis.setAutoRanging(false);
//        xAxis.setTickLabelsVisible(false);
//        xAxis.setTickMarkVisible(false);
//        xAxis.setMinorTickVisible(false);
//
//        NumberAxis yAxis = new NumberAxis();
//
//        // Create a LineChart
//        lineChart = new LineChart<Number, Number>(xAxis, yAxis) {
//            // Override to remove symbols on each data point
//            @Override
//            protected void dataItemAdded(XYChart.Series<Number, Number> series, int itemIndex, XYChart.Data<Number, Number> item) {
//            }
//        };
//
//        lineChart.setAnimated(false);
//        lineChart.setHorizontalGridLinesVisible(true);
//
//        // Set Name for Series
//        series1.setName("Throughput");
//        // Add Chart Series
//        lineChart.getData().addAll(series1 );
//        //chartsGridPane.add(lineChart,0,0);
    }
    /**
     * Converts a timestamp in this format : hhmmss.millis into milli seconds
     * @param time time in the illustrated format
     * @return time in milliseconds
     */
    private long timeToMilliSeconds(String time){
        long hours = Integer.valueOf(String.valueOf(time.charAt(0)) + String.valueOf(time.charAt(1)));
        long minutes = Integer.valueOf(String.valueOf(time.charAt(2)) + String.valueOf(time.charAt(3)));
        long seconds = Integer.valueOf(String.valueOf(time.charAt(4)) + String.valueOf(time.charAt(5)));
        StringBuffer sb = new StringBuffer();
        for(int i=6 ; i < time.length() ; i++){
            sb.append(time.charAt(i));
        }
        long millis = Long.valueOf(sb.toString());
        return hours*60*60*1000+minutes*60*1000+seconds*1000+millis/1000;
    }
    /*
    private void addValueToGraph(String value){
        if(value != null){
            XYChart.Data <Number,Number> data = new XYChart.Data<>(xSeriesData++, Double.valueOf(value));
            data.setNode(new Circle(5,Color.RED));
            series1.getData().add(data);
            // remove points to keep us at no more than MAX_DATA_POINTS
            if (series1.getData().size() > MAX_DATA_POINTS) {
                series1.getData().remove(0, series1.getData().size() - MAX_DATA_POINTS);
            }
            // update
            xAxis.setLowerBound(xSeriesData - MAX_DATA_POINTS);
            xAxis.setUpperBound(xSeriesData - 1);
            lineChart.getData().clear();
            lineChart.getData().add(series1);
            series1.getNode().setStyle("-fx-stroke:red;-fx-stroke-width:3");
        }
    }*/
}


class HoveredThresholdNodea extends StackPane {

    public HoveredThresholdNodea(String string, Object object) {
        System.out.println("\t\t 1. Entered Constructor");
        setPrefSize(15, 15);

        final Label label = createDataThresholdLabel(string, object);

        setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("\t\t 3.1 Mouse Enter");
                getChildren().setAll(label);
                setCursor(Cursor.NONE);
                toFront();
            }
        });
        setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                System.out.println("\t\t 3.2 Mouse Leave");
                getChildren().clear();
            }
        });
    }

    private Label createDataThresholdLabel(String string, Object object) {
        final Label label = new Label(object + "");
        label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
        label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");

        System.out.println(string);
        if (string.equals("Throughput")) {
            System.err.println("\t\t 2. Entered createDataThreasholdLabel");
            label.setTextFill(Color.RED);
            label.setStyle("-fx-border-color: RED;");
        }
        label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
        return label;
    }
}