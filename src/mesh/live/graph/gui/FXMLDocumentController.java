package mesh.live.graph.gui;

/**
 * @author Haytham
 */
import java.io.FileNotFoundException;
import mesh.live.graph.gui.graphelements.Nodes;
import mesh.live.graph.gui.chartstab.LiveChart;
import mesh.live.graph.gui.chartstab.LivePieChart;
import javafx.scene.text.Font;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.util.Pair;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controls all the GUI components <br>
 * Requires Local host class to be up and connected to the database
 */
public class FXMLDocumentController implements Initializable {
    
    /*/*****************************************************************************************************
     * *********************      Declarations of GUI components    ****************************************
     * *****  DON'T CHANGE ANY VARIABLE NAME UNLESS IT'S CHANGED IN FinalView.fxml OR IN SCENEBUILDER  *****
    ********************************************************************************************************/
    @FXML // Left panel titles
    private VBox title1,title2,title3, title4;
    @FXML // A Stack layout that contains a GridPane that holds all the nodes and a Drawing Pane that holds all the lines/curvers
    private StackPane stackPane;
    @FXML
    private GridPane gridPane;
    @FXML
    private Pane drawPane;
    @FXML // Buttons of the left panel
    private Button btnNext, btnPrev,btnDelete, btnRestart,btnHistory,btnTopology;
    @FXML // Upper tabs to switch between the live graph and live charts
    private Tab tab1, tab2, tab3;
    @FXML
    private VBox chartsVBox;
    @FXML
    private TableView sensorTable;
    @FXML
    private GridPane chartsGridPane;
    /*/*****************************************************************************************************
    * *********************     Declarations of non GUI variables   ****************************************
    ********************************************************************************************************/

    // GridPane height and width calculated at initialization
    private double gridHeight,gridWidth;
    // GridPane number of rows and columns calculated at initialization
    private int    gridRows,gridCols;
    // GridPane's cell height and width calculated at initialization
    private double gridCellHeight,gridCellWidth;
    // Stores any added nodes to the GridPane
    private ArrayList<Nodes> nodesList;
    // Stores any added lines to the Drawing Pane
    private ArrayList<Arrow> linesList;
    // Stores two nodes to draw a line between them
    private ArrayList<Nodes> drawList;
    // Stores the history
    private ArrayList<mesh.live.graph.gui.LocalHost.RowMessage> historyList;
    /**
     * Status = 0 : First Choice of the left panel <br>
     * Status = 1 : Second Choice of the left panel <br>
     * Status = 2 : Third Choice of the left panel <br>
     */
    private int status;
    /**
     * topologySelect = 0 : Custom topology
     * topologySelect = 1 : Diamond topology
     * topologySelect = 2 : One line topology
     */
    private int topologySelect; // Changes the topologies on the grid pane
    // Instance of the LocalHost class to read data from the localhost database
    private LocalHost localHost;
    // A background running task that listens to the localhost database
    private Task localHostListening;
    // Sleeping interval between every two successive reads from the database
    private int graphWaitInterval = 1000;
    // Controls the continuation/stoppage of the localHostListening task
    private boolean localHostThreadPause = false;
    // A thread that runs the aforementioned task
    private Thread localHostThread;
    // A Synchronization lock to control stoppage and continuation of the task
    private final Object pauseLock = new Object();
    
    /*/*****************************************************************************************************
    ***********************              Components Colors          ****************************************
    *******************************************************************************************************/
    
    static final private Color COLOR_RREQ          = javafx.scene.paint.Color.DARKMAGENTA;
    static final private Color COLOR_DRREQ         = javafx.scene.paint.Color.AQUA;
    static final private Color COLOR_RREP          = javafx.scene.paint.Color.BLUE;
    static final private Color COLOR_RERR          = javafx.scene.paint.Color.RED;
    static final private Color COLOR_RWAIT         = javafx.scene.paint.Color.YELLOW;
    static final private Color COLOR_DATA          = javafx.scene.paint.Color.WHITE;
    static final private Color COLOR_DATA_2        = javafx.scene.paint.Color.DARKGOLDENROD;
    static final private Color COLOR_CENTRAL       = Color.valueOf("E29100");//javafx.scene.paint.Color.GOLD;
    static final private Color COLOR_INTERMEDIATE  = Color.valueOf("F975A8"); //javafx.scene.paint.Color.PINK;//Color.valueOf("2F3EAE") ; // javafx.scene.paint.Color.979ED5;
    static final private Color COLOR_DESTINATION   = Color.valueOf("97979C") ;//javafx.scene.paint.Color.AQUA;
    static final private Color COLOR_DROPPED_NODE  = javafx.scene.paint.Color.RED;
    static final private Color COLOR_CIRCLE_STROKE = javafx.scene.paint.Color.WHITE;

    
    /**
     * Called when the GUI runs for the first time
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            nodesList = new ArrayList<Nodes>();
            drawList  = new ArrayList<Nodes>();
            linesList = new ArrayList<Arrow>();
            historyList = new ArrayList<mesh.live.graph.gui.LocalHost.RowMessage>();
            localHost = new LocalHost();
            status = 0; // First title is opened, others are hidden
            topologySelect=0; // Custom
            title2.setVisible(false);
            title3.setVisible(false);
            title4.setVisible(false);
            btnPrev.setDisable(true);
            // Create the localHostListeningTask and wait till status = 2 to run it
            createLocalHostListeningTask();
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }  
    
    /**
     * Called when a mouse click is detected on the GridPane / DrawingPane
     * @param event to get click coordination and button clicked
     */
    @FXML
    void gridPaneMouseClicked(MouseEvent event) {
        // If variables aren't initialized, initialize here
        if(gridHeight == 0){
            gridHeight     = gridPane.getHeight();
            gridWidth      = gridPane.getWidth();
            gridRows       = getRowCount();
            gridCols       = getColCount();
            gridCellHeight = stackPane.getHeight()/gridRows;
            gridCellWidth  = stackPane.getWidth()/gridCols;
        }
        // Set tabs width and font size to cover all the width
        tab1.setStyle("-fx-pref-width: "+gridWidth/3 +"; -fx-font-size: 20px;");
        tab2.setStyle("-fx-pref-width: "+gridWidth/3 +"; -fx-font-size: 20px;");
        tab3.setStyle("-fx-pref-width: "+gridWidth/3 +"; -fx-font-size: 20px;");

        if(event.getButton() == MouseButton.PRIMARY){
            // Status = 0 >> Placing/Removing nodes on/from the GridPane
            if(status == 0){ // Add nodes to clicked cells
                // Get the row and column corresponding to the clicked coordinates
                Pair<Integer,Integer> rowColPair = getSelectedRowCol(event);
                int row= rowColPair.getKey();
                int col = rowColPair.getValue();
                
                // Search for a node that already exists in the same (row,col)
                Nodes searchFor = isNodeExist(row, col);
                if(searchFor!= null){ 
                    // if found, remove it from the GridPane
                    removeNodeByRowColumnIndex(row,col);
                    nodesList.remove(new Nodes("circle"+col+","+row));
                }
                else{ // if not found, add a new node
                    Pair<String,String> addressTypePair = showNodeInfoDialog(); // Pop out a dialog to get the unicast address and node type
                    if(addressTypePair != null){ 
                        
                        Circle circle = new Circle(gridCellHeight/2); // Draw a new node in grid cell
                        circle.setId("circle"+col+","+row); // Set ID to track the drawn circle for future use
                        circle.setFill("Central Node".equals(addressTypePair.getValue()) ? COLOR_CENTRAL :
                                "Intermediate Node".equals(addressTypePair.getValue()) ? COLOR_INTERMEDIATE : COLOR_DESTINATION);
                        circle.setStroke(COLOR_CIRCLE_STROKE);
                        circle.setStrokeWidth(3);
                        gridPane.add(circle, col, row); // Set the circle color depending on node's role
                        // Add the drawn node to the nodes list
                        nodesList.add(new Nodes(row,col,"circle"+col+","+row,addressTypePair.getKey(),addressTypePair.getValue(),true));
                    }
                }
            }
            // Status = 1 >> Testing the network by selecting two successive nodes to draw a line between them
            else if (status == 1){
                // Get the row and column corresponding to the clicked coordinates
                Pair<Integer,Integer> rowColPair = getSelectedRowCol(event);
                int row= rowColPair.getKey();
                int col = rowColPair.getValue();
                
                if(isNodeExist(row, col) != null){ // If the selected cell contains a circle
                    if(drawList.size() == 1){ // If the list contains a node to start drawing from
                        Nodes node= getNodeByRowCol(row,col);
                        if(!drawList.contains(node))
                            drawList.add(node);
                        // Draw a line between both nodes 
                        drawLine((drawList.get(0)), (drawList.get(1)),"RREQ");
                        drawLine((drawList.get(0)), (drawList.get(1)),"RREP");
                        drawLine((drawList.get(0)), (drawList.get(1)),"Data");                        
                        drawList.clear(); // clear the draw list to accept new nodes
                    }
                    else{ // If the node is empty, add a starting node
                        Nodes node= getNodeByRowCol(row,col);
                        if(!drawList.contains(node))
                            drawList.add(node);
                    }  
                }
                
            }
        }
        // If the secondary (right) click is clicked, show the node's info
        else if(event.getButton() == MouseButton.SECONDARY && status > 0){
            showNodeInfo(event);
        }
    }

    /**
     * Handles carried actions when the next button is clicked
     */
    @FXML
    void btnNextClicked(ActionEvent event) {
        // If current status = 0 >> Placing the nodes on the grid cell
        if(status == 0){
            status++;
            title2.setVisible(true); // Move to stage 2 >> testing
            gridPane.setGridLinesVisible(false); // Hide the GridPane lines
            btnPrev.setDisable(false); // Now previous button can be used
            drawPane.toFront(); // Place the Drawing Pane on the front
        }
        // If current status = 1 >> Testing the network
        else if(status == 1){
            // Move to stage 3 >> Listening to the local host
            status++; 
            title3.setVisible(true); 
            title4.setVisible(true);
            btnNext.setDisable(true); 
            startLocalHostListening();
        }
        
    }
    
    /**
     * Handles carried actions when the previous button is clicked
     * @throws InterruptedException for pausing the localHostListening Task
     */
    @FXML
    void btnPrevClicked(ActionEvent event) throws InterruptedException{
        // If current status = 1 >> Testing the network
        if(status == 1){
            title2.setVisible(false);
            status--;
            gridPane.setGridLinesVisible(true);
            gridPane.toFront();
            btnPrev.setDisable(true);
        }
        // If current status = 2 >> Listening to the local host
        else if(status == 2){
            title3.setVisible(false);
            title4.setVisible(false);
            status--;
            btnNext.setDisable(false);
            pauseLocalHostListening();
        }
    }
    
    /**
     * Flushes the database
     * @param event
     * @throws SQLException 
     */
    @FXML
    void btnDeleteClicked(ActionEvent event) throws SQLException {
        localHost.DeleteAllRecords();
    }
    
    /**
     * TODO
     * Deletes all lines1
     * @param event 
     */
    @FXML
    void btnRestartClicked(ActionEvent event) {
        for(Arrow line : linesList){
            drawPane.getChildren().remove(line);
        }
        drawList.clear();
    }
    
    void addNodeToGridPane(int row, int col, String role, String address){
        if(gridHeight == 0){
            gridHeight     = gridPane.getHeight();
            gridWidth      = gridPane.getWidth();
            gridRows       = getRowCount();
            gridCols       = getColCount();
            gridCellHeight = stackPane.getHeight()/gridRows;
            gridCellWidth  = stackPane.getWidth()/gridCols;
        }
        Circle circle = new Circle(gridCellHeight/2);
        circle.setId("circle"+col+","+row); // Set ID to track the drawn circle for future use
        circle.setFill(role.equals("Central Node") ? COLOR_CENTRAL : role.equals("Intermediate Node") ? COLOR_INTERMEDIATE : COLOR_DESTINATION);
        circle.setStroke(COLOR_CIRCLE_STROKE);
        circle.setStrokeWidth(3);
        gridPane.add(circle, col, row); // Set the circle color depending on node's role
        // Add the drawn node to the nodes list
        nodesList.add(new Nodes(row,col,"circle"+col+","+row,address,role,true));
    }
    @FXML
    void btnTopologyClicked(ActionEvent event) throws InterruptedException{
        gridPane.getChildren().clear();
        nodesList.clear();
        drawPane.getChildren().clear();
        linesList.clear();
        historyList.clear();
        switch (topologySelect) {
            case 0:
                // Add the diamond topology
                topologySelect++;
                addNodeToGridPane(0 , 5 , "Central Node"     , "0001");
                addNodeToGridPane(5 , 0 , "Intermediate Node", "0002");
                addNodeToGridPane(10, 5 , "Destination Node" , "0003");
                addNodeToGridPane(5 , 10, "Intermediate Node", "0004");
                //addNodeToGridPane(5 , 3 , "Intermediate Node", "0005");
                //addNodeToGridPane(5 , 7 , "Intermediate Node", "0006");
                btnTopology.setText("Diamond");
                break;
            case 1:
                // Add the one line topology
                topologySelect++;
                // Add central node
                addNodeToGridPane(5, 0, "Central Node", "0001");
                // Add left branch
                addNodeToGridPane(5, 3, "Intermediate Node", "0002");
                // Add right branch
                addNodeToGridPane(5, 7, "Intermediate Node", "0003");
                // Add destination node
                addNodeToGridPane(5, 10, "Destination Node", "0004");
                btnTopology.setText("Line");
                break;
            case 2:
                // Custom topology, remove all nodes
                topologySelect = 0;
                btnTopology.setText("Custom");
                break;
            default:
                break;
        }
    }
    /**
     * When a history button is clicked, a pop out dialog viewing all graph history
     * @param event 
     */
    @FXML
    void btnHistoryClicked(ActionEvent event) throws FileNotFoundException{
        TableView<LocalHost.RowMessage> table = new TableView();
        table.setEditable(true);
        table.setPrefSize(gridWidth/2, gridHeight-300);
        TableColumn sourceAddressCol = new TableColumn("Source Address");
        TableColumn messageCol= new TableColumn("Message");
        TableColumn destinationAddressCol = new TableColumn("Destination Address");
        TableColumn timestampCol = new TableColumn("Timestamp");
        
        sourceAddressCol.setCellValueFactory     (new PropertyValueFactory<>("sourceAddress"));
        messageCol.setCellValueFactory           (new PropertyValueFactory<>("controlMessage"));
        destinationAddressCol.setCellValueFactory(new PropertyValueFactory<>("destinationAddress"));
        timestampCol.setCellValueFactory         (new PropertyValueFactory<>("timestamp"));
        
        sourceAddressCol.setPrefWidth(gridWidth/8);
        messageCol.setPrefWidth(gridWidth/8);
        destinationAddressCol.setPrefWidth(gridWidth/8);
        timestampCol.setPrefWidth(gridWidth/8);
        table.setStyle("-fx-font-size:20px;");

        table.getColumns().addAll(sourceAddressCol,messageCol,destinationAddressCol,timestampCol);

        for(mesh.live.graph.gui.LocalHost.RowMessage message : historyList){
            table.getItems().add(message);
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setWidth(gridWidth);
        alert.setHeight(gridHeight);
        alert.setHeaderText("Messages History");
        alert.setWidth(1000);
        alert.setHeight(1000);
        alert.getDialogPane().setContent(new ScrollPane(table));
        alert.showAndWait();

    }
    /*************************************************
     **********     GRIDPANE FUNCTIONS      **********
     *************************************************/
    /**
     * @return the number of rows of the GridPane 
     */
    private int getRowCount() {
        int numRows = gridPane.getRowConstraints().size();
        for (int i = 0; i < gridPane.getChildren().size(); i++) {
            Node child = gridPane.getChildren().get(i);
            if (child.isManaged()) {
                Integer rowIndex = GridPane.getRowIndex(child);
                if(rowIndex != null){
                    numRows = Math.max(numRows,rowIndex+1);
                }
            }
        }
        return numRows;
    }
    /**
     * @return the number of columns of the GridPane 
     */
    private int getColCount() {
        int numRows = gridPane.getColumnConstraints().size();
        for (int i = 0; i < gridPane.getChildren().size(); i++) {
            Node child = gridPane.getChildren().get(i);
            if (child.isManaged()) {
                Integer rowIndex = GridPane.getColumnIndex(child);
                if(rowIndex != null){
                    numRows = Math.max(numRows,rowIndex+1);
                }
            }
        }
        return numRows;
    }
    
    /**
     * checks if the node corresponding to the passed row and column is found in the nodesList
     * @return the found node or null indicating not found
     */
    private Nodes isNodeExist(int row, int col){
        for(Nodes node : nodesList){
            if(node.getRow() == row && node.getCol() == col){
                return node;
            }
        }
        return null;
    }
    /**
     * removes a node from the GridPane corresponding to the passed row and column
     */
    private void removeNodeByRowColumnIndex(final int row,final int column) {
        ObservableList<Node> childrens = gridPane.getChildren();
        for(Node node : childrens) {
            // Remove instances of the circle object only
            if(node instanceof Circle && gridPane.getRowIndex(node) == row && gridPane.getColumnIndex(node) == column) {
                gridPane.getChildren().remove(node);
                break;
            }
        } 
    }
    /**
     * Gets the row and the column corresponding to a mouse click
     * @param event mouse click
     * @return a pair having the corresponding row and column
     */
    private Pair<Integer,Integer> getSelectedRowCol(MouseEvent event){
        int row=-1,col=-1;
        for (int i = 0; i < gridRows; i++) {
                if(event.getY() > gridCellHeight*i && event.getY() < gridCellHeight*(i+1)){
                    row = i;
                }
            }
        for (int i = 0; i < gridCols; i++) {
            if(event.getX() > gridCellWidth*i && event.getX() < gridCellWidth*(i+1)){
                col = i;
            }
        }
        return new Pair<Integer,Integer>(row,col);
    }
    /**
     * pops out a box holding the nodes information (Address,Role,Status)
     * @param event mouse click
     */
    private void showNodeInfo(MouseEvent event){
        // Get the row and column of the selected node
        Pair<Integer,Integer> rowColPair = getSelectedRowCol(event);
        int row= rowColPair.getKey();
        int col = rowColPair.getValue();
        // Get the node corresponding to the fetched row and column
        Nodes node = getNodeByRowCol(row, col);
        // Define a new GridPane that holds the node's information
        GridPane infoGridPane = new GridPane();
        // Set the coordinates of the information box
        infoGridPane.setLayoutX((col+ (col>gridCols/2 ? -1 : 1))*gridCellWidth);
        infoGridPane.setLayoutY((row+ (row>gridRows/2 ? -1 : 1))*gridCellHeight);
        // Draw a border around the Grid Pane with background color
        infoGridPane.setStyle("-fx-border-color : black; -fx-border-radius : 8; -fx-background-color : #998B8B");
        
        // Form the following table by 6 labels
        /********* + *********
         * Address +         *
         * ******* + *********
         * Status  +         * 
         * ******* + *********
         * Role    +         *
         * *******************
         */
        Label []data = new Label[6];
        String []stringData = new String[]{
            "Address"," : "+node.getAddress(),
            "Role" , " : "+node.getRole(),
            "Status", " : "+(node.getStatus() ? "Running" : "Down")
        };
        for (int i = 0; i <=5; i++) {
            data[i] = new Label(stringData[i]);  
            data[i].setFont(new Font("Arial",20));
            data[i].setTextFill(Color.WHITE);
        }      
        // Add the 6 labels to the information Grid Pane
        infoGridPane.add(data[0], 0, 0);
        infoGridPane.add(data[1], 1, 0);
        infoGridPane.add(data[2], 0, 1);
        infoGridPane.add(data[3], 1, 1);
        infoGridPane.add(data[4], 0, 2);
        infoGridPane.add(data[5], 1, 2);
        infoGridPane.setPadding(new Insets(10,10,10,10));
        
        infoGridPane.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t) {
                // If the box is clicked, remove it
                drawPane.getChildren().remove(infoGridPane);
            }
        });
        // Add the box to the Drawing Pane
        drawPane.getChildren().add(infoGridPane);
    }
    /**
     * TESTING ONLY : List all drawn nodes by address
     */
    private void listAllNodes(){
        for(Nodes node : nodesList){
            System.out.println("Node : " + node.getAddress());
        }
    }
    /**
     * Draws a line/curve between two nodes with an arrow
     * @param startNode Line/Curve starts from this node
     * @param endNode Line/Curve ends from this node
     * @param controlMessage RREQ/RREP/RERR/RWAIT/Data
     */
    public void drawLine(Nodes startNode, Nodes endNode, String controlMessage){
        System.err.println("[GUI] drawLine : Drawing ("+controlMessage+") between = "+ startNode.getAddress() + " - " + endNode.getAddress());
        // Define a new line/curve with an arrow 
        Arrow arrow = new Arrow(controlMessage,startNode,endNode);
        for(Nodes node : nodesList){ // get the row and column of the start and the end nodes
            // Set the start coordinates of the arrow to begin from the center of the starting node
            if(node.getAddress().equals((startNode.getAddress()))){
                arrow.setStart(new Pair<Double,Double>((node.getCol()+0.5)*gridCellWidth,(node.getRow()+0.5)*gridCellHeight)); 
            }
            // Set the end coordinates of the arrow to begin from the center of the end node
            if(node.getAddress().equals((endNode.getAddress()))){
                arrow.setEnd(new Pair<Double,Double>((node.getCol()+0.5)*gridCellWidth,(node.getRow()+0.5)*gridCellHeight));
            }
        }
        // Draw and add the arrow to the Drawing Pane and line list
        arrow.draw();
        drawPane.getChildren().add(arrow);
        linesList.add(arrow);
    }
    
    /**
     * Pops out a dialog for the user to enter the address and the role of the node
     * @return a pair having the address and the role
     */
    private Pair<String,String> showNodeInfoDialog(){
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Node Information");
        dialog.setHeaderText("Please Enter the node's information");
        // Set the button types.
        ButtonType addButtonType = new ButtonType("Add", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        // Define a Grid Pane to hold the text field and the radio buttons
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        TextField address = new TextField();
        address.setPromptText("Address");
        RadioButton radio1 = new RadioButton("Central Node");
        RadioButton radio2 = new RadioButton("Intermediate Node");
        RadioButton radio3 = new RadioButton("Destination Node");
        grid.add(new Label("Address:"), 0, 0);
        grid.add(address, 1, 0);
        grid.add(new Label("Role:"), 0, 1);
        grid.add(radio1, 1, 1);
        grid.add(radio2, 1, 2);
        grid.add(radio3, 1, 3);
        // If one radio button is checked, uncheck the others
        radio1.selectedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
            if (isNowSelected) { 
                radio2.setSelected(false);
                radio3.setSelected(false);
            } 
        }
        });
        radio2.selectedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
            if (isNowSelected) { 
                radio1.setSelected(false);
                radio3.setSelected(false);
            } 
        }
        });
        radio3.selectedProperty().addListener(new ChangeListener<Boolean>() {
        @Override
        public void changed(ObservableValue<? extends Boolean> obs, Boolean wasPreviouslySelected, Boolean isNowSelected) {
            if (isNowSelected) { 
                radio1.setSelected(false);
                radio2.setSelected(false);
            } 
        }
        });
        
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        // Do some validation (using the Java 8 lambda syntax).
        address.textProperty().addListener((observable, oldValue, newValue) -> {
            boolean alreadyExist = false;
                for(Nodes node : nodesList){
                    if(node.getAddress().equals(address.getText())){
                        alreadyExist = true;
                    }
                }
                if(alreadyExist){ // Prevent entering non unique addresses 
                    dialog.setHeaderText("Address already exists, choose another one");
                    addButton.setDisable(true);
                }
                else{ // Indicate that the entered address is valud
                    dialog.setHeaderText("Valid Address");
                    addButton.setDisable(newValue.trim().isEmpty() && (!radio1.isSelected() && !radio2.isSelected() && !radio3.isSelected()));
                }
            });

        dialog.getDialogPane().setContent(grid);
        // Request focus on the address field by default.
        Platform.runLater(() -> address.requestFocus());
        // Convert the result to a address-type-pair when the add button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                    return new Pair<>(address.getText(), radio1.isSelected() ? radio1.getText() : 
                            radio2.isSelected() ? radio2.getText() : radio3.getText());                        
            }
            return null;
        });
        Optional<Pair<String, String>> result = dialog.showAndWait();
        if(result.isPresent()){
            return result.get();
        }
        return null;
    }
    /**
     * Creates the local host listening task with the ability to start and pause it
     */
    private void createLocalHostListeningTask(){
        localHostListening = new Task<Void>() {
        @Override
        public Void call() throws Exception {
            while (true) {
                synchronized(pauseLock){ // A Synchronization lock to control stoppage/continuation of the task
                    if(localHostThreadPause){
                        System.err.println("[GUI] createLocalHostListeningTask() : Paused");
                        pauseLock.wait();
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run(){
                            try{
                                System.err.println("[GUI] createLocalHostListeningTask() : Running");
                                // Fetch messages from the local host
                                LocalHost.RowMessage row = localHost.FetchMessage();
                                String src = row.getSourceAddress();
                                if(row.getDestinationAddress() != null && row.getSourceAddress() != null){
                                    if(row.getControlMessage().equals("Data")){
                                        graphWaitInterval=800;
                                    }
                                    else{
                                        graphWaitInterval=1000;
                                    }
                                    // Get the nodes of the source and the destination addresses
                                    Nodes startNode = null, endNode = null;
                                    historyList.add(row);
                                    if(row.getSourceAddress().equals("N/A")){
                                        for(Nodes dropped : nodesList){
                                            if(dropped.getAddress().equals(row.getDestinationAddress())){
                                                dropped.setStatus(false);
                                                removeNodeByRowColumnIndex(dropped.getRow(), dropped.getCol());
                                                Circle circle = new Circle(gridCellHeight/2); // Draw a new node in grid cell
                                                circle.setFill(COLOR_DROPPED_NODE);
                                                circle.setStroke(COLOR_CIRCLE_STROKE);
                                                circle.setStrokeWidth(3);
                                                gridPane.add(circle,dropped.getCol(),dropped.getRow());
                                            }
                                        }
                                    }
                                    else{
                                        for(Nodes node : nodesList){
                                            if(src.equals(node.getAddress()) ){
                                                startNode = node;
                                            }
                                            if (row.getDestinationAddress().equals(node.getAddress())){
                                                endNode = node;
                                            }
                                        }
                                        if(row.getControlMessage().equals("Data")){
                                            for(Arrow line : linesList){
                                                if(line.getControlMessage().equals("RREQ") || line.getControlMessage().equals("RWAIT")){
                                                    drawPane.getChildren().remove(line);
                                                }
                                            }
                                        }
                                        boolean found = false;
                                        if(row.getControlMessage().equals("Data")){
                                            for(Arrow line : linesList){
                                                if(line.getNodeStart() == endNode && line.getNodeEnd() == startNode 
                                                        && line.getControlMessage().equals("Data") ){
                                                    found = true;
                                                    if(line.getStroke() == COLOR_DATA){
                                                        line.setStroke(COLOR_DATA_2);
                                                    }
                                                    else if(line.getStroke() == COLOR_DATA_2){
                                                        line.setStroke(COLOR_DATA);
                                                    }
                                                }
                                            }
                                        }
                                        if(!found){
                                            drawLine(endNode, startNode, row.getControlMessage());
                                        }
                                        // Remove the record from the database to avoid drawing it again
                                        localHost.DeleteMessage(row.getTimestamp());
                                    }                                
                                }
                            }
                            catch(SQLException | InterruptedException ex){
                                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                    Thread.sleep(graphWaitInterval);
                }
              }
            }
        };
    }
    /*
    /**
     * Creates the local host listening thread or continues it
     */
    private void startLocalHostListening(){
        if(localHostThread == null){
            localHostThread = new Thread(localHostListening);
            localHostThread.setDaemon(true);
            localHostThread.start();
        }
        else{
            synchronized (pauseLock) {
                localHostThreadPause = false;
                pauseLock.notifyAll(); // Unblocks thread
            }
        }
        
    }
    
    /**
     * Pauses the local host listening thread 
     */
    private void pauseLocalHostListening() throws InterruptedException{
        localHostThreadPause = true;
    }
    /**
     * Fetches a node by the passed row and column
     * @return returns a Nodes object or null indicating not found
     */
    private Nodes getNodeByRowCol(int row, int col){
        for(Nodes node : nodesList){
            if(node.getCol() == col && node.getRow() == row){
                return node;
            }
        }
        return null;
    }
    /**
     * Depending on the fetched control message : <br>
     *      - [RREQ/RREP/RERR/RWAIT] Draws a line between two nodes with an arrow at the end node <br>
     *      - [Data]                 Draws a curve between two nodes with an arrow at the end node <br>
     * TODO :  <br>
     *      - The control point of curve isn't set probably, the commented 
     *        getControlPoints function needs to be fixed
     */
    class Arrow extends Path{
        private static final double DEFAULT_ARROW_SIZE = 15.0; // Arrow size
        private double startX, startY;                         // Starting coordinates
        private double endX, endY;                             // Ending coordinates
        private Nodes nodeStart, nodeEnd;                      // Start and End Nodes
        private String controlMessage;                         // RREQ/RREP/RERR/RWAIT?Data
        private double controlPointAdjustFactor;

        public Nodes getNodeStart() {
            return nodeStart;
        }

        public Nodes getNodeEnd() {
            return nodeEnd;
        }
        
        
        public Arrow(String controlMessage, Nodes nodeStart, Nodes nodeEnd){
            this.controlMessage = controlMessage;
            // Set the line/curve color depending on the control message
            setStroke(controlMessage.equals("RREQ")? COLOR_RREQ : controlMessage.equals("RREP") ? COLOR_RREP :
                    controlMessage.equals("RERR") ? COLOR_RERR : controlMessage.equals("DRREQ") ? COLOR_DRREQ : COLOR_RWAIT);
            setStrokeWidth(5);
            if(controlMessage.equals("RERR")){
                setStrokeWidth(15);
            }
            this.nodeStart = nodeStart;
            this.nodeEnd = nodeEnd;
        }
        
        /**
         * called to start drawing after initializing the instance by the needed coordinates
         */
        public void draw(){
            double angle; // Arrow angle
            if(!"Data".equals(controlMessage) && !"RREP".equals(controlMessage)){
                // If the control message is RREQ/RREP/RERR/RWAIT draw a line
                getElements().add(new MoveTo(startX, startY));
                getElements().add(new LineTo(endX, endY));
                angle = Math.atan2((endY-startY), (endX - startX)) - Math.PI / 2.0;
            }
            else{
                Point2D controlPoints = getControlPoint(new Point2D(startX,startY), new Point2D(endX,endY));
                double controlX = controlPoints.getX();
                double controlY = controlPoints.getY();
                // If the control message is Data draw a curve
                // Initialize the curve 
                QuadCurveTo curve = new QuadCurveTo(controlX,controlY, endX, endY);
                setStroke(Color.CHARTREUSE);
                setStrokeWidth(5);
                setFill(null);
                getElements().add(new MoveTo(startX, startY));
                getElements().add(curve);
                if(controlMessage.equals("RREP")){
                    setStroke(COLOR_RREP);
                }
                else if(controlMessage.equals("Data")){
                    setStroke(COLOR_DATA);
                }
                /* To get the right orienation of the arrow, get an angle of the line connecting
                    the end point and a point on the edge of the cell */
                ParabolaEquation parabolaEquation= new ParabolaEquation(new Point2D(startX, startY),
                        new Point2D(controlX,controlY), 
                        new Point2D(endX,endY));
                Point2D nearEnd = parabolaEquation.getNearPoint();
                angle = Math.atan2((nearEnd.getY() - endY),(nearEnd.getX() - endX))+ Math.PI / 2.0;
            }
            //ArrowHead      
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            //point1
            double x1 = (- 1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * DEFAULT_ARROW_SIZE + endX;
            double y1 = (- 1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * DEFAULT_ARROW_SIZE + endY;
            //point2
            double x2 = (1.0 / 2.0 * cos + Math.sqrt(3) / 2 * sin) * DEFAULT_ARROW_SIZE + endX;
            double y2 = (1.0 / 2.0 * sin - Math.sqrt(3) / 2 * cos) * DEFAULT_ARROW_SIZE + endY;
            
            getElements().add(new LineTo(x1, y1));
            getElements().add(new LineTo(x2, y2));
            getElements().add(new LineTo(endX, endY));

        }   
        public void setStart(Pair<Double,Double> xy){
            startX = xy.getKey();
            startY = xy.getValue();
        }
        public void setEnd(Pair<Double,Double> xy){
            endX = xy.getKey();
            endY = xy.getValue();
        }
        public String getControlMessage(){
            return this.controlMessage;
        }
        public void setControlPointAdjustFactor(double point){
            this.controlPointAdjustFactor = point;
        }
        public double getControlPointAdjustFactor(){
            return this.controlPointAdjustFactor;
        }
        private Point2D getControlPoint(Point2D start, Point2D end){
            boolean found = false;
            if(controlMessage.equals("RREP")){
                for(Arrow line : linesList){
                    if(line.getNodeStart() == nodeStart && line.getNodeEnd() == nodeEnd){
                        found = true;
                        this.controlPointAdjustFactor = line.getControlPointAdjustFactor() + 150;
                    }
                }
            }
            if(!found){
                this.controlPointAdjustFactor = 250;
            }
            
            setControlPointAdjustFactor(this.controlPointAdjustFactor);
            
            System.err.println("Control Point : " + controlPointAdjustFactor );
            // Get mid point between the start and end
            Point2D center = new Point2D((start.getX()+end.getX())/2 ,(start.getY()+end.getY())/2);
            // Get angle of the line between start and end
            double angle = Math.atan2(Math.abs(start.getY()-end.getY()),Math.abs(start.getX()-end.getX()));
            double height = Math.abs(center.getY()-end.getY());
            if(controlMessage.equals("Data")){
                if(start.getY() == end.getY()){
                    // Nodes are horizontal
                    return new Point2D(center.getX(), center.getY()-controlPointAdjustFactor);
                }
                if(start.getX() == end.getX()){
                    // Nodes are vertical
                    return new Point2D(center.getX()-controlPointAdjustFactor, center.getY());
                }
                else{
                    return new Point2D(center.getX()-controlPointAdjustFactor*Math.cos(angle), center.getY()-controlPointAdjustFactor*Math.cos(angle));
                }
            }
            else if(controlMessage.equals("RREP")){
                if(start.getY() == end.getY()){
                    // Nodes are horizontal
                    return new Point2D(center.getX(), center.getY()+controlPointAdjustFactor);
                }
                if(start.getX() == end.getX()){
                    // Nodes are vertical
                    return new Point2D(center.getX()+controlPointAdjustFactor, center.getY());
                }
                else{
                    return new Point2D(center.getX()+controlPointAdjustFactor*Math.cos(angle), center.getY()+controlPointAdjustFactor*Math.cos(angle));
                }
            }
            else{
                return null;
            }
        }
    }
    /**
     * Gets the equation of the drawn curve to get a point near the end point <br>
     * Parabola Equation : B(t)=(1-t)*(1-t)*P0 + 2*(1-t)*t*P1 + t*t*P2, t in range of (0,1) <br>
     *      - P0 : Starting Point <br>
     *      - P1 : Control Point <br>
     *      - P2 : End Point <br>
     *      - t  : How far or near the point is to the start or end point
     *              (0 : start point - 1 : end point)
     */
    class ParabolaEquation{
        private Point2D controlPoint,endPoint,startPoint;
        public ParabolaEquation(Point2D startPoint, Point2D controlPoint , Point2D endPoint){
            this.controlPoint = controlPoint;
            this.endPoint = endPoint;
            this.startPoint = startPoint;
        }
        
        public Point2D getNearPoint(){
            double t = 0.9;
            double x = (1-t)*(1-t)*startPoint.getX() + 2*(1-t)*t*controlPoint.getX() + t*t*endPoint.getX();
            double y = (1-t)*(1-t)*startPoint.getY() + 2*(1-t)*t*controlPoint.getY() + t*t*endPoint.getY();
            return new Point2D(x,y);
        }
    }

    private boolean chartsTabFlag = false;
    @FXML
    void chartsTabClicked(Event event) throws ClassNotFoundException, SQLException, InterruptedException {
        if(!chartsTabFlag){
            if(gridHeight == 0){
                gridHeight     = gridPane.getHeight();
                gridWidth      = gridPane.getWidth();
                gridRows       = getRowCount();
                gridCols       = getColCount();
                gridCellHeight = stackPane.getHeight()/gridRows;
                gridCellWidth  = stackPane.getWidth()/gridCols;
            }
            LiveChart liveChart = new LiveChart(chartsGridPane, "Throughput",localHost);
            LivePieChart livePieChart = new LivePieChart(chartsGridPane,localHost, nodesList);
            //E2ETable e2ETable = new E2ETable(localHost, chartsGridPane, gridWidth,nodesList);
            chartsTabFlag = true;
        }
        
    }
    @FXML
    void sensorTabClicked(Event event) throws SQLException, InterruptedException{
        if(gridHeight == 0){
            gridHeight     = gridPane.getHeight();
            gridWidth      = gridPane.getWidth();
            gridRows       = getRowCount();
            gridCols       = getColCount();
            gridCellHeight = stackPane.getHeight()/gridRows;
            gridCellWidth  = stackPane.getWidth()/gridCols;
        }
        SensorTable liveTable = new SensorTable(localHost, sensorTable, gridWidth);
    }
}