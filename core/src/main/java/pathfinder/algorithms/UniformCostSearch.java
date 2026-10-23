package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;

public class UniformCostSearch extends SearchAlgorithm {

    @Override
    public String getName() { return "UCS"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0);

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::getGCost)
                        .thenComparingInt(Node::hashCode));
        Set<Node>        closed = new HashSet<>();
        List<SearchStep> steps  = new ArrayList<>();

        startNode.setGCost(0);
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
                        current.getGCost(), expanded, ms, true);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (closed.contains(neighbor)) continue;
                double tentative = current.getGCost() + neighbor.getCost();
                if (tentative < neighbor.getGCost()) {
                    neighbor.setGCost(tentative);
                    neighbor.setParent(current);
                    open.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        return noPath(steps, expanded, System.currentTimeMillis() - start);
    }
}