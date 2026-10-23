package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.NodeState;
import pathfinder.model.SearchResult;
import pathfinder.model.SearchStep;

import java.util.*;

public class BidirectionalSearch extends SearchAlgorithm {

    @Override
    public String getName() {
        return "Bidirectional Search";
    }

    @Override
    public SearchResult search(Grid grid) {
        long startTime = System.currentTimeMillis();
        Node start = grid.getStartNode();
        Node goal = grid.getGoalNode();

        if (start == null || goal == null) {
            return noPath(List.of(), 0, System.currentTimeMillis() - startTime);
        }

        if (start.equals(goal)) {
            List<Node> path = List.of(start);
            List<SearchStep> singleStep = List.of(new SearchStep(start, NodeState.CLOSED));
            return new SearchResult(getName(), path, singleStep, 0.0, 1, System.currentTimeMillis() - startTime, true);
        }

        Queue<Node> forwardQueue = new LinkedList<>();
        Queue<Node> backwardQueue = new LinkedList<>();

        Map<Node, Node> forwardParent = new HashMap<>();
        Map<Node, Node> backwardParent = new HashMap<>();

        Set<Node> forwardVisited = new HashSet<>();
        Set<Node> backwardVisited = new HashSet<>();

        List<SearchStep> steps = new ArrayList<>();

        forwardQueue.add(start);
        forwardVisited.add(start);
        forwardParent.put(start, null);
        steps.add(new SearchStep(start, NodeState.OPEN));

        backwardQueue.add(goal);
        backwardVisited.add(goal);
        backwardParent.put(goal, null);
        steps.add(new SearchStep(goal, NodeState.OPEN));

        Node meetingNode = null;
        int nodesExpanded = 0;

        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // --- Espansione Forward ---
            Node currentForward = forwardQueue.poll();
            steps.add(new SearchStep(currentForward, NodeState.CLOSED));
            nodesExpanded++;

            if (backwardVisited.contains(currentForward)) {
                meetingNode = currentForward;
                break;
            }

            for (Node neighbor : grid.getNeighbors(currentForward)) {
                if (!neighbor.isWall() && !forwardVisited.contains(neighbor)) {
                    forwardVisited.add(neighbor);
                    forwardParent.put(neighbor, currentForward);
                    forwardQueue.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }

            // --- Espansione Backward ---
            Node currentBackward = backwardQueue.poll();
            steps.add(new SearchStep(currentBackward, NodeState.CLOSED));
            nodesExpanded++;

            if (forwardVisited.contains(currentBackward)) {
                meetingNode = currentBackward;
                break;
            }

            for (Node neighbor : grid.getNeighbors(currentBackward)) {
                if (!neighbor.isWall() && !backwardVisited.contains(neighbor)) {
                    backwardVisited.add(neighbor);
                    backwardParent.put(neighbor, currentBackward);
                    backwardQueue.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        if (meetingNode == null) {
            return noPath(steps, nodesExpanded, duration);
        }

        List<Node> pathFromStart = new ArrayList<>();
        Node curr = meetingNode;
        while (curr != null) {
            pathFromStart.add(curr);
            curr = forwardParent.get(curr);
        }
        Collections.reverse(pathFromStart);

        List<Node> pathToGoal = new ArrayList<>();
        curr = backwardParent.get(meetingNode);
        while (curr != null) {
            pathToGoal.add(curr);
            curr = backwardParent.get(curr);
        }

        List<Node> fullPath = new ArrayList<>(pathFromStart);
        fullPath.addAll(pathToGoal);

        double totalCost = 0;
        for (int i = 0; i < fullPath.size() - 1; i++) {
            totalCost += fullPath.get(i + 1).getCost();
        }

        return new SearchResult(getName(), fullPath, steps, totalCost, nodesExpanded, duration, true);
    }
}