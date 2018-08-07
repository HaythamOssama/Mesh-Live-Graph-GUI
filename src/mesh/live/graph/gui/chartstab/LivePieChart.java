package mesh.live.graph.gui.chartstab;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.PieChart.Data;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.GridPane;
import mesh.live.graph.gui.LocalHost;
import mesh.live.graph.gui.graphelements.Nodes;

/**
 * Sets a pue chart and a bar chart in the chart tab that contains packet analysis and traffic of every node <br>
 * Fetches data from the Metrics table using the local host
 */
public class LivePieChart{
    // List holding the Pie Chart values that are updated periodically
    private ObservableList<PieChart.Data> actualVsOverheadList;
    private ObservableList<PieChart.Data> actualVsRedundantList;
    private Task packetListeningTask;  // Background task to get packet data from the localhost
    private Object synchObject; // Object to synchronize (Start/pause) the task
    private boolean synchFlag; // flag to control synchronization
    static private boolean opened = true; // flag to avoid recreating the views
    static private PieChart actualVsOverheadPieChart; // To display the overhead and actual data
    static private PieChart actualVsRedundantPieChart; // To display the overhead and actual data
    static private BarChart<String,Number> barChart; // To display the traffic
    static private NumberAxis yAxis;
    static private CategoryAxis xAxis;
    static private XYChart.Series<String,Number> series1; // Stores the barchart data
    private double totalSize=0; // Total packet size that gets accumulated with each local host fetch
    private double actual=0; // Actual packet size that gets accumulated with each local host fetch
    private double overhead=0; // Overhead packet size that gets accumulated with each local host fetch
    private double redundant=0;
    /**
     * A constructor that starts fetching actual and overhead data from the local host <br>
     * Adds a pie chart to represent overhead vs actual fetched data <br>
     * Adds a bar chart to represent the traffic of each node <br>
     * @param chartsGridPane Grid pane located in the charts tab
     * @param localHost local host instance to prevent reconnecting to the database
     * @param nodesList This list represents the xaxis of the bar chart
     */
    public LivePieChart(GridPane chartsGridPane, LocalHost localHost, ArrayList<Nodes> nodesList){
        if(opened){
            actualVsOverheadPieChart = new PieChart();
            actualVsRedundantPieChart = new PieChart();
            xAxis = new CategoryAxis();
            yAxis = new NumberAxis();
            barChart = new BarChart(xAxis, yAxis);
            series1 = new XYChart.Series();
            series1.setName("Traffic in Bytes");
            barChart.setTitle("Traffic Analysis");
            xAxis.setLabel("Nodes");       
            yAxis.setLabel("Traffic");
            // Add the nodes addresses to the xaxis of the bar chart
            for(Nodes node : nodesList){
                series1.getData().add(new XYChart.Data(node.getAddress(), 0));
            }
            barChart.getData().add(series1);
            actualVsOverheadList = FXCollections.observableArrayList();
            actualVsRedundantList = FXCollections.observableArrayList();
            synchObject = new Object();
            packetListeningTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    while(true){
                        synchronized(synchObject){
                            if(synchFlag){
                                System.err.println("[GUI] Packet Data Task : Paused");
                                synchObject.wait();
                            }
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run(){
                                    try {
                                        /**
                                         * Fetch the actual and overhead data
                                         * For the pie chart:
                                         *      - Update the sectors of the chart
                                         *      - Accumulate the totalSize, actual and overhead variables
                                         *      - Set the title to the percentages of the actual and overheads
                                         */
                                        System.err.println("[GUI] Packet Data Task - Running");
                                        LocalHost.RowMetrics fetchedMetrics = localHost.FetchMetricsPieChart();
                                        if(fetchedMetrics.getData() != null){
                                            boolean overheadFound = false;
                                            boolean dataFound = false;
                                            boolean redundantFound = false;
                                            /*
                                             * If the overhead or actual data sectors are already drawn in the actual vs overhead piechart,
                                             * increase the values only
                                             */
                                            for(PieChart.Data row :actualVsOverheadList){
                                                if(row.getName().equals("PktOverhead") && fetchedMetrics.getCommand().equals("PktOverhead")){
                                                   row.setPieValue(row.getPieValue() +Double.valueOf(fetchedMetrics.getData()));
                                                   overheadFound = true;
                                                }
                                                else if((row.getName().equals("PktActual")&& fetchedMetrics.getCommand().equals("PktActual")) ||
                                                        (row.getName().equals("PktRed")&& fetchedMetrics.getCommand().equals("PktRed"))){
                                                   row.setPieValue(row.getPieValue() + Double.valueOf(fetchedMetrics.getData()));
                                                   dataFound = true;
                                                }
                                            }
                                            //If the overhead or actual data sectors aren't drawn, draw the sectors for the first time only
                                            if(!overheadFound && fetchedMetrics.getCommand().equals("PktOverhead") ||
                                                    (!dataFound && fetchedMetrics.getCommand().equals("PktActual"))){
                                                actualVsOverheadList.add(new Data(fetchedMetrics.getCommand(), Double.valueOf(fetchedMetrics.getData())));
                                                actualVsOverheadPieChart.setData(actualVsOverheadList);
                                            }
                                            // Set the traffic in the bar chart
                                            for (XYChart.Data<String, Number> data : series1.getData()) {
                                                if(data.getXValue().equals(fetchedMetrics.getAddress())){
                                                    data.setYValue(data.getYValue().doubleValue()+Double.valueOf(fetchedMetrics.getData()));
                                                }
                                            }
                                            // Calculate the new statistics
                                            if(fetchedMetrics.getCommand().equals("PktOverhead")){
                                                overhead+=Double.valueOf(fetchedMetrics.getData());
                                                totalSize += Double.valueOf(fetchedMetrics.getData());
                                            }
                                            else if(fetchedMetrics.getCommand().equals("PktActual")){
                                                actual+=Double.valueOf(fetchedMetrics.getData());
                                                totalSize += Double.valueOf(fetchedMetrics.getData());
                                            }
                                            else if(fetchedMetrics.getCommand().equals("PktRed")){
                                                redundant+=Double.valueOf(fetchedMetrics.getData());
                                            }
                                            actualVsOverheadPieChart.setTitle("Actual Data = " + actual + " Bytes || "+ Math.ceil(100*actual/totalSize) +" %" 
                                                    + "\n Overhead = " + overhead +" Bytes || " + Math.floor(100*overhead/totalSize )+ " %") ;
                                            
                                            dataFound=false;
                                            /*
                                             * If the overhead or actual data sectors are already drawn in the actual vs redundant piechart,
                                             * increase the values only
                                             */
                                            for(PieChart.Data row :actualVsRedundantList){
                                                if(row.getName().equals("PktActual") && fetchedMetrics.getCommand().equals("PktActual")){
                                                   row.setPieValue(row.getPieValue() +Double.valueOf(fetchedMetrics.getData()));
                                                   dataFound = true;
                                                }
                                                else if(row.getName().equals("PktRed")&& fetchedMetrics.getCommand().equals("PktRed")){
                                                   row.setPieValue(row.getPieValue() + Double.valueOf(fetchedMetrics.getData()));
                                                   redundantFound = true;
                                                }
                                            }
                                            
                                            //If the overhead or actual data sectors aren't drawn, draw the sectors for the first time only
                                            if(!redundantFound && fetchedMetrics.getCommand().equals("PktRed") ||
                                                    (!dataFound && fetchedMetrics.getCommand().equals("PktActual"))){
                                                actualVsRedundantList.add(new Data(fetchedMetrics.getCommand(), Double.valueOf(fetchedMetrics.getData())));
                                                actualVsRedundantPieChart.setData(actualVsRedundantList);
                                            }
                                            actualVsRedundantPieChart.setTitle("Actual Data = " + actual + " Bytes || "+ Math.ceil(100*actual/(actual+redundant)) +" %" 
                                                    + "\n Redundant Data = " + redundant +" Bytes || " + Math.floor(100*redundant/(actual+redundant) )+ " %") ;
                                        }
                                    } catch (SQLException | InterruptedException ex) {
                                        Logger.getLogger(LivePieChart.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            });
                        }
                        Thread.sleep(500);
                    }
                }
            };
            chartsGridPane.add(actualVsOverheadPieChart,1,0);
            chartsGridPane.add(actualVsRedundantPieChart,1,1);
            chartsGridPane.add(barChart, 0, 1);
            // Start the packet listening task
            Thread thread = new Thread(packetListeningTask);
            thread.setDaemon(true);
            thread.start();
        }
    }
}
