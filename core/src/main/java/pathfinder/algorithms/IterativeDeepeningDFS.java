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

    private int totalNodesExpanded;
    private int maxNodesInMemory; // Campo per tracciare il picco di memoria
    private List<SearchStep> steps;

    @Override
    public String getName() {
        return "IDDFS (Iterative Deepening DFS)";
    }

    public int getTotalNodesExpanded() {
        return totalNodesExpanded;
    }

    public int getMaxNodesInMemory() {
        return maxNodesInMemory;
    }

    @Override
    public SearchResult search(Grid grid) {
        long startTime = System.currentTimeMillis();
        Node start = grid.getStartNode();
        Node goal = grid.getGoalNode();

        if (start == null || goal == null) {
            return noPath(List.of(), 0, 0, System.currentTimeMillis() - startTime);
        }

        totalNodesExpanded = 0;
        maxNodesInMemory = 0;
        steps = new ArrayList<>();
        int maxDepthLimit = grid.getRows() * grid.getCols();

        steps.add(new SearchStep(start, NodeState.OPEN));

        for (int depth = 0; depth <= maxDepthLimit; depth++) {
            Set<Node> visitedInPath = new HashSet<>();
            List<Node> path = new ArrayList<>();

            if (dls(start, goal, depth, visitedInPath, path, grid)) {
                long duration = System.currentTimeMillis() - startTime;
                double totalCost = 0;
                for (int i = 0; i < path.size() - 1; i++) {
                    totalCost += path.get(i + 1).getCost();
                }
                return new SearchResult(getName(), path, steps, totalCost, totalNodesExpanded, maxNodesInMemory, duration, true);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        return noPath(steps, totalNodesExpanded, maxNodesInMemory, duration);
    }

    private boolean dls(Node current, Node goal, int limit, Set<Node> visited, List<Node> path, Grid grid) {
        totalNodesExpanded++;
        steps.add(new SearchStep(current, NodeState.CLOSED));
        path.add(current);

        int currentMem = path.size() + visited.size();
        if (currentMem > maxNodesInMemory) {
            maxNodesInMemory = currentMem;
        }

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
                steps.add(new SearchStep(neighbor, NodeState.OPEN));
                if (dls(neighbor, goal, limit - 1, visited, path, grid)) {
                    return true;
                }
            }
        }

        visited.remove(current);
        path.remove(path.size() - 1);
        return false;
    }

    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, long ms) {
        return new SearchResult(getName(), List.of(), steps, Double.MAX_VALUE, expanded, maxMemory, ms, false);
    }
}