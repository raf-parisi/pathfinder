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
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0, 0);

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::getFCost)
                        .thenComparingDouble(n -> -n.getGCost())
                        .thenComparingInt(Node::hashCode));
        Set<Node>        closed = new HashSet<>();
        List<SearchStep> steps  = new ArrayList<>();

        startNode.setGCost(0);
        startNode.setHCost(heuristic.apply(startNode, goalNode));
        open.add(startNode);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;
        int maxNodesInMemory = 0; // <--- TRACCIAMENTO PICCO MEMORIA

        while (!open.isEmpty()) {
            // Memoria corrente = Open + Closed
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
                long ms = System.currentTimeMillis() - start;
                boolean allRoad = path.stream().allMatch(n -> n.getTerrain() == Node.Terrain.ROAD);
                return new SearchResult(getName(), path, steps,
                        current.getGCost(), expanded, maxNodesInMemory, ms, allRoad);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (closed.contains(neighbor)) continue;
                double tentG = current.getGCost() + neighbor.getCost();
                if (tentG < neighbor.getGCost()) {
                    neighbor.setGCost(tentG);
                    neighbor.setHCost(heuristic.apply(neighbor, goalNode));
                    neighbor.setParent(current);
                    open.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        return noPath(steps, expanded, maxNodesInMemory, System.currentTimeMillis() - start);
    }

    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, long ms) {
        return new SearchResult(getName(), List.of(), steps,
                Double.MAX_VALUE, expanded, maxMemory, ms, false);
    }
}