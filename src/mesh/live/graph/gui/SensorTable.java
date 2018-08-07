package mesh.live.graph.gui;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Sets a table in the sensors tab that contains the values of temperature and pressure <br>
 * Fetches data from the Sensors table using the local host
 */
public class SensorTable {
    // COlumns of the Sensor table
    private TableColumn<LocalHost.SensorData, String> addressCol   = new TableColumn<>("Address");
    private TableColumn<LocalHost.SensorData, String> typeCol      = new TableColumn<>("Sensor Type");
    private TableColumn<LocalHost.SensorData, String> valueCol     = new TableColumn<>("Value");
    private TableColumn<LocalHost.SensorData, String> timestampCol = new TableColumn<>("Timestamp");
    private Task sensorListeningTask; // Background task to fetch sensor data from the local host
    private Object synchObject; // Object to synchronize (Start/pause) the task
    private boolean synchFlag; // flag to control synchronization
    
    /**
     * Constructor that starts a task to fetch sensor data from the local hos
     * @param localHost to avoid reconnecting the local host
     * @param table Table in sensor tab
     * @param width Used to set the width of the columns
     * @throws SQLException
     * @throws InterruptedException 
     */
    public SensorTable(LocalHost localHost, TableView table, double width) throws SQLException, InterruptedException {
        // Divide the width equally between the columns
        addressCol.setPrefWidth(width / 4);
        typeCol.setPrefWidth(width / 4);
        valueCol.setPrefWidth(width / 4);
        timestampCol.setPrefWidth(width / 4);
        // Increase the font size of the table
        table.setStyle("-fx-font-size:20px;");
        // Grab the variable names of Sensor data
        addressCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        table.getColumns().addAll(addressCol, typeCol, valueCol, timestampCol);
        
        synchObject = new Object();
        sensorListeningTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while(true){
                    synchronized(synchObject){
                        if(synchFlag){
                            System.err.println("[GUI] Sensor Task : Paused");
                            synchObject.wait();
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run(){
                                try {
                                    // Fetch one row and add it to the table
                                    System.err.println("[GUI] Sensor Task - Running");
                                    LocalHost.SensorData row = localHost.FetchSensorData();
                                    System.err.println("Row Fetched : " + row.getAddress() + " - " + " - " + row.getType() + " - " 
                                            + row.getValue());
                                    if(row.getAddress() != null){
                                        table.getItems().add(row);
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
        // Start the sensor listening task 
        Thread thread = new Thread(sensorListeningTask);
        thread.setDaemon(true);
        thread.start();
    }
}
