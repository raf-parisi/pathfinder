package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.*;

public class HillClimbing extends SearchAlgorithm {

    @Override
    public String getName() { return "Hill Climbing"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0);

        Node       current  = startNode;
        Set<Node>  visited  = new HashSet<>();
        List<Node> visitedList = new ArrayList<>();

        current.setGCost(0);
        int expanded = 0;

        while (true) {
            visited.add(current);
            visitedList.add(current);
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                long ms = System.currentTimeMillis() - start;
                return new SearchResult(getName(), path, visitedList,
                        current.getGCost(), expanded, ms, false);
            }

            // Trova il vicino non visitato con h minore
            Node best = null;
            double bestH = Double.MAX_VALUE;

            for (Node neighbor : grid.getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;
                double h = manhattan(neighbor, goalNode);
                if (h < bestH) {
                    bestH = h;
                    best  = neighbor;
                }
            }

            if (best == null) {
                // minimo locale
                break;
            }

            best.setGCost(current.getGCost() + best.getCost());
            best.setParent(current);
            current = best;
        }

        return noPath(visitedList, expanded, System.currentTimeMillis() - start);
    }
}
