/**
 *
 * @author Haytham
 */
package mesh.live.graph.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
/**
 * Starts the GUI in full screen
 */
public class MeshLiveGraphGUI extends Application {
    
    static FXMLDocumentController controller;
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GUIView.fxml"));
        Parent root = loader.load();
        controller = loader.getController();    
        Scene scene = new Scene(root);
        stage.setTitle("Mesh Live Graph Demo");
        stage.setMaximized(true); // For Full screen view if needed
        stage.setScene(scene);
        stage.show();
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
        
    }
}