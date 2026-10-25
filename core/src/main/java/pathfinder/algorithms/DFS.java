package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;

public class DFS extends SearchAlgorithm {

    @Override
    public String getName() { return "DFS"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long startNano = System.nanoTime();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0, 0.0);

        Deque<Node> stack    = new ArrayDeque<>();
        Set<Node>   visited  = new HashSet<>();
        List<SearchStep> steps = new ArrayList<>();

        stack.push(startNode);
        startNode.setGCost(0);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;
        int maxNodesInMemory = 0;

        while (!stack.isEmpty()) {
            int currentMemory = stack.size() + visited.size();
            if (currentMemory > maxNodesInMemory) {
                maxNodesInMemory = currentMemory;
            }

            Node current = stack.pop();

            if (visited.contains(current)) continue;
            visited.add(current);
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
                return new SearchResult(getName(), path, steps,
                        current.getGCost(), expanded, maxNodesInMemory, durationMs);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    neighbor.setGCost(current.getGCost() + neighbor.getCost());
                    neighbor.setParent(current);
                    stack.push(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return noPath(steps, expanded, maxNodesInMemory, durationMs);
    }

    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, double durationMs) {
        return new SearchResult(getName(), List.of(), steps,
                Double.MAX_VALUE, expanded, maxMemory, durationMs);
    }
}