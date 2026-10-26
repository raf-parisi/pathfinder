package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;

public class GreedyBestFirst extends SearchAlgorithm {

    @Override
    public String getName() { return "Greedy Best-First"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long startNano = System.nanoTime();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0, 0.0);


        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::getHCost)
                        .thenComparingInt(Node::hashCode));
        Set<Node>        closed = new HashSet<>();
        List<SearchStep> steps  = new ArrayList<>();

        startNode.setGCost(0);
        startNode.setHCost(manhattan(startNode, goalNode));
        open.add(startNode);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;
        int maxNodesInMemory = 0;

        while (!open.isEmpty()) {
            int currentMemory = open.size() + closed.size();
            if (currentMemory > maxNodesInMemory) {
                maxNodesInMemory = currentMemory;
            }

            Node current = open.poll();
            if (closed.contains(current)) continue;
            closed.add(current);
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
                return new SearchResult(getName(), path, steps,
                        current.getGCost(), expanded, maxNodesInMemory, durationMs);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (closed.contains(neighbor)) continue;
                double tentG = current.getGCost() + neighbor.getCost();
                if (tentG < neighbor.getGCost()) {
                    neighbor.setGCost(tentG);
                    neighbor.setHCost(manhattan(neighbor, goalNode));
                    neighbor.setParent(current);
                    open.add(neighbor);
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