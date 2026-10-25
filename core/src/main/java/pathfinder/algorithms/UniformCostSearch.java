package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;

public class UniformCostSearch extends SearchAlgorithm {


    private record QueueEntry(Node node, double gCost) implements Comparable<QueueEntry> {
        @Override
        public int compareTo(QueueEntry o) {
            int cmp = Double.compare(this.gCost, o.gCost);
            if (cmp != 0) return cmp;
            return Integer.compare(this.node.hashCode(), o.node.hashCode());
        }
    }

    @Override
    public String getName() { return "UCS"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long startNano = System.nanoTime();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0, 0.0);

        PriorityQueue<QueueEntry> open = new PriorityQueue<>();
        Map<Node, Double> bestGCost = new HashMap<>();
        Set<Node> closed = new HashSet<>();
        List<SearchStep> steps = new ArrayList<>();

        startNode.setGCost(0);
        open.add(new QueueEntry(startNode, 0.0));
        bestGCost.put(startNode, 0.0);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;
        int maxMemory = 0;

        while (!open.isEmpty()) {
            int currentMemory = open.size() + closed.size();
            if (currentMemory > maxMemory) {
                maxMemory = currentMemory;
            }

            QueueEntry entry = open.poll();
            Node current = entry.node();


            if (entry.gCost() > bestGCost.getOrDefault(current, Double.MAX_VALUE)) continue;
            if (closed.contains(current)) continue;

            closed.add(current);
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
                return new SearchResult(getName(), path, steps,
                        current.getGCost(), expanded, maxMemory, durationMs);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (closed.contains(neighbor)) continue;

                double tentative = current.getGCost() + neighbor.getCost();
                if (tentative < bestGCost.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    bestGCost.put(neighbor, tentative);
                    neighbor.setGCost(tentative);
                    neighbor.setParent(current);

                    open.add(new QueueEntry(neighbor, tentative));
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return noPath(steps, expanded, maxMemory, durationMs);
    }

    @Override
    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, double durationMs) {
        return new SearchResult(getName(), List.of(), steps, Double.MAX_VALUE, expanded, maxMemory, durationMs);
    }
}