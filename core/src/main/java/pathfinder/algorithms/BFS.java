package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.*;

public class BFS extends SearchAlgorithm {

    @Override
    public String getName() { return "BFS"; }

    @Override
    public SearchResult search(Grid grid) {
        grid.resetSearchState();
        long start = System.currentTimeMillis();

        Node startNode = grid.getStartNode();
        Node goalNode  = grid.getGoalNode();
        if (startNode == null || goalNode == null) return noPath(List.of(), 0, 0);

        Queue<Node> frontier   = new ArrayDeque<>();
        Set<Node>   discovered = new HashSet<>();
        List<Node>  visited    = new ArrayList<>();

        startNode.setGCost(0);
        frontier.add(startNode);
        discovered.add(startNode);

        int expanded = 0;

        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            visited.add(current);
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                long ms = System.currentTimeMillis() - start;
                boolean allRoad = path.stream()
                    .allMatch(n -> n.getTerrain() == Node.Terrain.ROAD);
                return new SearchResult(getName(), path, visited,
                        current.getGCost(), expanded, ms, allRoad);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (!discovered.contains(neighbor)) {
                    discovered.add(neighbor);
                    neighbor.setGCost(current.getGCost() + neighbor.getCost());
                    neighbor.setParent(current);
                    frontier.add(neighbor);
                }
            }
        }

        return noPath(visited, expanded, System.currentTimeMillis() - start);
    }
}
