package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.ArrayList;
import java.util.List;

public class IDAStar extends SearchAlgorithm {

    private static final int MAX_ITERATIONS = 1_000_000;
    private int lastNodesExpanded = 0;

    @Override
    public String getName() { return "IDA*"; }

    public int getLastNodesExpanded() {
        return lastNodesExpanded;
    }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0, 0);

        List<SearchStep> steps = new ArrayList<>();
        List<Node> path       = new ArrayList<>();
        int[]      expanded   = {0};
        int[]      maxMem     = {0}; // <--- TRACCIAMENTO PICCO MEMORIA

        double threshold = manhattan(startNode, goalNode);
        startNode.setGCost(0);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            path.clear();
            path.add(startNode);

            double result = dfs(startNode, goalNode, grid, path, steps,
                    expanded, maxMem, threshold, 0.0);

            lastNodesExpanded = expanded[0];

            if (result == -1.0) {
                List<Node> finalPath = new ArrayList<>(path);
                long ms = System.currentTimeMillis() - start;
                return new SearchResult(getName(), finalPath, steps,
                        finalPath.size() - 1, expanded[0], maxMem[0], ms, true);
            }

            if (result == Double.MAX_VALUE) break;

            threshold = result;
        }

        lastNodesExpanded = expanded[0];
        return noPath(steps, expanded[0], maxMem[0], System.currentTimeMillis() - start);
    }

    private double dfs(Node current, Node goal, Grid grid,
                       List<Node> path, List<SearchStep> steps,
                       int[] expanded, int[] maxMem, double threshold, double g) {

        // La memoria in IDA* corrisponde alla dimensione del percorso corrente (profondità stack)
        int currentMem = path.size();
        if (currentMem > maxMem[0]) {
            maxMem[0] = currentMem;
        }

        double f = g + manhattan(current, goal);
        if (f > threshold) return f;

        steps.add(new SearchStep(current, NodeState.CLOSED));
        expanded[0]++;
        lastNodesExpanded = expanded[0];

        if (current.equals(goal)) return -1.0;

        double min = Double.MAX_VALUE;
        for (Node neighbor : grid.getNeighbors(current)) {
            if (path.contains(neighbor)) continue;

            neighbor.setGCost(g + neighbor.getCost());
            neighbor.setParent(current);
            path.add(neighbor);
            steps.add(new SearchStep(neighbor, NodeState.OPEN));

            double result = dfs(neighbor, goal, grid, path, steps, expanded, maxMem, threshold, g + neighbor.getCost());

            if (result == -1.0) return -1.0;

            if (result < min) min = result;
            path.remove(path.size() - 1);
        }

        return min;
    }

    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, long ms) {
        return new SearchResult(getName(), List.of(), steps,
                Double.MAX_VALUE, expanded, maxMemory, ms, false);
    }
}