package pathfinder.model;

import java.util.ArrayList;
import java.util.List;

public class Grid {

    private final int rows;
    private final int cols;
    private final Node[][] nodes;

    private Node startNode = null;
    private Node goalNode  = null;

    public Grid(int rows, int cols) {
        this.rows  = rows;
        this.cols  = cols;
        this.nodes = new Node[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                nodes[r][c] = new Node(r, c);
    }


    public Node get(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return null;
        return nodes[row][col];
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Node getStartNode() { return startNode; }
    public Node getGoalNode()  { return goalNode;  }

    public void setStart(int row, int col) {
        if (startNode != null) startNode.setType(Node.Type.FREE);
        startNode = nodes[row][col];
        startNode.setType(Node.Type.START);
    }

    public void setGoal(int row, int col) {
        if (goalNode != null) goalNode.setType(Node.Type.FREE);
        goalNode = nodes[row][col];
        goalNode.setType(Node.Type.GOAL);
    }

    public void setTerrain(int row, int col, Node.Terrain terrain) {
        Node n = nodes[row][col];
        if (n.isWall() || n.isStart() || n.isGoal()) return;
        n.setTerrain(terrain);
    }

    public void toggleWall(int row, int col) {
        Node n = nodes[row][col];
        if (n.isWall()) {
            n.setType(Node.Type.FREE);
        } else if (n.getType() == Node.Type.FREE) {
            n.setType(Node.Type.WALL);
        }
    }

    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        int r = node.getRow(), c = node.getCol();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            Node nb = get(r + d[0], c + d[1]);
            if (nb != null && !nb.isWall()) neighbors.add(nb);
        }
        return neighbors;
    }

    public void resetSearchState() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                nodes[r][c].resetSearch();
    }

    public void clearAll() {
        startNode = null;
        goalNode  = null;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                nodes[r][c].setType(Node.Type.FREE);
                nodes[r][c].setTerrain(Node.Terrain.ROAD);
            }
    }

    public void clearWalls() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (nodes[r][c].isWall()) nodes[r][c].setType(Node.Type.FREE);
    }


    public Grid copyWalls() {
        Grid copy = new Grid(rows, cols);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Node src = nodes[r][c];
                Node dst = copy.nodes[r][c];
                if (src.isWall()) {
                    dst.setType(Node.Type.WALL);
                } else { // è calpestabile, copio il tipo di terreno dalla cella originale
                    dst.setTerrain(src.getTerrain());
                }
            }
        }
        return copy;
    }

    // calcola la percentuale di occupazione di un terreno sulla griglia, usata come feature
    public double terrainFraction(Node.Terrain t) {
        int total = 0, count = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                Node n = nodes[r][c];
                if (n.isWall()) continue;
                total++;
                if (n.getTerrain() == t) count++;
            }
        return total == 0 ? 0 : (double) count / total;
    }
}
