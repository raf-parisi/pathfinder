package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

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
        List<SearchStep> steps = new ArrayList<>();

        startNode.setGCost(0);
        frontier.add(startNode);
        discovered.add(startNode);
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;

        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                long ms = System.currentTimeMillis() - start;
                boolean allRoad = path.stream()
                        .allMatch(n -> n.getTerrain() == Node.Terrain.ROAD);
                return new SearchResult(getName(), path, steps,
                        current.getGCost(), expanded, ms, allRoad);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (!discovered.contains(neighbor)) {
                    discovered.add(neighbor);
                    neighbor.setGCost(current.getGCost() + neighbor.getCost());
                    neighbor.setParent(current);
                    frontier.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        return noPath(steps, expanded, System.currentTimeMillis() - start);
    }

    protected SearchResult noPath(List<SearchStep> steps, int expanded, long ms) {
        return new SearchResult(getName(), List.of(), steps,
                Double.MAX_VALUE, expanded, ms, false);
    }
}