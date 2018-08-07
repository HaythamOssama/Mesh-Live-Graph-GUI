/*
 * @author haytham
 * 
 * Handles all MYSQL data transactions from and to the local host
 * 
 * ******************************************************************
 * ******************        TABLE STRUCTURE       ******************
 * ******************************************************************
 *                  TABLE 1 : For Graph Drawing                     *
 *  + --------------------+---------+----------------+-----------+  *
 *  + Destination Address + Message + Source Address + Timestamp +  *
 *  + --------------------+---------+----------------+-----------+  *
 *                      TABLE 2 : For Metrics                       *
 *            + --------+---------+------+-----------+              *              
 *            + Address + Command + Data + Timestamp +              *
 *            + --------+---------+------+-----------+              *
 *                      TABLE 3 : For Sensors                       *
 *             + --------+------+-------+-----------+               *
 *             + Address + Type + Value + Timestamp +               *
 *             + --------+------+-------+-----------+               *
 * ******************************************************************
 */
package mesh.live.graph.gui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Handles all local sever transactions (Inserting/Fetching/Deleting Records)
 */
public class LocalHost {
    /**
     * MySQL Credentials to connect to the local host <br>
     * Remember to change the IP address here to the IP of the IP having the local host
     */
    final static private String     DB_USERNAME       = "username";
    final static private String     DB_PASSWORD       = "password";
    final static private String     LOCALHOST_IP      = "192.168.43.110";
    final static private String     DB_NAME           = "MeshGraph";
    
    final static private String     TABLE_NAME_1      = "Graph";
    final static private String     COL_SRC_ADDRESS   = "SourceAddress";
    final static private String     COL_CONTROL_MSG   = "Message";
    final static private String     COL_DST_ADDRESS   = "DestinationAddress";
    final static private String     COL_TIMESTAMP     = "Timestamp";
    
    final static private String     TABLE_NAME_2      = "Metrics";
    final static private String     COL_COMMAND       = "Command";
    final static private String     COL_DATA          = "Data";
    
    final static private String     TABLE_NAME_3      = "Sensors";
    final static private String     COL_ADDRESS       = "Address";
    final static private String     COL_TYPE          = "Type";
    final static private String     COL_VALUE         = "Value";
    
    
    static private Connection connection; // To establish connection with MYSQL, throws SQLException
    static public  LinkedHashSet <RowMessage> fetchedMessages; // Stores data from table 1
    static public  LinkedHashSet <RowMetrics> fetchedMetrics; // Stores data from table 2
    static public  LinkedHashSet <SensorData> fetchedSensorData; // Stores data from table 3
    
    public LocalHost() throws ClassNotFoundException, SQLException{
        // Try to establish a connection with the Local host. Remember to start Apache and MYSQL services on XAMPP
        Class.forName("com.mysql.jdbc.Driver");
        // Request connection
        connection = (Connection) DriverManager.getConnection("jdbc:mysql://"+LOCALHOST_IP+":3306/"+DB_NAME, DB_USERNAME, DB_PASSWORD);
        System.out.println(connection);
        // Initialize LinkedHashSet
        fetchedMessages = new LinkedHashSet<>();
        fetchedMetrics = new LinkedHashSet<>();
        fetchedSensorData = new LinkedHashSet<>();
    }
    
    /**
     * Insert data to the Graph table
     * @param destinationAddress Node receiving the control message
     * @param message RREQ/RREP/RERR/Data
     * @param sourceAddress Node originating the message
     * @param timestamp Times at which the control message is received
     * @return true if the query is executed correctly, false otherwise
     * @throws SQLException 
     */
    public boolean InsertMessage(String destinationAddress, String message, String sourceAddress, String timestamp) throws SQLException{
        System.err.println("[UploadToLocalHost] : Insert Data() : Data = " + destinationAddress + " - " + message + " - " + sourceAddress
                    + " - " + timestamp);
        Statement stmt=(Statement)connection.createStatement();
        return stmt.executeUpdate("INSERT INTO "+TABLE_NAME_1+" VALUES('"+destinationAddress+"','"+message+
                "','"+sourceAddress+"','"+timestamp +"');") > 0;
    }
    /**
     * Insert data to the Metrics table
     * @param command StartE2E/EndE2E/Throughput/PktOverhead/PktActual
     * @param data values of the throughput/overhead/actual 
     * @param timestamp Times at which the command is sent
     * @return true if the query is executed correctly, false otherwise
     * @throws SQLException 
     */
    public boolean InsertMetric(String command, String data, String timestamp) throws SQLException{
        System.err.println("[UploadToLocalHost] : Insert Data() : Data = " + timestamp + " - " + command + " - " + data);
        Statement stmt=(Statement)connection.createStatement();
        return stmt.executeUpdate("INSERT INTO "+TABLE_NAME_2+" VALUES('"+command+"','"+data+"','"+timestamp+"');") > 0;
    }
    /**
     * Insert data to the Sensors table
     * @param address Node sending the sensor data
     * @param type Pressure/Temperature
     * @param value Value of the pressure or temperature
     * @param timestamp Time at which the value is recorded
     * @return true if the query is executed correctly, false otherwise
     * @throws SQLException 
     */
    public boolean InsertSensorData(String address, String type, String value, String timestamp) throws SQLException{
        System.err.println("[UploadToLocalHost] : Insert Data() : Data = " + address + " - " + type + " - " + value + " - " + timestamp);
        Statement stmt=(Statement)connection.createStatement();
        return stmt.executeUpdate("INSERT INTO "+TABLE_NAME_3+" VALUES('"+address+"','"+type+"','"+value+"','"+timestamp+"');") > 0;
    }
    /**
     * Fetch the time of the local server now
     * @return the time of the local host in this format : 25-01-2018 15:34:10.123456
     * @throws SQLException 
     */
    public String FetchTimeNow() throws SQLException{
        Statement stmt=(Statement)connection.createStatement();
        ResultSet result = stmt.executeQuery("SELECT NOW(6)");   
        return result.toString();
    }
    /**
     * Fetch the graph control messages from Graph table
     * @return a linked hashset of all the control messages
     * @throws SQLException 
     */
    public RowMessage FetchMessage() throws SQLException, InterruptedException{
        Statement stmt=(Statement)connection.createStatement();
        ResultSet result = stmt.executeQuery("SELECT "+COL_DST_ADDRESS + " , " + COL_CONTROL_MSG  + " , " + COL_SRC_ADDRESS + " , " + COL_TIMESTAMP +
                " FROM " + TABLE_NAME_1 + " LIMIT 1");
        RowMessage message = new RowMessage();
        while(result.next()){
            message.setDestinationAddress(result.getString(COL_DST_ADDRESS));
            message.setControlMessage    (result.getString(COL_CONTROL_MSG));
            message.setSourceAddress     (result.getString(COL_SRC_ADDRESS));
            message.setTimestamp         (result.getString(COL_TIMESTAMP));
            fetchedMessages.add(message);
        }
        DeleteMessage(message.getTimestamp());
        return message;
    }
    /**
     * Fetches the throughput data from Metrics Table and deletes the record
     * @return One row containing one throughput value
     * @throws SQLException
     * @throws InterruptedException 
     */
    public RowMetrics FetchMetricsThroughput() throws SQLException, InterruptedException{
        Statement stmt=(Statement)connection.createStatement();
        ResultSet result = stmt.executeQuery("SELECT "+COL_COMMAND + " , " + COL_DATA  + " , " + COL_TIMESTAMP +
                " FROM " + TABLE_NAME_2 + " WHERE " + COL_COMMAND +" = 'Throughput' LIMIT 1");
        RowMetrics row = new RowMetrics();
        while(result.next()){
            row.setCommand(result.getString(COL_COMMAND));
            row.setData(result.getString(COL_DATA));
            row.setTimestamp(result.getString(COL_TIMESTAMP));
            fetchedMetrics.add(row);
        }
        DeleteCommandsAndMetrics(row.getTimestamp());
        return row;
    }
    /**
     * Fetch packet data from Metrics table and deletes the record
     * @return One row containing a value either a packet overhead or actual data
     * @throws SQLException
     * @throws InterruptedException 
     */
     public RowMetrics FetchMetricsPieChart() throws SQLException, InterruptedException{
        Statement stmt=(Statement)connection.createStatement();
        ResultSet result = stmt.executeQuery("SELECT "+COL_ADDRESS + " , " + COL_COMMAND + " , " + COL_DATA  + " , " + COL_TIMESTAMP +
                " FROM " + TABLE_NAME_2 + " WHERE " + COL_COMMAND +" = 'PktOverhead' OR " + COL_COMMAND +" = 'PktActual'"
                        + "OR " + COL_COMMAND + " = 'PktRed' LIMIT 1");
        RowMetrics row = new RowMetrics();
        while(result.next()){
            row.setAddress(result.getString(COL_ADDRESS));
            row.setCommand(result.getString(COL_COMMAND));
            row.setData(result.getString(COL_DATA));
            row.setTimestamp(result.getString(COL_TIMESTAMP));
            fetchedMetrics.add(row);
        }
        DeleteCommandsAndMetrics(row.getTimestamp());
        return row;
    }
     /**
      * TODO
      * @param address
      * @return
      * @throws SQLException
      * @throws InterruptedException 
      */
     public ArrayList<RowMetrics> FetchMetricsE2E(String address) throws SQLException, InterruptedException{
        ArrayList<RowMetrics> arrayList = new ArrayList<>();
        RowMetrics row1 = new RowMetrics();
        RowMetrics row2 = new RowMetrics();
        Statement stmt=(Statement)connection.createStatement();
        ResultSet result1 = stmt.executeQuery("SELECT "+ COL_ADDRESS + " , " + COL_COMMAND + " , " + COL_DATA  + " , " + COL_TIMESTAMP +
                " FROM " + TABLE_NAME_2 + " WHERE " + COL_ADDRESS +" = '" + address +"' AND " + COL_COMMAND + " = 'StartE2E' LIMIT 1");
        while(result1.next()){
            row1.setAddress  (result1.getString(COL_ADDRESS));
            row1.setCommand  (result1.getString(COL_COMMAND));
            row1.setData     (result1.getString(COL_DATA));
            row1.setTimestamp(result1.getString(COL_TIMESTAMP));
        }
        ResultSet result2 = stmt.executeQuery("SELECT "+ COL_ADDRESS + " , " + COL_COMMAND + " , " + COL_DATA  + " , " + COL_TIMESTAMP +
                " FROM " + TABLE_NAME_2 + " WHERE " + COL_ADDRESS +" = '" + address +"' AND " + COL_COMMAND + " = 'EndE2E' LIMIT 1");
        while(result2.next()){
            row2.setAddress  (result2.getString(COL_ADDRESS));
            row2.setCommand  (result2.getString(COL_COMMAND));
            row2.setData     (result2.getString(COL_DATA));
            row2.setTimestamp(result2.getString(COL_TIMESTAMP));
        }
        if(row1.getAddress() == null || row2.getAddress() == null){
            return null;
        }
        else{
            arrayList.add(row1);
            arrayList.add(row2);
            DeleteCommandsAndMetrics(row1.getTimestamp());
            DeleteCommandsAndMetrics(row2.getTimestamp());
            return arrayList;
        }
     }
     /**
      * Fetch sensor data from Sensor table and deletes the record
      * @return One row containing a value from temperature or pressure
      * @throws SQLException
      * @throws InterruptedException 
      */
    public SensorData FetchSensorData() throws SQLException, InterruptedException{
                Statement stmt=(Statement)connection.createStatement();
        ResultSet result = stmt.executeQuery("SELECT "+COL_ADDRESS + " , " + COL_TYPE  + " , " + COL_VALUE+ " , " + COL_TIMESTAMP +
                " FROM " + TABLE_NAME_3 + " LIMIT 1");
        SensorData row = new SensorData();
        while(result.next()){
            row.setAddress(result.getString(COL_ADDRESS));
            row.setType(result.getString(COL_TYPE));
            row.setValue(result.getString(COL_VALUE));
            row.setTimestamp(result.getString(COL_TIMESTAMP));
            fetchedSensorData.add(row);
        }
        DeleteSensorData(row.getTimestamp());
        return row;

    }
    /**
     * Deletes a record from Graph table depending on the timestamp
     * @param timestamp
     * @return True on success, false otherwise
     * @throws SQLException
     * @throws InterruptedException 
     */
    public boolean DeleteMessage(String timestamp) throws SQLException, InterruptedException{
        Iterator<RowMessage>  it=fetchedMessages.iterator();
            while(it.hasNext()) {
            RowMessage value=it.next();
            if(timestamp.equals(value.getTimestamp())){
                it.remove();
            }
        }
        Statement stmt=(Statement)connection.createStatement();
        if(timestamp != null){
            System.err.println("[GUI] DeleteRow() : Data deleted = " + timestamp);
        }
        return stmt.executeUpdate("DELETE FROM "+ TABLE_NAME_1 +" WHERE "+COL_TIMESTAMP+"="+timestamp)> 0;
    }
    /**
     * Deletes a record from Metrics table depending on the timestamp
     * @param timestamp
     * @return True on success, false otherwise
     * @throws SQLException
     * @throws InterruptedException 
     */
    public boolean DeleteCommandsAndMetrics(String timestamp) throws SQLException, InterruptedException{
        Iterator<RowMetrics>  it=fetchedMetrics.iterator();
            while(it.hasNext()) {
            RowMetrics value=it.next();
            if(timestamp.equals(value.getTimestamp())){
                it.remove();
            }
        }
        Statement stmt=(Statement)connection.createStatement();
        if(timestamp != null){
            System.err.println("[GUI] DeleteRow() : Data deleted = " + timestamp);
        }
        return stmt.executeUpdate("DELETE FROM "+ TABLE_NAME_2 +" WHERE "+COL_TIMESTAMP+"="+timestamp)> 0;
    }
    /**
     * Deletes a record from Sensors table depending on the timestamp
     * @param timestamp
     * @return True on success, false otherwise
     * @throws SQLException
     * @throws InterruptedException 
     */
    public boolean DeleteSensorData(String timestamp) throws SQLException, InterruptedException{
        Iterator<SensorData>  it=fetchedSensorData.iterator();
            while(it.hasNext()) {
            SensorData value=it.next();
            if(timestamp.equals(value.getTimestamp())){
                it.remove();
            }
        }
        Statement stmt=(Statement)connection.createStatement();
        if(timestamp != null){
            System.err.println("[GUI] DeleteRow() : Data deleted = " + timestamp);
        }
        return stmt.executeUpdate("DELETE FROM "+ TABLE_NAME_3 +" WHERE "+COL_TIMESTAMP+"="+timestamp)> 0;
    }
    /**
     * Flushes all the tables of the database
     * @return True on success, false otherwise
     * @throws SQLException
     */
    public boolean DeleteAllRecords() throws SQLException{
        System.err.println("[GUI] DeleteAllRecords() : Database flushed");
        Statement stmt=(Statement)connection.createStatement();
        stmt.executeUpdate("TRUNCATE "+TABLE_NAME_2);
        stmt.executeUpdate("TRUNCATE "+TABLE_NAME_3);
        return stmt.executeUpdate("TRUNCATE "+TABLE_NAME_1) >0;
    }
   
    
    // Instance of a row from Graph table
    public class RowMessage{
        private String sourceAddress;
        private String destinationAddress;
        private String controlMessage;
        private String timestamp;
        public RowMessage(String sourceAddress, String controlMessage, String destinationAddress, String timestamp) {
            this.sourceAddress = sourceAddress;
            this.controlMessage = controlMessage;
            this.timestamp = timestamp;
            this.destinationAddress = destinationAddress;
        }

        public RowMessage(String timestamp) {
            this.timestamp = timestamp;
        }

        public RowMessage() {
        }

        public void setControlMessage(String controlMessage) {
            this.controlMessage = controlMessage;
        }

        public String getControlMessage() {
            return controlMessage;
        }

        public void setDestinationAddress(String destinationAddress) {
            this.destinationAddress = destinationAddress;
        }

        public String getDestinationAddress() {
            return destinationAddress;
        }

        public void setSourceAddress(String sourceAddress) {
            this.sourceAddress = sourceAddress;
        }

        public String getSourceAddress() {
            return sourceAddress;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean equals(Object obj) {
            RowMessage node = (RowMessage) obj;
            if (this.getTimestamp().equals(node.getTimestamp()))
                return true;
            return false;
        }

    }
    // Instance of a row from Metrics table
    public class RowMetrics{
        private String command;
        private String data;
        private String timestamp;
        private String address;
        
        public RowMetrics(){
            
        }
        public RowMetrics(String address, String command, String data, String timestamp){
            this.command   = command;
            this.data      = data;
            this.timestamp = timestamp;
            this.address = address;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public void setData(String data) {
            this.data = data;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        
        public String getCommand() {
            return command;
        }

        public String getData() {
            return data;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getAddress() {
            return address;
        }
        
    }
    // Instance of a row from Sensors table
    public class SensorData{
        private String address;
        private String type;
        private String value;
        private String timestamp;

        public SensorData() {
        }
        
        
        SensorData(String address, String type, String value, String timestamp){
            this.address = address;
            this.type = type;
            this.value = value;
            this.timestamp = timestamp;
        }

        public String getAddress() {
            return address;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setValue(String value) {
            this.value = value;
        }
        
    }

}
