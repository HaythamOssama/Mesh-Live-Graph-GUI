package mesh.live.graph.gui.chartstab;

import java.sql.SQLException;
import java.util.ArrayList;
import javafx.animation.AnimationTimer;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import mesh.live.graph.gui.LocalHost;

/**
 * Sets a XY chart in the chart tab that contains the throughput of every node <br>
 * Fetches data from the Metrics table using the local host
 */
public class LiveChart{
    // Maximum number of points on the graph. After MAX_DATA_POINTS, the graph is shifted to the left
    private static final int MAX_DATA_POINTS = 30;
    private int xSeriesData = 0;
    // XY Chart series to display the values
    private XYChart.Series<Number, Number > series1 = new XYChart.Series<>();
    // Queue holding the values to be displayed
    private ConcurrentLinkedQueue<Number> dataQ1 = new ConcurrentLinkedQueue<>();
    // Axes of the chart
    private NumberAxis xAxis,yAxis;
    // TODO
    private static ArrayList<String> chartsList = new ArrayList<>();
    private Task throughputListeningTask; // A Background task to fetch throughput data from the local host
    private Object synchObject; // Object to synchronize (Start/pause) the task
    private boolean synchFlag; // flag to control synchronization
    private LineChart<Number, Number> lineChart;
    /**
     * A Constructor that creates the live chart and starts the throughput listening task 
     * @param chartsGridPane Grid Pane located in the charts tab
     * @param id // TODO
     * @param localHost To run fetch queries without connecting again to the localhost
     * @throws ClassNotFoundException
     * @throws SQLException 
     */
    public LiveChart(GridPane chartsGridPane, String id, LocalHost localHost) throws ClassNotFoundException, SQLException{
        if(!isChartExist(id)){
            synchObject = new Object();
            // Create the throughput listening task
            throughputListeningTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    while(true){
                        synchronized(synchObject){
                            if(synchFlag){
                                System.err.println("[GUI] Throughput Task : Paused");
                                synchObject.wait();
                            }
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run(){
                                    System.err.println("[GUI] Throughput Task - Running");
                                    try {
                                        /**
                                         * Fetch the throughput values from the local host
                                         * Add the values to the queue
                                         * Call prepareTimeLine() to start plotting the new values
                                         */
                                        LocalHost.RowMetrics fetchedMetrics = localHost.FetchMetricsThroughput();
                                        if(fetchedMetrics.getCommand() != null){
                                            if(fetchedMetrics.getCommand().equals("Throughput")){
                                                addValueToGraph(String.valueOf(8*Double.valueOf(fetchedMetrics.getData())/100.0));                                     
                                            }
                                        }
                                    } catch (InterruptedException | SQLException ex) {
                                        Logger.getLogger(LiveChart.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    
                                }
                            });
                        }
                        Thread.sleep(500);
                    }
                }
            };
            // Start the throughput listeing task
            Thread thread = new Thread(throughputListeningTask);
            thread.setDaemon(true);
            thread.start();
            // ToDo
            chartsList.add(id);
            // Set the xAxis properties
            xAxis = new NumberAxis(0, MAX_DATA_POINTS, MAX_DATA_POINTS / 10);
            xAxis.setForceZeroInRange(false);
            xAxis.setAutoRanging(false);
            xAxis.setTickLabelsVisible(false);
            xAxis.setTickMarkVisible(false);
            xAxis.setMinorTickVisible(false);

            NumberAxis yAxis = new NumberAxis();

            // Create a LineChart
            lineChart = new LineChart<Number, Number>(xAxis, yAxis) {
                // Override to remove symbols on each data point
                @Override
                protected void dataItemAdded(XYChart.Series<Number, Number> series, int itemIndex, Data<Number, Number> item) {
                }
            };

            lineChart.setAnimated(false);
            lineChart.setTitle(id);
            lineChart.setHorizontalGridLinesVisible(true);

            // Set Name for Series
            series1.setName("Throughput");

            // Add Chart Series
            lineChart.getData().addAll(series1 );
            chartsGridPane.add(lineChart,0,0);
            
            
        }        
    }
    
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
    }

    
    private boolean isChartExist(String id){
        for(String chart : chartsList){
            if(chart.equals(id)){
                return true;
            }
        }
        return false;
    }
}
