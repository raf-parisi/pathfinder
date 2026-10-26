package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;
import java.util.function.BiFunction;

public class AStar extends SearchAlgorithm {

    private final String name;
    private final BiFunction<Node, Node, Double> heuristic;


    private record QueueEntry(Node node, double gCost, double fCost) implements Comparable<QueueEntry> {
        @Override
        public int compareTo(QueueEntry o) {
            int cmp = Double.compare(this.fCost, o.fCost);
            if (cmp != 0) return cmp;
            return Double.compare(o.gCost, this.gCost);
        }
    }

    public AStar(String name, BiFunction<Node, Node, Double> heuristic) {
        this.name      = name;
        this.heuristic = heuristic;
    }

    public AStar() {
        this("A* (Manhattan)", (a, b) ->
                (double)(Math.abs(a.getRow()-b.getRow()) + Math.abs(a.getCol()-b.getCol())));
    }

    @Override
    public String getName() { return name; }

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
        double startH = heuristic.apply(startNode, goalNode);
        startNode.setHCost(startH);

        open.add(new QueueEntry(startNode, 0.0, startH));
        bestGCost.put(startNode, 0.0);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;
        int maxNodesInMemory = 0;

        while (!open.isEmpty()) {
            int currentMemory = open.size() + closed.size();
            if (currentMemory > maxNodesInMemory) {
                maxNodesInMemory = currentMemory;
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
                        current.getGCost(), expanded, maxNodesInMemory, durationMs);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (closed.contains(neighbor)) continue;

                double tentG = current.getGCost() + neighbor.getCost();
                if (tentG < bestGCost.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    bestGCost.put(neighbor, tentG);
                    neighbor.setGCost(tentG);
                    double h = heuristic.apply(neighbor, goalNode);
                    neighbor.setHCost(h);
                    neighbor.setParent(current);

                    open.add(new QueueEntry(neighbor, tentG, tentG + h));
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