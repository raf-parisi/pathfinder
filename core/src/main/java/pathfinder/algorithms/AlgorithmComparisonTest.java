package pathfinder.algorithms;

import pathfinder.ml.PerlinGridGenerator;
import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class AlgorithmComparisonTest {

    private static final int NUM_GRIDS = 50;
    private static final int PAIRS_PER_GRID = 5;
    private static final long TIMEOUT_MS = 200;
    private static final long SEED = 2026L;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        System.out.println("=== BENCHMARK COMPARATIVO ALGORITMI DI SEARCH ===");
        System.out.println("Configurazione: " + NUM_GRIDS + " griglie, " + PAIRS_PER_GRID + " coppie/griglia, Timeout IDDFS/IDA*: " + TIMEOUT_MS + "ms\n");

        int[][] dimensions = {{10, 10}, {20, 20}, {50, 80}};

        for (int[] dim : dimensions) {
            int rows = dim[0];
            int cols = dim[1];

            System.out.println("=========================================================================================");
            System.out.printf("   DIMENSIONE GRIGLIA: %dx%d%n", rows, cols);
            System.out.println("=========================================================================================");

            runBenchmarkScenario(rows, cols, "SCENARIO A: Solo Strada", false, 0.0, false);
            runBenchmarkScenario(rows, cols, "SCENARIO B: Strada + Muri", false, 0.20, true);
            runBenchmarkScenario(rows, cols, "SCENARIO C: Terreni Variabili", true, 0.10, false);
        }

        executor.shutdown();
    }

    private static void runBenchmarkScenario(int rows, int cols, String scenarioTitle, boolean usePerlinFull, double density, boolean usePerlinWalls) {
        System.out.printf("%n--- %s ---%n%n", scenarioTitle);

        List<AlgorithmMetrics> metricsList = new ArrayList<>();
        metricsList.add(new AlgorithmMetrics("UCS"));
        metricsList.add(new AlgorithmMetrics("A* Manh."));
        metricsList.add(new AlgorithmMetrics("A* Eucl."));
        metricsList.add(new AlgorithmMetrics("IDA*"));
        metricsList.add(new AlgorithmMetrics("IDDFS"));
        metricsList.add(new AlgorithmMetrics("Greedy BF"));
        metricsList.add(new AlgorithmMetrics("Hill Climb"));
        metricsList.add(new AlgorithmMetrics("BFS"));
        metricsList.add(new AlgorithmMetrics("DFS"));
        metricsList.add(new AlgorithmMetrics("Bidirectional"));

        Random seedRnd = new Random(SEED + rows * 31L + cols);
        PerlinGridGenerator perlinGen = new PerlinGridGenerator(0.1, 0.3, 0.0, density);

        int totalPairs = 0;

        for (int g = 0; g < NUM_GRIDS; g++) {
            long currentSeed = seedRnd.nextLong();
            Grid grid;

            if (usePerlinFull) {
                grid = perlinGen.generate(rows, cols, currentSeed);
            } else if (usePerlinWalls) {
                grid = generatePerlinWallGrid(rows, cols, density, currentSeed);
            } else {
                grid = new Grid(rows, cols);
            }

            List<int[]> free = new ArrayList<>();
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    if (!grid.get(r, c).isWall()) free.add(new int[]{r, c});

            if (free.size() < 2) continue;

            for (int p = 0; p < PAIRS_PER_GRID; p++) {
                int si = seedRnd.nextInt(free.size());
                int gi = seedRnd.nextInt(free.size());
                if (si == gi) continue;

                grid.setStart(free.get(si)[0], free.get(si)[1]);
                grid.setGoal(free.get(gi)[0], free.get(gi)[1]);

                Grid gUcs = copyGrid(grid);
                SearchResult rUcs = new UniformCostSearch().search(gUcs);
                if (!rUcs.isPathFound()) continue;

                double optimalCost = rUcs.getTotalCost();
                totalPairs++;

                getMetrics("UCS", metricsList).recordResult(rUcs, optimalCost);

                for (AlgorithmMetrics m : metricsList) {
                    if (m.name.equals("UCS")) continue;

                    SearchAlgorithm algo = instantiateAlgorithm(m.name);
                    if (algo == null) continue;

                    Grid copy = copyGrid(grid);

                    if (m.name.equals("IDDFS") || m.name.equals("IDA*")) {
                        runWithTimeoutAndRecord(algo, copy, TIMEOUT_MS, m, optimalCost);
                    } else {
                        SearchResult res = algo.search(copy);
                        m.recordResult(res, optimalCost);
                    }
                }
            }
        }

        if (totalPairs == 0) {
            System.out.println("Nessuna coppia valida testata.");
            return;
        }

        System.out.printf("%-14s %12s %12s %9s %10s %12s %14s%n",
                "Algoritmo", "Nodi espansi", "Max Mem.", "Tempo", "%Ottim.", "Overhead", "Fail/Timeout");
        System.out.println("-".repeat(90));

        for (AlgorithmMetrics m : metricsList) {
            m.printRow(totalPairs);
        }
    }

    private static Grid generatePerlinWallGrid(int rows, int cols, double wallDensity, long seed) {
        PerlinGridGenerator generator = new PerlinGridGenerator(0.1, 1.0, 1.0, wallDensity);
        return generator.generate(rows, cols, seed);
    }

    private static void runWithTimeoutAndRecord(SearchAlgorithm algo, Grid grid, long timeoutMs, AlgorithmMetrics metrics, double optimalCost) {
        Future<SearchResult> future = executor.submit(() -> algo.search(grid));
        try {
            SearchResult res = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            metrics.recordResult(res, optimalCost);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            future.cancel(true);
            long partialNodes = 0;
            long partialMaxMem = 0;
            if (algo instanceof IDAStar) {
                partialNodes = ((IDAStar) algo).getLastNodesExpanded();
            } else if (algo instanceof IterativeDeepeningDFS) {
                partialNodes = ((IterativeDeepeningDFS) algo).getTotalNodesExpanded();
                partialMaxMem = ((IterativeDeepeningDFS) algo).getMaxNodesInMemory();
            }
            metrics.recordTimeout(timeoutMs, partialNodes, partialMaxMem);
        }
    }

    private static SearchAlgorithm instantiateAlgorithm(String name) {
        return switch (name) {
            case "BFS" -> new BFS();
            case "DFS" -> new DFS();
            case "UCS" -> new UniformCostSearch();
            case "Greedy BF" -> new GreedyBestFirst();
            case "A* Manh." -> new AStar("A* (Manhattan)", (a, b) ->
                    (double)(Math.abs(a.getRow() - b.getRow()) + Math.abs(a.getCol() - b.getCol())));
            case "A* Eucl." -> new AStar("A* (Euclidea)", (a, b) ->
                    Math.hypot(a.getRow() - b.getRow(), a.getCol() - b.getCol()));
            case "Hill Climb" -> new HillClimbing();
            case "IDDFS" -> new IterativeDeepeningDFS();
            case "IDA*" -> new IDAStar();
            case "Bidirectional" -> new BidirectionalSearch();
            default -> null;
        };
    }

    private static AlgorithmMetrics getMetrics(String name, List<AlgorithmMetrics> list) {
        for (AlgorithmMetrics m : list) if (m.name.equals(name)) return m;
        return null;
    }

    private static Grid copyGrid(Grid original) {
        Grid copy = original.copyWalls();
        for (int r = 0; r < original.getRows(); r++) {
            for (int c = 0; c < original.getCols(); c++) {
                copy.setTerrain(r, c, original.get(r, c).getTerrain());
            }
        }
        Node s = original.getStartNode();
        Node g = original.getGoalNode();
        if (s != null) copy.setStart(s.getRow(), s.getCol());
        if (g != null) copy.setGoal(g.getRow(), g.getCol());
        return copy;
    }

    private static class AlgorithmMetrics {
        String name;
        long totalExpanded = 0;
        long totalMaxMemory = 0;
        long totalTimeMs = 0;
        int optimalCount = 0;
        int successCount = 0;
        double sumOverhead = 0;
        int timeoutsOrFailures = 0;

        AlgorithmMetrics(String name) {
            this.name = name;
        }

        void recordResult(SearchResult res, double optimalCost) {
            totalExpanded += res.getNodesExpanded();
            totalMaxMemory += res.getMaxNodesInMemory();
            totalTimeMs += res.getExecutionTimeMs();

            if (!res.isPathFound()) {
                timeoutsOrFailures++;
                return;
            }

            successCount++;

            boolean isOpt = Math.abs(res.getTotalCost() - optimalCost) < 1e-6;
            if (isOpt) {
                optimalCount++;
            }

            double overhead = ((res.getTotalCost() - optimalCost) / optimalCost) * 100.0;
            sumOverhead += overhead;
        }

        void recordTimeout(long timeoutMs, long partialNodes, long partialMaxMem) {
            timeoutsOrFailures++;
            totalExpanded += partialNodes;
            totalMaxMemory += partialMaxMem;
            totalTimeMs += timeoutMs;
        }

        void printRow(int totalPairs) {
            double avgExp = (double) totalExpanded / totalPairs;
            double avgMem = (double) totalMaxMemory / totalPairs;
            double avgTime = (double) totalTimeMs / totalPairs;
            double optPct = 100.0 * optimalCount / totalPairs;

            String ovhStr = successCount > 0 ? String.format("%.1f%%", sumOverhead / successCount) : "-";
            String failStr = String.format("%d/%d", timeoutsOrFailures, totalPairs);

            System.out.printf("%-14s %12.1f %12.1f %7.2fms %10.1f%% %12s %14s%n",
                    name, avgExp, avgMem, avgTime, optPct, ovhStr, failStr);
        }
    }
}