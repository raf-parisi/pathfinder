package pathfinder.model;

import java.util.Objects;

/**
 * Rappresenta una singola cella della griglia.
 * Ogni nodo ha un Type (FREE/WALL/START/GOAL) che ne descrive il ruolo strutturale(es. FREE è calpestabile)
 * e un terreno che determina il costo di attraversamento.
 */
public class Node {

    public enum Type {
        FREE, WALL, START, GOAL
    }

    public enum Terrain {
        ROAD(1), GRASS(3), WATER(8);

        public final int cost;
        Terrain(int cost) { this.cost = cost; }
    }

    // coordinate che identificano il nodo all'interno della griglia
    private final int row;
    private final int col;

    // default
    private Type    type    = Type.FREE;
    private Terrain terrain = Terrain.ROAD;

    // costo effettivo accumulato dal nodo START al nodo corrente
    private double gCost  = Double.MAX_VALUE;

    // stima euristica dal nodo corrente al GOAL
    private double hCost  = 0.0;

    // per ricostruire il cammino
    private Node   parent = null;

    public Node(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public Type    getType()              { return type; }
    public void    setType(Type type)     { this.type = type; }
    public boolean isWall()               { return type == Type.WALL;  }
    public boolean isStart()              { return type == Type.START; }
    public boolean isGoal()               { return type == Type.GOAL;  }

    public Terrain getTerrain()               { return terrain; }
    public void    setTerrain(Terrain t)      { this.terrain = t; }

    public int     getCost()                  { return terrain.cost; }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public double getGCost()            { return gCost; }
    public void   setGCost(double g)    { this.gCost = g; }
    public double getHCost()            { return hCost; }
    public void   setHCost(double h)    { this.hCost = h; }
    public double getFCost()            { return gCost + hCost; }
    public Node   getParent()           { return parent; }
    public void   setParent(Node p)     { this.parent = p; }

    public void resetSearch() {
        gCost  = Double.MAX_VALUE;
        hCost  = 0.0;
        parent = null;
    }


    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node n)) return false;
        return row == n.row && col == n.col;
    }
    @Override public int hashCode() { return Objects.hash(row, col); }
    @Override public String toString() {
        return "Node(" + row + "," + col + ")[" + type + "/" + terrain + "]";
    }
}
