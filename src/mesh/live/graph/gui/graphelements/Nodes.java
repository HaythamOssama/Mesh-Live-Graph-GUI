package mesh.live.graph.gui.graphelements;

/**
 * Represents any added node to the network
 */
public class Nodes {
    
    private int row;
    private int col;
    private String id;
    private String address;
    private String role;
    private boolean status;
    
    public Nodes(int row, int col, String id, String address, String role, boolean status){
        this.row = row;
        this.col = col;
        this.id  = id;
        this.address = address;
        this.role = role;
        this.status = status;
    }
    public Nodes(int row, int col){
        this.row = row;
        this.col = col;
    }
    public Nodes(String id){
        this.id = id;
    }
    public void setCol(int col) {
        this.col = col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public String getRole() {
        return role;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public boolean getStatus(){
        return status;
    }
    @Override
    public boolean equals(Object obj) {
        Nodes node = (Nodes) obj;
        if (this.getId().equals(node.getId()))
            return true;
        return false;
    }

    public void setRole(String role) {
        this.role = role;
    }
    
}
