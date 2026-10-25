package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IterativeDeepeningDFS extends SearchAlgorithm {

    @Override
    public String getName() {
        return "IDDFS (Iterative Deepening DFS)";
    }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long startNano = System.nanoTime();

        Node start = grid.getStartNode();
        Node goal = grid.getGoalNode();

        if (start == null || goal == null) {
            return noPath(List.of(), 0, 0, 0.0);
        }

        int[] totalNodesExpanded = {0};
        int[] maxNodesInMemory = {0};
        List<SearchStep> steps = new ArrayList<>();
        int maxDepthLimit = grid.getRows() * grid.getCols();

        for (int limit = 0; limit <= maxDepthLimit; limit++) {
            Set<Node> visitedInPath = new HashSet<>();
            List<Node> currentPath = new ArrayList<>();

            if (dls(start, goal, limit, visitedInPath, currentPath, grid, steps, totalNodesExpanded, maxNodesInMemory)) {
                double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;

                double totalCost = 0;
                for (int i = 0; i < currentPath.size() - 1; i++) {
                    totalCost += currentPath.get(i + 1).getCost();
                }
                return new SearchResult(getName(), currentPath, steps, totalCost, totalNodesExpanded[0], maxNodesInMemory[0], durationMs);
            }
        }

        double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return noPath(steps, totalNodesExpanded[0], maxNodesInMemory[0], durationMs);
    }

    private boolean dls(Node current, Node goal, int limit,
                        Set<Node> visited, List<Node> path, Grid grid,
                        List<SearchStep> steps, int[] totalNodesExpanded, int[] maxNodesInMemory) {

        totalNodesExpanded[0]++;
        steps.add(new SearchStep(current, NodeState.CLOSED));
        path.add(current);
        visited.add(current);

        int currentMem = path.size();
        if (currentMem > maxNodesInMemory[0]) {
            maxNodesInMemory[0] = currentMem;
        }

        if (current.equals(goal)) {
            return true;
        }

        // limite raggiunto
        if (limit <= 0) {
            visited.remove(current);
            path.remove(path.size() - 1);
            return false;
        }

        // esplorazione vicini
        for (Node neighbor : grid.getNeighbors(current)) {
            if (!neighbor.isWall() && !visited.contains(neighbor)) {
                steps.add(new SearchStep(neighbor, NodeState.OPEN));
                if (dls(neighbor, goal, limit - 1, visited, path, grid, steps, totalNodesExpanded, maxNodesInMemory)) {
                    return true;
                }
            }
        }

        // backtracking
        visited.remove(current);
        path.remove(path.size() - 1);
        return false;
    }

    @Override
    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, double durationMs) {
        return new SearchResult(getName(), List.of(), steps, Double.MAX_VALUE, expanded, maxMemory, durationMs);
    }
}