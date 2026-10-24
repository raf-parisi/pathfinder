package pathfinder.gui.ui;

import javafx.concurrent.Task;
import pathfinder.algorithms.*;
import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.List;
import java.util.function.Consumer;

public class AlgorithmRunner {

    private List<SearchAlgorithm> algorithms;

    public AlgorithmRunner() {
        buildAlgorithms();
    }

    private void buildAlgorithms() {
        algorithms = List.of(
                new BFS(),
                new DFS(),
                new UniformCostSearch(),
                new GreedyBestFirst(),
                new AStar(),
                new AStar("A* (Euclidea)",
                        (a, b) -> {
                            double dr = a.getRow() - b.getRow();
                            double dc = a.getCol() - b.getCol();
                            return Math.sqrt(dr * dr + dc * dc);
                        }),
                new IDAStar(),
                new HillClimbing(),
                new BidirectionalSearch(),
                new IterativeDeepeningDFS()
        );
    }

    public List<String> getAlgorithmNames() {
        return algorithms.stream().map(SearchAlgorithm::getName).toList();
    }

    public void run(String algoName, Grid grid,
                    Consumer<SearchResult> onDone,
                    Consumer<Throwable>    onError) {

        SearchAlgorithm algo = findAlgorithm(algoName);
        Grid copy = copyGrid(grid);

        Task<SearchResult> task = new Task<>() {
            @Override protected SearchResult call() { return algo.search(copy); }
        };

        task.setOnSucceeded(e -> onDone.accept(task.getValue()));
        task.setOnFailed(e    -> onError.accept(task.getException()));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private SearchAlgorithm findAlgorithm(String name) {
        for (SearchAlgorithm a : algorithms)
            if (a.getName().equals(name)) return a;

        throw new IllegalArgumentException("Algoritmo non trovato: " + name);
    }

    private Grid copyGrid(Grid original) {
        Grid copy = original.copyWalls();
        Node s = original.getStartNode();
        Node g = original.getGoalNode();
        if (s != null) copy.setStart(s.getRow(), s.getCol());
        if (g != null) copy.setGoal(g.getRow(), g.getCol());
        return copy;
    }
}