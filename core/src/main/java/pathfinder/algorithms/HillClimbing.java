package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;

public class HillClimbing extends SearchAlgorithm {

    @Override
    public String getName() { return "Hill Climbing"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long startNano = System.nanoTime();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0, 0.0);

        Node current = startNode;
        Set<Node> visited = new HashSet<>();
        List<SearchStep> steps = new ArrayList<>();

        current.setGCost(0);
        int expanded = 0;
        int maxNodesInMemory = 0;
        steps.add(new SearchStep(current, NodeState.OPEN));

        while (true) {
            int currentMemory = visited.size() + 1;
            if (currentMemory > maxNodesInMemory) {
                maxNodesInMemory = currentMemory;
            }

            visited.add(current);
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
                return new SearchResult(getName(), path, steps,
                        current.getGCost(), expanded, maxNodesInMemory, durationMs);
            }

            Node best = null;
            double bestH = Double.MAX_VALUE;

            for (Node neighbor : grid.getNeighbors(current)) {
                if (visited.contains(neighbor)) continue;
                steps.add(new SearchStep(neighbor, NodeState.OPEN));
                double h = manhattan(neighbor, goalNode);
                if (h < bestH) {
                    bestH = h;
                    best  = neighbor;
                }
            }

            if (best == null) {
                break;
            }

            best.setGCost(current.getGCost() + best.getCost());
            best.setParent(current);
            current = best;
        }

        double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return noPath(steps, expanded, maxNodesInMemory, durationMs);
    }

    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, double durationMs) {
        return new SearchResult(getName(), List.of(), steps,
                Double.MAX_VALUE, expanded, maxMemory, durationMs);
    }
}