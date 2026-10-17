package pathfinder.algorithms;

import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

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
            return new SearchResult(getName(), path, path, 0.0, 1, System.currentTimeMillis() - startTime, true);
        }

        // Frontiere FIFO (o code) per le due direzioni
        Queue<Node> forwardQueue = new LinkedList<>();
        Queue<Node> backwardQueue = new LinkedList<>();

        // Mappe dei genitori per ricostruire il percorso
        Map<Node, Node> forwardParent = new HashMap<>();
        Map<Node, Node> backwardParent = new HashMap<>();

        // Insiemi dei nodi visitati da entrambe le direzioni
        Set<Node> forwardVisited = new HashSet<>();
        Set<Node> backwardVisited = new HashSet<>();

        forwardQueue.add(start);
        forwardVisited.add(start);
        forwardParent.put(start, null);

        backwardQueue.add(goal);
        backwardVisited.add(goal);
        backwardParent.put(goal, null);

        Node meetingNode = null;
        List<Node> visitedOrder = new ArrayList<>();
        int nodesExpanded = 0;

        while (!forwardQueue.isEmpty() && !backwardQueue.isEmpty()) {
            // --- Espansione Forward ---
            Node currentForward = forwardQueue.poll();
            visitedOrder.add(currentForward);
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
                }
            }

            // --- Espansione Backward ---
            Node currentBackward = backwardQueue.poll();
            visitedOrder.add(currentBackward);
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
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        if (meetingNode == null) {
            return noPath(visitedOrder, nodesExpanded, duration);
        }

        // Ricostruzione del percorso unendo le due metà attraverso il meetingNode
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

        // Calcolo del costo totale basato sui pesi dei nodi/terreno o passi
        double totalCost = 0;
        for (int i = 0; i < fullPath.size() - 1; i++) {
            totalCost += fullPath.get(i + 1).getCost();
        }

        return new SearchResult(getName(), fullPath, visitedOrder, totalCost, nodesExpanded, duration, true);
    }
}