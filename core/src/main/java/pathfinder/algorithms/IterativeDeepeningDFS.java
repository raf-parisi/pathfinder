package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IterativeDeepeningDFS extends SearchAlgorithm {

    private int totalNodesExpanded;
    private List<Node> visitedOrder;

    @Override
    public String getName() {
        return "IDDFS (Iterative Deepening DFS)";
    }

    @Override
    public SearchResult search(Grid grid) {
        long startTime = System.currentTimeMillis();
        Node start = grid.getStartNode();
        Node goal = grid.getGoalNode();

        if (start == null || goal == null) {
            return noPath(List.of(), 0, System.currentTimeMillis() - startTime);
        }

        totalNodesExpanded = 0;
        visitedOrder = new ArrayList<>();
        int maxDepthLimit = grid.getRows() * grid.getCols();

        for (int depth = 0; depth <= maxDepthLimit; depth++) {
            Set<Node> visitedInPath = new HashSet<>();
            List<Node> path = new ArrayList<>();

            if (dls(start, goal, depth, visitedInPath, path, grid)) {
                long duration = System.currentTimeMillis() - startTime;
                double totalCost = 0;
                for (int i = 0; i < path.size() - 1; i++) {
                    totalCost += path.get(i + 1).getCost();
                }
                return new SearchResult(getName(), path, visitedOrder, totalCost, totalNodesExpanded, duration, true);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return noPath(visitedOrder, totalNodesExpanded, duration);
    }

    private boolean dls(Node current, Node goal, int limit, Set<Node> visited, List<Node> path, Grid grid) {
        totalNodesExpanded++;
        visitedOrder.add(current);
        path.add(current);

        if (current.equals(goal)) {
            return true;
        }

        if (limit <= 0) {
            path.remove(path.size() - 1);
            return false;
        }

        visited.add(current);

        for (Node neighbor : grid.getNeighbors(current)) {
            if (!neighbor.isWall() && !visited.contains(neighbor)) {
                if (dls(neighbor, goal, limit - 1, visited, path, grid)) {
                    return true;
                }
            }
        }

        visited.remove(current);
        path.remove(path.size() - 1);
        return false;
    }
}