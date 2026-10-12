package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SearchAlgorithm {

    // nome visualizzato nella UI
    public abstract String getName();

    // grid griglia su cui cercare
    public abstract SearchResult search(Grid grid);

    protected List<Node> reconstructPath(Node goal) {
        List<Node> path = new ArrayList<>();
        Node current = goal;
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }
        Collections.reverse(path);
        return path;
    }

    protected double manhattan(Node a, Node b) {
        return Math.abs(a.getRow() - b.getRow())
             + Math.abs(a.getCol() - b.getCol());
    }

    protected double euclidean(Node a, Node b) {
        double dr = a.getRow() - b.getRow();
        double dc = a.getCol() - b.getCol();
        return Math.sqrt(dr * dr + dc * dc);
    }

    protected SearchResult noPath(List<Node> visited, int expanded, long ms) {
        return new SearchResult(getName(), List.of(), visited,
                Double.MAX_VALUE, expanded, ms, false);
    }
}
