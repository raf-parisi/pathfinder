package pathfinder.algorithms;

import pathfinder.ml.PerlinGridGenerator;
import pathfinder.model.Grid;
import pathfinder.model.Node;
import pathfinder.model.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AlgorithmComparisonTest {

    private static final int NUM_GRIDS = 50;
    private static final int PAIRS_PER_GRID = 5;
    private static final long TIMEOUT_MS = 200;
    private static final long SEED = 2026L;
    private static final int NUM_WARMUP_GRIDS = 10;
    private static final long WARMUP_SEED = 7L;

    public static void main(String[] args) {
        System.out.println("=== BENCHMARK COMPARATIVO ALGORITMI DI SEARCH ===");
        System.out.println("Configurazione: " + NUM_GRIDS + " griglie, " + PAIRS_PER_GRID + " coppie/griglia, Timeout IDDFS/IDA*: " + TIMEOUT_MS + "ms");
        System.out.println();
        System.out.println("Legenda:");
        System.out.println("  Nodi espansi, Max Mem., Tempo : medie calcolate solo sulle istanze risolte dal singolo algoritmo");
        System.out.println("  Successo     : % di istanze in cui l'algoritmo trova un percorso (entro il timeout, dove previsto)");
        System.out.println("  Ottimi/Succ. : % di soluzioni a costo ottimo tra quelle trovate");
        System.out.println("  Overhead     : extra-costo medio rispetto all'ottimo, solo sulle soluzioni subottime");
        System.out.println("  Max Mem.     : picco di nodi tenuti in memoria contemporaneamente (esclusa la lista degli step)");
        System.out.println();

        int[][] dimensions = {{10, 10}, {20, 20}, {50, 80}};

        System.out.println("Warm-up JIT in corso (risultati scartati)...");
        runAllScenarios(dimensions, true);
        System.gc();
        System.out.println("Warm-up completato.\n");

        runAllScenarios(dimensions, false);
    }

    private static void runAllScenarios(int[][] dimensions, boolean warmup) {
        for (int[] dim : dimensions) {
            int rows = dim[0];
            int cols = dim[1];

            if (!warmup) {
                System.out.println("=========================================================================================================");
                System.out.printf("   DIMENSIONE GRIGLIA: %dx%d%n", rows, cols);
                System.out.println("=========================================================================================================");
            }

            runBenchmarkScenario(rows, cols, "SCENARIO A: Solo Strada", false, 0.0, false, warmup);
            runBenchmarkScenario(rows, cols, "SCENARIO B: Strada + Muri Fitti (40%)", false, 0.40, true, warmup);
            runBenchmarkScenario(rows, cols, "SCENARIO C: Terreni Variabili + Muri Fitti (40%)", true, 0.40, false, warmup);
        }
    }

    private static void runBenchmarkScenario(int rows, int cols, String scenarioTitle,
                                             boolean usePerlinFull, double density, boolean usePerlinWalls,
                                             boolean warmup) {
        if (!warmup) System.out.printf("%n--- %s ---%n%n", scenarioTitle);

        int numGrids = warmup ? NUM_WARMUP_GRIDS : NUM_GRIDS;
        long baseSeed = warmup ? WARMUP_SEED : SEED;

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

        Random seedRnd = new Random(baseSeed + rows * 31L + cols);
        PerlinGridGenerator perlinGen = new PerlinGridGenerator(0.1, 0.3, 0.0, density);

        int totalPairs = 0;

        for (int g = 0; g < numGrids; g++) {
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

                    if (m.name.equals("IDDFS") || m.name.equals("IDA*")) {
                        runWithTimeoutAndRecord(algo, grid, TIMEOUT_MS, m, optimalCost);
                    } else {
                        SearchResult res = algo.search(copyGrid(grid));
                        m.recordResult(res, optimalCost);
                    }
                }
            }
        }

        if (warmup) return;

        if (totalPairs == 0) {
            System.out.println("Nessuna coppia valida testata.");
            return;
        }

        System.out.printf("%-14s %12s %12s %9s %9s %13s %10s%n",
                "Algoritmo", "Nodi espansi", "Max Mem.", "Tempo", "Successo", "Ottimi/Succ.", "Overhead");
        System.out.println("-".repeat(87));

        for (AlgorithmMetrics m : metricsList) {
            m.printRow(totalPairs);
        }

        System.out.println("(Nodi espansi, Max Mem. e Tempo: medie sulle sole istanze risolte da ciascun algoritmo)");
    }

    private static Grid generatePerlinWallGrid(int rows, int cols, double wallDensity, long seed) {
        PerlinGridGenerator generator = new PerlinGridGenerator(0.1, 1.0, 1.0, wallDensity);
        return generator.generate(rows, cols, seed);
    }

    private static void runWithTimeoutAndRecord(SearchAlgorithm algo, Grid original, long timeoutMs, AlgorithmMetrics metrics, double optimalCost) {
        Grid limited = new TimeLimitedGrid(original, timeoutMs);
        try {
            SearchResult res = algo.search(limited);
            metrics.recordResult(res, optimalCost);
        } catch (SearchTimeoutException e) {
            metrics.recordTimeout();
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

    // ── supporto al timeout cooperativo ──────────────────────────────────

    private static class SearchTimeoutException extends RuntimeException {
        SearchTimeoutException() { super("timeout", null, false, false); }
    }

    private static class TimeLimitedGrid extends Grid {
        private final long deadlineNs;
        private int calls = 0;

        TimeLimitedGrid(Grid source, long timeoutMs) {
            super(source.getRows(), source.getCols());

            for (int r = 0; r < source.getRows(); r++) {
                for (int c = 0; c < source.getCols(); c++) {
                    Node src = source.get(r, c);
                    Node dst = this.get(r, c);
                    if (src.isWall()) dst.setType(Node.Type.WALL);
                    else              dst.setTerrain(src.getTerrain());
                }
            }
            Node s = source.getStartNode();
            Node g = source.getGoalNode();
            if (s != null) setStart(s.getRow(), s.getCol());
            if (g != null) setGoal(g.getRow(), g.getCol());

            this.deadlineNs = System.nanoTime() + timeoutMs * 1_000_000L;
        }

        @Override
        public List<Node> getNeighbors(Node node) {
            if ((++calls & 63) == 0 && System.nanoTime() > deadlineNs) {
                throw new SearchTimeoutException();
            }
            return super.getNeighbors(node);
        }
    }

    private static class AlgorithmMetrics {
        String name;
        long totalExpandedSuccess = 0;
        long totalMaxMemorySuccess = 0;
        double totalTimeMsSuccess = 0.0;
        int optimalCount = 0;
        int successCount = 0;
        double sumOverhead = 0;
        int timeoutsOrFailures = 0;

        AlgorithmMetrics(String name) {
            this.name = name;
        }

        void recordResult(SearchResult res, double optimalCost) {
            if (!res.isPathFound()) {
                timeoutsOrFailures++;
                return;
            }

            successCount++;
            totalExpandedSuccess += res.getNodesExpanded();
            totalMaxMemorySuccess += res.getMaxNodesInMemory();
            totalTimeMsSuccess += res.getExecutionTimeMs();

            boolean isOpt = Math.abs(res.getTotalCost() - optimalCost) < 1e-6;
            if (isOpt) {
                optimalCount++;
            }

            double overhead = ((res.getTotalCost() - optimalCost) / optimalCost) * 100.0;
            sumOverhead += overhead;
        }

        void recordTimeout() {
            timeoutsOrFailures++;
        }

        void printRow(int totalPairs) {
            String avgExpStr = successCount > 0 ? String.format("%.1f", (double) totalExpandedSuccess / successCount) : "-";
            String avgMemStr = successCount > 0 ? String.format("%.1f", (double) totalMaxMemorySuccess / successCount) : "-";
            String avgTimeStr = successCount > 0 ? String.format("%.2fms", totalTimeMsSuccess / successCount) : "-";

            String successStr = String.format("%.1f%%", 100.0 * successCount / totalPairs);
            String optimalStr = successCount > 0
                    ? String.format("%.1f%%", 100.0 * optimalCount / successCount)
                    : "-";

            int subOptimalSuccessCount = successCount - optimalCount;
            String ovhStr;
            if (subOptimalSuccessCount > 0) {
                ovhStr = String.format("%.1f%%", sumOverhead / subOptimalSuccessCount);
            } else if (successCount > 0) {
                ovhStr = "0.0%";
            } else {
                ovhStr = "-";
            }

            System.out.printf("%-14s %12s %12s %9s %9s %13s %10s%n",
                    name, avgExpStr, avgMemStr, avgTimeStr, successStr, optimalStr, ovhStr);
        }
    }
}