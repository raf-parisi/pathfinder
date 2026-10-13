package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.*;

public class GreedyBestFirst extends SearchAlgorithm {

    @Override
    public String getName() { return "Greedy Best-First"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0);

        // Ordina per h(n)
        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::getHCost));
        Set<Node>  closed  = new HashSet<>();
        List<Node> visited = new ArrayList<>();

        startNode.setGCost(0);
        startNode.setHCost(manhattan(startNode, goalNode));
        open.add(startNode);
        int expanded = 0;

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (closed.contains(current)) continue;
            closed.add(current);
            visited.add(current);
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                long ms = System.currentTimeMillis() - start;
                return new SearchResult(getName(), path, visited,
                        current.getGCost(), expanded, ms, false);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (closed.contains(neighbor)) continue;
                double tentG = current.getGCost() + neighbor.getCost();
                if (tentG < neighbor.getGCost()) {
                    neighbor.setGCost(tentG);
                    neighbor.setHCost(manhattan(neighbor, goalNode));
                    neighbor.setParent(current);
                    open.add(neighbor);
                }
            }
        }

        return noPath(visited, expanded, System.currentTimeMillis() - start);
    }
}
