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
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0);

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::getHCost));
        Set<Node>        closed = new HashSet<>();
        List<SearchStep> steps  = new ArrayList<>();

        startNode.setGCost(0);
        startNode.setHCost(manhattan(startNode, goalNode));
        open.add(startNode);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (closed.contains(current)) continue;
            closed.add(current);
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                long ms = System.currentTimeMillis() - start;
                return new SearchResult(getName(), path, steps,
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
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        return noPath(steps, expanded, System.currentTimeMillis() - start);
    }
}