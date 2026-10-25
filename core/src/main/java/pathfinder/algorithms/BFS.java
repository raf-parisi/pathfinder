package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;

public class BFS extends SearchAlgorithm {

    @Override
    public String getName() { return "BFS"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long startNano = System.nanoTime();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0, 0.0);

        Queue<Node> frontier   = new ArrayDeque<>();
        Set<Node>   discovered = new HashSet<>();
        List<SearchStep> steps = new ArrayList<>();

        startNode.setGCost(0);
        frontier.add(startNode);
        discovered.add(startNode);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;
        int maxNodesInMemory = 0;

        while (!frontier.isEmpty()) {
            int currentMemory = frontier.size() + discovered.size();
            if (currentMemory > maxNodesInMemory) {
                maxNodesInMemory = currentMemory;
            }

            Node current = frontier.poll();
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
                return new SearchResult(getName(), path, steps,
                        current.getGCost(), expanded, maxNodesInMemory, durationMs);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (!discovered.contains(neighbor)) {
                    discovered.add(neighbor);
                    neighbor.setGCost(current.getGCost() + neighbor.getCost());
                    neighbor.setParent(current);
                    frontier.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return noPath(steps, expanded, maxNodesInMemory, durationMs);
    }
}