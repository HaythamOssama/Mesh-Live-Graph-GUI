package mesh.live.graph.gui.graphelements;
/**
 * Stores a single line/curve between two nodes
 * @author haytham
 */
public class Lines {
    private int startX, startY;
    private int endX, endY;

    public Lines(int startX, int startY, int endX, int endY){
        
    }
    public void setEndX(int endX) {
        this.endX = endX;
    }

    public void setEndY(int endY) {
        this.endY = endY;
    }

    public void setStartX(int startX) {
        this.startX = startX;
    }

    public void setStartY(int startY) {
        this.startY = startY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }
    
    
}
