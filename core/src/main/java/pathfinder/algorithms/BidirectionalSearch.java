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
        long startNano = System.nanoTime();
        Node start = grid.getStartNode();
        Node goal = grid.getGoalNode();

        if (start == null || goal == null) {
            return noPath(List.of(), 0, 0, 0.0);
        }

        if (start.equals(goal)) {
            List<Node> path = List.of(start);
            List<SearchStep> singleStep = List.of(new SearchStep(start, NodeState.CLOSED));
            double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;
            return new SearchResult(getName(), path, singleStep, 0.0, 1, 1, durationMs);
        }

        Map<Node, Double> forwardCost = new HashMap<>();
        Map<Node, Double> backwardCost = new HashMap<>();

        PriorityQueue<Node> forwardQueue = new PriorityQueue<>(
                Comparator.comparingDouble((Node n) -> forwardCost.getOrDefault(n, Double.MAX_VALUE))
                        .thenComparingInt(Node::hashCode));
        PriorityQueue<Node> backwardQueue = new PriorityQueue<>(
                Comparator.comparingDouble((Node n) -> backwardCost.getOrDefault(n, Double.MAX_VALUE))
                        .thenComparingInt(Node::hashCode));

        Map<Node, Node> forwardParent = new HashMap<>();
        Map<Node, Node> backwardParent = new HashMap<>();

        Set<Node> forwardClosed = new HashSet<>();
        Set<Node> backwardClosed = new HashSet<>();

        List<SearchStep> steps = new ArrayList<>();

        forwardCost.put(start, 0.0);
        forwardQueue.add(start);
        forwardParent.put(start, null);
        steps.add(new SearchStep(start, NodeState.OPEN));

        backwardCost.put(goal, 0.0);
        backwardQueue.add(goal);
        backwardParent.put(goal, null);
        steps.add(new SearchStep(goal, NodeState.OPEN));

        Node meetingNode = null;
        int nodesExpanded = 0;
        int maxNodesInMemory = 0;

        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            int currentMemory = forwardQueue.size() + backwardQueue.size()
                    + forwardClosed.size() + backwardClosed.size();
            if (currentMemory > maxNodesInMemory) {
                maxNodesInMemory = currentMemory;
            }


            Node currentForward = forwardQueue.poll();
            if (forwardClosed.contains(currentForward)) continue;
            forwardClosed.add(currentForward);
            steps.add(new SearchStep(currentForward, NodeState.CLOSED));
            nodesExpanded++;

            if (backwardClosed.contains(currentForward) || backwardCost.containsKey(currentForward)) {
                meetingNode = currentForward;
                break;
            }

            for (Node neighbor : grid.getNeighbors(currentForward)) {
                if (neighbor.isWall()) continue;
                double tentativeCost = forwardCost.get(currentForward) + neighbor.getCost();
                if (tentativeCost < forwardCost.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    forwardCost.put(neighbor, tentativeCost);
                    forwardParent.put(neighbor, currentForward);
                    forwardQueue.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }


            Node currentBackward = backwardQueue.poll();
            if (backwardClosed.contains(currentBackward)) continue;
            backwardClosed.add(currentBackward);
            steps.add(new SearchStep(currentBackward, NodeState.CLOSED));
            nodesExpanded++;

            if (forwardClosed.contains(currentBackward) || forwardCost.containsKey(currentBackward)) {
                meetingNode = currentBackward;
                break;
            }

            for (Node neighbor : grid.getNeighbors(currentBackward)) {
                if (neighbor.isWall()) continue;
                double tentativeCost = backwardCost.get(currentBackward) + neighbor.getCost();
                if (tentativeCost < backwardCost.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    backwardCost.put(neighbor, tentativeCost);
                    backwardParent.put(neighbor, currentBackward);
                    backwardQueue.add(neighbor);
                    steps.add(new SearchStep(neighbor, NodeState.OPEN));
                }
            }
        }

        double durationMs = (System.nanoTime() - startNano) / 1_000_000.0;

        if (meetingNode == null) {
            return noPath(steps, nodesExpanded, maxNodesInMemory, durationMs);
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

        return new SearchResult(getName(), fullPath, steps, totalCost, nodesExpanded, maxNodesInMemory, durationMs);
    }

    protected SearchResult noPath(List<SearchStep> steps, int expanded, int maxMemory, double durationMs) {
        return new SearchResult(getName(), List.of(), steps, Double.MAX_VALUE, expanded, maxMemory, durationMs);
    }
}