package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.*;

public class DFS extends SearchAlgorithm {

    @Override
    public String getName() { return "DFS"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0);

        Deque<Node> stack    = new ArrayDeque<>();
        Set<Node>   visited  = new LinkedHashSet<>();  // ordine di visita
        List<Node>  visitedList = new ArrayList<>();

        stack.push(startNode);
        startNode.setGCost(0);
        int expanded = 0;

        while (!stack.isEmpty()) {
            Node current = stack.pop();

            if (visited.contains(current)) continue;
            visited.add(current);
            visitedList.add(current);
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                long ms = System.currentTimeMillis() - start;
                return new SearchResult(getName(), path, visitedList,
                        path.size() - 1, expanded, ms, false);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    neighbor.setGCost(current.getGCost() + neighbor.getCost());
                    neighbor.setParent(current);
                    stack.push(neighbor);
                }
            }
        }

        return noPath(visitedList, expanded, System.currentTimeMillis() - start);
    }
}
