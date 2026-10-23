package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

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
        Set<Node>   visited  = new HashSet<>();
        List<SearchStep> steps = new ArrayList<>();

        stack.push(startNode);
        startNode.setGCost(0);
        // Nota: Il nodo iniziale viene considerato OPEN quando entra nello stack
        steps.add(new SearchStep(startNode, NodeState.OPEN));

        int expanded = 0;

        while (!stack.isEmpty()) {
            Node current = stack.pop();

            if (visited.contains(current)) continue;
            visited.add(current);
            steps.add(new SearchStep(current, NodeState.CLOSED));
            expanded++;

            if (current.equals(goalNode)) {
                List<Node> path = reconstructPath(current);
                long ms = System.currentTimeMillis() - start;
                return new SearchResult(getName(), path, steps,
                        path.size() - 1, expanded, ms, false);
            }

            for (Node neighbor : grid.getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    neighbor.setGCost(current.getGCost() + neighbor.getCost());
                    neighbor.setParent(current);
                    stack.push(neighbor);
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