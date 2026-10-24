package pathfinder.gui.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GridCanvas extends Canvas {

    public static final int DEFAULT_ROWS = 60;
    public static final int DEFAULT_COLS = 80;
    public static final int DEFAULT_CELL = 12;

    private int rows;
    private int cols;
    private int cell;

    private static final Color C_ROAD    = Color.WHITE;
    private static final Color C_GRASS   = Color.web("#a8d5a2");
    private static final Color C_WATER   = Color.web("#89c4e1");
    private static final Color C_WALL    = Color.DARKSLATEGRAY;
    private static final Color C_START   = Color.GREEN;
    private static final Color C_GOAL    = Color.RED;
    private static final Color C_OPEN    = Color.web("#89c4ff"); // Ciano per la frontiera
    private static final Color C_CLOSED  = Color.web("#ffd580"); // Arancione chiaro per esplorati
    private static final Color C_PATH    = Color.ORANGE;
    private static final Color C_GRID    = Color.LIGHTGRAY;

    public enum EditMode { WALL, ROAD, GRASS, WATER }

    private Grid     grid;
    private EditMode editMode = EditMode.WALL;

    private Set<Node> openSet   = new HashSet<>();
    private Set<Node> closedSet = new HashSet<>();
    private Set<Node> pathSet   = new HashSet<>();

    private Runnable onChanged;

    private int     lastDragRow  = -1;
    private int     lastDragCol  = -1;
    private Boolean dragPainting = null;

    // Gestione Drag & Drop per Start e Goal
    private enum DragTarget { NONE, START, GOAL, PAINTING }
    private DragTarget currentDragTarget = DragTarget.NONE;

    public GridCanvas() {
        this(DEFAULT_ROWS, DEFAULT_COLS, DEFAULT_CELL);
    }

    public GridCanvas(int rows, int cols, int cell) {
        super(cols * cell, rows * cell);
        this.rows = rows;
        this.cols = cols;
        this.cell = cell;
        setupMouse();
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getCell() { return cell; }

    public Grid resize(int newRows, int newCols, int newCell) {
        this.rows = newRows;
        this.cols = newCols;
        this.cell = newCell;
        setWidth(cols * cell);
        setHeight(rows * cell);
        clearOverlay();
        Grid newGrid = new Grid(rows, cols);
        setGrid(newGrid);
        return newGrid;
    }

    public void setGrid(Grid g)          { this.grid = g; clearOverlay(); redraw(); }
    public void setEditMode(EditMode m)  { this.editMode = m; }
    public void setOnChanged(Runnable r) { this.onChanged = r; }

    public void clearOverlay() {
        openSet.clear();
        closedSet.clear();
        pathSet.clear();
    }

    public void applyStep(pathfinder.model.SearchStep step) {
        boolean stateChanged = false;

        if (step.state() == NodeState.OPEN) {
            stateChanged = openSet.add(step.node()); // True se il nodo non era già in openSet
        } else if (step.state() == NodeState.CLOSED) {
            boolean removedFromOpen = openSet.remove(step.node());
            boolean addedToClosed   = closedSet.add(step.node());
            stateChanged = removedFromOpen || addedToClosed; // True se si è mosso da open a closed o è nuovo
        }

        // Ridisegna la cella SOLO SE lo stato è realmente cambiato!
        if (stateChanged) {
            drawCell(getGraphicsContext2D(), step.node());
        }
    }

    public void showPath(List<Node> path) {
        pathSet = new HashSet<>(path);
        redraw();
    }

    public void redraw() {
        if (grid == null) return;
        GraphicsContext gc = getGraphicsContext2D();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                drawCell(gc, grid.get(r, c));
    }

    private void drawCell(GraphicsContext gc, Node node) {
        double x = node.getCol() * cell;
        double y = node.getRow() * cell;

        Color fill;
        if      (node.isWall())              fill = C_WALL;
        else if (node.isStart())             fill = C_START;
        else if (node.isGoal())              fill = C_GOAL;
        else if (pathSet.contains(node))     fill = C_PATH;
        else if (closedSet.contains(node))   fill = C_CLOSED;
        else if (openSet.contains(node))     fill = C_OPEN;
        else                                 fill = terrainColor(node.getTerrain());

        gc.setFill(fill);
        gc.fillRect(x, y, cell, cell);
        gc.setStroke(C_GRID);
        gc.setLineWidth(0.5);
        gc.strokeRect(x, y, cell, cell);

        if (node.isStart()) drawLabel(gc, x, y, "S");
        if (node.isGoal())  drawLabel(gc, x, y, "G");
    }

    private Color terrainColor(Node.Terrain t) {
        return switch (t) {
            case ROAD  -> C_ROAD;
            case GRASS -> C_GRASS;
            case WATER -> C_WATER;
        };
    }

    private void drawLabel(GraphicsContext gc, double x, double y, String lbl) {
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(Math.max(8, cell * 0.7)));
        gc.fillText(lbl, x + cell * 0.2, y + cell * 0.75);
    }

    private void setupMouse() {
        setOnMousePressed(e -> {
            lastDragRow  = -1;
            lastDragCol  = -1;
            dragPainting = null;
            currentDragTarget = DragTarget.NONE;
            handle(e.getX(), e.getY(), true);
        });
        setOnMouseDragged(e -> handle(e.getX(), e.getY(), false));
        setOnMouseReleased(e -> {
            lastDragRow = -1;
            lastDragCol = -1;
            dragPainting = null;
            currentDragTarget = DragTarget.NONE;
        });
    }

    private void handle(double px, double py, boolean isPress) {
        if (grid == null) return;
        int c = (int)(px / cell), r = (int)(py / cell);
        if (r < 0 || r >= rows || c < 0 || c >= cols) return;
        Node n = grid.get(r, c);

        if (isPress) {
            if (n.isStart()) {
                currentDragTarget = DragTarget.START;
            } else if (n.isGoal()) {
                currentDragTarget = DragTarget.GOAL;
            } else {
                currentDragTarget = DragTarget.PAINTING;
                if (editMode == EditMode.WALL) {
                    dragPainting = !n.isWall();
                }
            }
        }

        if (currentDragTarget == DragTarget.START) {
            if (!n.isWall() && !n.isGoal()) {
                grid.setStart(r, c);
                clearOverlay();
            }
        } else if (currentDragTarget == DragTarget.GOAL) {
            if (!n.isWall() && !n.isStart()) {
                grid.setGoal(r, c);
                clearOverlay();
            }
        } else if (currentDragTarget == DragTarget.PAINTING) {
            switch (editMode) {
                case WALL -> {
                    if (n.isStart() || n.isGoal()) return;
                    if (r == lastDragRow && c == lastDragCol) return;
                    lastDragRow = r; lastDragCol = c;
                    if (dragPainting != null) {
                        if ( dragPainting && !n.isWall()) { grid.toggleWall(r, c); clearOverlay(); }
                        if (!dragPainting &&  n.isWall()) { grid.toggleWall(r, c); clearOverlay(); }
                    }
                }
                case ROAD, GRASS, WATER -> {
                    if (n.isWall() || n.isStart() || n.isGoal()) return;
                    if (r == lastDragRow && c == lastDragCol) return;
                    lastDragRow = r; lastDragCol = c;
                    Node.Terrain t = switch (editMode) {
                        case GRASS -> Node.Terrain.GRASS;
                        case WATER -> Node.Terrain.WATER;
                        default    -> Node.Terrain.ROAD;
                    };
                    grid.setTerrain(r, c, t);
                    clearOverlay();
                }
            }
        }

        redraw();
        if (onChanged != null) onChanged.run();
    }
}