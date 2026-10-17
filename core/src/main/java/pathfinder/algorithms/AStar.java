package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.*;
import java.util.function.BiFunction;

public class AStar extends SearchAlgorithm {

    private final String name;
    private final BiFunction<Node, Node, Double> heuristic;

    /**
     * @param name       nome visualizzato
     * @param heuristic  funzione (nodo, goal) → stima euristica
     */
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
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0);

        // PQ ordinata per f(n) = g(n) + h(n)
        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(Node::getFCost)
                          .thenComparingDouble(n -> -n.getGCost())
                          .thenComparingInt(Node::hashCode));
        Set<Node>  closed  = new HashSet<>();
        List<Node> visited = new ArrayList<>();

        startNode.setGCost(0);
        startNode.setHCost(heuristic.apply(startNode, goalNode));
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
                    neighbor.setHCost(heuristic.apply(neighbor, goalNode));
                    neighbor.setParent(current);
                    open.add(neighbor);
                }
            }
        }

        return noPath(visited, expanded, System.currentTimeMillis() - start);
    }
}
